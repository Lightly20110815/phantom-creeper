package com.syyann.phantomcreeper;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
//? if >=1.21.2 {
/*import net.minecraft.world.entity.EntitySpawnReason;
*///?} else {
import net.minecraft.world.entity.MobSpawnType;
//?}
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 自然刷新幻翼苦力怕。参考原版 PhantomSpawner，但不检查“玩家多久没睡觉”，
 * 数量、间隔、白天、室内、维度都由 {@link ModGameRules} 控制。
 * <p>
 * 生成位置：
 * <ul>
 *   <li>有天空的维度（主世界）、玩家头顶露天：玩家上方 {@value #SKY_MIN_HEIGHT}~{@value #SKY_MAX_HEIGHT} 格</li>
 *   <li>有天空的维度、玩家在室内：默认不生成；开启室内刷新后生成在玩家所在的室内空间</li>
 *   <li>没有天空的维度（下界、末地）：玩家上方 {@value #OPEN_AIR_MIN_HEIGHT}~{@value #OPEN_AIR_MAX_HEIGHT} 格、
 *       玩家看得见的空中；天花板太低找不到位置时，开启室内刷新才会改为生成在身边</li>
 * </ul>
 */
public final class PhantomCreeperSpawner {
    private static final int SKY_MIN_HEIGHT = 15;
    private static final int SKY_MAX_HEIGHT = 25;
    private static final int HORIZONTAL_SPREAD = 10;
    /** 每只找生成点的尝试次数 */
    private static final int POSITION_ATTEMPTS = 10;
    /** 下界 / 末地：洞顶通常不高，所以生成得比主世界低 */
    private static final int OPEN_AIR_MIN_HEIGHT = 6;
    private static final int OPEN_AIR_MAX_HEIGHT = 14;
    private static final int OPEN_AIR_POSITION_ATTEMPTS = 20;
    /** 室内生成：水平范围、与玩家的最小距离、尝试次数 */
    private static final int INDOOR_SPREAD = 8;
    private static final int INDOOR_MIN_DISTANCE = 3;
    private static final int INDOOR_POSITION_ATTEMPTS = 24;

    private static final PhantomCreeperSpawner INSTANCE = new PhantomCreeperSpawner();

    /** 距离下次刷新的 tick 数；-1 表示还没开始计时（刚启动服务器 / 进入世界） */
    private int cooldown = -1;

    private PhantomCreeperSpawner() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(INSTANCE::tick);
    }

    private void tick(MinecraftServer server) {
        // 游戏规则整个服务器共用，统一从主世界读取
        ServerLevel overworld = server.overworld();
        if (overworld.getDifficulty() == Difficulty.PEACEFUL
                || !ModGameRules.isMobSpawningEnabled(overworld)
                || !ModGameRules.get(overworld, ModGameRules.SPAWNING)) {
            return;
        }
        int interval = ModGameRules.getInt(overworld, ModGameRules.SPAWN_INTERVAL) * 20;
        // 进入世界后先等满一个间隔再刷，免得玩家一登录就被炸
        if (this.cooldown < 0) {
            this.cooldown = interval;
        }
        // 把间隔调小时立即生效，不用等上一轮的倒计时走完
        if (this.cooldown > interval) {
            this.cooldown = interval;
        }
        if (--this.cooldown > 0) {
            return;
        }
        this.cooldown = interval;
        for (ServerLevel level : server.getAllLevels()) {
            if (isEnabledIn(level, overworld)) {
                this.spawnIn(level, overworld);
            }
        }
    }

    /** 各维度的开关；模组添加的其他维度不刷新 */
    private static boolean isEnabledIn(ServerLevel level, ServerLevel overworld) {
        if (level.dimension() == Level.OVERWORLD) {
            return ModGameRules.get(overworld, ModGameRules.OVERWORLD_SPAWNING);
        }
        if (level.dimension() == Level.NETHER) {
            return ModGameRules.get(overworld, ModGameRules.NETHER_SPAWNING);
        }
        if (level.dimension() == Level.END) {
            return ModGameRules.get(overworld, ModGameRules.END_SPAWNING);
        }
        return false;
    }

    /** @param overworld 游戏规则从主世界读取 */
    private void spawnIn(ServerLevel level, ServerLevel overworld) {
        boolean hasSky = level.dimensionType().hasSkyLight();
        // 只有有天空的维度分昼夜：天色不够暗（白天）时，除非开启了白天刷新，否则不刷；雷暴天气白天也算暗
        if (hasSky && !ModGameRules.get(overworld, ModGameRules.DAYTIME_SPAWNING) && level.getSkyDarken() < 5) {
            return;
        }
        int count = ModGameRules.getInt(overworld, ModGameRules.SPAWN_COUNT);
        boolean indoorSpawning = ModGameRules.get(overworld, ModGameRules.INDOOR_SPAWNING);
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                this.spawnWave(level, player, count, hasSky, indoorSpawning);
            }
        }
    }

    private void spawnWave(ServerLevel level, ServerPlayer player, int count, boolean hasSky, boolean indoorSpawning) {
        BlockPos playerPos = player.blockPosition();
        // 头顶看不到天空就算室内（建筑内、洞穴里）。原版幻翼还要求玩家高于海平面（y=63），
        // 这会导致超平坦世界永远不刷，所以没有照搬
        boolean indoors = hasSky && !level.canSeeSky(playerPos);
        if (indoors && !indoorSpawning) {
            return;
        }
        DifficultyInstance localDifficulty = level.getCurrentDifficultyAt(playerPos);
        // 同一批生成的共用一个编组：互相炸不死，其中一只爆炸时其余全部立刻引爆
        UUID spawnGroup = Mth.createInsecureUUID(level.getRandom());
        SpawnGroupData entityData = null;
        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSpawnPos(level, player, hasSky, indoors, indoorSpawning);
            if (spawnPos == null) {
                continue;
            }
            //? if >=1.21.2 {
            /*PhantomCreeperEntity entity = ModEntities.PHANTOM_CREEPER.create(level, EntitySpawnReason.NATURAL);
            *///?} else {
            PhantomCreeperEntity entity = ModEntities.PHANTOM_CREEPER.create(level);
            //?}
            if (entity == null) {
                continue;
            }
            float yaw = level.getRandom().nextFloat() * 360.0f;
            //? if >=1.21.5 {
            /*entity.snapTo(spawnPos, yaw, 0.0f);
            *///?} else {
            entity.moveTo(spawnPos, yaw, 0.0f);
            //?}
            entity.setSpawnGroup(spawnGroup);
            //? if >=1.21.2 {
            /*entityData = entity.finalizeSpawn(level, localDifficulty, EntitySpawnReason.NATURAL, entityData);
            *///?} else if >=1.20.5 {
            /*entityData = entity.finalizeSpawn(level, localDifficulty, MobSpawnType.NATURAL, entityData);
            *///?} else {
            entityData = entity.finalizeSpawn(level, localDifficulty, MobSpawnType.NATURAL, entityData, null);
            //?}
            level.addFreshEntityWithPassengers(entity);
        }
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, ServerPlayer player, boolean hasSky, boolean indoors, boolean indoorSpawning) {
        if (hasSky) {
            return indoors
                    ? findIndoorSpawnPos(level, player, level.getRandom())
                    : findSkySpawnPos(level, player.blockPosition(), level.getRandom());
        }
        BlockPos pos = findOpenAirSpawnPos(level, player, level.getRandom());
        return pos == null && indoorSpawning ? findIndoorSpawnPos(level, player, level.getRandom()) : pos;
    }

    /** 主世界：玩家上方 15~25 格的空中 */
    @Nullable
    private static BlockPos findSkySpawnPos(ServerLevel level, BlockPos playerPos, RandomSource random) {
        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            BlockPos pos = playerPos
                    .above(SKY_MIN_HEIGHT + random.nextInt(SKY_MAX_HEIGHT - SKY_MIN_HEIGHT + 1))
                    .east(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1))
                    .south(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1));
            if (hasRoomAt(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 下界 / 末地：玩家上方 6~14 格、玩家能直接看到的空中（洞顶低于 6 格的地方刷不出来） */
    @Nullable
    private static BlockPos findOpenAirSpawnPos(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos playerPos = player.blockPosition();
        for (int attempt = 0; attempt < OPEN_AIR_POSITION_ATTEMPTS; attempt++) {
            BlockPos pos = playerPos
                    .above(OPEN_AIR_MIN_HEIGHT + random.nextInt(OPEN_AIR_MAX_HEIGHT - OPEN_AIR_MIN_HEIGHT + 1))
                    .east(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1))
                    .south(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1));
            if (hasRoomAt(level, pos) && isVisibleFrom(level, player, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 玩家所在的室内空间：同样看不到天空，且和玩家之间没有墙挡着（不会刷进隔壁房间或地下空洞） */
    @Nullable
    private static BlockPos findIndoorSpawnPos(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos playerPos = player.blockPosition();
        for (int attempt = 0; attempt < INDOOR_POSITION_ATTEMPTS; attempt++) {
            BlockPos pos = playerPos.offset(
                    -INDOOR_SPREAD + random.nextInt(INDOOR_SPREAD * 2 + 1),
                    -1 + random.nextInt(4),
                    -INDOOR_SPREAD + random.nextInt(INDOOR_SPREAD * 2 + 1));
            if (pos.distSqr(playerPos) >= INDOOR_MIN_DISTANCE * INDOOR_MIN_DISTANCE
                    && !level.canSeeSky(pos)
                    && hasRoomAt(level, pos)
                    && isVisibleFrom(level, player, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 玩家的视线能直接看到该位置（中间没有方块挡着） */
    private static boolean isVisibleFrom(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0, 0.8, 0.0);
        HitResult hit = level.clip(new ClipContext(player.getEyePosition(), center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** 该位置能放下一只幻翼苦力怕（碰撞箱 0.9 x 1.6，不在液体里） */
    private static boolean hasRoomAt(ServerLevel level, BlockPos pos) {
        //? if >=1.20.5 {
        /*AABB box = ModEntities.PHANTOM_CREEPER.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        *///?} else {
        AABB box = ModEntities.PHANTOM_CREEPER.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        //?}
        return NaturalSpawner.isValidEmptySpawnBlock(level, pos, level.getBlockState(pos), level.getFluidState(pos), ModEntities.PHANTOM_CREEPER)
                && level.noCollision(box);
    }
}

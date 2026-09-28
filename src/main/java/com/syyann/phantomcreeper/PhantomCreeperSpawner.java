package com.syyann.phantomcreeper;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;

/**
 * 自然刷新幻翼苦力怕。参考原版 PhantomSpawner，但不检查“玩家多久没睡觉”，
 * 数量、间隔、白天是否刷新都由 {@link ModGameRules} 控制。
 * <p>
 * 每次刷新时，对每名头顶露天的玩家，在其上方 {@value #MIN_SPAWN_HEIGHT}~{@value #MAX_SPAWN_HEIGHT} 格、
 * 水平 ±{@value #HORIZONTAL_SPREAD} 格的空域内生成。玩家在室内（头顶看不到天空）时默认不生成，
 * 开启 doPhantomCreeperIndoorSpawning 后改为直接生成在玩家所在的室内空间。
 */
public final class PhantomCreeperSpawner {
    private static final int MIN_SPAWN_HEIGHT = 15;
    private static final int MAX_SPAWN_HEIGHT = 25;
    private static final int HORIZONTAL_SPREAD = 10;
    /** 每只找生成点的尝试次数 */
    private static final int POSITION_ATTEMPTS = 10;
    /** 室内生成：水平范围、与玩家的最小距离、尝试次数 */
    private static final int INDOOR_SPREAD = 8;
    private static final int INDOOR_MIN_DISTANCE = 3;
    private static final int INDOOR_POSITION_ATTEMPTS = 24;

    private static final PhantomCreeperSpawner INSTANCE = new PhantomCreeperSpawner();

    private int cooldown;

    private PhantomCreeperSpawner() {
    }

    static void register() {
        ServerTickEvents.END_WORLD_TICK.register(INSTANCE::tick);
    }

    private void tick(ServerWorld world) {
        GameRules rules = world.getGameRules();
        if (world.getRegistryKey() != World.OVERWORLD
                || world.getDifficulty() == Difficulty.PEACEFUL
                || !rules.getBoolean(GameRules.DO_MOB_SPAWNING)
                || !rules.getBoolean(ModGameRules.DO_PHANTOM_CREEPER_SPAWNING)) {
            return;
        }
        int interval = rules.getInt(ModGameRules.PHANTOM_CREEPER_SPAWN_INTERVAL) * 20;
        // 把间隔调小时立即生效，不用等上一轮的倒计时走完
        if (this.cooldown > interval) {
            this.cooldown = interval;
        }
        if (--this.cooldown > 0) {
            return;
        }
        this.cooldown = interval;
        // 天色不够暗（白天）时，除非开启了白天刷新，否则不刷；雷暴天气白天也算暗
        if (!rules.getBoolean(ModGameRules.DO_PHANTOM_CREEPER_DAYTIME_SPAWNING) && world.getAmbientDarkness() < 5) {
            return;
        }
        int count = rules.getInt(ModGameRules.PHANTOM_CREEPER_SPAWN_COUNT);
        boolean indoorSpawning = rules.getBoolean(ModGameRules.DO_PHANTOM_CREEPER_INDOOR_SPAWNING);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            // 头顶看不到天空就算室内（建筑内、洞穴里）。原版幻翼还要求玩家高于海平面（y=63），
            // 这会导致超平坦世界永远不刷，所以没有照搬
            boolean indoors = !world.isSkyVisible(player.getBlockPos());
            if (indoors && !indoorSpawning) {
                continue;
            }
            this.spawnWave(world, player, count, indoors);
        }
    }

    private void spawnWave(ServerWorld world, ServerPlayerEntity player, int count, boolean indoors) {
        BlockPos playerPos = player.getBlockPos();
        LocalDifficulty localDifficulty = world.getLocalDifficulty(playerPos);
        // 同一批生成的共用一个编组：互相炸不死，其中一只爆炸时其余全部立刻引爆
        UUID spawnGroup = MathHelper.randomUuid(world.random);
        EntityData entityData = null;
        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = indoors
                    ? findIndoorSpawnPos(world, player, world.random)
                    : findSkySpawnPos(world, playerPos, world.random);
            if (spawnPos == null) {
                continue;
            }
            PhantomCreeperEntity entity = ModEntities.PHANTOM_CREEPER.create(world);
            if (entity == null) {
                continue;
            }
            entity.refreshPositionAndAngles(spawnPos, world.random.nextFloat() * 360.0f, 0.0f);
            entity.setSpawnGroup(spawnGroup);
            entityData = entity.initialize(world, localDifficulty, SpawnReason.NATURAL, entityData, null);
            world.spawnEntityAndPassengers(entity);
        }
    }

    /** 玩家上方 15~25 格的空中 */
    @Nullable
    private static BlockPos findSkySpawnPos(ServerWorld world, BlockPos playerPos, Random random) {
        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            BlockPos pos = playerPos
                    .up(MIN_SPAWN_HEIGHT + random.nextInt(MAX_SPAWN_HEIGHT - MIN_SPAWN_HEIGHT + 1))
                    .east(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1))
                    .south(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1));
            if (hasRoomAt(world, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 玩家所在的室内空间：同样看不到天空，且和玩家之间没有墙挡着（不会刷进隔壁房间或地下空洞） */
    @Nullable
    private static BlockPos findIndoorSpawnPos(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos playerPos = player.getBlockPos();
        Vec3d eyePos = player.getEyePos();
        for (int attempt = 0; attempt < INDOOR_POSITION_ATTEMPTS; attempt++) {
            BlockPos pos = playerPos.add(
                    -INDOOR_SPREAD + random.nextInt(INDOOR_SPREAD * 2 + 1),
                    -1 + random.nextInt(4),
                    -INDOOR_SPREAD + random.nextInt(INDOOR_SPREAD * 2 + 1));
            if (pos.getSquaredDistance(playerPos) < INDOOR_MIN_DISTANCE * INDOOR_MIN_DISTANCE
                    || world.isSkyVisible(pos)
                    || !hasRoomAt(world, pos)) {
                continue;
            }
            Vec3d center = Vec3d.ofBottomCenter(pos).add(0.0, 0.8, 0.0);
            HitResult hit = world.raycast(new RaycastContext(eyePos, center,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return pos;
            }
        }
        return null;
    }

    /** 该位置能放下一只幻翼苦力怕（碰撞箱 0.9 x 1.6，不在液体里） */
    private static boolean hasRoomAt(ServerWorld world, BlockPos pos) {
        return SpawnHelper.isClearForSpawn(world, pos, world.getBlockState(pos), world.getFluidState(pos), ModEntities.PHANTOM_CREEPER)
                && world.isSpaceEmpty(ModEntities.PHANTOM_CREEPER.createSimpleBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }
}

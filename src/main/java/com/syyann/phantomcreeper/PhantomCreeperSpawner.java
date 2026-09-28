package com.syyann.phantomcreeper;

import org.jetbrains.annotations.Nullable;

import com.syyann.phantomcreeper.entity.PhantomCreeperEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;

/**
 * 自然刷新幻翼苦力怕。参考原版 PhantomSpawner，但不检查“玩家多久没睡觉”，
 * 数量、间隔、白天是否刷新都由 {@link ModGameRules} 控制。
 * <p>
 * 每次刷新时，对每名头顶露天的玩家，在其上方 {@value #MIN_SPAWN_HEIGHT}~{@value #MAX_SPAWN_HEIGHT} 格、
 * 水平 ±{@value #HORIZONTAL_SPREAD} 格的空域内生成。
 */
public final class PhantomCreeperSpawner {
    private static final int MIN_SPAWN_HEIGHT = 15;
    private static final int MAX_SPAWN_HEIGHT = 25;
    private static final int HORIZONTAL_SPREAD = 10;
    /** 每只找生成点的尝试次数 */
    private static final int POSITION_ATTEMPTS = 10;

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
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos playerPos = player.getBlockPos();
            // 只要求头顶露天（在洞穴/室内不刷）。原版幻翼还要求玩家高于海平面（y=63），
            // 这会导致超平坦世界永远不刷，所以去掉了
            if (!world.isSkyVisible(playerPos)) {
                continue;
            }
            this.spawnAbove(world, playerPos, count);
        }
    }

    private void spawnAbove(ServerWorld world, BlockPos playerPos, int count) {
        LocalDifficulty localDifficulty = world.getLocalDifficulty(playerPos);
        EntityData entityData = null;
        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSpawnPos(world, playerPos, world.random);
            if (spawnPos == null) {
                continue;
            }
            PhantomCreeperEntity entity = ModEntities.PHANTOM_CREEPER.create(world);
            if (entity == null) {
                continue;
            }
            entity.refreshPositionAndAngles(spawnPos, world.random.nextFloat() * 360.0f, 0.0f);
            entityData = entity.initialize(world, localDifficulty, SpawnReason.NATURAL, entityData, null);
            world.spawnEntityAndPassengers(entity);
        }
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerWorld world, BlockPos playerPos, Random random) {
        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            BlockPos pos = playerPos
                    .up(MIN_SPAWN_HEIGHT + random.nextInt(MAX_SPAWN_HEIGHT - MIN_SPAWN_HEIGHT + 1))
                    .east(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1))
                    .south(-HORIZONTAL_SPREAD + random.nextInt(HORIZONTAL_SPREAD * 2 + 1));
            if (SpawnHelper.isClearForSpawn(world, pos, world.getBlockState(pos), world.getFluidState(pos), ModEntities.PHANTOM_CREEPER)
                    && world.isSpaceEmpty(ModEntities.PHANTOM_CREEPER.createSimpleBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                return pos;
            }
        }
        return null;
    }
}

package com.syyann.phantomcreeper;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

/** 所有游戏规则，可用 /gamerule 或创建世界时的“编辑游戏规则”界面修改 */
public final class ModGameRules {
    /** 是否自然生成幻翼苦力怕 */
    public static final GameRules.Key<GameRules.BooleanRule> DO_PHANTOM_CREEPER_SPAWNING = GameRuleRegistry.register(
            "doPhantomCreeperSpawning", GameRules.Category.SPAWNING, GameRuleFactory.createBooleanRule(true));

    /** 白天是否也生成；开启时幻翼苦力怕也不会被阳光烧着 */
    public static final GameRules.Key<GameRules.BooleanRule> DO_PHANTOM_CREEPER_DAYTIME_SPAWNING = GameRuleRegistry.register(
            "doPhantomCreeperDaytimeSpawning", GameRules.Category.SPAWNING, GameRuleFactory.createBooleanRule(false));

    /** 每次刷新时，在每名玩家上空生成的数量 */
    public static final GameRules.Key<GameRules.IntRule> PHANTOM_CREEPER_SPAWN_COUNT = GameRuleRegistry.register(
            "phantomCreeperSpawnCount", GameRules.Category.SPAWNING, GameRuleFactory.createIntRule(1, 1, 64));

    /** 两次刷新之间的间隔（秒） */
    public static final GameRules.Key<GameRules.IntRule> PHANTOM_CREEPER_SPAWN_INTERVAL = GameRuleRegistry.register(
            "phantomCreeperSpawnInterval", GameRules.Category.SPAWNING, GameRuleFactory.createIntRule(60, 1, 3600));

    private ModGameRules() {
    }

    /** 触发类加载，确保在世界加载前完成注册 */
    static void register() {
    }
}

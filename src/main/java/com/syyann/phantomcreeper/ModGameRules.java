package com.syyann.phantomcreeper;

//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
*///?} else {
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
//?}

/**
 * 所有游戏规则，可用 /gamerule 或创建世界时的“编辑游戏规则”界面修改。
 * <p>
 * 1.21.11 起原版把游戏规则改成了带命名空间的小写 ID（如 minecraft:spawn_mobs），
 * 所以在 1.21.11+ 里本 Mod 的规则叫 phantomcreeper:spawning 等；更早的版本沿用 doPhantomCreeperSpawning 等名字。
 * 其他代码统一通过 {@link #get} / {@link #getInt} 读取，不用关心版本差异。
 */
public final class ModGameRules {
    // 各规则含义：
    // SPAWNING            是否自然生成（总开关）
    // DAYTIME_SPAWNING    白天是否也生成；开启时幻翼苦力怕也不会被阳光烧着
    // INDOOR_SPAWNING     玩家头顶看不到天空（建筑内、洞穴里）时是否也生成；开启后直接生成在玩家所在的室内空间
    // OVERWORLD/NETHER/END_SPAWNING  各维度是否生成；下界和末地没有天空和昼夜，会生成在玩家上方看得见的空中
    // SPAWN_COUNT         每次刷新时，在每名玩家附近生成的数量
    // SPAWN_INTERVAL      两次刷新之间的间隔（秒）
    //? if >=1.21.11 {
    /*public static final GameRule<Boolean> SPAWNING = bool("spawning", true);
    public static final GameRule<Boolean> DAYTIME_SPAWNING = bool("daytime_spawning", false);
    public static final GameRule<Boolean> INDOOR_SPAWNING = bool("indoor_spawning", false);
    public static final GameRule<Boolean> OVERWORLD_SPAWNING = bool("overworld_spawning", true);
    public static final GameRule<Boolean> NETHER_SPAWNING = bool("nether_spawning", false);
    public static final GameRule<Boolean> END_SPAWNING = bool("end_spawning", false);
    public static final GameRule<Integer> SPAWN_COUNT = integer("spawn_count", 1, 1, 64);
    public static final GameRule<Integer> SPAWN_INTERVAL = integer("spawn_interval", 60, 1, 3600);

    private static GameRule<Boolean> bool(String name, boolean defaultValue) {
        return GameRuleBuilder.forBoolean(defaultValue)
                .category(GameRuleCategory.SPAWNING)
                .buildAndRegister(PhantomCreeperMod.id(name));
    }

    private static GameRule<Integer> integer(String name, int defaultValue, int min, int max) {
        return GameRuleBuilder.forInteger(defaultValue)
                .range(min, max)
                .category(GameRuleCategory.SPAWNING)
                .buildAndRegister(PhantomCreeperMod.id(name));
    }

    public static boolean get(ServerLevel level, GameRule<Boolean> rule) {
        return level.getGameRules().get(rule);
    }

    public static int getInt(ServerLevel level, GameRule<Integer> rule) {
        return level.getGameRules().get(rule);
    }

    public static boolean isMobSpawningEnabled(ServerLevel level) {
        return level.getGameRules().get(GameRules.SPAWN_MOBS);
    }
    *///?} else {
    public static final GameRules.Key<GameRules.BooleanValue> SPAWNING = bool("doPhantomCreeperSpawning", true);
    public static final GameRules.Key<GameRules.BooleanValue> DAYTIME_SPAWNING = bool("doPhantomCreeperDaytimeSpawning", false);
    public static final GameRules.Key<GameRules.BooleanValue> INDOOR_SPAWNING = bool("doPhantomCreeperIndoorSpawning", false);
    public static final GameRules.Key<GameRules.BooleanValue> OVERWORLD_SPAWNING = bool("doPhantomCreeperOverworldSpawning", true);
    public static final GameRules.Key<GameRules.BooleanValue> NETHER_SPAWNING = bool("doPhantomCreeperNetherSpawning", false);
    public static final GameRules.Key<GameRules.BooleanValue> END_SPAWNING = bool("doPhantomCreeperEndSpawning", false);
    public static final GameRules.Key<GameRules.IntegerValue> SPAWN_COUNT = integer("phantomCreeperSpawnCount", 1, 1, 64);
    public static final GameRules.Key<GameRules.IntegerValue> SPAWN_INTERVAL = integer("phantomCreeperSpawnInterval", 60, 1, 3600);

    private static GameRules.Key<GameRules.BooleanValue> bool(String name, boolean defaultValue) {
        return GameRuleRegistry.register(name, GameRules.Category.SPAWNING, GameRuleFactory.createBooleanRule(defaultValue));
    }

    private static GameRules.Key<GameRules.IntegerValue> integer(String name, int defaultValue, int min, int max) {
        return GameRuleRegistry.register(name, GameRules.Category.SPAWNING, GameRuleFactory.createIntRule(defaultValue, min, max));
    }

    public static boolean get(Level level, GameRules.Key<GameRules.BooleanValue> rule) {
        return level.getGameRules().getBoolean(rule);
    }

    public static int getInt(Level level, GameRules.Key<GameRules.IntegerValue> rule) {
        return level.getGameRules().getInt(rule);
    }

    public static boolean isMobSpawningEnabled(Level level) {
        return level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
    }
    //?}

    private ModGameRules() {
    }

    /** 触发类加载，确保在世界加载前完成注册 */
    static void register() {
    }
}

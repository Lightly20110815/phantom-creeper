# 幻翼苦力怕 Phantom Creeper

一个 Minecraft Java 版 **Fabric** Mod，支持 **1.20.1 / 1.20.4 / 1.21.1 / 1.21.11 / 26.3**：一只下面吊着苦力怕的幻翼，入夜后从玩家头顶出现，锁定玩家俯冲，贴脸自爆。

![幻翼苦力怕](docs/screenshot.png)

> English: **Fabric only** (requires Fabric API; Forge/NeoForge are not supported). Builds for Minecraft **1.20.1, 1.20.4, 1.21.1, 1.21.11 and 26.3** — download the jar matching your game version. A phantom carrying a creeper: it spawns above players (no insomnia required), locks on, dives and explodes within 6 blocks. Spawn count, interval, daytime/indoor spawning and per-dimension spawning are configurable via game rules — see [Game rules](#游戏规则) (rule names differ between 1.20.1–1.21.1 and 1.21.11+).

## 特性

- **外观**：上半截是幻翼，下半截吊着一只苦力怕。俯冲时苦力怕保持竖直下垂，还会轻轻摆动
- **刷新**：不需要失眠条件。玩家头顶露天时，每隔一段时间在玩家上方 15～25 格内生成
- **攻击**：锁定 64 格内看得到的玩家 → 尖啸俯冲 → 进入 6 格点燃引信（0.5 秒，会膨胀闪白）→ 爆炸，威力与苦力怕相同
- **放弃**：玩家躲进室内，连续 5 秒看不到就解除锁定，回到空中盘旋
- **连锁引爆**：同一批生成的幻翼苦力怕互相炸不死；其中一只爆炸时，同批的其余成员不论在哪都会**立刻全部引爆**
- **室内生成（可选）**：默认玩家在室内/洞穴里不会刷新；开启游戏规则后会直接生成在玩家所在的室内空间
- **维度（可选）**：默认只在主世界刷新；可以用游戏规则开启下界、末地，或关闭主世界
- **怕阳光**：白天会像幻翼一样被晒着火（开启白天刷新时除外）
- **破坏方块**：受 `mobGriefing` 游戏规则控制
- **掉落**：火药 0～2；被玩家击杀时额外掉幻翼膜 0～1（均受抢夺加成）
- **刷怪蛋**：在创造模式物品栏的“刷怪蛋”页
- **语言**：简体中文、English

## 兼容性

每个游戏版本对应一个 jar，**按你的游戏版本下载对应的那个**：

| 游戏版本 | 下载这个 jar | 需要的 Fabric API | Java |
|---|---|---|---|
| 1.20、1.20.1 | `phantom-creeper-1.2.0+1.20.1.jar` | 1.20.1 版（如 0.92.12+1.20.1） | 17+ |
| 1.20.3、1.20.4 | `phantom-creeper-1.2.0+1.20.4.jar` | 1.20.4 版（如 0.97.3+1.20.4） | 17+ |
| 1.21、1.21.1 | `phantom-creeper-1.2.0+1.21.1.jar` | 1.21.1 版（如 0.116.17+1.21.1） | 21+ |
| 1.21.11 | `phantom-creeper-1.2.0+1.21.11.jar` | 1.21.11 版（如 0.141.6+1.21.11） | 21+ |
| 26.3 | `phantom-creeper-1.2.0+26.3.jar` | 26.3 版（如 0.161.0+26.3） | 25+ |

- 表中其他版本（1.20.2、1.20.5～1.20.6、1.21.2～1.21.10、26.1～26.2 等）**暂不支持**，放进去加载器会直接拒绝
- 实际测试过的是 1.20.1、1.20.4、1.21.1、1.21.11、26.3；1.20、1.20.3、1.21 与对应版本内部相同（和 Fabric API 声明的范围一致），但没有单独测试
- Java 版本由启动器自动选择，一般不用管

| 项目 | 支持情况 |
|---|---|
| Fabric | ✅ 需要 Fabric Loader + Fabric API（Loader 用最新版即可） |
| Forge / NeoForge | ❌ 不支持，放进 Forge/NeoForge 的 mods 文件夹不会生效 |
| Quilt | ⚠️ Quilt 通常能加载 Fabric Mod，但未测试 |
| 基岩版（手机、主机、Win10/11 商店版） | ❌ 不支持，这是 Java 版 Mod |
| 单人 / 局域网 / 服务器 | ✅ 都可以。多人游戏时服务端和所有客户端都要安装（1.21.11 起没装的客户端会被 Fabric API 拒绝进入） |
| 其他 Mod | ✅ 本 Mod 不修改原版代码（没有使用 Mixin），一般不会冲突。Sodium、Iris 等未专门测试 |
| 旧存档 | ✅ 直接加入即可；卸载后已生成的幻翼苦力怕会从存档中消失 |

## 安装

1. 到 [Releases](https://github.com/Lightly20110815/phantom-creeper/releases) 下载**与游戏版本对应**的 jar（见上表）
2. 下载对应版本的 [Fabric API](https://modrinth.com/mod/fabric-api/versions)
3. 把两个 jar 一起放进 `mods` 文件夹：
   - **官方启动器 / 未开启版本隔离**：`.minecraft/mods/`
   - **HMCL / PCL 等开启了版本隔离的启动器**：`.minecraft/versions/<版本名>/mods/`
     （HMCL 也可以在“版本设置 → 模组管理”里直接把 jar 拖进去）
4. 用启动器启动对应版本的 **Fabric** 版本

升级时记得**删掉 mods 里旧版本的本 Mod jar**，两个版本同时存在会导致游戏无法启动。

## 快速体验

进入一个**非和平难度**的世界，打开聊天框输入：

```mcfunction
/time set midnight
# 1.20.1 ~ 1.21.1：
/gamerule phantomCreeperSpawnInterval 5
# 1.21.11 / 26.3：
/gamerule phantomcreeper:spawn_interval 5
```

站在露天处，几秒后幻翼苦力怕就会在头顶出现并俯冲过来。

> 创造模式下它们不会攻击你（和原版怪物一样），想被炸就切回生存模式：`/gamemode survival`

也可以直接召唤：

```mcfunction
/summon phantomcreeper:phantom_creeper ~ ~20 ~
```

## 游戏规则

所有规则都可以用 `/gamerule` 修改，也可以在创建世界时的“更多世界选项 → 游戏规则”界面里修改（“生物生成”分类下）。

1.21.11 起原版把游戏规则改成了带命名空间的小写名字（如 `doMobSpawning` 变成了 `minecraft:spawn_mobs`），本 Mod 也跟着改了，所以**两组版本的规则名不同**：

| 1.20.1 ～ 1.21.1 | 1.21.11 / 26.3 | 默认值 | 取值范围 | 说明 |
|---|---|---|---|---|
| `doPhantomCreeperSpawning` | `phantomcreeper:spawning` | `true` | true / false | 是否自然生成（总开关） |
| `doPhantomCreeperDaytimeSpawning` | `phantomcreeper:daytime_spawning` | `false` | true / false | 白天是否也生成。开启时幻翼苦力怕**不会被阳光烧着** |
| `doPhantomCreeperIndoorSpawning` | `phantomcreeper:indoor_spawning` | `false` | true / false | 玩家在室内（头顶看不到天空，含建筑和洞穴）时是否也生成。关闭时不刷；开启时直接生成在玩家所在的室内空间 |
| `doPhantomCreeperOverworldSpawning` | `phantomcreeper:overworld_spawning` | `true` | true / false | 是否在**主世界**生成 |
| `doPhantomCreeperNetherSpawning` | `phantomcreeper:nether_spawning` | `false` | true / false | 是否在**下界**生成 |
| `doPhantomCreeperEndSpawning` | `phantomcreeper:end_spawning` | `false` | true / false | 是否在**末地**生成 |
| `phantomCreeperSpawnCount` | `phantomcreeper:spawn_count` | `1` | 1 ～ 64 | 每次刷新时，在**每名玩家**附近生成的数量 |
| `phantomCreeperSpawnInterval` | `phantomcreeper:spawn_interval` | `60` | 1 ～ 3600 | 两次刷新之间的间隔（秒）。调小后立即生效 |

示例（1.20.1 ～ 1.21.1 的写法；1.21.11 / 26.3 换成右边一列的名字即可）：

```mcfunction
# 每 30 秒刷一次，每次 3 只
/gamerule phantomCreeperSpawnInterval 30
/gamerule phantomCreeperSpawnCount 3

# 白天也刷
/gamerule doPhantomCreeperDaytimeSpawning true

# 躲在屋里也会刷进屋里
/gamerule doPhantomCreeperIndoorSpawning true

# 只在下界刷，主世界不刷
/gamerule doPhantomCreeperNetherSpawning true
/gamerule doPhantomCreeperOverworldSpawning false

# 关闭自然生成（刷怪蛋和 /summon 仍然可用）
/gamerule doPhantomCreeperSpawning false

# 爆炸不破坏方块（原版规则，同时影响其他生物；1.21.11+ 为 minecraft:mob_griefing）
/gamerule mobGriefing false
```

### 刷新条件

进入世界后先等满一个刷新间隔，之后每到间隔，检查以下条件：

1. 难度**不是和平**，原版的生物生成规则（`doMobSpawning` / `minecraft:spawn_mobs`）与总开关都为 `true`
2. 玩家所在维度的开关已开启（主世界 / 下界 / 末地）
3. **主世界**：天色够暗（夜晚或雷暴天）；开启白天刷新时忽略此条。下界和末地没有昼夜，**随时**都会刷
4. 玩家不是旁观模式

然后按玩家所处位置生成指定数量：

- **主世界、露天**（头顶能看到天空）：在玩家上方 15～25 格、水平 ±10 格内的空位生成
- **主世界、室内**（头顶看不到天空，包括建筑和洞穴）：
  - 室内刷新关闭（默认）：不生成
  - 室内刷新开启：在玩家周围水平 ±8 格、距离玩家至少 3 格、**玩家能直接看到**的室内空位生成（不会刷进隔壁房间或地下的封闭空洞）。室内空间小，生成后往往很快就会进入 6 格引爆范围，非常危险
- **下界、末地**：在玩家上方 6～14 格、水平 ±10 格、**玩家能直接看到**的空中生成；洞顶太低（不足 6 格）找不到位置时，只有开启室内刷新才会改为生成在玩家身边

### 连锁引爆

每次刷新时，给同一名玩家生成的那一批幻翼苦力怕属于同一个编组：

- 同组成员的爆炸**炸不死**彼此
- 任何一只爆炸时，同组其余成员**不论距离多远、引信是否点燃，全部立刻引爆**
- 不同批次、刷怪蛋或 `/summon` 召唤的不受影响（会被正常炸伤）

也可以用 NBT 手动编组，例如召唤两只同组的：

```mcfunction
/summon phantomcreeper:phantom_creeper ~ ~10 ~ {SpawnGroup:[I;1,2,3,4]}
/summon phantomcreeper:phantom_creeper ~5 ~10 ~ {SpawnGroup:[I;1,2,3,4]}
```

## 可调参数

以下数值目前需要改代码后重新构建，位于 [`PhantomCreeperEntity.java`](src/main/java/com/syyann/phantomcreeper/entity/PhantomCreeperEntity.java) 和 [`PhantomCreeperSpawner.java`](src/main/java/com/syyann/phantomcreeper/PhantomCreeperSpawner.java) 开头：

| 常量 | 默认 | 含义 |
|---|---|---|
| `TRIGGER_RADIUS` | 6.0 | 距离玩家多少格以内点燃引信 |
| `FUSE_TIME` | 10 | 引信时长（tick，20 tick = 1 秒） |
| `EXPLOSION_POWER` | 3.0 | 爆炸威力（苦力怕为 3，闪电苦力怕为 6） |
| `LOCK_ON_RANGE` | 64.0 | 锁定玩家的最大距离 |
| `DIVE_SPEED` / `CIRCLE_SPEED` | 0.85 / 0.45 | 俯冲 / 盘旋速度 |
| `SKY_MIN_HEIGHT` / `SKY_MAX_HEIGHT` | 15 / 25 | 主世界生成高度（玩家上方多少格） |
| `OPEN_AIR_MIN_HEIGHT` / `OPEN_AIR_MAX_HEIGHT` | 6 / 14 | 下界 / 末地生成高度 |

## 从源码构建

项目用 [Stonecutter](https://github.com/stonecutter-versioning/stonecutter) 管理多个游戏版本：**只有一份源码**，各版本的差异用 `//? if >=1.21 {` 这样的注释标出，构建时自动为每个版本生成对应代码。

```bash
git clone https://github.com/Lightly20110815/phantom-creeper.git
cd phantom-creeper
./gradlew build                  # 构建全部 5 个版本，产物在 versions/<版本>/build/libs/
./gradlew :1.20.4:build          # 只构建某个版本
./gradlew :1.20.4:runClient      # 启动该版本带本 Mod 的开发版客户端
./gradlew :1.20.4:runServer      # 启动该版本的开发版服务端
```

**JDK**：Gradle 本身运行在 JDK 25 上，编译 1.20.x / 1.21.x 用 JDK 21，编译 26.x 用 JDK 25。本机没有的会**自动下载**，不需要手动安装，也不要求它们是系统默认 Java。

**修改代码**：`src/` 里直接编辑的是“当前激活版本”（仓库里提交的是 1.20.4）。要编辑其他版本特有的代码，先切换激活版本，改完再切回来提交：

```bash
./gradlew "Set active project to 26.3"   # 切换后 src/ 里 26.3 的代码变为可编辑状态
./gradlew "Reset active project"          # 提交前切回 1.20.4
```

### 项目结构

```
stonecutter.gradle.kts / settings.gradle.kts / build.gradle.kts   # 多版本构建配置
versions/<版本>/gradle.properties   # 各版本的 Fabric API 版本、兼容范围
src/main/java/com/syyann/phantomcreeper/
├── PhantomCreeperMod.java          # 入口
├── ModEntities.java                # 实体注册（碰撞箱 0.9 × 1.6）
├── ModItems.java                   # 刷怪蛋
├── ModGameRules.java               # 游戏规则（两套命名都在这里）
├── PhantomCreeperSpawner.java      # 自然刷新（维度、室内、昼夜）
└── entity/PhantomCreeperEntity.java  # AI：锁定、俯冲、盘旋、引信、爆炸、连锁引爆
src/client/java/com/syyann/phantomcreeper/client/
├── PhantomCreeperRenderer.java, HangingCreeperLayer.java, PhantomCreeperRenderState.java
│                                    # 1.21.2+ 的渲染（新版“渲染状态”结构）
└── LegacyPhantomCreeperRenderer.java, LegacyHangingCreeperLayer.java
                                     # 1.21.1 及更早的渲染
src/versioned/                      # 各版本格式不同的资源（掉落表、刷怪蛋模型/贴图）
```

模型直接复用原版幻翼与苦力怕的资源；1.21.5 起刷怪蛋需要单独贴图，Mod 自带一张原创的 16×16 刷怪蛋贴图。

## 许可证

[MIT](LICENSE)

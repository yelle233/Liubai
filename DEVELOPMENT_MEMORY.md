# 留白（Liubai）开发记忆

> 最后更新：2026-07-24  
> 当前发布版本：1.1.2  
> 用途：让后续开发者快速恢复项目背景。开始开发前应完整阅读本文；完成重要改动后同步更新。

## 1. 项目定位

- 名称：留白 / Liubai
- Mod ID：`liubai`
- 包名：`com.yelle233.liubai`
- Minecraft：1.21.1
- 加载器：NeoForge
- Java：21
- 类型：纯物理客户端渲染优化模组
- 作者：Yelle233
- 许可证：MIT，正文见 `LICENSE`
- 标志：根目录 `logo.png`，同一文件复制到 `src/main/resources/logo.png`

核心定位：

```text
Entity Culling：判断普通对象是否完全不可见。
Liubai：决定剩余可见对象值得消耗多少渲染预算。
```

留白不优化服务器 TPS，不停止实体 AI、红石、配方、机器逻辑或服务端 Tick。

## 2. 当前版本与发布元数据

`gradle.properties` 当前关键值：

```properties
minecraft_version=1.21.1
neo_version=21.1.243
neo_version_range=[21.1.115,)
mod_version=1.1.2
mod_license=MIT
```

- `neo_version` 是开发编译版本。
- `neo_version_range` 是发布包声明的最低兼容范围，二者不要重新合并。
- NeoForge 21.1.115 已实际编译并启动验证。
- 21.1.65 曾尝试验证，但官方 Maven 连接连续重置，未得出兼容结论；不要声明支持 21.1.115 以下版本。
- Create 6.0.10 自身要求 NeoForge 21.1.219+，Ponder 1.0.82 要求 21.1.206+。整合包最终下限可能高于留白下限。

当前发布产物：

```text
build/libs/liubai-1.1.2.jar
```

已验证 JAR 内包含：

```text
META-INF/neoforge.mods.toml
logo.png
META-INF/LICENSE_liubai
```

最终元数据已验证为：

```toml
license="MIT"
version="1.1.2"
logoFile="logo.png"
```

`TEMPLATE_LICENSE.txt` 是 NeoForge MDK 模板的上游许可证，必须与项目自己的 `LICENSE` 区分并保留。

## 3. 入口与客户端生命周期

入口：`src/main/java/com/yelle233/liubai/Liubai.java`

```java
@Mod(value = Liubai.MODID, dist = Dist.CLIENT)
```

- 标准专用服务端安装留白时不会初始化，也没有优化效果，正常情况下不会崩溃。
- 单人游戏仍是物理客户端环境，留白正常工作，但不干涉内置服务器逻辑。
- `LiubaiClientRuntime.initialize` 必须先注册配置事件监听器，再由入口注册配置。
- 配置只在 `ModConfigEvent.Loading/Reloading` 后读取；`Unloading` 时清空快照。
- 配置未加载时，渲染帧必须安全走原版路径，禁止直接调用 `ConfigValue.get()`。

历史崩溃：根目录 `crash-2026-07-23_17.51.12-client.txt`。

原异常：

```text
IllegalStateException: Cannot get config value before config is loaded.
ConfigSnapshot.read -> LiubaiClientSystem.beginFrame
```

已修复：

- 监听器先于配置注册。
- 首帧不再兜底读取未加载配置。
- 每帧固定使用同一个 `ConfigSnapshot`，避免帧中重载产生混合配置。

不要恢复旧的 `if (config == null) config = ConfigSnapshot.read()` 行为。

## 4. 主要代码结构

| 文件/模块 | 职责 |
|---|---|
| `Liubai.java` | 纯客户端入口、配置注册 |
| `client/LiubaiClientRuntime.java` | 客户端初始化、配置生命周期、配置屏幕工厂 |
| `client/ClientEvents.java` | 帧事件、实体事件、名称牌、HUD、ESC配置按钮 |
| `client/LiubaiClientSystem.java` | 每帧协调器、配置快照、压力/后端/策略调度 |
| `client/FrameBudgetController.java` | NORMAL/HIGH/CRITICAL 自适应压力 |
| `client/RenderPolicyManager.java` | 实体、方块实体、阴影、名称牌、粒子决策 |
| `client/RenderStatistics.java` | HUD统计，当前主要是上一帧统计 |
| `client/TemporalLodScheduler.java` | 显式适配器使用的时间更新槽 |
| `visibility/VisibilityService.java` | 内置保守DDA、队列、缓存和确认 |
| `compat/CompatibilityManager.java` | Entity Culling/Create/Flywheel检测与后端选择 |
| `mixin/*` | 实体、方块实体、阴影、粒子、Flywheel注入 |
| `config/ClientConfig.java` | NeoForge客户端配置及默认值 |
| `api/LiubaiApi.java` | 强制可见与显式时间LOD接口 |

Mixin配置：`src/main/resources/liubai.mixins.json`。

- `required=false`
- `defaultRequire=0`
- Flywheel Mixin 使用 `@Pseudo`，Flywheel缺失或方法不匹配时应安全跳过。

## 5. 自适应帧预算

`FrameBudgetController` 使用指数平滑：

- 初始平均帧时间：16.67 ms
- 每帧新样本权重：0.08
- 平均时间高于目标帧时间的 1.12 倍，累计24帧后升压。
- 平均时间低于目标帧时间的 0.88 倍，累计120帧后逐级恢复。
- 压力：`NORMAL -> HIGH -> CRITICAL`
- 关闭自适应模式时固定回到 `NORMAL`。

当前控制器可能在粒子削减使帧时间恢复后出现周期性压力振荡，需要后续观察和改进。

## 6. Entity Culling协作

遮挡模式：

- `AUTO`：检测到 Entity Culling 时使用 `ENTITY_CULLING`；否则使用 `BUILTIN`。
- `BUILTIN`：强制留白内置DDA。与Entity Culling同装会重叠，不推荐。
- `EXTERNAL`：当前没有通用外部提供者接口实现；该模式不会运行内置DDA，不要宣传为已适配更多模组。
- `OFF`：关闭通用遮挡，次要效果和显式LOD仍可工作。

Entity Culling存在且使用AUTO时：

- 留白不运行DDA队列。
- 留白不遮挡剔除方块实体。
- 留白自己的“遮挡实体/方块实体”HUD统计通常为0。
- 留白仍可处理屏幕中过小的实体、阴影、名称牌、粒子和Flywheel更新限制。
- Entity Culling的跳过数量不会计入留白HUD。

已测试：Entity Culling 1.10.5。

## 7. Create/Flywheel协作

明确支持的版本：

```text
Create 6.0.10
Flywheel 1.0.6
```

适配目标：`dev.engine_room.flywheel.impl.visual.BandedPrimeLimiter#getUpdateDivisor(double)`。

行为：

- 只在版本匹配、时间LOD开启、Flywheel限制开启且压力非NORMAL时工作。
- 距离不超过 `max(16, safeDistance)` 时保持原值。
- HIGH：默认把原更新间隔乘以2。
- CRITICAL：默认乘以3。
- CRITICAL且距离平方至少1024（32格）时，间隔至少达到配置的最大时间间隔，默认8。
- 最终间隔限制在安全范围内，最大31。
- 只降低客户端Visual更新频率，不删除Instance，不停止机械逻辑。

如果HUD中“Flywheel限制增强”始终为0，检查：

1. Create/Flywheel版本是否匹配。
2. Flywheel后端是否为 `flywheel:off`。
3. 压力是否仍为NORMAL。
4. 时间LOD和自适应限制是否开启。
5. 机械是否超过16格。
6. 被测试Visual是否实际使用BandedPrimeLimiter。

## 8. 内置遮挡实现

`VisibilityService` 当前特点：

- 队列最大4096。
- 默认每帧检查8个请求。
- 对包围盒中心和八角附近共9个采样点做Amanatides-Woo体素遍历。
- 只接受 `canOcclude && isSolidRender` 的完整实心方块作为强遮挡物。
- 任一路径清晰即判定可见；区块未加载或不确定时返回UNKNOWN并正常渲染。
- 默认连续确认2次遮挡才允许跳过。
- 默认遮挡缓存30帧；可见结果最多6帧后复查，未知2帧后复查。

当前已知问题一：移动失效导致帧率下降。

```java
record.camera.distanceToSqr(currentCamera) > 0.25
```

摄像机相对记录位置移动超过约0.5格时，旧结论立即变为UNKNOWN并恢复渲染，同时进入重检队列。大量对象、默认每帧8次检查和连续2次确认会导致玩家移动时队列追不上。

建议的后续方向：短期保留稳定遮挡结果、设置很短宽限期、按风险/距离优先重检、移动时动态预算、墙角/门口快速失效。

当前已知问题二：近墙时不再遮挡。

- 默认 `safeDistance=8`，8格内实体直接返回SAFE，不做遮挡。
- 默认 `occlusionMinDistance=12`，8～12格也不会进入遮挡检测。
- 当前计算的是摄像机到对象距离，不是摄像机到墙的距离。

因此玩家靠近墙时，墙后掉落物即使不可见也可能重新渲染。更合理的设计是：安全距离只保护屏幕尺寸LOD和激进效果，可靠完整实心墙仍可进行高置信度遮挡。

临时配置建议：`safeDistance=2`、`occlusionMinDistance=4`，但要测试墙角闪现。

## 9. 当前渲染策略

### 实体

- 安全距离、重要实体、API强制可见、白名单和禁用命名空间优先保护。
- 屏幕尺寸跳过只在HIGH/CRITICAL、距离超过24格、投影半径低于阈值时发生。
- 默认阈值：HIGH约1.25像素，CRITICAL约2像素。
- 内置遮挡只在有效后端为BUILTIN时运行。

重要实体包括：玩家、末影龙、凋灵、TNT、弹射物、闪电、乘客、载具、发光、着火、准星目标，以及正在攻击玩家的Mob。

### 方块实体

- 只在BUILTIN后端使用留白遮挡。
- Entity Culling协作模式直接完整放行给外部模组处理。
- 默认保护信标、末地折跃门、末地传送门、结构方块和潮涌核心。

### 阴影

- 默认距离24格。
- HIGH约18格，CRITICAL约12格。
- 重要实体保护。

### 名称牌

- 默认距离48格。
- CRITICAL约36格。
- 玩家和其他重要实体保护。

### 粒子

- 只在HIGH/CRITICAL且超过默认32格时减少。
- HIGH按序列跳过约1/2；CRITICAL跳过约3/4。
- 注入位置是 `ParticleEngine.add`，粒子对象已经创建，仍支付数据包解析、Provider调用和对象分配成本。
- 持续高强度粒子源可能让保留下来的粒子最终仍填满Minecraft粒子容量，因此稳态收益有限。

当前已知问题：粒子HUD疯狂在0和某个数间跳动。

- `RenderStatistics.finishFrame` 每个渲染帧把统计清零。
- 命令方块粒子通常每游戏Tick（20 Hz）批量加入，而渲染帧更快。
- 因此只有收到粒子的帧有数字，其余帧显示0。这是统计粒度问题，不等于策略每帧开关。

建议的后续方向：

1. HUD改成1秒滑动总量、每秒速率和接受率。
2. 在Provider创建粒子前进行预算判断。
3. 根据当前存活粒子总量使用令牌桶/动态预算。
4. 给粒子压力增加更稳定的迟滞，避免状态周期振荡。

## 10. 配置与界面

- 配置文件：生产环境 `config/liubai-client.toml`，开发环境 `run/config/liubai-client.toml`。
- 配置屏幕通过NeoForge `IConfigScreenFactory` 注册。
- ESC暂停菜单右上角有“留白设置”按钮。
- 中英文翻译：
  - `src/main/resources/assets/liubai/lang/zh_cn.json`
  - `src/main/resources/assets/liubai/lang/en_us.json`
- HUD默认关闭。
- 配置保存/重载必须继续使用不可变 `ConfigSnapshot`，不要在热路径反复读取同步配置值。

主要默认值：

| 配置 | 默认值 |
|---|---:|
| enabled | true |
| targetFps | 60 |
| adaptiveMode | true |
| occlusionMode | AUTO |
| safeDistance | 8 |
| occlusionMinDistance | 12 |
| checksPerFrame | 8 |
| occludedConfirmations | 2 |
| cacheTtlFrames | 30 |
| minimumProjectedRadius | 1.25 |
| screenSpace | true |
| temporal | true |
| maxTemporalInterval | 8 |
| shadowDistance | 24 |
| nameTagDistance | 48 |
| particleDistance | 32 |
| Flywheel HIGH倍率 | 2 |
| Flywheel CRITICAL倍率 | 3 |

默认禁用通用实体/方块实体剔除的命名空间：`create`、`flywheel`。这不等于禁用版本锁定的Flywheel适配器。

## 11. 已完成验证

### 当前开发版本

- NeoForge 21.1.243 完整 `gradlew.bat build` 成功。
- Create 6.0.10、Flywheel 1.0.6、Entity Culling 1.10.5 开发客户端曾成功加载。
- 日志确认Entity Culling协作后端切换成功。
- 日志确认Flywheel Mixin在支持版本应用。

### 最低NeoForge版本

- 使用 `-Pneo_version=21.1.115` 编译成功。
- 临时移出要求更高NeoForge的Create 6.0.10后，NeoForge 21.1.115 + Liubai 1.1.1 + Entity Culling 1.10.5 实际启动和资源加载成功。
- 测试后Create JAR已恢复到 `run/mods`。

### 许可证和封面

- `logo.png` 为2048×2048 PNG。
- 源图和JAR内 `logo.png` 的SHA-256已验证一致。
- JAR元数据、封面和MIT正文均已验证存在。

## 12. 构建与测试命令

PowerShell / Windows：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat "-Pneo_version=21.1.115" compileJava
```

快速进入测试世界：

```powershell
.\gradlew.bat runClient -PliubaiQuickPlayWorld=测试世界目录名
```

注意：PowerShell传带多个点的Gradle属性时应加双引号，例如 `"-Pneo_version=21.1.115"`，否则曾被解析成错误任务名。

`runClient` 被终止后，Windows下可能残留Gradle包装进程和游戏Java子进程。只能结束本次启动且命令行明确包含本项目路径/`forgeclient*`的进程，不要杀死所有Java进程。

当前没有单元测试源码，构建输出为 `test NO-SOURCE`。行为改动至少需要完整构建和开发客户端烟雾测试。

## 13. 工作区现状与注意事项

- 当前目录未发现可用 `.git` 仓库，之前执行 `git status` 返回“not a git repository”。不要假设可以通过Git回退。
- 可能存在用户改动；不要覆盖未知文件。
- 根目录历史崩溃报告保留用于追踪配置竞态。
- 当前README仍引用以下已不存在的路径：
  - `LIUBAI_PERFORMANCE_COMPATIBILITY_TEST_PLAN.md`
  - `docs/COMPATIBILITY_AND_API.md`
- 之前生成过视频脚本文档，但当前工作区已不存在；不要未经用户要求自动恢复。
- `run/mods` 当前包含：
  - `create-1.21.1-6.0.10.jar`
  - `entityculling-neoforge-1.10.5-mc1.21.1.jar`

## 14. 建议优先级

如果用户下一步要求继续改善实际效果，建议按以下顺序：

1. 修复粒子预算：提前拦截、存活总量预算、稳定HUD速率。
2. 改善玩家移动时遮挡缓存：稳定结果宽限、优先队列和移动预算。
3. 拆分“近距离完整质量保护”和“可靠实心墙遮挡”，解决贴墙掉落物重新渲染。
4. 为这些行为增加可重复自动/半自动测试和统计导出。
5. 扩大Create/Flywheel版本矩阵；每个版本必须验证目标类和方法签名。
6. 恢复或重写缺失的兼容/API和性能测试文档，并修复README断链。

不要优先实现通用代理模型、GPU Hi-Z或大规模异步重构，除非用户明确要求；当前更紧迫的是现有策略在真实场景中的稳定性和可测量收益。


# 留白（Liubai）开发记忆

> 最后更新：2026-07-26
> 当前首发候选版本：1.0.1（已构建验证，尚未在本文中确认外部发布）
> 公开发布状态：尚无公开发布版本
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
mod_version=1.0.1
mod_license=MIT
```

- `neo_version` 是开发编译版本。
- `neo_version_range` 是发布包声明的最低兼容范围，二者不要重新合并。
- NeoForge 21.1.115 已实际编译并启动验证。
- 21.1.65 曾尝试验证，但官方 Maven 连接连续重置，未得出兼容结论；不要声明支持 21.1.115 以下版本。
- Create 6.0.10 自身要求 NeoForge 21.1.219+，Ponder 1.0.82 要求 21.1.206+。整合包最终下限可能高于留白下限。

当前本地构建产物：

```text
build/libs/liubai-1.0.1.jar
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
version="1.0.1"
logoFile="logo.png"
```

`TEMPLATE_LICENSE.txt` 是 NeoForge MDK 模板的上游许可证，必须与项目自己的 `LICENSE` 区分并保留。

版本策略（用户于2026-07-24确认）：此前工作区使用过的1.1.x/1.2.x均是未公开的内部开发编号。由于模组尚未发布，正式版本线从1.0.0开始向上迭代。不要因为旧文档、旧对话或构建目录残留而自动把`mod_version`改回1.2.x；后续按语义化版本从1.0.0递增。

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
| `client/StableDensitySelector.java` | 跨相邻帧稳定保留掉落物/经验球密度集合的纯算法组件 |
| `client/RenderStatistics.java` | HUD统计，当前主要是上一帧统计 |
| `client/TemporalLodScheduler.java` | 显式适配器使用的时间更新槽 |
| `client/HookStatus.java` | 记录本会话实际到达过的实体、方块实体、粒子和Flywheel Hook |
| `api/RenderQuality.java` | 显式适配器使用的FULL/REDUCED/MINIMAL质量建议 |
| `visibility/VisibilityService.java` | 内置保守DDA、队列、缓存和确认 |
| `visibility/VisibilityMath.java` | 可回归测试的边界变化和有限范围安全判断 |
| `compat/CompatibilityManager.java` | Entity Culling/Create/Flywheel检测与后端选择 |
| `compat/sable/SableCompatibility.java` | 通过缓存MethodHandle识别Sable动态子世界，避免可选依赖硬引用 |
| `compat/iris/IrisCompatibility.java` | 通过Iris公开API识别阴影Pass，避免复用主摄像机策略 |
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
- 平均时间持续高于目标帧时间的 1.12 倍约400毫秒后逐级升压。
- 平均时间低于目标帧时间的 0.88 倍时，先等待约2秒恢复冷却，再要求约3秒持续低负载才逐级恢复；时长使用真实时间，不再随实际FPS变化。
- 若游戏自身帧率上限低于留白目标，控制器使用两者较低值，避免因不可达到的目标长期误判高压；260视为原版无限制档。
- 压力：`NORMAL -> HIGH -> CRITICAL`
- 关闭自适应模式时固定回到 `NORMAL`。

控制器仍是全局三档压力，不是按粒子、实体、Flywheel分别建模；当前1.0.1包含真实时间迟滞，仍需要真实基地继续观察。

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

### Sable/Aeronautics动态子世界安全模式（当前1.0.1）

已针对当前开发环境确认的版本实现保守兼容：

```text
Sable 2.0.3
Aeronautics 1.3.0
Create 6.0.10
Flywheel 1.0.6
```

- Sable本身已经用稳定UUID、局部Plot、根Pose、`globalBounds` / `lastGlobalBounds`实现动态结构根节点；留白没有也不应重复扫描整座结构或建设第二套根节点。
- `SableCompatibility`只在Sable存在时初始化，通过公开的`ActiveSableCompanion`和`SubLevelContainer`方法建立缓存MethodHandle。Sable未安装时没有运行时硬依赖。
- Sable Plot中的方块实体在获取第三方渲染边界之前就退出留白，不进入DDA、屏幕LOD或留白统计。
- tracking/vehicle/Plot中的实体对主模型、阴影和名称牌保守放行。
- Plot局部坐标的粒子不执行主世界摄像机距离预算。
- 当Aeronautics存在且客户端确有活动SubLevel时，留白不叠加Flywheel限制倍率；Flywheel自己的原始`BandedPrimeLimiter`仍照常工作。没有活动SubLevel时，普通世界Create仍可使用留白限频。
- 原因是`BandedPrimeLimiter#getUpdateDivisor(double)`只暴露距离、不暴露Visual所有者，无法可靠区分普通世界与Sable Visual。当前全局回退只在活动航空学子世界存在期间生效，是稳定性优先的临时边界。
- HUD在安全模式活动时显示实体、方块实体、粒子和Flywheel保护次数/秒。
- 兼容桥状态明确区分`NOT_INSTALLED`、`AVAILABLE`和`DETECTED_BUT_UNAVAILABLE`。若初始化或运行时查询失败，留白进入fail-closed回退：全局停用无法安全归属的实体/方块实体逐对象策略和远距离粒子削减；Aeronautics存在时也不叠加Flywheel倍率。HUD和日志会明确提示。该回退优先保证不误处理Plot对象，可能暂时减少主世界优化收益。

不要把该安全模式描述成结构级LOD、代理模型、GPU遮挡或完整航空学优化。它消除的是留白自身的重复工作和错误坐标判断。

如果HUD中“Flywheel限制增强”始终为0，检查：

1. Create/Flywheel版本是否匹配。
2. Flywheel后端是否为 `flywheel:off`。
3. 压力是否仍为NORMAL。
4. 时间LOD和自适应限制是否开启。
5. 机械是否超过16格。
6. 被测试Visual是否实际使用BandedPrimeLimiter。

## 8. 内置遮挡实现

`VisibilityService` 当前特点：

- 待处理对象最大4096；同一对象再次请求会替换成最新包围盒，旧堆条目会被丢弃。
- 请求按“旧状态为遮挡、摄像机/边界变化、距离”排序，不再使用纯FIFO。
- 默认每帧最多检查32个请求，玩家移动时数量上限可翻倍到64，但同时受默认900微秒客户端主线程预算限制；通常会先命中时间上限，因此32是数量上限而非保证执行数。
- 当前1.0.1把deadline传入对象九射线和每条DDA射线内部，每8个体素检查一次。开始新对象前也会检查截止时间；超时结果不写成UNKNOWN、不覆盖已有遮挡证明，而是保留请求到下一帧。HUD显示本次采样的超时次数。该机制把最坏超出量限制在少量体素查询和固定收尾工作内，并非操作系统级绝对900微秒保证。
- 对包围盒中心和八角附近共9个采样点做Amanatides-Woo体素遍历。
- 只接受 `canOcclude && isSolidRender` 的完整实心方块作为强遮挡物。
- 同一帧复用射线路径中已查询的实心方块结果，不跨帧缓存世界方块状态。
- 任一路径清晰即判定可见；区块未加载或不确定时返回UNKNOWN并正常渲染。
- 默认连续确认2次遮挡才允许跳过。
- 默认遮挡缓存30帧；32格内的遮挡结果最多保留10帧，可见结果最多6帧，未知2帧。
- 当前1.0.0已修复旧实现中“摄像机相对记录位置位移超过约0.5格便让所有记录立即UNKNOWN”的问题。旧行为会使大型基地无法在默认900微秒预算内重检完，造成玩家一移动跳过数量归零和负载突增。
- 当前实现为每次确认遮挡保存9条采样射线命中的完整实心方块。摄像机移动后，若从新位置到9个旧采样点的线段仍分别穿过原遮挡方块内部，则暂时保留OCCLUDED并排入紧急重检；任一路径越过墙边、摄像机进入旧遮挡块，或对象边界变化超过0.25格，立即返回UNKNOWN并正常渲染。
- 移动证明只复用几何关系，不盲目延长缓存TTL，也不跳过后台DDA。方块被拆除后的刷新时限仍服从既有缓存和重检机制。
- 同一稳定对象已有待处理请求时不再每个移动帧重复替换请求和堆条目；只有边界变化或普通请求需要升级为紧急请求时才替换。

当前1.0.1同时包含最新边界替换、风险优先、移动数量预算和射线内部deadline，并解决了“移动必定全部恢复渲染”的策略缺陷；墙角、开门和拆墙场景仍需实机确认没有可见闪烁。

近墙遮挡策略已于2026-07-25调整：`safeDistance`只保护屏幕尺寸LOD和密度预算，不再让实体或方块实体直接跳过内置遮挡。内置DDA仍遵循`occlusionMinDistance`（新配置默认4格），并额外保留最前约2格的硬保护。这样墙后对象可以在安全距离内接受完整实心方块的遮挡确认，而玩家身边对象仍完整渲染。两次遮挡确认、摄像机移动证明、边界失效和DDA deadline延期保持不变。已有配置文件不会自动从12改为4；需要时在界面或文件中手动调整。墙角、开门/拆墙和贴墙转向仍需实机验证，不能宣称已完成运行时稳定性验证。

## 9. 当前渲染策略

### 实体

- 当前1.0.1不再在`RenderFrameEvent.Pre`立即执行DDA。实体或方块实体的第一个已确认主摄像机Hook到达后，系统同步`GameRenderer`当前摄像机位置/旋转并处理本帧队列；实体Hook仍会比较`shouldRender`摄像机参数，额外摄像机不进入留白策略。这样避免策略和DDA继续使用上一帧插值位置。
- 留白决策改在原版/其他模组的 `EntityRenderDispatcher.shouldRender` 返回之后；原版已判定不可见的对象不再进入留白策略或DDA队列。
- 使用 `Entity.getBoundingBoxForCulling()`，并允许API适配器修正范围；非有限或任一边超过256格的范围保守放行。
- 安全距离、重要实体、API强制可见、白名单和禁用命名空间优先保护。
- 屏幕尺寸跳过只在HIGH/CRITICAL、距离超过24格、投影半径连续3帧低于阈值时发生；恢复阈值为隐藏阈值的1.35倍。
- 默认阈值：HIGH约1.25像素，CRITICAL约2像素。
- HIGH/CRITICAL下，32格外的原版掉落物和经验球按每区块稳定数量预算；默认HIGH 24、CRITICAL 12。1.0.1跨帧保留已选UUID并短期清理离开集合的对象，降低依赖遍历顺序造成的闪烁；只跳过渲染，不影响对象逻辑。
- 实体和方块实体类型的注册表ID、白名单与禁用命名空间结果按类型缓存，配置快照变化时统一清空；常见决策结果复用静态实例，减少热路径短命对象。
- 内置遮挡只在有效后端为BUILTIN时运行。

重要实体包括：玩家、末影龙、凋灵、TNT、弹射物、闪电、乘客、载具、发光、着火、准星目标，以及正在攻击玩家的Mob。

### 方块实体

- 决策发生在渲染器自身 `shouldRender` 通过以后。
- 当前1.0.0包含Hook前置门控：只有留白内置BUILTIN方块实体遮挡实际启用时，才调用渲染器的`getRenderBoundingBox`。AUTO+Entity Culling、关闭留白、关闭方块实体剔除等路径不再产生这次额外边界查询。
- 使用NeoForge `BlockEntityRenderer#getRenderBoundingBox` 的真实范围，不再固定为所在方块1×1×1；API可继续修正范围，异常和无限范围保守放行。
- 只在BUILTIN后端使用留白遮挡。
- Entity Culling协作模式直接完整放行给外部模组处理。
- 默认保护信标、末地折跃门、末地传送门、结构方块和潮涌核心。

### Iris/特殊Pass

- 1.0.1新增反射隔离的Iris公开API桥，当前目标API为`IrisApi#getInstance()`与`isRenderingShadowPass()`；Iris未安装时没有硬依赖。
- Iris阴影Pass中的实体、方块实体和原版实体阴影策略保守放行，不复用主摄像机逐对象结果。
- Iris已安装但API缺失，或运行时查询失败时，本会话保守停用这些逐对象Hook策略。该行为优先避免阴影缺失，可能降低主画面优化收益。
- 这不是主画面/阴影/反射独立LOD；Iris共载成功也不代表所有光影包已经行为验证。

### 阴影

- 默认距离24格。
- HIGH约18格，CRITICAL约12格。
- 重要实体保护。

### 名称牌

- 默认距离48格。
- CRITICAL约36格。
- 玩家和其他重要实体保护。

### 粒子

- `ParticleEngine.createParticle` 在Provider调用前预算，避免被拒绝粒子的Provider调用和对象分配；直接调用 `add` 的模组仍有兜底路径。
- 只在HIGH/CRITICAL且超过默认32格时减少；伤害提示、不死图腾、音波、爆炸发射器、闪光和横扫粒子被保护。
- 默认存活粒子软上限4096；超过后远距离采样更严格。
- 默认每渲染帧远距离接收预算：HIGH 256、CRITICAL 128；近距离粒子不受此预算影响。
- HUD改为约1秒窗口速率，并显示存活粒子、远距离接收/秒和拒绝/秒，不再按渲染帧在0与批量数字间跳动。
- HUD使用最近最多300个样本计算P95/P99帧时间，每60个样本更新一次；这用于观察卡顿尾部，不是持久化基准导出。
- 1.0.1把原版粒子存活数量字符串解析降为每8个渲染帧采样一次；HUD开启时的完整可见性记录遍历降为每10帧一次。预算使用最近样本，因此粒子软上限最多存在约8帧响应延迟。
- 尚未在持续高强度粒子源中量化稳态收益；近距离源仍可能填满原版容量，这是为了观感采取的保守选择。
- 当前1.0.0会先识别Sable Plot坐标；动态子世界粒子不会因局部Plot坐标与主世界摄像机相距极远而被错误拒绝。

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
| occlusionMinDistance | 4 |
| checksPerFrame | 32 |
| visibility budget | 900微秒/帧 |
| occludedConfirmations | 2 |
| cacheTtlFrames | 30 |
| minimumProjectedRadius | 1.25 |
| screenSpace | true |
| temporal | true |
| maxTemporalInterval | 8 |
| 高密度原版对象起始距离 | 32 |
| HIGH/CRITICAL每区块对象预算 | 24 / 12 |
| shadowDistance | 24 |
| nameTagDistance | 48 |
| particleDistance | 32 |
| 粒子存活软上限 | 4096 |
| HIGH/CRITICAL远距离粒子预算 | 256 / 128 每渲染帧 |
| Flywheel HIGH倍率 | 2 |
| Flywheel CRITICAL倍率 | 3 |

默认禁用通用实体/方块实体剔除的命名空间：`create`、`flywheel`。这不等于禁用版本锁定的Flywheel适配器。

当前1.0.1为三种列表配置提供NeoForge `newElementSupplier`，通用配置界面会显示新增按钮。列表顺序没有优先级语义，运行时复制为不可变集合。

用户于2026-07-24要求将`checksPerFrame`的新配置默认值从8提高到32，以改善大型基地和移动时的队列追赶速度。已有`liubai-client.toml`会保留原值，不会因代码默认值变化被自动改写；需要在界面或文件中手动调整。900微秒默认硬预算没有改变。

## 11. 已完成验证

### 当前开发版本

- Liubai 1.0.1是尚未公开发布的首发候选；版本线从用户确认的1.0.0向上递增，不要把旧内部1.1.x/1.2.x写成公开历史。
- 2026-07-24执行`gradlew.bat check build`成功；最终`build/libs/liubai-1.0.1.jar`已读取验证：版本1.0.1、MIT、NeoForge`[21.1.115,)`，并包含`logo.png`、`META-INF/LICENSE_liubai`和`META-INF/neoforge.mods.toml`。中英文语言JSON也已解析验证。
- 同日使用`-Pneo_version=21.1.115 compileJava`再次成功，确认本轮新增代码没有引入高于声明最低版本的NeoForge编译API。
- 1.0.1新增DDA射线内部deadline和延期、当前主摄像机延迟同步、Sable fail-closed、Iris阴影Pass保护、稳定密度集合、类型策略缓存、真实时间压力迟滞、粒子/HUD降频采样以及Hook观测状态。
- 2026-07-25将近墙遮挡与`safeDistance`解耦：安全距离内不再启用屏幕尺寸或密度跳过，但从`occlusionMinDistance`起仍可进行内置DDA；新配置默认值为4格，且保留约2格相机近端保护。已补充纯函数回归断言，尚待开发客户端贴墙专项测试。
- 本轮只完成编译、自检、构建和JAR静态验证，尚未启动开发客户端。因此Iris阴影Pass、Sable桥故障模拟、持续移动基地和大型航空学结构仍需运行时专项测试，不得写成已经实机通过。
- 1.0.0修复玩家移动时实体策略被主摄像机快照误判为非主渲染通道，以及内置遮挡记录无条件变UNKNOWN的问题。
- 自检包含线段穿越已确认完整方块的回归断言，覆盖继续遮挡、越过遮挡范围和摄像机进入旧遮挡块三种情况；2026-07-24以`mod_version=1.0.0`重新执行`check build`成功。
- 将新配置的`checksPerFrame`默认值从8提高到32后再次执行`check build`成功，并已用`javap`确认最终`liubai-1.0.0.jar`的`ClientConfig.class`包含默认常量32；JAR元数据仍为1.0.0 / MIT / NeoForge `[21.1.115,)`。
- 当前源代码曾使用`-Pneo_version=21.1.115`编译成功；版本重置只改变发布编号，没有改变该次验证过的代码内容。
- 开发客户端在`BUILTIN`后端下与现有模组组进入世界成功，日志确认兼容桥、Mixin和内置后端初始化，无新增崩溃报告。自动烟雾测试没有模拟持续键盘移动，因此跳过数量稳定性仍需玩家在实际基地复测。
- 开发客户端与Sable 2.0.3、Aeronautics 1.3.0、Create 6.0.10、Flywheel 1.0.6、Entity Culling 1.10.5、Iris、Sodium及车万女仆共载并进入现有世界；日志确认Sable动态子世界兼容桥初始化和留白四个Mixin应用。
- 本次客户端烟雾测试没有自动操作物理装配器，也没有完成大型移动结构的补丁前后P95/P99量化。只能确认代码路径、类加载和共载，不得宣称航空学所有结构场景已完全兼容或卡顿已全部消失。
- 本次Flywheel后端仍回退到`flywheel:off`，所以活动SubLevel下的Flywheel安全回退没有在真实Flywheel渲染后端中验证收益。
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
.\gradlew.bat check
.\gradlew.bat runClient
.\gradlew.bat "-Pneo_version=21.1.115" compileJava
```

快速进入测试世界：

```powershell
.\gradlew.bat runClient -PliubaiQuickPlayWorld=测试世界目录名
```

注意：PowerShell传带多个点的Gradle属性时应加双引号，例如 `"-Pneo_version=21.1.115"`，否则曾被解析成错误任务名。

`runClient` 被终止后，Windows下可能残留Gradle包装进程和游戏Java子进程。只能结束本次启动且命令行明确包含本项目路径/`forgeclient*`的进程，不要杀死所有Java进程。

`src/test/java/com/yelle233/liubai/LiubaiSelfTest.java` 使用Java断言，不依赖外部测试框架；Gradle `check` 自动执行 `liubaiSelfTest`。它覆盖边界位移、仅尺寸变化、无限边界、投影比例、移动遮挡线段证明，以及稳定密度集合的限额、跨帧顺序稳定和离场释放。渲染行为改动仍至少需要完整构建和开发客户端烟雾测试。

## 13. 工作区现状与注意事项

- 当前目录存在可用`.git`仓库，但工作树已有用户改动：`.gitignore`、`DEVELOPMENT_MEMORY.md`被修改，`crash-2026-07-23_17.51.12-client.txt`和`wenti.txt`处于删除状态。本轮不恢复、不覆盖这些无关改动；提交或回退前必须重新核对归属。
- 可能存在用户改动；不要覆盖未知文件。
- 根目录历史崩溃报告保留用于追踪配置竞态。
- `README.md`现为中英双语，并链接到现存的`docs/COMPATIBILITY_AND_API.md`。此前记忆中提到的`留白模组_玩家快速上手.md`和`留白模组_平台投稿介绍.md`当前不在工作区，不要保留指向它们的断链或宣称文件存在。
- 旧README曾链接`LIUBAI_PERFORMANCE_COMPATIBILITY_TEST_PLAN.md`，但该文件当前不存在；链接已移除，不要在记忆中继续声称它存在。
- 之前生成过视频脚本文档，但当前工作区已不存在；不要未经用户要求自动恢复。
- `run/mods` 当前包含：
  - `create-1.21.1-6.0.10.jar`
  - `create-aeronautics-bundled-1.21.1-1.3.0.jar`
  - `entityculling-neoforge-1.10.5-mc1.21.1.jar`
  - `iris-neoforge-1.8.14-beta.1+mc1.21.1.jar`
  - `sable-neoforge-1.21.1-2.0.3.jar`
  - `sodium-neoforge-0.8.12+mc1.21.1.jar`
  - `touhoulittlemaid-1.5.3-neoforge+mc1.21.1.jar`

## 14. 建议优先级

如果用户下一步要求继续改善实际效果，建议按以下顺序：

1. 用首发候选1.0.1在同一基地持续行走和转角，确认“实体/秒跳过”和遮挡缓存不再整体归零，并观察DDA超时计数；随后测试墙角、开门/拆墙，确保旧遮挡证明在视线越过遮挡块时立即失效，并记录P95/P99而非只看平均FPS。
2. 开启实际Iris光影包验证主画面与阴影阶段；当前仅验证Iris共载和进世界。
3. 用同一座Aeronautics结构专项比较留白关闭和首发候选1.0.1：至少覆盖组装瞬间、静止、平移、旋转、面向/背对结构，并记录P95/P99和安全模式计数；共载成功不等于具体渲染行为正确。
4. 在可用Flywheel后端下验证限制器计数与收益；本次烟雾测试后端为off。
5. 在贴墙、转角、开门和拆墙场景验证近墙遮挡改动，重点观察可见闪烁、DDA超时和对象恢复渲染是否及时。
6. 增加统计导出和正式基准；当前HUD已有一秒速率、DDA微秒和P95/P99数据，但没有CSV持久化导出。
7. 扩大Create/Flywheel版本矩阵；每个版本必须验证目标类和方法签名。

不要优先实现通用代理模型、GPU Hi-Z或大规模异步重构，除非用户明确要求；当前更紧迫的是现有策略在真实场景中的稳定性和可测量收益。

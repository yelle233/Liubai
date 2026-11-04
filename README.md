# 留白 / Liubai — NeoForge 26.1.2

[简体中文](#简体中文) | [English](#english)

## 简体中文

### AI 制作声明

[![AI 辅助制作](https://img.shields.io/badge/AI-OpenAI%20Codex%20辅助制作-10A37F?style=for-the-badge&logo=openai&logoColor=white)](#ai-制作声明)

> [!IMPORTANT]
> **AI 制作声明：本项目采用生成式 AI 参与设计、编码、UI 改进、文档编写与测试。** AI 生成或修改的内容由项目维护者审阅、验证并承担最终维护责任。

留白（Liubai）是纯客户端自适应渲染预算模组，主要改善大型基地和大量可见动态对象场景的客户端渲染压力。

留白不会优化服务器 TPS，也不会停止实体 AI、红石、配方、机器逻辑或服务端 Tick。

### 版本与使用

- 当前分支：`26.1.2neoforge`；留白版本：`1.0.0`。
- 运行环境：Minecraft `26.1.2`、NeoForge；需要 Java `25`。当前构建与自动检查使用 NeoForge `26.1.2.109`。
- 仅安装在客户端，不需要安装到服务器；其他模组均非必需前置。
- 配置文件：`config/liubai-client.toml`。默认按 `L` 打开设置，可在按键设置中重新绑定。
- 配置界面和调试 HUD 支持中英文；观察者模式剔除默认关闭。

### 功能

- 自动识别 Entity Culling，并在 `AUTO` 模式暂停留白的重复 DDA 遮挡队列；
- 未安装外部遮挡模组时提供保守的实体与普通方块实体遮挡后端；
- 原版视锥确认后才执行额外策略，减少视野外判断和遮挡队列污染；
- 使用实体原版剔除边界和方块实体渲染器真实边界，异常或无限边界保守放行；
- 基于屏幕投影尺寸、对象密度和目标 FPS 的稳定远距离策略；
- 粒子 Provider 创建前预算、存活数量软上限，以及实体阴影和名称牌预算；
- 有数量与严格微秒截止时间、移动遮挡证明、优先重检和最新边界替换的内置遮挡队列；长射线超时会延期，不覆盖已有安全证明；
- 在渲染状态提取前初始化本帧，在实际主相机 Hook 到达后同步摄像机并处理可见性队列，渲染结束后汇总统计；
- 使用真实时间迟滞的 `NORMAL / HIGH / CRITICAL` 帧预算压力控制器，不再因玩家 FPS 不同而改变恢复时长；
- 安全的时间 LOD 更新槽 API，不停止实体或机器逻辑 Tick；
- 显式渲染质量建议与第三方实体/方块实体边界注册 API；
- Iris 公开 API 阴影 Pass 保护，阴影相机阶段保守放行实体和方块实体；
- 稳定的掉落物/经验球密度集合、类型策略缓存，以及降频采样的粒子数量和 HUD 可见性统计；
- 中英文配置界面、默认按 `L` 打开的可重绑定设置快捷键和详细性能 HUD；HUD 可显示 DDA 超时和本会话实际观测到的 Mixin Hook；
- 未知版本、缺失模组和适配失败时自动回退。

### Entity Culling 协作

默认遮挡模式是 `AUTO`：

- 安装 Entity Culling：通用遮挡交给 Entity Culling，留白不运行 DDA 队列；
- 未安装 Entity Culling：留白使用内置保守遮挡后端；
- 屏幕空间 LOD、次要效果和通用时间 LOD API 在两种情况下都可以继续工作。

可以在配置界面选择 `AUTO / BUILTIN / EXTERNAL / OFF`。不建议同时强制启用两个通用遮挡后端。`EXTERNAL` 当前没有通用外部提供者接口实现，因此不会运行留白的内置 DDA。

Entity Culling 自己跳过的对象不会计入留白 HUD。本分支的 26.1.2 运行时联动仍需实机验证，不沿用旧版 Minecraft 的测试结论。

### 26.1.2 NeoForge 兼容范围

截至 2026-10-23，查询 Create 官方、Create Aeronautics 和 Sable 的 Modrinth 发布列表，未找到 Minecraft 26.1.2 NeoForge 对应版本。因此本分支不包含 Create/Flywheel 限频适配或 Sable/航空学动态子世界安全模式，也不显示其配置和 HUD 项。

Create Fly 有 Minecraft 26.1.2 移植版，但对应文件仅支持 Fabric，不能用于此 NeoForge 分支。来源与后续重新接入条件见[兼容性与 API](docs/COMPATIBILITY_AND_API.md#version-availability-checked-2026-10-23)。其他 Minecraft 分支的兼容代码不受此次清理影响。

### 构建与验证

在项目根目录使用 Java 25 运行：

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs/liubai-1.0.0.jar`。`build` 会执行渲染 Hook 目标与帧时序检查、HUD 颜色与计数回归检查，以及可见性数学和密度选择自检。

本分支已修复 HUD 文字因缺少透明度而不可见、帧初始化晚于渲染状态提取导致 DDA 队列不处理，以及方块实体/阴影仍使用旧版入口的问题。自动检查通过不代表完整的游戏内兼容性验证；实机测试应确认内置 DDA 检测不再始终为 0，并在有效遮挡场景检查跳过数量。HUD 为留白自己的统计，不包含原版或 Entity Culling 已跳过的对象。

### 文档

- [兼容性与 API](docs/COMPATIBILITY_AND_API.md)

---

## English

### AI Assistance Disclosure

[![AI Assisted](https://img.shields.io/badge/AI-OpenAI%20Codex%20Assisted-10A37F?style=for-the-badge&logo=openai&logoColor=white)](#ai-assistance-disclosure)

> [!IMPORTANT]
> **AI assistance disclosure: Generative AI was used in the design, coding, UI improvements, documentation, and testing of this project.** AI-generated or AI-modified content is reviewed and verified by the project maintainer, who retains final responsibility for maintenance.

Liubai is a client-only adaptive rendering-budget mod designed primarily to reduce client-side rendering pressure in large bases and scenes with many visible dynamic objects.

Liubai does not improve server TPS or stop entity AI, redstone, recipes, machine logic, or server ticks.

### Version and usage

- Branch: `26.1.2neoforge`; Liubai version: `1.0.0`.
- Requires Minecraft `26.1.2`, NeoForge, and Java `25`. The current build and automated checks use NeoForge `26.1.2.109`.
- Install on the client only; no server installation or other mods are required.
- Configuration file: `config/liubai-client.toml`. Press `L` by default to open settings; the shortcut is rebindable.
- Chinese and English configuration screens and HUD are included. Spectator-mode culling is disabled by default.

### Features

- Detects Entity Culling automatically and suspends Liubai's duplicate DDA occlusion queue in `AUTO` mode;
- Provides a conservative entity and ordinary block-entity occlusion backend when no external culling mod is installed;
- Applies additional policies only after the vanilla frustum check succeeds, reducing off-screen work and occlusion-queue pollution;
- Uses vanilla entity culling bounds and the real bounds supplied by block-entity renderers, conservatively allowing invalid or infinite bounds;
- Applies stable distance policies based on projected screen size, object density, and the target FPS;
- Budgets particles before the Provider creates them, uses a live-particle soft limit, and budgets entity shadows and name tags;
- Includes a built-in occlusion queue with count and strict microsecond deadlines, camera-movement occlusion proofs, priority rechecks, and latest-bound replacement; long ray checks are deferred without overwriting an existing safe proof;
- Initializes the frame before render-state extraction, synchronizes the camera and processes visibility at the first confirmed main-camera hook, and publishes statistics after rendering;
- Uses a real-time-hysteresis `NORMAL / HIGH / CRITICAL` frame-budget pressure controller whose recovery timing does not vary with the player's FPS;
- Exposes a safe temporal LOD update-slot API without stopping entity or machine logic ticks;
- Exposes explicit rendering-quality hints and registration APIs for third-party entity and block-entity bounds;
- Uses the public Iris API to protect shadow passes, conservatively allowing entities and block entities during the shadow-camera stage;
- Keeps stable density selections for dropped items and experience orbs, caches type policies, and samples particle counts and HUD visibility statistics at reduced frequency;
- Includes Chinese and English configuration screens, a rebindable settings shortcut mapped to `L` by default, and a detailed performance HUD that can show DDA timeouts and Mixin hooks actually observed in the current session;
- Falls back safely when versions are unknown, optional mods are missing, or an integration fails.

### Working with Entity Culling

The default occlusion mode is `AUTO`:

- With Entity Culling installed, general occlusion is delegated to Entity Culling and Liubai does not run its DDA queue;
- Without Entity Culling, Liubai uses its conservative built-in occlusion backend;
- Screen-space LOD, secondary-effect budgets, and the generic temporal LOD API can continue to work in either case.

The configuration screen offers `AUTO / BUILTIN / EXTERNAL / OFF`. Forcing two general occlusion backends at the same time is not recommended. `EXTERNAL` currently has no general external-provider implementation, so Liubai's built-in DDA does not run in that mode.

Objects skipped by Entity Culling are not included in Liubai's HUD counters. Runtime integration on 26.1.2 still requires in-game verification; test results from older Minecraft versions do not apply automatically.

### 26.1.2 NeoForge compatibility scope

As of 2026-10-23, the Modrinth release lists for official Create, Create Aeronautics, and Sable contain no Minecraft 26.1.2 NeoForge releases. This branch therefore omits the Create/Flywheel limiter adapter and Sable/Aeronautics dynamic-subworld safety mode, including their configuration and HUD entries.

Create Fly has a Minecraft 26.1.2 port, but its matching files are Fabric-only and cannot be used on this NeoForge branch. See [availability evidence and re-integration requirements](docs/COMPATIBILITY_AND_API.md#version-availability-checked-2026-10-23). Compatibility code on other Minecraft branches is unchanged.

### Build and verification

Run from the project root with Java 25:

```powershell
.\gradlew.bat build
```

The artifact is `build/libs/liubai-1.0.0.jar`. The build runs render-hook target and frame-order checks, HUD color and counter regression checks, and visibility-math and density-selection self-tests.

This branch fixes invisible zero-alpha HUD text, a frame-start ordering error that prevented DDA queue processing, and obsolete block-entity/shadow hooks. Automated checks do not replace in-game compatibility testing. Verify that built-in DDA checks no longer remain at zero and that skipped counts increase in a valid occlusion scene. HUD counters cover Liubai's own decisions, not objects already skipped by vanilla or Entity Culling.

### Documentation

- [Compatibility and API](docs/COMPATIBILITY_AND_API.md)

## License

MIT. See [LICENSE](LICENSE).

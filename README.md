# 留白 / Liubai

[简体中文](#简体中文) | [English](#english)

## 简体中文

### AI 制作声明

[![AI 辅助制作](https://img.shields.io/badge/AI-OpenAI%20Codex%20辅助制作-10A37F?style=for-the-badge&logo=openai&logoColor=white)](#ai-制作声明)

> [!IMPORTANT]
> **AI 制作声明：本项目采用生成式 AI 参与设计、编码、UI 改进、文档编写与测试。** AI 生成或修改的内容由项目维护者审阅、验证并承担最终维护责任。

留白（Liubai）是纯客户端自适应渲染预算模组，主要改善大型基地和大量可见动态对象场景的客户端渲染压力。

留白不会优化服务器 TPS，也不会停止实体 AI、红石、配方、机器逻辑或服务端 Tick。

### 功能

- 自动识别 Entity Culling，并在 `AUTO` 模式暂停留白的重复 DDA 遮挡队列；
- 未安装外部遮挡模组时提供保守的实体与普通方块实体遮挡后端；
- 原版视锥确认后才执行额外策略，减少视野外判断和遮挡队列污染；
- 使用实体原版剔除边界和 Forge 方块实体真实渲染边界，异常或无限边界保守放行；
- 基于屏幕投影尺寸、对象密度和目标 FPS 的稳定远距离策略；
- 粒子 Provider 创建前预算、存活数量软上限，以及实体阴影和名称牌预算；
- 有数量与严格微秒截止时间、移动遮挡证明、优先重检和最新边界替换的内置遮挡队列；长射线超时会延期，不覆盖已有安全证明；
- 在实际主渲染 Hook 到达后同步当前摄像机，再处理本帧可见性队列，降低移动时使用上一帧插值位置的风险；
- 使用真实时间迟滞的 `NORMAL / HIGH / CRITICAL` 帧预算压力控制器，不再因玩家 FPS 不同而改变恢复时长；
- 安全的时间 LOD 更新槽 API，不停止实体或机器逻辑 Tick；
- 显式渲染质量建议与第三方实体/方块实体边界注册 API；
- Create 6.0.8 / 内嵌 Flywheel 1.0.5 的版本锁定更新限制器适配；
- Oculus 1.8.0 内置 Iris API 的阴影 Pass 保护，阴影相机阶段保守放行实体和方块实体；
- 稳定的掉落物/经验球密度集合、类型策略缓存，以及降频采样的粒子数量和 HUD 可见性统计；
- 中英文配置界面、默认按 `L` 打开的可重绑定设置快捷键和详细性能 HUD；HUD 可显示 DDA 超时和本会话实际观测到的 Mixin Hook；
- 未知版本、缺失模组和适配失败时自动回退。

### Entity Culling 协作

默认遮挡模式是 `AUTO`：

- 安装 Entity Culling：通用遮挡交给 Entity Culling，留白不运行 DDA 队列；
- 未安装 Entity Culling：留白使用内置保守遮挡后端；
- 屏幕空间 LOD、次要效果和 Flywheel 时间 LOD 在两种情况下都可以继续工作。

可以在配置界面选择 `AUTO / BUILTIN / EXTERNAL / OFF`。不建议同时强制启用两个通用遮挡后端。`EXTERNAL` 当前没有通用外部提供者接口实现，因此不会运行留白的内置 DDA。

当前已测试 Entity Culling 1.10.5。Entity Culling 自己跳过的对象不会计入留白 HUD。

### Create、Flywheel 与 Oculus

当前明确支持：

```text
Create 6.0.8
Flywheel 1.0.5（由 Create 内嵌）
```

留白不会删除或重建 Flywheel Instance，而是在高压力下增强 Flywheel 自带的距离更新限制器。近距离 Visual 保持原频率，远距离机械只降低渲染状态更新频率，机器逻辑不受影响。

其他版本会记录兼容状态并保持原 Flywheel 行为，不会因为缺失类阻止客户端启动。

Oculus 1.8.0 存在时，留白通过其内置的 Iris 公开 API 识别阴影 Pass。Sable 与 Create Aeronautics 没有 Forge 1.20.1 对应发布，因此本版本不包含动态子世界桥接。


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

### Features

- Detects Entity Culling automatically and suspends Liubai's duplicate DDA occlusion queue in `AUTO` mode;
- Provides a conservative entity and ordinary block-entity occlusion backend when no external culling mod is installed;
- Applies additional policies only after the vanilla frustum check succeeds, reducing off-screen work and occlusion-queue pollution;
- Uses vanilla entity culling bounds and Forge's real block-entity render bounds, conservatively allowing invalid or infinite bounds;
- Applies stable distance policies based on projected screen size, object density, and the target FPS;
- Budgets particles before the Provider creates them, uses a live-particle soft limit, and budgets entity shadows and name tags;
- Includes a built-in occlusion queue with count and strict microsecond deadlines, camera-movement occlusion proofs, priority rechecks, and latest-bound replacement; long ray checks are deferred without overwriting an existing safe proof;
- Synchronizes the current camera when the first confirmed main-render hook arrives, then processes the frame's visibility queue to reduce the risk of using the previous frame's interpolated camera position while moving;
- Uses a real-time-hysteresis `NORMAL / HIGH / CRITICAL` frame-budget pressure controller whose recovery timing does not vary with the player's FPS;
- Exposes a safe temporal LOD update-slot API without stopping entity or machine logic ticks;
- Exposes explicit rendering-quality hints and registration APIs for third-party entity and block-entity bounds;
- Provides a version-locked update-limiter integration for Create 6.0.8 / bundled Flywheel 1.0.5;
- Uses the Iris API bundled with Oculus 1.8.0 to protect shadow passes, conservatively allowing entities and block entities during the shadow-camera stage;
- Keeps stable density selections for dropped items and experience orbs, caches type policies, and samples particle counts and HUD visibility statistics at reduced frequency;
- Includes Chinese and English configuration screens, a rebindable settings shortcut mapped to `L` by default, and a detailed performance HUD that can show DDA timeouts and Mixin hooks actually observed in the current session;
- Falls back safely when versions are unknown, optional mods are missing, or an integration fails.

### Working with Entity Culling

The default occlusion mode is `AUTO`:

- With Entity Culling installed, general occlusion is delegated to Entity Culling and Liubai does not run its DDA queue;
- Without Entity Culling, Liubai uses its conservative built-in occlusion backend;
- Screen-space LOD, secondary-effect budgets, and Flywheel temporal LOD can continue to work in either case.

The configuration screen offers `AUTO / BUILTIN / EXTERNAL / OFF`. Forcing two general occlusion backends at the same time is not recommended. `EXTERNAL` currently has no general external-provider implementation, so Liubai's built-in DDA does not run in that mode.

Entity Culling 1.10.5 has been tested. Objects skipped by Entity Culling are not included in Liubai's HUD counters.

### Create, Flywheel, and Oculus

Explicitly supported versions:

```text
Create 6.0.8
Flywheel 1.0.5 (bundled by Create)
```

Liubai does not remove or rebuild Flywheel instances. Under high rendering pressure, it strengthens Flywheel's own distance-based update limiter. Nearby visuals retain their original update rate, while distant machinery only updates its rendered state less often; machine logic is unaffected.

Other versions retain Flywheel's original behavior and report their compatibility status. Missing classes do not prevent the client from starting.

With Oculus 1.8.0 installed, Liubai uses its bundled public Iris API to identify shadow passes. Sable and Create Aeronautics have no Forge 1.20.1 release, so this version does not include the dynamic-subworld bridge.

### Documentation

- [Compatibility and API](docs/COMPATIBILITY_AND_API.md)

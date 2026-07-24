# 留白 / Liubai

Liubai 是面向 Minecraft 1.21.1 + NeoForge 21.1.x 的纯客户端自适应渲染预算模组，重点服务大型科技基地和大量可见动态对象场景。首发候选版本为 `1.0.0`，当前构建声明支持 NeoForge `21.1.115` 及以上版本，并使用 `21.1.243` 作为开发验证版本。

它与 Entity Culling 的分工是：

```text
Entity Culling：判断普通对象是否完全不可见
Liubai：决定剩余可见对象值得消耗多少渲染预算
```

## 功能

- 自动识别 Entity Culling，并在 `AUTO` 模式暂停留白的重复 DDA 遮挡队列；
- 未安装外部遮挡模组时提供保守的实体与普通方块实体遮挡后端；
- 原版视锥确认后才执行额外策略，减少视野外判断和遮挡队列污染；
- 使用实体原版剔除边界和方块实体渲染器真实边界，异常或无限边界保守放行；
- 基于屏幕投影尺寸、对象密度和目标 FPS 的稳定远距离策略；
- 粒子 Provider 创建前预算、存活数量软上限，以及实体阴影和名称牌预算；
- 有数量与微秒双重上限、移动遮挡证明、优先重检和最新边界替换的内置遮挡队列；
- `NORMAL / HIGH / CRITICAL` 帧预算压力控制器；
- 安全的时间 LOD 更新槽 API，不停止实体或机器逻辑 Tick；
- 显式渲染质量建议与第三方实体/方块实体边界注册 API；
- Create 6.0.10 / Flywheel 1.0.6 的版本锁定更新限制器适配；
- Sable 2.0.3 / Create Aeronautics 1.3.0 动态子世界安全模式；
- 中英文配置界面、ESC 快捷按钮和详细性能 HUD；
- 未知版本、缺失模组和适配失败时自动回退。

## Entity Culling 协作

默认遮挡模式是 `AUTO`：

- 安装 Entity Culling：通用遮挡交给 Entity Culling，留白不运行 DDA 队列；
- 未安装 Entity Culling：留白使用内置保守遮挡后端；
- 屏幕空间 LOD、次要效果和 Flywheel 时间 LOD 在两种情况下都可以继续工作。

可以在配置界面选择 `AUTO / BUILTIN / EXTERNAL / OFF`。不建议同时强制启用两个通用遮挡后端。

## Create/Flywheel 协作

当前明确支持：

```text
Create 6.0.10
Flywheel 1.0.6
```

留白不会删除或重建 Flywheel Instance，而是在高压力下增强 Flywheel 自带的距离更新限制器。近距离 Visual 保持原频率，远距离机械只降低渲染状态更新频率，机器逻辑不受影响。

其他版本会记录兼容状态并保持原 Flywheel 行为，不会因为缺失类阻止客户端启动。

Sable/Create Aeronautics存在时，留白会保守绕过动态子世界对象的逐对象策略，并保护Plot局部坐标粒子。活动SubLevel存在期间不会叠加无法可靠区分所属Visual的Flywheel倍率。这是避免负优化的安全模式，不是结构级LOD或完整航空学性能接管。

## 构建与运行

使用 Java 21：

```text
gradlew.bat build
gradlew.bat check
gradlew.bat runClient
```

自动进入指定测试世界：

```text
gradlew.bat runClient -PliubaiQuickPlayWorld=测试世界目录名
```

配置文件位于 `config/liubai-client.toml`，开发环境位于 `run/config/liubai-client.toml`。

`check` 会运行无需外部测试框架的可见性边界、移动遮挡证明与投影回归检查。玩家请先阅读 [快速上手](留白模组_玩家快速上手.md)，平台发布文案见 [投稿介绍](留白模组_平台投稿介绍.md)，显式适配方式见 [兼容性与 API](docs/COMPATIBILITY_AND_API.md)。兼容性仍以实际模组版本和客户端烟雾测试为准；未知渲染器默认保守放行。

## 许可证

Liubai 以 [MIT License](LICENSE) 开源。NeoForge MDK 模板文件的上游许可证见 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。

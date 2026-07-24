# 留白 / Liubai

Liubai 是面向 Minecraft 1.21.1 + NeoForge 21.1.x 的纯客户端自适应渲染预算模组，重点服务大型科技基地和大量可见动态对象场景。当前发布包支持 NeoForge `21.1.115` 及以上版本，并使用 `21.1.243` 作为开发验证版本。

它与 Entity Culling 的分工是：

```text
Entity Culling：判断普通对象是否完全不可见
Liubai：决定剩余可见对象值得消耗多少渲染预算
```

## 功能

- 自动识别 Entity Culling，并在 `AUTO` 模式暂停留白的重复 DDA 遮挡队列；
- 未安装外部遮挡模组时提供保守的实体与普通方块实体遮挡后端；
- 基于屏幕投影尺寸和目标 FPS 的自适应远距离策略；
- 实体阴影、名称牌和远距离粒子预算；
- `NORMAL / HIGH / CRITICAL` 帧预算压力控制器；
- 安全的时间 LOD 更新槽 API，不停止实体或机器逻辑 Tick；
- Create 6.0.10 / Flywheel 1.0.6 的版本锁定更新限制器适配；
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

## 构建与运行

使用 Java 21：

```text
gradlew.bat build
gradlew.bat runClient
```

自动进入指定测试世界：

```text
gradlew.bat runClient -PliubaiQuickPlayWorld=测试世界目录名
```

配置文件位于 `config/liubai-client.toml`，开发环境位于 `run/config/liubai-client.toml`。

测试清单见 [LIUBAI_PERFORMANCE_COMPATIBILITY_TEST_PLAN.md](LIUBAI_PERFORMANCE_COMPATIBILITY_TEST_PLAN.md)，适配接口见 [docs/COMPATIBILITY_AND_API.md](docs/COMPATIBILITY_AND_API.md)。

## 许可证

Liubai 以 [MIT License](LICENSE) 开源。NeoForge MDK 模板文件的上游许可证见 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。

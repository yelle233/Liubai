# 留白 / Liubai — Fabric 1.21.11

纯客户端渲染预算优化模组。它通过自适应帧预算、保守遮挡确认、屏幕尺寸/密度策略以及阴影、名称牌和粒子预算，降低可见对象的客户端渲染成本；不影响服务器 Tick 或游戏逻辑。

当前版本：`1.0.0-1.21.11`。运行环境：Minecraft 1.21.11、Fabric Loader、Fabric API、Java 21。

配置文件：`config/liubai-client.json`；默认按键：`L`。配置界面和调试 HUD 支持中英文，并保留原项目的七个配置分组（包括 Create Fly / Flywheel）。

兼容策略：

- Entity Culling：`AUTO` 模式检测到它后停用留白内置 DDA，避免重复遮挡工作。
- Iris：阴影 Pass 中保守放行逐对象策略；公开 API 不可用时 fail-closed。
- Sodium：通过原版渲染 Hook 共存，没有 Sodium 私有 API 依赖。
- Valkyrien Skies 非官方 1.21.11 移植：船体对象绕过普通世界逐对象预算；无法确定粒子归属时采用活动船期间的保守放行。
- Create Fly：对已核验的 `Create Fly 6.0.9-5` 内置 Flywheel 距离更新限制器进行版本锁定联动。高压力时只降低安全距离外 Visual 的客户端更新频率，不停止机械、红石或服务器逻辑；VS 活动船期间保守放行。

Minecraft 1.21.11 Fabric 仍没有对应的 Sable、官方 Create Fabric、独立 Flywheel 或 Create Aeronautics 版本；其中 Create 功能由社区移植 Create Fly 提供，本工程只对上述已核验版本启用联动，其他版本会安全保持原行为。

## License

MIT. See [LICENSE](LICENSE).

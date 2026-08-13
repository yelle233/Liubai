# 留白 / Liubai — Fabric 1.21.1

纯客户端渲染预算优化模组。它通过自适应帧预算、保守遮挡确认、屏幕尺寸/密度策略以及阴影、名称牌和粒子预算，降低可见对象的客户端渲染成本；不影响服务器 Tick 或游戏逻辑。

当前版本：`1.0.0-1.21.1`。运行环境：Minecraft 1.21.1、Fabric Loader、Fabric API、Java 21。

配置文件：`config/liubai-client.json`；默认按键：`L`。配置界面和调试 HUD 支持中英文，并覆盖除 Fabric 不存在功能外与 NeoForge 原项目相同的六个配置分组。当前显式兼容 Entity Culling、Iris 和 Sable；Sodium 无需专用桥即可共存。

## License
MIT. See [LICENSE](LICENSE).

# 留白 / Liubai — Fabric 1.20.1

纯客户端渲染预算优化模组。它通过自适应帧预算、保守遮挡确认、屏幕尺寸/密度策略以及阴影、名称牌和粒子预算，降低可见对象的客户端渲染成本；不影响服务器 Tick 或游戏逻辑。

当前版本：`1.0.1-1.20.1`。运行环境：Minecraft 1.20.1、Fabric Loader、Fabric API、Java 17。

配置文件：`config/liubai-client.json`；默认按键：`L`。配置界面和调试 HUD 支持中英文，并包含常规、剔除、LOD、次要效果、Create/Flywheel、兼容性和调试七个配置分组。观察者模式剔除默认关闭。

## 已核验联动

- Entity Culling `1.10.5`：`AUTO` 模式下由 Entity Culling 负责通用遮挡，留白暂停重复 DDA 队列。
- Iris `1.7.6+1.20.1`：通过公开 API 识别阴影 Pass，阴影相机阶段保守放行逐对象策略。
- Create Fabric `6.0.8.x` / Flywheel `1.0.5.x`：版本锁定增强 Flywheel 自带的远距离更新限制器，只降低 Visual 更新频率，不停止机械或服务端逻辑。
- Valkyrien Skies `2.4.x`（已核验 `2.4.11`）：船体管理的实体与方块实体绕过普通世界策略；无法确定归属的粒子和 Flywheel Hook 在活动船期间保守放行。
- Sodium 无需专用桥即可共存。

没有找到 Sable 或官方 Create Aeronautics 的 Minecraft 1.20.1 Fabric 发布，因此本分支不包含对应动态子世界桥。

更多兼容性说明见 [docs/COMPATIBILITY_AND_API.md](docs/COMPATIBILITY_AND_API.md)。

## License
MIT. See [LICENSE](LICENSE).

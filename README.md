# KltytonUI

用 HTML、CSS 和 JavaScript 编写 Minecraft 界面。

支持屏幕、叠加层、世界内窗口、Canvas/SVG 和音频，提供独立 Rhino 页面运行时及 Vue、McUI 组件。原生渲染接口用于绘制区块网格；地图浏览和玩家指示器由 [XaeroWorldMapEarth](https://github.com/kltyton/XaeroWorldMapEarth) 实现。

## 版本

| 加载器 | Minecraft |
| --- | --- |
| Forge | 1.18.2、1.19.2、1.20.1 |
| Fabric | 1.20.1、1.21.1、26.1（含 26.1.2）、26.2 |
| NeoForge | 1.21.1、26.1（含 26.1.2）、26.2 |

## 开发

共享代码位于 `common/`，各版本独立构建工程位于 `targets/`。例如构建 NeoForge 26.2（Java 25）：

```powershell
cd targets\neoforge-26.2
.\gradlew.bat assemble
```

产物位于该目标的 `build/libs/`。使用方法见 [文档](docs/intro.md)。

基于 [ApricityUI](https://github.com/Tower-of-Sighs/AUI)，由 kltyton 维护，沿用 [LGPL-2.1](LICENSE) 许可证。

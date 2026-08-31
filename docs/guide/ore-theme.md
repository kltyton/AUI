# Ore UI

AUI 只内置一套 Ore UI：基于
[`ShenYuanOR/mcui-oreui`](https://github.com/ShenYuanOR/mcui-oreui) 1.2.2
适配的 AUI 原生主题。源码记录固定在提交
`ec87d29a9516a741e5bd4ac707dcabc704409cb2`。

## 使用

```html
<link rel="stylesheet" href="/apricityui/theme/ore/ore.css">
<body class="ore-theme">
  <div id="app"></div>
  <script src="runtime/vue.aui.js"></script>
  <script src="runtime/mcui-oreui.aui.js"></script>
  <script>
    var app = Vue.createApp({ template: '<mc-button>创建</mc-button>' });
    app.use(McUIVue.default);
    app.mount('#app');
  </script>
</body>
```

- 逻辑目录：`apricityui/theme/ore/`
- 入口：`ore.css`（内部加载 `ore-components.css`）
- 根作用域：`.ore-theme`
- 主题 token：`--ore-*`
- 上游组件 token：`--mc-*`
- 组件文档总览：`apricityui/theme/ore/example.html`
- 客户单文件产品 Demo：仓库根目录 `mcui-oreui-customer-demo.html`

游戏内按 F10 打开资源管理器，双击模组内唯一的组件总览
`apricityui/theme/ore/example.html`。它加载主题目录中的多个运行时文件，覆盖
header、appbar、按钮、panel、表单控件、dropdown、tabs、list、progress、spinner 和
modal，并包含键盘与点击交互；32 个元素集中在单页展示，标题只在页内定位。

发给客户时只需发送一个文件：仓库根目录的 `mcui-oreui-customer-demo.html`。它不会打包进模组资源，
而是单文件产品化前端 Demo
工作台，将全部 32 个 McUI 元素自然组合成导航、业务卡片、表单、列表、状态、弹层和反馈界面，
不是组件文档总览或逐项组件目录。该文件完全内联 CSS、字体、真实 Ore Vue/McUI
运行时、图标和音效，使用独立 Demo app，不依赖 `details/`、`showcase.aui.js` 或
文档 shell。

## 与 mcui-oreui 的关系

上游 npm 包使用 Vue 3 + TypeScript + Vite。AUI 随 Ore UI 分发语法适配的 Vue
3.5.34 全局资源 `runtime/vue.aui.js` 和 mcui 运行时资源
`runtime/mcui-oreui.aui.js`；通过 `app.use(McUIVue.default)` 注册组件。
保留的 Vue 元素仍由该运行时提供；客户 Demo 按产品界面职责组合它们，而不是把它们
逐项作为组件目录展示。

AUI Java 核心只实现通用 ECMAScript、DOM、CSSOM、事件和媒体闭包；不包含组件专用
Java，也不使用 Chromium、MCEF、JCEF、WebView、WebView2 或 WebKit。字体、音效、
CSS 和页面资源仍由 AUI 的资源加载与绘制链处理：

- 保留 OreUI 基础 CSS、mcui 组件 class 和 DOM 结构；
- 将样式统一限制在 `.ore-theme`；
- 字体走 AUI 资源加载器；图标和音效保留在上游运行包内；
- 页面可以直接使用随附的 Vue/mcui 运行时处理组件行为；
- 业务代码仍应以真实的 `example.html` 结构和脚本为准。

模组内的 `example.html` 加载主题目录内的 `.aui.js` 运行时资源，不需要
外部 npm 包或浏览器内核；客户单文件 Demo 只内联 Vue/McUI 运行时并运行专用前端应用，
不复用组件总览或文档 shell。

## 组件结构

### 按钮

```html
<button class="btn middle_btn primary_btn">创建</button>
<button class="btn middle_btn normal_btn">取消</button>
<button class="btn middle_btn error_btn">删除</button>
```

### Panel

```html
<section class="mc-panel mc-panel--bordered">
  <header class="mc-panel__header">
    <div class="mc-panel__title-area">
      <div class="mc-panel__title">标题</div>
      <div class="mc-panel__subtitle">副标题</div>
    </div>
  </header>
  <div class="mc-panel__body">内容</div>
</section>
```

### Progress

```html
<div class="mc-progress mc-progress--success">
  <div class="mc-progress__header">
    <span class="mc-progress__label">加载</span>
    <span class="mc-progress__value">72%</span>
  </div>
  <div class="mc-progress__track">
    <div class="mc-progress__bar" style="width:72%"></div>
  </div>
</div>
```

checkbox、switch、dropdown、tabs、modal 等状态组件必须同时维护对应 ARIA 状态并
提供键盘操作。不要只复制 class 名；以 `example.html` 的完整结构和脚本为准。

## 资源与许可

`source.md` 记录上游版本、适配差异和运行时边界；`license.txt` 保留上游 MIT
许可证。字体、音效和 PNG 控件资源均位于同一主题目录，不依赖网络。
`scripts/ore/refresh-runtime.ps1` 从固定提交重建 Vue/mcui 运行包；
`scripts/ore/refresh-integrity.ps1 -Mode Verify` 校验除清单本身外的全部 Ore
发布资源，根发布脚本会先执行该校验。

## 已知边界

- AUI 的外部图片解码器目前不声明 SVG 支持，因此未批量内置上游完整 SVG 图标库；
  页面可继续使用 AUI 支持的内联基础 `<svg>`。
- 上游 Vue SFC 的响应式状态、`v-model` 和生命周期由随附 Vue/mcui 运行时处理；
  业务状态按 `example.html` 的方式接入。
- 分发前仍应按项目政策审查第三方 OreUI 视觉设计权利；MIT 只覆盖上游提供的代码和
  随附资产许可声明。

# Ore UI

AUI includes one Ore UI: an AUI-native adaptation of
[`ShenYuanOR/mcui-oreui`](https://github.com/ShenYuanOR/mcui-oreui) 1.2.2,
pinned to commit `ec87d29a9516a741e5bd4ac707dcabc704409cb2`.

## Use

```html
<link rel="stylesheet" href="/apricityui/theme/ore/ore.css">
<body class="ore-theme">
  <div id="app"></div>
  <script src="runtime/vue.aui.js"></script>
  <script src="runtime/mcui-oreui.aui.js"></script>
  <script>
    var app = Vue.createApp({ template: '<mc-button>Create</mc-button>' });
    app.use(McUIVue.default);
    app.mount('#app');
  </script>
</body>
```

- Logical directory: `apricityui/theme/ore/`
- Entry: `ore.css` (loads `ore-components.css`)
- Root scope: `.ore-theme`
- Theme tokens: `--ore-*`
- Upstream component tokens: `--mc-*`
- Component documentation overview: `apricityui/theme/ore/example.html`
- Customer single-file product demo: repository-root `mcui-oreui-customer-demo.html`

In game, press F10 and open the mod's single component overview at
`apricityui/theme/ore/example.html`. It loads several runtime files from the theme
directory and exercises headers, appbars, buttons, panels, form controls, dropdowns,
tabs, lists, progress, spinners, modals, keyboard input, and pointer input; its
32 retained elements are shown on one page and headings only navigate within it.

For a customer delivery, send only the repository-root
`mcui-oreui-customer-demo.html`. It is not packaged as a mod resource; it is a
productized single-file frontend demo workbench that naturally combines all 32
McUI elements into navigation, business-card views, forms, lists, state, overlays, and feedback instead
of presenting a component documentation overview or itemized component catalog.
It fully inlines the CSS, fonts, real Ore Vue/McUI runtime, icons, and sounds, runs
a dedicated demo app, and does not depend on `details/`, `showcase.aui.js`, or the
documentation shell.

## Relationship to mcui-oreui

The upstream npm package uses Vue 3, TypeScript, and Vite. Ore UI bundles a
syntax-adapted Vue 3.5.34 global at `runtime/vue.aui.js` and the mcui runtime at
`runtime/mcui-oreui.aui.js`; register the components with
`app.use(McUIVue.default)`. The retained Vue elements are provided by that runtime;
the customer demo composes them by product-surface responsibility rather than
presenting an itemized component catalog.

AUI's Java core implements only the generic ECMAScript, DOM, CSSOM, event, and media
closure. It has no component-specific Java and uses no Chromium, MCEF, JCEF, WebView,
WebView2, or WebKit. Fonts, sounds, CSS, and page resources continue through AUI's
resource-loading and drawing pipeline:

- OreUI base CSS, mcui component classes, and DOM anatomy are preserved.
- All built-in selectors are scoped under `.ore-theme`.
- Fonts use AUI resources; icons and sounds remain embedded in the upstream runtime bundle.
- Component behavior is provided by the bundled Vue/mcui runtime; use the real
  `example.html` as the integration reference.

The mod's `example.html` loads bundled `.aui.js` runtime resources from the theme
directory and does not need an external npm package
or browser engine. The customer single-file demo only inlines the Vue/McUI runtime
and runs its own frontend app; it does not reuse the component overview or docs shell.

## Component structure

### Buttons

```html
<button class="btn middle_btn primary_btn">Create</button>
<button class="btn middle_btn normal_btn">Cancel</button>
<button class="btn middle_btn error_btn">Delete</button>
```

### Panel

```html
<section class="mc-panel mc-panel--bordered">
  <header class="mc-panel__header">
    <div class="mc-panel__title-area">
      <div class="mc-panel__title">Title</div>
      <div class="mc-panel__subtitle">Subtitle</div>
    </div>
  </header>
  <div class="mc-panel__body">Content</div>
</section>
```

### Progress

```html
<div class="mc-progress mc-progress--success">
  <div class="mc-progress__header">
    <span class="mc-progress__label">Loading</span>
    <span class="mc-progress__value">72%</span>
  </div>
  <div class="mc-progress__track">
    <div class="mc-progress__bar" style="width:72%"></div>
  </div>
</div>
```

Stateful checkbox, switch, dropdown, tab, and modal structures must maintain
their ARIA state and keyboard behavior. Use the complete `example.html`
structure and script rather than copying isolated class names.

## Resources and license

`source.md` records the pinned source, adaptation differences, and runtime
boundary. `license.txt` preserves the upstream MIT license. Fonts, sounds, and
PNG control assets are local to the theme and require no network access.
`scripts/ore/refresh-runtime.ps1` rebuilds the Vue/mcui bundles from the pinned
checkout. `scripts/ore/refresh-integrity.ps1 -Mode Verify` validates every Ore
release resource except the manifest itself, and the root publication script
runs that check first.

## Known boundaries

- AUI's external image decoder does not currently claim SVG support, so the
  complete upstream external SVG icon catalog is not bundled. Inline basic SVG
  remains available.
- Vue SFC reactivity, `v-model`, and lifecycle behavior are provided by the bundled
  Vue/mcui runtime; connect business state as shown in `example.html`.
- Distribution should still apply the project's third-party visual-rights
  review; MIT covers the upstream code and its accompanying asset statement.

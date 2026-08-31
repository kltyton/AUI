# ApricityUI Ore UI

This directory is ApricityUI's only built-in Ore UI. Its visual design and
component behavior are adapted from `ShenYuanOR/mcui-oreui` 1.2.2, pinned to commit
`ec87d29a9516a741e5bd4ac707dcabc704409cb2`.

Use the stable logical path and scope:

```html
<link rel="stylesheet" href="/apricityui/theme/ore/ore.css">
<body class="ore-theme">
```

`ore.css` contains the OreUI base surface and imports `ore-components.css`.
The theme also bundles a syntax-adapted Vue 3.5.34 global and the mcui runtime:

```html
<script src="runtime/vue.aui.js"></script>
<script src="runtime/mcui-oreui.aui.js"></script>
<script>
  var app = Vue.createApp({ template: '<mc-button>Create</mc-button>' });
  app.use(McUIVue.default);
  app.mount('#app');
</script>
```

The real complete example is `apricityui/theme/ore/example.html`; it also loads
`runtime/showcase.aui.js`.

For customer review, send the repository-root
`mcui-oreui-customer-demo.html`. It is a self-contained product workbench that
uses all 32 retained elements in one application flow; it is not packaged as a
mod resource and does not depend on this directory's detail pages.

## Runtime boundary

The npm package uses Vue 3 as its authoring source. The bundled syntax-adapted Vue
global is Vue 3.5.34, and `runtime/mcui-oreui.aui.js` contains the mcui runtime.
The 32 retained Vue components remain the behavior source:

`McAppbar`, `McAppbarButton`, `McAppbarIcon`, `McButton`, `McButtonTabs`, `McCard`,
`McCheckbox`, `McConfirm`, `McDrawer`, `McDropdown`, `McFormField`, `McFormattedText`,
`McHeader`, `McIcon`, `McLayout`, `McList`, `McListItem`, `McLoadingMask`, `McModal`,
`McPanel`, `McPopHost`, `McProgress`, `McRadio`, `McRadioGroup`, `McScrollView`,
`McSlider`, `McSpinner`, `McSwitch`, `McTabs`, `McTcode`,
`McTextField`, `McTooltip`.

AUI's Java core implements only the generic ECMAScript, DOM, CSSOM, event, and media
closure. It has no component-specific Java and uses no Chromium, MCEF, JCEF, WebView,
WebView2, or WebKit. CSS, fonts, and page resources continue through AUI's
resource-loading and drawing implementation; runtime icons and short UI sounds are
embedded in the pinned mcui bundle.

Every loader target requires the pure-Java Rhino runtime for page execution.
KubeJS is optional and does not own the built-in Vue runtime.

The theme includes the upstream fonts under the MIT license.

Runtime bundles are rebuilt from the pinned checkout with
`scripts/ore/refresh-runtime.ps1`. `scripts/ore/refresh-integrity.ps1 -Mode Verify`
checks every Ore resource before release; the root publish script runs that
verification automatically.

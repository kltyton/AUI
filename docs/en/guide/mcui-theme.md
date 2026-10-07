# McUI Theme

McUI is a standalone pure-CSS theme following the [built-in theme contract](../../../common/src/main/resources/assets/kltytonui/kltytonui/kltytonui/theme/theme-spec.md). It implements the same shared component classes, state classes, and token names as Ore. To switch a business page, change only the stylesheet and root scope class; keep its DOM and interaction code.

    <link rel="stylesheet" href="/kltytonui/theme/mcui/mcui.css">
    <body class="mcui-theme">

The entry point is kltytonui/theme/mcui/mcui.css, with local fonts. The theme does not require Vue or a browser engine. The stylesheet is MPL-2.0; the bundled McUI fonts retain their MIT attribution in the same directory.

Open kltytonui/theme/mcui/example.html in game for the seven-page pure-CSS showcase. Pages need only the stylesheet above.

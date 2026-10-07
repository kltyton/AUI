package io.github.kltyton.kltytonui.webapi;

import io.github.kltyton.kltytonui.canvas.BrowserImage;
import io.github.kltyton.kltytonui.element.Canvas;
import io.github.kltyton.kltytonui.element.Img;
import io.github.kltyton.kltytonui.element.Input;
import io.github.kltyton.kltytonui.element.Option;
import io.github.kltyton.kltytonui.element.Path;
import io.github.kltyton.kltytonui.element.Select;
import io.github.kltyton.kltytonui.element.Svg;
import io.github.kltyton.kltytonui.element.TextArea;
import io.github.kltyton.kltytonui.event.Event;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.init.Window;
import io.github.kltyton.kltytonui.loader.Loader;
import io.github.kltyton.kltytonui.layout.Box;
import io.github.kltyton.kltytonui.parser.CSS;
import io.github.kltyton.kltytonui.resource.Font;
import io.github.kltyton.kltytonui.script.ecmascript.EcmaEventListener;
import io.github.kltyton.kltytonui.spi.KuiScriptService;
import io.github.kltyton.kltytonui.spi.KuiServices;
import io.github.kltyton.kltytonui.style.Background;
import io.github.kltyton.kltytonui.util.DataUri;
import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class McUiComponentCompatibilityTest {
    private static final String RESOURCE_ROOT =
            "assets/kltytonui/kltytonui/kltytonui/runtime/";
    private KuiScriptService previousScriptService;

    @BeforeEach
    void captureScriptService() {
        previousScriptService = KuiServices.script();
        registerBrowserElements();
    }

    @AfterEach
    void restoreScriptService() {
        KuiServices.setScript(previousScriptService);
    }

    @Test
    void latestPluginRegistersSixtyEightComponentsAndStylesCheckedControl() throws Exception {
        Document document = TestDocumentFactory.createDocument();
        Map<String, Map<String, CSS.Declaration>> css = new LinkedHashMap<>();
        CSS.readCSS(read("mcui/components.css"), css, "runtime/mcui/components.css");
        document.CSSCache.putAll(css);
        document.rebuildSelectorIndex();
        Context context = RhinoTestSupport.enterContext();
        ScriptableObject scope = browserScope(context, document);

        Object result;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            load(context, scope, "mcui/mcui-oreui.kui.js", "mcui/mcui-icons-normal.kui.js");
            result = context.evaluateString(scope, """
                    var root = document.createElement('div'); document.body.appendChild(root);
                    var Root = {render:function(){return Vue.h(McUIVue.McApp,null,{
                      default:function(){return Vue.h(McUIVue.McCheckbox,{modelValue:true,label:'Checked'});}
                    });}};
                    var app = Vue.createApp(Root);
                    app.config.throwUnhandledErrorInProduction = true;
                    app.use(McUIVue.createMcUI({sounds:{enabled:false},
                      icons:{sets:{mc:McUINormalIcons.mcNormalIconSet}}}));
                    app.mount(root);
                    var names = Object.keys(McUIVue).filter(function(name){return /^Mc[A-Z]/.test(name);});
                    names.length + '|' + (typeof McUIVue.McSkinViewer) + '|'
                      + !!root.querySelector('.mc-app') + '|'
                      + !!root.querySelector('.mc-checkbox__mark img');
                    """, "mcui2-checkbox", 1, null);
        }
        try {
            assertEquals("68|undefined|true|true", result);
            Element control = document.querySelector(".mc-checkbox__control");
            Element mark = document.querySelector(".mc-checkbox__mark");
            assertNotNull(control);
            assertNotNull(mark);
            assertEquals("#3c8527", Background.of(control).color);
            assertEquals("1", mark.getComputedStyle().opacity);
            document.querySelector(".mc-checkbox").setHover(true);
            document.flushPendingStyleUpdates();
            String hover = Background.of(control).color;
            assertEquals(0xFF316D20, io.github.kltyton.kltytonui.parser.Color.parse(hover), hover);
        } finally {
            try (Document.ContextScope ignored = Document.withContext(document)) {
                context.evaluateString(scope, "app.unmount();", "mcui2-unmount", 1, null);
            }
        }
    }

    @Test
    void latestUpstreamGalleryMountsAllRetainedComponents() throws Exception {
        Document document = TestDocumentFactory.createDocument();
        document.body.setAttribute("style", "width:1280px;height:720px");
        Context context = RhinoTestSupport.enterContext();
        ScriptableObject scope = browserScope(context, document);

        Object result;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            load(context, scope, "mcui/mcui-oreui.kui.js", "mcui/mcui-icons-normal.kui.js",
                    "mcui/gallery.kui.js");
            result = context.evaluateString(scope, """
                    var root=document.createElement('div');document.body.appendChild(root);
                    var app=Vue.createApp(McUIVisualGallery.default);
                    var errors=[];
                    app.config.errorHandler=function(error,vm,info){
                      var options=vm&&vm.$options;
                      errors.push((options&&(options.name||options.__name)||'unknown')+':'+info+':'+error);
                    };
                    app.use(McUIVue.createMcUI({sounds:{enabled:false},
                      icons:{sets:{mc:McUINormalIcons.mcNormalIconSet}}}));
                    app.mount(root);
                    var count=document.querySelectorAll('[data-gallery-component]').length;
                    var virtual=document.querySelector('.gallery-virtual-row');
                    var virtualText=virtual ? virtual.textContent : '<missing>';
                    var mapped=Array.from({length:3},function(_,i){return 'Chunk '+(i+1)});
                    var panels=document.querySelectorAll('.mc-tabs__panel');
                    var tabsState=panels.length === 2
                      ? panels[0].hasAttribute('hidden')+'/'+panels[1].hasAttribute('hidden')
                        +'/'+panels[0].getAttribute('hidden')
                        +'/'+panels[0].textContent : '<missing>';
                    var firstListItem=root.querySelector('.mc-list__item');
                    var listState=firstListItem
                      ? firstListItem.getAttribute('aria-selected')+'/'
                        +(firstListItem.getAttribute('class').indexOf('mc-list__item--active')>=0)
                      : '<missing>';
                    var proxyList=Vue.ref(['survival']).value;
                    var proxyArrayState=Array.isArray(proxyList)+'/'
                      +(typeof proxyList.includes==='function' ? proxyList.includes('survival') : 'no-includes');
                    app.unmount();
                    count+'|'+errors.slice(0,3).join(';')+'|'+virtualText+'|'+mapped.join(',')+'|'+tabsState+'|'+proxyArrayState+'|'+listState;
                    """, "mcui2-gallery", 1, null);
        }
        assertEquals("68||Chunk 1|Chunk 1,Chunk 2,Chunk 3|false/true/null/Selected: worlds|true/true|true/true", result);
    }

    @Test
    void galleryRadioGroupMatchesBrowserFieldsetHeight() throws Exception {
        try (InputStream font = McUiComponentCompatibilityTest.class.getClassLoader().getResourceAsStream(
                RESOURCE_ROOT + "mcui/fonts/minecraft_seven.otf")) {
            assertTrue(Font.registerFont("Minecraft Seven", font));
        }
        Document document = TestDocumentFactory.createDocument();
        document.body.setAttribute("style", "width:1920px;height:1080px;margin:0");
        Map<String, Map<String, CSS.Declaration>> css = new LinkedHashMap<>();
        CSS.readCSS(read("mcui/components.css"), css, "runtime/mcui/components.css");
        CSS.readCSS(read("mcui/kui-defaults.css"), css, "runtime/mcui/kui-defaults.css");
        CSS.readCSS(read("mcui/gallery.css"), css, "runtime/mcui/gallery.css");
        document.CSSCache.putAll(css);
        document.rebuildSelectorIndex();
        Context context = RhinoTestSupport.enterContext();
        ScriptableObject scope = browserScope(context, document);

        Object heights;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            load(context, scope, "mcui/mcui-oreui.kui.js", "mcui/mcui-icons-normal.kui.js",
                    "mcui/gallery.kui.js");
            heights = context.evaluateString(scope, """
                    var root=document.createElement('div');document.body.appendChild(root);
                    var app=Vue.createApp(McUIVisualGallery.default);
                    app.use(McUIVue.createMcUI({sounds:{enabled:false},
                      icons:{sets:{mc:McUINormalIcons.mcNormalIconSet}}}));
                    app.mount(root);
                    var group=root.querySelector('[data-gallery-component="McRadioGroup"]');
                    var checkbox=root.querySelector('[data-gallery-component="McCheckbox"]');
                    var radio=group.children[2].children[0];
                    var label=radio.children[0].children[0];
                    var heights=[group].concat(Array.from(group.children)).concat([checkbox,
                      radio,radio.children[0],label]).concat(Array.from(label.children))
                      .map(function(el){return Math.round(el.getBoundingClientRect().height*100)/100;})
                      .join('|');
                    heights+='|'+Math.round(root.querySelector('.mc-expansion-panel__header')
                      .getBoundingClientRect().height);
                    app.unmount();
                    heights;
                    """, "mcui2-radio-group-geometry", 1, null);
        }
        assertEquals("60|15|11|28|60|28|28|28|1|24|0|43", heights);
    }

    @Test
    void dialogParagraphUsesBrowserMarginsWithoutChangingHostDefault() throws Exception {
        Document document = TestDocumentFactory.createDocument();
        Map<String, Map<String, CSS.Declaration>> css = new LinkedHashMap<>();
        try (InputStream input = McUiComponentCompatibilityTest.class.getClassLoader()
                .getResourceAsStream("assets/kltytonui/kltytonui/global.css")) {
            assertNotNull(input);
            CSS.readCSS(new String(input.readAllBytes(), StandardCharsets.UTF_8), css, "global.css");
        }
        CSS.readCSS(read("mcui/components.css"), css, "runtime/mcui/components.css");
        CSS.readCSS(read("mcui/kui-defaults.css"), css, "runtime/mcui/kui-defaults.css");
        document.CSSCache.putAll(css);
        document.rebuildSelectorIndex();

        Element ordinaryParagraph = document.createElement("p");
        document.body.appendChild(ordinaryParagraph);
        Element dialog = document.createElement("section");
        dialog.setAttribute("class", "mc-dialog");
        document.body.appendChild(dialog);
        Element body = document.createElement("div");
        body.setAttribute("class", "mc-dialog__body");
        dialog.appendChild(body);
        Element dialogParagraph = document.createElement("p");
        body.appendChild(dialogParagraph);

        assertEquals(8, Box.of(ordinaryParagraph).getMarginTop(), 0.01);
        assertEquals(16, Box.of(dialogParagraph).getMarginTop(), 0.01);
        assertEquals(16, Box.of(dialogParagraph).getMarginBottom(), 0.01);
        assertEquals(dialogParagraph.getBoundingClientRect().height + 64,
                body.getBoundingClientRect().height, 0.01);
    }

    @Test
    void nativeButtonCheckboxAndSwitchEventsReachVue() throws Exception {
        Document document = TestDocumentFactory.createDocument();
        Context context = RhinoTestSupport.enterContext();
        ScriptableObject scope = browserScope(context, document);
        CountDownLatch updated = new CountDownLatch(1);
        ScriptableObject.putProperty(scope, "__auiUpdated",
                RhinoTestSupport.wrap(context, scope, updated), context);

        Object result;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            load(context, scope, "mcui/mcui-oreui.kui.js");
            result = context.evaluateString(scope, """
                    var clicks=0,checked=Vue.ref(false),switched=Vue.ref(false);
                    var root=document.createElement('div');document.body.appendChild(root);
                    var Root={render:function(){return Vue.h(McUIVue.McApp,null,{
                      default:function(){return Vue.h('div',null,[
                        Vue.h(McUIVue.McButton,{onClick:function(){clicks++;}},{default:function(){return 'Click';}}),
                        Vue.h(McUIVue.McCheckbox,{modelValue:checked.value,
                          'onUpdate:modelValue':function(v){checked.value=v;}}),
                        Vue.h(McUIVue.McSwitch,{modelValue:switched.value,
                          'onUpdate:modelValue':function(v){switched.value=v;}})
                      ]);}
                    });}};
                    var app=Vue.createApp(Root);
                    app.config.throwUnhandledErrorInProduction=true;
                    app.use(McUIVue.createMcUI({sounds:{enabled:false}}));app.mount(root);
                    root.querySelector('.mc-button').click();
                    var checkbox=root.querySelector('.mc-checkbox__input');
                    checkbox.focus({preventScroll:true});checkbox.click();
                    var hits=0,prevented=true;
                    function passive(event){hits++;event.preventDefault();prevented=event.defaultPrevented;}
                    checkbox.addEventListener('mcui-options',passive,{passive:true});
                    var event=window.createEvent('mcui-options',false);event.cancelable=true;
                    checkbox.dispatchEvent(event);
                    checkbox.removeEventListener('mcui-options',passive,{passive:true});
                    checkbox.dispatchEvent(window.createEvent('mcui-options',false));
                    root.querySelector('.mc-switch__input').click();
                    var answer=clicks+'|'+checked.value+'|'+switched.value+'|'
                      +(document.activeElement===checkbox)+'|'+hits+'|'+prevented;
                    Vue.nextTick(function(){__auiUpdated.countDown();});answer;
                    """, "mcui2-control-events", 1, null);
        }
        assertEquals("1|true|true|true|1|false", result);
        assertTrue(updated.await(2, TimeUnit.SECONDS));
        assertTrue(document.querySelector(".mc-switch__input").isChecked());
    }

    @Test
    void numberTextFieldFiltersInvalidCharacters() throws Exception {
        Document document = TestDocumentFactory.createDocument();
        Context context = RhinoTestSupport.enterContext();
        ScriptableObject scope = browserScope(context, document);

        Object result;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            load(context, scope, "mcui/mcui-oreui.kui.js");
            result = context.evaluateString(scope, """
                    var value=Vue.ref(''),invalid=0;
                    var root=document.createElement('div');document.body.appendChild(root);
                    var Root={render:function(){return Vue.h(McUIVue.McApp,null,{
                      default:function(){return Vue.h(McUIVue.McTextField,{
                        modelValue:value.value,filter:'number',
                        'onUpdate:modelValue':function(v){value.value=v;},
                        onInvalidInput:function(){invalid++;}
                      });}
                    });}};
                    var app=Vue.createApp(Root);
                    app.config.throwUnhandledErrorInProduction=true;
                    app.use(McUIVue.createMcUI({sounds:{enabled:false}}));app.mount(root);
                    var input=root.querySelector('input.mc-input');
                    input.value='12a3';input.dispatchEvent(window.createEvent('input',true));
                    value.value+'|'+invalid+'|'+input.value;
                    """, "mcui2-number-filter", 1, null);
        }
        assertEquals("123|1|123", result);
    }

    @Test
    void pixelIconCompletesTheBlobImageCanvasPipeline() throws Exception {
        Document document = TestDocumentFactory.createDocument();
        Element root = document.createElement("div");
        document.body.appendChild(root);
        Context context = RhinoTestSupport.enterContext();
        ScriptableObject scope = browserScope(context, document);
        CountDownLatch completed = new CountDownLatch(1);
        ScriptableObject.putProperty(scope, "__auiPixelDone",
                RhinoTestSupport.wrap(context, scope, completed), context);

        try (Document.ContextScope ignored = Document.withContext(document)) {
            load(context, scope, "mcui/mcui-oreui.kui.js");
            context.evaluateString(scope, """
                    var Root={render:function(){return Vue.h(McUIVue.McIcon,{
                      path:'M2 2h20v20H2z',viewBox:'0 0 24 24',pixelSize:24,color:'#35d04f'});}};
                    var app=Vue.createApp(Root);
                    app.config.throwUnhandledErrorInProduction=true;
                    app.use(McUIVue.createMcUI({sounds:{enabled:false}}));app.mount(__auiTestDocument.body.firstChild);
                    (function waitForPixel(){
                      var image=__auiTestDocument.body.firstChild.querySelector('img');
                      if(image&&image.getAttribute('src')){__auiPixelDone.countDown();return;}
                      setTimeout(waitForPixel,10);
                    }());
                    """, "mcui2-pixel-icon", 1, null);
        }
        assertTrue(completed.await(10, TimeUnit.SECONDS), "pixel icon callback did not complete");
        Element image = root.getElementsByTagName("img").stream().findFirst().orElse(null);
        assertNotNull(image);
        assertTrue(image.getAttribute("src").startsWith("data:image/png;base64,"));
        DataUri.Decoded decoded = DataUri.decode(image.getAttribute("src"));
        var raster = ImageIO.read(new ByteArrayInputStream(decoded.bytes()));
        assertNotNull(raster);
        assertEquals(0x35D04F,
                raster.getRGB(raster.getWidth() / 2, raster.getHeight() / 2) & 0x00FFFFFF);
    }

    @Test
    void detachedBrowserImageDecodesPercentEncodedSvgDataUrl() {
        BrowserImage image = new BrowserImage();
        image.setSrc("data:image/svg+xml,%3csvg%20xmlns='http://www.w3.org/2000/svg'%20"
                + "viewBox='0%200%2016%2016'%3e%3cpath%20d='M2%204h12v2H2z'%20fill='%23FFFFFF'/%3e%3c/svg%3e");
        assertEquals(16, image.getNaturalWidth());
        assertEquals(16, image.getNaturalHeight());
    }

    private static void registerBrowserElements() {
        Element.register(Input.TAG_NAME, (owner, tag) -> new Input(owner));
        Element.register(TextArea.TAG_NAME, (owner, tag) -> new TextArea(owner));
        Element.register(Select.TAG_NAME, (owner, tag) -> new Select(owner));
        Element.register(Option.TAG_NAME, (owner, tag) -> new Option(owner));
        Element.register(Img.TAG_NAME, (owner, tag) -> new Img(owner));
        Element.register(Svg.TAG_NAME, (owner, tag) -> new Svg(owner));
        Element.register(Path.TAG_NAME, (owner, tag) -> new Path(owner));
        Element.register(Canvas.TAG_NAME, (owner, tag) -> new Canvas(owner));
    }

    private static void load(Context context, ScriptableObject scope, String... optional) throws Exception {
        context.evaluateString(scope, read("vue.kui.js"), "vue.kui.js", 1, null);
        for (String name : optional) {
            context.evaluateString(scope, read(name), name, 1, null);
        }
    }

    private static ScriptableObject browserScope(Context context, Document document) throws Exception {
        ScriptableObject scope = context.initStandardObjects();
        KuiServices.setScript(new RhinoTestScriptService(context, scope));
        ScriptableObject.putProperty(scope, "__auiTestDocument",
                RhinoTestSupport.wrap(context, scope, document), context);
        ScriptableObject.putProperty(scope, "__auiTestWindow",
                RhinoTestSupport.wrap(context, scope, Window.window), context);
        String bootstrap = Loader.readGlobalJS()
                .replace("let document = KltytonUI.getDocumentByUUID(\"__KUI_DOCUMENT_UUID__\");",
                        "let document = __auiTestDocument;")
                .replace("let window = KltytonUI.getWindow();", "let window = __auiTestWindow;");
        context.evaluateString(scope, bootstrap, "global.js", 1, null);
        return scope;
    }

    private static String read(String name) throws Exception {
        try (InputStream input = McUiComponentCompatibilityTest.class.getClassLoader()
                .getResourceAsStream(RESOURCE_ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record RhinoTestScriptService(Context context, Scriptable scope) implements KuiScriptService {
        @Override
        public void eval(String code, Event event, String source) {
        }

        @Override
        public void reload() {
        }

        @Override
        public Consumer<Object> createCallback(Object callback) {
            if (!(callback instanceof Callable callable)) return null;
            return new EcmaEventListener(callable, scope, context);
        }

        @Override
        public Object wrapHostObject(Object value) {
            return RhinoTestSupport.wrap(context, scope, value);
        }
    }
}

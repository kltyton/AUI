package io.github.kltyton.kltytonui.webapi;

import io.github.kltyton.kltytonui.forge.ScriptService;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Window;
import io.github.kltyton.kltytonui.spi.KuiScriptService;
import io.github.kltyton.kltytonui.spi.KuiServices;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptableObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RhinoConsumerEventListenerTest {
    @Test
    void javascriptFunctionConvertsToConsumerAndCanBeRemovedByIdentity() {
        Context context = Context.enter();
        ScriptableObject scope = context.initStandardObjects();
        Document document = TestDocumentFactory.createDocument();
        Window window = new Window();
        KuiScriptService previousScript = KuiServices.script();
        KuiServices.setScript(ScriptService.INSTANCE);
        ScriptableObject.putProperty(scope, "window", Context.javaToJS(context, window, scope), context);

        Object calls;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            calls = context.evaluateString(scope, """
                var calls = 0;
                var listener = function(event) { calls++; };
                window.addEventListener('custom', listener);
                window.dispatchEvent(window.createEvent('custom', false));
                window.removeEventListener('custom', listener);
                window.dispatchEvent(window.createEvent('custom', false));
                calls;
                    """, "<consumer-event-listener-test>", 1, null);
        } finally {
            KuiServices.setScript(previousScript);
        }

        assertEquals(1.0, ((Number) calls).doubleValue());
    }
}

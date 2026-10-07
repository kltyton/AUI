package io.github.kltyton.kltytonui.neoforge;

import io.github.kltyton.kltytonui.event.Event;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.neoforge.script.rhino.KuiRhinoContextBridge;
import io.github.kltyton.kltytonui.script.StandaloneRhinoRuntime;
import io.github.kltyton.kltytonui.spi.KuiScriptService;
import dev.latvian.mods.rhino.Context;

import java.util.function.Consumer;

/**
 * NeoForge 26.1 script bridge backed by the required standalone Rhino runtime.
 */
public final class ScriptService implements KuiScriptService {
    public static final ScriptService INSTANCE = new ScriptService();

    private ScriptService() {
    }

    @Override
    public void eval(String code, Event event, String source) {
        StandaloneRhinoRuntime.eval(code, event, source);
    }

    @Override
    public void evalGlobal(String code, String documentUuid) {
        StandaloneRhinoRuntime.evalGlobal(code, documentUuid);
    }

    @Override
    public void reload() {
        StandaloneRhinoRuntime.reload();
    }

    @Override
    public void warmUp() {
        StandaloneRhinoRuntime.warmUp();
    }

    @Override
    public Context enterRhinoContext() {
        return KuiRhinoContextBridge.enter();
    }

    @Override
    public void releaseDocument(Document document) {
        StandaloneRhinoRuntime.release(document);
    }

    @Override
    public Object wrapHostObject(Object value) {
        return StandaloneRhinoRuntime.wrapHostValue(value);
    }

    @Override
    public Consumer<Object> createCallback(Object callback) {
        return StandaloneRhinoRuntime.createCallback(callback);
    }
}

package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.event.Event;
import io.github.kltyton.kltytonui.fabric.script.rhino.KuiRhinoContextBridge;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.script.StandaloneRhinoRuntime;
import io.github.kltyton.kltytonui.spi.KuiScriptService;
import dev.latvian.mods.rhino.Context;

import java.util.function.Consumer;

/** Fabric JavaScript bridge backed by the required standalone Rhino runtime. */
public final class FabricScriptService implements KuiScriptService {
    public static final FabricScriptService INSTANCE = new FabricScriptService();
    private FabricScriptService() { }
    public void eval(String code, Event event, String source) { StandaloneRhinoRuntime.eval(code, event, source); }
    public void evalGlobal(String code, String documentUuid) { StandaloneRhinoRuntime.evalGlobal(code, documentUuid); }
    public void reload() { StandaloneRhinoRuntime.reload(); }
    public void warmUp() { StandaloneRhinoRuntime.warmUp(); }
    public Context enterRhinoContext() { return KuiRhinoContextBridge.enter(); }
    public void releaseDocument(Document document) { StandaloneRhinoRuntime.release(document); }
    public Object wrapHostObject(Object value) { return StandaloneRhinoRuntime.wrapHostValue(value); }
    public Consumer<Object> createCallback(Object callback) { return StandaloneRhinoRuntime.createCallback(callback); }
}

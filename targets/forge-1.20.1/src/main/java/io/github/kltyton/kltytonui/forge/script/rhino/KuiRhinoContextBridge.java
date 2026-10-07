package io.github.kltyton.kltytonui.forge.script.rhino;

import io.github.kltyton.kltytonui.script.StandaloneRhinoRuntime;
import io.github.kltyton.kltytonui.script.host.KuiScriptHost;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.Scriptable;

/** Rhino 1.20.1 host wrapper adapter. */
public final class KuiRhinoContextBridge {
    private KuiRhinoContextBridge() {
    }

    public static Context enter() {
        Context context = Context.enter();
        context.addCustomJavaToJsWrapper(KuiScriptHost.class, host ->
                (cx, scope, staticType) -> {
                    Scriptable delegate = new NativeJavaObject(scope, host, host.getClass(), cx);
                    return StandaloneRhinoRuntime.wrapHostObject(host, delegate, scope);
                });
        return context;
    }
}

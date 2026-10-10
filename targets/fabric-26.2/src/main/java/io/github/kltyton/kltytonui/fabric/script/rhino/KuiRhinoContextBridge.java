package io.github.kltyton.kltytonui.fabric.script.rhino;

import io.github.kltyton.kltytonui.script.StandaloneRhinoRuntime;
import io.github.kltyton.kltytonui.script.host.KuiScriptHost;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.type.TypeInfo;

/** Modern Rhino host wrapper adapter. */
public final class KuiRhinoContextBridge {
    private static final Factory FACTORY = new Factory();

    private KuiRhinoContextBridge() {
    }

    public static Context enter() {
        return FACTORY.enter();
    }

    private static final class Factory extends ContextFactory {
        @Override
        protected Context createContext() {
            return new KuiContext(this);
        }
    }

    private static final class KuiContext extends Context {
        private KuiContext(ContextFactory factory) {
            super(factory);
        }

        @Override
        public Scriptable wrapAsJavaObject(Scriptable scope, Object value, TypeInfo staticType) {
            Scriptable delegate = super.wrapAsJavaObject(scope, value, staticType);
            return value instanceof KuiScriptHost host
                    ? StandaloneRhinoRuntime.wrapHostObject(host, delegate, scope)
                    : delegate;
        }
    }
}

package io.github.kltyton.kltytonui.script;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.event.Event;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Window;
import io.github.kltyton.kltytonui.loader.Loader;
import io.github.kltyton.kltytonui.parser.JS;
import io.github.kltyton.kltytonui.script.host.KuiScriptHost;
import io.github.kltyton.kltytonui.script.host.RhinoHostObject;
import io.github.kltyton.kltytonui.spi.KuiServices;
import io.github.kltyton.kltytonui.util.KuiLog;
import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.Script;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;

import java.util.Map;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Loader-independent Rhino runtime used by every supported client target. */
public final class StandaloneRhinoRuntime {
    private static final Object RHINO_CONTEXT_FACTORY = createContextFactory();
    private static final Map<String, RuntimeState> RUNTIMES = new ConcurrentHashMap<>();
    private static final Object GLOBAL_SCRIPT_LOCK = new Object();
    private static final String DOCUMENT_UUID_BINDING = "__auiDocumentUuid";
    private static String cachedGlobalCode;
    private static Script cachedGlobalScript;

    private StandaloneRhinoRuntime() {
    }

    public static void eval(String code, Event event, String source) {
        if (code == null || code.isBlank()) {
            KltytonUI.LOGGER.warn("[KUI JS] empty script skipped source={}", KuiLog.source(source));
            return;
        }
        Document document = Document.getContextDocument();
        if (document == null || document.isDisposed()) {
            KltytonUI.LOGGER.warn("[KUI JS] script skipped without active document source={}", KuiLog.source(source));
            return;
        }
        RuntimeState runtime = runtimeFor(document);
        Context context = enterContext();
        synchronized (runtime) {
            Scriptable scope = null;
            Object previousEvent = null;
            boolean hadEvent = false;
            try {
                ensureScope(context, runtime);
                scope = runtime.scope;
                if (event != null) {
                    previousEvent = scope.get(context, "event", scope);
                    hadEvent = previousEvent != Scriptable.NOT_FOUND;
                    scope.put(context, "event", scope, event);
                }
                Window.window.beginScriptTask();
                context.evaluateString(scope, JS.rewriteForRhino(code), KuiLog.source(source), 1, null);
            } catch (RuntimeException exception) {
                KltytonUI.LOGGER.error(
                        "[KUI JS] script execution failed source={} event={} code={}",
                        KuiLog.source(source), event == null ? "<none>" : event.type,
                        KuiLog.compact(code), exception);
                throw exception;
            } finally {
                if (event != null && scope != null) {
                    if (hadEvent) scope.put(context, "event", scope, previousEvent);
                    else scope.delete(context, "event");
                }
                Window.window.endScriptTask();
            }
        }
    }

    public static void evalGlobal(String code, String documentUuid) {
        if (code == null || code.isBlank()) return;
        Document document = Document.getContextDocument();
        if (document == null || document.isDisposed()) {
            KltytonUI.LOGGER.warn("[KUI JS] global script skipped without active document");
            return;
        }
        RuntimeState runtime = runtimeFor(document);
        Context context = enterContext();
        synchronized (runtime) {
            ensureScope(context, runtime);
            Scriptable scope = runtime.scope;
            Object previousUuid = scope.get(context, DOCUMENT_UUID_BINDING, scope);
            boolean hadUuid = previousUuid != Scriptable.NOT_FOUND;
            scope.put(context, DOCUMENT_UUID_BINDING, scope, documentUuid == null ? "" : documentUuid);
            try {
                Window.window.beginScriptTask();
                compiledGlobalScript(context, code).exec(context, scope);
            } catch (RuntimeException exception) {
                KltytonUI.LOGGER.error(
                        "[KUI JS] global script execution failed document={} code={}",
                        documentUuid, KuiLog.compact(code), exception);
                throw exception;
            } finally {
                if (hadUuid) scope.put(context, DOCUMENT_UUID_BINDING, scope, previousUuid);
                else scope.delete(context, DOCUMENT_UUID_BINDING);
                Window.window.endScriptTask();
            }
        }
    }

    public static void reload() {
        RUNTIMES.clear();
        synchronized (GLOBAL_SCRIPT_LOCK) {
            cachedGlobalCode = null;
            cachedGlobalScript = null;
        }
    }

    public static void release(Document document) {
        if (document != null) RUNTIMES.remove(document.getUuid().toString());
    }

    public static RhinoHostObject wrapHostObject(
            KuiScriptHost host,
            Scriptable delegate,
            Scriptable scope
    ) {
        if (host == null || delegate == null) return null;
        Document document = host instanceof Document hostDocument
                ? hostDocument
                : Document.getContextDocument();
        if (document == null || document.isDisposed()) {
            return new RhinoHostObject(host, delegate, scope);
        }
        RuntimeState runtime = runtimeFor(document);
        synchronized (runtime) {
            RhinoHostObject cached = runtime.hostObjects.get(host);
            if (cached != null) return cached;
            RhinoHostObject wrapped = new RhinoHostObject(host, delegate, scope);
            runtime.hostObjects.put(host, wrapped);
            return wrapped;
        }
    }

    public static Object wrapHostValue(Object value) {
        if (!(value instanceof KuiScriptHost)) return value;
        Document document = value instanceof Document hostDocument
                ? hostDocument
                : Document.getContextDocument();
        if (document == null || document.isDisposed()) return value;
        RuntimeState runtime = runtimeFor(document);
        Context context = enterContext();
        synchronized (runtime) {
            ensureScope(context, runtime);
            return wrapHostValue(context, value, runtime.scope);
        }
    }

    public static void warmUp() {
        Context context = enterContext();
        context.initStandardObjects();
        String globalJs = Loader.readGlobalJS();
        if (globalJs == null || globalJs.isBlank()) {
            context.compileString("void 0;", "<kui-global-warmup>", 1, null);
            return;
        }
        compiledGlobalScript(context, globalJs);
    }

    public static Consumer<Object> createCallback(Object callback) {
        if (!(callback instanceof Callable callable)) return null;
        Document document = Document.getContextDocument();
        if (document == null || document.isDisposed()) return null;
        RuntimeState runtime = runtimeFor(document);
        return value -> {
            if (document.isDisposed() || runtime.generation != document.getRefreshGeneration()) return;
            Context context = enterContext();
            synchronized (runtime) {
                ensureScope(context, runtime);
                Object wrapped = wrapHostValue(context, value, runtime.scope);
                Window.window.beginScriptTask();
                try {
                    callable.call(context, runtime.scope, callbackThis(context, value, runtime.scope), new Object[]{wrapped});
                } finally {
                    Window.window.endScriptTask();
                }
            }
        };
    }

    /**
     * Resolves the receiver for a callback while preserving the runtime scope for ordinary callbacks.
     */
    public static Scriptable callbackThis(Context context, Object value, Scriptable scope) {
        if (!(value instanceof Event event) || event.currentTarget == null) return scope;
        Object wrapped = wrapHostValue(context, event.currentTarget, scope);
        return wrapped instanceof Scriptable scriptable ? scriptable : scope;
    }

    private static Script compiledGlobalScript(Context context, String code) {
        String prepared = JS.rewriteForRhino(code)
                .replace("\"__KUI_DOCUMENT_UUID__\"", DOCUMENT_UUID_BINDING)
                .replace("'__KUI_DOCUMENT_UUID__'", DOCUMENT_UUID_BINDING)
                .replace("__KUI_DOCUMENT_UUID__", DOCUMENT_UUID_BINDING);
        synchronized (GLOBAL_SCRIPT_LOCK) {
            if (cachedGlobalScript == null || !prepared.equals(cachedGlobalCode)) {
                cachedGlobalScript = context.compileString(prepared, "global.js", 1, null);
                cachedGlobalCode = prepared;
            }
            return cachedGlobalScript;
        }
    }

    private static RuntimeState runtimeFor(Document document) {
        String key = document.getUuid().toString();
        RuntimeState runtime = RUNTIMES.computeIfAbsent(key, ignored -> new RuntimeState());
        if (runtime.generation != document.getRefreshGeneration()) {
            synchronized (runtime) {
                if (runtime.generation != document.getRefreshGeneration()) {
                    runtime.scope = null;
                    runtime.hostObjects.clear();
                    runtime.generation = document.getRefreshGeneration();
                }
            }
        }
        return runtime;
    }

    private static void ensureScope(Context context, RuntimeState runtime) {
        if (runtime.scope != null) return;
        ScriptableObject scope = context.initStandardObjects();
        Scriptable kltytonUI = new NativeJavaClass(context, scope, KltytonUI.class);
        ScriptableObject.putProperty(scope, "KltytonUI", kltytonUI, context);
        runtime.scope = scope;
    }

    private static Context enterContext() {
        Context loaderContext = KuiServices.script().enterRhinoContext();
        if (loaderContext != null) return loaderContext;
        if (RHINO_CONTEXT_FACTORY != null) {
            try {
                return (Context) RHINO_CONTEXT_FACTORY.getClass().getMethod("enter")
                        .invoke(RHINO_CONTEXT_FACTORY);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not enter Rhino context", failure);
            }
        }
        try {
            return (Context) Context.class.getMethod("enter").invoke(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not enter Rhino context", failure);
        }
    }

    private static Object createContextFactory() {
        try {
            Class<?> factoryType = Class.forName("dev.latvian.mods.rhino.ContextFactory");
            return factoryType.getConstructor().newInstance();
        } catch (ClassNotFoundException oldRhino) {
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not create Rhino context factory", failure);
        }
    }

    private static Object wrapValue(Context context, Object value, Scriptable scope) {
        try {
            Method instance = Context.class.getMethod("javaToJS", Object.class, Scriptable.class);
            return instance.invoke(context, value, scope);
        } catch (NoSuchMethodException oldRhino) {
            try {
                Method legacy = Context.class.getMethod("javaToJS", Context.class, Object.class, Scriptable.class);
                return legacy.invoke(null, context, value, scope);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not wrap Rhino callback value", failure);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not wrap Rhino callback value", failure);
        }
    }

    private static Object wrapHostValue(Context context, Object value, Scriptable scope) {
        Object wrapped = wrapValue(context, value, scope);
        if (wrapped instanceof RhinoHostObject || !(value instanceof KuiScriptHost host)) return wrapped;
        return wrapped instanceof Scriptable scriptable
                ? wrapHostObject(host, scriptable, scope)
                : wrapped;
    }

    private static final class RuntimeState {
        private long generation = -1L;
        private ScriptableObject scope;
        private final IdentityHashMap<KuiScriptHost, RhinoHostObject> hostObjects = new IdentityHashMap<>();
    }
}

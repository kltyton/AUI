package io.github.kltyton.kltytonui.script;

import io.github.kltyton.kltytonui.KltytonUI;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.rhino.Script;
import dev.latvian.mods.rhino.Scriptable;
import io.github.kltyton.kltytonui.event.Event;
import io.github.kltyton.kltytonui.loader.Loader;
import io.github.kltyton.kltytonui.parser.JS;
import io.github.kltyton.kltytonui.util.KuiLog;
import net.minecraftforge.fml.ModList;

public class KltytonJS {
    private static final Object GLOBAL_SCRIPT_LOCK = new Object();
    private static final String DOCUMENT_UUID_BINDING = "__auiDocumentUuid";
    private static String cachedGlobalCode;
    private static Script cachedGlobalScript;

    public static void eval(String code) {
        eval(code, null, "<global>");
    }

    public static void eval(String code, Event event) {
        eval(code, event, "<inline>");
    }

    public static void eval(String code, Event event, String source) {
        if (!isKubeJsLoaded()) return;
        if (code == null || code.isBlank()) {
            KltytonUI.LOGGER.warn("[KUI JS] empty script skipped source={}", KuiLog.source(source));
            return;
        }

        if (event != null) {
            // Event scripts get a stable source label while preserving the existing event binding.
        }
        code = JS.rewriteForRhino(code);

        var manager = KubeJS.getClientScriptManager();
        var context = manager.context;
        var top = manager.topLevelScope;
        Object previousEvent = null;
        boolean hadEvent = false;
        if (event != null) {
            previousEvent = top.get(context, "event", top);
            hadEvent = previousEvent != Scriptable.NOT_FOUND;
            top.put(context, "event", top, event);
        }
        try {
            context.evaluateString(top, code, KuiLog.source(source), 1, null);
        } catch (RuntimeException exception) {
            KltytonUI.LOGGER.error(
                    "[KUI JS] script execution failed source={} event={} code={}",
                    KuiLog.source(source),
                    event == null ? "<none>" : event.type,
                    KuiLog.compact(code),
                    exception
            );
            throw exception;
        } finally {
            if (event != null) {
                if (hadEvent) {
                    top.put(context, "event", top, previousEvent);
                } else {
                    top.delete(context, "event");
                }
            }
        }
    }

    public static void evalGlobal(String code, String documentUuid) {
        if (!isKubeJsLoaded()) return;
        if (code == null || code.isBlank()) return;

        var manager = KubeJS.getClientScriptManager();
        var context = manager.context;
        var top = manager.topLevelScope;
        Object previousUuid = top.get(context, DOCUMENT_UUID_BINDING, top);
        boolean hadUuid = previousUuid != Scriptable.NOT_FOUND;
        top.put(context, DOCUMENT_UUID_BINDING, top, documentUuid == null ? "" : documentUuid);
        try {
            compiledGlobalScript(context, code).exec(context, top);
        } catch (RuntimeException exception) {
            KltytonUI.LOGGER.error(
                    "[KUI JS] global script execution failed document={} code={}",
                    documentUuid,
                    KuiLog.compact(code),
                    exception
            );
            throw exception;
        } finally {
            if (hadUuid) top.put(context, DOCUMENT_UUID_BINDING, top, previousUuid);
            else top.delete(context, DOCUMENT_UUID_BINDING);
        }
    }

    public static void reload() {
        if (!isKubeJsLoaded()) return;
        clearGlobalScriptCache();
        try {
            KubeJS.PROXY.reloadClientInternal();
        } catch (RuntimeException exception) {
            KltytonUI.LOGGER.error("[KUI JS] KubeJS client script reload failed", exception);
            throw exception;
        }
    }

    public static void warmUp() {
        if (!isKubeJsLoaded()) return;
        var manager = KubeJS.getClientScriptManager();
        String globalJs = Loader.readGlobalJS();
        if (globalJs == null || globalJs.isBlank()) return;
        compiledGlobalScript(manager.context, globalJs);
    }

    private static Script compiledGlobalScript(dev.latvian.mods.rhino.Context context, String code) {
        String prepared = prepareGlobalCode(code);
        synchronized (GLOBAL_SCRIPT_LOCK) {
            if (cachedGlobalScript == null || !prepared.equals(cachedGlobalCode)) {
                cachedGlobalScript = context.compileString(prepared, "global.js", 1, null);
                cachedGlobalCode = prepared;
            }
            return cachedGlobalScript;
        }
    }

    private static String prepareGlobalCode(String code) {
        return JS.rewriteForRhino(code)
                .replace("\"__KUI_DOCUMENT_UUID__\"", DOCUMENT_UUID_BINDING)
                .replace("'__KUI_DOCUMENT_UUID__'", DOCUMENT_UUID_BINDING)
                .replace("__KUI_DOCUMENT_UUID__", DOCUMENT_UUID_BINDING);
    }

    private static void clearGlobalScriptCache() {
        synchronized (GLOBAL_SCRIPT_LOCK) {
            cachedGlobalCode = null;
            cachedGlobalScript = null;
        }
    }

    private static boolean isKubeJsLoaded() {
        try {
            return ModList.get() != null && ModList.get().isLoaded("kubejs");
        } catch (LinkageError | RuntimeException unavailableForgeRuntime) {
            return false;
        }
    }
}

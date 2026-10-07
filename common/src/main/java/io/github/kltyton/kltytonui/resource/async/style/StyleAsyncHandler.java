package io.github.kltyton.kltytonui.resource.async.style;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.task.AbstractAsyncHandler;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.loader.ClientLoader;
import io.github.kltyton.kltytonui.loader.Loader;
import io.github.kltyton.kltytonui.render.FontDrawer;
import io.github.kltyton.kltytonui.parser.CSS;
import io.github.kltyton.kltytonui.parser.ResourceUsageIndex;
import io.github.kltyton.kltytonui.resource.Font;
import io.github.kltyton.kltytonui.resource.async.network.NetworkAsyncHandler;
import io.github.kltyton.kltytonui.layout.Size;
import io.github.kltyton.kltytonui.util.KuiLog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StyleAsyncHandler extends AbstractAsyncHandler<StyleAsyncHandler.ApplyTask> {
    public static final StyleAsyncHandler INSTANCE = new StyleAsyncHandler();

    private static final int MAX_IMPORT_DEPTH = 3;

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?i)@import\\s+(?:url\\s*\\(\\s*)?['\"]?([^'\"\\)\\s;]+)['\"]?\\s*\\)?\\s*;");
    private static final Pattern FONT_FACE_PATTERN = Pattern.compile("(?is)@font-face\\s*\\{(.*?)}");

    private static final Map<UUID, StyleHandle> HANDLES = new ConcurrentHashMap<>();

    private final Object globalCssCacheLock = new Object();
    private volatile GlobalCssCache globalCssCache;
    private final Map<ParsedCssCacheKey, ParsedCss> parsedCssCache = new ConcurrentHashMap<>();
    private final Map<ExternalCssCacheKey, ParsedCss> preparedExternalCss = new ConcurrentHashMap<>();
    private final Set<String> scheduledFontKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, byte[]> preparedLocalFontBytes = new ConcurrentHashMap<>();

    private StyleAsyncHandler() {
        super("style", 256, 3, 1_500_000L, "KltytonUI-StyleWorker");
    }

    public void attach(Document document, String contextPath, List<String> externalStyleSrcs, List<String> inlineStyles) {
        attach(document, contextPath, externalStyleSrcs, inlineStyles, null);
    }

    /**
     * Registers every author stylesheet of a document, in the order the cascade must see them.
     *
     * <p>{@code orderedSources} interleaves {@code <link>} and {@code <style>} in document order,
     * which is what a browser uses for equal-specificity conflicts. When it is absent the caller
     * only supplied the two separated lists, and the legacy order (inline, then external) is kept
     * so existing callers keep working.</p>
     */
    public void attach(Document document, String contextPath, List<String> externalStyleSrcs,
                       List<String> inlineStyles, List<CSS.StylesheetSource> orderedSources) {
        if (document == null) {
            KltytonUI.LOGGER.error("[KUI CSS] cannot attach styles without document path={}", KuiLog.source(contextPath));
            return;
        }
        long generation = currentGeneration();
        StyleHandle handle = new StyleHandle(document.getUuid(), generation);
        StyleHandle old = HANDLES.put(document.getUuid(), handle);
        if (old != null) old.markStale();

        int order = 0;

        ParsedCss parsedGlobalCss = getGlobalCss(generation);
        if (parsedGlobalCss != null) {
            ParsedCss parsed = parsedGlobalCss;
            handle.putCssEntry(order++, new StyleHandle.CssEntry("global.css", parsed.cssText));
            enqueueFontLoads(handle, parsed.fontTasks);
        }

        boolean ordered = false;
        if (orderedSources != null && !orderedSources.isEmpty()) {
            ordered = true;
            for (CSS.StylesheetSource source : orderedSources) {
                if (source == null || source.value() == null || source.value().isBlank()) continue;
                if (source.external()) {
                    order = registerExternalStylesheet(handle, contextPath, source.value(), order, generation);
                } else {
                    ParsedCss parsed = parseCssCached(source.value(), contextPath, generation);
                    handle.putCssEntry(order++, new StyleHandle.CssEntry(contextPath, parsed.cssText));
                    enqueueFontLoads(handle, parsed.fontTasks);
                }
            }
        }

        if (!ordered && inlineStyles != null) {
            for (String inlineCss : inlineStyles) {
                if (inlineCss == null || inlineCss.isBlank()) continue;
                ParsedCss parsed = parseCssCached(inlineCss, contextPath, generation);
                handle.putCssEntry(order++, new StyleHandle.CssEntry(contextPath, parsed.cssText));
                enqueueFontLoads(handle, parsed.fontTasks);
            }
        }

        if (!ordered && externalStyleSrcs != null) {
            for (String src : externalStyleSrcs) {
                if (src == null || src.isBlank()) continue;
                order = registerExternalStylesheet(handle, contextPath, src, order, generation);
            }
        }

        rebuildCssCache(document, handle);
        handle.markReadyIfIdle();
    }

    /** Resolves, loads and registers one external stylesheet at {@code order}; returns the next order. */
    private int registerExternalStylesheet(StyleHandle handle, String contextPath, String src,
                                           int order, long generation) {
        String resolved = Loader.resolve(contextPath, src);
        if (resolved == null || resolved.isBlank()) {
            KltytonUI.LOGGER.error(
                    "[KUI CSS] external stylesheet resolved to an empty path document={} src={}",
                    KuiLog.source(contextPath),
                    src
            );
            return order;
        }
        int currentOrder = order;
        ParsedCss prepared = preparedExternalCss.get(new ExternalCssCacheKey(generation, resolved));
        if (prepared != null) {
            handle.putCssEntry(currentOrder, new StyleHandle.CssEntry(resolved, prepared.cssText));
            enqueueFontLoads(handle, prepared.fontTasks);
            return currentOrder + 1;
        }
        handle.queueTask();
        submitWorker(() -> {
            try {
                String merged = loadCssWithImports(resolved, 0, new HashSet<>());
                ParsedCss parsed = parseCssCached(merged, resolved, generation);
                enqueueApplyTask(new CssTask(handle, currentOrder, resolved, parsed.cssText, parsed.fontTasks));
            } catch (Exception exception) {
                enqueueApplyTask(new FailedTask(handle, resolved, "stylesheet", exception));
            }
        }, rejected -> enqueueApplyTask(new FailedTask(handle, resolved, "stylesheet-worker", rejected)));
        return currentOrder + 1;
    }

    @Override
    protected void applyOnMainThread(ApplyTask task, long currentGeneration) {
        if (task.handle().generation() != currentGeneration) {
            return;
        }
        StyleHandle current = HANDLES.get(task.handle().documentId());
        if (current != task.handle()) {
            return;
        }

        Document document = Document.getByUUID(task.handle().documentId().toString());
        if (document == null) {
            if (task instanceof FontTask fontTask) {
                scheduledFontKeys.remove(fontTask.family + "|" + fontTask.path);
            }
            task.handle().completeTask(true);
            return;
        }

        task.handle().markApplying();
        if (task instanceof CssTask cssTask) {
            try {
                task.handle().putCssEntry(cssTask.order, new StyleHandle.CssEntry(cssTask.contextPath, cssTask.cssText));
                enqueueFontLoads(task.handle(), cssTask.fontTasks);
                rebuildCssCache(document, task.handle());
                document.reapplyStylesFromCache();
                task.handle().completeTask(false);
            } catch (RuntimeException exception) {
                KltytonUI.LOGGER.error(
                        "[KUI CSS] applying stylesheet failed document={} path={}",
                        document.getPath(),
                        cssTask.contextPath,
                        exception
                );
                task.handle().completeTask(true);
                throw exception;
            }
            return;
        }

        if (task instanceof FontTask fontTask) {
            boolean loaded = registerFont(fontTask);
            if (loaded) {
                FontDrawer.clearCache();
                // Font faces live in the process-wide registry. A font requested by
                // one document may finish while another document is laying out with
                // fallback metrics, so every active document must reflow.
                for (Document activeDocument : Document.getAll()) {
                    if (activeDocument != null) activeDocument.invalidateFontMetrics();
                }
            } else {
                scheduledFontKeys.remove(fontTask.family + "|" + fontTask.path);
                KltytonUI.LOGGER.error(
                        "[KUI CSS] web font registration failed document={} family={} path={}",
                        document.getPath(),
                        fontTask.family,
                        fontTask.path
                );
            }
            task.handle().completeTask(!loaded);
            return;
        }

        if (task instanceof FailedTask failedTask) {
            KltytonUI.LOGGER.error(
                    "[KUI CSS] async style task failed document={} kind={} path={}",
                    document.getPath(),
                    failedTask.kind,
                    failedTask.path,
                    failedTask.error
            );
            task.handle().completeTask(true);
        }
    }

    @Override
    protected void onBeforeClear(long nextGeneration) {
        synchronized (globalCssCacheLock) {
            globalCssCache = null;
        }
        parsedCssCache.clear();
        preparedExternalCss.clear();
        scheduledFontKeys.clear();
        preparedLocalFontBytes.clear();
        for (StyleHandle handle : HANDLES.values()) {
            handle.markStale();
        }
        HANDLES.clear();
    }

    /** Reads and parses global.css once for the current resource generation. */
    public void warmUpGlobalCss() {
        getGlobalCss(currentGeneration());
    }

    public void invalidatePreparedStylesheets() {
        synchronized (globalCssCacheLock) {
            globalCssCache = null;
        }
        parsedCssCache.clear();
        preparedExternalCss.clear();
        CSS.clearCompiledStylesheets();
        io.github.kltyton.kltytonui.parser.Selector.clearCompiledCache();
    }

    /** Prepares all synchronous stylesheet work needed by one template. */
    public int warmUpTemplateStyles(String contextPath,
                                    List<String> externalStyleSrcs,
                                    List<String> inlineStyles,
                                    Size viewport) {
        long generation = currentGeneration();
        int warmed = 0;
        ParsedCss global = getGlobalCss(generation);
        if (global != null) {
            warmUpLocalFonts(global.fontTasks);
            CSS.warmUp(global.cssText, "global.css", viewport);
            warmed++;
        }
        if (inlineStyles != null) {
            for (String inlineCss : inlineStyles) {
                if (inlineCss == null || inlineCss.isBlank()) continue;
                ParsedCss parsed = parseCssCached(inlineCss, contextPath, generation);
                warmUpLocalFonts(parsed.fontTasks);
                CSS.warmUp(parsed.cssText, contextPath, viewport);
                warmed++;
            }
        }
        if (externalStyleSrcs != null) {
            for (String src : externalStyleSrcs) {
                if (src == null || src.isBlank()) continue;
                String resolved = Loader.resolve(contextPath, src);
                if (resolved == null || resolved.isBlank() || Loader.isRemotePath(resolved)) continue;
                try {
                    ExternalCssCacheKey key = new ExternalCssCacheKey(generation, resolved);
                    ParsedCss parsed = preparedExternalCss.get(key);
                    if (parsed == null) {
                        String merged = loadCssWithImports(resolved, 0, new HashSet<>());
                        parsed = parseCssCached(merged, resolved, generation);
                        preparedExternalCss.put(key, parsed);
                    }
                    warmUpLocalFonts(parsed.fontTasks);
                    CSS.warmUp(parsed.cssText, resolved, viewport);
                    warmed++;
                } catch (IOException | RuntimeException exception) {
                    KltytonUI.LOGGER.debug(
                            "[KUI CSS] stylesheet warm-up failed; create will load lazily document={} path={}",
                            KuiLog.source(contextPath),
                            resolved,
                            exception
                    );
                }
            }
        }
        return warmed;
    }

    private ParsedCss getGlobalCss(long generation) {
        GlobalCssCache cached = globalCssCache;
        if (cached != null && cached.generation == generation) {
            return cached.parsed;
        }
        synchronized (globalCssCacheLock) {
            cached = globalCssCache;
            if (cached != null && cached.generation == generation) {
                return cached.parsed;
            }
            String globalCss = ClientLoader.readGlobalCSS();
            ParsedCss parsed = globalCss == null || globalCss.isBlank()
                    ? null
                    : parseCssCached(globalCss, "global.css", generation);
            globalCssCache = new GlobalCssCache(generation, parsed);
            return parsed;
        }
    }

    private void rebuildCssCache(Document document, StyleHandle handle) {
        document.CSSCache.clear();
        document.CSSDebugRules.clear();
        int order = 0;
        Size viewport = new Size(
                document.getViewport().layoutWidth(),
                document.getViewport().layoutHeight()
        );
        for (Map.Entry<Integer, StyleHandle.CssEntry> entry : handle.snapshotCssEntries()) {
            StyleHandle.CssEntry cssEntry = entry.getValue();
            order = CSS.readCSS(
                    cssEntry.cssText(),
                    document.CSSCache,
                    document.CSSDebugRules,
                    cssEntry.contextPath(),
                    order,
                    viewport
            );
        }
        document.rebuildSelectorIndex();
    }

    public void handleViewportChange(Document document) {
        if (document == null || document.documentElement == null) return;
        StyleHandle handle = HANDLES.get(document.getUuid());
        if (handle == null || handle.state() == AsyncState.STALE) return;
        rebuildCssCache(document, handle);
        document.reapplyStylesFromCache();
    }

    private boolean registerFont(FontTask fontTask) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(fontTask.bytes)) {
            return Font.registerFont(fontTask.family, stream);
        } catch (IOException exception) {
            KltytonUI.LOGGER.error(
                    "[KUI CSS] failed to close/load web font family={} path={}",
                    fontTask.family,
                    fontTask.path,
                    exception
            );
            return false;
        }
    }

    private void warmUpLocalFonts(List<FontSource> fontSources) {
        if (fontSources == null || fontSources.isEmpty()) return;
        for (FontSource source : fontSources) {
            if (source == null || source.family.isBlank() || source.path.isBlank()
                    || Loader.isRemotePath(source.path)) continue;
            String key = source.family + "|" + source.path;
            if (!scheduledFontKeys.add(key)) continue;
            try {
                byte[] bytes = preparedLocalFontBytes.get(source.path);
                if (bytes == null) {
                    bytes = fetchBytes(source.path);
                    preparedLocalFontBytes.put(source.path, bytes);
                }
                if (!registerFont(new FontTask(null, source.family, source.path, bytes))) {
                    scheduledFontKeys.remove(key);
                }
            } catch (IOException | RuntimeException exception) {
                scheduledFontKeys.remove(key);
                KltytonUI.LOGGER.error(
                        "[KUI CSS] local web font warm-up failed family={} path={}",
                        source.family,
                        source.path,
                        exception
                );
            }
        }
    }

    private void enqueueFontLoads(StyleHandle handle, List<FontSource> fontSources) {
        if (fontSources == null || fontSources.isEmpty()) return;

        for (FontSource source : fontSources) {
            if (source == null || source.family.isBlank() || source.path.isBlank()) continue;
            String key = source.family + "|" + source.path;
            if (!handle.tryReserveFont(key)) continue;
            if (!scheduledFontKeys.add(key)) continue;

            handle.queueTask();
            submitWorker(() -> {
                try {
                    byte[] bytes = fetchBytes(source.path);
                    enqueueApplyTask(new FontTask(handle, source.family, source.path, bytes));
                } catch (Exception exception) {
                    scheduledFontKeys.remove(key);
                    KltytonUI.LOGGER.error(
                            "[KUI CSS] web font resource load failed family={} path={}",
                            source.family,
                            source.path,
                            exception
                    );
                    enqueueApplyTask(new FailedTask(handle, source.path, "font", exception));
                }
            }, rejected -> {
                scheduledFontKeys.remove(key);
                enqueueApplyTask(new FailedTask(handle, source.path, "font-worker", rejected));
            });
        }
    }

    private String loadCssWithImports(String path, int depth, Set<String> visited) throws IOException {
        if (path == null || path.isBlank()) {
            KltytonUI.LOGGER.error("[KUI CSS] @import resolved to an empty path depth={}", depth);
            return "";
        }
        if (depth > MAX_IMPORT_DEPTH) {
            KltytonUI.LOGGER.warn("[KUI CSS] @import depth limit reached path={} depth={}", path, depth);
            return "";
        }
        String normalized = path.trim();
        if (!visited.add(normalized)) {
            KltytonUI.LOGGER.warn("[KUI CSS] cyclic @import ignored path={}", normalized);
            return "";
        }

        byte[] bytes = fetchBytes(normalized);
        String css = new String(bytes, StandardCharsets.UTF_8);
        List<String> imports = extractImports(css);
        String cssWithoutImports = stripImports(css);

        StringBuilder merged = new StringBuilder();
        if (depth < MAX_IMPORT_DEPTH) {
            for (String importPath : imports) {
                String resolved = Loader.resolve(normalized, importPath);
                if (resolved == null || resolved.isBlank()) continue;
                ResourceUsageIndex.recordImport(normalized, resolved);
                try {
                    String imported = loadCssWithImports(resolved, depth + 1, visited);
                    if (!imported.isBlank()) merged.append(imported).append('\n');
                } catch (IOException exception) {
                    KltytonUI.LOGGER.error(
                            "[KUI CSS] imported stylesheet failed parent={} import={}",
                            normalized,
                            resolved,
                            exception
                    );
                }
            }
        }
        merged.append(cssWithoutImports);
        return merged.toString();
    }

    private byte[] fetchBytes(String path) throws IOException {
        if (Loader.isRemotePath(path)) {
            return NetworkAsyncHandler.INSTANCE.fetchBytes(path);
        }
        try (InputStream stream = ClientLoader.getResourceStream(path)) {
            if (stream == null) {
                throw new IOException("stylesheet resource not found: " + path);
            }
            return stream.readAllBytes();
        }
    }

    private ParsedCss parseCss(String css, String contextPath) {
        if (css == null || css.isBlank()) {
            KltytonUI.LOGGER.warn("[KUI CSS] stylesheet is empty path={}", KuiLog.source(contextPath));
            return new ParsedCss("", List.of());
        }
        String clean = COMMENT_PATTERN.matcher(css).replaceAll("");

        Matcher matcher = FONT_FACE_PATTERN.matcher(clean);
        StringBuffer bodyCss = new StringBuffer();
        ArrayList<FontSource> fontSources = new ArrayList<>();
        while (matcher.find()) {
            FontSource source = parseFontFace(matcher.group(1), contextPath);
            if (source != null) fontSources.add(source);
            matcher.appendReplacement(bodyCss, "");
        }
        matcher.appendTail(bodyCss);
        return new ParsedCss(bodyCss.toString(), fontSources);
    }

    private ParsedCss parseCssCached(String css, String contextPath, long generation) {
        ParsedCssCacheKey key = new ParsedCssCacheKey(
                generation,
                contextPath == null ? "" : contextPath,
                css == null ? "" : css
        );
        return parsedCssCache.computeIfAbsent(key, ignored -> parseCss(css, contextPath));
    }

    private FontSource parseFontFace(String rules, String contextPath) {
        if (rules == null || rules.isBlank()) return null;

        HashMap<String, String> values = new HashMap<>();
        for (String pair : rules.split(";")) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) continue;
            values.put(parts[0].trim().toLowerCase(), parts[1].trim());
        }

        String family = cleanQuote(values.get("font-family"));
        String src = values.get("src");
        if (family == null || family.isBlank() || src == null || src.isBlank()) {
            KltytonUI.LOGGER.warn(
                    "[KUI CSS] invalid @font-face declaration path={} family={} src={}",
                    KuiLog.source(contextPath),
                    family,
                    KuiLog.compact(src)
            );
            return null;
        }

        Matcher matcher = CSS.URL_EXTRACTOR.matcher(src);
        if (!matcher.find()) {
            KltytonUI.LOGGER.warn("[KUI CSS] @font-face src has no url() path={} family={}", KuiLog.source(contextPath), family);
            return null;
        }
        String rawPath = cleanQuote(matcher.group(1));
        if (rawPath == null || rawPath.isBlank()) {
            KltytonUI.LOGGER.warn("[KUI CSS] @font-face url() is empty path={} family={}", KuiLog.source(contextPath), family);
            return null;
        }

        String resolvedPath = Loader.resolve(contextPath, rawPath);
        if (resolvedPath == null || resolvedPath.isBlank()) {
            KltytonUI.LOGGER.warn(
                    "[KUI CSS] @font-face path could not be resolved path={} raw={}",
                    KuiLog.source(contextPath),
                    rawPath
            );
            return null;
        }
        return new FontSource(family, resolvedPath);
    }

    private String cleanQuote(String text) {
        if (text == null) return null;
        return text.replace("\"", "").replace("'", "").trim();
    }

    private List<String> extractImports(String css) {
        if (css == null || css.isBlank()) return List.of();
        ArrayList<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(css);
        while (matcher.find()) {
            String path = matcher.group(1);
            if (path == null || path.isBlank()) continue;
            imports.add(path.trim());
        }
        return imports;
    }

    private String stripImports(String css) {
        if (css == null || css.isBlank()) return "";
        return IMPORT_PATTERN.matcher(css).replaceAll("");
    }

    interface ApplyTask {
        StyleHandle handle();
    }

    private record CssTask(
            StyleHandle handle,
            int order,
            String contextPath,
            String cssText,
            List<FontSource> fontTasks
    ) implements ApplyTask {
    }

    private record FontTask(
            StyleHandle handle,
            String family,
            String path,
            byte[] bytes
    ) implements ApplyTask {
    }

    private record FailedTask(StyleHandle handle, String path, String kind, Throwable error) implements ApplyTask {
    }

    private record GlobalCssCache(long generation, ParsedCss parsed) {
    }

    private record ParsedCssCacheKey(long generation, String contextPath, String cssText) {
    }

    private record ExternalCssCacheKey(long generation, String path) {
    }

    private record ParsedCss(String cssText, List<FontSource> fontTasks) {
        private ParsedCss {
            cssText = cssText == null ? "" : cssText;
            fontTasks = fontTasks == null ? List.of() : List.copyOf(fontTasks);
        }
    }

    private record FontSource(String family, String path) {
    }
}

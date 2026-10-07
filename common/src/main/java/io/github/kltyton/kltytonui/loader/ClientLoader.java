package io.github.kltyton.kltytonui.loader;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.dev.DevTools;
import io.github.kltyton.kltytonui.ui.ToastManager;
import io.github.kltyton.kltytonui.task.AbstractAsyncHandler;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.task.FrameTaskScheduler;
import io.github.kltyton.kltytonui.render.FontDrawer;
import io.github.kltyton.kltytonui.render.ImageDrawer;
import io.github.kltyton.kltytonui.resource.Font;
import io.github.kltyton.kltytonui.parser.HTML;
import io.github.kltyton.kltytonui.parser.CSS;
import io.github.kltyton.kltytonui.parser.Selector;
import io.github.kltyton.kltytonui.resource.async.image.ImageAsyncHandler;
import io.github.kltyton.kltytonui.resource.async.network.NetworkAsyncHandler;
import io.github.kltyton.kltytonui.resource.async.style.StyleAsyncHandler;
import io.github.kltyton.kltytonui.spi.KuiServices;
import io.github.kltyton.kltytonui.style.Style;
import io.github.kltyton.kltytonui.style.Text;
import io.github.kltyton.kltytonui.layout.Size;
import io.github.kltyton.kltytonui.viewport.KltytonViewport;
import io.github.kltyton.kltytonui.dom.DocumentRegistry;
import net.minecraft.client.Minecraft;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import io.github.kltyton.kltytonui.world.WorldWindow;

public class ClientLoader extends Loader {
    private static final Object STATIC_RESOURCE_CACHE_LOCK = new Object();
    private static List<StaticResourceEntry> cachedFinalStaticResources = null;
    private static boolean reloadQueued;
    private static boolean reloadRequested;
    public ClientLoader(String extension) {
        super(extension);
    }

    public static void reload() {
        reloadRequested = true;
        if (reloadQueued) return;
        reloadQueued = true;
        String progressToast = ToastManager.show(
                "Reloading...",
                new ToastManager.ToastOptions(0, false, "", "", "", "")
        );

        // Leave one complete UI frame between the progress toast and the synchronous reload.
        FrameTaskScheduler.scheduleAfterFrames(2, deadlineNs -> {
            try {
                do {
                    reloadRequested = false;
                    long beginNs = System.nanoTime();
                    KuiServices.script().reload();
                    reloadResourcesInternal(beginNs);
                } while (reloadRequested);
            } finally {
                ToastManager.dismiss(progressToast);
                reloadQueued = false;
            }
            return true;
        });
    }

    /** Reloads all client resources after the loader's resource manager is ready. */
    public static void reloadResources() {
        reloadResourcesInternal(System.nanoTime());
    }

    private static void reloadResourcesInternal(long beginNs) {
        invalidateStaticResourceCache();
        ensureAsyncHandlersInitialized();
        AbstractAsyncHandler.clearAllAndBumpGeneration();
        CSS.clearCompiledStylesheets();
        Selector.clearCompiledCache();
        DocumentRegistry.resetCreateTimingState();
        ImageDrawer.clearRenderTypeCache();
        FontDrawer.clearCache();
        Font.prepareReload();
        warmUpDocumentInfrastructure();

        long scanStartNs = System.nanoTime();
        HTML.scan();
        long scanCostMs = (System.nanoTime() - scanStartNs) / 1_000_000L;

        long firstCreateWarmStartNs = System.nanoTime();
        int preparedTemplates = HTML.prepareTemplates();
        int preparedStylesheets = 0;
        for (HTML.TemplateResources template : HTML.preparedTemplateResources()) {
            preparedStylesheets += StyleAsyncHandler.INSTANCE.warmUpTemplateStyles(
                    template.path(),
                    template.externalStyleSrcs(),
                    template.inlineStyles(),
                    resolveWarmupViewport(template.path())
            );
        }
        long firstCreateWarmCostMs = (System.nanoTime() - firstCreateWarmStartNs) / 1_000_000L;
        KltytonUI.LOGGER.debug(
                "[KUI Resource] first-create warm-up templates={} stylesheets={} cost={}ms",
                preparedTemplates,
                preparedStylesheets,
                firstCreateWarmCostMs
        );

        long refreshStartNs = System.nanoTime();
        Document.refreshAll();
        WorldWindow.windows.forEach(worldWindow -> worldWindow.document.refresh());
        DevTools.refresh();
        io.github.kltyton.kltytonui.dev.ResourceManager.refresh();
        long refreshCostMs = (System.nanoTime() - refreshStartNs) / 1_000_000L;

        long totalCostMs = (System.nanoTime() - beginNs) / 1_000_000L;
        ToastManager.show(
                "Reload complete: " + totalCostMs + "ms (scan " + scanCostMs + "ms, refresh " + refreshCostMs + "ms)",
                new ToastManager.ToastOptions(4200, true, "", "", "", "")
        );
    }

    private static Size resolveWarmupViewport(String path) {
        try {
            KltytonViewport viewport = KltytonViewport.spec(path)
                    .createState(path)
                    .resolve(Minecraft.getInstance().getWindow());
            return new Size(viewport.layoutWidth(), viewport.layoutHeight());
        } catch (RuntimeException | LinkageError exception) {
            return new Size(1024, 768);
        }
    }

    private static void ensureAsyncHandlersInitialized() {
        ImageAsyncHandler.INSTANCE.id();
        StyleAsyncHandler.INSTANCE.id();
        NetworkAsyncHandler.INSTANCE.id();
    }

    private static void warmUpDocumentInfrastructure() {
        try {
            Style.warmUpMetadata();
        } catch (RuntimeException | LinkageError exception) {
            KltytonUI.LOGGER.warn("[KUI Resource] style metadata warm-up failed", exception);
        }
        try {
            Text.warmUpFontMetrics();
        } catch (RuntimeException | LinkageError exception) {
            KltytonUI.LOGGER.warn("[KUI Resource] font metrics warm-up failed", exception);
        }
        try {
            StyleAsyncHandler.INSTANCE.warmUpGlobalCss();
        } catch (RuntimeException | LinkageError exception) {
            KltytonUI.LOGGER.warn("[KUI Resource] global stylesheet warm-up failed", exception);
        }
        try {
            KuiServices.script().warmUp();
        } catch (RuntimeException | LinkageError exception) {
            KltytonUI.LOGGER.warn("[KUI Resource] script engine warm-up failed", exception);
        }
    }

    public static InputStream getResourceStream(String path) {
        InputStream filesystemStream = Loader.getResourceStream(path);
        if (filesystemStream != null) {
            return filesystemStream;
        }
        if (path == null || path.isEmpty()) return null;
        try {
            Optional<InputStream> resource = KuiServices.resources().openResource("kltytonui/" + path);
            return resource.orElse(null);
        } catch (RuntimeException | LinkageError exception) {
            KltytonUI.LOGGER.warn("[KUI Resource] failed to open resource-pack resource path={}", path, exception);
        }
        return null;
    }

    public static List<StaticResourceEntry> listFinalStaticResources() {
        synchronized (STATIC_RESOURCE_CACHE_LOCK) {
            if (cachedFinalStaticResources != null) return cachedFinalStaticResources;
        }

        LinkedHashMap<String, StaticResourceEntry> merged = new LinkedHashMap<>();
        loadResourcePackEntries(merged);
        loadFilesystemStaticResources(merged);
        List<StaticResourceEntry> entries = merged.values().stream()
                .sorted(Comparator.comparing(StaticResourceEntry::path))
                .toList();
        synchronized (STATIC_RESOURCE_CACHE_LOCK) {
            cachedFinalStaticResources = entries;
        }
        return entries;
    }

    public static void invalidateStaticResourceCache() {
        synchronized (STATIC_RESOURCE_CACHE_LOCK) {
            cachedFinalStaticResources = null;
        }
    }

    public static String readGlobalCSS() {
        try (InputStream stream = getResourceStream("global.css")) {
            if (stream != null) return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            KltytonUI.LOGGER.warn("[KUI Resource] failed to read global.css", exception);
        }
        return null;
    }

    private static void loadResourcePackEntries(Map<String, StaticResourceEntry> merged) {
        Map<String, String> resources = KuiServices.resources().listResourcePaths("kltytonui", "");
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String path = entry.getKey();
            if (path.isBlank()) continue;
            String sourcePack = Loader.safe(entry.getValue());
            merged.put(path, new StaticResourceEntry(
                    path,
                    Loader.extensionOf(path),
                    ResourceLayer.RESOURCE_PACK,
                    "resource-pack",
                    sourcePack,
                    -1L
            ));
        }
    }

    public void loadResources(BiConsumer<String, String> handler) {
        this.handler = handler;
        loadedResourceCount = 0;
        loadFromResourcePack();
        loadFromLocalFolder();
        loadFromDevFolders();
        KltytonUI.LOGGER.debug("[KUI Resource] scanned extension={} loaded={}", extension, loadedResourceCount);
    }

    private void loadFromResourcePack() {
        Map<String, String> paths = KuiServices.resources().listResourcePaths("kltytonui", "." + extension);

        for (String path : paths.keySet()) {
            try (InputStream stream = KuiServices.resources().openResource("kltytonui/" + path).orElse(null)) {
                if (stream == null) continue;
                handler.accept(path, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                loadedResourceCount++;
            } catch (IOException exception) {
                KltytonUI.LOGGER.error(
                        "[KUI Resource] failed to read resource-pack {} path={}",
                        extension,
                        path,
                        exception
                );
            }
        }
    }
}

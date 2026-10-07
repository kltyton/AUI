package io.github.kltyton.kltytonui.dev.resource;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.loader.ClientLoader;
import io.github.kltyton.kltytonui.loader.Loader;
import io.github.kltyton.kltytonui.resource.Font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Loads font resources under a stable family name shared by cards and previews. */
public final class ResourceFontAsset {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("ttf", "otf");
    private static final String FAMILY_PREFIX = "kui-resource-font-";

    private ResourceFontAsset() {
    }

    public static boolean isFont(Loader.StaticResourceEntry entry) {
        return entry != null && SUPPORTED_EXTENSIONS.contains(safe(entry.extension()).toLowerCase(Locale.ROOT));
    }

    public static String familyName(Loader.StaticResourceEntry entry) {
        String path = entry == null ? "" : safe(entry.path());
        UUID id = UUID.nameUUIDFromBytes(path.getBytes(StandardCharsets.UTF_8));
        return FAMILY_PREFIX + id;
    }

    public static boolean ensureLoaded(Loader.StaticResourceEntry entry) {
        if (!isFont(entry)) return false;
        String family = familyName(entry);
        if (Font.isRegistered(family)) return true;
        String path = safe(entry.path());
        try (InputStream stream = ClientLoader.getResourceStream(path)) {
            if (stream == null) {
                KltytonUI.LOGGER.warn("[KUI Font] preview font resource is missing path={}", path);
                return false;
            }
            boolean loaded = Font.registerFont(family, stream);
            if (!loaded) {
                KltytonUI.LOGGER.error("[KUI Font] preview font registration failed family={} path={}", family, path);
            }
            return loaded;
        } catch (IOException exception) {
            KltytonUI.LOGGER.error("[KUI Font] preview font read failed family={} path={}", family, path, exception);
            return false;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

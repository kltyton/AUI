package io.github.kltyton.kltytonui.forge;

import io.github.kltyton.kltytonui.dom.ForgeDocumentExpander;
import io.github.kltyton.kltytonui.spi.KuiServices;

/**
 * Registers the loader-side service implementations.
 *
 * <p>Loaded lazily by {@link KuiServices} on first access, so headless test JVMs
 * that have this class on the classpath receive the real implementations while
 * environments without the loader fall back to the safe defaults.</p>
 */
public final class KuiServicesBootstrap {
    static {
        KuiServices.setNetwork(NetworkService.INSTANCE);
        KuiServices.setExpander(new ForgeDocumentExpander());
        KuiServices.setConfig(ConfigService.INSTANCE);
        KuiServices.setScript(ScriptService.INSTANCE);
    }

    private KuiServicesBootstrap() {
    }

    /**
     * Explicit trigger from the mod entry point. Referencing this method forces
     * the static initializer above to run, registering the real services.
     */
    public static void init() {
    }
}

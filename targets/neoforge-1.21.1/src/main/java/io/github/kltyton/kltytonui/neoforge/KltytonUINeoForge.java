package io.github.kltyton.kltytonui.neoforge;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.config.KltytonUIConfig;
import io.github.kltyton.kltytonui.registry.KltytonMenus;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.network.NetworkPlatform;
import io.github.kltyton.kltytonui.script.KubeJS;
import io.github.kltyton.kltytonui.util.KuiLogging;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * NeoForge 1.21.1 entry point.
 *
 * <p>This is the loader-specific bootstrap for the 1.21.1 target. The loader
 * service wiring (config, registry scan, network, render backend) is registered
 * through {@code KuiServicesBootstrap}; the concrete NeoForge implementations
 * of each SPI live in this package.</p>
 */
@Mod(KltytonUI.MODID)
public final class KltytonUINeoForge {
    public KltytonUINeoForge(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        KuiLogging.installFileAppender();
        // Register the loader SPI implementations before any common code touches
        // them; otherwise KuiServices falls back to its headless defaults.
        KuiServicesBootstrap.init();
        NetworkPlatform.setCurrentServerSupplier(net.neoforged.neoforge.server.ServerLifecycleHooks::getCurrentServer);
        if (dist == Dist.CLIENT) {
            ClientServicesBootstrap.init(modEventBus);
        }
        // Element/container scanning is a loader-service concern (see
        // ReflectionUtils); menus must be bound to the mod event bus before any
        // client code touches KLTYTONUI_CONTAINER, or the holder stays unbound.
        KltytonUIRegistry.scanPackages("io.github.kltyton.kltytonui.element", "io.github.kltyton.kltytonui.element");
        if (ModList.get().isLoaded("kubejs")) {
            KubeJS.scanPackage("io.github.kltyton.kltytonui.util.kjs");
            KubeJS.scanPackage("io.github.kltyton.kltytonui.container.filter");
        }
        KltytonMenus.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.CLIENT, KltytonUIConfig.CLIENT_SPEC,
                "%s_config.toml".formatted(KltytonUI.MODID));
        modEventBus.addListener(this::onConfigReload);

        if (dist == Dist.CLIENT) {
            KltytonUIRegistry.register();
        }
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != KltytonUIConfig.CLIENT_SPEC) return;
        KltytonUIConfig.markClientReloadPending();
    }
}

package io.github.kltyton.kltytonui.neoforge;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.config.KltytonUIConfig;
import io.github.kltyton.kltytonui.registry.KltytonMenus;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.network.NetworkPlatform;
import io.github.kltyton.kltytonui.util.KuiLogging;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/** NeoForge 26.2 loader entry point. */
@Mod(KltytonUI.MODID)
public final class KltytonUINeoForge {
    public KltytonUINeoForge(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        KuiLogging.installFileAppender();
        KuiServicesBootstrap.init();
        NetworkPlatform.setCurrentServerSupplier(net.neoforged.neoforge.server.ServerLifecycleHooks::getCurrentServer);
        if (dist == Dist.CLIENT) {
            ClientServicesBootstrap.init(modEventBus);
        }

        KltytonUIRegistry.scanPackages("io.github.kltyton.kltytonui.element", "io.github.kltyton.kltytonui.element");
        KltytonMenus.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.CLIENT, KltytonUIConfig.CLIENT_SPEC,
                "%s_config.toml".formatted(KltytonUI.MODID));
        modEventBus.addListener(this::onConfigReload);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != KltytonUIConfig.CLIENT_SPEC) return;
        KltytonUIConfig.markClientReloadPending();
    }
}

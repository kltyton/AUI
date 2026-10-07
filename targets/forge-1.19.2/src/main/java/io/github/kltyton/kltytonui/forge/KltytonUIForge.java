package io.github.kltyton.kltytonui.forge;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.config.KltytonUIConfig;
import io.github.kltyton.kltytonui.network.api.NetworkAutoRegistration;
import io.github.kltyton.kltytonui.network.NetworkPlatform;
import io.github.kltyton.kltytonui.network.forge.NetworkManagerImpl;
import io.github.kltyton.kltytonui.registry.KltytonMenus;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.script.KubeJS;
import io.github.kltyton.kltytonui.util.KuiLogging;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;


/**
 * Forge entry point. The mod loading wiring (config registration, KubeJS
 * package scan, menu/network registration, service bootstrap) lives here in the
 * loader target; {@link KltytonUI} in {@code common} is the loader-neutral API.
 */
@Mod(KltytonUI.MODID)
public class KltytonUIForge {
    public KltytonUIForge() {
        KuiLogging.installFileAppender();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        KuiServicesBootstrap.init();
        NetworkPlatform.setCurrentServerSupplier(net.minecraftforge.server.ServerLifecycleHooks::getCurrentServer);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientServicesBootstrap.init(modEventBus);
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KltytonUIConfig.CLIENT_SPEC);
        modEventBus.addListener(this::onConfigReload);
        if (ModList.get().isLoaded("kubejs")) {
            KubeJS.scanPackage("io.github.kltyton.kltytonui.util.kjs");
            KubeJS.scanPackage("io.github.kltyton.kltytonui.container.filter");
        }
        KltytonUIRegistry.scanPackages("io.github.kltyton.kltytonui.element", "io.github.kltyton.kltytonui.element");
        KltytonMenus.register(modEventBus);
        NetworkManagerImpl.installAutoRegistrationHook();
        NetworkAutoRegistration.findAllAnnotatedPackets();

    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != KltytonUIConfig.CLIENT_SPEC) return;
        KltytonUIConfig.markClientReloadPending();
    }
}

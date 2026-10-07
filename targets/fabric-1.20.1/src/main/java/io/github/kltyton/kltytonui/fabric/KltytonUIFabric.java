package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.registry.KltytonMenus;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.network.api.NetworkAutoRegistration;
import io.github.kltyton.kltytonui.network.NetworkPlatform;
import io.github.kltyton.kltytonui.network.fabric.NetworkManagerImpl;
import io.github.kltyton.kltytonui.util.KuiLogging;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicReference;

public final class KltytonUIFabric implements ModInitializer {
    public void onInitialize() {
        KuiLogging.installFileAppender();
        AtomicReference<MinecraftServer> server = new AtomicReference<>();
        ServerLifecycleEvents.SERVER_STARTING.register(server::set);
        ServerLifecycleEvents.SERVER_STOPPING.register(ignored -> server.set(null));
        NetworkPlatform.setCurrentServerSupplier(server::get);
        FabricServicesBootstrap.initCommon();
        KltytonMenus.register();
        NetworkManagerImpl.initialize();
        NetworkAutoRegistration.findAllAnnotatedPackets();
    }
}

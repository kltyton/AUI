package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.registry.KltytonMenus;
import io.github.kltyton.kltytonui.network.KltytonNetwork;
import io.github.kltyton.kltytonui.util.KuiLogging;
import net.fabricmc.api.ModInitializer;

public final class KltytonUIFabric implements ModInitializer {
    public void onInitialize() {
        KuiLogging.installFileAppender();
        FabricServicesBootstrap.initCommon();
        KltytonMenus.register();
        KltytonNetwork.register();
    }
}

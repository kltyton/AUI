package io.github.kltyton.kltytonui.forge;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.dev.DevToolsLogBridge;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.spi.KuiServices;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.io.IOException;

/** Client-only service and shader wiring. */
public final class ClientServicesBootstrap {
    private ClientServicesBootstrap() {
    }

    public static void init(IEventBus modEventBus) {
        KuiServices.setClient(ClientService.INSTANCE);
        KuiServices.setResources(ResourceService.INSTANCE);
        KuiServices.setKeys(KeyService.INSTANCE);
        KuiServices.setRender(RenderService.INSTANCE);
        KuiServices.setItems(ItemRenderService.INSTANCE);
        KuiServices.setAudio(io.github.kltyton.kltytonui.media.openal.OpenAlAudioService.create(
                () -> net.minecraft.client.Minecraft.getInstance().options
                        .getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)));
        KuiServices.setWebView(io.github.kltyton.kltytonui.webview.NativeWebViewService.INSTANCE);
        DevToolsLogBridge.install(KltytonUI.LOGGER);
        KltytonUIRegistry.register();
        modEventBus.addListener(ClientServicesBootstrap::onRegisterShaders);
    }

    private static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ShaderRegistry.register(event);
        } catch (IOException ignored) {
        }
    }
}

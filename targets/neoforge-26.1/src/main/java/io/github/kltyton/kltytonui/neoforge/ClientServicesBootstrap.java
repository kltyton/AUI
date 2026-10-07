package io.github.kltyton.kltytonui.neoforge;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.client.gui.KltytonGuiLayers;
import io.github.kltyton.kltytonui.dev.DevToolsLogBridge;
import io.github.kltyton.kltytonui.loader.ClientLoaderForge;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.registry.Keybindings;
import io.github.kltyton.kltytonui.spi.KuiServices;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Client-only service and 26.1 render-event wiring. */
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

        // Client and WorldWindowRenderer carry @EventBusSubscriber, so FML
        // registers them automatically. Do not register them a second time.
        modEventBus.register(Keybindings.class);
        modEventBus.register(ClientLoaderForge.class);
        modEventBus.addListener(ClientServicesBootstrap::configureMainRenderTarget);
        modEventBus.addListener(ClientServicesBootstrap::registerRenderPipelines);
        modEventBus.addListener(ClientServicesBootstrap::registerGuiLayers);
        modEventBus.addListener(ClientServicesBootstrap::registerPipRenderers);
    }

    private static void configureMainRenderTarget(ConfigureMainRenderTargetEvent event) {
        event.enableStencil();
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        PipelineRegistry.registerPipelines(event);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        KltytonGuiLayers.register(event);
    }

    private static void registerPipRenderers(RegisterPictureInPictureRenderersEvent event) {
        KltytonGuiLayers.registerPictureInPictureRenderers(event);
    }
}

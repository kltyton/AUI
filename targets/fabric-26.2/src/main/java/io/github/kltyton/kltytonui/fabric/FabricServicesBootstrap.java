package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.dev.DevToolsLogBridge;
import io.github.kltyton.kltytonui.spi.KuiServices;

public final class FabricServicesBootstrap {
    private FabricServicesBootstrap() { }
    public static void initCommon() {
        KuiServices.setNetwork(FabricNetworkService.INSTANCE);
        KuiServices.setExpander(new FabricDocumentExpander());
        KuiServices.setConfig(FabricConfigService.INSTANCE);
        KuiServices.setScript(FabricScriptService.INSTANCE);
    }
    public static void initClient() {
        KuiServices.setClient(FabricClientService.INSTANCE);
        KuiServices.setResources(ResourceService.INSTANCE);
        KuiServices.setKeys(FabricKeyService.INSTANCE);
        KuiServices.setRender(RenderService.INSTANCE);
        KuiServices.setItems(ItemRenderService.INSTANCE);
        KuiServices.setAudio(io.github.kltyton.kltytonui.media.openal.OpenAlAudioService.create(
                () -> net.minecraft.client.Minecraft.getInstance().options
                        .getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)));
        KuiServices.setWebView(io.github.kltyton.kltytonui.webview.NativeWebViewService.INSTANCE);
        DevToolsLogBridge.install(KltytonUI.LOGGER);
    }
}

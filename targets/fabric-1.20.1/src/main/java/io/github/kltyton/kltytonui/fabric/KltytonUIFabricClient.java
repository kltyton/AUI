package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.client.Client;
import io.github.kltyton.kltytonui.dom.expander.RecipeExpander;
import io.github.kltyton.kltytonui.registry.KltytonUIRegistry;
import io.github.kltyton.kltytonui.registry.ClientMenuScreens;
import io.github.kltyton.kltytonui.registry.Keybindings;
import io.github.kltyton.kltytonui.world.WorldWindow;
import io.github.kltyton.kltytonui.network.fabric.NetworkManagerImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class KltytonUIFabricClient implements ClientModInitializer {
    public void onInitializeClient() {
        FabricServicesBootstrap.initClient();
        NetworkManagerImpl.initializeClient();
        // The common entrypoint runs before the client service exists. Register
        // the scan scope after installing the real Fabric client service.
        KltytonUIRegistry.scanPackages("io.github.kltyton.kltytonui.element");
        FabricShaderRegistry.register();
        registerKeys();
        KltytonUIRegistry.register();
        ClientMenuScreens.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> RecipeExpander.clearCache());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RecipeExpander.clearCache());
        ClientTickEvents.END_CLIENT_TICK.register(client -> Client.tick());
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (Minecraft.getInstance().screen != null) return;
            io.github.kltyton.kltytonui.init.Window.window.fireAnimationFrame();
            Client.drawOverlayLike(graphics);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (WorldWindow.windows.isEmpty()) return;
            for (WorldWindow window : WorldWindow.windows) window.render(context.matrixStack(), context.projectionMatrix(), context.tickDelta());
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register((Screen target, GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) -> {
                    io.github.kltyton.kltytonui.init.Window.window.fireAnimationFrame();
                    Client.drawScreenLike(graphics);
                }));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(KltytonUI.MODID, "client_resources");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                RecipeExpander.clearCache();
                Minecraft.getInstance().execute(io.github.kltyton.kltytonui.loader.ClientLoader::reloadResources);
            }
        });
    }

    private static void registerKeys() {
        KeyBindingHelper.registerKeyBinding(Keybindings.RELEASE_MOUSE);
        KeyBindingHelper.registerKeyBinding(Keybindings.RELOAD);
        KeyBindingHelper.registerKeyBinding(Keybindings.DEV_TOOLS);
        KeyBindingHelper.registerKeyBinding(Keybindings.RESOURCE_MANAGER);
    }
}

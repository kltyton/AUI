package io.github.kltyton.kltytonui.registry;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.screen.KltytonContainerMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Constructor;
import io.github.kltyton.kltytonui.screen.KltytonContainerScreen;

@Mod.EventBusSubscriber(modid = KltytonUI.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientMenuScreens {
    private static final String CONTAINER_SCREEN_CLASS = "io.github.kltyton.kltytonui.screen.KltytonContainerScreen";

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> MenuScreens.register(KltytonMenus.KLTYTONUI_CONTAINER.get(), ClientMenuScreens::createScreen)
        );
    }

    @SuppressWarnings("unchecked")
    private static AbstractContainerScreen<KltytonContainerMenu> createScreen(KltytonContainerMenu menu, Inventory inventory, Component title) {
        try {
            Class<?> screenClass = Class.forName(CONTAINER_SCREEN_CLASS);
            Constructor<?> constructor = screenClass.getConstructor(KltytonContainerMenu.class, Inventory.class, Component.class);
            return (AbstractContainerScreen<KltytonContainerMenu>) constructor.newInstance(menu, inventory, title);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create Kltyton container screen", exception);
        }
    }
}

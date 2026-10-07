package io.github.kltyton.kltytonui.registry;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.screen.KltytonContainerMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public final class KltytonMenus {
    public static final MenuType<KltytonContainerMenu> KLTYTONUI_CONTAINER = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(KltytonUI.MODID, "kltytonui_container"),
            new ExtendedScreenHandlerType<>(KltytonContainerMenu::new)
    );
    private KltytonMenus() { }
    public static void register() { }
}

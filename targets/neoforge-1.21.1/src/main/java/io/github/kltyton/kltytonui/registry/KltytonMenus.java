package io.github.kltyton.kltytonui.registry;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.screen.KltytonContainerMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KltytonMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, KltytonUI.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<KltytonContainerMenu>> KLTYTONUI_CONTAINER =
            MENUS.register("kltytonui_container", () -> IMenuTypeExtension.create(KltytonContainerMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}

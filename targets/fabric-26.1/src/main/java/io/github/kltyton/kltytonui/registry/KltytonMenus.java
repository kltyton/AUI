package io.github.kltyton.kltytonui.registry;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.container.SlotLayout;
import io.github.kltyton.kltytonui.screen.KltytonContainerMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public final class KltytonMenus {
    private static final StreamCodec<RegistryFriendlyByteBuf, SlotLayout> SLOT_LAYOUT_CODEC =
            StreamCodec.of((buf, layout) -> layout.write(buf), SlotLayout::read);

    public static final MenuType<KltytonContainerMenu> KLTYTONUI_CONTAINER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(KltytonUI.MODID, "kltytonui_container"),
            new ExtendedMenuType<>(KltytonContainerMenu::new, SLOT_LAYOUT_CODEC)
    );

    private KltytonMenus() {
    }

    public static void register() {
    }
}

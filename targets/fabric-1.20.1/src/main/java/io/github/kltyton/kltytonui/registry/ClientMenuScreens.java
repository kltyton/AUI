package io.github.kltyton.kltytonui.registry;

import io.github.kltyton.kltytonui.screen.KltytonContainerMenu;
import io.github.kltyton.kltytonui.screen.KltytonContainerScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ClientMenuScreens {
    private ClientMenuScreens() { }
    public static void register() {
        MenuScreens.register(KltytonMenus.KLTYTONUI_CONTAINER, KltytonContainerScreen::new);
    }
}

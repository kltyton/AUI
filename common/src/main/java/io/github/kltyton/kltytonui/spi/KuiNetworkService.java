package io.github.kltyton.kltytonui.spi;

import io.github.kltyton.kltytonui.element.ContainerDeclaration;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Loader-side networking and server menu access.
 *
 * <p>Implemented by the Forge network layer and registered through
 * {@link KuiServices}. Common KJS bindings use this interface instead of the
 * loader's network classes directly.</p>
 */
public interface KuiNetworkService {
    /** Creates a pending container menu for the given player. */
    KuiPendingMenu pendingMenu(ServerPlayer player, String templatePath);

    /** Opens a screen for the player with the given container declarations. */
    void openScreen(ServerPlayer player, String templatePath, List<ContainerDeclaration> declarations);
}

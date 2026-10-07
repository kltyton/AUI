package io.github.kltyton.kltytonui.forge;

import io.github.kltyton.kltytonui.element.ContainerDeclaration;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;
import io.github.kltyton.kltytonui.network.handler.PendingMenu;
import io.github.kltyton.kltytonui.spi.KuiNetworkService;
import io.github.kltyton.kltytonui.spi.KuiPendingMenu;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Forge implementation of {@link KuiNetworkService}, delegating to the loader's
 * network layer ({@link PendingMenu} / {@link KltytonScreenNetworkHandler}).
 */
public final class NetworkService implements KuiNetworkService {
    public static final NetworkService INSTANCE = new NetworkService();

    private NetworkService() {
    }

    @Override
    public KuiPendingMenu pendingMenu(ServerPlayer player, String templatePath) {
        return new PendingMenuAdapter(player, templatePath);
    }

    /**
     * Concrete Forge-side adapter exposed to KubeJS/Rhino instead of a synthetic
     * lambda class. The common SPI remains loader-neutral.
     */
    public static final class PendingMenuAdapter implements KuiPendingMenu {
        private final PendingMenu delegate;

        public PendingMenuAdapter(ServerPlayer player, String templatePath) {
            this.delegate = new PendingMenu(player, templatePath);
        }

        @Override
        public void bind(Consumer<io.github.kltyton.kltytonui.spi.KuiBindingBuilder> binder) {
            if (binder == null) {
                delegate.bind(null);
                return;
            }
            delegate.bind(builder -> binder.accept(builder));
        }
    }

    @Override
    public void openScreen(ServerPlayer player, String templatePath, List<ContainerDeclaration> declarations) {
        KltytonScreenNetworkHandler.openScreen(player, templatePath, declarations);
    }
}

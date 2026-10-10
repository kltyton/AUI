package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.element.ContainerDeclaration;
import io.github.kltyton.kltytonui.network.handler.KltytonScreenNetworkHandler;
import io.github.kltyton.kltytonui.network.handler.PendingMenu;
import io.github.kltyton.kltytonui.spi.KuiNetworkService;
import io.github.kltyton.kltytonui.spi.KuiPendingMenu;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.Consumer;

public final class FabricNetworkService implements KuiNetworkService {
    public static final FabricNetworkService INSTANCE = new FabricNetworkService();

    private FabricNetworkService() {
    }

    @Override
    public KuiPendingMenu pendingMenu(ServerPlayer player, String templatePath) {
        return new PendingMenuAdapter(player, templatePath);
    }

    /** Stable loader-side adapter exposed to scripting integrations. */
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

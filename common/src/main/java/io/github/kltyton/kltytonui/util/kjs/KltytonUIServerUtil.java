package io.github.kltyton.kltytonui.util.kjs;

import io.github.kltyton.kltytonui.element.ContainerDeclaration;
import io.github.kltyton.kltytonui.registry.annotation.KJSBindings;
import io.github.kltyton.kltytonui.spi.KuiPendingMenu;
import io.github.kltyton.kltytonui.spi.KuiServices;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

@KJSBindings(value = "KltytonUI")
public class KltytonUIServerUtil {

    /**
     * 服务端创建带容器绑定的菜单 Screen。
     * <p>
     * 使用示例（KJS）：
     * <pre>
     * KltytonUI.menu(player, "test/test.html").bind(b => b.blockEntity(pos).player())
     * </pre>
     */
    public static KuiPendingMenu menu(ServerPlayer player, String path) {
        return KuiServices.network().pendingMenu(player, path);
    }

    /**
     * 服务端打开 Screen（带容器声明）。
     *
     * @deprecated 使用 {@link #menu(ServerPlayer, String)} 替代
     */
    @Deprecated
    public static void openScreen(ServerPlayer player, String path, List<ContainerDeclaration> declarations) {
        KuiServices.network().openScreen(player, path, declarations);
    }

    /**
     * 服务端打开纯 UI Screen（无容器）。
     *
     * @deprecated 使用 {@link #menu(ServerPlayer, String)} 替代
     */
    @Deprecated
    public static void openScreen(ServerPlayer player, String path) {
        KuiServices.network().openScreen(player, path, List.of());
    }
}

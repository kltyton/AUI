package io.github.kltyton.kltytonui.spi;

import java.util.function.Consumer;

/**
 * A server-side menu opened with container bindings, returned by
 * {@code KltytonUI.menu(player, templatePath)}.
 */
public interface KuiPendingMenu {
    void bind(Consumer<KuiBindingBuilder> configurator);
}

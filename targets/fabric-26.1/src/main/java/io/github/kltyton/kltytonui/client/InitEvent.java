package io.github.kltyton.kltytonui.client;

import io.github.kltyton.kltytonui.init.Window;

/** Fabric client lifecycle hooks for common persistent client state. */
public final class InitEvent {
    private static int tickCounter;

    private InitEvent() {
    }

    public static void init() {
        Window.window.localStorage.load();
    }

    public static void tick() {
        if (++tickCounter < 5000) return;
        tickCounter = 0;
        Window.window.localStorage.save();
    }
}

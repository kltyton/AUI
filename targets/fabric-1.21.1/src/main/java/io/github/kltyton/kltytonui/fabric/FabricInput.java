package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.KltytonUI;

/** Defensive entry points used by the Fabric keyboard and mouse mixins. */
public final class FabricInput {
    private FabricInput() {
    }

    public static boolean keyPress(int key, int scanCode, int action, int modifiers) {
        if (action != org.lwjgl.glfw.GLFW.GLFW_PRESS
                && action != org.lwjgl.glfw.GLFW.GLFW_REPEAT
                && action != org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
            return false;
        }
        try {
            return io.github.kltyton.kltytonui.client.Client.handleKeyInput(key, scanCode, action, modifiers);
        } catch (Throwable exception) {
            KltytonUI.LOGGER.error("[KUI Fabric] keyboard dispatch failed", exception);
            return false;
        }
    }

    public static boolean mouseButton(int button, int action) {
        try {
            return io.github.kltyton.kltytonui.client.Client.handleMouseButton(button, action);
        } catch (Throwable exception) {
            KltytonUI.LOGGER.error("[KUI Fabric] mouse button dispatch failed", exception);
            return false;
        }
    }

    public static boolean mouseScroll(double delta) {
        try {
            return io.github.kltyton.kltytonui.client.Client.handleMouseScroll(delta);
        } catch (Throwable exception) {
            KltytonUI.LOGGER.error("[KUI Fabric] mouse scroll dispatch failed", exception);
            return false;
        }
    }
}

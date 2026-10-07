package io.github.kltyton.kltytonui.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keybindings {
    public static final KeyMapping RELEASE_MOUSE = new KeyMapping("key.kltytonui.release_mouse", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.kltytonui");
    public static final KeyMapping RELOAD = new KeyMapping("key.kltytonui.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.kltytonui");
    public static final KeyMapping DEV_TOOLS = new KeyMapping("key.kltytonui.dev_tools", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.kltytonui");
    public static final KeyMapping RESOURCE_MANAGER = new KeyMapping("key.kltytonui.resource_manager", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.kltytonui");
    private Keybindings() { }
}

package io.github.kltyton.kltytonui.registry;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kltyton.kltytonui.KltytonUI;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = KltytonUI.MODID, value = Dist.CLIENT)
public class Keybindings {
    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(KltytonUI.MODID, "kltytonui"));

    public static final KeyMapping RELEASE_MOUSE = new KeyMapping("key.kltytonui.release_mouse",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );

    public static final KeyMapping RELOAD = new KeyMapping("key.kltytonui.reload",
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping DEV_TOOLS = new KeyMapping("key.kltytonui.dev_tools",
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping RESOURCE_MANAGER = new KeyMapping("key.kltytonui.resource_manager",
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(RELEASE_MOUSE);
        event.register(RELOAD);
        event.register(DEV_TOOLS);
        event.register(RESOURCE_MANAGER);
    }
}

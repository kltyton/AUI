package io.github.kltyton.kltytonui.world;

import io.github.kltyton.kltytonui.KltytonUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge world-render hook for {@link WorldWindow}.
 *
 * <p>The render invocation (which event, when, and which matrices to pass) is
 * loader/version-specific, so it lives in the loader target; the shared window
 * data, registry and {@link WorldWindow#render} logic stay in {@code common}.
 * Other loaders provide their own hook that calls {@code window.render(...)}.</p>
 */
@Mod.EventBusSubscriber(modid = KltytonUI.MODID, value = Dist.CLIENT)
public final class WorldWindowRenderer {
    private WorldWindowRenderer() {
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (WorldWindow.windows.isEmpty()) return;

        for (WorldWindow window : WorldWindow.windows) {
            window.render(event.getPoseStack(), event.getProjectionMatrix(), event.getPartialTick());
        }
    }
}

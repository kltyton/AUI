package io.github.kltyton.kltytonui.client.gui;

import io.github.kltyton.kltytonui.client.gui.pip.KltytonUiPipRenderState;
import io.github.kltyton.kltytonui.client.gui.pip.KltytonUiPipRenderer;
import io.github.kltyton.kltytonui.mixin.accessor.GuiGraphicsExtractorAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/**
 * Submits KUI's documents as vanilla Picture-in-Picture states and registers the
 * PIP renderer that rasterises them.
 *
 * <p>26.2 only rasterises GUI content in the render phase, so immediate-mode
 * drawing during extraction is impossible: KUI documents are posted as a
 * fullscreen {@link KltytonUiPipRenderState} and drawn by
 * {@link KltytonUiPipRenderer} into a texture the vanilla {@code GuiRenderer}
 * then composites. HUD extraction goes through
 * {@code KltytonUIFabricClient}'s {@code HudElementRegistry} element;
 * {@code ScreenEvents.afterExtract} covers the screen case, where the HUD layer
 * manager does not run.</p>
 */
public final class KltytonGuiLayers {
    private KltytonGuiLayers() {
    }

    /** Registers the PIP renderer backing {@link KltytonUiPipRenderState}. */
    public static void registerPictureInPictureRenderers() {
        PictureInPictureRendererRegistry.register(context -> new KltytonUiPipRenderer());
        KuiNativeViewport.register();
    }

    public static void submitOverlay(GuiGraphicsExtractor guiGraphics) {
        submitUi(guiGraphics);
        submitCursor(guiGraphics);
    }

    /**
     * Submits only the document PIP state. {@code KuiLinkedScreen}s call this
     * themselves mid-extraction so frame-local floating items can be attached
     * to the same PIP payload.
     */
    public static void submitUi(GuiGraphicsExtractor guiGraphics) {
        submitUi(guiGraphics, null);
    }

    public static void submitUi(
            GuiGraphicsExtractor guiGraphics,
            KltytonUiPipRenderState.FloatingItemBatch floatingItems
    ) {
        if (isDuplicateThisFrame(guiGraphics, true)) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        submit(guiGraphics, KltytonUiPipRenderState.ui(0, 0, w, h, null, floatingItems));
    }

    /** Submits only the pseudo-cursor PIP state; always composited last. */
    public static void submitCursor(GuiGraphicsExtractor guiGraphics) {
        if (isDuplicateThisFrame(guiGraphics, false)) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        submit(guiGraphics, KltytonUiPipRenderState.cursor(0, 0, w, h, null));
    }

    /**
     * Posts the state into the extractor's render state.
     *
     * <p>The scissor area is left {@code null} on purpose: vanilla keeps the
     * extractor's scissor stack private (NeoForge exposes
     * {@code peekScissorStack()}), and KUI's overlay always covers the whole GUI
     * surface, so intersecting it with an outer scissor would only ever shrink
     * KUI's own output. KUI clips its content itself through
     * {@code io.github.kltyton.kltytonui.render.Mask}.</p>
     */
    private static void submit(GuiGraphicsExtractor guiGraphics, KltytonUiPipRenderState state) {
        GuiRenderState renderState = ((GuiGraphicsExtractorAccessor) (Object) guiGraphics).kui$guiRenderState();
        if (renderState == null) return;
        renderState.addPicturesInPictureState(state);
    }

    // The extractor is created fresh per frame in GameRenderer.extractGui, so
    // its identity doubles as a frame stamp.
    private static GuiGraphicsExtractor lastUiExtractor;
    private static GuiGraphicsExtractor lastCursorExtractor;

    /**
     * Guards against submitting two equal PIP states in one frame: the PIP
     * renderer pool keys renderers by state equality, and a second equal state
     * overwrites the first one's pool entry without closing it — orphaning
     * (leaking) a fullscreen-texture renderer every frame.
     */
    private static boolean isDuplicateThisFrame(GuiGraphicsExtractor guiGraphics, boolean ui) {
        if (ui) {
            if (guiGraphics == lastUiExtractor) return true;
            lastUiExtractor = guiGraphics;
            return false;
        }
        if (guiGraphics == lastCursorExtractor) return true;
        lastCursorExtractor = guiGraphics;
        return false;
    }
}

package io.github.kltyton.kltytonui.client.gui.pip;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.kltytonui.dev.resource.ResourcePreviewDialog;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.neoforge.RenderService;
import io.github.kltyton.kltytonui.render.Base;
import io.github.kltyton.kltytonui.render.DocumentLayerOrder;
import io.github.kltyton.kltytonui.render.Mask;
import io.github.kltyton.kltytonui.render.RenderNode;
import io.github.kltyton.kltytonui.style.Cursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

/**
 * Renders KUI documents into the PIP texture.
 *
 * <p>The vanilla PIP pose (translate-to-centre + guiScale) targets physical
 * pixels and centres the coordinate system, which does not match KUI's
 * immediate-mode GUI-coordinate drawing — feeding KUI content through it puts
 * everything half a texture off-centre. Instead this renderer installs the
 * same state KUI used on 1.21.1's main target: an orthographic projection in
 * GUI units, an identity model-view, and a fresh identity {@link PoseStack}.
 * The fullscreen PIP texture is exactly the window's physical size, so the
 * device-pixel scissor rectangles computed by {@link Mask} line up with the
 * window coordinate system.</p>
 */
public final class KltytonUiPipRenderer extends PictureInPictureRenderer<KltytonUiPipRenderState> {
    private GpuTexture stencilDepthTexture;
    private GpuTextureView stencilDepthTextureView;
    private static final float MIN_PIP_DEPTH_RANGE = 1024.0F;

    public KltytonUiPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NonNull Class<KltytonUiPipRenderState> getRenderStateClass() {
        return KltytonUiPipRenderState.class;
    }

    /**
     * Vanilla allocates the PIP depth attachment as {@code DEPTH32} (no stencil
     * bits), which silently disables {@link Mask}'s stencil clips (rounded
     * corners, masks under transformed ancestors) for every overlay document.
     * Swap in our own depth-stencil texture while the PIP pass renders; vanilla
     * resets the override to null right after {@code renderToTexture}.
     */
    private void installStencilDepthOverride() {
        GpuTextureView override = RenderSystem.outputDepthTextureOverride;
        if (override == null) return;
        GpuTexture vanilla = override.texture();
        if (vanilla.getFormat().hasStencilAspect()) return;
        int width = vanilla.getWidth(0);
        int height = vanilla.getHeight(0);
        if (stencilDepthTexture == null || stencilDepthTexture.getWidth(0) != width
                || stencilDepthTexture.getHeight(0) != height) {
            closeStencilDepth();
            GpuDevice device = RenderSystem.getDevice();
            stencilDepthTexture = device.createTexture(
                    () -> "kltytonui_pip_depth_stencil",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.DEPTH32_STENCIL8, width, height, 1, 1);
            stencilDepthTextureView = device.createTextureView(stencilDepthTexture);
        }
        RenderSystem.outputDepthTextureOverride = stencilDepthTextureView;
        // Vanilla cleared its own depth texture before renderToTexture; ours
        // needs the same per-frame reset (depth + stencil).
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(stencilDepthTexture, 1.0);
        encoder.clearStencilTexture(stencilDepthTexture, 0);
    }

    private void closeStencilDepth() {
        if (stencilDepthTextureView != null) {
            stencilDepthTextureView.close();
            stencilDepthTextureView = null;
        }
        if (stencilDepthTexture != null) {
            stencilDepthTexture.close();
            stencilDepthTexture = null;
        }
    }

    @Override
    public void close() {
        closeStencilDepth();
        super.close();
    }

    @Override
    protected void renderToTexture(KltytonUiPipRenderState renderState, @NonNull PoseStack poseStack) {
        installStencilDepthOverride();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.identity();
            int guiWidth = Math.max(1, renderState.x1() - renderState.x0());
            int guiHeight = Math.max(1, renderState.y1() - renderState.y0());
            float depthRange = Math.max(
                    MIN_PIP_DEPTH_RANGE,
                    Base.getFlatOverlayZ() + Base.getGuiItemDecorationZ() + 1.0F
            );
            RenderService.INSTANCE.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0f, guiWidth, guiHeight, 0.0f, -depthRange, depthRange));

            PoseStack guiPose = new PoseStack();
            if (renderState.mode() == KltytonUiPipRenderState.Mode.UI) {
                Mask.resetDepth();
                for (Document document : DocumentLayerOrder.backToFront(Document.getAll())) {
                    if (document == null || document.inWorld || document.isManuallyRendered()) continue;
                    Base.drawOverlayDocument(guiPose, document);
                    // Resource previews are intentionally excluded from the
                    // normal document pass and draw themselves inside the owner's
                    // viewport. Draw them right after the owner so the previewed
                    // HTML stays below the DevTools tool document and the toast.
                    ResourcePreviewDialog.draw(guiPose, document);
                }
                // Floating container items share the same PoseStack backend but
                // must not inherit the final document's clip or stencil state.
                Mask.resetDepth();
                guiPose.pushPose();
                guiPose.translate(0.0F, 0.0F, Base.getFlatOverlayZ());
                try {
                    for (KltytonUiPipRenderState.FloatingItem item : renderState.floatingItems().items()) {
                        RenderNode.ItemNode.positioned(
                                item::stack,
                                item.x(),
                                item.y(),
                                1.0D,
                                232,
                                true,
                                item.overlayText(),
                                item.decorationOffsetY(),
                                false
                        ).render(guiPose);
                    }
                } finally {
                    guiPose.popPose();
                }
                // Ensure scissor/mask state never leaks into later GUI rendering.
                Mask.resetDepth();
            } else if (renderState.mode() == KltytonUiPipRenderState.Mode.CURSOR) {
                Cursor.drawPseudoCursor(guiPose);
            }
            // Font text queued by the documents lives in the shared buffer
            // source; flush it while the PIP override is still active.
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        } finally {
            modelView.popMatrix();
        }
    }

    @Override
    protected @NonNull String getTextureLabel() {
        return "kltytonui";
    }
}

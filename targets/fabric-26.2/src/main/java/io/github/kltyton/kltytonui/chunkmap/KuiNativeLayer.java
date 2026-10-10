package io.github.kltyton.kltytonui.chunkmap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.render.Base;
import io.github.kltyton.kltytonui.render.Mask;
import io.github.kltyton.kltytonui.render.OutputTargets;
import io.github.kltyton.kltytonui.spi.KuiServices;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/** Draws a manually owned KUI map document into the host's current native map target. */
public final class KuiNativeLayer {
    public static void draw(Document document, int guiWidth, int guiHeight) {
        if (!document.isManuallyRendered()) throw new IllegalArgumentException("Map document must be manually rendered");
        var backend = KuiServices.render();
        var output = OutputTargets.rawCurrentTarget();
        var projection = backend.getProjectionMatrix();
        var nativeProjection = RenderSystem.getProjectionMatrixBuffer();
        var projectionType = RenderSystem.getProjectionType();
        var scissor = new ScissorState(RenderSystem.getScissorStateForRenderTypeDraws());
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        Base.pushDocumentZOffset(0);
        Base.pushDepthTest(false);
        try (var state = backend.pushFilterRenderState()) {
            modelView.identity();
            OutputTargets.setCurrent(Minecraft.getInstance().gameRenderer.mainRenderTarget());
            var guiProjection = new net.minecraft.client.renderer.Projection();
            guiProjection.setupOrtho(-10000, 10000, guiWidth, guiHeight, true);
            backend.setProjectionMatrix(guiProjection.getMatrix(new Matrix4f()));
            Mask.resetDepth();
            Base.drawOverlayDocument(new PoseStack(), document);
        } finally {
            Mask.resetDepth();
            Base.popDocumentZOffset();
            Base.popDepthTest();
            backend.setProjectionMatrix(projection);
            RenderSystem.setProjectionMatrix(nativeProjection, projectionType);
            RenderSystem.getScissorStateForRenderTypeDraws().setFrom(scissor);
            OutputTargets.setCurrent(output);
            modelView.popMatrix();
        }
    }

    private KuiNativeLayer() { }
}

package io.github.kltyton.kltytonui.screen;

import io.github.kltyton.kltytonui.client.Client;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.event.Event;
import io.github.kltyton.kltytonui.loader.ClientLoader;
import io.github.kltyton.kltytonui.spi.KuiServices;
import io.github.kltyton.kltytonui.render.Base;
import io.github.kltyton.kltytonui.render.FrameTimingHud;
import io.github.kltyton.kltytonui.render.Mask;
import io.github.kltyton.kltytonui.render.PoseMatrices;
import io.github.kltyton.kltytonui.style.Cursor;
import io.github.kltyton.kltytonui.layout.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import io.github.kltyton.kltytonui.viewport.KltytonViewport;

public class KltytonScreen extends Screen implements KuiLinkedScreen {
    private final String templatePath;
    private boolean pauseGame;
    private boolean showDefaultBackground;
    private Document linkedDocument;
    private boolean loggedInitState = false;
    private boolean loggedRenderState = false;

    public KltytonScreen(String templatePath) {
        super(Component.empty());
        this.templatePath = templatePath;
    }

    /** Sets whether Minecraft should pause while this screen is open. */
    public KltytonScreen setPauseGame(boolean pauseGame) {
        this.pauseGame = pauseGame;
        return this;
    }

    /** Sets whether Minecraft's standard screen background should be drawn first. */
    public KltytonScreen setShowDefaultBackground(boolean showDefaultBackground) {
        this.showDefaultBackground = showDefaultBackground;
        return this;
    }

    public boolean isPauseGame() {
        return pauseGame;
    }

    public boolean isShowDefaultBackground() {
        return showDefaultBackground;
    }

    public Document getLinkedDocument() {
        return linkedDocument;
    }

    @Override
    protected void init() {
        super.init();

        if (linkedDocument != null) {
            linkedDocument.remove();
            linkedDocument = null;
        }

        linkedDocument = Document.create(templatePath);
        if (linkedDocument != null) {
            linkedDocument.applyViewport(false);
        }
        if (!loggedInitState) {
            loggedInitState = true;
            KltytonViewport viewport = currentViewport();
            io.github.kltyton.kltytonui.KltytonUI.LOGGER.debug(
                    "[KUI Screen] init path={} viewport={}x{} doc={} body={} paintList={}",
                    templatePath,
                    viewport.layoutWidth(),
                    viewport.layoutHeight(),
                    linkedDocument == null ? "<null>" : linkedDocument.getUuid(),
                    linkedDocument == null || linkedDocument.body == null ? "<null>" : linkedDocument.body.tagName,
                    linkedDocument == null ? -1 : linkedDocument.getPaintList().size()
            );
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (linkedDocument != null) {
            linkedDocument.applyViewport(true);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        FrameTimingHud.beginFrame();
        try {
            if (showDefaultBackground) {
                renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            }
            if (linkedDocument != null) {
                if (!loggedRenderState) {
                    loggedRenderState = true;
                    io.github.kltyton.kltytonui.KltytonUI.LOGGER.debug(
                            "[KUI Screen] render path={} doc={} body={} paintList={} dirty={}",
                            templatePath,
                            linkedDocument.getUuid(),
                            linkedDocument.body == null ? "<null>" : linkedDocument.body.tagName,
                            linkedDocument.getPaintList().size(),
                            linkedDocument.getDirtyElements().size()
                    );
                }
                KltytonViewport viewport = currentViewport();
                guiGraphics.pose().pushPose();
                try {
                    PoseMatrices.scale2D(guiGraphics.pose(), viewport.renderScale(), viewport.renderScale());
                    Mask.pushScissorScale(viewport.scissorScale(), guiGraphics.pose());
                    Base.drawScreenDocument(guiGraphics.pose(), linkedDocument);
                } finally {
                    Mask.popScissorScale();
                    guiGraphics.pose().popPose();
                }
                Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            }
            // Draw the resource preview right after its owning document so the
            // previewed HTML stays below the DevTools tool document (and toasts).
            io.github.kltyton.kltytonui.dev.resource.ResourcePreviewDialog.draw(guiGraphics.pose(), linkedDocument);
            Client.drawPersistentScreenDocuments(guiGraphics, linkedDocument);
            guiGraphics.flush();
            Cursor.drawPseudoCursor(guiGraphics.pose());
            guiGraphics.flush();
        } finally {
            FrameTimingHud.endFrame();
            Client.drawFrameTimingHud(guiGraphics);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalScroll, double delta) {
        if (hasControlDown() && handleViewportZoom(delta > 0)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalScroll, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KuiServices.keys().reloadKey()) {
            ClientLoader.reload();
            return true;
        }
        if (isControlModifier(modifiers)) {
            if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
                return handleViewportZoom(true);
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                return handleViewportZoom(false);
            }
            if (keyCode == GLFW.GLFW_KEY_0 || keyCode == GLFW.GLFW_KEY_KP_0) {
                return resetViewportZoom();
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (linkedDocument != null) {
            if (linkedDocument.body != null) {
                Event.triggerSingle(new Event(linkedDocument.body, "unload", false));
            }
            linkedDocument.remove();
        }
        Size.clearViewportOverride();
        Cursor.resetToDefault();
        super.onClose();
    }

    @Override
    public void removed() {
        if (linkedDocument != null) {
            linkedDocument.remove();
        }
        Size.clearViewportOverride();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return pauseGame;
    }

    public boolean handleViewportZoom(boolean zoomIn) {
        return linkedDocument != null && linkedDocument.handleViewportZoom(zoomIn);
    }

    public boolean resetViewportZoom() {
        return linkedDocument != null && linkedDocument.resetViewportZoom();
    }

    private KltytonViewport currentViewport() {
        return linkedDocument == null ? new KltytonViewport(1, 1, 1.0f, 1.0d) : linkedDocument.getViewport();
    }

    private static boolean isControlModifier(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }
}

package io.github.kltyton.kltytonui.element;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.layout.Position;
import io.github.kltyton.kltytonui.layout.Size;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;
import io.github.kltyton.kltytonui.render.Base;
import io.github.kltyton.kltytonui.render.ImageDrawer;
import io.github.kltyton.kltytonui.render.Rect;
import io.github.kltyton.kltytonui.spi.TextureKey;
import io.github.kltyton.kltytonui.parser.CSS;

/**
 * Renders a texture already managed by Minecraft's texture manager.
 * The {@code src} attribute is a {@link ResourceLocation}; authors must provide
 * the element's rendered width and height through CSS.
 */
@ElementRegister(Texture.TAG_NAME)
public class Texture extends Element {
    public static final String TAG_NAME = "TEXTURE";

    private String observedSrc = "";
    private TextureKey textureLocation;

    public Texture(Document document) {
        super(document, TAG_NAME);
    }

    public TextureKey getTextureLocation() {
        syncSource();
        return textureLocation;
    }

    public String getCurrentSrc() {
        TextureKey location = getTextureLocation();
        return location == null ? "" : location.toString();
    }

    @Override
    protected void onInitFromDom(Element origin) {
        syncSource();
    }

    @Override
    public void setAttribute(String name, String value) {
        super.setAttribute(name, value);
        if ("src".equals(name)) syncSource();
    }

    @Override
    public void removeAttribute(String name) {
        super.removeAttribute(name);
        if ("src".equals(name)) syncSource();
    }

    @Override
    public void drawPhase(PoseStack poseStack, Base.RenderPhase phase) {
        Rect rectRenderer = Rect.of(this);
        switch (phase) {
            case SHADOW -> rectRenderer.drawShadow(poseStack);
            case BODY -> {
                rectRenderer.drawBody(poseStack);
                drawTexture(poseStack, rectRenderer);
            }
            case BORDER -> rectRenderer.drawBorder(poseStack);
        }
    }

    private void drawTexture(PoseStack poseStack, Rect rectRenderer) {
        TextureKey location = getTextureLocation();
        if (location == null) return;

        Position position = rectRenderer.getBodyRectPosition();
        Size size = rectRenderer.getBodyRectSize();
        if (size.width() <= 0 || size.height() <= 0) return;

        ImageDrawer.draw(
                poseStack,
                location,
                (float) position.x,
                (float) position.y,
                (float) size.width(),
                (float) size.height(),
                "true".equals(getAttribute("blur"))
        );
    }

    private void syncSource() {
        String src = getAttribute("src");
        src = src == null ? "" : src.trim();
        if (src.equals(observedSrc)) return;
        observedSrc = src;
        textureLocation = io.github.kltyton.kltytonui.spi.KuiServices.resources().tryParseTextureKey(src);
    }
}

package io.github.kltyton.kltytonui.element;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.kltytonui.element.Span;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;
import io.github.kltyton.kltytonui.render.Base;
import io.github.kltyton.kltytonui.render.Rect;
import io.github.kltyton.kltytonui.layout.NormalFlow;
import io.github.kltyton.kltytonui.style.Text;
import net.minecraft.network.chat.Component;
import io.github.kltyton.kltytonui.parser.HTML;

@ElementRegister(Translation.TAG_NAME)
public class Translation extends Span {
    public static final String TAG_NAME = "TRANSLATION";

    public Translation(Document document) {
        super(document);
        // Keep the registered tag name so HTML's closing-tag parser can match </translation>.
        this.tagName = TAG_NAME;
    }

    public String getTranslatedText() {
        return Component.translatable(super.getTextContent()).getString();
    }

    @Override
    public void drawPhase(PoseStack poseStack, Base.RenderPhase phase) {
        if (NormalFlow.isInlineTextPaintedByAncestor(this)) return;
        Rect rectRenderer = Rect.of(this);
        switch (phase) {
            case SHADOW -> rectRenderer.drawShadow(poseStack);
            case BODY -> {
                rectRenderer.drawBody(poseStack);
                drawStaticText(poseStack, rectRenderer, Text.of(this));
            }
            case BORDER -> {
                rectRenderer.drawBorder(poseStack);
            }
        }
    }
}

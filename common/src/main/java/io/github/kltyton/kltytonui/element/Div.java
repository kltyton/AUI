package io.github.kltyton.kltytonui.element;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;
import io.github.kltyton.kltytonui.render.Base;

@ElementRegister(Div.TAG_NAME)
public class Div extends Element {
    public static final String TAG_NAME = "DIV";

    public Div(Document document) {
        super(document, TAG_NAME);
    }

    @Override
    public void drawPhase(PoseStack poseStack, Base.RenderPhase phase) {
        super.drawPhase(poseStack, phase);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + children.size() + ")";
    }
}

package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;

@ElementRegister(Body.TAG_NAME)
public class Body extends Div {
    public static final String TAG_NAME = "BODY";

    public Body(Document document) {
        super(document);
        tagName = TAG_NAME;
    }
}

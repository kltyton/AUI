package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;

@ElementRegister(Head.TAG_NAME)
public class Head extends Div {
    public static final String TAG_NAME = "HEAD";

    public Head(Document document) {
        super(document);
        tagName = TAG_NAME;
        setAttribute("style", "display:none;");
    }
}

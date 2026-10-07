package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;

@ElementRegister(Pre.TAG_NAME)
public class Pre extends Element {
    public static final String TAG_NAME = "PRE";

    public Pre(Document document) {
        super(document, TAG_NAME);
    }

    @Override
    protected void onInitFromDom(Element origin) {
        // Keep line breaks for preformatted text.
        this.innerText = origin.innerText;
    }
}

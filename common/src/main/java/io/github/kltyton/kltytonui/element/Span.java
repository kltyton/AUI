package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;

@ElementRegister(Span.TAG_NAME)
public class Span extends Element {
    public static final String TAG_NAME = "SPAN";

    public Span(Document document) {
        super(document, TAG_NAME);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + innerText + ")";
    }
}

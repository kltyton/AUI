package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;

@ElementRegister(Option.TAG_NAME)
public class Option extends Element {
    public static final String TAG_NAME = "OPTION";

    public Option(Document document) {
        super(document, TAG_NAME);
    }
}

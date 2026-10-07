package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;
import io.github.kltyton.kltytonui.parser.HTML;

@ElementRegister(Html.TAG_NAME)
public class Html extends Div {
    public static final String TAG_NAME = "HTML";

    public Html(Document document) {
        super(document);
        tagName = TAG_NAME;
    }
}

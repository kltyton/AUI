package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;

@ElementRegister(Path.TAG_NAME)
public class Path extends Div {
    public static final String TAG_NAME = "PATH";

    public Path(Document document) {
        super(document);
        this.tagName = TAG_NAME;
    }
}

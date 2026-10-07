package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;
import io.github.kltyton.kltytonui.spi.KuiServices;

import java.net.URI;

@ElementRegister(A.TAG_NAME)
public class A extends Element {
    public static final String TAG_NAME = "A";

    public A(Document document) {
        super(document, TAG_NAME);
    }

    @Override
    protected boolean hasClickActivationBehavior() {
        String href = getAttribute("href");
        return href != null && !href.isBlank();
    }

    @Override
    public void handleClickDefault() {
        String href = getAttribute("href");
        if (href == null || href.isBlank()) return;
        try {
            KuiServices.client().openUri(new URI(href.trim()));
        } catch (Exception exception) {
            KltytonUI.LOGGER.warn("Failed to open href: {}", href, exception);
        }
    }
}

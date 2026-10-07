package io.github.kltyton.kltytonui.ui.toast;

import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.ui.ToastManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToastManagerTest {
    @Test
    void translationToastMountsLiveTranslationDomContent() {
        Document document = new Document("test://toast", false);
        Element message = ToastManager.createTranslationMessagePart(document, "ore_editor.kltytonui.notice.saved");
        Element translation = message.querySelector("TRANSLATION");

        assertNotNull(translation);
        assertEquals("TRANSLATION", translation.tagName);
        assertEquals("ore_editor.kltytonui.notice.saved", translation.getTextContent());
    }
}

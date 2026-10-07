package io.github.kltyton.kltytonui.style;

import io.github.kltyton.kltytonui.dom.TextNode;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.webapi.TestDocumentFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class TextContentFastPathTest {
    @Test
    void singleTextNodeReturnsItsStableStringWithoutBuilderAllocation() {
        Document document = TestDocumentFactory.createDocument();
        Element span = document.createElement("span");
        TextNode text = document.createTextNode("Selected: 50.00");
        span.appendChild(text);

        assertSame(text.getTextContent(), Text.resolveElementTextContent(span));
    }
}

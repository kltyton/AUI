package io.github.kltyton.kltytonui.webapi;

import io.github.kltyton.kltytonui.element.Body;
import io.github.kltyton.kltytonui.element.Head;
import io.github.kltyton.kltytonui.element.Html;
import io.github.kltyton.kltytonui.init.Document;

public final class TestDocumentFactory {
    private TestDocumentFactory() {
    }

    public static Document createDocument() {
        Document document = new Document("test://doc", false);
        document.documentElement = new Html(document);
        document.head = new Head(document);
        document.body = new Body(document);
        document.documentElement.appendChild(document.head);
        document.documentElement.appendChild(document.body);
        return document;
    }
}

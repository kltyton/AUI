package io.github.kltyton.kltytonui.dev.debug;

import com.google.gson.JsonObject;
import io.github.kltyton.kltytonui.element.Body;
import io.github.kltyton.kltytonui.element.Div;
import io.github.kltyton.kltytonui.element.Head;
import io.github.kltyton.kltytonui.element.Html;
import io.github.kltyton.kltytonui.element.AbstractText;
import io.github.kltyton.kltytonui.init.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.kltyton.kltytonui.parser.HTML;

class DebugDomTest {
    @Test
    void queriesLiveNodesAndRejectsDetachedIds() {
        Document document = document();
        Div card = new Div(document);
        card.setAttribute("class", "card");
        card.setAttribute("data-kind", "file");
        card.setTextContent("example");
        document.body.appendChild(card);

        assertEquals(card.uuid.toString(), DebugDom.query(document, ".card").get("nodeId").getAsString());
        assertEquals(1, DebugDom.queryAll(document, "[data-kind=file]").getAsJsonArray("nodeIds").size());
        assertEquals("example", DebugDom.text(card).get("text").getAsString());
        assertEquals("file", DebugDom.attributes(card).getAsJsonObject("attributes").get("data-kind").getAsString());

        String nodeId = card.uuid.toString();
        document.body.removeChild(card);
        DebugProtocolException error = assertThrows(DebugProtocolException.class,
                () -> DebugDom.requireElement(document, nodeId));
        assertEquals(DebugProtocolException.NODE_DETACHED, error.code());
    }

    @Test
    void snapshotEnforcesNodeLimit() {
        Document document = document();
        document.body.appendChild(new Div(document));

        JsonObject snapshot = DebugDom.snapshot(document, 32, 10);
        assertEquals("HTML", snapshot.getAsJsonObject("root").get("nodeName").getAsString());
        assertEquals(4, snapshot.get("nodeCount").getAsInt());

        DebugProtocolException error = assertThrows(DebugProtocolException.class,
                () -> DebugDom.snapshot(document, 32, 2));
        assertEquals(DebugProtocolException.LIMIT_EXCEEDED, error.code());
    }

    @Test
    void fillUsesEditableTextControlSemantics() {
        Document document = document();
        StubText input = new StubText(document);
        input.setValue("before");
        document.body.appendChild(input);

        assertEquals("after", DebugInput.fill(input, "after").get("value").getAsString());
        assertEquals("after", input.getValue());
        assertTrue(input.selectedAll);

        Div div = new Div(document);
        document.body.appendChild(div);
        DebugProtocolException error = assertThrows(DebugProtocolException.class,
                () -> DebugInput.fill(div, "invalid"));
        assertEquals(DebugProtocolException.NOT_ACTIONABLE, error.code());
    }

    @Test
    void missingQueryReturnsJsonNull() {
        assertTrue(DebugDom.query(document(), ".missing").get("nodeId").isJsonNull());
    }

    private static Document document() {
        Document document = new Document("test://debug", false);
        document.documentElement = new Html(document);
        document.head = new Head(document);
        document.body = new Body(document);
        document.documentElement.appendChild(document.head);
        document.documentElement.appendChild(document.body);
        return document;
    }

    private static final class StubText extends AbstractText {
        private boolean selectedAll;

        private StubText(Document document) {
            super(document, "input");
        }

        @Override
        public void selectAll() {
            selectedAll = true;
        }

        @Override
        public void replaceSelection(String value) {
            setValue(value);
        }
    }
}

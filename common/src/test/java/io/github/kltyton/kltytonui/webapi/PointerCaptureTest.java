package io.github.kltyton.kltytonui.webapi;

import io.github.kltyton.kltytonui.event.MouseEvent;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.layout.Position;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointerCaptureTest {
    @Test
    void capturedPointerMoveAndUpStayOnTheCapturingElement() {
        Document document = TestDocumentFactory.createDocument();
        Element captureTarget = document.createElement("div");
        Element outsideTarget = document.createElement("div");
        document.body.appendChild(captureTarget);
        document.body.appendChild(outsideTarget);
        AtomicInteger moves = new AtomicInteger();
        AtomicInteger ups = new AtomicInteger();
        AtomicInteger lost = new AtomicInteger();

        captureTarget.addEventListener("pointerdown", event -> captureTarget.setPointerCapture(1));
        captureTarget.addEventListener("pointermove", event -> moves.incrementAndGet());
        captureTarget.addEventListener("pointerup", event -> ups.incrementAndGet());
        captureTarget.addEventListener("lostpointercapture", event -> lost.incrementAndGet());

        MouseEvent.dispatchToTarget(new MouseEvent("mousedown", Position.ZERO, 0, false), document, captureTarget);
        assertTrue(captureTarget.hasPointerCapture(1));
        MouseEvent.dispatchToTarget(new MouseEvent("mousemove", new Position(100, 100), -1, false), document, outsideTarget);
        MouseEvent.dispatchToTarget(new MouseEvent("mouseup", new Position(100, 100), 0, false), document, outsideTarget);

        assertEquals(1, moves.get());
        assertEquals(1, ups.get());
        assertEquals(1, lost.get());
        assertFalse(captureTarget.hasPointerCapture(1));
    }
}

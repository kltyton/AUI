package io.github.kltyton.kltytonui.render;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.layout.LayoutMeasureCache;
import io.github.kltyton.kltytonui.style.Style;
import io.github.kltyton.kltytonui.webapi.TestDocumentFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryQueryScopeTest {
    @Test
    void nestedScopesKeepLayoutCachingActiveUntilTheOuterScopeCloses() {
        assertFalse(LayoutMeasureCache.isActive());
        try (GeometryQueryScope outer = GeometryQueryScope.open()) {
            assertTrue(LayoutMeasureCache.isActive());
            try (GeometryQueryScope inner = GeometryQueryScope.open()) {
                assertTrue(LayoutMeasureCache.isActive());
            }
            assertTrue(LayoutMeasureCache.isActive());
        }
        assertFalse(LayoutMeasureCache.isActive());
    }

    @Test
    void publicBoundingRectEnablesLayoutCachingForAnUncommittedElement() {
        Document document = TestDocumentFactory.createDocument();
        ProbeElement element = new ProbeElement(document);
        element.setAttribute("style", "width:40px;height:20px;");
        document.body.appendChild(element);

        element.getBoundingClientRect();

        assertTrue(element.observedLayoutScope);
        assertFalse(LayoutMeasureCache.isActive());
    }

    private static final class ProbeElement extends Element {
        private boolean observedLayoutScope;

        private ProbeElement(Document document) {
            super(document, "probe");
        }

        @Override
        public Style getComputedStyle() {
            observedLayoutScope |= LayoutMeasureCache.isActive();
            return super.getComputedStyle();
        }
    }
}

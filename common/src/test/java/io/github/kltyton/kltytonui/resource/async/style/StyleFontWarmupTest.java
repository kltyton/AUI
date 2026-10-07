package io.github.kltyton.kltytonui.resource.async.style;

import io.github.kltyton.kltytonui.layout.Size;
import io.github.kltyton.kltytonui.resource.Font;
import io.github.kltyton.kltytonui.task.AbstractAsyncHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleFontWarmupTest {
    @Test
    void localFontFacesLoadBeforeDocumentCreationAndOnlyOncePerGeneration() {
        AbstractAsyncHandler.clearAllAndBumpGeneration();
        Font.prepareReload();
        long before = Font.getMetricsRevision();

        int stylesheets = StyleAsyncHandler.INSTANCE.warmUpTemplateStyles(
                "kltytonui/theme/ore/mcui-example.html",
                List.of("../../runtime/mcui/components.css", "../../runtime/mcui/fonts.css",
                        "../../runtime/mcui/gallery.css"),
                List.of(),
                new Size(1920, 1080)
        );
        long afterFirstWarmup = Font.getMetricsRevision();

        StyleAsyncHandler.INSTANCE.warmUpTemplateStyles(
                "kltytonui/theme/ore/mcui-example.html",
                List.of("../../runtime/mcui/components.css", "../../runtime/mcui/fonts.css",
                        "../../runtime/mcui/gallery.css"),
                List.of(),
                new Size(1920, 1080)
        );

        assertEquals(4, stylesheets);
        assertTrue(afterFirstWarmup > before);
        assertEquals(afterFirstWarmup, Font.getMetricsRevision());
        assertTrue(Font.isRegistered("Minecraft Seven"));
        assertTrue(Font.isRegistered("Minecraft Ten"));
        assertTrue(Font.isRegistered("Minecraft Five"));
    }
}

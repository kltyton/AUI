package io.github.kltyton.kltytonui.webapi;

import io.github.kltyton.kltytonui.dev.resource.ResourcePreviewDialog;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.loader.Loader;
import io.github.kltyton.kltytonui.parser.HTML;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Closing the preview window through its own ✕ button must dispose the preview
 * document; otherwise it stays in the document registry and keeps showing up in
 * the DevTools document dropdown. Regression test for the missing onClose wiring.
 */
class ResourcePreviewDialogCloseTest {
    @Test
    void closingViaDialogButtonDisposesPreviewDocument() throws Exception {
        String path = "test://resource-preview-close-button";
        HTML.putTemple(path, "<body><div style=\"width:40px;height:20px;\"></div></body>");
        Loader.StaticResourceEntry entry = new Loader.StaticResourceEntry(
                path, "html", Loader.ResourceLayer.DEV_FOLDER, "", "", 1);

        Document owner = TestDocumentFactory.createDocument();
        setViewport(owner, 1280, 720);
        ResourcePreviewDialog previewDialog = new ResourcePreviewDialog();
        previewDialog.open(owner, entry);
        Document preview = previewDocument(previewDialog);
        try {
            assertNotNull(preview);
            assertTrue(Document.getAll().contains(preview));
            assertTrue(previewDialog.isOpen());

            Element closeButton = owner.querySelector(".resource-preview-window .resource-preview-close");
            assertNotNull(closeButton);
            closeButton.click();

            assertFalse(previewDialog.isOpen());
            assertTrue(preview.isDisposed());
            assertFalse(Document.getAll().contains(preview));
        } finally {
            previewDialog.close();
            owner.remove();
        }
    }

    @Test
    void closeIsSafeWhenInvokedTwiceThroughDialogCallback() throws Exception {
        String path = "test://resource-preview-close-twice";
        HTML.putTemple(path, "<body><div style=\"width:40px;height:20px;\"></div></body>");
        Loader.StaticResourceEntry entry = new Loader.StaticResourceEntry(
                path, "html", Loader.ResourceLayer.DEV_FOLDER, "", "", 1);

        Document owner = TestDocumentFactory.createDocument();
        setViewport(owner, 1280, 720);
        ResourcePreviewDialog previewDialog = new ResourcePreviewDialog();
        previewDialog.open(owner, entry);
        Document preview = previewDocument(previewDialog);
        try {
            assertNotNull(preview);
            // First close goes through the dialog callback; the explicit second
            // close must not throw or resurrect anything.
            previewDialog.close();
            previewDialog.close();
            assertTrue(preview.isDisposed());
            assertFalse(Document.getAll().contains(preview));
            assertFalse(previewDialog.isOpen());
        } finally {
            previewDialog.close();
            owner.remove();
        }
    }

    private static Document previewDocument(ResourcePreviewDialog dialog) throws Exception {
        Field field = ResourcePreviewDialog.class.getDeclaredField("preview");
        field.setAccessible(true);
        return (Document) field.get(dialog);
    }

    private static void setViewport(Document document, int width, int height) throws Exception {
        Field viewport = Document.class.getDeclaredField("viewport");
        viewport.setAccessible(true);
        viewport.set(document, new io.github.kltyton.kltytonui.viewport.KltytonViewport(width, height, 1.0f, 1.0d));
    }
}

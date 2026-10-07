package io.github.kltyton.kltytonui.dev.devtools;

import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.spi.KuiServices;
import io.github.kltyton.kltytonui.ui.DialogWindow;
import io.github.kltyton.kltytonui.ui.ToastManager;

import java.util.LinkedHashMap;
import java.util.Map;

/** Visual editor for the client-side Forge configuration used by KltytonUI. */
final class DevToolsConfigDialog {
    private final Map<String, Element> booleanInputs = new LinkedHashMap<>();
    private final Map<String, Element> numberInputs = new LinkedHashMap<>();
    private DialogWindow dialog;
    private Document document;

    void open(Document document) {
        close();
        if (document == null || document.body == null) return;
        this.document = document;
        this.dialog = DialogWindow.open(document, new DialogWindow.Options(
                DevToolsTranslations.translate("devtools.kltytonui.settings.title"), 720, 620, true,
                "dialog-overlay show", "dialog settings-dialog", "dialog-header", "dialog-title",
                "dialog-close", "dialog-body", "settings-dialog-title-icon"
        ), this::clearReferences);

        Element root = dialog.content();
        root.setAttribute("style", "position:relative;flex:1;min-height:0;display:flex;flex-direction:column;");
        Element scroll = DevToolsDom.element(document, "DIV", "settings-scroll");
        scroll.append(DevToolsDom.text(document, "DIV", "settings-note",
                DevToolsTranslations.translate("devtools.kltytonui.settings.description")));

        Element debug = appendSection(scroll, "devtools.kltytonui.settings.section.debug");
        Element debugGrid = appendGrid(debug);
        appendBooleanField(debugGrid, "debugAutoReload", "devtools.kltytonui.settings.debug_auto_reload",
                "devtools.kltytonui.settings.debug_auto_reload.description",
                KuiServices.config().debugAutoReload());
        appendBooleanField(debugGrid, "aiAutoScreenshot", "devtools.kltytonui.settings.ai_auto_screenshot",
                "devtools.kltytonui.settings.ai_auto_screenshot.description",
                KuiServices.config().aiAutoScreenshot());
        appendBooleanField(debugGrid, "frameTimingHud", "devtools.kltytonui.settings.frame_timing_hud",
                "devtools.kltytonui.settings.frame_timing_hud.description",
                KuiServices.config().frameTimingHud());
        appendBooleanField(debugGrid, "remoteDebug", "devtools.kltytonui.settings.remote_debug",
                "devtools.kltytonui.settings.remote_debug.description",
                KuiServices.config().remoteDebug());

        Element input = appendSection(scroll, "devtools.kltytonui.settings.section.input");
        Element inputGrid = appendGrid(input);
        appendBooleanField(inputGrid, "viewportZoomPassThrough",
                "devtools.kltytonui.settings.viewport_zoom_pass_through",
                "devtools.kltytonui.settings.viewport_zoom_pass_through.description",
                KuiServices.config().viewportZoomPassThrough());
        appendBooleanField(inputGrid, "blockMouseEventsWhenCursorHidden",
                "devtools.kltytonui.settings.block_mouse_events_when_cursor_hidden",
                "devtools.kltytonui.settings.block_mouse_events_when_cursor_hidden.description",
                KuiServices.config().blockMouseEventsWhenCursorHidden());

        Element worldWindow = appendSection(scroll, "devtools.kltytonui.settings.section.world_window");
        Element worldGrid = appendGrid(worldWindow);
        appendNumberField(worldGrid, "worldWindowDepthOffsetScale",
                "devtools.kltytonui.settings.world_window_depth_offset_scale",
                "devtools.kltytonui.settings.world_window_depth_offset_scale.description",
                Double.toString(KuiServices.config().worldWindowDepthOffsetScale()), "0", "1", "0.01");
        appendNumberField(worldGrid, "worldWindowMaxDisplayDistance",
                "devtools.kltytonui.settings.world_window_max_display_distance",
                "devtools.kltytonui.settings.world_window_max_display_distance.description",
                Integer.toString(KuiServices.config().worldWindowMaxDisplayDistance()), "0",
                Integer.toString(Integer.MAX_VALUE), "1");
        appendBooleanField(worldGrid, "worldWindowLodEnabled",
                "devtools.kltytonui.settings.world_window_lod_enabled",
                "devtools.kltytonui.settings.world_window_lod_enabled.description",
                KuiServices.config().worldWindowLodEnabled());
        appendNumberField(worldGrid, "worldWindowFullDetailDistance",
                "devtools.kltytonui.settings.world_window_full_detail_distance",
                "devtools.kltytonui.settings.world_window_full_detail_distance.description",
                Integer.toString(KuiServices.config().worldWindowFullDetailDistance()), "0",
                Integer.toString(Integer.MAX_VALUE), "1");
        appendNumberField(worldGrid, "worldWindowReducedDetailDistance",
                "devtools.kltytonui.settings.world_window_reduced_detail_distance",
                "devtools.kltytonui.settings.world_window_reduced_detail_distance.description",
                Integer.toString(KuiServices.config().worldWindowReducedDetailDistance()), "0",
                Integer.toString(Integer.MAX_VALUE), "1");

        root.append(scroll);
        Element footer = DevToolsDom.element(document, "DIV", "dialog-footer");
        Element cancel = button(document, "devtools.kltytonui.cancel", "dialog-btn dialog-btn-cancel");
        cancel.addEventListener("click", event -> close());
        Element save = button(document, "devtools.kltytonui.settings.save", "dialog-btn dialog-btn-confirm");
        save.addEventListener("click", event -> save());
        footer.append(cancel);
        footer.append(save);
        dialog.window().append(footer);
        DevToolsDom.markDirty(document);
    }

    void close() {
        DialogWindow current = dialog;
        dialog = null;
        if (current != null && current.isOpen()) current.close();
        clearReferences();
    }

    private Element appendSection(Element parent, String titleKey) {
        Element section = DevToolsDom.element(document, "SECTION", "settings-section");
        section.append(DevToolsDom.text(document, "H2", "settings-section-title",
                DevToolsTranslations.translate(titleKey)));
        parent.append(section);
        return section;
    }

    private Element appendGrid(Element section) {
        Element grid = DevToolsDom.element(document, "DIV", "settings-section-grid");
        section.append(grid);
        return grid;
    }

    private void appendBooleanField(Element parent, String key, String labelKey, String descriptionKey,
                                    boolean currentValue) {
        Element field = DevToolsDom.element(document, "LABEL", "settings-checkbox-field");
        Element input = DevToolsDom.element(document, "INPUT", "settings-checkbox-input");
        input.setAttribute("id", "kuiSetting-" + key);
        input.setAttribute("type", "checkbox");
        input.setChecked(currentValue);
        Element checkmark = DevToolsDom.element(document, "SPAN", "settings-checkbox-mark");
        Element copy = DevToolsDom.element(document, "SPAN", "settings-checkbox-copy");
        copy.append(DevToolsDom.text(document, "SPAN", "settings-field-label",
                DevToolsTranslations.translate(labelKey)));
        copy.append(DevToolsDom.text(document, "SPAN", "settings-field-description",
                DevToolsTranslations.translate(descriptionKey)));
        field.append(input);
        field.append(checkmark);
        field.append(copy);
        input.addEventListener("change", event -> DevToolsDom.markDirty(document));
        field.addEventListener("click", event -> {
            if (event.target != input) {
                input.setChecked(!input.isChecked());
                event.preventDefault();
                DevToolsDom.markDirty(document);
            }
        });
        parent.append(field);
        booleanInputs.put(key, input);
    }

    private void appendNumberField(Element parent, String key, String labelKey, String descriptionKey,
                                   String currentValue, String min, String max, String step) {
        Element field = DevToolsDom.element(document, "LABEL", "settings-number-field");
        Element copy = DevToolsDom.element(document, "SPAN", "settings-number-copy");
        copy.append(DevToolsDom.text(document, "SPAN", "settings-field-label",
                DevToolsTranslations.translate(labelKey)));
        copy.append(DevToolsDom.text(document, "SPAN", "settings-field-description",
                DevToolsTranslations.translate(descriptionKey)));
        Element input = DevToolsDom.element(document, "INPUT", "settings-number-input");
        input.setAttribute("id", "kuiSetting-" + key);
        input.setAttribute("type", "number");
        input.setAttribute("min", min);
        input.setAttribute("max", max);
        input.setAttribute("step", step);
        input.setValue(currentValue);
        input.addEventListener("input", event -> DevToolsDom.markDirty(document));
        field.append(copy);
        field.append(input);
        parent.append(field);
        numberInputs.put(key, input);
    }

    private Element button(Document document, String labelKey, String className) {
        Element button = DevToolsDom.element(document, "BUTTON", className);
        button.append(DevToolsDom.text(document, "SPAN", "dialog-btn-label",
                DevToolsTranslations.translate(labelKey)));
        return button;
    }

    private void save() {
        Double depthOffsetScale = readDouble("worldWindowDepthOffsetScale");
        Integer maxDisplayDistance = readInteger("worldWindowMaxDisplayDistance");
        Integer fullDetailDistance = readInteger("worldWindowFullDetailDistance");
        Integer reducedDetailDistance = readInteger("worldWindowReducedDetailDistance");
        if (depthOffsetScale == null || maxDisplayDistance == null || fullDetailDistance == null
                || reducedDetailDistance == null) {
            ToastManager.show(DevToolsTranslations.translate("devtools.kltytonui.settings.invalid_number"));
            return;
        }
        if (depthOffsetScale < 0.0d || depthOffsetScale > 1.0d) {
            ToastManager.show(DevToolsTranslations.translate("devtools.kltytonui.settings.depth_range"));
            return;
        }
        if (maxDisplayDistance < 0 || fullDetailDistance < 0 || reducedDetailDistance < 0) {
            ToastManager.show(DevToolsTranslations.translate("devtools.kltytonui.settings.distance_range"));
            return;
        }
        if (reducedDetailDistance < fullDetailDistance) {
            ToastManager.show(DevToolsTranslations.translate("devtools.kltytonui.settings.distance_order"));
            return;
        }

        try {
            KuiServices.config().setDebugAutoReload(isChecked("debugAutoReload"));
            KuiServices.config().setAiAutoScreenshot(isChecked("aiAutoScreenshot"));
            KuiServices.config().setFrameTimingHud(isChecked("frameTimingHud"));
            KuiServices.config().setRemoteDebug(isChecked("remoteDebug"));
            KuiServices.config().setViewportZoomPassThrough(isChecked("viewportZoomPassThrough"));
            KuiServices.config().setBlockMouseEventsWhenCursorHidden(isChecked("blockMouseEventsWhenCursorHidden"));
            KuiServices.config().setWorldWindowDepthOffsetScale(depthOffsetScale);
            KuiServices.config().setWorldWindowMaxDisplayDistance(maxDisplayDistance);
            KuiServices.config().setWorldWindowLodEnabled(isChecked("worldWindowLodEnabled"));
            KuiServices.config().setWorldWindowFullDetailDistance(fullDetailDistance);
            KuiServices.config().setWorldWindowReducedDetailDistance(reducedDetailDistance);
            KuiServices.config().save();
        } catch (RuntimeException exception) {
            ToastManager.show(DevToolsTranslations.translate("devtools.kltytonui.settings.save_failed"));
            return;
        }
        KuiServices.config().markClientReloadPending();
        close();
        ToastManager.show(DevToolsTranslations.translate("devtools.kltytonui.settings.saved"));
    }

    private boolean isChecked(String key) {
        Element input = booleanInputs.get(key);
        return input != null && input.isChecked();
    }

    private Double readDouble(String key) {
        String raw = valueOf(numberInputs.get(key));
        if (raw.isBlank()) return null;
        try {
            double value = Double.parseDouble(raw);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer readInteger(String key) {
        String raw = valueOf(numberInputs.get(key));
        if (raw.isBlank()) return null;
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String valueOf(Element input) {
        return input == null || input.getValue() == null ? "" : input.getValue().trim();
    }

    private void clearReferences() {
        dialog = null;
        document = null;
        booleanInputs.clear();
        numberInputs.clear();
    }
}

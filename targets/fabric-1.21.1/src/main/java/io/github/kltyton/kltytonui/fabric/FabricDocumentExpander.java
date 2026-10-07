package io.github.kltyton.kltytonui.fabric;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.dom.DocumentExpander;
import io.github.kltyton.kltytonui.dom.SlotContentRules;
import io.github.kltyton.kltytonui.dom.expander.ContainerExpander;
import io.github.kltyton.kltytonui.dom.expander.RecipeExpander;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Node;

public final class FabricDocumentExpander implements DocumentExpander {
    @Override
    public void apply(Document document) {
        if (document == null) return;
        String templatePath = document.getPath();
        try {
            SlotContentRules.normalizeTemplate(document);
        } catch (Exception exception) {
            KltytonUI.LOGGER.warn("SlotContentRules normalization failed, template={}", templatePath, exception);
        }
        try {
            ContainerExpander.expand(document);
        } catch (Exception exception) {
            KltytonUI.LOGGER.warn("ContainerExpander failed, template={}", templatePath, exception);
        }
        try {
            RecipeExpander.expand(document);
        } catch (Exception exception) {
            KltytonUI.LOGGER.warn("RecipeExpander failed, template={}", templatePath, exception);
        }
    }

    @Override
    public void validateRuntimeInsertion(Document document, Node parent, Node child) {
        SlotContentRules.validateRuntimeInsertion(parent, child);
    }

    @Override
    public void normalizeRuntimeChildren(Document document, Node parent) {
        SlotContentRules.normalizeRuntimeChildren(parent);
    }

    @Override
    public void restoreRequiredContent(Document document, Node parent) {
        SlotContentRules.restoreRequiredContent(parent);
    }
}

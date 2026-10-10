package io.github.kltyton.kltytonui.container.filter;

import io.github.kltyton.kltytonui.util.common.NormalizeUtil;

/**
 * Identifies a CSS selector scoped to one non-player container binding.
 */
public record ContainerSlotSelector(String containerId, String selector) {
    public ContainerSlotSelector {
        containerId = NormalizeUtil.normalizeContainerId(containerId);
        selector = selector == null ? null : selector.trim();
    }

    public boolean isValid() {
        return containerId != null && selector != null && !selector.isBlank();
    }
}

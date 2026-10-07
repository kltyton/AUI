package io.github.kltyton.kltytonui.element;

import io.github.kltyton.kltytonui.container.bind.ContainerBindType;

/**
 * Container declaration record describing one container in a parsed template.
 * Extracted by the loader-side {@code Container} element and sent to the server
 * when opening a container-bound screen.
 */
public record ContainerDeclaration(
        String id,
        ContainerBindType bindType,
        int capacity,
        boolean primary
) {
    public ContainerDeclaration {
        id = id == null ? "" : id.trim();
        bindType = bindType == null ? ContainerBindType.PLAYER : bindType;
        capacity = Math.max(0, capacity);
    }
}

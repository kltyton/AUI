package io.github.kltyton.kltytonui.spi;

/** Loader-side backend for rendering a Minecraft item from an KUI paint node. */
@FunctionalInterface
public interface KuiItemRenderService {
    void render(KuiItemRenderRequest request);

    /** Lets common paint code reject an empty platform stack before flushing KUI batches. */
    default boolean isEmptyStack(Object stack) {
        return stack == null;
    }
}

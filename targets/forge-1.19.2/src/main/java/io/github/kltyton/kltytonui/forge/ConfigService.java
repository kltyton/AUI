package io.github.kltyton.kltytonui.forge;

import io.github.kltyton.kltytonui.config.KltytonUIConfig;
import io.github.kltyton.kltytonui.spi.KuiConfigService;

/**
 * Forge implementation of {@link KuiConfigService}, backed by
 * {@link KltytonUIConfig}'s {@code ForgeConfigSpec}.
 */
public final class ConfigService implements KuiConfigService {
    public static final ConfigService INSTANCE = new ConfigService();

    private ConfigService() {
    }

    private static KltytonUIConfig.Client client() {
        return KltytonUIConfig.CLIENT;
    }

    @Override
    public boolean debugAutoReload() {
        return KltytonUIConfig.get(client().debugAutoReload);
    }

    @Override
    public void setDebugAutoReload(boolean value) {
        client().debugAutoReload.set(value);
    }

    @Override
    public boolean aiAutoScreenshot() {
        return KltytonUIConfig.get(client().aiAutoScreenshot);
    }

    @Override
    public void setAiAutoScreenshot(boolean value) {
        client().aiAutoScreenshot.set(value);
    }

    @Override
    public boolean frameTimingHud() {
        return KltytonUIConfig.get(client().frameTimingHud);
    }

    @Override
    public void setFrameTimingHud(boolean value) {
        client().frameTimingHud.set(value);
    }

    @Override
    public boolean remoteDebug() {
        return KltytonUIConfig.get(client().remoteDebug);
    }

    @Override
    public void setRemoteDebug(boolean value) {
        client().remoteDebug.set(value);
    }

    @Override
    public boolean viewportZoomPassThrough() {
        return KltytonUIConfig.get(client().viewportZoomPassThrough);
    }

    @Override
    public void setViewportZoomPassThrough(boolean value) {
        client().viewportZoomPassThrough.set(value);
    }

    @Override
    public boolean blockMouseEventsWhenCursorHidden() {
        return KltytonUIConfig.get(client().blockMouseEventsWhenCursorHidden);
    }

    @Override
    public void setBlockMouseEventsWhenCursorHidden(boolean value) {
        client().blockMouseEventsWhenCursorHidden.set(value);
    }

    @Override
    public float worldWindowDepthOffsetScale() {
        return client().worldWindowDepthOffsetScale();
    }

    @Override
    public void setWorldWindowDepthOffsetScale(double value) {
        client().worldWindowDepthOffsetScale.set(value);
    }

    @Override
    public int worldWindowMaxDisplayDistance() {
        return KltytonUIConfig.get(client().worldWindowMaxDisplayDistance);
    }

    @Override
    public void setWorldWindowMaxDisplayDistance(int value) {
        client().worldWindowMaxDisplayDistance.set(value);
    }

    @Override
    public boolean worldWindowLodEnabled() {
        return KltytonUIConfig.get(client().worldWindowLodEnabled);
    }

    @Override
    public void setWorldWindowLodEnabled(boolean value) {
        client().worldWindowLodEnabled.set(value);
    }

    @Override
    public int worldWindowFullDetailDistance() {
        return KltytonUIConfig.get(client().worldWindowFullDetailDistance);
    }

    @Override
    public void setWorldWindowFullDetailDistance(int value) {
        client().worldWindowFullDetailDistance.set(value);
    }

    @Override
    public int worldWindowReducedDetailDistance() {
        return KltytonUIConfig.get(client().worldWindowReducedDetailDistance);
    }

    @Override
    public void setWorldWindowReducedDetailDistance(int value) {
        client().worldWindowReducedDetailDistance.set(value);
    }

    @Override
    public boolean initialCommitSliceEnabled() {
        return KltytonUIConfig.get(client().initialCommitSliceEnabled);
    }

    @Override
    public void setInitialCommitSliceEnabled(boolean value) {
        client().initialCommitSliceEnabled.set(value);
    }

    @Override
    public float initialCommitSliceMs() {
        return KltytonUIConfig.get(client().initialCommitSliceMs).floatValue();
    }

    @Override
    public void setInitialCommitSliceMs(double value) {
        client().initialCommitSliceMs.set(value);
    }

    @Override
    public void save() {
        KltytonUIConfig.CLIENT_SPEC.save();
    }

    @Override
    public void markClientReloadPending() {
        KltytonUIConfig.markClientReloadPending();
    }

    @Override
    public boolean consumeClientReloadPending() {
        return KltytonUIConfig.consumeClientReloadPending();
    }
}

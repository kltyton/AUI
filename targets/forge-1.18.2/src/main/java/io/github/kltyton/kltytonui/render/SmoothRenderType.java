package io.github.kltyton.kltytonui.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Image render type used by {@link io.github.kltyton.kltytonui.render.ImageDrawer}.
 *
 * <p>The {@link RenderType} composite state (texture shard, shader, depth and
 * transparency) is Minecraft-version-specific, so it lives in the loader target
 * and is exposed to {@code common} through
 * {@link io.github.kltyton.kltytonui.spi.KuiResourceService#smoothRenderType}.</p>
 */
public final class SmoothRenderType extends RenderType {
    private SmoothRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                             boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType createSmooth(ResourceLocation location, boolean blur, boolean depthTest) {
        return create("kltytonui_image",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                CompositeState.builder()
                        .setTextureState(new TextureStateShard(location, blur, false))
                        .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }
}

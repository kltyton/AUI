package io.github.kltyton.kltytonui.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches immutable 26.2 {@link RenderType}s for KUI's immediate-mode meshes.
 *
 * <p>1.21.5 removed the mutable global render state ({@code RenderSystem}
 * blend/depth/stencil setters) in favour of per-draw {@link RenderPipeline}
 * objects, so the state intent that common code expresses through
 * {@code KuiRenderService} is materialised here as cached pipeline/render-type
 * combinations.</p>
 *
 * <p>Stencil is deliberately absent from the key: vanilla 26.2 pipelines cannot
 * carry it (NeoForge patches stencil into {@code DepthStencilState}), and the
 * Fabric backend reports stencil as unavailable, so masks degrade to the
 * scissor path there.</p>
 */
public final class PipelineCache {
    private static final Map<Key, RenderType> TYPES = new ConcurrentHashMap<>();

    private PipelineCache() {
    }

    public static RenderType renderType(VertexFormat format, PrimitiveTopology mode,
                                        boolean depthTest, int depthFunc, boolean depthMask,
                                        boolean blend, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha,
                                        boolean cull, boolean polygonOffset, float biasScale, float biasUnits,
                                        int colorWriteMask) {
        Key key = new Key(format, mode, depthTest, depthFunc, depthMask, blend,
                srcRgb, dstRgb, srcAlpha, dstAlpha, cull, polygonOffset, biasScale, biasUnits,
                colorWriteMask);
        return TYPES.computeIfAbsent(key, PipelineCache::build);
    }

    private static RenderType build(Key key) {
        String shader = shaderFor(key.format());
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withLocation(Identifier.fromNamespaceAndPath("kltytonui", "pipeline/mesh_" + Integer.toHexString(key.hashCode())))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withVertexBinding(0, key.format())
                .withPrimitiveTopology(key.mode())
                .withColorTargetState(new ColorTargetState(
                        key.blend() ? Optional.of(blendFunction(key)) : Optional.empty(),
                        GpuFormat.RGBA8_UNORM,
                        key.colorWriteMask()))
                .withCull(key.cull());

        if (key.depthTest() || key.polygonOffset()) {
            builder.withDepthStencilState(new DepthStencilState(
                    key.depthTest() ? compareOp(key.depthFunc()) : CompareOp.ALWAYS_PASS,
                    key.depthMask(),
                    key.polygonOffset() ? key.biasScale() : 0.0f,
                    key.polygonOffset() ? key.biasUnits() : 0.0f));
        } else {
            builder.withDepthStencilState(Optional.empty());
        }

        RenderPipeline pipeline = builder.build();
        return RenderType.create(
                "kltytonui_mesh_" + Integer.toHexString(key.hashCode()),
                RenderSetup.builder(pipeline)
                        .setOutputTarget(OutputTargets.KUI_OUTPUT)
                        .createRenderSetup());
    }

    private static String shaderFor(VertexFormat format) {
        if (format == DefaultVertexFormat.POSITION) return "core/position";
        if (format == DefaultVertexFormat.POSITION_TEX) return "core/position_tex";
        return "core/gui";
    }

    private static CompareOp compareOp(int func) {
        return switch (func) {
            case 512 -> CompareOp.NEVER_PASS;
            case 513 -> CompareOp.GREATER_THAN;
            case 514 -> CompareOp.EQUAL;
            case 516 -> CompareOp.LESS_THAN;
            case 517 -> CompareOp.NOT_EQUAL;
            case 518 -> CompareOp.LESS_THAN_OR_EQUAL;
            case 519 -> CompareOp.ALWAYS_PASS;
            default -> CompareOp.GREATER_THAN_OR_EQUAL;
        };
    }

    private static BlendFunction blendFunction(Key key) {
        return new BlendFunction(sourceFactor(key.srcRgb()), destinationFactor(key.dstRgb()),
                sourceFactor(key.srcAlpha()), destinationFactor(key.dstAlpha()));
    }

    private static BlendFactor sourceFactor(int value) {
        return switch (value) {
            case 0 -> BlendFactor.ZERO;
            case 1 -> BlendFactor.ONE;
            case 768 -> BlendFactor.SRC_COLOR;
            case 769 -> BlendFactor.ONE_MINUS_SRC_COLOR;
            case 770 -> BlendFactor.SRC_ALPHA;
            case 771 -> BlendFactor.ONE_MINUS_SRC_ALPHA;
            case 772 -> BlendFactor.DST_ALPHA;
            case 773 -> BlendFactor.ONE_MINUS_DST_ALPHA;
            case 774 -> BlendFactor.DST_COLOR;
            case 775 -> BlendFactor.ONE_MINUS_DST_COLOR;
            default -> BlendFactor.SRC_ALPHA;
        };
    }

    private static BlendFactor destinationFactor(int value) {
        return switch (value) {
            case 0 -> BlendFactor.ZERO;
            case 1 -> BlendFactor.ONE;
            case 768 -> BlendFactor.SRC_COLOR;
            case 769 -> BlendFactor.ONE_MINUS_SRC_COLOR;
            case 770 -> BlendFactor.SRC_ALPHA;
            case 771 -> BlendFactor.ONE_MINUS_SRC_ALPHA;
            case 772 -> BlendFactor.DST_ALPHA;
            case 773 -> BlendFactor.ONE_MINUS_DST_ALPHA;
            case 774 -> BlendFactor.DST_COLOR;
            case 775 -> BlendFactor.ONE_MINUS_DST_COLOR;
            default -> BlendFactor.ONE_MINUS_SRC_ALPHA;
        };
    }

    private record Key(VertexFormat format, PrimitiveTopology mode, boolean depthTest, int depthFunc,
                       boolean depthMask, boolean blend, int srcRgb, int dstRgb, int srcAlpha,
                       int dstAlpha, boolean cull, boolean polygonOffset, float biasScale, float biasUnits,
                       int colorWriteMask) {
    }
}

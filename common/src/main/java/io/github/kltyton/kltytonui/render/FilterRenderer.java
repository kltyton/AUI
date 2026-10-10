package io.github.kltyton.kltytonui.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.spi.KuiServices;
import io.github.kltyton.kltytonui.spi.KuiRenderService;
import io.github.kltyton.kltytonui.spi.FboHandle;
import io.github.kltyton.kltytonui.spi.MeshBuilder;
import io.github.kltyton.kltytonui.spi.MeshFormat;
import io.github.kltyton.kltytonui.spi.MeshMode;
import io.github.kltyton.kltytonui.style.Filter;
import io.github.kltyton.kltytonui.style.MaskImage;
import io.github.kltyton.kltytonui.layout.Position;
import io.github.kltyton.kltytonui.layout.Size;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

public class FilterRenderer {
    private static final Stack<FboHandle> fboStack = new Stack<>();
    private static FboHandle mainRenderTarget;
    private static final List<FboHandle> fboPool = new ArrayList<>();
    private static int poolPointer = 0;
    private static final List<FboHandle> backdropPool = new ArrayList<>();
    private static int backdropPoolPointer = 0;
    /** Optional external backdrop used by mix-blend-mode and backdrop-filter. */
    private static FboHandle compositingReference;
    private static final float MAX_REASONABLE_BACKDROP_BLUR = 32.0f;
    private static boolean stencilCapabilityResolved;
    private static boolean stencilAvailable = true;

    /**
     * Returns whether KUI may allocate/use a stencil attachment for the
     * current GL context. Desktop OpenGL keeps the existing path; GLES is
     * conservatively treated as unavailable because some Android drivers
     * reject Minecraft's depth/stencil framebuffer combination.
     */
    public static boolean isStencilAvailable() {
        resolveStencilCapability();
        return stencilAvailable;
    }

    private static void resolveStencilCapability() {
        if (stencilCapabilityResolved) return;

        // Resolve lazily from the render thread, after a context exists. If a
        // test/headless context cannot answer the query, preserve the desktop
        // behavior rather than disabling stencil for the whole process.
        stencilCapabilityResolved = true;
        try {
            if (!KuiServices.render().supportsStencil()) {
                stencilAvailable = false;
                KltytonUI.LOGGER.warn("[KltytonUI] stencil masks are unavailable on this render backend; using scissor fallback");
                return;
            }
            String version = KuiServices.render().getGLVersionString();
            if (version != null && version.toLowerCase(Locale.ROOT).contains("opengl es")) {
                stencilAvailable = false;
                KltytonUI.LOGGER.warn(
                        "[KltytonUI] OpenGL ES detected ({}); disabling stencil-backed masks for compatibility",
                        version
                );
            }
        } catch (RuntimeException ignored) {
            stencilAvailable = true;
        }
    }

    public static void beginFrame() {
        warnOffscreenLeak("beginFrame");
        // 防御式清理：若上帧因异常或节点错配残留栈，避免 poolPointer 无界增长
        if (!fboStack.isEmpty()) {
            fboStack.clear();
        }
        mainRenderTarget = KuiServices.render().getMainRenderTarget();
        poolPointer = 0;
        backdropPoolPointer = 0;
        // The reference is a frame-scoped backdrop source. Holding an FBO
        // from the previous frame can sample a destroyed or resized target.
        compositingReference = null;
    }

    public static void endFrame() {
        warnOffscreenLeak("endFrame");
        if (!fboStack.isEmpty()) {
            fboStack.clear();
            if (mainRenderTarget != null) {
                KuiServices.render().bindWrite(mainRenderTarget, false);
            }
        }
        compositingReference = null;
    }

    public static void pushFilter() {
        // Pending parent draws must land in the parent target before the child
        // filter binds its offscreen target. Otherwise they inherit the child's opacity.
        // 用 commitDraws 而不是只刷 Graph/贴图队列：默认字体的文本由 Font.drawInBatch
        // 写进加载器的共享顶点缓冲，漏掉它本层文字就留在合成层之外。
        Base.commitDraws();

        if (fboStack.isEmpty()) {
            mainRenderTarget = KuiServices.render().getMainRenderTarget();
            poolPointer = 0;
        }

        FboHandle temp;
        double width = KuiServices.client().getWindowWidth();
        double height = KuiServices.client().getWindowHeight();

        if (poolPointer < fboPool.size()) {
            temp = fboPool.get(poolPointer);
            if (temp.width != (int) width || temp.height != (int) height) {
                KuiServices.render().destroyBuffers(temp);
                temp = KuiServices.render().createOffscreenTarget((int) width, (int) height, true);
                fboPool.set(poolPointer, temp);
            }
        } else {
            temp = KuiServices.render().createOffscreenTarget((int) width, (int) height, true);
            fboPool.add(temp);
        }
        poolPointer++;

        // 注意：这里的 clear 会清除当前绑定的 FBO 的缓冲区
        KuiServices.render().clear(temp, 0f, 0f, 0f, 0f);
        fboStack.push(temp);
        KuiServices.render().bindWrite(temp, false);
    }

    /**
     * 把离屏合成层的静默丢弃变成可观测信号。这条路径会吞掉子树内容且不留任何日志，
     * 表现为“卡片整张或局部缺失”却查不到原因。默认关闭；
     * 开 {@code -Dkltytonui.test.logRenderPhases=true} 即可印出具体层数。
     */
    private static void warnOffscreenLeak(String phase) {
        if (fboStack.isEmpty()) return;
        if (!Boolean.getBoolean("kltytonui.test.logRenderPhases")) return;
        io.github.kltyton.kltytonui.KltytonUI.LOGGER.warn(
                "[KUI Filter] offscreen compositing layer leaked at {}: depth={} (subtree content will be dropped)",
                phase,
                fboStack.size()
        );
    }

    public static FboHandle getCurrentTarget() {
        return fboStack.isEmpty() ? KuiServices.render().getMainRenderTarget() : fboStack.peek();
    }

    /**
     * CSS mask 合成。调用前 {@link #pushFilter()} 已把内容子树切到离屏 FBO C。
     * 这里把 mask 层画进第二个池化 FBO M（M 的透明区域即被遮掉的区域），
     * 用 dst-in 混合（C 是预乘 alpha，乘以 M 的 mask 值即挖空）写回 C，
     * 最后以恒等 filter 把 C 合成回父目标。mask 值取 alpha 还是 luminance
     * 由 {@link MaskImage#effectiveLuminance} 决定（混合 mode 按 alpha）。
     *
     * <p>加载失败/未就绪导致一层都画不上时跳过 dst-in，内容保持可见
     * （fail-open，与浏览器"遮罩失败=全遮掉"的行为不同，见文档）。</p>
     */
    public static void popMaskImage(Element target, PoseStack poseStack) {
        if (fboStack.isEmpty()) return;

        // 与 popFilter 相同：先把子树剩余的批处理绘制落进内容 FBO（含共享顶点缓冲里的文本）
        Base.commitDraws();

        List<MaskImage.ResolvedLayer> layers = MaskImage.layersOf(target);

        // 注意顺序：必须在内容 FBO C 出栈之前取 mask 画布 M。
        // 根级 mask 下栈一空 pushFilter 会把 poolPointer 归零，M 可能复用到 C 本身。
        pushFilter();
        FboHandle maskFbo = fboStack.peek();
        boolean painted;
        try {
            painted = paintMaskLayers(target, poseStack, layers, maskFbo);
        } finally {
            ImageDrawer.flushBatch();
            Graph.endBatch();
            // mask 画布用完即出栈（不做 filter 合成）；stencil/scissor 状态
            // 已由 MaskImagePainter 内部的 Mask.push/pop 成对恢复
            fboStack.pop();
        }

        FboHandle contentFbo = fboStack.pop();
        FboHandle parentFbo = fboStack.isEmpty() ? mainRenderTarget : fboStack.peek();
        try {
            if (painted) {
                KuiServices.render().bindWrite(contentFbo, true);
                drawMaskBlit(maskFbo, MaskImage.effectiveLuminance(layers));
            }
            KuiServices.render().bindWrite(parentFbo, true);
            drawWithShader(contentFbo, contentFbo, Filter.FilterState.EMPTY, 1.0f);
        } finally {
            if (parentFbo != null) KuiServices.render().bindWrite(parentFbo, true);
        }
    }

    /**
     * 自下而上逐层累积 mask 画布 M：add 层直接以标准半透明（source-over）画进 M；
     * intersect/subtract/exclude 层先画进 scratch FBO L（栈上有 M，pushFilter 不会
     * 重置 poolPointer，L 必然是新 FBO），再以对应 Porter-Duff 混合 merge 回 M。
     * 最底层的 composite 值没有意义（下方无可合成对象），按 add 处理。
     */
    private static boolean paintMaskLayers(Element target, PoseStack poseStack,
                                           List<MaskImage.ResolvedLayer> layers, FboHandle maskFbo) {
        boolean any = false;
        for (int i = layers.size() - 1; i >= 0; i--) {
            MaskImage.ResolvedLayer layer = layers.get(i);
            KuiRenderService.MaskCompositeOp op = mergeOpOf(layer.composite());
            if (op == null || i == layers.size() - 1) {
                any |= MaskImagePainter.paintLayer(target, poseStack, layer);
                continue;
            }
            pushFilter();
            FboHandle scratchFbo = fboStack.peek();
            boolean scratchPainted;
            try {
                scratchPainted = MaskImagePainter.paintLayer(target, poseStack, layer);
            } finally {
                ImageDrawer.flushBatch();
                Graph.endBatch();
                fboStack.pop();
            }
            if (scratchPainted) {
                KuiServices.render().bindWrite(maskFbo, true);
                drawMaskMergeBlit(scratchFbo, op);
                any = true;
            }
        }
        return any;
    }

    /** 非 add 的 composite 值映射到 Porter-Duff merge 算子；add/未知值返回 null。 */
    private static KuiRenderService.MaskCompositeOp mergeOpOf(String composite) {
        return switch (composite) {
            case "intersect" -> KuiRenderService.MaskCompositeOp.INTERSECT;
            case "subtract" -> KuiRenderService.MaskCompositeOp.SUBTRACT;
            case "exclude" -> KuiRenderService.MaskCompositeOp.EXCLUDE;
            default -> null;
        };
    }

    /**
     * dst-in 全窗 blit：dest(C) *= src(M) 的 mask 值（alpha 或 luminance）。
     * legacy 端由 filter_mask 着色器 JSON 自带 zero/srcalpha 混合
     * （ShaderInstance.apply 会覆盖动态混合状态，不能靠 setBlendFuncSeparate），
     * 26.1 端由 filter_mask pipeline 烘焙同款混合。
     */
    private static void drawMaskBlit(FboHandle maskFbo, boolean luminance) {
        Object shader = KuiServices.render().getFilterMaskShader(luminance);
        if (shader == null) return; // 后端未提供 mask shader 时 fail-open

        withBlendRenderState(false, () -> {
            Base.setShader(shader);
            KuiServices.render().bindColorTexture(maskFbo, 0);
            Base.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            KuiServices.render().setShaderUniformFloat("MaskLuminance", luminance ? 1.0f : 0.0f);

            float guiW = (float) KuiServices.client().getScaledWidth();
            float guiH = (float) KuiServices.client().getScaledHeight();
            Base.setProjectionMatrix(orthoProjection(guiW, guiH));

            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            mesh.vertexUV(identity, 0, guiH, 0, 0, 0);
            mesh.vertexUV(identity, guiW, guiH, 0, 1, 0);
            mesh.vertexUV(identity, guiW, 0, 0, 1, 1);
            mesh.vertexUV(identity, 0, 0, 0, 0, 1);
            mesh.submit();
        });
    }

    /**
     * mask-composite merge 全窗 blit：L(src) 按 Porter-Duff 算子合成进 M(dst)。
     * 算子完全由后端烘焙的混合状态表达（source-in / source-out / xor），
     * 着色器只做 Sampler0 透传。
     */
    private static void drawMaskMergeBlit(FboHandle scratchFbo, KuiRenderService.MaskCompositeOp op) {
        Object shader = KuiServices.render().getFilterMaskMergeShader(op);
        if (shader == null) return; // 后端不支持 merge 时丢弃该层（fail-open）

        withBlendRenderState(false, () -> {
            Base.setShader(shader);
            KuiServices.render().bindColorTexture(scratchFbo, 0);
            Base.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            float guiW = (float) KuiServices.client().getScaledWidth();
            float guiH = (float) KuiServices.client().getScaledHeight();
            Base.setProjectionMatrix(orthoProjection(guiW, guiH));

            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            mesh.vertexUV(identity, 0, guiH, 0, 0, 0);
            mesh.vertexUV(identity, guiW, guiH, 0, 1, 0);
            mesh.vertexUV(identity, guiW, 0, 0, 1, 1);
            mesh.vertexUV(identity, 0, 0, 0, 0, 1);
            mesh.submit();
        });
    }

    public static void popFilter(Filter.FilterState state) {
        popFilter(state, 1.0f);
    }

    /**
     * Supplies the framebuffer that CSS compositing effects should sample as
     * their backdrop. The reference is never written by KUI; it is copied to
     * an internal snapshot immediately before each pass, which makes it safe
     * to point at the currently bound parent target.
     */
    public static void setCompositingReference(FboHandle reference) {
        compositingReference = reference;
    }

    /** Clears the external compositing reference and restores parent sampling. */
    public static void clearCompositingReference() {
        compositingReference = null;
    }

    /** Returns the currently configured external compositing reference. */
    public static FboHandle getCompositingReference() {
        return compositingReference;
    }

    public static void popFilter(Filter.FilterState state, float dynamicRangeLimit) {
        if (fboStack.isEmpty()) return;

        // 在切回父 FBO 之前 flush 批处理绘制，使 batched draw calls
        // 先写入当前离屏 FBO，避免绕过 filter/opacity 合成。
        // commitDraws 覆盖了共享顶点缓冲：默认字体的文本不在 Graph/贴图队列里，
        // 只刷那两个后端会让本层文字漏到合成层外面。
        Base.commitDraws();

        FboHandle currentFbo = fboStack.pop();
        FboHandle parentFbo = fboStack.isEmpty() ? mainRenderTarget : fboStack.peek();
        try {
            FboHandle filteredFbo = prepareFullFilterSource(currentFbo, state.blurRadius());
            FboHandle shadowFbo = state.hasDropShadow()
                    ? prepareFullFilterSource(currentFbo, state.dropShadowBlur()) : currentFbo;
            KuiServices.render().bindWrite(parentFbo, true);
            drawWithShader(filteredFbo, shadowFbo, state, dynamicRangeLimit);
        } finally {
            if (parentFbo != null) KuiServices.render().bindWrite(parentFbo, true);
        }
    }

    /**
     * Composites an isolated element layer using CSS Compositing and Blending
     * Level 1. Both textures are sampled by the shader; fixed-function
     * blending is disabled so darken/lighten/HSL operators and partial alpha
     * use the same formula on every backend.
     */
    public static void popBlend(String mode) {
        if (fboStack.isEmpty()) return;
        // 合成前把本层待提交的绘制落进离屏目标；共享顶点缓冲（默认字体文本）也在其中。
        Base.commitDraws();
        FboHandle source = fboStack.pop();
        FboHandle parent = fboStack.isEmpty() ? mainRenderTarget : fboStack.peek();
        if (parent == null) return;

        FboHandle reference = compositingReference != null ? compositingReference : parent;
        FboHandle backdrop = snapshotReference(reference);
        KuiServices.render().bindWrite(parent, true);
        drawBlend(source, backdrop, mode);
    }

    private static FboHandle snapshotReference(FboHandle reference) {
        if (reference == null || reference.width <= 0 || reference.height <= 0) return null;
        FboHandle snapshot = acquireBackdropTarget(reference.width, reference.height);
        blitRegion(reference, snapshot, 0, 0, reference.width, reference.height);
        return snapshot;
    }

    private static void drawBlend(FboHandle source, FboHandle backdrop, String mode) {
        Object shader = KuiServices.render().getFilterBlendShader();
        if (shader == null || backdrop == null) {
            // Third-party bridges compiled before the blend SPI can still
            // display the layer with ordinary source-over compositing.
            drawSourceOver(source);
            return;
        }

        withBlendRenderState(false, () -> {
            Base.setShader(shader);
            KuiServices.render().setShaderUniformFloat("BlendMode", CssBlendMode.id(mode));
            KuiServices.render().bindColorTexture(source, 0);
            if (backdrop != null) KuiServices.render().bindColorTexture(backdrop, 1);
            Base.setShaderColor(1, 1, 1, 1);

            float width = (float) KuiServices.client().getScaledWidth();
            float height = (float) KuiServices.client().getScaledHeight();
            Base.setProjectionMatrix(orthoProjection(width, height));
            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            mesh.vertexUV(identity, 0, height, 0, 0, 0);
            mesh.vertexUV(identity, width, height, 0, 1, 0);
            mesh.vertexUV(identity, width, 0, 0, 1, 1);
            mesh.vertexUV(identity, 0, 0, 0, 0, 1);
            mesh.submit();
        });
    }

    private static void drawSourceOver(FboHandle source) {
        Object shader = KuiServices.render().getFilterShader();
        if (shader == null) return;
        withBlendRenderState(true, () -> {
            Base.setShader(shader);
            setupUniforms(shader, Filter.FilterState.EMPTY, source, false, true, 1.0f,
                    1.0f / Math.max(1, KuiServices.client().getScaledWidth()),
                    1.0f / Math.max(1, KuiServices.client().getScaledHeight()));
            KuiServices.render().bindColorTexture(source, 0);
            Base.setShaderColor(1, 1, 1, 1);
            float width = (float) KuiServices.client().getScaledWidth();
            float height = (float) KuiServices.client().getScaledHeight();
            Base.setProjectionMatrix(orthoProjection(width, height));
            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            mesh.vertexUV(identity, 0, height, 0, 0, 0);
            mesh.vertexUV(identity, width, height, 0, 1, 0);
            mesh.vertexUV(identity, width, 0, 0, 1, 1);
            mesh.vertexUV(identity, 0, 0, 0, 0, 1);
            mesh.submit();
        });
    }

    /** Runs a shader pass with the standard filter blend/depth state. */
    private static void withBlendRenderState(boolean enableBlend, Runnable body) {
        Matrix4f oldProjection = new Matrix4f(Base.getProjectionMatrix());
        KuiRenderService.RenderStateScope scope = KuiServices.render().pushFilterRenderState();
        if (scope == null) scope = KuiRenderService.RenderStateScope.NOOP;
        if (enableBlend) {
            KuiServices.render().enableBlend();
            KuiServices.render().setBlendFuncSeparate(
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA
            );
        } else {
            KuiServices.render().disableBlend();
        }
        KuiServices.render().disableDepthTest();
        KuiServices.render().setDepthMask(false);
        KuiServices.render().disableCull();
        try {
            body.run();
        } finally {
            try {
                KuiServices.render().setDepthMask(true);
                if (Base.isDepthTestEnabled()) KuiServices.render().enableDepthTest();
                else KuiServices.render().disableDepthTest();
                Base.setProjectionMatrix(oldProjection);
            } finally {
                scope.close();
            }
        }
    }

    private static void drawWithShader(FboHandle fbo, FboHandle shadowFbo, Filter.FilterState state,
                                       float dynamicRangeLimit) {
        Object shader = KuiServices.render().getFilterShader();

        withBlendRenderState(true, () -> {
            if (shader == null) {
                Base.setPositionColorShader();
            } else {
                Base.setShader(shader);
                // Blur is precomputed as two separable passes. The composite shader
                // only applies the inexpensive color/opacity/shadow operations.
                setupUniforms(shader, state, fbo, false, true, dynamicRangeLimit,
                        1.0f / Math.max(1, KuiServices.client().getScaledWidth()),
                        1.0f / Math.max(1, KuiServices.client().getScaledHeight()));
            }

            KuiServices.render().bindColorTexture(fbo, 0);
            KuiServices.render().bindColorTexture(shadowFbo, 1);
            Base.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            float guiW = (float) KuiServices.client().getScaledWidth();
            float guiH = (float) KuiServices.client().getScaledHeight();
            Base.setProjectionMatrix(orthoProjection(guiW, guiH));

            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            mesh.vertexUV(identity, 0, guiH, 0, 0, 0);
            mesh.vertexUV(identity, guiW, guiH, 0, 1, 0);
            mesh.vertexUV(identity, guiW, 0, 0, 1, 1);
            mesh.vertexUV(identity, 0, 0, 0, 0, 1);
            mesh.submit();
        });
    }

    public static void renderBackdrop(Element target, PoseStack poseStack) {
        // A backdrop snapshot must include every draw submitted before this
        // element. It also creates a natural batch boundary for the FBO copy.
        Base.commitDraws();

        FboHandle destination = fboStack.isEmpty() ? KuiServices.render().getMainRenderTarget() : fboStack.peek();
        FboHandle sampleSource = compositingReference != null ? compositingReference : destination;
        Filter.FilterState state = Filter.getBackdropFilterOf(target);
        Rect rect = Rect.of(target);
        float[] layout = layoutSize(target);
        try {
            BackdropSource source = prepareBackdropSource(sampleSource, rect, state.blurRadius(), layout[0], layout[1]);
            if (source == null) return;
            FboHandle shadowTarget = prepareBackdropShadow(source, state);

            KuiServices.render().bindWrite(destination, true);
            drawBackdropWithShader(source, shadowTarget, state, rect, layout[0], layout[1]);
        } finally {
            if (destination != null) KuiServices.render().bindWrite(destination, true);
        }
    }

    private static void drawBackdropWithShader(BackdropSource source, FboHandle shadowTarget,
                                               Filter.FilterState state, Rect rect,
                                               float layoutW, float layoutH) {
        Object shader = KuiServices.render().getFilterShader();
        if (shader == null) return;

        withBlendRenderState(true, () -> {
            Position p = rect.getBodyRectPosition();
            Size s = rect.getBodyRectSize();

            // The element rect lives in the document's layout space, so the ortho
            // projection and the clip uniforms must use that same space. Using the
            // GUI-scaled window size here would scale the quad (and the sampled UVs)
            // by layoutSize/guiSize, which magnifies the backdrop instead of blurring
            // it and paints its top-left corner over the element.
            Base.setProjectionMatrix(orthoProjection(layoutW, layoutH));

            Base.setShader(shader);
            setupUniforms(shader, state, source.target(), true, true, 1.0f, source.uvPerGuiX(), source.uvPerGuiY());
            setupBackdropClipUniforms(shader, rect, layoutW, layoutH);
            KuiServices.render().bindColorTexture(source.target(), 0);
            KuiServices.render().bindColorTexture(shadowTarget, 1);
            Base.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Base.setProjectionMatrix(orthoProjection(layoutW, layoutH));

            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            float x0 = (float) p.x;
            float y0 = (float) p.y;
            float x1 = x0 + (float) s.width();
            float y1 = y0 + (float) s.height();

            mesh.vertexUV(identity, x0, y1, 0, source.u0(), source.vBottom());
            mesh.vertexUV(identity, x1, y1, 0, source.u1(), source.vBottom());
            mesh.vertexUV(identity, x1, y0, 0, source.u1(), source.vTop());
            mesh.vertexUV(identity, x0, y0, 0, source.u0(), source.vTop());
            mesh.submit();
        });
    }

    /**
     * Layout-space size of the document the element belongs to. Backdrop sampling mixes
     * element rects (layout space) with source-texture pixels, so every factor in that
     * path has to be derived from this size. Falls back to the GUI-scaled window size
     * for elements without a document (e.g. detached render nodes).
     */
    private static float[] layoutSize(Element target) {
        if (target != null && target.document != null) {
            io.github.kltyton.kltytonui.viewport.KltytonViewport viewport = target.document.getViewport();
            if (viewport != null) {
                double width = viewport.layoutWidth();
                double height = viewport.layoutHeight();
                if (width > 0 && height > 0) return new float[]{(float) width, (float) height};
            }
        }
        return new float[]{(float) KuiServices.client().getScaledWidth(),
                (float) KuiServices.client().getScaledHeight()};
    }

    private static BackdropSource prepareBackdropSource(FboHandle source, Rect rect, float cssBlurRadius,
                                                        float layoutW, float layoutH) {
        if (source == null || source.width <= 0 || source.height <= 0) return null;
        if (layoutW <= 0 || layoutH <= 0) return null;

        Position position = rect.getBodyRectPosition();
        Size size = rect.getBodyRectSize();
        // Rect coordinates are in the document's layout space, so the layout -> source
        // pixel factor has to come from that same space (not from the GUI-scaled window
        // size, which shrinks with guiScale and would sample the wrong region).
        float scaleX = source.width / layoutW;
        float scaleY = source.height / layoutH;
        float physicalRadius = Math.min(MAX_REASONABLE_BACKDROP_BLUR, Math.max(0, cssBlurRadius))
                * Math.max(scaleX, scaleY);
        float padding = physicalRadius + 2.0f;

        int srcX0 = clamp((int) Math.floor(position.x * scaleX - padding), 0, source.width);
        int srcX1 = clamp((int) Math.ceil((position.x + size.width()) * scaleX + padding), 0, source.width);
        int srcY0 = clamp((int) Math.floor(source.height - (position.y + size.height()) * scaleY - padding), 0, source.height);
        int srcY1 = clamp((int) Math.ceil(source.height - position.y * scaleY + padding), 0, source.height);
        if (srcX1 <= srcX0 || srcY1 <= srcY0) return null;

        int downsample = chooseDownsample(physicalRadius);
        int targetWidth = Math.max(1, (int) Math.ceil((srcX1 - srcX0) / (double) downsample));
        int targetHeight = Math.max(1, (int) Math.ceil((srcY1 - srcY0) / (double) downsample));
        FboHandle ping = acquireBackdropTarget(targetWidth, targetHeight);
        // The scratch target is pooled and still holds the previous element's/frame's
        // contents. On 26.1 the snapshot copy below only becomes visible to the blur
        // passes that sample this target in the same frame when the destination has
        // already been written through the normal clear path: without the clear the
        // blur passes read the stale (cleared) contents, so the backdrop ends up
        // empty and the composite paints nothing — backdrop-filter silently degrades
        // to "no filter at all" (or, with ForceAlpha, to opaque black).
        KuiServices.render().clear(ping, 0, 0, 0, 0);
        blitRegion(source, ping, srcX0, srcY0, srcX1, srcY1);

        float reducedRadius = physicalRadius / downsample;
        if (reducedRadius >= 0.5f) {
            FboHandle pong = acquireBackdropTarget(targetWidth, targetHeight);
            drawBlurPass(ping, pong, reducedRadius, 1.0f / targetWidth, 0.0f);
            drawBlurPass(pong, ping, reducedRadius, 0.0f, 1.0f / targetHeight);
        }

        float sourceWidth = srcX1 - srcX0;
        float sourceHeight = srcY1 - srcY0;
        float u0 = (float) ((position.x * scaleX - srcX0) / sourceWidth);
        float u1 = (float) (((position.x + size.width()) * scaleX - srcX0) / sourceWidth);
        float vTop = (float) ((source.height - position.y * scaleY - srcY0) / sourceHeight);
        float vBottom = (float) ((source.height - (position.y + size.height()) * scaleY - srcY0) / sourceHeight);
        float uvPerGuiX = scaleX / sourceWidth;
        float uvPerGuiY = scaleY / sourceHeight;
        return new BackdropSource(ping, u0, vBottom, u1, vTop, uvPerGuiX, uvPerGuiY);
    }

    private static FboHandle prepareBackdropShadow(BackdropSource source, Filter.FilterState state) {
        if (!state.hasDropShadow() || state.dropShadowBlur() < 0.5f) return source.target();
        float textureRadius = state.dropShadowBlur() * Math.max(
                source.uvPerGuiX() * source.target().width,
                source.uvPerGuiY() * source.target().height
        );
        return blurTexture(source.target(), textureRadius);
    }

    private static FboHandle prepareFullFilterSource(FboHandle source, float cssBlurRadius) {
        if (source == null || cssBlurRadius < 0.5f) return source;
        float guiW = Math.max(1.0f, (float) KuiServices.client().getScaledWidth());
        float guiH = Math.max(1.0f, (float) KuiServices.client().getScaledHeight());
        float physicalRadius = Math.max(0, cssBlurRadius)
                * Math.max(source.width / guiW, source.height / guiH);
        return blurTexture(source, physicalRadius);
    }

    private static FboHandle blurTexture(FboHandle source, float physicalRadius) {
        int downsample = chooseDownsample(physicalRadius);
        int width = Math.max(1, (int) Math.ceil(source.width / (double) downsample));
        int height = Math.max(1, (int) Math.ceil(source.height / (double) downsample));
        FboHandle ping = acquireBackdropTarget(width, height);
        blitRegion(source, ping, 0, 0, source.width, source.height);
        FboHandle pong = acquireBackdropTarget(width, height);
        float reducedRadius = Math.min(32.0f, physicalRadius / downsample);
        drawBlurPass(ping, pong, reducedRadius, 1.0f / width, 0.0f);
        drawBlurPass(pong, ping, reducedRadius, 0.0f, 1.0f / height);
        return ping;
    }

    private static int chooseDownsample(float physicalRadius) {
        int result = physicalRadius >= 6.0f ? 2 : 1;
        while (physicalRadius / result > 18.0f && result < 64) result *= 2;
        return result;
    }

    private static FboHandle acquireBackdropTarget(int width, int height) {
        FboHandle target;
        if (backdropPoolPointer < backdropPool.size()) {
            target = backdropPool.get(backdropPoolPointer);
            if (target.width != width || target.height != height) {
                KuiServices.render().destroyBuffers(target);
                target = KuiServices.render().createOffscreenTarget(width, height, false);
                backdropPool.set(backdropPoolPointer, target);
            }
        } else {
            target = KuiServices.render().createOffscreenTarget(width, height, false);
            backdropPool.add(target);
        }
        backdropPoolPointer++;
        return target;
    }

    private static void blitRegion(FboHandle source, FboHandle target,
                                   int srcX0, int srcY0, int srcX1, int srcY1) {
        KuiServices.render().blitFramebuffer(source, target, srcX0, srcY0, srcX1, srcY1);
    }

    private static void drawBlurPass(FboHandle source, FboHandle target, float radius,
                                     float directionX, float directionY) {
        Object shader = KuiServices.render().getFilterBlurShader();
        if (shader == null) return;

        KuiServices.render().clear(target, 0, 0, 0, 0);
        KuiServices.render().bindWrite(target, true);
        withBlendRenderState(false, () -> {
            Base.setProjectionMatrix(orthoProjection(target.width, target.height));
            Base.setShader(shader);
            KuiServices.render().setShaderUniform2f("Direction", directionX, directionY);
            KuiServices.render().setShaderUniformFloat("Radius", Math.min(32.0f, radius));
            KuiServices.render().bindColorTexture(source, 0);
            Base.setShaderColor(1, 1, 1, 1);

            MeshBuilder mesh = KuiServices.render().beginMesh(MeshMode.QUADS, MeshFormat.POSITION_TEX);
            Matrix4f identity = new Matrix4f();
            mesh.vertexUV(identity, 0, target.height, 0, 0, 0);
            mesh.vertexUV(identity, target.width, target.height, 0, 1, 0);
            mesh.vertexUV(identity, target.width, 0, 0, 1, 1);
            mesh.vertexUV(identity, 0, 0, 0, 0, 1);
            mesh.submit();
        });
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record BackdropSource(FboHandle target, float u0, float vBottom, float u1, float vTop,
                                  float uvPerGuiX, float uvPerGuiY) {}

    private static void setupUniforms(Object shader, Filter.FilterState state, FboHandle fbo,
                                      boolean forceAlpha, boolean preBlurred, float dynamicRangeLimit,
                                      float uvPerGuiX, float uvPerGuiY) {
        float blurRadius = preBlurred ? 0.0f
                : (forceAlpha ? Math.min(state.blurRadius(), MAX_REASONABLE_BACKDROP_BLUR) : state.blurRadius());
        KuiServices.render().setShaderUniformFloat("BlurRadius", blurRadius);
        KuiServices.render().setShaderUniformFloat("Brightness", state.brightness());
        KuiServices.render().setShaderUniformFloat("Contrast", state.contrast());
        KuiServices.render().setShaderUniformFloat("Saturate", state.saturate());
        KuiServices.render().setShaderUniformFloat("Sepia", state.sepia());
        KuiServices.render().setShaderUniformFloat("Grayscale", state.grayscale());
        KuiServices.render().setShaderUniformFloat("Invert", state.invert());
        KuiServices.render().setShaderUniformFloat("HueRotate", state.hueRotate());
        KuiServices.render().setShaderUniformFloat("Opacity", state.opacity());
        KuiServices.render().setShaderUniformFloat("DynamicRangeLimit", dynamicRangeLimit);
        KuiServices.render().setShaderUniform2f("ShadowOffset", state.dropShadowX(), state.dropShadowY());
        KuiServices.render().setShaderUniformFloat("ShadowBlur", state.dropShadowBlur());
        int c = state.dropShadowColor();
        float a = ((c >>> 24) & 0xFF) / 255f;
        float r = ((c >>> 16) & 0xFF) / 255f;
        float g = ((c >>> 8) & 0xFF) / 255f;
        float b = (c & 0xFF) / 255f;
        KuiServices.render().setShaderUniform4f("ShadowColor", r, g, b, a);
        KuiServices.render().setShaderUniform2f("InSize", (float) fbo.width, (float) fbo.height);
        KuiServices.render().setShaderUniformFloat("ForceAlpha", forceAlpha ? 1.0f : 0.0f);
        KuiServices.render().setShaderUniformFloat("ClipEnabled", 0.0f);
        KuiServices.render().setShaderUniform2f("GuiSize",
                (float) KuiServices.client().getScaledWidth(), (float) KuiServices.client().getScaledHeight());
        KuiServices.render().setShaderUniform2f("UvPerGuiPixel", uvPerGuiX, uvPerGuiY);
    }

    private static void setupBackdropClipUniforms(Object shader, Rect rect, float guiW, float guiH) {
        Position p = rect.getBodyRectPosition();
        Size s = rect.getBodyRectSize();
        float[] radii = rect.getBodyRadius();
        KuiServices.render().setShaderUniformFloat("ClipEnabled", 1.0f);
        KuiServices.render().setShaderUniform4f("ClipRect",
                (float) p.x, (float) p.y, (float) s.width(), (float) s.height());
        if (radii != null && radii.length >= 4) {
            KuiServices.render().setShaderUniform4f("ClipRadii", radii[0], radii[1], radii[2], radii[3]);
        }
        KuiServices.render().setShaderUniform2f("GuiSize", guiW, guiH);
    }

    private static Matrix4f orthoProjection(float width, float height) {
        return new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
    }
}

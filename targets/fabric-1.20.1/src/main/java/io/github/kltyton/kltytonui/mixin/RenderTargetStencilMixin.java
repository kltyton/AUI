package io.github.kltyton.kltytonui.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.kltyton.kltytonui.fabric.RenderService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-applies KUI's stencil attachment after vanilla recreates a target's
 * buffers on window resize. See {@link RenderService#onTargetBuffersCreated}.
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetStencilMixin {
    @Inject(method = "createBuffers", at = @At("RETURN"))
    private void kltytonui$reapplyStencil(int width, int height, boolean onOSX, CallbackInfo ci) {
        RenderService.onTargetBuffersCreated((RenderTarget) (Object) this);
    }
}

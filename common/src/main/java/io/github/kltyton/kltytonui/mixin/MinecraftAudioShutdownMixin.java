package io.github.kltyton.kltytonui.mixin;

import io.github.kltyton.kltytonui.resource.async.audio.AudioAsyncHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftAudioShutdownMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void kltytonui$shutdownAudio(CallbackInfo ci) {
        AudioAsyncHandler.INSTANCE.shutdown();
    }
}

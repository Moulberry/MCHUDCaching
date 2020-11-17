package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public class MixinFrameBuffer {

    @Inject(method="bindFramebuffer", at=@At("HEAD"), cancellable = true)
    public void bindFramebuffer(boolean viewport, CallbackInfo ci) {
        if(HUDCaching.overrideFramebuffer) {
            ci.cancel();
        }
    }

}

package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {

    @Inject(method="enableBlend", at=@At("HEAD"), cancellable = true)
    private static void enableBlend(CallbackInfo ci) {
        if(HUDCaching.overrideFramebuffer) {
            ci.cancel();
        }
    }

}

package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {

    @Inject(method="renderCrosshairs", at=@At("HEAD"), cancellable = true, remap = false)
    public void renderCrosshairs(CallbackInfo ci) {
        if(HUDCaching.renderingCacheOverride) {
            ci.cancel();
        }
    }

    @Inject(method="pre", at=@At("HEAD"), cancellable = true, remap = false)
    public void pre(CallbackInfoReturnable<Boolean> cir) {
        if(HUDCaching.renderingCacheOverride && HUDCaching.compatibilityMode) {
            cir.setReturnValue(false);
        }
    }

}

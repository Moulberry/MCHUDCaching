package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method="renderVignette", at=@At("HEAD"), cancellable = true)
    public void renderVignette(CallbackInfo ci) {
        if(HUDCaching.renderingCacheOverride) {
            ci.cancel();
        }
    }

}

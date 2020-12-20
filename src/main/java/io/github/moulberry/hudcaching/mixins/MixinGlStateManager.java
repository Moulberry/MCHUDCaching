package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {

    @Inject(method="blendFunc", at=@At("RETURN"))
    private static void blendFunc(int srcFactor, int dstFactor, CallbackInfo ci) {
        GlStateManager.tryBlendFuncSeparate(srcFactor, dstFactor, srcFactor, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Inject(method="tryBlendFuncSeparate", at=@At("RETURN"))
    private static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha, CallbackInfo ci) {
        if(dstFactorAlpha != GL11.GL_ONE_MINUS_SRC_ALPHA) {
            GlStateManager.tryBlendFuncSeparate(srcFactor, dstFactor, srcFactorAlpha, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

}

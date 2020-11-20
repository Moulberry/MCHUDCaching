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

    private static int srcFactor = 770;
    private static int dstFactor = 771;
    private static boolean wantBlend = true;

    @Inject(method="enableBlend", at=@At("HEAD"), cancellable = true)
    private static void enableBlend(CallbackInfo ci) {
        wantBlend = true;
        if(HUDCaching.renderingCacheOverride && srcFactor == 770 && dstFactor == 771) {
            ci.cancel();
        }
    }

    @Inject(method="disableBlend", at=@At("HEAD"))
    private static void disableBlend(CallbackInfo ci) {
        wantBlend = false;
    }

    @Inject(method="blendFunc", at=@At("RETURN"))
    private static void blendFunc(int srcFactor, int dstFactor, CallbackInfo ci) {
        MixinGlStateManager.srcFactor = srcFactor;
        MixinGlStateManager.dstFactor = dstFactor;

        if(srcFactor != 770 || dstFactor != 771) {
            if(wantBlend) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }
        }
    }

    @Inject(method="tryBlendFuncSeparate", at=@At("RETURN"))
    private static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha, CallbackInfo ci) {
        MixinGlStateManager.srcFactor = srcFactor;
        MixinGlStateManager.dstFactor = dstFactor;

        if(srcFactor != 770 || dstFactor != 771) {
            if(wantBlend) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }
        }
    }

}

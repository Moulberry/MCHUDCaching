package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {

    @Inject(method="blendFunc", at=@At("HEAD"), cancellable = true)
    private static void blendFunc(int srcFactor, int dstFactor, CallbackInfo ci) {
        if(HUDCaching.renderingCacheOverride) {
            OpenGlHelper.glBlendFunc(srcFactor, dstFactor, 1, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ci.cancel();
        }
    }

    private static int desSrcFactor = 0;
    private static int desDstFactor = 0;
    private static int desSrcAlphaFactor = 0;
    @Inject(method="tryBlendFuncSeparate", at=@At("HEAD"), cancellable = true)
    private static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha, CallbackInfo ci) {
        if(HUDCaching.renderingCacheOverride && dstFactorAlpha != GL11.GL_ONE_MINUS_SRC_ALPHA) {
            desSrcFactor = srcFactor;
            desDstFactor = dstFactor;
            desSrcAlphaFactor = srcFactorAlpha;
            OpenGlHelper.glBlendFunc(srcFactor, dstFactor, 1, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ci.cancel();
        }
    }

    private static boolean blendEnabled = false;
    @Inject(method="disableBlend", at=@At("HEAD"))
    private static void disableBlend(CallbackInfo ci) {
        if(HUDCaching.renderingCacheOverride) {
            blendEnabled = false;
        }
    }

    @Inject(method="enableBlend", at=@At("HEAD"))
    private static void enableBlend(CallbackInfo ci) {
        if(HUDCaching.renderingCacheOverride) {
            blendEnabled = true;
        }
    }

    @Inject(method="color(FFFF)V", at=@At("HEAD"), cancellable = true)
    private static void color(float red, float green, float blue, float alpha, CallbackInfo ci) {
        if(!blendEnabled && HUDCaching.renderingCacheOverride && alpha < 1) {
            GlStateManager.color(red, green, blue, 1f);
            ci.cancel();
        }
    }

}

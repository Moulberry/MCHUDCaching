package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(method="updateCameraAndRender",
            at=@At(
                value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V"
            )
    )
    public void updateCameraAndRender_renderGameOverlay(GuiIngame guiIngame, float partialTicks) {
        HUDCaching.renderCachedHud(((EntityRenderer)(Object)this), guiIngame, partialTicks);
    }

}

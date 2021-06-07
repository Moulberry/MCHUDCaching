package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.HUDCaching;
import io.github.moulberry.hudcaching.entityculling.EntityCulling;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumWorldBlockLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Redirect(method="renderWorldPass", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/EnumWorldBlockLayer;DILnet/minecraft/entity/Entity;)I")
    )
    public int renderWorldPass_renderBlockLayer(RenderGlobal renderGlobal, EnumWorldBlockLayer layer,
                                                 double partialTicks, int pass, Entity renderViewEntity) {
        int result = renderGlobal.renderBlockLayer(layer, partialTicks, pass, renderViewEntity);
        if(layer == EnumWorldBlockLayer.CUTOUT) {
            EntityCulling.getInstance().updateOccludedEntities((float)partialTicks, renderViewEntity);
        }
        return result;
    }

}

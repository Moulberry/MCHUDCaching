package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.entityculling.EntityCulling;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Render.class)
public class MixinRender {

    @Inject(method="shouldRender", at=@At("HEAD"), cancellable = true)
    public void shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if(!EntityCulling.getInstance().shouldRenderEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

}

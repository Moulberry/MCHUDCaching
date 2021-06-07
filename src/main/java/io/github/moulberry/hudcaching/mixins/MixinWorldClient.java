package io.github.moulberry.hudcaching.mixins;

import io.github.moulberry.hudcaching.entityculling.EntityCulling;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldClient.class)
public class MixinWorldClient {

    @Inject(method="addEntityToWorld", at=@At("HEAD"))
    public void addEntityToWorld(int entityID, Entity entityToSpawn, CallbackInfo ci) {
        EntityCulling.getInstance().addEntity(entityID, entityToSpawn);
    }

    @Inject(method="removeEntityFromWorld", at=@At("RETURN"))
    public void removeEntityFromWorld(int entityID, CallbackInfoReturnable<Entity> cir) {
        EntityCulling.getInstance().removeEntity(entityID, cir.getReturnValue());
    }

}

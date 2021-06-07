package io.github.moulberry.hudcaching.mixins;

import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiIngameForge.class)
public interface GuiIngameForgeInvoker {

    @Invoker(remap = false)
    void invokeRenderCrosshairs(int width, int height);

}

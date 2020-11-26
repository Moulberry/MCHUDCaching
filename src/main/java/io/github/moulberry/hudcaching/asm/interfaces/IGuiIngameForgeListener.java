package io.github.moulberry.hudcaching.asm.interfaces;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

public interface IGuiIngameForgeListener {

    void renderCrosshairs(int width, int height);
    boolean pre(RenderGameOverlayEvent.ElementType type);
    void post(RenderGameOverlayEvent.ElementType type);

}

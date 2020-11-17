package io.github.moulberry.hudcaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

import java.util.*;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.CROSSHAIRS;

@Mod(modid = HUDCaching.MODID, version = HUDCaching.VERSION, clientSideOnly = true)
public class HUDCaching {
    public static final String MODID = "hudcaching";
    public static final String VERSION = "1.0-REL";

    private static Framebuffer framebuffer = null;
    public static boolean overrideFramebuffer = false;

    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static int tickCounter = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        //System.out.println("FPS:"+Minecraft.getDebugFPS());
        tickCounter--;
    }

    /*@SubscribeEvent
    public void pre(RenderGameOverlayEvent.Pre event) {
        if(Keyboard.isKeyDown(Keyboard.KEY_M) && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            event.setCanceled(true);
        }
    }*/

    public static void renderCachedHud(EntityRenderer entityRenderer, GuiIngame guiIngame, float partialTicks) {
        if(!OpenGlHelper.isFramebufferEnabled() || Keyboard.isKeyDown(Keyboard.KEY_N)) {
            guiIngame.renderGameOverlay(partialTicks);
        } else {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            double width = scaledResolution.getScaledWidth();
            double height = scaledResolution.getScaledHeight();
            double widthD = scaledResolution.getScaledWidth_double();
            double heightD = scaledResolution.getScaledHeight_double();

            if(framebuffer == null || tickCounter <= 0) {
                tickCounter = 1;

                framebuffer = checkFramebufferSizes(framebuffer, Minecraft.getMinecraft().displayWidth,
                        Minecraft.getMinecraft().displayHeight);

                framebuffer.bindFramebuffer(false);

                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.disableLighting();
                GlStateManager.disableFog();

                overrideFramebuffer = true;
                GuiIngameForge.renderCrosshairs = false;
                guiIngame.renderGameOverlay(partialTicks);
                GuiIngameForge.renderCrosshairs = true;
                overrideFramebuffer = false;

                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
                GlStateManager.enableBlend();
            }

            entityRenderer.setupOverlayRendering();
            framebuffer.bindFramebufferTexture();
            drawTexturedRect(0, 0, (float)widthD, (float)heightD, 0, 1, 1, 0, GL11.GL_NEAREST);


            if (!pre(partialTicks, scaledResolution, CROSSHAIRS) && showCrosshair()) {
                bind(Gui.icons);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                GlStateManager.enableAlpha();
                drawTexturedModalRect((int)width / 2 - 7, (int)height / 2 - 7, 0, 0, 16, 16);
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                GlStateManager.disableBlend();
            }
            post(partialTicks, scaledResolution, CROSSHAIRS);
        }
    }

    //Helper macros
    private static boolean pre(float partialTicks, ScaledResolution res, RenderGameOverlayEvent.ElementType type) {
        return MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Pre(new RenderGameOverlayEvent(partialTicks, res), type));
    }
    private static void post(float partialTicks, ScaledResolution res, RenderGameOverlayEvent.ElementType type) {
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(new RenderGameOverlayEvent(partialTicks, res), type));
    }
    private static void bind(ResourceLocation res) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(res);
    }
    protected static boolean showCrosshair() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo && !mc.thePlayer.hasReducedDebug() && !mc.gameSettings.reducedDebugInfo) {
            return false;
        } else if (mc.playerController.isSpectator()) {
            if (mc.pointedEntity != null) {
                return true;
            } else {
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos blockpos = mc.objectMouseOver.getBlockPos();

                    if (mc.theWorld.getTileEntity(blockpos) instanceof IInventory) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return true;
        }
    }
    private static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((x + 0), (y + height), 100).tex(((float)(textureX + 0) * f), ((float)(textureY + height) * f1)).endVertex();
        worldrenderer.pos((x + width), (y + height), 100).tex(((float)(textureX + width) * f), ((float)(textureY + height) * f1)).endVertex();
        worldrenderer.pos((x + width), (y + 0), 100).tex(((float)(textureX + width) * f), ((float)(textureY + 0) * f1)).endVertex();
        worldrenderer.pos((x + 0), (y + 0), 100).tex(((float)(textureX + 0) * f), ((float)(textureY + 0) * f1)).endVertex();
        tessellator.draw();
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if(framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if(framebuffer == null) {
                framebuffer = new Framebuffer(width, height, false);
            } else {
                framebuffer.createBindFramebuffer(width, height);
            }
            framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
        return framebuffer;
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer
                .pos(x, y+height, 0.0D)
                .tex(uMin, vMax).endVertex();
        worldrenderer
                .pos(x+width, y+height, 0.0D)
                .tex(uMax, vMax).endVertex();
        worldrenderer
                .pos(x+width, y, 0.0D)
                .tex(uMax, vMin).endVertex();
        worldrenderer
                .pos(x, y, 0.0D)
                .tex(uMin, vMin).endVertex();
        tessellator.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManager.disableBlend();
    }


}

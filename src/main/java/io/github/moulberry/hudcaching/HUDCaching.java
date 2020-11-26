package io.github.moulberry.hudcaching;

import io.github.moulberry.hudcaching.asm.interfaces.IGuiIngameForgeListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.*;

import java.lang.reflect.Method;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.CROSSHAIRS;

@Mod(modid = HUDCaching.MODID, version = HUDCaching.VERSION, clientSideOnly = true)
public class HUDCaching {
    public static final String MODID = "hudcaching";
    public static final String VERSION = "1.0-REL";

    public static Framebuffer framebuffer = null;
    public static boolean renderingCacheOverride = false;

    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    //1000 (default) renders every tick
    //2000 will render every 2 ticks
    //2500 will also render every 2 ticks
    //333 will render (up to) 3 times every tick
    //100 will render (up to) 10 times every tick
    private static int cacheTimeMilliticks = 1000; //CHANGE THIS IN CONFIG
    private static int tickCounter = 0;
    private static int rendersThisTick = 0;

    public static boolean doOptimization = true; //CHANGE THIS IN CONFIG
    public static boolean compatibilityMode = false; //MAYBE CHANGE THIS IS CONFIG, NOT SURE IF ITS GOOD OR NOT

    private boolean lastKey = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        boolean n = Keyboard.isKeyDown(Keyboard.KEY_N);
        boolean m = Keyboard.isKeyDown(Keyboard.KEY_M);
        if(!lastKey && Minecraft.getMinecraft().currentScreen == null) {
            if(n) {
                doOptimization = !doOptimization;
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Cache Optimization: " + doOptimization));
            } else if(m) {
                compatibilityMode = !compatibilityMode;
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Compatibility: " + compatibilityMode));
            }
        }
        lastKey = n || m;

        tickCounter--;
        rendersThisTick = 0;
    }

    public static void setupGlState() {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
    }

    public static void renderCachedHud(EntityRenderer entityRenderer, GuiIngame guiIngame, float partialTicks) {
        if(!OpenGlHelper.isFramebufferEnabled() || !doOptimization) {
            guiIngame.renderGameOverlay(partialTicks);
        } else {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledResolution.getScaledWidth();
            int height = scaledResolution.getScaledHeight();
            double widthD = scaledResolution.getScaledWidth_double();
            double heightD = scaledResolution.getScaledHeight_double();

            entityRenderer.setupOverlayRendering();
            GlStateManager.enableBlend();

            guiIngame.renderVignette(Minecraft.getMinecraft().thePlayer.getBrightness(partialTicks),
                    scaledResolution);

            if(framebuffer == null || (cacheTimeMilliticks >= 1000 && tickCounter <= 0) ||
                    (cacheTimeMilliticks < 1000 && partialTicks*1000 > rendersThisTick*cacheTimeMilliticks)) {
                tickCounter = cacheTimeMilliticks/1000;
                rendersThisTick++;

                framebuffer = checkFramebufferSizes(framebuffer, Minecraft.getMinecraft().displayWidth,
                        Minecraft.getMinecraft().displayHeight);

                framebuffer.framebufferClear();
                framebuffer.bindFramebuffer(false);

                GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.disableLighting();
                GlStateManager.disableFog();

                renderingCacheOverride = true;
                guiIngame.renderGameOverlay(partialTicks);
                renderingCacheOverride = false;

                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
                GlStateManager.enableBlend();
            }

            if(guiIngame instanceof GuiIngameForge) {
                ((IGuiIngameForgeListener)guiIngame).renderCrosshairs(width, height);
            } else if(guiIngame.showCrosshair()) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                GlStateManager.enableAlpha();
                drawTexturedModalRect(width/2 - 7, height/2 - 7, 0, 0, 16, 16);
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            }

            if(guiIngame instanceof GuiIngameForge && compatibilityMode) {
                GuiIngameForge guiIngameForge = (GuiIngameForge) guiIngame;

                if(!pre(guiIngameForge, ALL)) {
                    if(GuiIngameForge.renderHelmet && !pre(guiIngameForge, HELMET)) post(guiIngameForge, HELMET);
                    if(GuiIngameForge.renderPortal && !pre(guiIngameForge, PORTAL)) post(guiIngameForge, PORTAL);
                    if(GuiIngameForge.renderHotbar && !pre(guiIngameForge, HOTBAR)) post(guiIngameForge, HOTBAR);
                    if(GuiIngameForge.renderCrosshairs && !pre(guiIngameForge, CROSSHAIRS)) post(guiIngameForge, CROSSHAIRS);
                    if(GuiIngameForge.renderBossHealth && !pre(guiIngameForge, BOSSHEALTH)) post(guiIngameForge, BOSSHEALTH);

                    if (Minecraft.getMinecraft().playerController.shouldDrawHUD() &&
                            Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
                        if(GuiIngameForge.renderHealth && !pre(guiIngameForge, HEALTH)) post(guiIngameForge, HEALTH);
                        if(GuiIngameForge.renderArmor && !pre(guiIngameForge, ARMOR)) post(guiIngameForge, ARMOR);
                        if(GuiIngameForge.renderFood && !pre(guiIngameForge, FOOD)) post(guiIngameForge, FOOD);
                        if(GuiIngameForge.renderHealthMount && !pre(guiIngameForge, HEALTHMOUNT)) post(guiIngameForge, HEALTHMOUNT);
                        if(GuiIngameForge.renderAir && !pre(guiIngameForge, AIR)) post(guiIngameForge, AIR);
                    }

                    if(GuiIngameForge.renderJumpBar && !pre(guiIngameForge, JUMPBAR)) post(guiIngameForge, JUMPBAR);
                    if(GuiIngameForge.renderExperiance && !pre(guiIngameForge, EXPERIENCE)) post(guiIngameForge, EXPERIENCE);
                    if(!pre(guiIngameForge, DEBUG)) post(guiIngameForge, DEBUG);
                    if(!pre(guiIngameForge, TEXT)) post(guiIngameForge, TEXT);
                    if(!pre(guiIngameForge, CHAT)) post(guiIngameForge, CHAT);
                    if(!pre(guiIngameForge, PLAYER_LIST)) post(guiIngameForge, PLAYER_LIST);

                    post(guiIngameForge, ALL);
                }
            }

            framebuffer.bindFramebufferTexture();
            drawTexturedRect(0, 0, (float)widthD, (float)heightD, 0, 1, 1, 0, GL11.GL_NEAREST);
        }
    }

    private static boolean pre(GuiIngameForge guiIngameForge, RenderGameOverlayEvent.ElementType type) {
        setupGlState();
        return ((IGuiIngameForgeListener)guiIngameForge).pre(type);
    }
    private static void post(GuiIngameForge guiIngameForge, RenderGameOverlayEvent.ElementType type) {
        ((IGuiIngameForgeListener)guiIngameForge).post(type);
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
                framebuffer = new Framebuffer(width, height, true);
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

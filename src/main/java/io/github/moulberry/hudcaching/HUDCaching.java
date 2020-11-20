package io.github.moulberry.hudcaching;

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

    public static long lastReflectAttempt = 0;
    public static Method renderVignetteMethod = null;
    public static Method renderCrosshairMethod = null;

    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    //1000 (default) renders every tick
    //2000 will render every 2 ticks
    //2500 will also render every 2 ticks
    //333 will render (up to) 3 times every tick
    //100 will render (up to) 10 times every tick
    private static int cacheTimeMilliticks = 1000;
    private static int tickCounter = 0;
    private static int rendersThisTick = 0;

    public static boolean doOptimization = true;
    public static boolean compatibilityMode = false;

    private boolean lastKey = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        boolean n = Keyboard.isKeyDown(Keyboard.KEY_N);
        boolean m = Keyboard.isKeyDown(Keyboard.KEY_M);
        if(!lastKey) {
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

            long currentTime = System.currentTimeMillis();

            if(currentTime - lastReflectAttempt > 5000) {
                lastReflectAttempt = currentTime;
                if(renderVignetteMethod == null) {
                    try {
                        renderVignetteMethod = GuiIngame.class.getDeclaredMethod("renderVignette", float.class, ScaledResolution.class);
                    } catch(Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
                if(renderCrosshairMethod == null) {
                    try {
                        renderCrosshairMethod = GuiIngameForge.class.getDeclaredMethod("renderCrosshairs", int.class, int.class);
                    } catch(Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
            }

            if(Minecraft.isFancyGraphicsEnabled() && renderVignetteMethod != null) {
                try {
                    renderVignetteMethod.setAccessible(true);
                    GlStateManager.enableBlend();
                    renderVignetteMethod.invoke(guiIngame, Minecraft.getMinecraft().thePlayer.getBrightness(partialTicks),
                            scaledResolution);
                } catch(Exception ignored) {
                    ignored.printStackTrace();
                }
            }

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
                if(renderCrosshairMethod != null) {
                    try {
                        renderCrosshairMethod.setAccessible(true);
                        renderCrosshairMethod.invoke(guiIngame, width, height);
                    } catch(Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
            } else if(showCrosshair()) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                GlStateManager.enableAlpha();
                drawTexturedModalRect((int)width / 2 - 7, (int)height / 2 - 7, 0, 0, 16, 16);
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            }

            if(guiIngame instanceof GuiIngameForge && compatibilityMode) {
                RenderGameOverlayEvent parent = new RenderGameOverlayEvent(partialTicks, scaledResolution);
                if(!pre(parent, ALL)) {
                    if(GuiIngameForge.renderHelmet && !pre(parent, HELMET)) post(parent, HELMET);
                    if(GuiIngameForge.renderPortal && !pre(parent, PORTAL)) post(parent, PORTAL);
                    if(GuiIngameForge.renderHotbar && !pre(parent, HOTBAR)) post(parent, HOTBAR);
                    if(GuiIngameForge.renderCrosshairs && !pre(parent, CROSSHAIRS)) post(parent, CROSSHAIRS);
                    if(GuiIngameForge.renderBossHealth && !pre(parent, BOSSHEALTH)) post(parent, BOSSHEALTH);

                    if (Minecraft.getMinecraft().playerController.shouldDrawHUD() &&
                            Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
                        if(GuiIngameForge.renderHealth && !pre(parent, HEALTH)) post(parent, HEALTH);
                        if(GuiIngameForge.renderArmor && !pre(parent, ARMOR)) post(parent, ARMOR);
                        if(GuiIngameForge.renderFood && !pre(parent, FOOD)) post(parent, FOOD);
                        if(GuiIngameForge.renderHealthMount && !pre(parent, HEALTHMOUNT)) post(parent, HEALTHMOUNT);
                        if(GuiIngameForge.renderAir && !pre(parent, AIR)) post(parent, AIR);
                    }

                    if(GuiIngameForge.renderJumpBar && !pre(parent, JUMPBAR)) post(parent, JUMPBAR);
                    if(GuiIngameForge.renderExperiance && !pre(parent, EXPERIENCE)) post(parent, EXPERIENCE);
                    if(!pre(parent, DEBUG)) post(parent, DEBUG);
                    if(!pre(parent, TEXT)) post(parent, TEXT);
                    if(!pre(parent, CHAT)) post(parent, CHAT);
                    if(!pre(parent, PLAYER_LIST)) post(parent, PLAYER_LIST);

                    post(parent, ALL);
                }
            }

            framebuffer.bindFramebufferTexture();
            drawTexturedRect(0, 0, (float)widthD, (float)heightD, 0, 1, 1, 0, GL11.GL_NEAREST);
        }
    }

    private static boolean pre(RenderGameOverlayEvent parent, RenderGameOverlayEvent.ElementType type)
    {
        return MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Pre(parent, type));
    }
    private static void post(RenderGameOverlayEvent parent, RenderGameOverlayEvent.ElementType type)
    {
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(parent, type));
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

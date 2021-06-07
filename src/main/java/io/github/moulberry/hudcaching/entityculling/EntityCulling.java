package io.github.moulberry.hudcaching.entityculling;

import io.github.moulberry.hudcaching.HUDCaching;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector3f;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntityCulling {

    private static final EntityCulling INSTANCE = new EntityCulling();

    private static final float RAYCAST_STEP = 0.01f;

    private final ScheduledExecutorService RAYCAST_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        CubeVBOProvider.getInstance().clearVBOs();

        occludedEntities.clear();
        poolType.clear();
        visibleRaycastMap.clear();

        for(OcclusionQuery query : glQueries.values()) {
            if(query.queryId != -1) {
                GL15.glDeleteQueries(query.queryId);
            }
        }
    }

    @SubscribeEvent
    public void render(RenderLivingEvent.Pre<?> event) {
        GlStateManager.enableDepth();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @SubscribeEvent
    public void render(RenderLivingEvent.Post<?> event) {
        GlStateManager.enableDepth();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public enum OcclusionResult {
        WAITING,
        PASSED,
        FAILED
    }

    private static class OcclusionQuery {
        int queryId = -1;
        long testTime = 0;
        OcclusionResult result = OcclusionResult.PASSED;
        OcclusionResult lastResult = OcclusionResult.PASSED;

        public void setResult(OcclusionResult result) {
            if(this.result != OcclusionResult.WAITING) {
                this.lastResult = this.result;
            }
            this.result = result;
        }

        public boolean getOccluded() {
            if(result == OcclusionResult.WAITING) {
                return lastResult == OcclusionResult.FAILED;
            } else {
                return result == OcclusionResult.FAILED;
            }
        }
    }

    public enum PoolType {
        GENESIS,
        SURVIVOR,
        LONGTERM
    }

    private final Set<Entity> occludedEntities = new HashSet<>();
    private final Map<Entity, PoolType> poolType = new ConcurrentHashMap<>();
    private final Map<Entity, OcclusionQuery> glQueries = new HashMap<>();

    private int currentRaycastIndex = 0;
    private final List<Entity> visibleRaycastMap = new ArrayList<>();
    private final ReadWriteLock visibleRaycastLock = new ReentrantReadWriteLock(true);

    private EntityCulling() {
        RAYCAST_EXECUTOR_SERVICE.submit(this::runRaycastCheck);
    }

    public void updateOccludedEntities(float partialTicks, Entity e) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(-(e.prevPosX + (e.posX - e.prevPosX) * partialTicks),
                -(e.prevPosY + (e.posY - e.prevPosY) * partialTicks),
                -(e.prevPosZ + (e.posZ - e.prevPosZ) * partialTicks));

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        GlStateManager.colorMask(false, false, false, false);
        GlStateManager.depthMask(false);

        System.out.println("pool entities:"+poolType.size());

        Set<Entity> entities = new HashSet<>(poolType.keySet());

        for(Entity entity : entities) {
            if(entity == Minecraft.getMinecraft().thePlayer) continue;
            updateShouldRenderEntity(entity);
        }

        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        CubeVBOProvider.getInstance().resetVBO();

        GlStateManager.popMatrix();
    }

    public boolean shouldRenderEntity(Entity entity) {
        return !occludedEntities.contains(entity);
    }

    public void updateShouldRenderEntity(Entity entity) {
        PoolType type = poolType.get(entity);

        if(type == PoolType.GENESIS) {
            boolean shouldRender = shouldRenderGenesis(entity);

            if(shouldRender) {
                poolType.remove(entity);
                occludedEntities.remove(entity);
            } else {
                occludedEntities.add(entity);
            }
        }
    }

    private boolean shouldRenderGenesis(Entity entity) {
        long currentTimeMillis = System.currentTimeMillis();

        OcclusionQuery test = glQueries.get(entity);

        if(test == null) {
            test = new OcclusionQuery();
            glQueries.put(entity, test);
        }

        if(currentTimeMillis - test.testTime < 25) {
            return !test.getOccluded();
        }

        if(test.queryId == -1) {
            test.queryId = GL15.glGenQueries();
        }

        if(test.result != OcclusionResult.WAITING) {
            GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, test.queryId);

            AxisAlignedBB bb = entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f);

            GlStateManager.pushMatrix();
            GlStateManager.translate(bb.minX, bb.minY, bb.minZ);
            float xSize = (float)(bb.maxX - bb.minX);
            float ySize = (float)(bb.maxY - bb.minY);
            float zSize = (float)(bb.maxZ - bb.minZ);
            CubeVBOProvider.getInstance().renderVBO(xSize, ySize, zSize);
            GlStateManager.popMatrix();

            GL15.glEndQuery(GL33.GL_ANY_SAMPLES_PASSED);
        }

        boolean available = GL15.glGetQueryObjecti(test.queryId, GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE;

        if(!available) {
            test.setResult(OcclusionResult.WAITING);
            test.testTime = currentTimeMillis;
        } else {
            int queryResult = GL15.glGetQueryObjecti(test.queryId, GL15.GL_QUERY_RESULT);
            if(queryResult != GL11.GL_FALSE) {
                test.setResult(OcclusionResult.PASSED);
            } else {
                test.setResult(OcclusionResult.FAILED);
            }
            test.testTime = currentTimeMillis;
        }

        return !test.getOccluded();
    }

    public static EntityCulling getInstance() {
        return INSTANCE;
    }

    public void addEntity(int entityId, Entity entity) {
        visibleRaycastLock.writeLock().lock();
        try {
            visibleRaycastMap.add(entity);
        } finally {
            visibleRaycastLock.writeLock().unlock();
        }
    }

    public void removeEntity(int entityId, Entity entity) {
        if(entity == null) return;

        visibleRaycastLock.writeLock().lock();
        try {
            visibleRaycastMap.remove(entity);
        } finally {
            visibleRaycastLock.writeLock().unlock();
        }

        occludedEntities.remove(entity);
        poolType.remove(entity);

        OcclusionQuery query = glQueries.remove(entity);
        if(query != null && query.queryId != -1) {
            GL15.glDeleteQueries(query.queryId);
        }
    }

    private void runRaycastCheck() {
        RAYCAST_EXECUTOR_SERVICE.schedule(this::runRaycastCheck, 50, TimeUnit.MILLISECONDS);

        WorldClient world = Minecraft.getMinecraft().theWorld;
        if(world == null || visibleRaycastMap.isEmpty()) {
            return;
        }

        try {
            int visibleSize = visibleRaycastMap.size();
            int iterateAmount = (int)Math.ceil(visibleSize/20f);

            for(int i=0; i<iterateAmount; i++) {
                if(currentRaycastIndex >= visibleRaycastMap.size()) currentRaycastIndex = 0;

                Entity entity;
                visibleRaycastLock.readLock().lock();
                try {
                    entity = visibleRaycastMap.get(currentRaycastIndex);
                } finally {
                    visibleRaycastLock.readLock().unlock();
                }

                if(entity == null || poolType.containsKey(entity)) {
                    currentRaycastIndex++;
                    continue;
                }

                boolean visibleRaycast = checkVisibleRaycast(entity);

                if(!visibleRaycast) {
                    poolType.put(entity, PoolType.GENESIS);
                } else {
                    currentRaycastIndex++;
                }

            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkVisibleRaycast(Entity entity) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        WorldClient world = Minecraft.getMinecraft().theWorld;

        if(player == null || world == null) return true;

        AxisAlignedBB bb = entity.getEntityBoundingBox();

        Vector3f mid = new Vector3f((float)(bb.minX+bb.maxX)/2f, (float)(bb.minY+bb.maxY)/2f, (float)(bb.minZ+bb.maxZ)/2f);
        Vector3f raycastPos = new Vector3f((float)player.posX, (float)player.posY+player.eyeHeight, (float)player.posZ);

        Vector3f playerToMid = Vector3f.sub(mid, raycastPos, null);

        float distance = playerToMid.length();

        if(distance > 64) return false;

        Vector3f playerToMidStep = new Vector3f(playerToMid.x / distance * RAYCAST_STEP,
                playerToMid.y / distance * RAYCAST_STEP, playerToMid.z / distance * RAYCAST_STEP);

        int chunkKey = -1;
        Chunk chunk = null;
        for(int i=0; i<=Math.floor(distance / RAYCAST_STEP); i++) {
            Vector3f.add(raycastPos, playerToMidStep, raycastPos);

            int floorX = MathHelper.floor_float(raycastPos.x);
            int floorY = MathHelper.floor_float(raycastPos.y);
            int floorZ = MathHelper.floor_float(raycastPos.z);

            int chunkX = floorX / 16;
            int chunkZ = floorZ / 16;

            int thisChunkKey = (chunkX & 0b11) | ((chunkZ & 0b11) << 2);

            if(chunkKey != thisChunkKey) {
                chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                chunkKey = thisChunkKey;
            }

            if(chunk != null) {
                if(world.getBlockState(new BlockPos(floorX, floorY, floorZ)).getBlock().getMaterial() != Material.air) {
                    return false;
                }
            }
        }

        return true;
    }

}

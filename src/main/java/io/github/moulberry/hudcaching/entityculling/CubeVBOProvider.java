package io.github.moulberry.hudcaching.entityculling;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.IntHashMap;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

public class CubeVBOProvider {

    private static final CubeVBOProvider INSTANCE = new CubeVBOProvider();
    private static final int GL_DRAW_MODE = GL11.GL_TRIANGLE_STRIP;
    private static final VertexFormat VERTEX_FORMAT = DefaultVertexFormats.POSITION;
    private static final int TEN_BITS = 0b1111111111;

    private WorldRenderer worldRenderer = new WorldRenderer(14*3);

    public static CubeVBOProvider getInstance() {
        return INSTANCE;
    }

    private HashMap<Integer, VertexBuffer> vbos = new HashMap<>();
    private VertexBuffer lastVBO = null;

    public void resetVBO() {
        lastVBO = null;
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }

    public void renderVBO(float xSize, float ySize, float zSize) {
        int xUnits = Math.round(xSize*10);
        int yUnits = Math.round(ySize*10);
        int zUnits = Math.round(zSize*10);

        VertexBuffer buffer = getVBO(xUnits, yUnits, zUnits);
        if(lastVBO != buffer) {
            buffer.bindBuffer();

            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 12, 0L);
        }

        buffer.drawArrays(GL_DRAW_MODE);
    }

    public void clearVBOs() {
        for(VertexBuffer buffer : vbos.values()) {
            buffer.deleteGlBuffers();
        }
        vbos.clear();
        lastVBO = null;
    }

    private int getVBOId(int xUnits, int yUnits, int zUnits) {
        return (xUnits & TEN_BITS) | ((yUnits & TEN_BITS) << 10) | ((zUnits & TEN_BITS) << 10);
    }

    private VertexBuffer getVBO(int xUnits, int yUnits, int zUnits) {
        int vboId = getVBOId(xUnits, yUnits, zUnits);

        VertexBuffer buffer;
        if(vbos.containsKey(vboId)) {
            buffer = vbos.get(vboId);
        } else {
            renderAABB(worldRenderer, xUnits, yUnits, zUnits);

            buffer = new VertexBuffer(VERTEX_FORMAT);
            buffer.bufferData(worldRenderer.getByteBuffer());

            vbos.put(vboId, buffer);
        }

        return buffer;
    }

    private void renderAABB(WorldRenderer worldRenderer, int xUnits, int yUnits, int zUnits) {
        worldRenderer.begin(GL_DRAW_MODE, VERTEX_FORMAT);
        worldRenderer.pos(0, yUnits/10f, zUnits/10f).endVertex();
        worldRenderer.pos(xUnits/10f, yUnits/10f, zUnits/10f).endVertex();
        worldRenderer.pos(0, 0, zUnits/10f).endVertex();
        worldRenderer.pos(xUnits/10f, 0, zUnits/10f).endVertex();
        worldRenderer.pos(xUnits/10f, 0, 0).endVertex();
        worldRenderer.pos(xUnits/10f, yUnits/10f, zUnits/10f).endVertex();
        worldRenderer.pos(xUnits/10f, yUnits/10f, 0).endVertex();
        worldRenderer.pos(0, yUnits/10f, zUnits/10f).endVertex();
        worldRenderer.pos(0, yUnits/10f, 0).endVertex();
        worldRenderer.pos(0, 0, zUnits/10f).endVertex();
        worldRenderer.pos(0, 0, 0).endVertex();
        worldRenderer.pos(xUnits/10f, 0, 0).endVertex();
        worldRenderer.pos(0, yUnits/10f, 0).endVertex();
        worldRenderer.pos(xUnits/10f, yUnits/10f, 0).endVertex();
        worldRenderer.finishDrawing();
    }

}

package net.thegrimsey.projectstargate.client.renderers;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;

/*
*   Primarily based on original SGCraft implementation.
*   https://github.com/AlmuraDev/SGCraft/blob/master/src/mod/gcewing/sg/client/renderer/SGBaseTERenderer.java
 */
public class StarGateRenderer extends BlockEntityRenderer<SGBaseBlockEntity> {

    final static int ringSegmentCount = 32;
    final static float ringInnerRadius = 2.0f;
    final static float ringMidRadius = 2.25f;
    final static float ringOuterRadius = 2.5f;
    final static float ringDepth = 0.5f;
    final static double ringOverlap = 1/64.0d;
    final static double ringZOffset = 0.0001d;

    final static float chevronInnerRadius = 2.25f;
    final static float chevronOuterRadius = ringOuterRadius + 1/16.0f;
    final static float chevronWidth = (chevronOuterRadius - chevronInnerRadius) * 1.5f;
    final static float chevronDepth = 0.125f;
    final static float chevronBorderWidth = chevronWidth / 6f;
    final static float chevronMotionDistance = 1/8.0f;

    final static int textureTilesWide = 32;
    final static int textureTilesHigh = 2;
    final static double textureScaleU = 1.0/(textureTilesWide * 16);
    final static double textureScaleV = 1.0/(textureTilesHigh * 16);

    final static double ringSymbolTextureLength = 512.0; //27 * 8;
    final static double ringSymbolTextureHeight = 16.0; //12;
    final static double ringSymbolSegmentWidth = ringSymbolTextureLength / ringSegmentCount;

    public final static int ehGridRadialSize = 5;
    public final static int ehGridPolarSize = ringSegmentCount;
    public final static double ehBandWidth = ringInnerRadius / ehGridRadialSize;

    final static double numIrisBlades = 12;

    static int[][] chevronEngagementSequences = {
            {9, 3, 4, 5, 6, 0, 1, 2, 9}, // 7 symbols (9 = never enganged)
            {7, 3, 4, 5, 8, 0, 1, 2, 6}  // 9 symbols
    };

    static float[] s = new float[ringSegmentCount + 1];
    static float[] c = new float[ringSegmentCount + 1];

    static {
        for (int i = 0; i <= ringSegmentCount; i++) {
            double a = 2 * Math.PI * i / ringSegmentCount;
            s[i] = (float)Math.sin(a);
            c[i] = (float)Math.cos(a);
        }
    }

    double u0, v0;

    public StarGateRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(SGBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.merged)
            return;

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(new Identifier(ProjectStarGate.MODID, "textures/blockentity/stargate.png")));

        matrices.push();
        matrices.translate(0.5, 2.5, 0.5);
        renderStarGate(entity, tickDelta, matrices, vertexConsumer, overlay, light);
        matrices.pop();
    }

    void renderStarGate(SGBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light)
    {
        // Render outer ring.
        renderRing((float) (ringMidRadius - ringOverlap), ringOuterRadius, ringZOffset, matrices.peek().getModel(), vertexConsumer, overlay, light, false);

        // Render inner ring.
        matrices.push(); // It gets it's own matrix so we can rotate it.
        renderRing(ringInnerRadius, ringMidRadius, 0, matrices.peek().getModel(), vertexConsumer, overlay, light, true);
        matrices.pop();

        // Render chevrons.
        renderChevrons(entity, matrices, vertexConsumer, overlay, light);
    }

    void renderChevrons(SGBaseBlockEntity entity, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light)
    {
        int chevronCount = 7;
        float anglesBetweenChevrons = 360f / chevronCount;

        for(int i = 0; i < chevronCount; i++)
        {
            matrices.push();
            matrices.multiply(new Quaternion(0, 0, 90f - (i - 4) * anglesBetweenChevrons, true));

            renderChevron(false, matrices, vertexConsumer, overlay, light);
            matrices.pop();
        }
    }

    void renderRing(float r1, float r2, double dz, Matrix4f matrix4f, VertexConsumer vertexConsumer, int overlay, int light, boolean inner)
    {
        float z = (float)(ringDepth / 2 + dz);
        float inverse = (inner ? -1 : 1);

        for (int i = 0; i < ringSegmentCount; i++) {
            float r2c = r2 * c[i], r2c1 = r2 * c[i+1];
            float r2s = r2 * s[i], r2s1 = r2 * s[i+1];
            float r1c = r1 * c[i], r1c1 = r1 * c[i+1];
            float r1s = r1 * s[i], r1s1 = r1 * s[i+1];

            // Draw side.
            {
                float xNormal = c[i] * inverse;
                float yNormal = s[i] * inverse;

                float x1 = inner ? r1c : r2c, y1 = inner ? r1s : r2s;
                float x2 = inner ? r1c1 : r2c1, y2 = inner ? r1s1 : r2s1;

                vertex(matrix4f, vertexConsumer, x1, y1, inverse * z, xNormal, yNormal, 0, 0, 0, overlay, light);
                vertex(matrix4f, vertexConsumer, x1, y1, inverse * -z, xNormal, yNormal, 0, 0, 128, overlay, light);
                vertex(matrix4f, vertexConsumer, x2, y2, inverse * -z, xNormal, yNormal, 0, 128, 128, overlay, light);
                vertex(matrix4f, vertexConsumer, x2, y2, inverse * z, xNormal, yNormal, 0, 128, 0, overlay, light);
            }

            // Draw back.
            vertex(matrix4f, vertexConsumer, r1c,   r1s,  -z, 0, 0, -1, 0, 128, overlay, light);
            vertex(matrix4f, vertexConsumer, r1c1,  r1s1, -z, 0, 0, -1, 128, 128, overlay, light);
            vertex(matrix4f, vertexConsumer, r2c1,  r2s1, -z, 0, 0, -1, 128, 0, overlay, light);
            vertex(matrix4f, vertexConsumer, r2c,   r2s,  -z, 0, 0, -1, 0, 0, overlay, light);

            // Draw front.
            {
                float u = (inner ? 114*i : 128), uwidth = (inner ? 114 : 128);
                float v = (inner ? 128 : 0), vheight = 128;
                vertex(matrix4f, vertexConsumer, r1c,   r1s,  z, 0, 0, 1, u,v+vheight, overlay, light);
                vertex(matrix4f, vertexConsumer, r2c,   r2s,  z, 0, 0, 1, u,v, overlay, light);
                vertex(matrix4f, vertexConsumer, r2c1,  r2s1, z, 0, 0, 1, u+uwidth,v, overlay, light);
                vertex(matrix4f, vertexConsumer, r1c1,  r1s1, z, 0, 0, 1, u+uwidth,v+vheight, overlay, light);
            }
        }
    }

    void renderChevron(boolean engaged, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light)
    {
        /*
        *   This is rotated 90 degrees.
        *   Positive X goes outwards.
        *   Positive Y goes right.
        *   Positive Z goes towards the front.
         */

        float w = chevronBorderWidth;
        float w2 = w * 1.25f;

        float xInner = chevronInnerRadius, yInner = chevronWidth / 4f;
        float xOuter = chevronOuterRadius, yOuter = chevronWidth / 2f;

        float zFlat = ringDepth / 2f;
        float zOut = zFlat + chevronDepth;

        if(engaged)
            matrices.translate(-chevronMotionDistance, 0, 0);
        Matrix4f matrix4f = matrices.peek().getModel();

        // Left-Forward Face
        vertex(matrix4f, vertexConsumer, xOuter, yOuter,  zOut, 0, 0, 1, 384,33, overlay, light); // TOPLEFT
        vertex(matrix4f, vertexConsumer, xInner, yInner,  zOut, 0, 0, 1, 384,126, overlay, light); // BOTTOMLEFT
        vertex(matrix4f, vertexConsumer, xInner+w, yInner-w, zOut, 0, 0, 1, 416,126, overlay, light); // BOTTOMRIGHT
        vertex(matrix4f, vertexConsumer, xOuter, yOuter-w2, zOut, 0, 0, 1, 416,33, overlay, light); // TOPRIGHT

        // Mid-Bottom-Forward Face
        vertex(matrix4f, vertexConsumer, xInner+w, yInner-w, zOut,  0, 0, 1, 416,0, overlay, light); // TOP LEFT
        vertex(matrix4f, vertexConsumer, xInner, yInner, zOut,            0, 0, 1, 416,34, overlay, light); // BOTTOM LEFT
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zOut,           0, 0, 1, 480,34, overlay, light); // BOTTOM RIGHT
        vertex(matrix4f, vertexConsumer, xInner+w, -yInner+w, zOut, 0, 0, 1, 480,0, overlay, light); // TOP RIGHT

        // Right-Forward Face
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter+w2, zOut, 0, 0, 1, 480,33, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner+w, -yInner+w, zOut,0, 0, 1, 480,126, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zOut, 0, 0, 1, 512,126, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter, zOut, 0, 0, 1, 512,33, overlay, light);

        // Mid-Top-Forward Face (Light up part)
        vertex(matrix4f, vertexConsumer, xOuter, yOuter-w2, zOut, 0, 0, 1, 256,28, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner+w, yInner-w, zOut,0, 0, 1, 256,128, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner+w, 0, zOut, 0, 0, 1, 320, 128, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, 0, zOut, 0, 0, 1, 320,28, overlay, light);

        vertex(matrix4f, vertexConsumer, xOuter, 0, zOut, 0, 0, 1, 320,28, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner+w, 0, zOut,0, 0, 1, 320,128, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner+w, -yInner+w, zOut, 0, 0, 1, 384,128, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter+w2, zOut, 0, 0, 1, 384,28, overlay, light);

        // Left Side
        vertex(matrix4f, vertexConsumer, xOuter, yOuter, zOut, 0,0,1,0, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, yOuter, zFlat, 0,0,1,0, 4, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, yInner, zFlat, 0,0,1,16, 4, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, yInner, zOut, 0,0,1,16, 0, overlay, light);

        // Bottom
        vertex(matrix4f, vertexConsumer, xInner, yInner, zOut, 0,0,1,416, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, yInner, zFlat, 0,0,1,416, 34, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zFlat, 0,0,1,480, 34, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zOut, 0,0,1,480, 0, overlay, light);

        // Right Side
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zOut, 0, 0, 1, 0, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zFlat, 0, 0, 1, 0, 4, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter, zFlat, 0, 0, 1, 16, 4, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter, zOut, 0, 0, 1, 16, 0, overlay, light);

        // Top Left
        vertex(matrix4f, vertexConsumer, xOuter, yOuter, zOut, 0, 0, 1, 384, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, yOuter-w2, zOut, 0, 0, 1, 384, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, yOuter-w2, zFlat, 0, 0, 1, 384, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, yOuter, zFlat, 0, 0, 1, 416, 0, overlay, light);

        // Top Right
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter, zOut, 0, 0, 1, 384, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter, zFlat, 0, 0, 1, 384, 34, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter+w2, zFlat, 0, 0, 1, 416, 34, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter+w2, zOut, 0, 0, 1, 416, 0, overlay, light);

        // Top Middle (Light up part)
        vertex(matrix4f, vertexConsumer, xOuter, yOuter-w2, zFlat, 0,0, 1, 256, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, yOuter-w2, zOut, 0,0, 1, 256, 30, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter+w2, zOut, 0,0, 1, 384, 30, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter+w2, zFlat, 0,0, 1, 384, 0, overlay, light);

        // Back.
        vertex(matrix4f, vertexConsumer, xOuter, -yOuter, zFlat, 0,0,1,416, 34, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, -yInner, zFlat, 0,0,1,416, 130, overlay, light);
        vertex(matrix4f, vertexConsumer, xInner, yInner, zFlat, 0,0,1,480, 130, overlay, light);
        vertex(matrix4f, vertexConsumer, xOuter, yOuter, zFlat, 0,0,1,480, 34, overlay, light);
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light)
    {
        vertexConsumer.vertex(matrix4f, x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).texture(u/4096f, v/256f).overlay(overlay).light(light).normal(nX, nY, nZ).next();
    }
}

package net.thegrimsey.projectstargate.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;

/*
 *   Primarily based on original SGCraft implementation.
 *   https://github.com/AlmuraDev/SGCraft/blob/master/src/mod/gcewing/sg/client/renderer/SGBaseTERenderer.java
 */
public class StarGateRenderer implements BlockEntityRenderer<SGBaseBlockEntity> {

    final static Identifier TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/blockentity/stargate.png");
    final static Identifier TEXTURE_CHEVRON = new Identifier(ProjectStarGate.MODID, "textures/blockentity/chevron.png");
    final static int ringSegmentCount = 32;
    final static float ringInnerRadius = 2.0f;
    final static float ringMidRadius = 2.25f;
    final static float ringOuterRadius = 2.5f;
    final static float ringDepth = 0.5f;
    final static double ringOverlap = 1 / 64.0d;
    final static double ringZOffset = 0.0001d;

    final static float chevronInnerRadius = 2.25f;
    final static float chevronOuterRadius = ringOuterRadius + 1 / 16.0f;
    final static float chevronWidth = (chevronOuterRadius - chevronInnerRadius) * 1.5f;
    final static float chevronDepth = 0.125f;
    final static float chevronBorderWidth = chevronWidth / 6f;
    final static float chevronMotionDistance = 1 / 8.0f;
    final static double numIrisBlades = 12;

    final static int ehGridRadialSize = 5;
    final static int ehGridPolarSize = ringSegmentCount;
    final static double ehBandWidth = ringInnerRadius / ehGridRadialSize;

    static int[][] chevronEngagementSequences = {
            {9, 3, 4, 5, 6, 0, 1, 2, 9}, // 7 symbols (9 = never enganged)
            {7, 3, 4, 5, 8, 0, 1, 2, 6}  // 9 symbols
    };

    static float[] s = new float[ringSegmentCount + 1];
    static float[] c = new float[ringSegmentCount + 1];

    static {
        for (int i = 0; i <= ringSegmentCount; i++) {
            double a = 2 * Math.PI * i / ringSegmentCount;
            s[i] = (float) Math.sin(a);
            c[i] = (float) Math.cos(a);
        }
    }

    float TEXTURE_WIDTH = 1024;
    float TEXTURE_HEIGHT = 64;

    public StarGateRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(SGBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.notMerged())
            return;

        matrices.push();
        matrices.translate(0.5, 2.5, 0.5);

        //Rotate based on facing.
        switch (entity.getFacing()) {
            case SOUTH:
                matrices.multiply(new Quaternion(0.f, 180.f, 0.f, true));
                break;
            case EAST:
                matrices.multiply(new Quaternion(0.f, 270.f, 0.f, true));
                break;
            case WEST:
                matrices.multiply(new Quaternion(0.f, 90.f, 0.f, true));
                break;
            default:
                break;
        }

        renderStarGate(entity, tickDelta, matrices, vertexConsumers, overlay, light);
        matrices.pop();
    }

    void renderStarGate(SGBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int overlay, int light) {
        // Render outer ring.
        TEXTURE_WIDTH = 1024;
        TEXTURE_HEIGHT = 64;
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        renderRing((float) (ringMidRadius - ringOverlap), ringOuterRadius, ringZOffset, matrices.peek().getModel(), vertexConsumer, overlay, light, false);

        // Render inner ring.
        matrices.push(); // It gets it's own matrix so we can rotate it.
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(entity.currentRingRotation));
        renderRing(ringInnerRadius, ringMidRadius, 0, matrices.peek().getModel(), vertexConsumer, overlay, light, true);
        matrices.pop();

        // Render chevrons.
        TEXTURE_WIDTH = 64;
        TEXTURE_HEIGHT = 64;
        VertexConsumer chevronVertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE_CHEVRON));
        renderChevrons(entity, matrices, chevronVertexConsumer, overlay, light);
    }

    void renderChevrons(SGBaseBlockEntity entity, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light) {
        int chevronCount = 9;
        float anglesBetweenChevrons = 360f / chevronCount;

        for (int i = 0; i < chevronCount; i++) {
            matrices.push();
            matrices.multiply(new Quaternion(0, 0, 90f - (i - 4) * anglesBetweenChevrons, true));

            renderChevron(entity.isChevronEngaged(i), matrices, vertexConsumer, overlay, light);
            matrices.pop();
        }
    }

    void renderRing(float innerRadius, float outerRadius, double dz, Matrix4f matrix4f, VertexConsumer vertexConsumer, int overlay, int light, boolean inner) {
        float z = (float) (ringDepth / 2 + dz);
        float inverse = (inner ? -1 : 1);

        for (int i = 0; i < ringSegmentCount; i++) {
            float r2c = outerRadius * c[i], r2c1 = outerRadius * c[i + 1];
            float r2s = outerRadius * s[i], r2s1 = outerRadius * s[i + 1];
            float r1c = innerRadius * c[i], r1c1 = innerRadius * c[i + 1];
            float r1s = innerRadius * s[i], r1s1 = innerRadius * s[i + 1];

            // Draw side.
            {
                float xNormal = c[i] * inverse;
                float yNormal = s[i] * inverse;

                float x1 = inner ? r1c : r2c, y1 = inner ? r1s : r2s;
                float x2 = inner ? r1c1 : r2c1, y2 = inner ? r1s1 : r2s1;

                vertex(matrix4f, vertexConsumer, x1, y1, inverse * z, xNormal, yNormal, 0, 0, 0, overlay, light);
                vertex(matrix4f, vertexConsumer, x1, y1, inverse * -z, xNormal, yNormal, 0, 0, 32, overlay, light);
                vertex(matrix4f, vertexConsumer, x2, y2, inverse * -z, xNormal, yNormal, 0, 32, 32, overlay, light);
                vertex(matrix4f, vertexConsumer, x2, y2, inverse * z, xNormal, yNormal, 0, 32, 0, overlay, light);
            }

            // Draw back.
            vertex(matrix4f, vertexConsumer, r1c, r1s, -z, 0, 0, -1, 0, 32, overlay, light);
            vertex(matrix4f, vertexConsumer, r1c1, r1s1, -z, 0, 0, -1, 32, 32, overlay, light);
            vertex(matrix4f, vertexConsumer, r2c1, r2s1, -z, 0, 0, -1, 32, 0, overlay, light);
            vertex(matrix4f, vertexConsumer, r2c, r2s, -z, 0, 0, -1, 0, 0, overlay, light);

            // Draw front.
            {
                float u = (inner ? 29 * i : 32), uwidth = (inner ? 29 : 32);
                float v = (inner ? 35 : 0), vheight = 28;
                vertex(matrix4f, vertexConsumer, r1c, r1s, z, 0, 0, 1, u, v + vheight, overlay, light);
                vertex(matrix4f, vertexConsumer, r2c, r2s, z, 0, 0, 1, u, v, overlay, light);
                vertex(matrix4f, vertexConsumer, r2c1, r2s1, z, 0, 0, 1, u + uwidth, v, overlay, light);
                vertex(matrix4f, vertexConsumer, r1c1, r1s1, z, 0, 0, 1, u + uwidth, v + vheight, overlay, light);
            }
        }
    }

    void renderChevron(boolean engaged, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light) {
        /*
         *   This is rotated 90 degrees.
         *   Positive X goes outwards.
         *   Positive Y goes right.
         *   Positive Z goes towards the front.
         */

        float w = chevronBorderWidth;
        float w2 = w * 1.25f;

        float xBottom = chevronInnerRadius, yInner = chevronWidth / 4f;
        float xTop = chevronOuterRadius, yOuter = chevronWidth / 2f;

        float zFlat = ringDepth / 2f;
        float zOut = zFlat + chevronDepth;

        float engagedMultiplier = engaged ? 1.0f : 0.2f;

        if (engaged)
            matrices.translate(-chevronMotionDistance, 0, 0);
        Matrix4f matrix4f = matrices.peek().getModel();

        // Left-Forward Face
        vertex(matrix4f, vertexConsumer, xTop, yOuter, zOut, 0, 0, 1,                   0, 8, overlay, light); // TOPLEFT
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zOut, 0, 0, 1,                0, 32, overlay, light); // BOTTOMLEFT
        vertex(matrix4f, vertexConsumer, xBottom + w, yInner - w, zOut, 0, 0, 1,  8, 32, overlay, light); // BOTTOMRIGHT
        vertex(matrix4f, vertexConsumer, xTop, yOuter - w2, zOut, 0, 0, 1,           8, 8, overlay, light); // TOPRIGHT

        // Mid-Bottom-Forward Face
        vertex(matrix4f, vertexConsumer, xBottom + w, yInner - w, zOut, 0, 0, 1,  8 + 4, 32, overlay, light); // TOP LEFT
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zOut, 0, 0, 1,                8, 38, overlay, light); // BOTTOM LEFT
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zOut, 0, 0, 1,               32, 32, overlay, light); // BOTTOM RIGHT
        vertex(matrix4f, vertexConsumer, xBottom + w, -yInner + w, zOut, 0, 0, 1, 32 - 4, 38, overlay, light); // TOP RIGHT

        // Right-Forward Face
        vertex(matrix4f, vertexConsumer, xTop, -yOuter + w2, zOut, 0, 0, 1,          8, 8, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom + w, -yInner + w, zOut, 0, 0, 1, 8, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zOut, 0, 0, 1,               0, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter, zOut, 0, 0, 1,                  0, 8, overlay, light);

        // Mid-Top-Forward Face (Light up part)
        vertex(matrix4f, vertexConsumer, xTop,          yOuter - w2, zOut, 0, 0, 1,        8,  8, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xBottom + w,yInner - w, zOut, 0, 0, 1,         8,  32, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xBottom + w,0, zOut, 0, 0, 1,                  24, 32, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xTop,          0, zOut, 0, 0, 1,                  24, 8, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);

        vertex(matrix4f, vertexConsumer, xTop, 0, zOut, 0, 0, 1,                    24, 8, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xBottom + w, 0, zOut, 0, 0, 1,          24, 32, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xBottom + w, -yInner + w, zOut, 0, 0, 1,40, 32, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter + w2, zOut, 0, 0, 1,         40, 8, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);

        // Left Side
        vertex(matrix4f, vertexConsumer, xTop, yOuter, zOut, 0, 0, 1, 40, 8, overlay, light); // Top right
        vertex(matrix4f, vertexConsumer, xTop, yOuter, zFlat, 0, 0, 1, 48, 8, overlay, light); // Top left
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zFlat, 0, 0, 1, 48, 32, overlay, light); // Bottom left
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zOut, 0, 0, 1, 40, 32, overlay, light); // bottom right

        // Bottom
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zOut, 0, 0, 1, 8, 40, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zFlat, 0, 0, 1, 8, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zFlat, 0, 0, 1, 32, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zOut, 0, 0, 1, 32, 40, overlay, light);

        // Right Side
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zOut, 0, 0, 1, 40, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zFlat, 0, 0, 1, 48, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter, zFlat, 0, 0, 1, 48, 8, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter, zOut, 0, 0, 1, 40, 8, overlay, light);

        // Top Left
        vertex(matrix4f, vertexConsumer, xTop, yOuter, zOut, 0, 0, 1,          0,  8, overlay, light); //Bot left.
        vertex(matrix4f, vertexConsumer, xTop, yOuter - w2, zOut, 0, 0, 1,  8,  8, overlay, light); // Bot right.
        vertex(matrix4f, vertexConsumer, xTop, yOuter - w2, zFlat, 0, 0, 1, 8,  0, overlay, light); // Top right.
        vertex(matrix4f, vertexConsumer, xTop, yOuter, zFlat, 0, 0, 1,         0, 0, overlay, light); // Top left

        // Top Right
        vertex(matrix4f, vertexConsumer, xTop, -yOuter, zOut, 0, 0, 1,          8, 8, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter, zFlat, 0, 0, 1,         8, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter + w2, zFlat, 0, 0, 1, 0, 0, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter + w2, zOut, 0, 0, 1,  0, 8, overlay, light);

        // Top Middle (Light up part)
        vertex(matrix4f, vertexConsumer, xTop, yOuter - w2, zFlat, 0, 0, 1, 8, 0, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xTop, yOuter - w2, zOut, 0, 0, 1, 8, 8, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter + w2, zOut, 0, 0, 1, 40, 8, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);
        vertex(matrix4f, vertexConsumer, xTop, -yOuter + w2, zFlat, 0, 0, 1, 40, 0, overlay, light, engagedMultiplier, engagedMultiplier, engagedMultiplier);

        // Back.
        vertex(matrix4f, vertexConsumer, xTop, -yOuter, zFlat, 0, 0, 1, 104, 8, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, -yInner, zFlat, 0, 0, 1, 104, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xBottom, yInner, zFlat, 0, 0, 1, 120, 32, overlay, light);
        vertex(matrix4f, vertexConsumer, xTop, yOuter, zFlat, 0, 0, 1, 120, 8, overlay, light);
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light) {
        vertex(matrix4f, vertexConsumer, x, y, z, nX, nY, nZ, u, v, overlay, light, 1.0f, 1.0f, 1.0f);
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light, float r, float g, float b) {
        vertexConsumer.vertex(matrix4f, x, y, z).color(r, g, b, 1.0f).texture((float) (u / TEXTURE_WIDTH), v / TEXTURE_HEIGHT).overlay(overlay).light(light).normal(nX, nY, nZ).next();
    }
}

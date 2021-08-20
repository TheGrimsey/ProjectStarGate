package net.thegrimsey.projectstargate.client.renderers;

import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import net.thegrimsey.projectstargate.mixin.RenderLayerMultiPhaseAccessor;

/*
 *   Primarily based on original SGCraft implementation.
 *   https://github.com/AlmuraDev/SGCraft/blob/master/src/mod/gcewing/sg/client/renderer/SGBaseTERenderer.java
 *
 *   Might just switch this to draw using OpenGL directly eventually...
 */
public class StarGateRenderer implements BlockEntityRenderer<SGBaseBlockEntity> {

    final static Identifier TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/blockentity/stargate.png");
    final static Identifier TEXTURE_CHEVRON = new Identifier(ProjectStarGate.MODID, "textures/blockentity/chevron.png");
    final static Identifier TEXTURE_HORIZON = new Identifier(ProjectStarGate.MODID, "textures/blockentity/eventhorizon.png");

    final static RenderLayer HORIZON_LAYER = Util.memoize((texture) -> {
        RenderLayer.MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder().shader(RenderPhase.ENTITY_CUTOUT_SHADER).texture(new RenderPhase.Texture((Identifier) texture, false, false)).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).cull(RenderPhase.DISABLE_CULLING).lightmap(RenderPhase.ENABLE_LIGHTMAP).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true);
        return RenderLayerMultiPhaseAccessor.of("horizon_layer", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, false, multiPhaseParameters);
    }).apply(TEXTURE_HORIZON);

    public final static int RING_SEGMENT_COUNT = 32;
    final static float RING_INNER_RADIUS = 2.0f;
    final static float RING_MID_RADIUS = 2.25f;
    final static float RING_OUTER_RADIUS = 2.5f;
    final static float RING_DEPTH = 0.5f;
    final static double RING_OVERLAP = 1 / 64.0d;
    final static double RING_Z_OFFSET = 0.0001d;

    final static float CHEVRON_INNER_RADIUS = 2.25f;
    final static float CHEVRON_OUTER_RADIUS = RING_OUTER_RADIUS + 1 / 16.0f;
    final static float CHEVRON_WIDTH = (CHEVRON_OUTER_RADIUS - CHEVRON_INNER_RADIUS) * 1.5f;
    final static float CHEVRON_DEPTH = 0.125f;
    final static float CHEVRON_BORDER_WIDTH = CHEVRON_WIDTH / 6f;
    final static float CHEVRON_MOTION_DISTANCE = 1 / 8.0f;
    final static double NUM_IRIS_BLADES = 12;

    static final float[] S = new float[RING_SEGMENT_COUNT + 1];
    static final float[] C = new float[RING_SEGMENT_COUNT + 1];

    static {
        for (int i = 0; i <= RING_SEGMENT_COUNT; i++) {
            double a = 2 * Math.PI * i / RING_SEGMENT_COUNT;
            S[i] = (float) Math.sin(a);
            C[i] = (float) Math.cos(a);
        }
    }

    float textureWidth = 1024;
    float textureHeight = 64;

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
        textureWidth = 1024;
        textureHeight = 64;
        VertexConsumer ringVertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        renderRing((float) (RING_MID_RADIUS - RING_OVERLAP), RING_OUTER_RADIUS, RING_Z_OFFSET, matrices.peek().getModel(), ringVertexConsumer, overlay, light, false);

        // Render inner ring.
        matrices.push(); // It gets its own matrix, so we can rotate it.
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(entity.getInterpolatedRingRotation(tickDelta)));
        renderRing(RING_INNER_RADIUS, RING_MID_RADIUS, 0, matrices.peek().getModel(), ringVertexConsumer, overlay, light, true);
        matrices.pop();

        // Render chevrons.
        textureWidth = 64;
        textureHeight = 64;
        VertexConsumer chevronVertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE_CHEVRON));
        renderChevrons(entity, matrices, chevronVertexConsumer, overlay, light);

        if(entity.isConnected())
        {
            textureWidth = 1;
            textureHeight = 1;
            VertexConsumer horizonVertexConsumer = vertexConsumers.getBuffer(HORIZON_LAYER);
            renderEventHorizon(entity, matrices, horizonVertexConsumer, overlay);
        }
    }

    void renderChevrons(SGBaseBlockEntity entity, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light) {
        int chevronCount = entity.getChevronCount();
        float anglesBetweenChevrons = 360f / chevronCount;

        for (int i = 0; i < chevronCount; i++) {
            matrices.push();
            matrices.multiply(new Quaternion(0, 0, 90f - (i - 4) * anglesBetweenChevrons, true));

            renderChevron(entity.isChevronEngaged(i), matrices, vertexConsumer, overlay, light);
            matrices.pop();
        }
    }

    void renderRing(float innerRadius, float outerRadius, double dz, Matrix4f matrix4f, VertexConsumer vertexConsumer, int overlay, int light, boolean inner) {
        float z = (float) (RING_DEPTH / 2 + dz);
        float inverse = (inner ? -1 : 1);

        for (int i = 0; i < RING_SEGMENT_COUNT; i++) {
            float outerRadiusX = outerRadius * C[i], nextOuterRadiusX = outerRadius * C[i + 1];
            float outerRadiusY = outerRadius * S[i], nextOuterRadiusY = outerRadius * S[i + 1];
            float innerRadiusX = innerRadius * C[i], nextInnerRadiusX = innerRadius * C[i + 1];
            float innerRadiusY = innerRadius * S[i], nextInnerRadiusY = innerRadius * S[i + 1];

            // Draw side.
            {
                float xNormal = C[i] * inverse;
                float yNormal = S[i] * inverse;

                float x1 = inner ? innerRadiusX : outerRadiusX, y1 = inner ? innerRadiusY : outerRadiusY;
                float x2 = inner ? nextInnerRadiusX : nextOuterRadiusX, y2 = inner ? nextInnerRadiusY : nextOuterRadiusY;

                vertex(matrix4f, vertexConsumer, x1, y1, inverse * z, xNormal, yNormal, 0, 0, 0, overlay, light);
                vertex(matrix4f, vertexConsumer, x1, y1, inverse * -z, xNormal, yNormal, 0, 0, 32, overlay, light);
                vertex(matrix4f, vertexConsumer, x2, y2, inverse * -z, xNormal, yNormal, 0, 32, 32, overlay, light);
                vertex(matrix4f, vertexConsumer, x2, y2, inverse * z, xNormal, yNormal, 0, 32, 0, overlay, light);
            }

            // Draw back.
            vertex(matrix4f, vertexConsumer, innerRadiusX, innerRadiusY, -z, 0, 0, -1, 0, 32, overlay, light);
            vertex(matrix4f, vertexConsumer, nextInnerRadiusX, nextInnerRadiusY, -z, 0, 0, -1, 32, 32, overlay, light);
            vertex(matrix4f, vertexConsumer, nextOuterRadiusX, nextOuterRadiusY, -z, 0, 0, -1, 32, 0, overlay, light);
            vertex(matrix4f, vertexConsumer, outerRadiusX, outerRadiusY, -z, 0, 0, -1, 0, 0, overlay, light);

            // Draw front.
            {
                float u = (inner ? 29 * i : 32), uWidth = (inner ? 29 : 32);
                float v = (inner ? 35 : 0), vHeight = 28;
                vertex(matrix4f, vertexConsumer, innerRadiusX, innerRadiusY, z, 0, 0, 1, u, v + vHeight, overlay, light);
                vertex(matrix4f, vertexConsumer, outerRadiusX, outerRadiusY, z, 0, 0, 1, u, v, overlay, light);
                vertex(matrix4f, vertexConsumer, nextOuterRadiusX, nextOuterRadiusY, z, 0, 0, 1, u + uWidth, v, overlay, light);
                vertex(matrix4f, vertexConsumer, nextInnerRadiusX, nextInnerRadiusY, z, 0, 0, 1, u + uWidth, v + vHeight, overlay, light);
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

        float w = CHEVRON_BORDER_WIDTH;
        float w2 = w * 1.25f;

        float xBottom = CHEVRON_INNER_RADIUS, yInner = CHEVRON_WIDTH / 4f;
        float xTop = CHEVRON_OUTER_RADIUS, yOuter = CHEVRON_WIDTH / 2f;

        float zFlat = RING_DEPTH / 2f;
        float zOut = zFlat + CHEVRON_DEPTH;

        float engagedMultiplier = engaged ? 1.0f : 0.2f;

        if (engaged)
            matrices.translate(-CHEVRON_MOTION_DISTANCE, 0, 0);
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

    void renderEventHorizon(SGBaseBlockEntity entity, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay)
    {
        int light = 0xF000F0; // Full brightness.
        matrices.push();
        Matrix4f matrix = matrices.peek().getModel();

        int bands = entity.eventHorizonZ.length-1;
        for(int band = 0; band < bands; band++)
        {
            float outerRadius = RING_INNER_RADIUS * ((float)(bands - band) / bands);
            float innerRadius = RING_INNER_RADIUS * ((float)(bands - 1 - band) / bands);
            for(int i = 0; i < RING_SEGMENT_COUNT; i++)
            {
                renderEventHorizonPiece(outerRadius, innerRadius, vertexConsumer, overlay, light, matrix, i, entity.eventHorizonZ[band], entity.eventHorizonZ[band+1]);
            }
        }

        matrices.pop();
    }

    private void renderEventHorizonPiece(float outerRadius, float innerRadius, VertexConsumer vertexConsumer, int overlay, int light, Matrix4f matrix, int i, float[] outerBandZs, float[] innerBandZs) {
        /*
        *   Quad fan thing.
        *
         */

        float outerX = outerRadius * C[i];
        float outerY = outerRadius * S[i];

        float nextOuterX = outerRadius * C[(i + 1) % RING_SEGMENT_COUNT];
        float nextOuterY = outerRadius * S[(i + 1) % RING_SEGMENT_COUNT];

        float innerX = innerRadius * C[i];
        float innerY = innerRadius * S[i];

        // coordinate for < triangle's top point.
        float nextInnerX = innerRadius * C[(i + 1) % RING_SEGMENT_COUNT];
        float nextInnerY = innerRadius * S[(i + 1) % RING_SEGMENT_COUNT];

        float outerZ = outerBandZs[i];
        float innerZ = innerBandZs[i % innerBandZs.length];
        float nextOuterZ = outerBandZs[(i+1) % outerBandZs.length];
        float nextInnerZ = innerBandZs[(i+1) % innerBandZs.length];

        // Inner triangle.
        vertex(matrix, vertexConsumer, outerX, outerY, outerZ,               0, 0, 0,   outerX, outerY,         overlay, light);
        vertex(matrix, vertexConsumer, nextOuterX, nextOuterY, nextOuterZ,   0, 0, 0,   nextOuterX, nextOuterY, overlay, light);
        vertex(matrix, vertexConsumer, nextInnerX, nextInnerY, nextInnerZ,   0, 0, 0,   nextInnerX, nextInnerY, overlay, light);
        vertex(matrix, vertexConsumer, innerX, innerY, innerZ,               0, 0, 0,   innerX, innerY,         overlay, light);

    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light) {
        vertex(matrix4f, vertexConsumer, x, y, z, nX, nY, nZ, u, v, overlay, light, 1.0f, 1.0f, 1.0f);
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light, float r, float g, float b) {
        vertexConsumer.vertex(matrix4f, x, y, z).color(r, g, b, 1.0f).texture(u / textureWidth, v / textureHeight).overlay(overlay).light(light).normal(nX, nY, nZ).next();
    }
}

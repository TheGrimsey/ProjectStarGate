package net.thegrimsey.projectstargate.client.renderers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import org.lwjgl.opengl.GL32;

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
        RenderLayer.MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder().shader(RenderPhase.ENTITY_CUTOUT_SHADER).texture(new RenderPhase.Texture((Identifier) texture, false, false)).transparency(RenderPhase.NO_TRANSPARENCY).cull(RenderPhase.DISABLE_CULLING).lightmap(RenderPhase.ENABLE_LIGHTMAP).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true);
        return RenderLayerMultiPhaseAccessor.of("horizon_layer", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, false, multiPhaseParameters);
    }).apply(TEXTURE_HORIZON);

    public final static int ringSegmentCount = 32;
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

    final static int ehBands = 4;

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
        VertexConsumer ringVertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        renderRing((float) (ringMidRadius - ringOverlap), ringOuterRadius, ringZOffset, matrices.peek().getModel(), ringVertexConsumer, overlay, light, false);

        // Render inner ring.
        matrices.push(); // It gets it's own matrix so we can rotate it.
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(entity.currentRingRotation));
        renderRing(ringInnerRadius, ringMidRadius, 0, matrices.peek().getModel(), ringVertexConsumer, overlay, light, true);
        matrices.pop();

        // Render chevrons.
        TEXTURE_WIDTH = 64;
        TEXTURE_HEIGHT = 64;
        VertexConsumer chevronVertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE_CHEVRON));
        renderChevrons(entity, matrices, chevronVertexConsumer, overlay, light);

        TEXTURE_WIDTH = 1;
        TEXTURE_HEIGHT = 1;
        VertexConsumer horizonVertexConsumer = vertexConsumers.getBuffer(HORIZON_LAYER);
        renderEventHorizon(entity, tickDelta, matrices, horizonVertexConsumer, overlay, light);
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
        float z = (float) (ringDepth / 2 + dz);
        float inverse = (inner ? -1 : 1);

        for (int i = 0; i < ringSegmentCount; i++) {
            float outerRadiusX = outerRadius * c[i], nextOuterRadiusX = outerRadius * c[i + 1];
            float outerRadiusY = outerRadius * s[i], nextOuterRadiusY = outerRadius * s[i + 1];
            float innerRadiusX = innerRadius * c[i], nextInnerRadiusX = innerRadius * c[i + 1];
            float innerRadiusY = innerRadius * s[i], nextInnerRadiusY = innerRadius * s[i + 1];

            // Draw side.
            {
                float xNormal = c[i] * inverse;
                float yNormal = s[i] * inverse;

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
                float u = (inner ? 29 * i : 32), uwidth = (inner ? 29 : 32);
                float v = (inner ? 35 : 0), vheight = 28;
                vertex(matrix4f, vertexConsumer, innerRadiusX, innerRadiusY, z, 0, 0, 1, u, v + vheight, overlay, light);
                vertex(matrix4f, vertexConsumer, outerRadiusX, outerRadiusY, z, 0, 0, 1, u, v, overlay, light);
                vertex(matrix4f, vertexConsumer, nextOuterRadiusX, nextOuterRadiusY, z, 0, 0, 1, u + uwidth, v, overlay, light);
                vertex(matrix4f, vertexConsumer, nextInnerRadiusX, nextInnerRadiusY, z, 0, 0, 1, u + uwidth, v + vheight, overlay, light);
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

    void renderEventHorizon(SGBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumer vertexConsumer, int overlay, int light)
    {
        matrices.push();
        //matrices.translate(0,0,1); // DEBUG.
        Matrix4f matrix = matrices.peek().getModel();

        for(int i = 0; i < ringSegmentCount; i++)
        {
            renderEventHorizonPiece(ringInnerRadius, ringInnerRadius * 0.5f, vertexConsumer, overlay, light, matrix, i, entity.eventHorizonZ[0], entity.eventHorizonZ[1]);
            renderEventHorizonPiece(ringInnerRadius * 0.5f, ringInnerRadius * 0.25f, vertexConsumer, overlay, light, matrix, i, entity.eventHorizonZ[1], entity.eventHorizonZ[2]);
            renderEventHorizonPiece(ringInnerRadius * 0.25f, 0.0f, vertexConsumer, overlay, light, matrix, i, entity.eventHorizonZ[2], entity.eventHorizonZ[2]);
        }

        matrices.pop();
    }

    private void renderEventHorizonPiece(float outerRadius, float innerRadius, VertexConsumer vertexConsumer, int overlay, int light, Matrix4f matrix, int i, float[] outerBandZs, float[] innerBandZs) {
        /*
        *   Quad fan thing.
        *
         */

        float outerX = outerRadius * c[i];
        float outerY = outerRadius * s[i];

        float nextOuterX = outerRadius * c[(i + 1) % ringSegmentCount];
        float nextOuterY = outerRadius * s[(i + 1) % ringSegmentCount];

        float innerX = innerRadius * c[i];
        float innerY = innerRadius * s[i];

        // coordinate for < triangle's top point.
        float nextInnerX = innerRadius * c[(i + 1) % ringSegmentCount];
        float nextInnerY = innerRadius * s[(i + 1) % ringSegmentCount];

        float outerZ = outerBandZs[i];
        float innerZ = innerBandZs[i];
        float nextOuterZ = outerBandZs[(i+1) % outerBandZs.length];
        float nextInnerZ = innerBandZs[(i+1) % innerBandZs.length];

        float u = (outerX + ringInnerRadius), v = (outerY + ringInnerRadius);
        float u1 = (nextOuterX + ringInnerRadius), v1 = (nextOuterY + ringInnerRadius);
        float u2 = (nextInnerX + ringInnerRadius), v2 = (nextInnerY + ringInnerRadius);
        float u3 = (innerX + ringInnerRadius), v3 = (innerY + ringInnerRadius);

        // Inner triangle.
        vertex(matrix, vertexConsumer, outerX, outerY, outerZ, 0, 0, 0, u, v, overlay, light);
        vertex(matrix, vertexConsumer, nextOuterX, nextOuterY, nextOuterZ, 0, 0, 0, u1, v1, overlay, light);
        vertex(matrix, vertexConsumer, nextInnerX, nextInnerY, nextInnerZ, 0, 0, 0, u2, v2, overlay, light);
        vertex(matrix, vertexConsumer, innerX, innerY, innerZ, 0, 0, 0, u3, v3, overlay, light);

    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light) {
        vertex(matrix4f, vertexConsumer, x, y, z, nX, nY, nZ, u, v, overlay, light, 1.0f, 1.0f, 1.0f);
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light, float r, float g, float b) {
        vertexConsumer.vertex(matrix4f, x, y, z).color(r, g, b, 1.0f).texture((float) (u / TEXTURE_WIDTH), v / TEXTURE_HEIGHT).overlay(overlay).light(light).normal(nX, nY, nZ).next();
    }
}

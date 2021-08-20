package net.thegrimsey.projectstargate.client.renderers;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;

public class DHDRenderer implements BlockEntityRenderer<DHDBlockEntity> {

    static final Identifier TEXTURE_SIDE = new Identifier(ProjectStarGate.MODID, "textures/blockentity/dhd_side.png");

    static final float BASE_RADIUS = 0.45f;
    static final float BASE_HEIGHT = 0.8f;
    static final int BASE_RESOLUTION = 8;

    static final float PLATE_RADIUS = 0.7f;
    static final float PLATE_THICKNESS = 0.15f;

    static final float[] S = new float[BASE_RESOLUTION + 1];
    static final float[] C = new float[BASE_RESOLUTION + 1];

    static {
        for (int i = 0; i <= BASE_RESOLUTION; i++) {
            double a = 2 * Math.PI * i / BASE_RESOLUTION;
            S[i] = (float) Math.sin(a);
            C[i] = (float) Math.cos(a);
        }
    }

    public DHDRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(DHDBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.5, 0, 0.5);

        renderBase(matrices, vertexConsumers, light, overlay);

        renderPlate(matrices, vertexConsumers, light, overlay);

        matrices.pop();
    }

    void renderBase(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        Matrix4f matrix4f = matrices.peek().getModel();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE_SIDE));

        for(int i = 0; i < BASE_RESOLUTION; i++)
        {
            float x = S[i] * BASE_RADIUS, nextX = S[i+1] * BASE_RADIUS;
            float z = C[i] * BASE_RADIUS, nextZ = C[i+1] * BASE_RADIUS;

            vertex(matrix4f, vertexConsumer, x, 0, z, x, 0, z, 0,       0, overlay, light);
            vertex(matrix4f, vertexConsumer, nextX, 0, nextZ, x, 0, z,     0, 0.25f, overlay, light);
            vertex(matrix4f, vertexConsumer, nextX, BASE_HEIGHT, nextZ, x, 0, z, BASE_HEIGHT, 0.25f, overlay, light);
            vertex(matrix4f, vertexConsumer, x, BASE_HEIGHT, z, x, 0, z,         BASE_HEIGHT, 0, overlay, light);
        }

        // Render base plate. TODO Triangle fan rendering
        for(int i = 0; i < BASE_RESOLUTION; i++)
        {
            float x = S[i] * BASE_RADIUS, nextX = S[i+1] * BASE_RADIUS;
            float z = C[i] * BASE_RADIUS, nextZ = C[i+1] * BASE_RADIUS;

            vertex(matrix4f, vertexConsumer, 0, 0, 0, 0, -1, 0,       0.5f, 0.5f, overlay, light);
            vertex(matrix4f, vertexConsumer, 0, 0, 0, 0, -1, 0,       0.5f, 0.5f, overlay, light);
            vertex(matrix4f, vertexConsumer, nextX, 0, nextZ, 0, -1, 0,     nextX + 0.5f, nextZ + 0.5f, overlay, light);
            vertex(matrix4f, vertexConsumer, x, 0, z, 0, -1, 0,             x + 0.5f, z + 0.5f, overlay, light);
        }
    }

    void renderPlate(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE_SIDE));
        Matrix4f flatMatrix = matrices.peek().getModel();
        matrices.push();
        matrices.translate(0, BASE_HEIGHT, 0);
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(15.f));
        Matrix4f rotatedMatrix = matrices.peek().getModel();

        for(int i = 0; i < BASE_RESOLUTION; i++)
        {
            float sideX = S[i] * PLATE_RADIUS, nextSideX = S[i+1] * PLATE_RADIUS;
            float sideZ = C[i] * PLATE_RADIUS, nextSideZ = C[i+1] * PLATE_RADIUS;

            float baseX = S[i] * BASE_RADIUS, nextBaseX = S[i+1] * BASE_RADIUS;
            float baseZ = C[i] * BASE_RADIUS, nextBaseZ = C[i+1] * BASE_RADIUS;

            // Draw bottom.
            vertex(flatMatrix, vertexConsumer, baseX, BASE_HEIGHT, baseZ,                  0, -1, 0, baseX,     baseZ, overlay, light);
            vertex(flatMatrix, vertexConsumer, nextBaseX, BASE_HEIGHT, nextBaseZ,          0, -1, 0, nextBaseX, nextBaseZ, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, nextSideX, 0, nextSideZ,              0, -1, 0, nextSideX, nextSideZ, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, sideX, 0, sideZ,                      0, -1, 0, sideX,     sideZ, overlay, light);

            // Draw side.
            vertex(rotatedMatrix, vertexConsumer, sideX, 0, sideZ,                      sideX, 0, sideZ,           0, 0, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, nextSideX, 0, nextSideZ,              sideX, 0, sideZ,           0.5f, 0, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, nextSideX, PLATE_THICKNESS, nextSideZ,   sideX, 0, sideZ,           0.5f, PLATE_THICKNESS, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, sideX, PLATE_THICKNESS, sideZ,           sideX, 0, sideZ,           0, PLATE_THICKNESS, overlay, light);

            // Draw top.
            vertex(rotatedMatrix, vertexConsumer, sideX, PLATE_THICKNESS, sideZ,          0, 1, 0,              sideX + 0.5f, sideZ + 0.5f, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, nextSideX, PLATE_THICKNESS, nextSideZ,  0, 1, 0,              nextSideX + 0.5f, nextSideZ + 0.5f, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, 0, PLATE_THICKNESS, 0,            0, 1, 0,            0.5f, 0.5f, overlay, light);
            vertex(rotatedMatrix, vertexConsumer, 0, PLATE_THICKNESS, 0,            0, 1, 0,            0.5f, 0.5f, overlay, light);
        }

        matrices.pop();
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light) {
        vertex(matrix4f, vertexConsumer, x, y, z, nX, nY, nZ, u, v, overlay, light, 1.0f, 1.0f, 1.0f);
    }

    void vertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x, float y, float z, float nX, float nY, float nZ, float u, float v, int overlay, int light, float r, float g, float b) {
        vertexConsumer.vertex(matrix4f, x, y, z).color(r, g, b, 1.0f).texture(u / 1, v / 1).overlay(overlay).light(light).normal(nX, nY, nZ).next();
    }
}

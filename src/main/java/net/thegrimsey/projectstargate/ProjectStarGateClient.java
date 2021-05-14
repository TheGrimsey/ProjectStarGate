package net.thegrimsey.projectstargate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.server.command.ForceLoadCommand;
import net.thegrimsey.projectstargate.client.renderers.StarGateRenderer;

public class ProjectStarGateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(ProjectSGBlocks.SG_BASE_BLOCKENTITY, StarGateRenderer::new);
    }
}

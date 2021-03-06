package net.thegrimsey.projectstargate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.thegrimsey.projectstargate.client.renderers.DHDRenderer;
import net.thegrimsey.projectstargate.client.renderers.StarGateRenderer;
import net.thegrimsey.projectstargate.screens.DHDScreen;
import net.thegrimsey.projectstargate.screens.StargateScreen;

public class ProjectStarGateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(ProjectSGBlocks.SG_BASE_BLOCKENTITY, StarGateRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(ProjectSGBlocks.DHD_BLOCKENTITY, DHDRenderer::new);

        ScreenRegistry.register(ProjectStarGate.STARGATE_SCREENHANDLER, StargateScreen::new);
        ScreenRegistry.register(ProjectStarGate.DHD_SCREENHANDLER, DHDScreen::new);
    }
}

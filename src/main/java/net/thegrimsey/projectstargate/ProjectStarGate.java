package net.thegrimsey.projectstargate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.thegrimsey.projectstargate.screens.DHDScreenHandler;
import net.thegrimsey.projectstargate.screens.StargateScreenHandler;

public class ProjectStarGate implements ModInitializer {
    public static final String MODID = "projectstargate";

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.build(new Identifier(MODID, "item_group"), () -> new ItemStack(ProjectSGBlocks.SG_BASE_BLOCK));

    public static final ScreenHandlerType<StargateScreenHandler> STARGATE_SCREENHANDLER;
    public static final ScreenHandlerType<DHDScreenHandler> DHD_SCREENHANDLER;

    static {
        STARGATE_SCREENHANDLER = ScreenHandlerRegistry.registerExtended(new Identifier(MODID, "stargate_screenhandler"), StargateScreenHandler::new);
        DHD_SCREENHANDLER = ScreenHandlerRegistry.registerExtended(new Identifier(MODID, "dhd_screenhandler"), DHDScreenHandler::new);
    }

    @Override
    public void onInitialize() {
        ProjectSGBlocks.registerBlocks();
        ProjectSGItems.registerItems();
        ProjectSGFeatures.registerFeatures();

        ProjectSGNetworking.registerNetworking();

        ProjectSGSounds.registerSounds();
    }
}

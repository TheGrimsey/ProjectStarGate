package net.thegrimsey.projectstargate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class ProjectStarGate implements ModInitializer {
    public static final String MODID = "projectstargate";

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.build(new Identifier(MODID, "item_group"), () -> new ItemStack(ProjectSGBlocks.SG_BASE_BLOCK));

    @Override
    public void onInitialize() {

        ProjectSGBlocks.registerBlocks();
        ProjectSGItems.registerItems();

        ProjectSGFeatures.registerFeatures();
    }
}

package net.thegrimsey.projectstargate;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ProjectSGBlocks {

    public static void RegisterBlocks() {

    }

    private static void RegisterBlock(String Id, Block block)
    {
        Identifier identifier = new Identifier(ProjectStarGate.MODID, Id);
        Registry.register(Registry.BLOCK, identifier, block);
        Registry.register(Registry.ITEM, identifier, new BlockItem(block, new FabricItemSettings().group(ProjectStarGate.ITEM_GROUP)));
    }
}

package net.thegrimsey.projectstargate;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.thegrimsey.projectstargate.blocks.SGBaseBlock;
import net.thegrimsey.projectstargate.blocks.SGChevronBlock;
import net.thegrimsey.projectstargate.blocks.SGRingBlock;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;

public class ProjectSGBlocks {
    public static final FabricBlockSettings SG_BLOCK_SETTINGS = FabricBlockSettings.of(Material.METAL).breakByTool(FabricToolTags.PICKAXES).nonOpaque();
    public static final SGBaseBlock SG_BASE_BLOCK = new SGBaseBlock(SG_BLOCK_SETTINGS);
    public static final SGRingBlock SG_RING_BLOCK = new SGRingBlock(SG_BLOCK_SETTINGS);
    public static final SGChevronBlock SG_CHEVRON_BLOCK = new SGChevronBlock(SG_BLOCK_SETTINGS);

    public static BlockEntityType<SGBaseBlockEntity> SG_BASE_BLOCKENTITY;

    public static void RegisterBlocks() {
        RegisterBlock("stargate_base", SG_BASE_BLOCK);
        RegisterBlock("stargate_ring", SG_RING_BLOCK);
        RegisterBlock("stargate_chevron", SG_CHEVRON_BLOCK);

        SG_BASE_BLOCKENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(ProjectStarGate.MODID, "sgbase_blockentity"), BlockEntityType.Builder.create(SGBaseBlockEntity::new, SG_BASE_BLOCK).build(null));
    }

    private static void RegisterBlock(String Id, Block block)
    {
        Identifier identifier = new Identifier(ProjectStarGate.MODID, Id);
        Registry.register(Registry.BLOCK, identifier, block);
        Registry.register(Registry.ITEM, identifier, new BlockItem(block, new FabricItemSettings().group(ProjectStarGate.ITEM_GROUP)));
    }
}

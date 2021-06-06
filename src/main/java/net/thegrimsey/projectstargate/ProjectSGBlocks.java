package net.thegrimsey.projectstargate;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricMaterialBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.thegrimsey.projectstargate.blocks.DHDBlock;
import net.thegrimsey.projectstargate.blocks.SGBaseBlock;
import net.thegrimsey.projectstargate.blocks.SGChevronBlock;
import net.thegrimsey.projectstargate.blocks.SGRingBlock;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;

public class ProjectSGBlocks {
    public static final Material NaquadahMaterial = new FabricMaterialBuilder(MapColor.DARK_GREEN).blocksPistons().build();
    // Mining Level 3 == Diamond, 2 == Iron

    // Naquadah blocks.
    public static final Block NAQUADAH_ORE = new Block(FabricBlockSettings.of(NaquadahMaterial).requiresTool().breakByTool(FabricToolTags.PICKAXES, 3).strength(4.0f, 4.f));
    public static final Block NAQUADAH_ORE_NETHER = new Block(FabricBlockSettings.of(NaquadahMaterial).requiresTool().breakByTool(FabricToolTags.PICKAXES, 3).strength(4.0f, 4.f));
    public static final Block NAQUADAH_ORE_END = new Block(FabricBlockSettings.of(NaquadahMaterial).requiresTool().breakByTool(FabricToolTags.PICKAXES, 3).strength(4.0f, 4.f));
    public static final Block NAQUADAH_BLOCK = new Block(FabricBlockSettings.of(NaquadahMaterial).requiresTool().breakByTool(FabricToolTags.PICKAXES, 2).strength(2.0f, 36.f));

    // StarGate blocks.
    public static final FabricBlockSettings STARGATE_BLOCK_SETTINGS = FabricBlockSettings.of(NaquadahMaterial).breakByTool(FabricToolTags.PICKAXES).strength(3.0f, 24.f).nonOpaque();
    public static final SGBaseBlock SG_BASE_BLOCK = new SGBaseBlock(STARGATE_BLOCK_SETTINGS);
    public static final SGRingBlock SG_RING_BLOCK = new SGRingBlock(STARGATE_BLOCK_SETTINGS);
    public static final SGChevronBlock SG_CHEVRON_BLOCK = new SGChevronBlock(STARGATE_BLOCK_SETTINGS);

    // Dial Home Device.
    public static final DHDBlock DHD_BLOCK = new DHDBlock(STARGATE_BLOCK_SETTINGS);

    // Block Entities.
    public static BlockEntityType<SGBaseBlockEntity> SG_BASE_BLOCKENTITY;
    public static BlockEntityType<DHDBlockEntity> DHD_BLOCKENTITY;

    public static void registerBlocks() {
        // Naquadah blocks.
        registerBlock("naquadah_ore", NAQUADAH_ORE);
        registerBlock("naquadah_ore_nether", NAQUADAH_ORE_NETHER);
        registerBlock("naquadah_ore_end", NAQUADAH_ORE_END);
        registerBlock("naquadah_block", NAQUADAH_BLOCK);

        // Stargate blocks.
        registerBlock("stargate_base", SG_BASE_BLOCK);
        registerBlock("stargate_ring", SG_RING_BLOCK);
        registerBlock("stargate_chevron", SG_CHEVRON_BLOCK);

        // Dial Home Device.
        registerBlock("dial_home_device", DHD_BLOCK);

        // Block Entities.
        SG_BASE_BLOCKENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(ProjectStarGate.MODID, "sgbase_blockentity"), FabricBlockEntityTypeBuilder.create(SGBaseBlockEntity::new, SG_BASE_BLOCK).build(null));
        DHD_BLOCKENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(ProjectStarGate.MODID, "dhd_blockentity"), FabricBlockEntityTypeBuilder.create(DHDBlockEntity::new, DHD_BLOCK).build(null));
    }

    static void registerBlock(String id, Block block) {
        Identifier identifier = new Identifier(ProjectStarGate.MODID, id);
        Registry.register(Registry.BLOCK, identifier, block);
        Registry.register(Registry.ITEM, identifier, new BlockItem(block, new FabricItemSettings().group(ProjectStarGate.ITEM_GROUP)));
    }
}

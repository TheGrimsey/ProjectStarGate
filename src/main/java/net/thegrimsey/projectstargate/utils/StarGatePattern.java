package net.thegrimsey.projectstargate.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.thegrimsey.projectstargate.ProjectSGBlocks;

public class StarGatePattern {
    public static final int[] PATTERN = { // Upside down pattern.
            2, 1, 3, 1, 2,
            1, 0, 0, 0, 1,
            2, 0, 0, 0, 2,
            1, 0, 0, 0, 1,
            2, 1, 2, 1, 2
    };

    public static final Block[] BLOCK_LIST = {
            Blocks.AIR,
            ProjectSGBlocks.SG_RING_BLOCK,
            ProjectSGBlocks.SG_CHEVRON_BLOCK,
            ProjectSGBlocks.SG_BASE_BLOCK
    };
}

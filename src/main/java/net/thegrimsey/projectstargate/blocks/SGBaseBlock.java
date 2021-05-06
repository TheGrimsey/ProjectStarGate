package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;

public class SGBaseBlock extends AbstractStarGateBlock {
    public SGBaseBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(MERGED) ? BlockRenderType.MODEL : BlockRenderType.MODEL; // TODO Block Entity Render
    }
}



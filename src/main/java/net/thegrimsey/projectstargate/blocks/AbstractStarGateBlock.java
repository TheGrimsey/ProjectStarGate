package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractStarGateBlock extends Block{
    public static final BooleanProperty MERGED = BooleanProperty.of("merged");
    
    public AbstractStarGateBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(MERGED, false));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(MERGED) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(MERGED);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        checkMerge(world, pos);
    }

    void checkMerge(World world, BlockPos pos)
    {
        // Check for complete circle.
        // C R C R C
        // R A A A R
        // C A A A C
        // R A A A R
        // C R B R C

        for(int x = -2; x < 2; x++)
        {
            for(int y = 0; y < 5; y++)
            {
                BlockState state = world.getBlockState(new BlockPos(pos.getX()+x, pos.getY()+y, pos.getZ()));
                System.out.println("Block: " + state.getBlock().getTranslationKey());
            }
        }
    }
}

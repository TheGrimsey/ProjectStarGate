package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class AbstractStarGateBlock extends Block {
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

        if (checkMerge(world, state, pos))
            System.out.println("MERGED");
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);

        if (state.get(MERGED)) {
            // Unmerge.
        }
    }


    boolean checkMerge(World world, BlockState state, BlockPos pos) {
        // Check for complete circle.
        // C R C R C
        // R A A A R
        // C A A A C
        // R A A A R
        // C R B R C

        // TODO Figure out bottomPoint from any block in the structure.

        return false;
    }
}

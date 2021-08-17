package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import net.thegrimsey.projectstargate.utils.StarGatePattern;

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

        tryMerge(world, state, pos);
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);

        if (state.get(MERGED)) {
            // Unmerge.
        }
    }

    abstract void tryMerge(World world, BlockState state, BlockPos pos);

    protected boolean checkMergePattern(World world, int bX, int bY, int bZ, boolean onZ) {
        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        for (int xz = 0; xz < 5; xz++) {
            for (int y = 0; y < 5; y++) {
                int arrayIndex = y * 5 + xz;
                Block expectedBlock = StarGatePattern.BLOCK_LIST[StarGatePattern.PATTERN[arrayIndex]];

                blockPos.set(bX + (onZ ? 0 : xz), bY + y, bZ + (onZ ? xz : 0));
                BlockState blockState = world.getBlockState(blockPos);

                // Fail if this is the wrong block or if it is already merged with something.
                if (blockState.getBlock() != expectedBlock)
                    return false;

                if (blockState.getBlock() instanceof AbstractStarGateBlock && blockState.get(MERGED))
                    return false;
            }
        }

        // If we get here the structure is correct. Now we just go through and merge it.
        for (int xz = 0; xz < 5; xz++) {
            for (int y = 0; y < 5; y++) {
                blockPos.set(bX + (onZ ? 0 : xz), bY + y, bZ + (onZ ? xz : 0));
                BlockState blockState = world.getBlockState(blockPos);

                if (blockState.getBlock() instanceof AbstractStarGateBlock)
                    world.setBlockState(blockPos, blockState.with(MERGED, true));
            }
        }

        if (world.getBlockEntity(new BlockPos(onZ ? bX : bX + 2, bY, onZ ? bZ + 2 : bZ)) instanceof SGBaseBlockEntity sgBaseBlockEntity) {
            sgBaseBlockEntity.setMerged(true);
            sgBaseBlockEntity.markDirty();
        }

        return true;
    }
}

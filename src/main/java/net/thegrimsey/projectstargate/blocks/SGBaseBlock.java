package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;
import net.thegrimsey.projectstargate.utils.StarGatePattern;
import org.jetbrains.annotations.Nullable;

public class SGBaseBlock extends AbstractStarGateBlock implements BlockEntityProvider {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public SGBaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(MERGED, false));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new SGBaseBlockEntity();
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        SGBaseBlockEntity entity = (SGBaseBlockEntity) world.getBlockEntity(pos);
        if (entity != null) {
            entity.address = AddressingUtil.GetAddressForLocation(pos, world.getRegistryKey().getValue());
            entity.facing = state.get(FACING);
        }
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if(blockEntity instanceof SGBaseBlockEntity)
            ((SGBaseBlockEntity) blockEntity).setMerged(false);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    boolean checkMerge(World world, BlockState state, BlockPos pos) {

        // The base block is at the bottom-center of the stargate. Our facing matters so we can quickly figure out where the bottom-left of the structure should be and go from there.
        int bX = pos.getX(), bY = pos.getY(), bZ = pos.getZ();
        boolean onZ = false;

        switch (state.get(FACING)) {
            case NORTH:
            case SOUTH:
                bX -= 2;
                break;
            case EAST:
            case WEST:
                bZ -= 2;
                onZ = true;
                break;
            default:
                return false;
        }

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

        SGBaseBlockEntity sgBaseBlockEntity = (SGBaseBlockEntity) world.getBlockEntity(pos);
        sgBaseBlockEntity.setMerged(true);
        sgBaseBlockEntity.markDirty();

        return true;
    }
}



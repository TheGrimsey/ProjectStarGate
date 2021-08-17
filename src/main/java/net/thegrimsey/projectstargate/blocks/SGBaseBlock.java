package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
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
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SGBaseBlockEntity(pos, state);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if(world.isClient)
            return;

        if (world.getBlockEntity(pos) instanceof SGBaseBlockEntity blockEntity) {
            blockEntity.address = AddressingUtil.GetAddressForLocation((ServerWorld) world, pos);
        }
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);

        if(world.isClient())
            return;

        if (world.getBlockEntity(pos) instanceof SGBaseBlockEntity blockEntity)
            blockEntity.setMerged(false);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) {
            if (world.getBlockEntity(pos) instanceof SGBaseBlockEntity blockEntity) {
                ItemStack heldStack = player.getStackInHand(hand);
                if (heldStack.isEmpty()) {
                    player.openHandledScreen(blockEntity);
                    // Test code below.
                } else if (heldStack.getItem() == Items.BREAD) {
                    blockEntity.dimensionalUpgrade = !blockEntity.dimensionalUpgrade;
                    blockEntity.sync();
                } else {
                    blockEntity.disconnect(false);
                }
            }
            return ActionResult.success(true);
        }

        return super.onUse(state, world, pos, player, hand, hit);
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
    void tryMerge(World world, BlockState state, BlockPos pos) {
        // The base block is at the bottom-center of the stargate. Our facing matters, so we can quickly figure out where the bottom-left of the structure should be and go from there.
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
                return;
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
                    return;

                if (blockState.getBlock() instanceof AbstractStarGateBlock && blockState.get(MERGED))
                    return;
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

        if (world.getBlockEntity(pos) instanceof SGBaseBlockEntity sgBaseBlockEntity) {
            sgBaseBlockEntity.setMerged(true);
            sgBaseBlockEntity.markDirty();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == ProjectSGBlocks.SG_BASE_BLOCKENTITY ? SGBaseBlockEntity::tick : null;
    }
}



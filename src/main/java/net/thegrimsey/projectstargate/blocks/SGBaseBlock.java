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
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;
import org.jetbrains.annotations.Nullable;

public class SGBaseBlock extends AbstractStarGateBlock implements BlockEntityProvider {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    
    public SGBaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new SGBaseBlockEntity();
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        SGBaseBlockEntity entity = (SGBaseBlockEntity)world.getBlockEntity(pos);
        if(entity != null)
        {
            entity.address = AddressingUtil.GetAddressForLocation(pos, world.getRegistryKey().getValue());
            entity.facing = state.get(FACING);
        }
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
}



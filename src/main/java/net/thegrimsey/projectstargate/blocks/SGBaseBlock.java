package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;
import org.jetbrains.annotations.Nullable;

public class SGBaseBlock extends AbstractStarGateBlock implements BlockEntityProvider {
    public SGBaseBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(MERGED) ? BlockRenderType.MODEL : BlockRenderType.MODEL; // TODO Block Entity Render
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
            entity.address = AddressingUtil.GetAddressForLocation(pos, world.getRegistryKey().getValue());
    }
}



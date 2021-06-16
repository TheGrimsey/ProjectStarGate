package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import org.jetbrains.annotations.Nullable;

public class DHDBlock extends Block implements BlockEntityProvider {
    final int GateXYSearchRadius = 5;
    final int GateZSearchRadius = 1;

    public DHDBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DHDBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) {
            if(world.getBlockEntity(pos) instanceof  DHDBlockEntity dhdBlockEntity)
                if(dhdBlockEntity.getStargatePos() != null)
                    player.openHandledScreen(dhdBlockEntity);

            return ActionResult.success(true);
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        // Check if there is a merged stargate nearby.
        BlockPos.Mutable bPos = new BlockPos.Mutable();
        for (int x = -GateXYSearchRadius; x < GateZSearchRadius; x++) {
            for (int z = -GateXYSearchRadius; z < GateXYSearchRadius; z++) {
                for (int y = -GateZSearchRadius; y < GateZSearchRadius; y++) {
                    bPos.set(pos.getX() + x, pos.getY() + y,pos.getZ() + z);

                    if (world.getBlockEntity(bPos) instanceof SGBaseBlockEntity sgBase) {
                        // Skip if this one isn't merged.
                        if (sgBase.notMerged())
                            continue;

                        //If we have a valid DHD entity then save our stargatepos as this one.
                        if (world.getBlockEntity(pos) instanceof DHDBlockEntity dhdBlockEntity)
                            dhdBlockEntity.setStargatePos(bPos);

                        // We break even if we don't have one because if we don't it doesn't matter. We should always have one though so.
                        break;
                    }
                }
            }
        }
    }
}

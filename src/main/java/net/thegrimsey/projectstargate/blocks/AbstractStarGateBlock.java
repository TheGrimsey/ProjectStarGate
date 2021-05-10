package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;

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

    static int[] pattern = { // Upside down pattern.
            2, 1, 3, 1, 2,
            1, 0, 0, 0, 1,
            2, 0, 0, 0, 2,
            1, 0, 0, 0, 1,
            2, 1, 2, 1, 2
    };
    static Block[] blockList = null;

    void checkMerge(World world, BlockPos pos)
    {
        // Check for complete circle.
        // C R C R C
        // R A A A R
        // C A A A C
        // R A A A R
        // C R B R C

        if(blockList == null)
        {
            blockList = new Block[]{
                    Blocks.AIR,
                    ProjectSGBlocks.SG_RING_BLOCK,
                    ProjectSGBlocks.SG_CHEVRON_BLOCK,
                    ProjectSGBlocks.SG_BASE_BLOCK
            };
        }
        BlockPos bottomPoint = new BlockPos(pos.getX()-2, pos.getY(), pos.getZ()); // TODO Figure out bottomPoint from any block in the structure.

        boolean merged = true;
        for(int x = 0; x < 5; x++)
        {
            for(int y = 0; y < 5; y++)
            {
                int arrayIndex = y * 5 + x;
                Block expectedBlock = blockList[pattern[arrayIndex]];

                BlockState state = world.getBlockState(new BlockPos(bottomPoint.getX()+x, bottomPoint.getY()+y, bottomPoint.getZ()));

                if(state.getBlock() != expectedBlock)
                {
                    merged = false;
                    break;
                }
            }
        }

        if(!merged)
            return;

        for(int x = 0; x < 5; x++)
        {
            for(int y = 0; y < 5; y++)
            {
                BlockPos targetPos = new BlockPos(bottomPoint.getX()+x, bottomPoint.getY()+y, bottomPoint.getZ());
                BlockState state = world.getBlockState(targetPos);
                BlockEntity entity = world.getBlockEntity(pos);

                if(state.getBlock() instanceof  AbstractStarGateBlock)
                    world.setBlockState(targetPos, state.with(MERGED, true));
                if(entity instanceof SGBaseBlockEntity)
                {
                    ((SGBaseBlockEntity) entity).merged = true;
                    entity.markDirty();
                }
            }
        }
    }

    void OnMergeCheckDone() {}
}

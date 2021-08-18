package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SGRingBlock extends AbstractStarGateBlock {
    public SGRingBlock(Settings settings) {
        super(settings);
    }

    @Override
    void tryMerge(World world, BlockState state, BlockPos pos) {
        /* A ring block can be in one of 8 positions.
        *
        *  01234
        * =======
        *   6 7
        *  4   5
        *  2   3
        *   0 1
        *
        * The portal can then also be rotated in one of two directions.
        * We need to figure out which one we are at.
        *
        * Could likely be made faster, but we only do the checks when someone places a block so...
         */

        for(int i = 0; i < 8; i++)
        {
            // Figure out our position in gate.
            int y = i/2 + (i > 3 ? 1 : 0);
            int xz = (i%2);
            if(i > 1 && i < 6)
                xz *= 4;
            else
                xz = 1 + (xz*2);

            //On X.
            if(checkMergePattern(world, pos.getX() - xz, pos.getY() - y, pos.getZ(), false))
                return;
            //On Z.
            if(checkMergePattern(world, pos.getX(), pos.getY() - y, pos.getZ() - xz, true))
                return;
        }

    }
}

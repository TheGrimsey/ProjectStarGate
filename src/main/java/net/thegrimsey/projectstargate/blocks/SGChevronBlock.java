package net.thegrimsey.projectstargate.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SGChevronBlock extends AbstractStarGateBlock {
    public SGChevronBlock(Settings settings) {
        super(settings);
    }

    @Override
    void tryMerge(World world, BlockState state, BlockPos pos) {
        /* A chevron block can be in one of 7 positions.
         *
         *  01234
         * =======
         *  4 5 6
         *
         *  2   3
         *
         *  0   1
         *
         * The portal can then also be rotated in one of two directions.
         * We need to figure out which one we are at.
         *
         * Could likely be made faster, but we only do the checks when someone places a block so...
         */

        for(int i = 0; i < 7; i++)
        {
            // Figure out our position in gate.
            int y = Math.min(i / 2 * 2, 4);
            int xz;

            if(i < 4)
                xz = (i%2) * 4;
            else
                xz = Math.min((i-4) * 2, 4);

            //On X.
            if(checkMergePattern(world, pos.getX() - xz, pos.getY() - y, pos.getZ(), false))
                return;
            //On Z.
            if(checkMergePattern(world, pos.getX(), pos.getY() - y, pos.getZ() - xz, true))
                return;
        }

    }
}

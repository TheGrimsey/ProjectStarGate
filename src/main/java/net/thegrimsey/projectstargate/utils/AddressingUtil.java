package net.thegrimsey.projectstargate.utils;

import net.minecraft.util.math.BlockPos;

public class AddressingUtil {
    static final int REGION_WIDTH = 32;
    static final int WORLD_WIDTH = 60000000;
    static final int REGION_MAX = WORLD_WIDTH / REGION_WIDTH;

    static final int GLYPH_COUNT = 39;
    static final int GLYPH_PER_COORDINATE = 4;
    static final int GLYPH_COORD_MAX = (int) Math.pow(GLYPH_COUNT, GLYPH_PER_COORDINATE);

    static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?!#";

    public static String GetAddressForLocation(BlockPos pos)
    {
        StringBuilder address = new StringBuilder();

        int regionX = pos.getX() / REGION_WIDTH;
        int regionZ = pos.getZ() / REGION_WIDTH;

        address.append(ConvertCoordinateToBase39(regionX));
        address.append(ConvertCoordinateToBase39(regionZ));

        return address.toString();
    }

    /*
    *   Converts a coordinate to a 4 glyph long sequence.
    *   Going from least significant to most significant glyph.
     */
    static String ConvertCoordinateToBase39(int coord)
    {
        StringBuilder result = new StringBuilder();

        coord += WORLD_WIDTH/2;

        while(coord > GLYPH_COUNT-1)
        {
            int remainder = coord % GLYPH_COUNT;
            coord = coord / GLYPH_COUNT;

            result.append(GLYPHS.charAt(remainder));
        }

        result.append(GLYPHS.charAt(coord));

        while(result.length() < 4)
            result.append(GLYPHS.charAt(0));

        return result.toString();
    }
}

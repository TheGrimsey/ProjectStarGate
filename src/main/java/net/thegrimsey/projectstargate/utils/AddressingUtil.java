package net.thegrimsey.projectstargate.utils;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class AddressingUtil {
    static final int REGION_WIDTH = 32;
    static final int WORLD_WIDTH = 60000000;

    static final int GLYPH_COUNT = 39;
    static final int GLYPH_PER_COORDINATE = 4;

    static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?!#";

    public static String GetAddressForLocation(BlockPos pos, Identifier dimensionID)
    {
        StringBuilder address = new StringBuilder();

        String xAddress = ConvertCoordinateToAddress(pos.getX());
        String zAddress = ConvertCoordinateToAddress(pos.getZ());

        for(int i = 0; i < GLYPH_PER_COORDINATE; i++)
        {
            address.append(xAddress.charAt(i));
            address.append(zAddress.charAt(i));
        }

        return address.toString();
    }

    /*
    *   Converts a coordinate to a 4 glyph long sequence.
    *   Going from least significant to most significant glyph.
     */
    static String ConvertCoordinateToAddress(int coordinate)
    {
        // Translate coordinate to region coordinate.
        coordinate = (coordinate + WORLD_WIDTH/2) / REGION_WIDTH;

        StringBuilder result = new StringBuilder();

        // Convert to base GLYPH_COUNT and get corresponding glyph.
        while(coordinate > GLYPH_COUNT-1)
        {
            int remainder = coordinate % GLYPH_COUNT;
            coordinate = coordinate / GLYPH_COUNT;

            result.append(GLYPHS.charAt(remainder));
        }
        result.append(GLYPHS.charAt(coordinate));

        // Pad remaining address length.
        while(result.length() < GLYPH_PER_COORDINATE)
            result.append(GLYPHS.charAt(0));

        return result.toString();
    }
}

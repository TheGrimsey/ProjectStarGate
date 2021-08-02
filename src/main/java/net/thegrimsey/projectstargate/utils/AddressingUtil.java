package net.thegrimsey.projectstargate.utils;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public class AddressingUtil {
    /*
    *   In Stargate addresses are 7-9 glyphs long.
    *   7 glyphs are not enough to give addresses to the world accurately enough.
    *   With 7 glyphs we could get one every 215 blocks. That's not often enough for me.
    *   So we kinda break lore and use 8 glyphs, dedicating the 9th one to dimension.
    *   This allows us an address for every 48 blocks.
    *
    *   CCCCCCCCD
     */
    static final int REGION_WIDTH = 48;
    static final int WORLD_WIDTH = 60000000;

    static final int GLYPH_COUNT = 36;
    static final int GLYPH_PER_COORDINATE = 4;

    public static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static @NotNull String GetAddressForLocation(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        StringBuilder address = new StringBuilder(GLYPH_PER_COORDINATE + GLYPH_PER_COORDINATE + 1);

        String xAddress = ConvertCoordinateToAddress(pos.getX());
        String zAddress = ConvertCoordinateToAddress(pos.getZ());

        for (int i = 0; i < GLYPH_PER_COORDINATE; i++)
            address.append(xAddress.charAt(i)).append(zAddress.charAt(i));

        // Get dimension id.
        try {
            byte dimensionGlyph = DimensionGlyphStorage.getInstance(world.getServer()).GetOrCreateDimensionGlyph(world.getRegistryKey().getValue().toString());
            address.append(GLYPHS.charAt(dimensionGlyph));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return address.toString();
    }

    /*
     *   Converts a coordinate to a 4 glyph long sequence.
     *   Going from most significant to least significant glyph.
     */
    static String ConvertCoordinateToAddress(int coordinate) {
        // Translate coordinate to region coordinate.
        coordinate = (coordinate + WORLD_WIDTH / 2) / REGION_WIDTH;

        StringBuilder result = new StringBuilder(GLYPH_PER_COORDINATE);

        // Convert to base GLYPH_COUNT and get corresponding glyph.
        while (coordinate > GLYPH_COUNT - 1) {
            int remainder = coordinate % GLYPH_COUNT;
            coordinate = coordinate / GLYPH_COUNT;

            result.append(GLYPHS.charAt(remainder));
        }
        result.append(GLYPHS.charAt(coordinate));

        // Pad remaining address length.
        while (result.length() < GLYPH_PER_COORDINATE)
            result.append(GLYPHS.charAt(0));

        return result.toString();
    }
}

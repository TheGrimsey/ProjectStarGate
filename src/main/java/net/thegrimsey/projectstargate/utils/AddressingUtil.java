package net.thegrimsey.projectstargate.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.thegrimsey.projectstargate.persistentstate.DimensionGlyphStorage;
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

    static final int GLYPH_PER_COORDINATE = 4;
    public static final int GLYPH_COUNT = 36;
    public static final int ADDRESS_LENGTH = GLYPH_PER_COORDINATE * 2 + 1; // 9

    public static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static long GetAddressForLocation(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        byte[] address = new byte[ADDRESS_LENGTH];

        byte[] xAddress = ConvertCoordinateToAddress(pos.getX());
        byte[] zAddress = ConvertCoordinateToAddress(pos.getZ());

        // Manually unrolled loop for interleaving coordinates. Does it perform better? Meh, maybe.
        address[0] = xAddress[0];
        address[1] = zAddress[0];
        address[2] = xAddress[1];
        address[3] = zAddress[1];
        address[4] = xAddress[2];
        address[5] = zAddress[2];
        address[6] = xAddress[3];
        address[7] = zAddress[3];

        // Get dimension id.
        try {
            byte dimensionGlyph = DimensionGlyphStorage.getInstance(world.getServer()).GetOrCreateDimensionGlyph(world.getRegistryKey().getValue().toString());
            address[8] = dimensionGlyph;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ConvertAddressBytesToLong(address);
    }

    /*
     *   Converts a coordinate to a 4 glyph long sequence.
     *   Going from the most significant to the least significant glyph.
     */
    private static byte[] ConvertCoordinateToAddress(int coordinate) {
        // Translate coordinate to region coordinate.
        coordinate = (coordinate + WORLD_WIDTH / 2) / REGION_WIDTH;

        byte[] result = new byte[GLYPH_PER_COORDINATE];

        // Convert to base GLYPH_COUNT and get corresponding glyph.
        int i = 0;
        while (coordinate > GLYPH_COUNT - 1) {
            result[i] = (byte) (coordinate % GLYPH_COUNT);
            coordinate = coordinate / GLYPH_COUNT;

            i++;
        }
        result[i] = (byte) coordinate;

        return result;
    }

    public static World GetWorldFromDimensionGlyph(@NotNull MinecraftServer server, byte glyph)
    {
        Identifier dimensionId = new Identifier(DimensionGlyphStorage.getInstance(server).GetDimensionIdentifierFromGlyph(glyph));

        return server.getWorld(RegistryKey.of(Registry.WORLD_KEY,dimensionId));
    }

    public static long ConvertAddressBytesToLong(byte[] address)
    {
        long result = 0;

        for(int i = ADDRESS_LENGTH-1; i >= 0; i--)
        {
            result += address[i] * Math.pow(GLYPH_COUNT, i);
        }

        return result;
    }

    public static byte[] ConvertLongAddressToByteArray(long address)
    {
        byte[] result = new byte[ADDRESS_LENGTH];

        for(int i = 0; i < ADDRESS_LENGTH; i++)
        {
            result[i] = (byte) (address % GLYPH_COUNT);
            address /= GLYPH_COUNT;
        }

        return result;
    }

    public static String ConvertLongToString(long address)
    {
        byte[] byteAddress = ConvertLongAddressToByteArray(address);

        StringBuilder stringBuilder = new StringBuilder(ADDRESS_LENGTH);
        for(byte glyph : byteAddress)
        {
            stringBuilder.append(GLYPHS.charAt(glyph));
        }
        return stringBuilder.toString();
    }
}

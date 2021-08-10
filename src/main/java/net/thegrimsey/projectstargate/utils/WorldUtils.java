package net.thegrimsey.projectstargate.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.thegrimsey.projectstargate.networking.GlobalAddressStorage;

public class WorldUtils {
    public static Pair<BlockPos, World> getPosAndWorldForAddress(MinecraftServer server, long address) {
        GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance(server);
        if (!globalAddressStorage.hasAddress(address))
            return null;

        BlockPos targetPos = globalAddressStorage.getPosFromAddress(address);
        byte dimensionGlyph = (byte) (address / 36 / 36 / 36 / 36 / 36 / 36 / 36 / 36); // This is kinda cursed.

        return new Pair<>(targetPos, AddressingUtil.GetWorldFromDimensionGlyph(server, dimensionGlyph));
    }

    public static void setChunkLoading(World world, BlockPos pos, boolean load) {
        ((ServerWorld) world).setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, load);
    }
}

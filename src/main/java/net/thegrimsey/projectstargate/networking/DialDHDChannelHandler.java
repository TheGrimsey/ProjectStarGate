package net.thegrimsey.projectstargate.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;

public class DialDHDChannelHandler implements ServerPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        byte[] address = buf.readByteArray(9);

        server.execute(() -> {
            boolean canReachDHD = player.squaredDistanceTo((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
            if(canReachDHD && player.getServerWorld().getBlockEntity(pos) instanceof DHDBlockEntity blockEntity)
            {

                // TODO Dial. We need a dial method for bytearray again
            }
        });
    }
}

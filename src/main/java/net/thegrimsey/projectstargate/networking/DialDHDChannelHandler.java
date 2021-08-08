package net.thegrimsey.projectstargate.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.blocks.entity.SGBaseBlockEntity;

public class DialDHDChannelHandler implements ServerPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        long address = buf.readLong();

        server.execute(() -> {
            boolean canReachDHD = player.squaredDistanceTo((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
            if(canReachDHD && player.getServerWorld().getBlockEntity(pos) instanceof DHDBlockEntity blockEntity)
            {
                if(player.getServerWorld().getBlockEntity(blockEntity.getStargatePos()) instanceof SGBaseBlockEntity stargate)
                    stargate.dial(address);
            }
        });
    }
}

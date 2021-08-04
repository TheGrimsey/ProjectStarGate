package net.thegrimsey.projectstargate;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.networking.DialDHDChannelHandler;

public class ProjectSGNetworking {
    private static final Identifier DIAL_DHD = new Identifier(ProjectStarGate.MODID, "dial_dhd_packet");
    private static final DialDHDChannelHandler DIAL_DHD_CHANNEL_HANDLER = new DialDHDChannelHandler();


    public static void registerNetworking()
    {
        // Dialing DHD packet.
        ServerPlayNetworking.registerGlobalReceiver(DIAL_DHD, DIAL_DHD_CHANNEL_HANDLER);
    }

    @Environment(EnvType.CLIENT)
    public static void sendDialDHDPacket(BlockPos pos, byte[] address)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeBlockPos(pos);
        buf.writeByteArray(address);

        ClientPlayNetworking.send(DIAL_DHD, buf);
    }
}

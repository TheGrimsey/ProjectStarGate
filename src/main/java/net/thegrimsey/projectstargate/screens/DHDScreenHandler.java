package net.thegrimsey.projectstargate.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.*;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.ProjectSGNetworking;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;

import java.util.Arrays;

public class DHDScreenHandler extends ScreenHandler {
    ScreenHandlerContext context;
    BlockPos dhdPos;

    @Environment(EnvType.CLIENT)
    byte dimension = -1;
    @Environment(EnvType.CLIENT)
    byte[] writtenAddress = new byte[AddressingUtil.ADDRESS_LENGTH];
    @Environment(EnvType.CLIENT)
    int writeHead = 0;


    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
        dhdPos = buf.readBlockPos();
        dimension = buf.readByte();

        Arrays.fill(writtenAddress, (byte) -1);

        context = ScreenHandlerContext.create(playerInventory.player.world, dhdPos);
    }

    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, DHDBlockEntity sourceDHD) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
        this.dhdPos = sourceDHD.getPos();

        context = ScreenHandlerContext.create(playerInventory.player.world, dhdPos);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ProjectSGBlocks.DHD_BLOCK);
    }

    public void dialGlyph(byte glyph)
    {
        if(glyph < 0 || glyph > AddressingUtil.GLYPH_COUNT || writeHead == AddressingUtil.ADDRESS_LENGTH)
            return;

        writtenAddress[writeHead] = glyph;
        writeHead++;
    }

    public void dialGate()
    {
        if(writeHead < 8)
            return; // Trying to dial with unfinished address;

        if(writeHead < 9)
            writtenAddress[8] = dimension;

        ProjectSGNetworking.sendDialDHDPacket(dhdPos, AddressingUtil.ConvertAddressBytesToLong(writtenAddress));
    }

    public void eraseGlyph() {
        if(writeHead > 0 && writeHead <= writtenAddress.length)
        {
            writeHead--;
            writtenAddress[writeHead] = -1;
        }
    }

    public DHDBlockEntity getDHD()
    {
        return context.get((world, blockPos) -> {
            if(world.getBlockEntity(blockPos) instanceof DHDBlockEntity dhdBlockEntity)
                return dhdBlockEntity;

            return null;
        }, null);
    }
}

package net.thegrimsey.projectstargate.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;

public class DHDScreenHandler extends ScreenHandler {
    BlockPos dhdPos;

    @Environment(EnvType.CLIENT)
    byte dimension = -1;
    @Environment(EnvType.CLIENT)
    byte[] writtenAddress;

    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
        dhdPos = buf.readBlockPos();
        dimension = buf.readByte();
        writtenAddress = new byte[AddressingUtil.ADDRESS_LENGTH];
    }

    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, DHDBlockEntity sourceDHD) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
        this.dhdPos = sourceDHD.getPos();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        BlockState blockState = player.world.getBlockState(dhdPos);

        return blockState.isOf(ProjectSGBlocks.DHD_BLOCK);
    }


}

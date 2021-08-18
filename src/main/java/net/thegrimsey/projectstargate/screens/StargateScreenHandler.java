package net.thegrimsey.projectstargate.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.utils.AddressingUtil;

public class StargateScreenHandler extends ScreenHandler {
    private final ScreenHandlerContext context;

    @Environment(EnvType.CLIENT)
    private String address;

    public StargateScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ProjectStarGate.STARGATE_SCREENHANDLER, syncId);

        context = null;
        address = AddressingUtil.ConvertLongToString(buf.readLong());
    }

    public StargateScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(ProjectStarGate.STARGATE_SCREENHANDLER, syncId);

        context = ScreenHandlerContext.create(playerInventory.player.world, pos);
    }

    @Environment(EnvType.CLIENT)
    public String getAddress() {
        return address;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ProjectSGBlocks.SG_BASE_BLOCK);
    }
}

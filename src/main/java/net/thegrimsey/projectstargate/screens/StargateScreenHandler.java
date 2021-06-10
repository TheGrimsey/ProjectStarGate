package net.thegrimsey.projectstargate.screens;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.thegrimsey.projectstargate.ProjectStarGate;

public class StargateScreenHandler extends ScreenHandler {
    private String address;

    public StargateScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ProjectStarGate.STARGATE_SCREENHANDLER, syncId);

        address = buf.readString();
    }

    public StargateScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ProjectStarGate.STARGATE_SCREENHANDLER, syncId);
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}

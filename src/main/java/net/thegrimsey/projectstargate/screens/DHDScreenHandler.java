package net.thegrimsey.projectstargate.screens;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.thegrimsey.projectstargate.ProjectStarGate;

public class DHDScreenHandler extends ScreenHandler {
    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
    }

    public DHDScreenHandler(int syncId, PlayerInventory playerInventory)
    {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}

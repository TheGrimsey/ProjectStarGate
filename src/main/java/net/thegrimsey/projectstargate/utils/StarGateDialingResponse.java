package net.thegrimsey.projectstargate.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;

public enum StarGateDialingResponse {
    SUCCESS,
    CANT_DIAL_SELF,
    INVALID_REMOTE_ADDRESS,
    NOT_ENOUGH_POWER,
    SELF_REQUIRES_DIMENSIONAL_UPGRADE,
    SELF_IS_REMOTE_CANT_DISCONNECT,
    REMOTE_LOCKED,
    REMOTE_INVALID;

    public static void HandleResponse(PlayerEntity player, StarGateDialingResponse response) {
        player.sendMessage(new TranslatableText("projectstargate.dialingresponse." + response.toString().toLowerCase()), false);
    }
}

package net.thegrimsey.projectstargate.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.ProjectSGNetworking;
import net.thegrimsey.projectstargate.ProjectSGSounds;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;

import java.util.Arrays;

public class DHDScreenHandler extends ScreenHandler {
    final ScreenHandlerContext context;

    @Environment(EnvType.CLIENT)
    BlockPos dhdPos;
    @Environment(EnvType.CLIENT)
    byte dimension = -1;
    @Environment(EnvType.CLIENT)
    byte[] writtenAddress = null;
    @Environment(EnvType.CLIENT)
    int writeHead = 0;
    @Environment(EnvType.CLIENT)
    Text text = null;
    @Environment(EnvType.CLIENT)
    PlayerEntity player;


    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);
        dhdPos = buf.readBlockPos();
        dimension = buf.readByte();

        player = playerInventory.player;
        context = ScreenHandlerContext.create(playerInventory.player.world, dhdPos);

        writtenAddress = new byte[AddressingUtil.ADDRESS_LENGTH];
        Arrays.fill(writtenAddress, (byte) -1);
        updateText();
    }

    public DHDScreenHandler(int syncId, PlayerInventory playerInventory, DHDBlockEntity sourceDHD) {
        super(ProjectStarGate.DHD_SCREENHANDLER, syncId);

        context = ScreenHandlerContext.create(playerInventory.player.world, sourceDHD.getPos());
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ProjectSGBlocks.DHD_BLOCK) && getDHD().getGate() != null;
    }

    @Environment(EnvType.CLIENT)
    public void writeGlyph(byte glyph)
    {
        if(glyph < 0 || glyph > AddressingUtil.GLYPH_COUNT || writeHead >= getDHD().getGate().getChevronCount())
            return;

        playButtonClickSound();

        writtenAddress[writeHead] = glyph;
        writeHead++;
        updateText();
    }

    @Environment(EnvType.CLIENT)
    public void eraseGlyph() {
        if(writeHead > 0 && writeHead <= writtenAddress.length)
        {
            playButtonClickSound();

            writeHead--;
            writtenAddress[writeHead] = -1;
            updateText();
        }
    }

    @Environment(EnvType.CLIENT)
    void playButtonClickSound() {
        player.world.playSound(player, dhdPos, ProjectSGSounds.DHD_BUTTON_CLICK_EVENT, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }
    @Environment(EnvType.CLIENT)
    void playDialSound() {
        player.world.playSound(player, dhdPos, ProjectSGSounds.DHD_DIAL_EVENT, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    @Environment(EnvType.CLIENT)
    public void dialGate()
    {
        playDialSound();

        if(writeHead < 8)
            return; // Trying to dial with unfinished address;

        // Fill in dimension glyph if it wasn't entered.
        if(writeHead < 9)
        {
            writtenAddress[8] = dimension;
            writeHead = 9;
            updateText();
        }

        // Send dial packet to server.
        ProjectSGNetworking.sendDialDHDPacket(dhdPos, AddressingUtil.ConvertAddressBytesToLong(writtenAddress));
    }

    @Environment(EnvType.CLIENT)
    public DHDBlockEntity getDHD()
    {
        return context.get((world, blockPos) -> {
            if(world.getBlockEntity(blockPos) instanceof DHDBlockEntity dhdBlockEntity)
                return dhdBlockEntity;

            return null;
        }, null);
    }

    @Environment(EnvType.CLIENT)
    void updateText()
    {
        StringBuilder textString = new StringBuilder(AddressingUtil.ADDRESS_LENGTH);
        for(int i = 0; i < getDHD().getGate().getChevronCount(); i++)
        {
            if(writtenAddress[i] == -1)
                textString.append('-');
            else
                textString.append(AddressingUtil.GLYPHS.charAt(writtenAddress[i]));
        }
        if(textString.length() > 8) textString.insert(8, ' ');
        textString.insert(4, ' ');

        text = Text.of(textString.toString());
    }
}

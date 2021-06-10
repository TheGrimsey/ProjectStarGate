package net.thegrimsey.projectstargate.blocks.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.screens.DHDScreenHandler;
import org.jetbrains.annotations.Nullable;

public class DHDBlockEntity extends BlockEntity implements BlockEntityClientSerializable, ExtendedScreenHandlerFactory {
    BlockPos stargatePos = null;

    @Environment(EnvType.CLIENT)
    boolean hasGate = false;

    public DHDBlockEntity(BlockPos pos, BlockState state) {
        super(ProjectSGBlocks.DHD_BLOCKENTITY, pos, state);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (stargatePos != null) {
            nbt.putInt("X", stargatePos.getX());
            nbt.putInt("Y", stargatePos.getY());
            nbt.putInt("Z", stargatePos.getZ());
        }

        return super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("X")) {
            int x = nbt.getInt("X");
            int y = nbt.getInt("Y");
            int z = nbt.getInt("Z");

            stargatePos = new BlockPos(x, y, z);
        }

        super.readNbt(nbt);
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        hasGate = tag.getBoolean("hasGate");
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        tag.putBoolean("hasGate", stargatePos != null);

        return tag;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {

    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new DHDScreenHandler(syncId, inv);
    }

    public BlockPos getStargatePos() {
        return stargatePos;
    }

    public void setStargatePos(BlockPos stargatePos) {
        this.stargatePos = stargatePos;
        markDirty();
    }

    @Environment(EnvType.CLIENT)
    public boolean hasGate() {
        return hasGate;
    }
}

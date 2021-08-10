package net.thegrimsey.projectstargate.blocks.entity;

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
import net.thegrimsey.projectstargate.persistentstate.DimensionGlyphStorage;
import org.jetbrains.annotations.Nullable;

public class DHDBlockEntity extends BlockEntity implements BlockEntityClientSerializable, ExtendedScreenHandlerFactory {
    BlockPos stargatePos = null;

    public DHDBlockEntity(BlockPos pos, BlockState state) {
        super(ProjectSGBlocks.DHD_BLOCKENTITY, pos, state);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (stargatePos != null) {
            nbt.putInt("gateX", stargatePos.getX());
            nbt.putInt("gateY", stargatePos.getY());
            nbt.putInt("gateZ", stargatePos.getZ());
        }

        return super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("gateX")) {
            int x = nbt.getInt("gateX");
            int y = nbt.getInt("gateY");
            int z = nbt.getInt("gateZ");

            stargatePos = new BlockPos(x, y, z);
        }

        super.readNbt(nbt);
    }

    @Override
    public void fromClientTag(NbtCompound tag) {

        if (tag.contains("gateX")) {
            int x = tag.getInt("gateX");
            int y = tag.getInt("gateY");
            int z = tag.getInt("gateZ");

            stargatePos = new BlockPos(x, y, z);
        }
        else
            stargatePos = null;
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        if (stargatePos != null) {
            tag.putInt("gateX", stargatePos.getX());
            tag.putInt("gateY", stargatePos.getY());
            tag.putInt("gateZ", stargatePos.getZ());
        }

        return tag;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(getPos());

        byte dimension = -1;
        try {
            dimension = DimensionGlyphStorage.getInstance(player.getServer()).GetOrCreateDimensionGlyph(player.getServerWorld().getRegistryKey().getValue().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        buf.writeByte(dimension);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new DHDScreenHandler(syncId, inv, this);
    }

    public BlockPos getStargatePos() {
        return stargatePos;
    }

    public void setStargatePos(BlockPos stargatePos) {
        this.stargatePos = stargatePos;
        markDirty();
    }

    public boolean hasGate() {
        return stargatePos != null;
    }

    public SGBaseBlockEntity getGate()
    {
        if(hasGate() && world.getBlockEntity(stargatePos) instanceof SGBaseBlockEntity sgBaseBlockEntity)
            return sgBaseBlockEntity;

        return null;
    }
}

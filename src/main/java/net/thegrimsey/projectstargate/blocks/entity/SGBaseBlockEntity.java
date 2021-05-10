package net.thegrimsey.projectstargate.blocks.entity;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.thegrimsey.projectstargate.ProjectSGBlocks;

public class SGBaseBlockEntity extends BlockEntity implements BlockEntityClientSerializable {
    public String address = "";

    public SGBaseBlockEntity() {
        super(ProjectSGBlocks.SG_BASE_BLOCKENTITY);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        tag.putString("address", address);

        return tag;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);

        address = tag.getString("address");
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        address = tag.getString("address");
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {

        tag.putString("address", address);

        return tag;
    }
}
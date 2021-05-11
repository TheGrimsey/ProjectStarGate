package net.thegrimsey.projectstargate.blocks.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Direction;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.utils.StarGateState;

public class SGBaseBlockEntity extends BlockEntity implements BlockEntityClientSerializable, Tickable {
    public String address = "";
    public boolean merged = false;
    public Direction facing = Direction.NORTH;

    // Runtime values. These are not saved.
    public StarGateState state = StarGateState.IDLE;
    public float ringRotation = 0f;
    public short engagedChevrons = 0; //Bitfield.
    public String dialedAddress = "";

    public SGBaseBlockEntity() {
        super(ProjectSGBlocks.SG_BASE_BLOCKENTITY);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        tag.putString("address", address);
        tag.putBoolean("merged", merged);
        tag.putByte("facing", (byte) facing.getId());

        return tag;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);

        address = tag.getString("address");
        merged = tag.getBoolean("merged");
        facing = Direction.byId(tag.getByte("facing"));
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {

        tag.putString("address", address);
        tag.putBoolean("merged", merged);
        tag.putByte("facing", (byte) facing.getId());

        tag.putFloat("ringRotation", ringRotation);
        tag.putShort("engagedChevrons", engagedChevrons);

        return tag;
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        address = tag.getString("address");
        merged = tag.getBoolean("merged");
        facing = Direction.byId(tag.getByte("facing"));

        ringRotation = tag.getFloat("ringRotation");
        engagedChevrons = tag.getShort("engagedChevrons");
    }

    public boolean IsChevronEngaged(int chevron)
    {
        return (engagedChevrons & (1 << chevron)) != 0;
    }
    public void SetChevronEngaged(int chevron)
    {
        engagedChevrons |= (1 << chevron);
    }
    public void UnsetChevron(int chevron)
    {
        engagedChevrons &= ~(1 << chevron);
    }

    @Override
    public void tick() {
        if(world == null)
            return;

        if(world.isClient())
        {
            clientUpdate();
        }
        else
        {
            serverUpdate();
        }
    }

    @Environment(EnvType.CLIENT)
    private void clientUpdate()
    {

    }

    private void serverUpdate()
    {
        if(state == StarGateState.CONNECTED)
        {
            // Teleport entities in the gate.
        }
    }
}

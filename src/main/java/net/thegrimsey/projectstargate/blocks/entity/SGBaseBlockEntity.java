package net.thegrimsey.projectstargate.blocks.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.utils.StarGateState;

public class SGBaseBlockEntity extends BlockEntity implements BlockEntityClientSerializable, Tickable {
    public String address = "";
    public boolean merged = false;
    public Direction facing = Direction.NORTH;

    // Runtime values. These are not saved.
    public StarGateState state = StarGateState.DIALING;
    public float ringRotation = 0f;
    public short engagedChevrons = 0; //Bitfield.
    public String remoteAddress = "";
    public SGBaseBlockEntity remoteEntity;

    // Client visuals.
    public float currentRingRotation = 0.f, lastRingRotation = 0.f, startRingAngle = 0.f;

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

        tag.putByte("state", StarGateState.toID(state));
        tag.putFloat("ringRotation", ringRotation);
        tag.putShort("engagedChevrons", engagedChevrons);

        return tag;
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        address = tag.getString("address");
        merged = tag.getBoolean("merged");
        facing = Direction.byId(tag.getByte("facing"));

        state = StarGateState.fromID(tag.getByte("state"));
        ringRotation = tag.getFloat("ringRotation");
        engagedChevrons = tag.getShort("engagedChevrons");
    }

    public boolean IsChevronEngaged(int chevron) {
        return (engagedChevrons & (1 << chevron)) != 0;
    }
    public void SetChevronEngaged(int chevron) {
        engagedChevrons |= (1 << chevron);
    }
    public void UnsetChevron(int chevron) {
        engagedChevrons &= ~(1 << chevron);
    }

    @Override
    public void tick() {
        if (world == null)
            return;

        if (world.isClient()) {
            clientUpdate();
        } else {
            serverUpdate();
        }
    }

    @Environment(EnvType.CLIENT)
    private void clientUpdate() {
        lastRingRotation = currentRingRotation;
        switch(state)
        {
            case IDLE:
                break;
            case DIALING:
            {
                /*
                *   TODO Correct interpolation.
                *   Each time the server rotates we want to rotate to that rotation.
                 */
                if(Math.abs(currentRingRotation - ringRotation) > 10.f)
                {
                    currentRingRotation = (currentRingRotation + 30.f/20.f) % 360;
                }
            }
                break;
            case CONNECTED:
                break;
            default:
                assert false;
        }
    }

    private void serverUpdate() {
        switch(state)
        {
            case IDLE:
                break;
            case DIALING:
                dialingUpdate();
                break;
            case CONNECTED:
                connectedUpdate();
                break;
            default:
                assert false;
        }
    }

    private void dialingUpdate() {
        // Rate limit to once every 20 ticks.
        if(world.getTime() % 20 == 0)
        {
            /*
            *   What we really want to do here is:
            *   - Check which chevron we are currently locking in.
            *   - Rotate ring to it.
            *   - Engage chevron.
            *   - Repeat until all chevrons locked.
            *   - Change to connected state.
            *
            *   To add to network complexity the inner ring switches direction everytime a chevron is locked in.
            *   I think ideally we just tell clients to start the dialing sequence with a set of rotations
            *   and have them do it themselves. Problem comes with new clients though.gi
             */
            // Rotate ring, engage chevrons.
            ringRotation = (ringRotation + 30.f) % 360;
            sync();
        }
    }

    private void connectedUpdate() {
        /*
        *   Drain power.
        *
        *   Teleport all entities moving through the portal.
        *   - Must translate position to correct position in destination portal.
         */
        // Drain energy. Teleport entities.
    }

    public float getInterpolatedRingRotation(float tickDelta)
    {
        return (currentRingRotation + (30.f/20.f)*tickDelta) % 360;
    }

    Box getTeleportBounds()
    {
        float minY = getPos().getY() + 1, maxY = getPos().getY() + 4;
        boolean onZ = facing == Direction.WEST || facing == Direction.EAST;
        float minX = getPos().getX() - (onZ ? 0 : 1), maxX = getPos().getX() + (onZ ? 0 : 1);
        float minZ = getPos().getZ() - (onZ ? 1 : 0), maxZ = getPos().getZ() + (onZ ? 1 : 0);

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}

package net.thegrimsey.projectstargate.blocks.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.screens.StargateScreenHandler;
import net.thegrimsey.projectstargate.utils.GlobalAddressStorage;
import net.thegrimsey.projectstargate.utils.StarGateState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SGBaseBlockEntity extends BlockEntity implements BlockEntityClientSerializable, ExtendedScreenHandlerFactory {
    public String address = "";
    public StarGateState gateState = StarGateState.IDLE;
    String remoteAddress = "";
    public Direction facing = Direction.NORTH;
    boolean merged = false;
    boolean isRemote = false;

    // Runtime values. These are not saved.
    float ringRotation = 0f;
    short engagedChevrons = 0; //Bitfield.
    int ticksInState = 0;

    SGBaseBlockEntity remoteGate;
    boolean needsInitialization = false;

    Box cachedBounds = null;

    // Client visuals.
    @Environment(EnvType.CLIENT)
    public float currentRingRotation = 0.f;
    @Environment(EnvType.CLIENT)
    public float lastRingRotation = 0.f;

    public SGBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ProjectSGBlocks.SG_BASE_BLOCKENTITY, pos, state);
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (world != null) {
            if (world.isClient()) {
                ((SGBaseBlockEntity) blockEntity).clientUpdate();
            } else {
                ((SGBaseBlockEntity) blockEntity).serverUpdate();
            }
        }

    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putString("address", address);
        tag.putBoolean("merged", merged);
        tag.putByte("facing", (byte) facing.getId());

        tag.putByte("state", StarGateState.toID(gateState));
        if (gateState != StarGateState.IDLE) {
            tag.putString("remoteAddress", remoteAddress);
            tag.putBoolean("isRemote", isRemote);
        }
        return tag;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        address = tag.getString("address");
        merged = tag.getBoolean("merged");
        facing = Direction.byId(tag.getByte("facing"));

        gateState = StarGateState.fromID(tag.getByte("state"));
        if (gateState != StarGateState.IDLE) {
            remoteAddress = tag.getString("remoteAddress");
            isRemote = tag.getBoolean("isRemote");

            needsInitialization = true;
        }
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        tag.putString("address", address);
        tag.putBoolean("merged", merged);
        tag.putByte("facing", (byte) facing.getId());

        tag.putByte("state", StarGateState.toID(gateState));
        tag.putFloat("ringRotation", ringRotation);
        tag.putShort("engagedChevrons", engagedChevrons);

        return tag;
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        address = tag.getString("address");
        merged = tag.getBoolean("merged");
        facing = Direction.byId(tag.getByte("facing"));

        gateState = StarGateState.fromID(tag.getByte("state"));
        ringRotation = tag.getFloat("ringRotation");
        engagedChevrons = tag.getShort("engagedChevrons");
    }

    public boolean IsChevronEngaged(int chevron) {
        return (engagedChevrons & (1 << chevron)) != 0;
    }

    @Environment(EnvType.CLIENT)
    private void clientUpdate() {
        lastRingRotation = currentRingRotation;
        switch (gateState) {
            case IDLE:
                break;
            case DIALING: {
                /*
                 *   TODO Correct interpolation.
                 *   Each time the server rotates we want to rotate to that rotation.
                 */
                if (Math.abs(currentRingRotation - ringRotation) > 10.f) {
                    currentRingRotation = (currentRingRotation + 30.f / 20.f) % 360;
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
        if (needsInitialization) {
            connect();
            needsInitialization = false;
        }

        ticksInState++;

        switch (gateState) {
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
        if (Objects.requireNonNull(world).getTime() % 20 == 0) {
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
             *   and have them do it themselves. Problem comes with new clients though.
             */
            // Rotate ring, engage chevrons.
            ringRotation = (ringRotation + 30.f) % 360;
            sync();
        }

        // TEMP
        if (!isRemote && ticksInState >= 60) {
            connect();
        }
    }

    void connectedUpdate() {
        if (isRemote)
            return;

        /*
         *   Drain power.
         *
         *   Teleport all entities moving through the portal.
         *   - Must translate position to correct position in destination portal.
         */
        // Drain energy. Teleport entities.

        if(cachedBounds == null)
            cachedBounds = getTeleportBounds();

        List<LivingEntity> entitiesInGate = Objects.requireNonNull(world).getEntitiesByClass(LivingEntity.class, cachedBounds, (livingEntity -> true));
        entitiesInGate.forEach(livingEntity -> {
            /*double dX = livingEntity.getX() - livingEntity.prevX;
            double dZ = livingEntity.getZ() - livingEntity.prevZ;
            double length = Math.abs(dX) + Math.abs(dZ);
            dX /= length;
            dZ /= length;*/

            livingEntity.teleport(remoteGate.getPos().getX(), remoteGate.getPos().getY() + 1, remoteGate.getPos().getZ());
        });

        // StarGates can only be open for 38 minutes. (SG:Atlantis S1E4)
        if (ticksInState >= 38 * (20 * 60)) {
            disconnect(false);
        }
    }

    public float getInterpolatedRingRotation(float tickDelta) {
        return (currentRingRotation + (30.f / 20.f) * tickDelta) % 360;
    }

    Box getTeleportBounds() {
        float minY = getPos().getY() + 1, maxY = getPos().getY() + 4;
        boolean onZ = facing == Direction.WEST || facing == Direction.EAST;
        float minX = getPos().getX() - (onZ ? 0 : 1), maxX = getPos().getX() + (onZ ? 0 : 1);
        float minZ = getPos().getZ() - (onZ ? 1 : 0), maxZ = getPos().getZ() + (onZ ? 1 : 0);

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean notMerged() {
        return !merged;
    }

    public void setMerged(boolean merged) {
        if (this.merged == merged)
            return;

        this.merged = merged;

        if (world instanceof ServerWorld serverWorld) {
            GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance(serverWorld);

            if (merged) {
                globalAddressStorage.addAddress(address, getPos());


            } else {
                globalAddressStorage.removeAddress(address, getPos());

                if (gateState != StarGateState.IDLE)
                    disconnect(true);
            }
        }
    }

    public void dial(String dialingAddress) {

        if (dialingAddress.equals(address)) {
            System.out.println("Dialing failed. Can't dial self: " + address);
            return;
        }
        // TODO Check is connected & if we can disconnect before dialing.

        // Check if homeAddress is already locked & if dialingAddress is as well.
        GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance((ServerWorld) Objects.requireNonNull(world));
        if (globalAddressStorage.isAddressLocked(address) || globalAddressStorage.isAddressLocked(dialingAddress)) {
            //FAILED. Can't dial a locked address.
            System.out.println("Dialing failed. One of the following addresses is already locked: " + address + ", " + dialingAddress);
            return;
        }

        // Check if remote gate is connected. If so we just instantly fail.
        BlockPos targetPos = getBlockPosForAddress(dialingAddress);
        if (targetPos == null)
            return;

        // Chunk loading.
        setChunkLoading(targetPos, true);
        setChunkLoading(pos, true);

        // Get target block entity.
        BlockEntity remoteBlockEntity = world.getBlockEntity(targetPos);
        if (!(remoteBlockEntity instanceof SGBaseBlockEntity)) {
            setChunkLoading(targetPos, false);
            setChunkLoading(pos, false);
            return;
        }

        remoteGate = (SGBaseBlockEntity) remoteBlockEntity;
        remoteGate.isRemote = true;
        remoteGate.remoteAddress = address;
        remoteGate.setState(StarGateState.DIALING);

        remoteAddress = dialingAddress;
        setState(StarGateState.DIALING);

        globalAddressStorage.lockAddress(address);
        globalAddressStorage.lockAddress(remoteAddress);

        sync();
    }

    void connect() {
        BlockPos targetPos = getBlockPosForAddress(remoteAddress);
        if (targetPos == null) {
            System.out.println("No valid position for address: " + remoteAddress);
            return;
        }

        BlockEntity remoteBlockEntity = Objects.requireNonNull(world).getBlockEntity(targetPos);
        if (!(remoteBlockEntity instanceof SGBaseBlockEntity)) {
            System.out.println("No valid block entity for position: " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ());
            GlobalAddressStorage.getInstance((ServerWorld) world).removeAddress(remoteAddress, targetPos);
            return;
        }

        setState(StarGateState.CONNECTED);
        engagedChevrons = 0b1_1111_1111;
        sync();
        markDirty();

        remoteGate = (SGBaseBlockEntity) remoteBlockEntity;
        remoteGate.setState(StarGateState.CONNECTED);
        remoteGate.engagedChevrons = engagedChevrons;
        remoteGate.sync();
        remoteGate.markDirty();
    }

    public void disconnect(boolean force) {
        if (gateState != StarGateState.CONNECTED)
            return;

        if (isRemote && !force) {
            System.out.println("Can't disconnect if remote gate. Only dialing gate can disconnect.");
            return;
        }

        //Unlock addresses.
        GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance((ServerWorld) Objects.requireNonNull(world));
        globalAddressStorage.unlockAddress(address);
        globalAddressStorage.unlockAddress(remoteAddress);

        // Stop chunk loading
        setChunkLoading(pos, false);
        setChunkLoading(remoteGate.pos, false);

        remoteGate.isRemote = false;
        remoteGate.remoteGate = null;
        remoteGate.remoteAddress = "";
        remoteGate.engagedChevrons = 0;
        remoteGate.setState(StarGateState.IDLE);

        remoteGate = null;
        remoteAddress = "";
        engagedChevrons = 0;
        setState(StarGateState.IDLE);

        sync();
    }

    private void setState(StarGateState newState) {
        gateState = newState;
        ticksInState = 0;
    }

    BlockPos getBlockPosForAddress(String address) {
        GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance((ServerWorld) Objects.requireNonNull(world));
        if (!globalAddressStorage.hasAddress(address))
            return null;

        return globalAddressStorage.getBlockPosFromAddress(address);
    }

    void setChunkLoading(BlockPos pos, boolean load) {
        ((ServerWorld) Objects.requireNonNull(world)).setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, load);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeString(address);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new StargateScreenHandler(syncId, inv);
    }
}

package net.thegrimsey.projectstargate.blocks.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
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
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.thegrimsey.projectstargate.ProjectSGBlocks;
import net.thegrimsey.projectstargate.blocks.SGBaseBlock;
import net.thegrimsey.projectstargate.client.renderers.StarGateRenderer;
import net.thegrimsey.projectstargate.networking.GlobalAddressStorage;
import net.thegrimsey.projectstargate.screens.StargateScreenHandler;
import net.thegrimsey.projectstargate.utils.AddressingUtil;
import net.thegrimsey.projectstargate.utils.StarGateDialingResponse;
import net.thegrimsey.projectstargate.utils.StarGateState;
import net.thegrimsey.projectstargate.utils.WorldUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class SGBaseBlockEntity extends BlockEntity implements BlockEntityClientSerializable, ExtendedScreenHandlerFactory {
    static float rotationSpeedDegrees = 30.0f;
    static double angleBetweenSymbols = 360.0 / AddressingUtil.GLYPH_COUNT;

    public long address = -1;
    StarGateState gateState = StarGateState.IDLE;
    boolean merged = false;
    public boolean dimensionalUpgrade = false; // TODO Merge boolean values into bitmask.

    boolean isRemote = false;
    long remoteAddress = -1;

    // Runtime values. These are not saved.
    float ringRotation = 0f;
    float rotTimeToChevron = 0f;
    byte engagedChevrons = 0;
    int ticksInState = 0;

    SGBaseBlockEntity remoteGate;
    boolean needsConnectionInitialization = false;
    Box cachedBounds = null;

    // Client visuals.
    @Environment(EnvType.CLIENT)
    public float currentRingRotation = 0.f;
    @Environment(EnvType.CLIENT)
    float lastRingRotation = 0.f;
    @Environment(EnvType.CLIENT)
    public float[][] eventHorizonZ; // Event Horizon Z positions. Initial array is band.
    @Environment(EnvType.CLIENT)
    float[][] eventHorizonZVelocity; // Event Horizon Z velocity. Initial array is band.
    @Environment(EnvType.CLIENT)
    int eventHorizonMovingPointsCount = 0; // Count not including outer layer.
    @Environment(EnvType.CLIENT)
    static Random random = new Random(); // For event horizon.

    public SGBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ProjectSGBlocks.SG_BASE_BLOCKENTITY, pos, state);

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            eventHorizonZ = new float[5][]; // So much data.
            eventHorizonZVelocity = new float[eventHorizonZ.length][]; // So much data

            //Initialize points
            for (int i = 0; i < eventHorizonZ.length - 1; i++)
            {
                eventHorizonZ[i] = new float[StarGateRenderer.ringSegmentCount];
                eventHorizonZVelocity[i] = new float[StarGateRenderer.ringSegmentCount];
            }
            eventHorizonZ[eventHorizonZ.length - 1] = new float[1]; // Final middle point.
            eventHorizonZVelocity[eventHorizonZVelocity.length - 1] = new float[1]; // Final middle point.


            // Count points
            for (int i = 1; i < eventHorizonZ.length; i++)
                eventHorizonMovingPointsCount += eventHorizonZ[i].length;
        }
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (world != null && blockEntity instanceof SGBaseBlockEntity baseBlockEntity) {
            baseBlockEntity.ticksInState++;

            if (world.isClient())
                baseBlockEntity.clientUpdate();
            else
                baseBlockEntity.serverUpdate();
        }

    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putLong("address", address);
        tag.putBoolean("merged", merged);

        tag.putByte("state", StarGateState.toID(gateState));
        if (gateState != StarGateState.IDLE) {
            tag.putLong("remoteAddress", remoteAddress);
            tag.putBoolean("isRemote", isRemote);
            tag.putShort("engagedChevrons", engagedChevrons);
        }
        return tag;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        address = tag.getLong("address");
        merged = tag.getBoolean("merged");

        gateState = StarGateState.fromID(tag.getByte("state"));
        if (gateState != StarGateState.IDLE) {
            remoteAddress = tag.getLong("remoteAddress");
            isRemote = tag.getBoolean("isRemote");
            engagedChevrons = tag.getByte("engagedChevrons");

            needsConnectionInitialization = true;
        }
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        tag.putBoolean("merged", merged);
        tag.putBoolean("dimensionalUpgrade", dimensionalUpgrade);

        tag.putByte("state", StarGateState.toID(gateState));
        tag.putFloat("ringRotation", ringRotation);
        tag.putShort("engagedChevrons", engagedChevrons);

        return tag;
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        merged = tag.getBoolean("merged");
        dimensionalUpgrade = tag.getBoolean("dimensionalUpgrade");

        StarGateState state = StarGateState.fromID(tag.getByte("state"));
        if (state != gateState)
        {
            //if(state == StarGateState.CONNECTED)
            //    applyOpeningPulse();

            changeState(state);
        }
        ringRotation = tag.getFloat("ringRotation");
        engagedChevrons = tag.getByte("engagedChevrons");
    }

    public boolean isChevronEngaged(int chevron) {
        return engagedChevrons > chevron;
    }

    public int getChevronCount() {
        return dimensionalUpgrade ? 9 : 8;
    }

    @Environment(EnvType.CLIENT)
    private void clientUpdate() {
        lastRingRotation = currentRingRotation;
        switch (gateState) {
            case IDLE:
                break;
            case CONNECTED:
                applyRandomImpulse();
                updateEventHorizonNew();
                break;
            case DIALING: {
                /*
                 *   TODO Correct interpolation.
                 *   Each time the server rotates we want to rotate to that rotation.
                 */
                if (Math.abs(currentRingRotation - ringRotation) > 10.f) {
                    float rotationDirection = engagedChevrons % 2 == 0 ? 1f : -1f;

                    currentRingRotation = (ringRotation + (30.f / 20.f) * rotationDirection) % 360;
                }
            }
            break;
            default:
                assert false;
        }
    }

    private void serverUpdate() {
        if (needsConnectionInitialization) {
            connect();
            needsConnectionInitialization = false;
        }

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
        if (world.getTime() % 20 == 0) {
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
            float rotationDirection = engagedChevrons % 2 == 0 ? 1f : -1f;

            ringRotation = (ringRotation + 30.f * rotationDirection) % 360;
            engagedChevrons++;
            sync();
        }

        if (!isRemote && engagedChevrons == 9) {
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
        List<LivingEntity> entitiesInGate = Objects.requireNonNull(world).getEntitiesByClass(LivingEntity.class, getTeleportBounds(), (livingEntity -> true));
        entitiesInGate.forEach(livingEntity -> {
            // TODO Only allow teleport when walking in from front.

            // TODO figure out better way to do teleport. This plays nether portal sound :(
            FabricDimensions.teleport(livingEntity, (ServerWorld) remoteGate.world,
                    new TeleportTarget(
                            new Vec3d(remoteGate.getPos().getX(), remoteGate.getPos().getY() + 1, remoteGate.getPos().getZ()),
                            livingEntity.getVelocity(),
                            livingEntity.getYaw(),
                            livingEntity.getPitch()));
        });

        // StarGates can only be open for 38 minutes. (SG:Atlantis S1E4)
        if (ticksInState >= 38 * (20 * 60)) {
            disconnect(false);
        }
    }

    @Environment(EnvType.CLIENT)
    public float getInterpolatedRingRotation(float tickDelta) {
        return (currentRingRotation + (rotationSpeedDegrees / 20.f) * tickDelta) % 360;
    }

    @Environment(EnvType.CLIENT)
    void updateEventHorizon() {
        /*
         *   Replicating the original mod's update event horizon.
         */
        // Apply random impulse.
        applyRandomImpulse();

        float[][] currentZ = eventHorizonZ;
        float[][] velocity = eventHorizonZVelocity;

        float m = eventHorizonZ.length;
        float n = StarGateRenderer.ringSegmentCount;

        float dt = 1.0f;
        float asq = 0.03f; // ???
        float d = 0.95f;

        // Original mod lays out data as such:
        // [32][5] where the outer array is index around and inner is band.
        // Meanwhile we do the reverse.
        // [5][32/1]
        // We also appear to be sorting our bands in the reverse. Where we have 0 as the outermost they have 0 as the innermost.

        for (int i = 1; i < m; i++)
            for (int j = 1; j < n; j++) {

                // Average of previous band & next. (0.5 * nextBand - previousBand)
                double du_dr = 0.5 * (currentZ[j][i+1] - currentZ[j][i-1]);
                // nextBand + previousBand - 2 * current;
                double d2u_drsq = currentZ[j][i+1] - 2 * currentZ[j][i] + currentZ[j][i-1];
                // nextIndex + previousIndex + 2 current
                double d2u_dthsq = currentZ[j+1][i] - 2 * currentZ[j][i] + currentZ[j-1][i];

                // d * velocity + (asq * dt) *(d2u_drsq + du_dr / band + d2u_dthsq / (band^2))
                velocity[j][i] = (float) (d * velocity[j][i] + (asq * dt) * (d2u_drsq + du_dr / i + d2u_dthsq / (i * i)));
            }

        // Add velocity * deltaTime onto current
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++)
                currentZ[j][i] += velocity[j][i] * dt;


        // Sum of z for second to middle band.
        float u0 = 0, v0 = 0;
        for (int j = 1; j <= n; j++) {
            u0 += currentZ[j][1];
            v0 += velocity[j][1];
        }
        // Calculate average of second to middle band
        u0 /= n;
        v0 /= n;
        // Set innermost band to average of second to innermost.
        for (int j = 1; j <= n; j++) {
            currentZ[j][0] = u0;
            velocity[j][0] = v0;
        }

    }

    void applyRandomImpulse() {
        int i = random.nextInt(eventHorizonMovingPointsCount);
        int currentBand = 1;

        while (i >= eventHorizonZVelocity[currentBand].length) {
            i -= eventHorizonZVelocity[currentBand].length;
            currentBand++;
        }

        eventHorizonZVelocity[currentBand][i] += random.nextGaussian() * 0.05f;
    }

    void updateEventHorizonNew()
    {
        float dt = 1.0f;
        float asq = 0.03f; // ???
        float d = 0.95f;

        for(int band = 1; band < eventHorizonZ.length-1; band++)
        {
            for(int i = 0; i < eventHorizonZ[band].length; i++)
            {
                // Half difference between previous band & next. (0.5 * nextBand - previousBand)
                float halfDifference = 0.5f * (eventHorizonZ[band-1][i] - eventHorizonZ[band+1][i % eventHorizonZ[band+1].length]);

                // nextBand + previousBand - 2 * current;
                float d2u_drsq = eventHorizonZ[band-1][i] + eventHorizonZ[band+1][i % eventHorizonZ[band+1].length] - 2.0f * eventHorizonZ[band][i];

                // nextIndex + previousIndex - 2 current
                int nextIndex = (i+1) % eventHorizonZ[band].length;
                int previousIndex = (i-1 + eventHorizonZ[band].length) % eventHorizonZ[band].length;
                float d2u_dthsq = eventHorizonZ[band][nextIndex] + eventHorizonZ[band][previousIndex] - 2.0f * eventHorizonZ[band][i];

                eventHorizonZVelocity[band][i] = d * eventHorizonZVelocity[band][i] + (asq * dt) * (d2u_drsq + halfDifference / band + d2u_dthsq / (band * band));
            }
        }

        // Apply velocity to current.
        for(int band = 1; band < eventHorizonZ.length; band++)
            for(int i = 0; i < eventHorizonZ[band].length; i++)
                eventHorizonZ[band][i] += eventHorizonZVelocity[band][i] * dt;

        // Sum of z for second to middle band.
        float u0 = 0, v0 = 0;
        for (int i = 0; i < eventHorizonZ[eventHorizonZ.length-2].length; i++) {
            u0 += eventHorizonZ[eventHorizonZ.length-2][i];
            v0 += eventHorizonZVelocity[eventHorizonZ.length-2][i];
        }
        // Calculate average of second to middle band
        u0 /= eventHorizonZ[eventHorizonZ.length-2].length;
        v0 /= eventHorizonZ[eventHorizonZ.length-2].length;
        // Set innermost band to average of second to innermost.
        eventHorizonZ[eventHorizonZ.length-1][0] = u0;
        eventHorizonZVelocity[eventHorizonZVelocity.length-1][0] = v0;
    }

    @Environment(EnvType.CLIENT)
    void applyOpeningPulse()
    {
        for (int band = 1; band < eventHorizonZ.length; band++) {
            for (int i = 0; i < eventHorizonZ[band].length; i++) {
                float z = (float)(band+1) / (eventHorizonZ.length) * 5.0f;
                eventHorizonZ[band][i] = (float) (z + (random.nextGaussian() * 0.2f));
            }
        }
    }

    Box getTeleportBounds() {
        if (cachedBounds == null) {
            float minY = getPos().getY() + 1, maxY = getPos().getY() + 4;
            boolean onZ = getFacing() == Direction.WEST || getFacing() == Direction.EAST;
            float minX = getPos().getX() - (onZ ? 0 : 1), maxX = getPos().getX() + (onZ ? 0 : 1);
            float minZ = getPos().getZ() - (onZ ? 1 : 0), maxZ = getPos().getZ() + (onZ ? 1 : 0);

            cachedBounds = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        return cachedBounds;
    }

    public Direction getFacing() {
        return getCachedState().get(SGBaseBlock.FACING);
    }

    public boolean isActive() {
        return gateState != StarGateState.IDLE;
    }

    public boolean isConnected() {
        return gateState == StarGateState.CONNECTED;
    }

    public boolean notMerged() {
        return !merged;
    }

    public void setMerged(boolean merged) {
        if (this.merged == merged)
            return;

        this.merged = merged;

        if (world instanceof ServerWorld serverWorld) {
            GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance(serverWorld.getServer());

            if (merged) {
                globalAddressStorage.addAddress(address, getPos());
            } else {
                globalAddressStorage.removeAddress(address, getPos());

                if (gateState != StarGateState.IDLE)
                    disconnect(true);
            }
        }
    }

    public StarGateDialingResponse dial(long dialingAddress) {
        if (dialingAddress == address) {
            System.out.println("Dialing failed. Can't dial self: " + address);
            return StarGateDialingResponse.CANT_DIAL_SELF;
        }

        // If we are connected/dialing try to disconnect. If disconnect fails return.
        if (gateState != StarGateState.IDLE && !disconnect(false))
            return StarGateDialingResponse.SELF_IS_REMOTE_CANT_DISCONNECT;

        // Restrict cross-dimensional dialing without upgrade.
        if (!dimensionalUpgrade) {
            byte targetDimension = (byte) (dialingAddress / 36 / 36 / 36 / 36 / 36 / 36 / 36 / 36);
            byte selfDimension = (byte) (address / 36 / 36 / 36 / 36 / 36 / 36 / 36 / 36);

            if (targetDimension != selfDimension)
                return StarGateDialingResponse.SELF_REQUIRES_DIMENSIONAL_UPGRADE;
        }

        // Check if homeAddress is already locked & if dialingAddress is as well.
        GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance(world.getServer());
        if (globalAddressStorage.isAddressLocked(address) || globalAddressStorage.isAddressLocked(dialingAddress)) {
            //FAILED. Can't dial a locked address.
            return StarGateDialingResponse.REMOTE_LOCKED;
        }

        // Check if remote gate is connected. If so we just instantly fail.
        Pair<BlockPos, World> target = WorldUtils.getPosAndWorldForAddress(world.getServer(), dialingAddress);
        if (target == null)
            return StarGateDialingResponse.INVALID_REMOTE_ADDRESS;

        // Load target chunk
        target.getRight().getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        // Get target block entity.
        BlockEntity remoteBlockEntity = target.getRight().getBlockEntity(target.getLeft());
        if (!(remoteBlockEntity instanceof SGBaseBlockEntity)) {
            GlobalAddressStorage.getInstance(world.getServer()).removeAddress(dialingAddress, target.getLeft()); // Remove address location since there isn't actually a stargate there.
            return StarGateDialingResponse.REMOTE_INVALID;
        }

        // Keep target chunks loaded.
        WorldUtils.setChunkLoading(target.getRight(), target.getLeft(), true);
        WorldUtils.setChunkLoading(world, pos, true);

        remoteGate = (SGBaseBlockEntity) remoteBlockEntity;
        remoteGate.isRemote = true;
        remoteGate.remoteAddress = address;
        remoteGate.changeState(StarGateState.DIALING);

        remoteAddress = dialingAddress;
        changeState(StarGateState.DIALING);

        globalAddressStorage.lockAddress(address);
        globalAddressStorage.lockAddress(remoteAddress);

        sync();
        return StarGateDialingResponse.SUCCESS;
    }

    void connect() {
        Pair<BlockPos, World> target = WorldUtils.getPosAndWorldForAddress(world.getServer(), remoteAddress);
        if (target == null) {
            System.out.println("No valid position for address: " + remoteAddress);
            return;
        }

        BlockEntity remoteBlockEntity = target.getRight().getBlockEntity(target.getLeft());
        if (!(remoteBlockEntity instanceof SGBaseBlockEntity)) {
            System.out.println("No valid block entity for position: " + target.getLeft().getX() + ", " + target.getLeft().getY() + ", " + target.getLeft().getZ());
            GlobalAddressStorage.getInstance(world.getServer()).removeAddress(remoteAddress, target.getLeft());
            return;
        }

        changeState(StarGateState.CONNECTED);
        sync();
        markDirty();

        remoteGate = (SGBaseBlockEntity) remoteBlockEntity;
        remoteGate.changeState(StarGateState.CONNECTED);
        remoteGate.engagedChevrons = engagedChevrons;
        remoteGate.sync();
        remoteGate.markDirty();
    }

    // Returns true if we successfully disconnected.
    public boolean disconnect(boolean force) {
        if (gateState != StarGateState.CONNECTED)
            return true;

        if (isRemote && !force) {
            System.out.println("Can't disconnect if remote gate. Only dialing gate can disconnect.");
            return false;
        }

        //Unlock addresses.
        GlobalAddressStorage globalAddressStorage = GlobalAddressStorage.getInstance(world.getServer());
        globalAddressStorage.unlockAddress(address);
        globalAddressStorage.unlockAddress(remoteAddress);

        // Stop chunk loading
        WorldUtils.setChunkLoading(world, pos, false);
        WorldUtils.setChunkLoading(remoteGate.world, remoteGate.pos, false);

        remoteGate.isRemote = false;
        remoteGate.remoteGate = null;
        remoteGate.remoteAddress = -1;
        remoteGate.engagedChevrons = 0;
        remoteGate.changeState(StarGateState.IDLE);

        remoteGate = null;
        remoteAddress = -1;
        engagedChevrons = 0;
        changeState(StarGateState.IDLE);

        sync();
        return true;
    }

    private void changeState(StarGateState newState) {
        gateState = newState;
        ticksInState = 0;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(getPos());
        buf.writeLong(address);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new StargateScreenHandler(syncId, inv, this.getPos());
    }
}

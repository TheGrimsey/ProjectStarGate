package net.thegrimsey.projectstargate.networking;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;

public class GlobalAddressStorage extends PersistentState {
    // Set containing all known StarGate addresses and the positions of all SGBaseBlocks with that address.
    final HashMap<Long, HashSet<BlockPos>> worldAddresses;

    // All addresses which are currently connected. Only one gate from each address can be dialed at the time.
    final HashSet<Long> lockedAddresses;

    public GlobalAddressStorage() {
        worldAddresses = new HashMap<>();
        lockedAddresses = new HashSet<>();
    }

    public static GlobalAddressStorage getInstance(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(GlobalAddressStorage::fromNbt, GlobalAddressStorage::new, "StarGate_GlobalAddressStorage");
    }

    private static GlobalAddressStorage fromNbt(NbtCompound tag) {
        GlobalAddressStorage globalAddressStorage = new GlobalAddressStorage();

        NbtCompound addressesTag = tag.getCompound("addresses");

        addressesTag.getKeys().forEach((s -> {
            NbtCompound addressSet = addressesTag.getCompound(s);
            HashSet<BlockPos> addresses = new HashSet<>(addressSet.getKeys().size());

            addressSet.getKeys().forEach((positionKey -> {
                NbtCompound positionTag = addressSet.getCompound(positionKey);
                int X = positionTag.getInt("X");
                int Y = positionTag.getInt("Y");
                int Z = positionTag.getInt("Z");

                addresses.add(new BlockPos(X, Y, Z));
            }));

            globalAddressStorage.worldAddresses.put(Long.parseLong(s), addresses);
        }));

        return globalAddressStorage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtCompound addresses = new NbtCompound();

        worldAddresses.forEach((addressLong, blockPosSet) -> {
            NbtCompound address = new NbtCompound();
            blockPosSet.forEach(blockPos -> {
                NbtCompound positionTag = new NbtCompound();

                positionTag.putInt("X", blockPos.getX());
                positionTag.putInt("Y", blockPos.getY());
                positionTag.putInt("Z", blockPos.getZ());

                address.put(String.valueOf(address.getKeys().size()), positionTag);
            });
            addresses.put(Long.toString(addressLong), address);
        });

        tag.put("addresses", addresses);

        return tag;
    }

    public void addAddress(long address, BlockPos position) {
        if (!worldAddresses.containsKey(address))
            worldAddresses.put(address, new HashSet<>(1));
        worldAddresses.get(address).add(position);

        markDirty();
    }

    public void removeAddress(long address, BlockPos position) {
        if (!worldAddresses.containsKey(address))
            return;

        HashSet<BlockPos> positionSet = worldAddresses.get(address);
        positionSet.remove(position);

        if(positionSet.isEmpty())
            worldAddresses.remove(address);

        markDirty();
    }

    public boolean hasAddress(long address) {
        return worldAddresses.containsKey(address) && !worldAddresses.get(address).isEmpty();
    }

    public BlockPos getPosFromAddress(long address) {
        return (BlockPos) worldAddresses.get(address).toArray()[0];
    }

    public boolean isAddressLocked(long address) {
        return lockedAddresses.contains(address);
    }

    public void lockAddress(long address) {
        lockedAddresses.add(address);
    }

    public void unlockAddress(long address) {
        lockedAddresses.remove(address);
    }
}

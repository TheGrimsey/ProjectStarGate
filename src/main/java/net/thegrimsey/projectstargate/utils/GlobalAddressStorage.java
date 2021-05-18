package net.thegrimsey.projectstargate.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class GlobalAddressStorage extends PersistentState {
    // Set containing all known StarGate addresses and the positions of all SGBaseBlocks with that address.
    HashMap<String, HashSet<BlockPos>> worldAddresses;

    // All addresses which are currently connected. Only one gate from each address can be dialed at the time.
    HashSet<String> lockedAddresses;

    public GlobalAddressStorage() {
        super("StarGate_GlobalAddressStorage");

        worldAddresses = new HashMap<>();
        lockedAddresses = new HashSet<>();
    }

    @Override
    public void fromTag(CompoundTag tag) {
        CompoundTag addressesTag = tag.getCompound("addresses");

        addressesTag.getKeys().forEach((s -> {
            CompoundTag addressSet = addressesTag.getCompound(s);
            HashSet<BlockPos> addresses = new HashSet<>(addressSet.getKeys().size());

            addressSet.getKeys().forEach((positionKey -> {
                CompoundTag positionTag = addressSet.getCompound(positionKey);
                int X = positionTag.getInt("X");
                int Y = positionTag.getInt("Y");
                int Z = positionTag.getInt("Z");

                addresses.add(new BlockPos(X, Y, Z));
            }));

            worldAddresses.put(s, addresses);
        }));
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        CompoundTag addresses = new CompoundTag();

        worldAddresses.forEach((s, blockPosSet) -> {
            CompoundTag address = new CompoundTag();
            blockPosSet.forEach(blockPos -> {
                CompoundTag positionTag = new CompoundTag();

                positionTag.putInt("X", blockPos.getX());
                positionTag.putInt("Y", blockPos.getY());
                positionTag.putInt("Z", blockPos.getZ());

                address.put(String.valueOf(address.getKeys().size()), positionTag);
            });
            addresses.put(s, address);
        });

        tag.put("addresses", addresses);

        return tag;
    }

    public void addAddress(String address, BlockPos position)
    {
        if(!worldAddresses.containsKey(address))
            worldAddresses.put(address, new HashSet<BlockPos>(1));
        worldAddresses.get(address).add(position);

        markDirty();
    }
    public void removeAddress(String address, BlockPos position)
    {
        if(!worldAddresses.containsKey(address))
            return;

        HashSet<BlockPos> positionSet = worldAddresses.get(address);
        positionSet.remove(position);

        markDirty();
    }
    public boolean hasAddress(String address)
    {
        return worldAddresses.containsKey(address) && !worldAddresses.get(address).isEmpty();
    }
    public BlockPos getBlockPosFromAddress(String address)
    {
        return (BlockPos) worldAddresses.get(address).toArray()[0];
    }

    public boolean isAddressLocked(String address)
    {
        return lockedAddresses.contains(address);
    }

    public void lockAddress(String address)
    {
        lockedAddresses.add(address);
    }
    public void unlockAddress(String address)
    {
        lockedAddresses.remove(address);
    }
}

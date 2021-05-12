package net.thegrimsey.projectstargate.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.PersistentState;

public class GlobalAddressStorage extends PersistentState {
    public GlobalAddressStorage(String key) {
        super(key);
    }

    @Override
    public void fromTag(CompoundTag tag) {

    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        return null;
    }
}

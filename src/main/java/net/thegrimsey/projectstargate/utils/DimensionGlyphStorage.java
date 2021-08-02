package net.thegrimsey.projectstargate.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

public class DimensionGlyphStorage extends PersistentState {
    HashMap<String, Byte> dimensionGlyphs;

    public DimensionGlyphStorage()
    {
        dimensionGlyphs = new HashMap<>();
    }
    public static DimensionGlyphStorage getInstance(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(DimensionGlyphStorage::fromNbt, DimensionGlyphStorage::new, "StarGate_DimensionGlyphStorage");
    }

    private static DimensionGlyphStorage fromNbt(NbtCompound tag) {
        DimensionGlyphStorage dimensionGlyphStorage = new DimensionGlyphStorage();

        tag.getKeys().forEach(s -> dimensionGlyphStorage.dimensionGlyphs.put(s, tag.getByte(s)));

        return dimensionGlyphStorage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        dimensionGlyphs.forEach(nbt::putByte);

        return nbt;
    }

    public byte GetOrCreateDimensionGlyph(String identifier) throws Exception {
        if(!dimensionGlyphs.containsKey(identifier))
        {
            if(dimensionGlyphs.size() == AddressingUtil.GLYPH_COUNT)
                throw new Exception("Too many dimensions."); //

            dimensionGlyphs.put(identifier, (byte) dimensionGlyphs.size());
            markDirty();
        }

        return dimensionGlyphs.get(identifier);
    }

    public String GetDimensionIdentifierFromGlyph(byte glyph)
    {
        for(Map.Entry<String, Byte> entry : dimensionGlyphs.entrySet()) {
            if(entry.getValue() == glyph)
                return entry.getKey();
        }

        return "???";
    }
}

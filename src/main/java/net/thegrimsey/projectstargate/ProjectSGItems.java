package net.thegrimsey.projectstargate;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ProjectSGItems {

    public static final Item NAQUADAH_RAW = new Item(new FabricItemSettings().group(ProjectStarGate.ITEM_GROUP));
    public static final Item NAQUADAH_INGOT = new Item(new FabricItemSettings().group(ProjectStarGate.ITEM_GROUP));

    public static void registerItems()
    {
        registerItem("naquadah_raw", NAQUADAH_RAW);
        registerItem("naquadah_ingot", NAQUADAH_INGOT);
    }

    static void registerItem(String Id, Item item)
    {
        Registry.register(Registry.ITEM, new Identifier(ProjectStarGate.MODID, Id), item);
    }
}

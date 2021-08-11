package net.thegrimsey.projectstargate;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ProjectSGSounds {
    static final Identifier DHD_BUTTON_CLICK_ID = new Identifier(ProjectStarGate.MODID, "dhd_button_click");
    public static SoundEvent DHD_BUTTON_CLICK_EVENT = new SoundEvent(DHD_BUTTON_CLICK_ID);

    static final Identifier DHD_DIAL_ID = new Identifier(ProjectStarGate.MODID, "dhd_dial");
    public static SoundEvent DHD_DIAL_EVENT = new SoundEvent(DHD_DIAL_ID);

    public static void registerSounds()
    {
        Registry.register(Registry.SOUND_EVENT, DHD_BUTTON_CLICK_ID, DHD_BUTTON_CLICK_EVENT);
        Registry.register(Registry.SOUND_EVENT, DHD_DIAL_ID, DHD_DIAL_EVENT);
    }
}

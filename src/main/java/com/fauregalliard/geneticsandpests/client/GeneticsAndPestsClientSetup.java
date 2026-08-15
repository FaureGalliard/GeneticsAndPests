package com.fauregalliard.geneticsandpests.client;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;

/** Client-only registrations that have to happen on the mod event bus. */
@EventBusSubscriber(modid = GeneticsAndPests.MODID, value = Dist.CLIENT)
public final class GeneticsAndPestsClientSetup {
    @SubscribeEvent
    public static void onRegisterDebugEntries(RegisterDebugEntriesEvent event) {
        event.register(PlantDebugEntry.ID, new PlantDebugEntry());

        // Registering alone only makes the entry available; it also has to be put in a profile.
        // IN_OVERLAY means "shown while the debug overlay is open", which is what F3 opens.
        // ALWAYS_ON is a different thing entirely — a permanent HUD line during normal play.
        event.includeInProfile(PlantDebugEntry.ID, DebugScreenProfile.DEFAULT,
                DebugScreenEntryStatus.IN_OVERLAY);
    }

    private GeneticsAndPestsClientSetup() {}
}

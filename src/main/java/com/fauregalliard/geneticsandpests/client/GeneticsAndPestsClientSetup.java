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

        // Registering alone only makes the entry available; without being put in a profile it never
        // actually draws, which is why plain F3 showed nothing.
        event.includeInProfile(PlantDebugEntry.ID, DebugScreenProfile.DEFAULT,
                DebugScreenEntryStatus.ALWAYS_ON);
    }

    private GeneticsAndPestsClientSetup() {}
}

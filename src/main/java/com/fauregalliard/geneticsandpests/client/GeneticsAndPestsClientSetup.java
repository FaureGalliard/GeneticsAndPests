package com.fauregalliard.geneticsandpests.client;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

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
    }

    private GeneticsAndPestsClientSetup() {}
}

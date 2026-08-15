package com.fauregalliard.geneticsandpests.data;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for {@code gradlew runData}, which writes generated JSON into
 * {@code src/generated/resources} — already on the resource path in build.gradle.
 *
 * <p>Client and server data are gathered separately: models are a client concern, recipes are a
 * datapack one, and asking for them on the wrong side simply does nothing.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class ModDataGenerators {
    @SubscribeEvent
    public static void onGatherClientData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
    }

    @SubscribeEvent
    public static void onGatherServerData(GatherDataEvent.Server event) {
        event.createProvider(ModRecipeProvider.Runner::new);
    }

    private ModDataGenerators() {}
}

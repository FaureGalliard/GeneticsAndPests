package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GeneticsAndPests.MODID);

    /** Attached to seed items so a genome survives being picked up, stored and replanted. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlantGenes>> PLANT_GENES =
            DATA_COMPONENTS.register("plant_genes", () -> DataComponentType.<PlantGenes>builder()
                    .persistent(PlantGenes.CODEC)
                    .networkSynchronized(PlantGenes.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}
}

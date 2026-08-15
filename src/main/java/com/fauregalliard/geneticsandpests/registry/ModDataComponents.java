package com.fauregalliard.geneticsandpests.registry;

import com.mojang.serialization.Codec;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.Disease;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;
import com.fauregalliard.geneticsandpests.genetics.ScionData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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

    /** Rides on harvested seeds so Smut can travel in your inventory instead of through the soil. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Disease>> SEED_DISEASE =
            DATA_COMPONENTS.register("seed_disease", () -> DataComponentType.<Disease>builder()
                    .persistent(Disease.CODEC)
                    .networkSynchronized(Disease.STREAM_CODEC)
                    .build());

    /** Marks produce grown on an Ergot-infected plant: it looks ordinary and is not. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TAINTED =
            DATA_COMPONENTS.register("tainted", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** The trait and level a scion carries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ScionData>> SCION =
            DATA_COMPONENTS.register("scion", () -> DataComponentType.<ScionData>builder()
                    .persistent(ScionData.CODEC)
                    .networkSynchronized(ScionData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}
}

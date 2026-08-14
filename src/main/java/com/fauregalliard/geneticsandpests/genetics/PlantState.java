package com.fauregalliard.geneticsandpests.genetics;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

/**
 * Everything the mod remembers about one planted crop: what it inherited and what ails it.
 *
 * <p>The disease fields are optional in the codec, so a world saved before diseases existed still
 * loads with its genomes intact and simply reads as healthy.
 */
public record PlantState(PlantGenes genes, Optional<Disease> disease, int infectedFor) {
    /**
     * Written inline into its containing entry rather than under a key of its own, so a world saved
     * before diseases existed — when the entry held nothing but {@code genes} — still loads.
     */
    public static final MapCodec<PlantState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PlantGenes.CODEC.fieldOf("genes").forGetter(PlantState::genes),
            Disease.CODEC.optionalFieldOf("disease").forGetter(PlantState::disease),
            Codec.INT.optionalFieldOf("infected_for", 0).forGetter(PlantState::infectedFor)
    ).apply(instance, PlantState::new));

    public static final Codec<PlantState> CODEC = MAP_CODEC.codec();

    public static PlantState healthy(PlantGenes genes) {
        return new PlantState(genes, Optional.empty(), 0);
    }

    @Nullable
    public Disease diseaseOrNull() {
        return this.disease.orElse(null);
    }

    public boolean isDiseased() {
        return this.disease.isPresent();
    }

    public PlantState withGenes(PlantGenes newGenes) {
        return new PlantState(newGenes, this.disease, this.infectedFor);
    }

    public PlantState infectedWith(Disease newDisease) {
        return new PlantState(this.genes, Optional.of(newDisease), 0);
    }

    public PlantState cured() {
        return new PlantState(this.genes, Optional.empty(), 0);
    }

    /** One more pass spent sick, which is how Blight counts down to killing the plant. */
    public PlantState aged() {
        return new PlantState(this.genes, this.disease, this.infectedFor + 1);
    }
}

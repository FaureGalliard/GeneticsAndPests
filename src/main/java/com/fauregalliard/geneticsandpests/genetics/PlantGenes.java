package com.fauregalliard.geneticsandpests.genetics;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import com.fauregalliard.geneticsandpests.Config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The genome of a plant: one value per {@link Gene}.
 *
 * <p>Genomes ride along on ordinary seed items as a data component, which is what lets crops from
 * other mods take part without any code of theirs changing. Genes missing from the map read back
 * as {@link #MIN_VALUE}, so old saves and hand-written data keep working when new genes are added.
 */
public record PlantGenes(Map<Gene, Integer> values) {
    /** The value every gene starts at. A genome of all-minimum behaves exactly like vanilla. */
    public static final int MIN_VALUE = 1;

    /** The genome of any seed that was never bred. */
    public static final PlantGenes DEFAULT = new PlantGenes(Map.of());

    public static final Codec<PlantGenes> CODEC =
            Codec.unboundedMap(Gene.CODEC, Codec.INT).xmap(PlantGenes::new, PlantGenes::values);

    public static final StreamCodec<ByteBuf, PlantGenes> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, Gene.STREAM_CODEC, ByteBufCodecs.VAR_INT)
                    .map(PlantGenes::new, genes -> new HashMap<>(genes.values()));

    public PlantGenes {
        // Built key-first rather than with EnumMap's copy constructor, which rejects an empty map
        // because it cannot infer the key type from one.
        EnumMap<Gene, Integer> copy = new EnumMap<>(Gene.class);
        copy.putAll(values);
        values = Collections.unmodifiableMap(copy);
    }

    public int get(Gene gene) {
        return this.values.getOrDefault(gene, MIN_VALUE);
    }

    /** Total points earned above the baseline across every gene: how bred the seed is overall. */
    public int totalLevel() {
        int total = 0;
        for (Gene gene : Gene.values()) {
            total += this.get(gene) - MIN_VALUE;
        }
        return total;
    }

    /** True when every gene is still at the baseline, i.e. the plant is genetically ordinary. */
    public boolean isBaseline() {
        for (Gene gene : Gene.values()) {
            if (this.get(gene) > MIN_VALUE) {
                return false;
            }
        }
        return true;
    }

    /** A genome with every gene at the same value, clamped to the configured range. */
    public static PlantGenes uniform(int value) {
        Map<Gene, Integer> all = new EnumMap<>(Gene.class);
        int clamped = Mth.clamp(value, MIN_VALUE, Config.MAX_GENE_VALUE.getAsInt());
        for (Gene gene : Gene.values()) {
            all.put(gene, clamped);
        }
        return new PlantGenes(all);
    }

    /** A copy of this genome with one gene changed. */
    public PlantGenes with(Gene gene, int value) {
        Map<Gene, Integer> copy = new EnumMap<>(this.values);
        copy.put(gene, Mth.clamp(value, MIN_VALUE, Config.MAX_GENE_VALUE.getAsInt()));
        return new PlantGenes(copy);
    }

    /**
     * Crossbreeds two genomes. Each gene is inherited from one parent at random and may then drift
     * a point in either direction.
     */
    public PlantGenes cross(PlantGenes other, RandomSource random) {
        Map<Gene, Integer> child = new EnumMap<>(Gene.class);
        double mutationChance = Config.MUTATION_CHANCE.getAsDouble();
        int max = Config.MAX_GENE_VALUE.getAsInt();

        for (Gene gene : Gene.values()) {
            int value = random.nextBoolean() ? this.get(gene) : other.get(gene);
            if (random.nextDouble() < mutationChance) {
                value += random.nextBoolean() ? 1 : -1;
            }
            child.put(gene, Mth.clamp(value, MIN_VALUE, max));
        }
        return new PlantGenes(child);
    }

    /** Bumps one random gene by a point. This is the reward for harvesting a lucky seed. */
    public PlantGenes improved(RandomSource random) {
        Gene gene = Gene.values()[random.nextInt(Gene.values().length)];
        return this.with(gene, this.get(gene) + 1);
    }

    // --- Trait readings -------------------------------------------------------------------
    // Each of these turns a raw gene value into the number the game logic actually wants, so the
    // balancing lives in one place.

    /** Multiplier on the base chance to advance a growth stage. Baseline keeps the vanilla pace. */
    public float growthMultiplier() {
        return 1.0F + (this.get(Gene.GROWTH) - MIN_VALUE) * 0.35F;
    }

    /** Minimum light level the plant needs. Vanilla crops need 9. */
    public int requiredLight() {
        return Math.max(0, 9 - (this.get(Gene.PHOTOSENSITIVITY) - MIN_VALUE) * 2);
    }

    /** Whether the plant can grow on farmland that has dried out. */
    public boolean toleratesDryFarmland() {
        return this.get(Gene.THIRST) > MIN_VALUE;
    }

    /** Extra produce dropped on top of the guaranteed one. */
    public int rollBonusProduce(RandomSource random) {
        return random.nextInt(this.get(Gene.YIELD));
    }

    /** Extra seeds dropped on top of whatever the loot table already gave. */
    public int rollBonusSeeds(RandomSource random) {
        int fertility = this.get(Gene.FERTILITY) - MIN_VALUE;
        return fertility <= 0 ? 0 : random.nextInt(fertility + 1);
    }

    /** Probability in [0, 0.9] of resisting an incoming disease roll. */
    public double diseaseResistance() {
        return traitChance(Gene.RESISTANCE);
    }

    /** Probability in [0, 0.9] of replanting itself when destroyed. */
    public double regrowthChance() {
        return traitChance(Gene.REGROWTH);
    }

    /** Probability in [0, 0.9] of surviving being walked over. */
    public double tramplingResistance() {
        return traitChance(Gene.TRAMPLING);
    }

    /** Probability in [0, 0.9] that a pest fails to notice this plant. */
    public double camouflageChance() {
        return traitChance(Gene.CAMOUFLAGE);
    }

    private double traitChance(Gene gene) {
        return Math.min(0.9D, (this.get(gene) - MIN_VALUE) * 0.1D);
    }
}

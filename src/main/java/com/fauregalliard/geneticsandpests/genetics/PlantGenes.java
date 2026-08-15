package com.fauregalliard.geneticsandpests.genetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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
        // Keyed on Gene.class rather than copying this.values: that map is an unmodifiable wrapper,
        // never an EnumMap, so EnumMap's copy constructor would reject it whenever it is empty.
        Map<Gene, Integer> copy = new EnumMap<>(Gene.class);
        copy.putAll(this.values);
        copy.put(gene, Mth.clamp(value, MIN_VALUE, Config.MAX_GENE_VALUE.getAsInt()));
        return new PlantGenes(copy);
    }

    /**
     * Crossbreeds two genomes.
     *
     * <p>Each gene takes the better parent's value most of the time and the weaker one otherwise.
     * Nothing here can push a gene down on its own: breeding preserves what was earned, and the
     * only way a value rises is the improvement roll on harvest. A line you have spent an evening
     * building should not decay because of a bad die.
     */
    public PlantGenes cross(PlantGenes other, RandomSource random) {
        // Rolled once for the whole cross rather than once per gene. Per-gene rolls meant the same
        // two parents could produce any of 2^9 genomes, and since seeds only stack when their
        // genomes match exactly, harvesting a field buried the player in single-item stacks.
        boolean takeBetter = random.nextDouble() < Config.INHERITANCE_DOMINANCE.getAsDouble();

        Map<Gene, Integer> child = new EnumMap<>(Gene.class);
        for (Gene gene : Gene.values()) {
            int mine = this.get(gene);
            int theirs = other.get(gene);
            child.put(gene, takeBetter ? Math.max(mine, theirs) : Math.min(mine, theirs));
        }
        return new PlantGenes(child);
    }

    /**
     * Tries to raise one random gene by a point.
     *
     * <p>The odds fall off geometrically with the gene's current value, so the first levels come
     * quickly and the last ones are a project. Since the payoff curve climbs faster than the odds
     * fall, a high gene stays worth chasing.
     */
    public PlantGenes improved(RandomSource random) {
        Gene gene = Gene.values()[random.nextInt(Gene.values().length)];
        int value = this.get(gene);
        double odds = Math.pow(Config.IMPROVEMENT_FALLOFF.getAsDouble(), value - MIN_VALUE);
        return random.nextDouble() < odds ? this.with(gene, value + 1) : this;
    }

    /**
     * Drops one developed gene by a point. Used only where the player had a hand in it — an unripe
     * harvest, a field left dark or dry — never as a random tax on a good one.
     */
    public PlantGenes degraded(RandomSource random) {
        List<Gene> developed = new ArrayList<>();
        for (Gene gene : Gene.values()) {
            if (this.get(gene) > MIN_VALUE) {
                developed.add(gene);
            }
        }
        if (developed.isEmpty()) {
            return this;
        }
        Gene gene = developed.get(random.nextInt(developed.size()));
        return this.with(gene, this.get(gene) - 1);
    }

    // --- Trait readings -------------------------------------------------------------------
    // Each of these turns a raw gene value into the number the game logic actually wants, so the
    // balancing lives in one place and every trait scales with the configured ceiling rather than
    // with hardcoded step sizes.

    /** Where a gene sits between the baseline and the ceiling, from 0 to 1. */
    private double normalized(Gene gene) {
        int max = Config.MAX_GENE_VALUE.getAsInt();
        return max <= MIN_VALUE ? 0.0D : (double) (this.get(gene) - MIN_VALUE) / (max - MIN_VALUE);
    }

    /** Multiplier on the base chance to advance a growth stage. Baseline keeps the vanilla pace. */
    public float growthMultiplier() {
        double t = normalized(Gene.GROWTH);
        return (float) (1.0D + t * t * 8.0D);
    }

    /** Minimum light level the plant needs. Vanilla crops need 9; a maxed gene needs none. */
    public int requiredLight() {
        return Math.max(0, 9 - (int) Math.round(normalized(Gene.PHOTOSENSITIVITY) * 9.0D));
    }

    /** Whether the plant can grow on farmland that has dried out. */
    public boolean toleratesDryFarmland() {
        return this.get(Gene.THIRST) > MIN_VALUE;
    }

    /**
     * Extra produce dropped on top of the guaranteed one. A maxed Yield turns one wheat into
     * roughly 25 to 30.
     */
    public int bonusProduce(RandomSource random) {
        double mean = payoff(Gene.YIELD, 13.0D, 13.5D);
        double spread = mean * 0.08D;
        return roundStochastically(mean + (random.nextDouble() * 2.0D - 1.0D) * spread, random);
    }

    /** Extra seeds dropped on top of whatever the loot table already gave. */
    public int rollBonusSeeds(RandomSource random) {
        return roundStochastically(payoff(Gene.FERTILITY, 3.0D, 3.0D), random);
    }

    /**
     * The reward curve every scaling trait shares: half of it grows evenly with the gene and half
     * accelerates.
     *
     * <p>A purely quadratic curve looked right on paper and played badly — the first third of the
     * levels were indistinguishable from no levels at all. The linear half makes every single level
     * show up in the harvest, and the quadratic half keeps the expensive final levels worth their
     * price. {@code linear + accelerating} is the total at a maxed gene.
     */
    private double payoff(Gene gene, double linear, double accelerating) {
        double t = normalized(gene);
        return t * linear + t * t * accelerating;
    }

    /**
     * Rounds up or down in proportion to the fraction, so a mean below one still pays out sometimes
     * instead of silently truncating to nothing. Without this the first few levels of a quadratic
     * trait would do literally nothing.
     */
    private static int roundStochastically(double value, RandomSource random) {
        if (value <= 0.0D) {
            return 0;
        }
        int whole = (int) Math.floor(value);
        return random.nextDouble() < value - whole ? whole + 1 : whole;
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
        return normalized(gene) * 0.9D;
    }
}

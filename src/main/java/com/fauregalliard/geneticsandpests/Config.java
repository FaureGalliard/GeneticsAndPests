package com.fauregalliard.geneticsandpests;

import net.neoforged.neoforge.common.ModConfigSpec;

// Common config for the mod. Values here are read on both the client and the dedicated server.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_GENE_VALUE = BUILDER
            .comment("Upper bound every gene is clamped to")
            .defineInRange("maxGeneValue", 10, 1, 100);

    public static final ModConfigSpec.DoubleValue MUTATION_CHANCE = BUILDER
            .comment("Probability that a gene drifts a point instead of being inherited cleanly")
            .defineInRange("mutationChance", 0.1D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue IMPROVEMENT_CHANCE = BUILDER
            .comment("Probability that a harvested seed comes out with one gene improved.",
                    "At the default of 1/3, roughly one seed in three is an upgrade.")
            .defineInRange("improvementChance", 0.34D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue DISEASE_SPREAD_CHANCE = BUILDER
            .comment("Base probability, per disease pass, that a sick plant infects an adjacent one")
            .defineInRange("diseaseSpreadChance", 0.05D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue GROWTH_BONUS_CHANCE = BUILDER
            .comment("Chance per second of an extra growth stage, per point of Growth above the baseline.",
                    "Vanilla growth is untouched; this is only what genes add on top.")
            .defineInRange("growthBonusChance", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue LOW_LIGHT_GROWTH_CHANCE = BUILDER
            .comment("Chance per second of growing in light too dim for vanilla, once Photosensitivity allows it")
            .defineInRange("lowLightGrowthChance", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue DROUGHT_GROWTH_CHANCE = BUILDER
            .comment("Chance per second of shrugging off dry farmland, granted by Thirst")
            .defineInRange("droughtGrowthChance", 0.02D, 0.0D, 1.0D);

    static final ModConfigSpec SPEC = BUILDER.build();
}

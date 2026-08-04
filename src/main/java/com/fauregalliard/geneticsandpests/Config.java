package com.fauregalliard.geneticsandpests;

import net.neoforged.neoforge.common.ModConfigSpec;

// Common config for the mod. Values here are read on both the client and the dedicated server.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_GENE_VALUE = BUILDER
            .comment("Upper bound every gene is clamped to. Every trait scales against this,",
                    "so lowering it makes a maxed plant weaker rather than breaking the curve.")
            .defineInRange("maxGeneValue", 20, 1, 100);

    public static final ModConfigSpec.DoubleValue INHERITANCE_DOMINANCE = BUILDER
            .comment("Probability that a crossed gene takes the better parent's value instead of the weaker one.",
                    "Breeding never lowers a gene on its own; this only decides how fast the better line wins.")
            .defineInRange("inheritanceDominance", 0.75D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue IMPROVEMENT_CHANCE = BUILDER
            .comment("Probability that a harvested seed attempts to improve one of its genes.",
                    "The gene is picked at random, so all nine advance in parallel.")
            .defineInRange("improvementChance", 0.5D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue IMPROVEMENT_FALLOFF = BUILDER
            .comment("How much harder each level is than the one before it.",
                    "The odds of raising a gene are this value to the power of its current level,",
                    "so at the default of 0.90 the first levels are quick and the last ones are a project.",
                    "Set to 1.0 to make every level equally likely.")
            .defineInRange("improvementFalloff", 0.90D, 0.01D, 1.0D);

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

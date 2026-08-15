package com.fauregalliard.geneticsandpests;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

// Common config for the mod. Values here are read on both the client and the dedicated server.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_GENE_VALUE = BUILDER
            .comment("Upper bound every gene is clamped to. Every trait scales against this,",
                    "so lowering it makes a maxed plant weaker rather than breaking the curve.")
            .defineInRange("maxGeneValue", 20, 1, 100);

    public static final ModConfigSpec.DoubleValue INHERITANCE_DOMINANCE = BUILDER
            .comment("Probability that a cross takes after its better parent rather than its weaker one.",
                    "Rolled once per cross, not per gene. At the default of 1.0 a harvest is predictable",
                    "and its seeds all stack together; lowering it brings back variety, at the cost of",
                    "filling your inventory with near-identical single seeds.")
            .defineInRange("inheritanceDominance", 1.0D, 0.0D, 1.0D);

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

    public static final ModConfigSpec.DoubleValue RAIN_GROWTH_CHANCE = BUILDER
            .comment("Chance per second of an extra growth stage while rain falls on the plant.",
                    "Only applies where the sky can reach it, so greenhouses trade weather for control.")
            .defineInRange("rainGrowthChance", 0.03D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue STORM_SPREAD_MULTIPLIER = BUILDER
            .comment("How much faster disease spreads during a thunderstorm.")
            .defineInRange("stormSpreadMultiplier", 3.0D, 1.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue DISEASE_SPREAD_CHANCE = BUILDER
            .comment("Base probability, per disease pass, that a sick plant infects an adjacent one.",
                    "Each disease scales this by its own contagion.")
            .defineInRange("diseaseSpreadChance", 0.05D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue OUTBREAK_CHANCE = BUILDER
            .comment("Probability, per disease pass, that a healthy plant falls ill on its own.",
                    "Multiplied by how far past the monoculture threshold its surroundings are,",
                    "so isolated plants are effectively safe and packed fields are not.")
            .defineInRange("outbreakChance", 0.0004D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue MONOCULTURE_THRESHOLD = BUILDER
            .comment("How many of the eight surrounding blocks may be the same plant before",
                    "outbreaks begin. At the default of 2 a single row stays safe and every plant",
                    "in a 3x3 patch is already at risk, the corners least and the middle most.")
            .defineInRange("monocultureThreshold", 2, 0, 8);

    public static final ModConfigSpec.DoubleValue RECOVERY_CHANCE = BUILDER
            .comment("Base probability, per disease pass, that a sick plant shakes off a curable disease.",
                    "Resistance multiplies this, and recovery is what triggers the plague mutation.")
            .defineInRange("recoveryChance", 0.01D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue BLIGHT_LETHAL_PASSES = BUILDER
            .comment("How many passes (roughly seconds) a plant survives Blight before it dies.",
                    "Blight has no cure, so this is how long you have to tear out the infection.")
            .defineInRange("blightLethalPasses", 90, 1, 100000);

    public static final ModConfigSpec.DoubleValue POOR_HARVEST_LOSS_CHANCE = BUILDER
            .comment("Probability that a seed loses a gene point when the plant was harvested badly:",
                    "picked unripe, grown too dark for its Photosensitivity, or left on dry soil",
                    "without the Thirst to handle it. A well-kept plant never loses anything.")
            .defineInRange("poorHarvestLossChance", 0.25D, 0.0D, 1.0D);

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

    public static final ModConfigSpec.DoubleValue WILD_STRAIN_CHANCE = BUILDER
            .comment("Probability that a naturally generated crop — in practice, a village field —",
                    "carries a genome of its own. Set to 0 to keep the world's crops ordinary.")
            .defineInRange("wildStrainChance", 0.25D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue WILD_STRAIN_CEILING = BUILDER
            .comment("Highest gene level a wild strain can have. Deliberately well below the ceiling:",
                    "a village should be a head start, not a shortcut past breeding.")
            .defineInRange("wildStrainCeiling", 6, 2, 100);

    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Every tunable, by the same name it has in the config file, so the in-game command can list
     * and edit them without a second hand-written copy of this list drifting out of date.
     */
    public static final Map<String, ModConfigSpec.ConfigValue<?>> ENTRIES = entries();

    private static Map<String, ModConfigSpec.ConfigValue<?>> entries() {
        Map<String, ModConfigSpec.ConfigValue<?>> map = new LinkedHashMap<>();
        map.put("maxGeneValue", MAX_GENE_VALUE);
        map.put("inheritanceDominance", INHERITANCE_DOMINANCE);
        map.put("improvementChance", IMPROVEMENT_CHANCE);
        map.put("improvementFalloff", IMPROVEMENT_FALLOFF);
        map.put("poorHarvestLossChance", POOR_HARVEST_LOSS_CHANCE);
        map.put("growthBonusChance", GROWTH_BONUS_CHANCE);
        map.put("lowLightGrowthChance", LOW_LIGHT_GROWTH_CHANCE);
        map.put("droughtGrowthChance", DROUGHT_GROWTH_CHANCE);
        map.put("rainGrowthChance", RAIN_GROWTH_CHANCE);
        map.put("stormSpreadMultiplier", STORM_SPREAD_MULTIPLIER);
        map.put("diseaseSpreadChance", DISEASE_SPREAD_CHANCE);
        map.put("outbreakChance", OUTBREAK_CHANCE);
        map.put("monocultureThreshold", MONOCULTURE_THRESHOLD);
        map.put("recoveryChance", RECOVERY_CHANCE);
        map.put("blightLethalPasses", BLIGHT_LETHAL_PASSES);
        map.put("wildStrainChance", WILD_STRAIN_CHANCE);
        map.put("wildStrainCeiling", WILD_STRAIN_CEILING);
        return Collections.unmodifiableMap(map);
    }
}

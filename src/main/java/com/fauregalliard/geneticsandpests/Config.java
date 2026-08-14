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
                    "outbreaks begin. Lower values punish dense fields harder.")
            .defineInRange("monocultureThreshold", 5, 0, 8);

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
        map.put("diseaseSpreadChance", DISEASE_SPREAD_CHANCE);
        map.put("outbreakChance", OUTBREAK_CHANCE);
        map.put("monocultureThreshold", MONOCULTURE_THRESHOLD);
        map.put("recoveryChance", RECOVERY_CHANCE);
        map.put("blightLethalPasses", BLIGHT_LETHAL_PASSES);
        return Collections.unmodifiableMap(map);
    }
}

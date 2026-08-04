package com.fauregalliard.geneticsandpests;

import net.neoforged.neoforge.common.ModConfigSpec;

// Common config for the mod. Values here are read on both the client and the dedicated server.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue DISEASE_SPREAD_CHANCE = BUILDER
            .comment("Base probability, per server tick, that a diseased plant infects an adjacent plant")
            .defineInRange("diseaseSpreadChance", 0.05D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue MAX_GENE_VALUE = BUILDER
            .comment("Upper bound for the Growth / Yield / Resistance gene values")
            .defineInRange("maxGeneValue", 10, 1, 100);

    public static final ModConfigSpec.DoubleValue MUTATION_CHANCE = BUILDER
            .comment("Probability that a gene mutates instead of being inherited when crossbreeding")
            .defineInRange("mutationChance", 0.1D, 0.0D, 1.0D);

    static final ModConfigSpec SPEC = BUILDER.build();
}

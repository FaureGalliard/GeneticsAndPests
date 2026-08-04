package com.fauregalliard.geneticsandpests.genetics;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The traits a plant can carry. Every gene is an integer starting at {@link PlantGenes#MIN_VALUE};
 * adding a new trait to the mod is a matter of adding a constant here and reading it wherever it
 * should take effect.
 */
public enum Gene implements StringRepresentable {
    /** How quickly the plant advances through its growth stages. */
    GROWTH("growth"),
    /** How much produce a harvest yields. */
    YIELD("yield"),
    /** How well the plant shrugs off disease. */
    RESISTANCE("resistance"),
    /** How little light the plant needs; high values let it grow in caves. */
    PHOTOSENSITIVITY("photosensitivity"),
    /** Drought tolerance; high values let it grow on dry farmland. */
    THIRST("thirst"),
    /** Chance to replant itself instead of dying when harvested or destroyed. */
    REGROWTH("regrowth"),
    /** Chance to survive being walked over. */
    TRAMPLING("trampling"),
    /** How poorly pests can spot the plant when scanning for targets. */
    CAMOUFLAGE("camouflage"),
    /** How many seeds a harvest produces, and how eagerly the plant crossbreeds. */
    FERTILITY("fertility");

    public static final Codec<Gene> CODEC = StringRepresentable.fromEnum(Gene::values);

    public static final StreamCodec<ByteBuf, Gene> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(index -> values()[index], Gene::ordinal);

    private final String id;

    Gene(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public String translationKey() {
        return "gene.geneticsandpests." + this.id;
    }
}

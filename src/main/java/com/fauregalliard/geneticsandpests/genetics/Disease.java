package com.fauregalliard.geneticsandpests.genetics;

import com.mojang.serialization.Codec;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.util.StringRepresentable;

/**
 * The plant diseases, each with its own way of spreading, its own damage and its own remedy.
 *
 * <p>The colour is what the ground stain under an infected plant is tinted with, so a new disease
 * costs a constant here rather than a texture — the overlay art is a single greyscale gradient
 * shared by all of them.
 */
public enum Disease implements StringRepresentable {
    /**
     * The everyday one: contagious, visible, survivable. Halves growth and harvest.
     */
    RUST("rust", ParticleTypes.CRIMSON_SPORE, 0xC1_6A_2A, Spread.NEIGHBOURS, 1.0D),

    /**
     * The treacherous one. It costs the plant nothing and shows nothing in the harvest — the grain
     * simply comes out poisonous. Historically this is St. Anthony's Fire.
     */
    ERGOT("ergot", ParticleTypes.MYCELIUM, 0x5B_3A_78, Spread.NEIGHBOURS, 0.4D),

    /**
     * Travels in the seed rather than through the soil, so keeping fields apart does not help:
     * the infection rides home in your own inventory and sprouts wherever you sow it.
     */
    SMUT("smut", ParticleTypes.ASH, 0x2B_2B_2B, Spread.SEEDS, 0.0D),

    /**
     * The urgent one. Growth stops dead and the plant dies if it is left alone. There is no cure;
     * the only answer is to tear out the infected plants and the ring around them.
     */
    BLIGHT("blight", ParticleTypes.WARPED_SPORE, 0x6E_7F_5E, Spread.NEIGHBOURS, 0.7D);

    /** How an infection reaches its next host. */
    public enum Spread {
        /** Creeps outward through touching plants. */
        NEIGHBOURS,
        /** Carried by harvested seeds, ignoring distance entirely. */
        SEEDS
    }

    public static final Codec<Disease> CODEC = StringRepresentable.fromEnum(Disease::values);

    public static final StreamCodec<ByteBuf, Disease> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(index -> values()[index], Disease::ordinal);

    private final String id;
    private final ParticleOptions particle;
    private final int colour;
    private final Spread spread;
    private final double contagion;

    Disease(String id, ParticleOptions particle, int colour, Spread spread, double contagion) {
        this.id = id;
        this.particle = particle;
        this.colour = colour;
        this.spread = spread;
        this.contagion = contagion;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public ParticleOptions particle() {
        return this.particle;
    }

    /** Tint for the ground stain, as packed RGB. */
    public int colour() {
        return this.colour;
    }

    public Spread spread() {
        return this.spread;
    }

    /** Multiplier on the configured spread chance, so each disease creeps at its own pace. */
    public double contagion() {
        return this.contagion;
    }

    /** Whether growth is stopped outright rather than merely slowed. */
    public boolean stopsGrowth() {
        return this == BLIGHT;
    }

    /** Whether the plant eventually dies if the infection is never dealt with. */
    public boolean isLethal() {
        return this == BLIGHT;
    }

    /** Whether a harvest yields no produce at all. */
    public boolean destroysProduce() {
        return this == SMUT;
    }

    /** Whether the produce comes out edible but poisonous. */
    public boolean taintsProduce() {
        return this == ERGOT;
    }

    /**
     * Items that cure this disease, as a tag so the remedy can be swapped in a datapack without
     * touching code. An empty tag — Blight's — means there is no remedy at all.
     */
    public TagKey<Item> cureTag() {
        return TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "cures/" + this.id));
    }

    /** The stain drawn on whatever the plant is rooted in. One texture per disease, not per plant. */
    public Identifier stainTexture() {
        return Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "textures/effect/" + this.id + ".png");
    }

    public String translationKey() {
        return "disease.geneticsandpests." + this.id;
    }
}

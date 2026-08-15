package com.fauregalliard.geneticsandpests.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A cutting taken from a plant: one trait, at the level it had reached, and the crop it came from.
 *
 * <p>This is what makes breeding steerable. Harvest improvement picks a gene at random out of nine,
 * so pushing one specific trait to the ceiling by luck alone takes on the order of a thousand seeds.
 * A scion moves a trait that has already been earned onto another line — it buys direction, never
 * power, and the seed it was cut from is destroyed in the taking.
 *
 * <p>The source crop is part of the cutting because grafting is a graft: a potato cutting belongs on
 * a potato. Letting it take on wheat would turn a horticultural tool into a magic wand.
 */
public record ScionData(Item source, Gene gene, int level) {
    public static final Codec<ScionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("source").forGetter(ScionData::source),
            Gene.CODEC.fieldOf("gene").forGetter(ScionData::gene),
            Codec.INT.fieldOf("level").forGetter(ScionData::level)
    ).apply(instance, ScionData::new));

    // Registry-backed, so the buffer has to be the registry-aware one rather than a plain ByteBuf.
    public static final StreamCodec<RegistryFriendlyByteBuf, ScionData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(BuiltInRegistries.ITEM.key()), ScionData::source,
            Gene.STREAM_CODEC, ScionData::gene,
            ByteBufCodecs.VAR_INT, ScionData::level,
            ScionData::new);

    /** Whether this cutting will take on the given seed. */
    public boolean matches(ItemStack seed) {
        return seed.is(this.source);
    }
}

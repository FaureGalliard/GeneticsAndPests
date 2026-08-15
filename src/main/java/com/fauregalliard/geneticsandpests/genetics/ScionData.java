package com.fauregalliard.geneticsandpests.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A cutting taken from a plant: one trait, at the level it had reached.
 *
 * <p>This is what makes breeding steerable. Harvest improvement picks a gene at random out of nine,
 * so pushing one specific trait to the ceiling by luck alone takes on the order of a thousand seeds.
 * A scion moves a trait that has already been earned onto another line — it buys direction, never
 * power, and the seed it was cut from is destroyed in the taking.
 */
public record ScionData(Gene gene, int level) {
    public static final Codec<ScionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Gene.CODEC.fieldOf("gene").forGetter(ScionData::gene),
            Codec.INT.fieldOf("level").forGetter(ScionData::level)
    ).apply(instance, ScionData::new));

    public static final StreamCodec<ByteBuf, ScionData> STREAM_CODEC = StreamCodec.composite(
            Gene.STREAM_CODEC, ScionData::gene,
            ByteBufCodecs.VAR_INT, ScionData::level,
            ScionData::new);
}

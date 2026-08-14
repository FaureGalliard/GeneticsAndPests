package com.fauregalliard.geneticsandpests.genetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the mod tracks about the plants in one chunk, attached to the chunk itself.
 *
 * <p>Keeping this on the chunk rather than in a block entity is what makes the mod work with crops
 * it does not own: any block from any mod can carry a genome and an infection without that mod
 * knowing, and the data loads, saves and unloads along with the terrain it belongs to.
 */
public class CropGenes {
    public static final MapCodec<CropGenes> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("crops").forGetter(CropGenes::toEntries)
    ).apply(instance, CropGenes::fromEntries));

    /** Sent to clients so the debug screen and HUD mods can name what is wrong with a plant. */
    public static final StreamCodec<ByteBuf, CropGenes> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.plants.size());
                value.plants.forEach((pos, state) -> {
                    buffer.writeLong(pos.asLong());
                    PlantState.STREAM_CODEC.encode(buffer, state);
                });
            },
            buffer -> {
                int count = ByteBufCodecs.VAR_INT.decode(buffer);
                CropGenes result = new CropGenes();
                for (int i = 0; i < count; i++) {
                    result.put(BlockPos.of(buffer.readLong()), PlantState.STREAM_CODEC.decode(buffer));
                }
                return result;
            });

    private final Map<BlockPos, PlantState> plants = new HashMap<>();

    public CropGenes() {}

    @Nullable
    public PlantState get(BlockPos pos) {
        return this.plants.get(pos.immutable());
    }

    public void put(BlockPos pos, PlantState state) {
        this.plants.put(pos.immutable(), state);
    }

    public void remove(BlockPos pos) {
        this.plants.remove(pos.immutable());
    }

    public boolean isEmpty() {
        return this.plants.isEmpty();
    }

    /** A snapshot of the tracked positions, safe to iterate while the map is being edited. */
    public Set<BlockPos> positions() {
        return Set.copyOf(this.plants.keySet());
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(this.plants.size());
        this.plants.forEach((pos, state) -> entries.add(new Entry(pos, state)));
        return entries;
    }

    private static CropGenes fromEntries(List<Entry> entries) {
        CropGenes result = new CropGenes();
        entries.forEach(entry -> result.put(entry.pos(), entry.state()));
        return result;
    }

    private record Entry(BlockPos pos, PlantState state) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                PlantState.MAP_CODEC.forGetter(Entry::state)
        ).apply(instance, Entry::new));
    }
}

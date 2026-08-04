package com.fauregalliard.geneticsandpests.genetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The genomes of every tracked plant in one chunk, attached to the chunk itself.
 *
 * <p>Keeping this on the chunk rather than in a block entity is what makes the mod work with crops
 * it does not own: any block from any mod can carry a genome without that mod knowing, and the data
 * loads, saves and unloads along with the terrain it belongs to.
 */
public class CropGenes {
    public static final MapCodec<CropGenes> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("crops").forGetter(CropGenes::toEntries)
    ).apply(instance, CropGenes::fromEntries));

    private final Map<BlockPos, PlantGenes> genes = new HashMap<>();

    public CropGenes() {}

    @Nullable
    public PlantGenes get(BlockPos pos) {
        return this.genes.get(pos.immutable());
    }

    public void put(BlockPos pos, PlantGenes plantGenes) {
        this.genes.put(pos.immutable(), plantGenes);
    }

    public void remove(BlockPos pos) {
        this.genes.remove(pos.immutable());
    }

    public boolean isEmpty() {
        return this.genes.isEmpty();
    }

    /** A snapshot of the tracked positions, safe to iterate while the map is being edited. */
    public Set<BlockPos> positions() {
        return Set.copyOf(this.genes.keySet());
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(this.genes.size());
        this.genes.forEach((pos, plantGenes) -> entries.add(new Entry(pos, plantGenes)));
        return entries;
    }

    private static CropGenes fromEntries(List<Entry> entries) {
        CropGenes result = new CropGenes();
        entries.forEach(entry -> result.put(entry.pos(), entry.genes()));
        return result;
    }

    private record Entry(BlockPos pos, PlantGenes genes) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                PlantGenes.CODEC.fieldOf("genes").forGetter(Entry::genes)
        ).apply(instance, Entry::new));
    }
}

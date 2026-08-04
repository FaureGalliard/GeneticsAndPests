package com.fauregalliard.geneticsandpests.genetics;

import com.fauregalliard.geneticsandpests.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/** Reads and writes the genome of a planted crop, hiding the chunk attachment behind it. */
public final class GeneStorage {
    /** The genome at a position, or null when nothing genetic is planted there. */
    @Nullable
    public static PlantGenes get(Level level, BlockPos pos) {
        CropGenes stored = level.getChunkAt(pos).getExistingDataOrNull(ModAttachments.CROP_GENES);
        return stored == null ? null : stored.get(pos);
    }

    /** The genome at a position, falling back to the baseline genome. */
    public static PlantGenes getOrDefault(Level level, BlockPos pos) {
        PlantGenes genes = get(level, pos);
        return genes == null ? PlantGenes.DEFAULT : genes;
    }

    public static void set(Level level, BlockPos pos, PlantGenes genes) {
        LevelChunk chunk = level.getChunkAt(pos);
        chunk.getData(ModAttachments.CROP_GENES).put(pos, genes);
        chunk.markUnsaved();
        CropTracker.track(level, chunk.getPos());
    }

    public static void clear(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        CropGenes stored = chunk.getExistingDataOrNull(ModAttachments.CROP_GENES);
        if (stored != null) {
            stored.remove(pos);
            chunk.markUnsaved();
        }
    }

    private GeneStorage() {}
}

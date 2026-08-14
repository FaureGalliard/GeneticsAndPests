package com.fauregalliard.geneticsandpests.genetics;

import com.fauregalliard.geneticsandpests.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/** Reads and writes what the mod knows about a planted crop, hiding the chunk attachment. */
public final class GeneStorage {
    /** Everything tracked at a position, or null when nothing is planted there as far as we know. */
    @Nullable
    public static PlantState getState(Level level, BlockPos pos) {
        CropGenes stored = level.getChunkAt(pos).getExistingDataOrNull(ModAttachments.CROP_GENES);
        return stored == null ? null : stored.get(pos);
    }

    @Nullable
    public static PlantGenes get(Level level, BlockPos pos) {
        PlantState state = getState(level, pos);
        return state == null ? null : state.genes();
    }

    public static PlantGenes getOrDefault(Level level, BlockPos pos) {
        PlantGenes genes = get(level, pos);
        return genes == null ? PlantGenes.DEFAULT : genes;
    }

    @Nullable
    public static Disease getDisease(Level level, BlockPos pos) {
        PlantState state = getState(level, pos);
        return state == null ? null : state.diseaseOrNull();
    }

    public static void setState(Level level, BlockPos pos, PlantState state) {
        LevelChunk chunk = level.getChunkAt(pos);
        chunk.getData(ModAttachments.CROP_GENES).put(pos, state);
        chunk.markUnsaved();
        CropTracker.track(level, chunk.getPos());
    }

    public static void set(Level level, BlockPos pos, PlantGenes genes) {
        PlantState existing = getState(level, pos);
        setState(level, pos, existing == null ? PlantState.healthy(genes) : existing.withGenes(genes));
    }

    /**
     * Marks a plant as sick, starting to track it if it was an ordinary untracked one. Disease is a
     * hazard of the world rather than a punishment for breeding, so it has to be able to take hold
     * on a plain vanilla farm too.
     */
    public static void infect(Level level, BlockPos pos, Disease disease) {
        PlantState existing = getState(level, pos);
        PlantState base = existing == null ? PlantState.healthy(PlantGenes.DEFAULT) : existing;
        setState(level, pos, base.infectedWith(disease));
    }

    public static void cure(Level level, BlockPos pos) {
        PlantState existing = getState(level, pos);
        if (existing != null && existing.isDiseased()) {
            setState(level, pos, existing.cured());
        }
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

package com.fauregalliard.geneticsandpests.genetics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fauregalliard.geneticsandpests.Config;
import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.registry.ModAttachments;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Drives the growth genes.
 *
 * <p>Vanilla growth is left completely alone: a plant whose genome is at the baseline grows exactly
 * as it always did. What this class adds is <em>extra</em> chances to advance a stage, granted only
 * by genes — so the mod never has to take over another mod's growth logic to speed a plant up, and
 * a bug here can only ever make crops grow slower than intended, never break them.
 *
 * <p>Only chunks known to hold tracked plants are visited, and that index is rebuilt from chunk
 * loads rather than stored, so it can never drift out of sync with the saved data.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class CropTracker {
    /** How often the extra growth pass runs, in ticks. */
    private static final int INTERVAL = 20;

    private static final Map<ResourceKey<Level>, Set<ChunkPos>> ACTIVE_CHUNKS = new ConcurrentHashMap<>();

    /** Registers a chunk as holding tracked plants, so the growth pass will visit it. */
    public static void track(Level level, ChunkPos pos) {
        ACTIVE_CHUNKS.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelChunk chunk = event.getChunk();
        CropGenes stored = chunk.getExistingDataOrNull(ModAttachments.CROP_GENES);
        if (stored != null && !stored.isEmpty() && event.getLevel() instanceof Level level) {
            track(level, chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            Set<ChunkPos> chunks = ACTIVE_CHUNKS.get(level.dimension());
            if (chunks != null) {
                chunks.remove(event.getChunk().getPos());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0) {
            return;
        }

        Set<ChunkPos> chunks = ACTIVE_CHUNKS.get(level.dimension());
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        for (ChunkPos chunkPos : Set.copyOf(chunks)) {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                chunks.remove(chunkPos);
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            CropGenes stored = chunk.getExistingDataOrNull(ModAttachments.CROP_GENES);
            if (stored == null || stored.isEmpty()) {
                chunks.remove(chunkPos);
                continue;
            }

            for (BlockPos pos : stored.positions()) {
                PlantGenes genes = stored.get(pos);
                if (genes == null) {
                    continue;
                }
                if (!isTrackedCrop(level, pos)) {
                    // The plant is gone; forget it rather than leaking an entry forever.
                    stored.remove(pos);
                    chunk.markUnsaved();
                    continue;
                }
                tickPlant(level, pos, genes);
            }
        }
    }

    private static boolean isTrackedCrop(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.GENETIC_CROPS);
    }

    private static void tickPlant(ServerLevel level, BlockPos pos, PlantGenes genes) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) {
            return;
        }

        RandomSource random = level.random;
        int light = level.getRawBrightness(pos, 0);
        double chance = 0.0D;

        // Growth: extra attempts on top of whatever vanilla is already doing.
        if (light >= 9) {
            chance += (genes.growthMultiplier() - 1.0F) * Config.GROWTH_BONUS_CHANCE.getAsDouble();
        }

        // Photosensitivity: the only way to advance at all when it is too dark for vanilla.
        if (light < 9 && light >= genes.requiredLight()) {
            chance += Config.LOW_LIGHT_GROWTH_CHANCE.getAsDouble();
        }

        // Thirst: makes up for the slowdown vanilla applies on dry farmland.
        if (genes.toleratesDryFarmland() && isDryFarmland(level, pos)) {
            chance += Config.DROUGHT_GROWTH_CHANCE.getAsDouble();
        }

        if (chance > 0.0D && random.nextDouble() < chance) {
            level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), 2);
        }
    }

    private static boolean isDryFarmland(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof FarmBlock && below.getValue(FarmBlock.MOISTURE) == 0;
    }

    private CropTracker() {}
}

package com.fauregalliard.geneticsandpests.genetics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fauregalliard.geneticsandpests.Config;
import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.registry.ModAttachments;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The once-a-second pass over every tracked plant: gene-driven growth, and the disease automaton.
 *
 * <p>Vanilla growth is left alone for healthy plants — a baseline genome grows exactly as it always
 * did, and what genes add is <em>extra</em> chances on top. Disease is the one thing that reaches
 * back into vanilla growth, because a sick plant has to actually stall.
 *
 * <p>Only chunks known to hold tracked plants are visited, and that index is rebuilt from chunk
 * loads rather than saved, so it can never drift out of sync with the stored data.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class CropTracker {
    /** How often the pass runs, in ticks. */
    private static final int INTERVAL = 20;

    /** The eight neighbours a monoculture is measured across. */
    private static final int MONOCULTURE_SAMPLE = 8;

    private static final Map<ResourceKey<Level>, Set<ChunkPos>> ACTIVE_CHUNKS = new ConcurrentHashMap<>();

    /** Registers a chunk as holding tracked plants, so the pass will visit it. */
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

    /** Stalls vanilla growth on a sick plant: outright for Blight, half the time for anything else. */
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        Disease disease = GeneStorage.getDisease(level, event.getPos());
        if (disease == null) {
            return;
        }

        if (disease.stopsGrowth() || level.random.nextBoolean()) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
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
                PlantState plant = stored.get(pos);
                if (plant == null) {
                    continue;
                }
                if (!level.getBlockState(pos).is(ModTags.GENETIC_CROPS)) {
                    // The plant is gone; forget it rather than leaking an entry forever.
                    stored.remove(pos);
                    chunk.markUnsaved();
                    continue;
                }
                tickPlant(level, chunk, stored, pos, plant);
            }
        }
    }

    private static void tickPlant(ServerLevel level, LevelChunk chunk, CropGenes stored,
                                  BlockPos pos, PlantState plant) {
        if (plant.isDiseased()) {
            tickDisease(level, chunk, stored, pos, plant);
        } else {
            tickGrowth(level, pos, plant.genes());
            rollOutbreak(level, chunk, stored, pos, plant);
        }
    }

    // --- Growth ---------------------------------------------------------------------------

    private static void tickGrowth(ServerLevel level, BlockPos pos, PlantGenes genes) {
        BlockState state = level.getBlockState(pos);
        if (!PlantGrowth.canGrow(state)) {
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
        if (genes.toleratesDryFarmland() && isDryFarmland(level, pos, state)) {
            chance += Config.DROUGHT_GROWTH_CHANCE.getAsDouble();
        }

        if (chance > 0.0D && random.nextDouble() < chance) {
            level.setBlock(pos, PlantGrowth.grown(state), 2);
        }
    }

    private static boolean isDryFarmland(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState support = level.getBlockState(PlantGrowth.supportPos(level, pos, state));
        return support.getBlock() instanceof FarmBlock && support.getValue(FarmBlock.MOISTURE) == 0;
    }

    // --- Disease --------------------------------------------------------------------------

    /**
     * Rolls for a fresh outbreak. The odds climb with how much of the same plant is packed around
     * this one, so a scattered garden is nearly safe and a hundred-block wheat field is a matter of
     * time. Crop rotation and spacing become real decisions rather than decoration.
     */
    private static void rollOutbreak(ServerLevel level, LevelChunk chunk, CropGenes stored,
                                     BlockPos pos, PlantState plant) {
        int threshold = Config.MONOCULTURE_THRESHOLD.getAsInt();
        int crowding = countIdenticalNeighbours(level, pos) - threshold;
        if (crowding <= 0) {
            return;
        }

        double chance = Config.OUTBREAK_CHANCE.getAsDouble() * crowding
                * (1.0D - plant.genes().diseaseResistance());
        if (level.random.nextDouble() < chance) {
            stored.put(pos, plant.infectedWith(rollOutbreakDisease(level.random)));
            chunk.markUnsaved();
        }
    }

    private static int countIdenticalNeighbours(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && level.getBlockState(pos.offset(dx, 0, dz)).is(state.getBlock())) {
                    count++;
                }
            }
        }
        return Math.min(count, MONOCULTURE_SAMPLE);
    }

    /** Rust is the everyday blight, Ergot the quiet one, Blight the emergency. Smut only rides seeds. */
    private static Disease rollOutbreakDisease(RandomSource random) {
        double roll = random.nextDouble();
        if (roll < 0.60D) {
            return Disease.RUST;
        }
        return roll < 0.85D ? Disease.ERGOT : Disease.BLIGHT;
    }

    private static void tickDisease(ServerLevel level, LevelChunk chunk, CropGenes stored,
                                    BlockPos pos, PlantState plant) {
        Disease disease = plant.diseaseOrNull();
        if (disease == null) {
            return;
        }

        showSymptoms(level, pos, disease);

        RandomSource random = level.random;
        PlantState aged = plant.aged();

        // Blight kills what is left alone. Everything else can be waited out.
        if (disease.isLethal() && aged.infectedFor() >= Config.BLIGHT_LETHAL_PASSES.getAsInt()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            stored.remove(pos);
            chunk.markUnsaved();
            return;
        }

        // Surviving an infection is what breeds resistance, so recovery is driven by Resistance.
        double recovery = Config.RECOVERY_CHANCE.getAsDouble() * (1.0D + plant.genes().diseaseResistance() * 4.0D);
        if (!disease.isLethal() && random.nextDouble() < recovery) {
            stored.put(pos, PlantState.healthy(adaptTo(plant.genes(), random)));
            chunk.markUnsaved();
            return;
        }

        stored.put(pos, aged);
        chunk.markUnsaved();

        if (disease.spread() == Disease.Spread.NEIGHBOURS) {
            spreadToNeighbours(level, pos, disease, random);
        }
    }

    /**
     * The plague mutation: a plant that lives through an infection comes out hardier and worse at
     * something else. This is the only way Resistance can be bred deliberately — you have to let
     * the disease through your field to get it.
     *
     * <p>The guards matter more than the effect. A gene is only ever taken from a trait that is
     * actually developed, never from Resistance itself, and never from the plant's best trait, so a
     * run of bad luck cannot undo the thing you spent the evening breeding.
     */
    private static PlantGenes adaptTo(PlantGenes genes, RandomSource random) {
        PlantGenes hardened = genes.with(Gene.RESISTANCE, genes.get(Gene.RESISTANCE) + 1);

        Gene best = Gene.RESISTANCE;
        for (Gene gene : Gene.values()) {
            if (hardened.get(gene) > hardened.get(best)) {
                best = gene;
            }
        }

        java.util.List<Gene> payable = new java.util.ArrayList<>();
        for (Gene gene : Gene.values()) {
            if (gene != Gene.RESISTANCE && gene != best && hardened.get(gene) > PlantGenes.MIN_VALUE + 1) {
                payable.add(gene);
            }
        }
        if (payable.isEmpty()) {
            return hardened;
        }

        Gene cost = payable.get(random.nextInt(payable.size()));
        return hardened.with(cost, hardened.get(cost) - 1);
    }

    private static void spreadToNeighbours(ServerLevel level, BlockPos pos, Disease disease, RandomSource random) {
        BlockState state = level.getBlockState(pos);
        double base = Config.DISEASE_SPREAD_CHANCE.getAsDouble() * disease.contagion();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = pos.relative(direction);
            if (!level.getBlockState(neighbour).is(state.getBlock())) {
                continue;
            }

            PlantState target = GeneStorage.getState(level, neighbour);
            if (target != null && target.isDiseased()) {
                continue;
            }

            double resistance = target == null ? 0.0D : target.genes().diseaseResistance();
            if (random.nextDouble() < base * (1.0D - resistance)) {
                GeneStorage.infect(level, neighbour, disease);
            }
        }
    }

    /** Particles are the whole visual: no per-plant texture, so any crop from any mod shows it. */
    private static void showSymptoms(ServerLevel level, BlockPos pos, Disease disease) {
        level.sendParticles(disease.particle(),
                pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D,
                3, 0.3D, 0.2D, 0.3D, 0.0D);
    }

    private CropTracker() {}
}

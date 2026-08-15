package com.fauregalliard.geneticsandpests.genetics;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.server.level.ServerPlayer;
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

    /** One pass in this many emits spores, keeping a large outbreak from becoming a particle storm. */
    private static final int SYMPTOM_INTERVAL = 4;

    /** How many blocks near each player are checked for a fresh outbreak per pass. */
    private static final int SAMPLES_PER_PLAYER = 48;
    private static final int SCAN_RADIUS = 12;
    private static final int SCAN_HEIGHT = 3;

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

        scanForOutbreaks(level);

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
                    GeneStorage.markChanged(chunk, stored);
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
     * Looks for fresh outbreaks among the plants around each player.
     *
     * <p>This deliberately does not walk the tracked-plant list. Only plants with a stored genome
     * are in that list, so rolling outbreaks there meant an ordinary vanilla farm could never fall
     * ill at all — disease would have been a punishment reserved for players who engage with
     * breeding, which is backwards. Sampling the world instead reaches every crop, tracked or not.
     */
    private static void scanForOutbreaks(ServerLevel level) {
        double outbreak = Config.OUTBREAK_CHANCE.getAsDouble();
        if (outbreak <= 0.0D) {
            return;
        }

        RandomSource random = level.random;
        int threshold = Config.MONOCULTURE_THRESHOLD.getAsInt();

        for (ServerPlayer player : level.players()) {
            BlockPos origin = player.blockPosition();
            for (int sample = 0; sample < SAMPLES_PER_PLAYER; sample++) {
                BlockPos pos = origin.offset(
                        random.nextInt(SCAN_RADIUS * 2 + 1) - SCAN_RADIUS,
                        random.nextInt(SCAN_HEIGHT * 2 + 1) - SCAN_HEIGHT,
                        random.nextInt(SCAN_RADIUS * 2 + 1) - SCAN_RADIUS);

                if (!level.getBlockState(pos).is(ModTags.GENETIC_CROPS)) {
                    continue;
                }

                PlantState existing = GeneStorage.getState(level, pos);
                if (existing != null && existing.isDiseased()) {
                    continue;
                }

                // The odds climb with how much of the same plant is packed around this one, so a
                // scattered garden is nearly safe and a solid field is a matter of time.
                int crowding = countIdenticalNeighbours(level, pos) - threshold;
                if (crowding <= 0) {
                    continue;
                }

                PlantGenes genes = existing == null ? PlantGenes.DEFAULT : existing.genes();
                double chance = outbreak * crowding * (1.0D - genes.diseaseResistance());
                if (random.nextDouble() < chance) {
                    GeneStorage.infect(level, pos, rollOutbreakDisease(random));
                }
            }
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
            GeneStorage.markChanged(chunk, stored);
            return;
        }

        // Surviving an infection is what breeds resistance, so recovery is driven by Resistance.
        double recovery = Config.RECOVERY_CHANCE.getAsDouble() * (1.0D + plant.genes().diseaseResistance() * 4.0D);
        if (!disease.isLethal() && random.nextDouble() < recovery) {
            stored.put(pos, PlantState.healthy(adaptTo(plant.genes(), random)));
            GeneStorage.markChanged(chunk, stored);
            return;
        }

        stored.put(pos, aged);
        GeneStorage.markChanged(chunk, stored);

        if (disease.spread() == Disease.Spread.NEIGHBOURS) {
            spreadToNeighbours(level, pos, disease, random);
        }
    }

    /**
     * The plague mutation: a plant that lives through an infection comes out hardier and worse at
     * something else. This is the only way Resistance can be bred deliberately — you have to let
     * the disease through your field to get it.
     *
     * <p>Any developed trait can pay the price, the plant's best one included: a population under
     * pressure really does lose ground somewhere, and shielding the prize gene would make the
     * trade-off decorative. The only exclusion is Resistance itself, which is what is being bought.
     */
    private static PlantGenes adaptTo(PlantGenes genes, RandomSource random) {
        PlantGenes hardened = genes.with(Gene.RESISTANCE, genes.get(Gene.RESISTANCE) + 1);

        List<Gene> payable = new ArrayList<>();
        for (Gene gene : Gene.values()) {
            if (gene != Gene.RESISTANCE && hardened.get(gene) > PlantGenes.MIN_VALUE) {
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

    /**
     * A few spores drifting off a sick plant.
     *
     * <p>Kept deliberately sparse. Telling the player where the infection is now falls to the ground
     * stain drawn client-side; an outbreak covering a field would otherwise emit thousands of
     * particles a second and read as noise rather than as illness.
     */
    private static void showSymptoms(ServerLevel level, BlockPos pos, Disease disease) {
        if (level.random.nextInt(SYMPTOM_INTERVAL) != 0) {
            return;
        }

        level.sendParticles(disease.particle(),
                pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D,
                1, 0.25D, 0.2D, 0.25D, 0.0D);
    }

    private CropTracker() {}
}

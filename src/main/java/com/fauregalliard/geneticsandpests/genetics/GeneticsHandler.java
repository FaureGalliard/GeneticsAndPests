package com.fauregalliard.geneticsandpests.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fauregalliard.geneticsandpests.Config;
import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Wires genetics and disease into planting, harvesting, curing and eating.
 *
 * <p>Everything here works through events and tags rather than custom blocks or items, so a crop
 * from any mod joins in the moment it is added to the tags — no duplicated seeds, no parallel copy
 * of the vanilla farming tree, and nothing that assumes a plant grows on farmland.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class GeneticsHandler {
    /**
     * The seed a player is in the middle of planting. Placement is reported by a separate event
     * that does not carry the stack, so it is remembered here for the tick in between.
     */
    private static final Map<UUID, ItemStack> PLANTING = new ConcurrentHashMap<>();

    /** How far pollen travels once bees are involved. */
    private static final int POLLINATED_REACH = 2;

    /** How far from the plant a hive still counts. */
    private static final int HIVE_RANGE = 8;

    // --- Planting -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        ItemStack stack = event.getItemStack();

        // Soaking infected seed in a water trough is how Smut was actually beaten for centuries,
        // and it means the remedy costs no new item at all.
        if (trySoakSeeds(level, event.getPos(), stack)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // A remedy used on a sick plant cures it instead of doing whatever it normally does.
        if (tryCure(level, event.getPos(), stack, event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        UUID player = event.getEntity().getUUID();
        if (stack.is(ModTags.GENETIC_SEEDS)) {
            PLANTING.put(player, stack.copy());
        } else {
            PLANTING.remove(player);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()
                || !(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack seed = PLANTING.remove(player.getUUID());
        if (seed == null || !event.getPlacedBlock().is(ModTags.GENETIC_CROPS)) {
            return;
        }

        PlantGenes genes = seed.get(ModDataComponents.PLANT_GENES.get());
        Disease carried = seed.get(ModDataComponents.SEED_DISEASE.get());

        if (genes != null && !genes.isBaseline()) {
            GeneStorage.set(level, event.getPos(), genes);
        }
        // Smut sprouts wherever the seed was sown, however far that is from the infected field.
        if (carried != null) {
            GeneStorage.infect(level, event.getPos(), carried);
        }
    }

    // --- Curing ---------------------------------------------------------------------------

    /** Rinses a seed-borne infection off in a water cauldron, using up one level of water. */
    private static boolean trySoakSeeds(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty() || stack.get(ModDataComponents.SEED_DISEASE.get()) == null) {
            return false;
        }

        BlockState cauldron = level.getBlockState(pos);
        if (!cauldron.is(Blocks.WATER_CAULDRON)) {
            return false;
        }

        stack.remove(ModDataComponents.SEED_DISEASE.get());

        int water = cauldron.getValue(LayeredCauldronBlock.LEVEL);
        level.setBlockAndUpdate(pos, water > 1
                ? cauldron.setValue(LayeredCauldronBlock.LEVEL, water - 1)
                : Blocks.CAULDRON.defaultBlockState());
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
        return true;
    }

    /** Applies a remedy if the held item cures whatever is growing here. */
    private static boolean tryCure(Level level, BlockPos pos, ItemStack stack, Player player) {
        Disease disease = GeneStorage.getDisease(level, pos);
        if (disease == null || stack.isEmpty() || !stack.is(disease.cureTag())) {
            return false;
        }

        GeneStorage.cure(level, pos);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    // --- Harvest --------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!state.is(ModTags.GENETIC_CROPS)) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();

        // An untracked plant is an ordinary one, so it breeds from the baseline genome rather than
        // being skipped. This is what lets the system start from a plain vanilla farm.
        PlantState stored = GeneStorage.getState(level, pos);
        PlantGenes parent = stored == null ? PlantGenes.DEFAULT : stored.genes();
        Disease disease = stored == null ? null : stored.diseaseOrNull();
        if (stored != null) {
            GeneStorage.clear(level, pos);
        }

        RandomSource random = level.random;
        boolean mature = PlantGrowth.isMature(state);
        if (!mature && stored == null) {
            return;
        }

        PlantGenes offspring = mature
                ? parent.cross(findMate(level, pos, state, parent, random), random)
                : parent;

        // Careless husbandry is the only thing that costs a gene point, and it is always something
        // the player could have avoided.
        if (isPoorHarvest(level, pos, state, parent, mature)
                && random.nextDouble() < Config.POOR_HARVEST_LOSS_CHANCE.getAsDouble()) {
            offspring = offspring.degraded(random);
        }

        applyToDrops(event.getDrops(), level, pos, parent, offspring, disease, mature, random);

        if (mature && disease == null && random.nextDouble() < parent.regrowthChance()) {
            regrow(level, pos, state, parent);
        }
    }

    /**
     * Whether the plant was mistreated: picked unripe, kept darker than its Photosensitivity can
     * handle, or left on dry soil it has no Thirst for.
     */
    private static boolean isPoorHarvest(ServerLevel level, BlockPos pos, BlockState state,
                                         PlantGenes genes, boolean mature) {
        if (!mature) {
            return true;
        }
        if (level.getRawBrightness(pos, 0) < genes.requiredLight()) {
            return true;
        }

        BlockState support = level.getBlockState(PlantGrowth.supportPos(level, pos, state));
        boolean dry = support.getBlock() instanceof FarmBlock && support.getValue(FarmBlock.MOISTURE) == 0;
        return dry && !genes.toleratesDryFarmland();
    }

    /**
     * Stamps genomes onto the seeds a harvest produced, pays out Yield and Fertility, and applies
     * whatever the disease does to the crop.
     *
     * <p>The improvement is rolled <em>once for the whole harvest</em> and lands on a single seed.
     * Rolling it per seed produced a different genome on nearly every one, and since seeds only
     * stack when their genomes match exactly, a season's farming turned into an inventory of
     * one-item stacks. It matters most for potatoes and carrots, where the seed is also the food.
     * One harvest now yields at most two kinds of seed: the ordinary offspring, and the lucky one.
     */
    private static void applyToDrops(List<ItemEntity> drops, ServerLevel level, BlockPos pos,
                                     PlantGenes parent, PlantGenes offspring, @Nullable Disease disease,
                                     boolean mature, RandomSource random) {
        PlantGenes lucky = random.nextDouble() < Config.IMPROVEMENT_CHANCE.getAsDouble()
                ? offspring.improved(random)
                : offspring;
        boolean luckyPending = !lucky.equals(offspring);

        List<ItemEntity> extras = new ArrayList<>();
        List<ItemEntity> ruined = new ArrayList<>();

        for (ItemEntity entity : drops) {
            ItemStack stack = entity.getItem();

            if (!stack.is(ModTags.GENETIC_SEEDS)) {
                applyToProduce(entity, stack, parent, disease, mature, random, ruined);
                continue;
            }

            int seeds = stack.getCount() + (mature ? parent.rollBonusSeeds(random) : 0);

            // The stack that already exists becomes the first seed; the rest ride along beside it,
            // because two seeds with different genomes cannot share one stack.
            // The improved seed is split off on its own; the rest share one genome and one stack.
            if (luckyPending) {
                ItemStack best = stack.copyWithCount(1);
                stamp(best, lucky, disease);
                extras.add(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, best));
                luckyPending = false;
                seeds--;
            }

            if (seeds <= 0) {
                ruined.add(entity);
                continue;
            }

            stack.setCount(seeds);
            stamp(stack, offspring, disease);
        }

        drops.removeAll(ruined);
        drops.addAll(extras);
    }

    private static void applyToProduce(ItemEntity entity, ItemStack stack, PlantGenes parent,
                                       @Nullable Disease disease, boolean mature, RandomSource random,
                                       List<ItemEntity> ruined) {
        if (disease != null && disease.destroysProduce()) {
            // Smut leaves nothing but infected seed.
            ruined.add(entity);
            return;
        }

        if (!mature) {
            return;
        }

        int bonus = parent.bonusProduce(random);
        if (disease != null) {
            bonus /= 2;
            stack.setCount(Math.max(1, stack.getCount() / 2));
        }
        stack.grow(bonus);

        // Ergot costs the harvest nothing visible, which is the whole danger of it.
        if (disease != null && disease.taintsProduce()) {
            stack.set(ModDataComponents.TAINTED.get(), true);
        }
    }

    /**
     * Writes a genome onto a seed, but only one worth carrying. A seed that came out ordinary keeps
     * no component at all, so a vanilla farm never fills its chests with tagged items.
     */
    private static void stamp(ItemStack seed, PlantGenes genes, @Nullable Disease disease) {
        if (!genes.isBaseline()) {
            seed.set(ModDataComponents.PLANT_GENES.get(), genes);
        }
        if (disease != null && disease.spread() == Disease.Spread.SEEDS) {
            seed.set(ModDataComponents.SEED_DISEASE.get(), disease);
        }
    }

    /** Puts the plant back as a seedling, keeping the genome that earned it the second life. */
    private static void regrow(ServerLevel level, BlockPos pos, BlockState state, PlantGenes genes) {
        if (PlantGrowth.ageProperty(state).isEmpty()) {
            return;
        }
        level.setBlock(pos, PlantGrowth.withAge(state, 0), 2);
        GeneStorage.set(level, pos, genes);
    }

    /**
     * Picks a mature plant of the same kind to breed with. A plant with no eligible neighbour
     * pollinates itself, which keeps a lone crop from stalling.
     *
     * <p>A beehive nearby widens the search from the four touching plants to everything within two
     * blocks: real pollinators carry pollen further than a plant can reach on its own, and it gives
     * the player a reason to keep bees beside the field.
     */
    private static PlantGenes findMate(ServerLevel level, BlockPos pos, BlockState state,
                                       PlantGenes fallback, RandomSource random) {
        int reach = hasPollinators(level, pos) ? POLLINATED_REACH : 1;
        List<PlantGenes> mates = new ArrayList<>();

        for (BlockPos mate : BlockPos.betweenClosed(pos.offset(-reach, 0, -reach), pos.offset(reach, 0, reach))) {
            if (mate.equals(pos)) {
                continue;
            }
            BlockState neighbourState = level.getBlockState(mate);
            if (!neighbourState.is(state.getBlock()) || !PlantGrowth.isMature(neighbourState)) {
                continue;
            }
            PlantGenes genes = GeneStorage.get(level, mate);
            if (genes != null) {
                mates.add(genes);
            }
        }

        return mates.isEmpty() ? fallback : mates.get(random.nextInt(mates.size()));
    }

    /** Whether an occupied hive stands close enough for its bees to work this plant. */
    private static boolean hasPollinators(ServerLevel level, BlockPos pos) {
        for (BlockPos hive : BlockPos.betweenClosed(
                pos.offset(-HIVE_RANGE, -HIVE_RANGE, -HIVE_RANGE),
                pos.offset(HIVE_RANGE, HIVE_RANGE, HIVE_RANGE))) {
            if (level.getBlockState(hive).is(BlockTags.BEEHIVES)
                    && level.getBlockEntity(hive) instanceof BeehiveBlockEntity beehive
                    && !beehive.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // --- Other consequences ---------------------------------------------------------------

    @SubscribeEvent
    public static void onFarmlandTrampled(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        PlantGenes genes = GeneStorage.get(level, event.getPos().above());
        if (genes != null && level.random.nextDouble() < genes.tramplingResistance()) {
            event.setCanceled(true);
        }
    }

    /** Eating Ergot-tainted produce is where the disease finally presents itself. */
    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()
                || !Boolean.TRUE.equals(event.getItem().get(ModDataComponents.TAINTED.get()))) {
            return;
        }

        event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.NAUSEA, 300, 0));
    }

    private GeneticsHandler() {}
}

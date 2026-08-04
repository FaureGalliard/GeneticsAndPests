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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Wires the genome into planting, harvesting and trampling.
 *
 * <p>Everything here works through events and tags rather than custom blocks or items, so a crop
 * from any mod becomes genetic the moment it is added to the tags — no duplicated seeds, no
 * parallel copy of the vanilla farming tree.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class GeneticsHandler {
    /**
     * Genome of the seed a player is in the middle of planting. Placement is reported by a separate
     * event that does not carry the stack, so it is remembered here for the handful of ticks in
     * between.
     */
    private static final Map<UUID, PlantGenes> PLANTING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        UUID player = event.getEntity().getUUID();
        ItemStack stack = event.getItemStack();
        PlantGenes genes = stack.is(ModTags.GENETIC_SEEDS)
                ? stack.get(ModDataComponents.PLANT_GENES.get())
                : null;

        if (genes == null) {
            PLANTING.remove(player);
        } else {
            PLANTING.put(player, genes);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()
                || !(event.getEntity() instanceof Player player)) {
            return;
        }

        PlantGenes genes = PLANTING.remove(player.getUUID());
        if (genes != null && !genes.isBaseline() && event.getPlacedBlock().is(ModTags.GENETIC_CROPS)) {
            GeneStorage.set(level, event.getPos(), genes);
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!state.is(ModTags.GENETIC_CROPS)) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();

        // An untracked plant is an ordinary one, so it breeds from the baseline genome rather than
        // being skipped. This is what lets the whole system start from a plain vanilla farm: harvest
        // often enough and a seed eventually comes back better than the one that was sown.
        PlantGenes stored = GeneStorage.get(level, pos);
        PlantGenes parent = stored == null ? PlantGenes.DEFAULT : stored;
        if (stored != null) {
            GeneStorage.clear(level, pos);
        }

        RandomSource random = level.random;
        boolean mature = !(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state);
        if (!mature && stored == null) {
            // Nothing to inherit and nothing to breed: leave an ordinary immature plant alone.
            return;
        }

        // An immature plant simply hands its own genome back; only a real harvest breeds.
        PlantGenes offspring = mature
                ? parent.cross(findMate(level, pos, state, parent, random), random)
                : parent;

        applyToDrops(event.getDrops(), level, pos, parent, offspring, mature, random);

        if (mature && random.nextDouble() < parent.regrowthChance()) {
            regrow(level, pos, state, parent);
        }
    }

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

    /**
     * Stamps genomes onto the seeds a harvest produced and pays out the Yield and Fertility bonuses.
     *
     * <p>Every seed is rolled on its own, so a harvest reads the way breeding should: most seeds come
     * back roughly like the parent and the occasional one comes out better than it went in.
     */
    private static void applyToDrops(List<ItemEntity> drops, ServerLevel level, BlockPos pos,
                                     PlantGenes parent, PlantGenes offspring, boolean mature,
                                     RandomSource random) {
        double improvementChance = Config.IMPROVEMENT_CHANCE.getAsDouble();
        List<ItemEntity> extras = new ArrayList<>();

        for (ItemEntity entity : drops) {
            ItemStack stack = entity.getItem();

            if (!stack.is(ModTags.GENETIC_SEEDS)) {
                if (mature) {
                    stack.grow(parent.rollBonusProduce(random));
                }
                continue;
            }

            int seeds = stack.getCount() + (mature ? parent.rollBonusSeeds(random) : 0);

            // The stack that already exists becomes the first seed; the rest ride along beside it,
            // because two seeds with different genomes cannot share one stack.
            stack.setCount(1);
            stamp(stack, roll(offspring, improvementChance, random));

            for (int i = 1; i < seeds; i++) {
                ItemStack extra = stack.copyWithCount(1);
                stamp(extra, roll(offspring, improvementChance, random));
                extras.add(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, extra));
            }
        }

        drops.addAll(extras);
    }

    private static PlantGenes roll(PlantGenes offspring, double improvementChance, RandomSource random) {
        return random.nextDouble() < improvementChance ? offspring.improved(random) : offspring;
    }

    /**
     * Writes a genome onto a seed, but only one worth carrying. A seed that came out ordinary keeps
     * no component at all, so a vanilla farm never fills its chests with tagged items.
     */
    private static void stamp(ItemStack seed, PlantGenes genes) {
        if (!genes.isBaseline()) {
            seed.set(ModDataComponents.PLANT_GENES.get(), genes);
        }
    }

    /** Puts the plant back as a seedling, keeping the genome that earned it the second life. */
    private static void regrow(ServerLevel level, BlockPos pos, BlockState state, PlantGenes genes) {
        if (state.getBlock() instanceof CropBlock crop) {
            level.setBlock(pos, crop.getStateForAge(0), 2);
            GeneStorage.set(level, pos, genes);
        }
    }

    /**
     * Picks a mature plant of the same kind from the four neighbours to breed with. A plant with no
     * eligible neighbour pollinates itself, which keeps a lone crop from stalling.
     */
    private static PlantGenes findMate(ServerLevel level, BlockPos pos, BlockState state,
                                       PlantGenes fallback, RandomSource random) {
        List<PlantGenes> mates = new ArrayList<>(4);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = pos.relative(direction);
            BlockState neighbourState = level.getBlockState(neighbour);
            if (!neighbourState.is(state.getBlock()) || !isMature(neighbourState)) {
                continue;
            }
            PlantGenes genes = GeneStorage.get(level, neighbour);
            if (genes != null) {
                mates.add(genes);
            }
        }

        return mates.isEmpty() ? fallback : mates.get(random.nextInt(mates.size()));
    }

    private static boolean isMature(BlockState state) {
        return !(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state);
    }

    private GeneticsHandler() {}
}

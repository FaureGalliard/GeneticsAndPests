package com.fauregalliard.geneticsandpests.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fauregalliard.geneticsandpests.Config;
import com.fauregalliard.geneticsandpests.genetics.Gene;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

/**
 * A farmer selling seed off his own stock: a random crop with a trait or two already raised.
 *
 * <p>Buying a strain is meant to be a head start, never a substitute for breeding one — the levels
 * on offer are the same modest range a village field carries.
 */
public class WildSeedTrade implements VillagerTrades.ItemListing {
    private final int emeralds;
    private final int maxUses;
    private final int xp;
    private final int maxTraits;
    private final int ceiling;

    /**
     * @param maxTraits how many genes the seed may have raised
     * @param ceiling   how high those genes may go
     */
    public WildSeedTrade(int emeralds, int maxUses, int xp, int maxTraits, int ceiling) {
        this.emeralds = emeralds;
        this.maxUses = maxUses;
        this.xp = xp;
        this.maxTraits = maxTraits;
        this.ceiling = ceiling;
    }

    @Nullable
    @Override
    public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
        List<Holder<net.minecraft.world.item.Item>> seeds = BuiltInRegistries.ITEM
                .get(ModTags.GENETIC_SEEDS)
                .map(named -> named.stream().toList())
                .orElse(List.of());
        if (seeds.isEmpty()) {
            return null;
        }

        ItemStack seed = new ItemStack(seeds.get(random.nextInt(seeds.size())).value());
        seed.set(ModDataComponents.PLANT_GENES.get(), roll(random, this.maxTraits, this.ceiling));

        return new MerchantOffer(new ItemCost(Items.EMERALD, this.emeralds), seed,
                this.maxUses, this.xp, 0.05F);
    }

    private static PlantGenes roll(RandomSource random, int maxTraits, int ceiling) {
        int top = Math.max(PlantGenes.MIN_VALUE + 1, Math.min(ceiling, Config.MAX_GENE_VALUE.getAsInt()));
        Map<Gene, Integer> genes = new EnumMap<>(Gene.class);
        Gene[] all = Gene.values();

        for (int i = 0; i < 1 + random.nextInt(Math.max(1, maxTraits)); i++) {
            Gene gene = all[random.nextInt(all.length)];
            genes.put(gene, PlantGenes.MIN_VALUE + 1 + random.nextInt(top - PlantGenes.MIN_VALUE));
        }
        return new PlantGenes(genes);
    }
}

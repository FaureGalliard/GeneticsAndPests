package com.fauregalliard.geneticsandpests.genetics;

import com.fauregalliard.geneticsandpests.registry.ModDataComponents;
import com.fauregalliard.geneticsandpests.registry.ModItems;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The two operations a grafting bench performs, kept free of any inventory or menu so they can be
 * reasoned about — and reused — on their own.
 */
public final class Grafting {
    /**
     * The cutting a seed would yield: its strongest trait, capped by what the catalyst can carry.
     *
     * <p>Only the best gene can be taken, which keeps the tool honest — you are moving a plant's
     * one achievement, not stripping it for parts.
     */
    public static ItemStack extract(ItemStack seed, @Nullable Catalyst catalyst) {
        if (catalyst == null || !seed.is(ModTags.GENETIC_SEEDS)) {
            return ItemStack.EMPTY;
        }

        PlantGenes genes = seed.get(ModDataComponents.PLANT_GENES.get());
        if (genes == null || genes.isBaseline()) {
            return ItemStack.EMPTY;
        }

        Gene best = null;
        for (Gene gene : Gene.values()) {
            if (best == null || genes.get(gene) > genes.get(best)) {
                best = gene;
            }
        }

        int level = Math.min(genes.get(best), catalyst.cap());
        if (level <= PlantGenes.MIN_VALUE) {
            return ItemStack.EMPTY;
        }

        ItemStack scion = new ItemStack(ModItems.SCION.get());
        scion.set(ModDataComponents.SCION.get(), new ScionData(seed.getItem(), best, level));
        return scion;
    }

    /**
     * The seed a graft would produce, or nothing when the graft would achieve nothing — a scion
     * weaker than what the seed already carries is not worth the cutting it cost.
     */
    public static ItemStack graft(ItemStack seed, ItemStack scionStack) {
        ScionData scion = scionStack.get(ModDataComponents.SCION.get());
        if (scion == null || !seed.is(ModTags.GENETIC_SEEDS)) {
            return ItemStack.EMPTY;
        }

        // A cutting only takes on its own kind: potato onto potato, wheat onto wheat.
        if (!scion.matches(seed)) {
            return ItemStack.EMPTY;
        }

        PlantGenes genes = seed.getOrDefault(ModDataComponents.PLANT_GENES.get(), PlantGenes.DEFAULT);
        if (genes.get(scion.gene()) >= scion.level()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = seed.copyWithCount(1);
        result.set(ModDataComponents.PLANT_GENES.get(), genes.with(scion.gene(), scion.level()));
        return result;
    }

    private Grafting() {}
}

package com.fauregalliard.geneticsandpests.data;

import java.util.concurrent.CompletableFuture;

import com.fauregalliard.geneticsandpests.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Recipes for the three remedies, all of them things a medieval farmer could actually make. */
public class ModRecipeProvider extends RecipeProvider {
    protected ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // Wood ash: what is left when you burn the hedge.
        SimpleCookingRecipeBuilder.smelting(this.tag(ItemTags.LEAVES),
                        RecipeCategory.MISC, ModItems.ASH.get(), 0.1F, 200)
                .unlockedBy("has_leaves", this.has(ItemTags.LEAVES))
                .save(this.output);

        // Quicklime: calcite burnt in a kiln, which is why the cure for Ergot sends you to a geode.
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.CALCITE),
                        RecipeCategory.MISC, ModItems.LIME.get(), 0.1F, 200)
                .unlockedBy("has_calcite", this.has(Items.CALCITE))
                .save(this.output);

        // Brine: salt drawn out of dried kelp into a bottle of water.
        this.shapeless(RecipeCategory.MISC, ModItems.BRINE.get())
                .requires(Items.POTION)
                .requires(Items.DRIED_KELP, 2)
                .unlockedBy("has_dried_kelp", this.has(Items.DRIED_KELP))
                .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Genetics and Pests Recipes";
        }
    }
}

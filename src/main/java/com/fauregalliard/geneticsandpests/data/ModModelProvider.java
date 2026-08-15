package com.fauregalliard.geneticsandpests.data;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.registry.ModItems;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

/**
 * Writes the item models and their client-side definitions.
 *
 * <p>Each of these is two near-identical JSON files by hand; here it is one line that stops
 * compiling the day an item is renamed, which is the whole reason for generating them.
 */
public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, GeneticsAndPests.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.ASH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LIME.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BRINE.get(), ModelTemplates.FLAT_ITEM);
    }
}

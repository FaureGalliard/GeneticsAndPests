package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.content.GraftingTableBlock;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's only block. There are still no crops here: genetics rides on the plants the game
 * already has, and this is a workbench, not a plant.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GeneticsAndPests.MODID);

    public static final DeferredBlock<GraftingTableBlock> GRAFTING_TABLE = BLOCKS.registerBlock(
            "grafting_table",
            GraftingTableBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE));

    private ModBlocks() {}
}

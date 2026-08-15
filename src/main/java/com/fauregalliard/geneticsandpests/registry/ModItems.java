package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.content.BrineItem;
import com.fauregalliard.geneticsandpests.content.ScionItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's only items, and all three are remedies. There are deliberately no seeds or crops here:
 * genetics rides on the plants the game already has.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GeneticsAndPests.MODID);

    /** Wood ash, dusted on a plant to cure Rust. */
    public static final DeferredItem<Item> ASH = ITEMS.registerSimpleItem("ash", Item.Properties::new);

    /** Burnt lime, worked into the soil to cure Ergot. */
    public static final DeferredItem<Item> LIME = ITEMS.registerSimpleItem("lime", Item.Properties::new);

    /** Salt water for soaking seed, which is the only thing that clears Smut. */
    public static final DeferredItem<BrineItem> BRINE = ITEMS.registerItem("brine", BrineItem::new, Item.Properties::new);

    /** A cutting carrying one trait, taken from a seed at the grafting bench. */
    public static final DeferredItem<ScionItem> SCION = ITEMS.registerItem("scion", ScionItem::new, Item.Properties::new);

    public static final DeferredItem<BlockItem> GRAFTING_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.GRAFTING_TABLE);

    private ModItems() {}
}

package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Which blocks and items take part in the genetics system. Both tags are plain datapack tags, so
 * adding support for another mod's crop needs no code — only a line of JSON.
 */
public final class ModTags {
    /** Crop blocks that carry a genome while planted. */
    public static final TagKey<Block> GENETIC_CROPS = TagKey.create(Registries.BLOCK, id("genetic_crops"));

    /** Items that carry a genome between harvest and planting. */
    public static final TagKey<Item> GENETIC_SEEDS = TagKey.create(Registries.ITEM, id("genetic_seeds"));

    /** Anything with an edge good enough to take a cutting: shears, swords, modded knives. */
    public static final TagKey<Item> GRAFTING_BLADES = TagKey.create(Registries.ITEM, id("grafting_blades"));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, path);
    }

    private ModTags() {}
}

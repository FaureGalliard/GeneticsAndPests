package com.fauregalliard.geneticsandpests.genetics;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a grafting bench needs to take a cutting, and how good a cutting it can take.
 *
 * <p>The tier caps the level rather than charging a price, because a ceiling is something a player
 * can read off the item — "beeswax gets you to seven" — where a table of costs is something they
 * have to look up. Each tier is a tag, so a datapack can change the ingredients without code.
 */
public enum Catalyst {
    /** Grafting wax. Renewable, and enough for everyday work. */
    LESSER("lesser", 7),

    /** Sends you looking for a geode. */
    GREATER("greater", 14),

    /** Not renewable, which is what makes a top-level graft cost something real. */
    MASTER("master", 20);

    private final String id;
    private final int cap;

    Catalyst(String id, int cap) {
        this.id = id;
        this.cap = cap;
    }

    /** The highest gene level this catalyst can carry into a scion. */
    public int cap() {
        return this.cap;
    }

    public TagKey<Item> tag() {
        return TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "catalysts/" + this.id));
    }

    /** The best tier the stack qualifies for, or null when it is not a catalyst at all. */
    @Nullable
    public static Catalyst of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Catalyst best = null;
        for (Catalyst catalyst : values()) {
            if (stack.is(catalyst.tag()) && (best == null || catalyst.cap > best.cap)) {
                best = catalyst;
            }
        }
        return best;
    }
}

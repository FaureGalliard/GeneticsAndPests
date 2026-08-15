package com.fauregalliard.geneticsandpests.content;

import com.fauregalliard.geneticsandpests.genetics.Catalyst;
import com.fauregalliard.geneticsandpests.genetics.Grafting;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;
import com.fauregalliard.geneticsandpests.registry.ModMenus;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The bench's logic. One tool slot serves both operations, and which one happens is decided by what
 * is in it: wax takes a cutting, a cutting grafts it on. That removes the need for a mode button and
 * makes the bench readable from its contents alone.
 */
public class GraftingTableMenu extends AbstractContainerMenu {
    private final Container bench;

    /** Client-side constructor: the real contents arrive through the usual slot syncing. */
    public GraftingTableMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(GraftingTableBlockEntity.SIZE));
    }

    public GraftingTableMenu(int id, Inventory inventory, Container bench) {
        super(ModMenus.GRAFTING_TABLE.get(), id);
        this.bench = bench;

        // Both input slots recompute the preview themselves. A block entity's setChanged() does not
        // reach the menu, so relying on slotsChanged left the result stale until the screen was
        // closed and opened again.
        this.addSlot(new Slot(bench, GraftingTableBlockEntity.SLOT_SEED, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.GENETIC_SEEDS);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                GraftingTableMenu.this.refreshResult();
            }
        });

        this.addSlot(new Slot(bench, GraftingTableBlockEntity.SLOT_TOOL, 76, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Catalyst.of(stack) != null || stack.has(ModDataComponents.SCION.get());
            }

            @Override
            public void setChanged() {
                super.setChanged();
                GraftingTableMenu.this.refreshResult();
            }
        });

        this.addSlot(new Slot(bench, GraftingTableBlockEntity.SLOT_RESULT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // Both inputs are spent only once the player actually accepts the result, which is
                // what lets the preview sit there harmlessly while they decide.
                bench.removeItem(GraftingTableBlockEntity.SLOT_SEED, 1);
                bench.removeItem(GraftingTableBlockEntity.SLOT_TOOL, 1);
                GraftingTableMenu.this.refreshResult();
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        this.refreshResult();
    }

    /** Recomputes the preview whenever the inputs change. */
    private void refreshResult() {
        ItemStack seed = this.bench.getItem(GraftingTableBlockEntity.SLOT_SEED);
        ItemStack tool = this.bench.getItem(GraftingTableBlockEntity.SLOT_TOOL);

        ItemStack result = tool.has(ModDataComponents.SCION.get())
                ? Grafting.graft(seed, tool)
                : Grafting.extract(seed, Catalyst.of(tool));

        this.bench.setItem(GraftingTableBlockEntity.SLOT_RESULT, result);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.refreshResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.bench.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int benchSlots = GraftingTableBlockEntity.SIZE;

        if (index < benchSlots) {
            if (!this.moveItemStackTo(stack, benchSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (!this.moveItemStackTo(stack, 0, benchSlots - 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}

package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.content.GraftingTableMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, GeneticsAndPests.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GraftingTableMenu>> GRAFTING_TABLE =
            MENUS.register("grafting_table", () -> new MenuType<>(
                    GraftingTableMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}
}

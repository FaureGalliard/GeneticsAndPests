package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A tab of the mod's own, so the remedies are findable and legibly part of Genetics and Pests
 * rather than three unexplained items lost among the vanilla ingredients.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GeneticsAndPests.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.geneticsandpests"))
                    .withTabsBefore(CreativeModeTabs.INGREDIENTS)
                    .icon(() -> ModItems.BRINE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ASH.get());
                        output.accept(ModItems.LIME.get());
                        output.accept(ModItems.BRINE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}

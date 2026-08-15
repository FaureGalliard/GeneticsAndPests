package com.fauregalliard.geneticsandpests.content;

import java.util.function.Consumer;

import com.fauregalliard.geneticsandpests.genetics.ScionData;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A cutting carrying one trait. The tooltip is the only way to tell two of them apart. */
public class ScionItem extends Item {
    public ScionItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipAdder, TooltipFlag flag) {
        ScionData scion = stack.get(ModDataComponents.SCION.get());
        if (scion == null) {
            return;
        }
        tooltipAdder.accept(scion.source().getName(scion.source().getDefaultInstance())
                .copy().withStyle(ChatFormatting.DARK_GRAY));
        tooltipAdder.accept(Component.translatable(scion.gene().translationKey())
                .append(" " + scion.level())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

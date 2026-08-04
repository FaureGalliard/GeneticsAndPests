package com.fauregalliard.geneticsandpests.client;

import java.util.ArrayList;
import java.util.List;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.Gene;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Shows a seed's genome in its tooltip, in muted grey and only ever as much of it as is worth
 * reading at a glance.
 *
 * <ul>
 * <li>An ordinary seed shows nothing at all.</li>
 * <li>One or two bred genes are listed outright — short enough not to be in the way.</li>
 * <li>More than that collapses to a summary line, and Shift expands the full list.</li>
 * </ul>
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID, value = Dist.CLIENT)
public final class GeneTooltip {
    /** Above this many bred genes the list is folded away behind Shift. */
    private static final int INLINE_LIMIT = 2;

    /** Genes per line once expanded, so a fully bred seed reads as a couple of short lines. */
    private static final int PER_LINE = 3;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        PlantGenes genes = event.getItemStack().get(ModDataComponents.PLANT_GENES.get());
        if (genes == null || genes.isBaseline()) {
            return;
        }

        List<Component> bred = new ArrayList<>();
        for (Gene gene : Gene.values()) {
            int value = genes.get(gene);
            if (value > PlantGenes.MIN_VALUE) {
                bred.add(Component.translatable(gene.translationKey()).append(" " + value));
            }
        }

        List<Component> tooltip = event.getToolTip();
        if (bred.size() > INLINE_LIMIT && !isShiftDown()) {
            tooltip.add(Component.translatable("tooltip.geneticsandpests.collapsed",
                            bred.size(), genes.totalLevel())
                    .append(" · ")
                    .append(Component.translatable("tooltip.geneticsandpests.hold_shift"))
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        for (int i = 0; i < bred.size(); i += PER_LINE) {
            MutableComponent line = Component.empty();
            for (int j = i; j < Math.min(i + PER_LINE, bred.size()); j++) {
                if (j > i) {
                    line.append(" · ");
                }
                line.append(bred.get(j));
            }
            tooltip.add(line.withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * 1.21.11 dropped the static {@code Screen.hasShiftDown()} in favour of per-event modifiers, so
     * a tooltip — which is drawn outside any key event — has to ask the window directly.
     */
    private static boolean isShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
    }

    private GeneTooltip() {}
}

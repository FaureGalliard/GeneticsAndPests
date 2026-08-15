package com.fauregalliard.geneticsandpests.compat;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.Disease;
import com.fauregalliard.geneticsandpests.genetics.Gene;
import com.fauregalliard.geneticsandpests.genetics.GeneStorage;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;
import com.fauregalliard.geneticsandpests.genetics.PlantState;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows a plant's disease and genome in Jade's HUD.
 *
 * <p>Jade is an optional dependency: this class is only ever loaded by Jade's own plugin scanner,
 * so the mod runs perfectly well without it and nothing here is touched when it is absent.
 */
@WailaPlugin(GeneticsAndPests.MODID)
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new PlantProvider(), Block.class);
    }

    private static class PlantProvider implements IBlockComponentProvider {
        private static final Identifier UID =
                Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "plant");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getBlockState().is(ModTags.GENETIC_CROPS)) {
                return;
            }

            PlantState plant = GeneStorage.getState(accessor.getLevel(), accessor.getPosition());
            Disease disease = plant == null ? null : plant.diseaseOrNull();
            if (disease != null) {
                tooltip.add(Component.translatable(disease.translationKey())
                        .withStyle(ChatFormatting.RED));
            }

            PlantGenes genes = plant == null ? PlantGenes.DEFAULT : plant.genes();
            if (!genes.isBaseline()) {
                tooltip.add(describe(genes).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static MutableComponent describe(PlantGenes genes) {
        MutableComponent line = Component.empty();
        boolean first = true;
        for (Gene gene : Gene.values()) {
            int value = genes.get(gene);
            if (value <= PlantGenes.MIN_VALUE) {
                continue;
            }
            if (!first) {
                line.append(" · ");
            }
            line.append(Component.translatable(gene.translationKey())).append(" " + value);
            first = false;
        }
        return line;
    }
}

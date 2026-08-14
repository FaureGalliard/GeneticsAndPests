package com.fauregalliard.geneticsandpests.client;

import java.util.ArrayList;
import java.util.List;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.Disease;
import com.fauregalliard.geneticsandpests.genetics.Gene;
import com.fauregalliard.geneticsandpests.genetics.GeneStorage;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;
import com.fauregalliard.geneticsandpests.genetics.PlantState;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Adds the plant the player is looking at to the F3 screen: its disease, and its genome.
 *
 * <p>This only has anything to say because the chunk attachment is synced to clients. The data is
 * authored entirely on the server, so before syncing there was nothing here to read.
 */
public class PlantDebugEntry implements DebugScreenEntry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "plant");
    public static final Identifier GROUP = Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "plant");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level,
                        @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        if (level == null || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = ((BlockHitResult) minecraft.hitResult).getBlockPos();
        if (!level.getBlockState(pos).is(ModTags.GENETIC_CROPS)) {
            return;
        }

        PlantState plant = GeneStorage.getState(level, pos);
        List<String> lines = new ArrayList<>(2);

        Disease disease = plant == null ? null : plant.diseaseOrNull();
        lines.add("Health: " + (disease == null ? "healthy" : disease.getSerializedName()));

        PlantGenes genes = plant == null ? PlantGenes.DEFAULT : plant.genes();
        lines.add(genes.isBaseline() ? "Genes: baseline" : "Genes: " + describe(genes));

        displayer.addToGroup(GROUP, lines);
    }

    private static String describe(PlantGenes genes) {
        StringBuilder report = new StringBuilder();
        for (Gene gene : Gene.values()) {
            int value = genes.get(gene);
            if (value > PlantGenes.MIN_VALUE) {
                report.append(report.isEmpty() ? "" : ", ")
                        .append(gene.getSerializedName()).append(' ').append(value);
            }
        }
        return report.toString();
    }
}

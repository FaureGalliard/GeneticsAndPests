package com.fauregalliard.geneticsandpests.genetics;

import java.util.EnumMap;
import java.util.Map;

import com.fauregalliard.geneticsandpests.Config;
import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Seeds the world's own crops with genomes the moment their chunk is first generated.
 *
 * <p>Crops do not grow wild in Minecraft — the only ones the world places itself are in village
 * fields — so this needs no check for a village. That makes a village farm worth walking to and
 * worth robbing, and it means genetics can be found as well as bred.
 *
 * <p>Sections are skipped wholesale unless they contain a crop at all, so a freshly generated chunk
 * costs a handful of palette checks rather than a scan of 98,304 blocks.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class WildStrains {
    /** How many genes a wild strain has raised, and how far. */
    private static final int MAX_TRAITS = 3;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        double chance = Config.WILD_STRAIN_CHANCE.getAsDouble();
        if (chance <= 0.0D) {
            return;
        }

        LevelChunk chunk = event.getChunk();
        RandomSource random = level.random;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(ModTags.GENETIC_CROPS))) {
                continue;
            }
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));

            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (!section.getBlockState(x, y, z).is(ModTags.GENETIC_CROPS)
                                || random.nextDouble() >= chance) {
                            continue;
                        }
                        pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                        GeneStorage.set(level, pos.immutable(), roll(random));
                    }
                }
            }
        }
    }

    /**
     * A wild genome: a couple of traits a village has bred up over generations, never the whole
     * nine. Good enough to be worth stealing, nowhere near what a player can build.
     */
    private static PlantGenes roll(RandomSource random) {
        int ceiling = Math.max(PlantGenes.MIN_VALUE + 1, Config.WILD_STRAIN_CEILING.getAsInt());
        Map<Gene, Integer> genes = new EnumMap<>(Gene.class);
        Gene[] all = Gene.values();

        for (int i = 0; i < 1 + random.nextInt(MAX_TRAITS); i++) {
            Gene gene = all[random.nextInt(all.length)];
            genes.put(gene, PlantGenes.MIN_VALUE + 1 + random.nextInt(ceiling - PlantGenes.MIN_VALUE));
        }
        return new PlantGenes(genes);
    }

    private WildStrains() {}
}

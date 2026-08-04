package com.fauregalliard.geneticsandpests.command;

import java.util.Arrays;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.fauregalliard.geneticsandpests.Config;
import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.Gene;
import com.fauregalliard.geneticsandpests.genetics.PlantGenes;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;
import com.fauregalliard.geneticsandpests.registry.ModTags;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Editing tools for the genome of the seed in your main hand, so breeding can be tested without
 * farming a dozen generations first.
 *
 * <p>The command lives under the mod id rather than something short like {@code /genes}: a root
 * literal that generic would collide with any other genetics mod installed alongside this one.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class GeneticsCommand {
    private static final String GENE_ARG = "gene";
    private static final String VALUE_ARG = "value";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> genes = Commands.literal("genes")
                .then(Commands.literal("get")
                        .executes(GeneticsCommand::get))
                .then(Commands.literal("clear")
                        .executes(GeneticsCommand::clear))
                .then(Commands.literal("fill")
                        .then(Commands.argument(VALUE_ARG, IntegerArgumentType.integer(PlantGenes.MIN_VALUE))
                                .executes(GeneticsCommand::fill)))
                .then(Commands.literal("set")
                        .then(Commands.argument(GENE_ARG, StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Gene.values()).map(Gene::getSerializedName), builder))
                                .then(Commands.argument(VALUE_ARG, IntegerArgumentType.integer(PlantGenes.MIN_VALUE))
                                        .executes(GeneticsCommand::set))));

        event.getDispatcher().register(Commands.literal(GeneticsAndPests.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(genes));
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = heldSeed(context);
        PlantGenes genes = stack.getOrDefault(ModDataComponents.PLANT_GENES.get(), PlantGenes.DEFAULT);

        StringBuilder report = new StringBuilder();
        for (Gene gene : Gene.values()) {
            report.append(report.isEmpty() ? "" : ", ")
                    .append(gene.getSerializedName()).append(' ').append(genes.get(gene));
        }

        String description = report.toString();
        context.getSource().sendSuccess(() -> Component.literal(description), false);
        return genes.totalLevel();
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = heldSeed(context);
        String name = StringArgumentType.getString(context, GENE_ARG);
        Gene gene = Arrays.stream(Gene.values())
                .filter(candidate -> candidate.getSerializedName().equals(name))
                .findFirst()
                .orElse(null);

        if (gene == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.geneticsandpests.unknown_gene", name));
            return 0;
        }

        int value = IntegerArgumentType.getInteger(context, VALUE_ARG);
        PlantGenes genes = stack.getOrDefault(ModDataComponents.PLANT_GENES.get(), PlantGenes.DEFAULT)
                .with(gene, value);
        stack.set(ModDataComponents.PLANT_GENES.get(), genes);

        // The genome clamps to the configured ceiling, so report what was actually stored.
        int stored = genes.get(gene);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.geneticsandpests.set",
                        gene.getSerializedName(), stored), false);
        return stored;
    }

    private static int fill(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = heldSeed(context);
        PlantGenes genes = PlantGenes.uniform(IntegerArgumentType.getInteger(context, VALUE_ARG));
        stack.set(ModDataComponents.PLANT_GENES.get(), genes);

        int value = genes.get(Gene.GROWTH);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.geneticsandpests.filled", value), false);
        return genes.totalLevel();
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = heldSeed(context);
        stack.remove(ModDataComponents.PLANT_GENES.get());
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.geneticsandpests.cleared"), false);
        return 1;
    }

    /**
     * The stack in the player's main hand, rejected unless it is something the mod can actually
     * plant — a genome on a stone block would silently do nothing.
     */
    private static ItemStack heldSeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!stack.is(ModTags.GENETIC_SEEDS)) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Component.translatable("commands.geneticsandpests.not_a_seed")).create();
        }
        return stack;
    }

    private GeneticsCommand() {}
}

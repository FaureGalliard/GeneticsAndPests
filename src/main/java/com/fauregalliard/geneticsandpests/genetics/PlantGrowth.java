package com.fauregalliard.geneticsandpests.genetics;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Reads and advances a plant's growth stage without caring what class the block is.
 *
 * <p>The obvious implementation is {@code instanceof CropBlock}, and it is wrong: cocoa, nether
 * wart and sweet berries are not crop blocks, and neither are plenty of modded plants. What they
 * all share is an integer {@code age} property, so that is what this reads. A plant is supported
 * by whatever it is attached to — below for wheat, sideways for cocoa — which is what the disease
 * overlay needs in order to stain the right surface.
 */
public final class PlantGrowth {
    /** The block's age property, if it grows in stages at all. */
    public static Optional<IntegerProperty> ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && "age".equals(age.getName())) {
                return Optional.of(age);
            }
        }
        return Optional.empty();
    }

    public static int age(BlockState state) {
        return ageProperty(state).map(state::getValue).orElse(0);
    }

    /** The highest age this block can reach; 0 when it does not grow. */
    public static int maxAge(BlockState state) {
        return ageProperty(state)
                .map(age -> age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0))
                .orElse(0);
    }

    /** A plant with no age property is always ready, since there is nothing to wait for. */
    public static boolean isMature(BlockState state) {
        return ageProperty(state)
                .map(age -> state.getValue(age) >= maxAge(state))
                .orElse(true);
    }

    public static boolean canGrow(BlockState state) {
        return ageProperty(state).isPresent() && !isMature(state);
    }

    /** The same state one stage older, or the state untouched when it cannot grow. */
    public static BlockState grown(BlockState state) {
        return ageProperty(state)
                .filter(age -> state.getValue(age) < maxAge(state))
                .<BlockState>map(age -> state.setValue(age, state.getValue(age) + 1))
                .orElse(state);
    }

    public static BlockState withAge(BlockState state, int value) {
        return ageProperty(state).<BlockState>map(age -> state.setValue(age, value)).orElse(state);
    }

    /**
     * The face of the neighbouring block this plant is rooted in.
     *
     * <p>Anything with a horizontal facing — cocoa is the vanilla case — hangs off the block it
     * faces away from. Everything else stands on the block below.
     */
    public static Direction supportDirection(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()
                : Direction.DOWN;
    }

    /** The position of the block this plant is rooted in: farmland, a jungle log, soul sand. */
    public static BlockPos supportPos(BlockGetter level, BlockPos pos, BlockState state) {
        return pos.relative(supportDirection(state));
    }

    private PlantGrowth() {}
}

package com.fauregalliard.geneticsandpests.content;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A woodworker's bench for taking and setting cuttings. */
public class GraftingTableBlock extends Block implements EntityBlock {
    public static final MapCodec<GraftingTableBlock> CODEC = simpleCodec(GraftingTableBlock::new);

    public GraftingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraftingTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GraftingTableBlockEntity bench) {
            player.openMenu(bench);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whatever was left on the bench falls out when it is broken, rather than vanishing. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
                                               BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof GraftingTableBlockEntity bench) {
            // The result slot is only ever a preview of inputs that are still present, so dropping it
            // too would hand out a free copy.
            bench.setItem(GraftingTableBlockEntity.SLOT_RESULT, net.minecraft.world.item.ItemStack.EMPTY);
            Containers.dropContents(level, pos, bench);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}

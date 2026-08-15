package com.fauregalliard.geneticsandpests.registry;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.content.GraftingTableBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GeneticsAndPests.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraftingTableBlockEntity>> GRAFTING_TABLE =
            BLOCK_ENTITIES.register("grafting_table", () -> new BlockEntityType<>(
                    GraftingTableBlockEntity::new,
                    ModBlocks.GRAFTING_TABLE.get()));

    private ModBlockEntities() {}
}

package com.fauregalliard.geneticsandpests;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.fauregalliard.geneticsandpests.registry.ModAttachments;
import com.fauregalliard.geneticsandpests.registry.ModDataComponents;
import com.fauregalliard.geneticsandpests.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

/**
 * Genetics and Pests adds no blocks and no items of its own: it attaches genetics to the crops the
 * game already has, so any farming mod is supported by adding its blocks and seeds to two tags.
 */
@Mod(GeneticsAndPests.MODID)
public class GeneticsAndPests {
    public static final String MODID = "geneticsandpests";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GeneticsAndPests(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        // Remedies live in the vanilla ingredient tab rather than a tab of the mod's own
        modEventBus.addListener(ModItems::addToVanillaTabs);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}

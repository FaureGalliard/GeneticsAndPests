package com.fauregalliard.geneticsandpests;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GeneticsAndPests.MODID)
public class GeneticsAndPests {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "geneticsandpests";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Registers, all registered under the "geneticsandpests" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Placeholder item so the creative tab has an icon until the real content exists
    public static final DeferredItem<Item> GENETIC_SEED = ITEMS.registerSimpleItem("genetic_seed");

    // The mod's creative tab, placed after the food & drinks tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENETICS_TAB = CREATIVE_MODE_TABS.register("genetics_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.geneticsandpests"))
            .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
            .icon(() -> GENETIC_SEED.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(GENETIC_SEED.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public GeneticsAndPests(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Genetics and Pests: common setup complete");
    }
}

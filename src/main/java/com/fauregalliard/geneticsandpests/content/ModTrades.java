package com.fauregalliard.geneticsandpests.content;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.registry.ModItems;

import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * What a farmer will deal in once the mod is installed: the remedies a village would keep, and
 * eventually seed from their own fields.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID)
public final class ModTrades {
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(VillagerProfession.FARMER)) {
            return;
        }

        // Apprentice: the everyday remedy, cheap enough to keep a field going.
        event.getTrades().get(2).add(sell(1, ModItems.ASH.get(), 4, 12, 5));

        // Journeyman: lime, which otherwise means finding a geode.
        event.getTrades().get(3).add(sell(2, ModItems.LIME.get(), 2, 8, 10));

        // Expert: seed dressing, and field-grade seed — about what you would find growing in the
        // village anyway, so the convenience is what you are paying for.
        event.getTrades().get(4).add(sell(3, ModItems.BRINE.get(), 1, 6, 15));
        event.getTrades().get(4).add(new WildSeedTrade(8, 3, 15, 2, 5));

        // Master: the farmer's own stock, better than anything in his fields. This is what makes
        // levelling him up worth the emeralds — the expert seed is not just cheaper, it is worse.
        event.getTrades().get(5).add(new WildSeedTrade(16, 2, 30, 3, 9));
    }

    private static VillagerTrades.ItemListing sell(int emeralds, net.minecraft.world.item.Item result,
                                                   int count, int maxUses, int xp) {
        return (level, trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds), new ItemStack(result, count), maxUses, xp, 0.05F);
    }

    private ModTrades() {}
}

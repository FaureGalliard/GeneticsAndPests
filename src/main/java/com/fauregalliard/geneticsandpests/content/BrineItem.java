package com.fauregalliard.geneticsandpests.content;

import com.fauregalliard.geneticsandpests.registry.ModDataComponents;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Salt water for treating seed. Using it soaks every infected seed the player is carrying at once,
 * which is how seed dressing actually worked: you treated the sack before sowing, not one grain at
 * a time.
 */
public class BrineItem extends Item {
    public BrineItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack brine = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int cleaned = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.remove(ModDataComponents.SEED_DISEASE.get()) != null) {
                cleaned++;
            }
        }

        if (cleaned == 0) {
            return InteractionResult.PASS;
        }

        if (!player.getAbilities().instabuild) {
            brine.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY,
                SoundSource.PLAYERS, 0.7F, 1.1F);
        return InteractionResult.SUCCESS;
    }
}

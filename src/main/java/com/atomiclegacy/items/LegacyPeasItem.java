package com.atomiclegacy.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Peas despawn nearby duck entities, matching ItemPeas. */
public final class LegacyPeasItem extends Item {
    public LegacyPeasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // [parity H1] ItemPeas.onItemRightClick: consume one, then every EntityQuackos within
        // player.boundingBox.expand(50, 50, 50) despawns (balefire burst + 3 spawn_duck).
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!level.isClientSide) {
            for (com.atomiclegacy.entity.LegacyQuackosEntity duck : level.getEntitiesOfClass(
                    com.atomiclegacy.entity.LegacyQuackosEntity.class, player.getBoundingBox().inflate(50.0D))) {
                duck.legacyDespawn();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

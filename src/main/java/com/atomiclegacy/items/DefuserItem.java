package com.atomiclegacy.items;

import com.atomiclegacy.entity.GlyphidEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DefuserItem extends Item {
    public DefuserItem(Properties properties) {
        super(properties);
    }

    // [parity I3: ItemTooling extends ItemCraftingDegradation - container item with +1 damage, no anvil repair]
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return LegacyCraftingDegradation.remainder(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (target instanceof Creeper creeper) {
            if (!player.level().isClientSide) {
                return LegacyDefuserArmorModItem.defuse(creeper, player, true)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        if (target instanceof GlyphidEntity glyphid && glyphid.isNuclearDeathCountdownActive()) {
            if (!player.level().isClientSide) {
                return glyphid.legacyDefuseNuclearCountdown(player)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

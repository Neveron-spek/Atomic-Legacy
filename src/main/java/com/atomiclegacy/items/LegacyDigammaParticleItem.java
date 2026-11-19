package com.atomiclegacy.items;

import com.atomiclegacy.entity.LegacyBlackHoleEntity;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Exact gameplay counterpart of 1.7.10 ItemDigamma(60). */
public final class LegacyDigammaParticleItem extends ParticleCapsuleItem {
    private static final float DOSE_PER_TICK = 1.0F / 60.0F;

    public LegacyDigammaParticleItem(Properties properties) {
        super(properties, true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player) {
            HbmLivingRadiation data = HbmLivingRadiation.get(player);
            com.atomiclegacy.util.ContaminationUtil.applyDigammaData(player, data, DOSE_PER_TICK); // [parity K2] ItemDigamma.onUpdate
            HbmLivingRadiation.set(player, data);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide && entity.onGround()) {
            // EntityQuasar in the current 1.7.10 source is the size-5
            // black-hole implementation; no separate world logic is added.
            if (HbmConfig.DROPPED_SINGULARITY_EFFECT.get()) {
                LegacyBlackHoleEntity.spawn(entity.level(), entity.position(), 5.0F, false);
            }
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // ItemDigamma.addInformation, digamma = 60: (digamma / 20F) + "s"; ((int) ((1000F / digamma) * 200F)) / 10F
        int digamma = 60;
        LegacyTooltips.line(tooltip, ChatFormatting.GOLD, LegacyTooltips.resolve("trait.atomiclegacy.hlParticle", "1.67*10\u00b3\u2074a"));
        LegacyTooltips.line(tooltip, ChatFormatting.RED, LegacyTooltips.resolve("trait.atomiclegacy.hlPlayer", (digamma / 20F) + "s"));
        LegacyTooltips.line(tooltip, "");
        super.appendHoverText(stack, context, tooltip, flag);
        float d = ((int) ((1000F / digamma) * 200F)) / 10F;
        LegacyTooltips.line(tooltip, ChatFormatting.RED, "[" + LegacyTooltips.resolve("trait.atomiclegacy.digamma") + "]");
        LegacyTooltips.line(tooltip, ChatFormatting.DARK_RED, "" + d + "mDRX/s");
        LegacyTooltips.dropTrait(tooltip);
    }
}

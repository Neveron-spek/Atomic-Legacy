package com.atomiclegacy.items;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.radiation.RadiationEvents;
import com.atomiclegacy.registry.HbmMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Shared 1.7.10 helpers of the medicine and food items (VersatileConfig,
 * ItemSimpleConsumable, ContaminationUtil and the lang-driven lore).
 */
public final class LegacyConsumableSupport {
    private LegacyConsumableSupport() {
    }

    /** PotionConfig.potionSickness: 0 = off (default), 1 = NORMAL, 2 = TERRARIA. */
    public static int potionSicknessMode() {
        String value;
        try {
            value = HbmConfig.POTION_SICKNESS.get();
        } catch (IllegalStateException notLoaded) {
            return 0;
        }
        if ("normal".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("terraria".equalsIgnoreCase(value)) {
            return 2;
        }
        return 0;
    }

    /** VersatileConfig.hasPotionSickness. */
    public static boolean hasPotionSickness(LivingEntity entity) {
        return entity.hasEffect(HbmMobEffects.POTION_SICKNESS);
    }

    /** VersatileConfig.applyPotionSickness: nothing when off, x12 in TERRARIA mode, no curative items. */
    public static void applyPotionSickness(LivingEntity entity, int seconds) {
        int mode = potionSicknessMode();
        if (mode == 0) {
            return;
        }
        if (mode == 2) {
            seconds *= 12;
        }
        addIncurable(entity, HbmMobEffects.POTION_SICKNESS, seconds * 20);
    }

    /** PotionEffect with setCurativeItems(new ArrayList()): milk does not remove it. */
    public static void addIncurable(LivingEntity entity, Holder<MobEffect> effect, int ticks) {
        MobEffectInstance instance = new MobEffectInstance(effect, ticks);
        instance.getCures().clear();
        entity.addEffect(instance);
    }

    /** ItemSimpleConsumable.addPotionEffect: an active effect is extended by the new duration. */
    public static void addStackingEffect(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance active = entity.getEffect(effect);
        if (active == null) {
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
        } else {
            int total = active.getDuration() + duration;
            // 1.7.10 PotionEffect.combine replaced the duration of an equal-amplifier
            // effect only when it grew; the modern update() does the same.
            entity.addEffect(new MobEffectInstance(effect, total, amplifier));
        }
    }

    /**
     * ContaminationUtil.contaminate(entity, RADIATION, RAD_BYPASS, amount):
     * always feeds the environment readout, then skips creative players,
     * players younger than 200 ticks and rad-immune entities.
     */
    public static boolean contaminateRadBypass(LivingEntity entity, float amount) {
        // [parity K2: consumables] delegated to the shared ContaminationUtil.contaminate
        return com.atomiclegacy.util.ContaminationUtil.contaminate(entity,
                com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                com.atomiclegacy.util.ContaminationUtil.ContaminationType.RAD_BYPASS, amount);
    }

    /** HbmLivingProps.incrementRadiation. */
    public static void incrementRadiation(LivingEntity entity, float amount) {
        HbmLivingRadiation data = HbmLivingRadiation.get(entity);
        data.addRadiation(amount);
        HbmLivingRadiation.set(entity, data);
    }

    /** player.inventory.addItemStackToInventory, dropping the stack when the inventory is full. */
    public static void giveOrDrop(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** A bare player.inventory.addItemStackToInventory: the stack is lost when the inventory is full. */
    public static void giveNoDrop(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            player.getInventory().add(stack);
        }
    }

    /** I18nUtil lore: the translated "<key>" split at '$', nothing when the key is missing. */
    public static void loreLines(List<Component> tooltip, String key) {
        String text = Component.translatable(key).getString();
        if (!text.equals(key)) {
            for (String line : text.split("\\$")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** list.add("literal"): 1.7.10 drew untranslated lore lines in gray. */
    public static void literal(List<Component> tooltip, String line) {
        tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
}

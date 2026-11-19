package com.atomiclegacy.items;

import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct port of ItemModMedal's ten RAD-per-second body-radiation reduction. */
public final class LegacyMedalArmorModItem extends ArmorModItem {
    public LegacyMedalArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, true, true, false, false);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
        radiation.setRadiation(Math.max(radiation.getRadiation() - 0.5F, 0F)); // [parity I2] Math.max(rad, 0)
    }

    // [parity I2: tooltip] ItemModMedal (no blank line before the common block)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.GOLD, "-10 RAD/s"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.GOLD, mod, " (-10 RAD/s)"));
    }
}

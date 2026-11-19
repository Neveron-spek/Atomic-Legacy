package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModWD40: +4 maximum health and an 80% chance to undo one armor-damage point. */
public final class LegacyWd40ArmorModItem extends ArmorModItem {
    public LegacyWd40ArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    public double extraHealth() {
        return 4.0D;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        if (armor.isDamageableItem() && armor.getDamageValue() > 0 && player.getRandom().nextInt(5) != 0) {
            armor.setDamageValue(armor.getDamageValue() - 1);
        }
        return amount;
    }

    // [parity I2: tooltip] ItemModWD40 (blinking BLUE/YELLOW)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(blink(ChatFormatting.BLUE, ChatFormatting.YELLOW), "Highly reduces damage taken by armor, +2 HP"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(blink(ChatFormatting.BLUE, ChatFormatting.YELLOW), mod, " (-80% armor wear / +2 HP)"));
    }
}

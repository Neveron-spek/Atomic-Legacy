package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModShield, installed in the chestplate insert slot. */
public final class LegacyShieldArmorModItem extends ArmorModItem {
    private final float shield;

    public LegacyShieldArmorModItem(Item.Properties properties, float shield) {
        super(properties, ArmorModHandler.KEVLAR, false, true, false, false);
        this.shield = shield;
    }

    public float shield() {
        return this.shield;
    }

    // [parity I2: tooltip] ItemModShield (blinking YELLOW/GOLD)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(blink(ChatFormatting.YELLOW, ChatFormatting.GOLD), "+" + (Math.round(this.shield * 10) * 0.1) + " shield"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(blink(ChatFormatting.YELLOW, ChatFormatting.GOLD), mod, " (+" + (Math.round(this.shield * 10) * 0.1) + " health)"));
    }
}

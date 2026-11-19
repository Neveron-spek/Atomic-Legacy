package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModHealth, installed in the extra armor-modification slot. */
public final class HealthArmorModItem extends ArmorModItem {
    private final double health;
    private final boolean nostalgia;

    public HealthArmorModItem(Properties properties, double health, boolean nostalgia) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false); // [parity I2] chestplates only
        this.health = health;
        this.nostalgia = nostalgia;
    }

    public double health() {
        return this.health;
    }

    @Override
    public double extraHealth() {
        return this.health;
    }

    // [parity I2: tooltip] ItemModHealth (blinking RED/LIGHT_PURPLE)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(blink(ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE), "+" + (Math.round(this.health * 10) * 0.1) + " health"));
        tooltip.add(Component.empty());
        if (this.nostalgia) {
            tooltip.add(line(ChatFormatting.DARK_GRAY, "Nostalgia"));
            tooltip.add(Component.empty());
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(blink(ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE), mod, " (+" + (Math.round(this.health * 10) * 0.1) + " health)"));
    }
}

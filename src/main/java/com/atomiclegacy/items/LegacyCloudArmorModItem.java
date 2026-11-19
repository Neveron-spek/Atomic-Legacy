package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModCloud: chestplate-only dash module with a 12.5% speed multiplier. */
public final class LegacyCloudArmorModItem extends ArmorModItem {
    public LegacyCloudArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
    }

    @Override
    public double movementMultiplier() {
        return 1.125D;
    }

    @Override
    public int extraDashes() {
        return 3;
    }

    // [parity I2: tooltip] ItemModCloud
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.WHITE, "Grants horizontal dashes"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.RED, mod, " (Dashes)"));
    }
}

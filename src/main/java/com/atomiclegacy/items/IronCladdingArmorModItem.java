package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 iron cladding: +0.5 knockback resistance on the fitted armor piece. */
public final class IronCladdingArmorModItem extends ArmorModItem {
    public IronCladdingArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.CLADDING, true, true, true, true);
    }

    @Override
    public double knockbackResistance(ItemStack armor) {
        return 0.5D;
    }

    // [parity I2: tooltip] ItemModIron
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.WHITE, "+0.5 knockback resistance"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.WHITE, mod, " (+0.5 knockback resistence)"));
    }
}

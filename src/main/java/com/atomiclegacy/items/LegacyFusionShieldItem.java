package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * [parity b1: fusion shield] 1.7.10 ItemFusionShield: a relic of the old fusion reactor that nothing consumes any
 * more. Keeps the long "damage" counter, the two hardcoded English tooltip lines and the durability bar.
 */
public final class LegacyFusionShieldItem extends Item {
    public final long maxDamage;
    public final int maxTemp;

    public LegacyFusionShieldItem(Properties properties, long maxDamage, int maxTemp) {
        super(properties.stacksTo(1));
        this.maxDamage = maxDamage;
        this.maxTemp = maxTemp;
    }

    public static long getShieldDamage(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("damage");
    }

    public static void setShieldDamage(ItemStack stack, long damage) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong("damage", damage));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        long damage = getShieldDamage(stack);
        int percent = (int) ((maxDamage - damage) * 100 / maxDamage);
        list.add(Component.literal("Durability: " + (maxDamage - damage) + "/" + maxDamage + " (" + percent + "%)").withStyle(ChatFormatting.GRAY));
        list.add(Component.literal("Melting point: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(maxTemp + "\u00B0C").withStyle(ChatFormatting.RED)));
    }

    private double durabilityForDisplay(ItemStack stack) {
        return (double) getShieldDamage(stack) / (double) maxDamage;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return durabilityForDisplay(stack) != 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float) durabilityForDisplay(stack) * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) (1.0D - durabilityForDisplay(stack))) / 3.0F, 1.0F, 1.0F);
    }
}

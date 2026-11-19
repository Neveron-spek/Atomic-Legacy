package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class InfiniteBatteryItem extends Item {
    public static final long POWER = Long.MAX_VALUE;
    public static final long TRANSFER_RATE = 1_000_000_000_000L;

    public InfiniteBatteryItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55FFFF;
    }

    // [parity L1: batteries] ItemBatteryCreative has no addInformation in 1.7.10.
}

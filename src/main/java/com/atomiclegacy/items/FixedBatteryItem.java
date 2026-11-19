package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** 1.7.10 ItemBattery for fixed-capacity legacy batteries such as Memory. */
public class FixedBatteryItem extends Item implements HbmChargeableItem {
    private static final String CHARGE_KEY = "charge";

    private final long maxCharge;
    private final long chargeRate;
    private final long dischargeRate;

    public FixedBatteryItem(Properties properties, long maxCharge, long chargeRate, long dischargeRate) {
        super(properties.stacksTo(1));
        this.maxCharge = Math.max(1L, maxCharge);
        this.chargeRate = Math.max(0L, chargeRate);
        this.dischargeRate = Math.max(0L, dischargeRate);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * hbmCharge(stack) / (float) this.maxCharge), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity L1: batteries] ItemBattery.addInformation
        LegacyBatteryTooltips.itemBattery(tooltip, hbmCharge(stack), this.maxCharge, this.chargeRate, this.dischargeRate);
    }

    @Override
    public long hbmCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(CHARGE_KEY) ? Mth.clamp(tag.getLong(CHARGE_KEY), 0L, this.maxCharge) : this.maxCharge;
    }

    @Override
    public long hbmCapacity(ItemStack stack) {
        return this.maxCharge;
    }

    @Override
    public long hbmChargeRate(ItemStack stack) {
        return this.chargeRate;
    }

    @Override
    public long hbmDischargeRate(ItemStack stack) {
        return this.dischargeRate;
    }

    @Override
    public void hbmSetCharge(ItemStack stack, long charge) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(CHARGE_KEY, Mth.clamp(charge, 0L, this.maxCharge));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}

package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * [parity L1: batteries] Literal 1.7.10 battery tooltips (the original strings are
 * hard-coded English, not lang keys) with BobMathUtil.getShortNumber formatting.
 */
public final class LegacyBatteryTooltips {
    private LegacyBatteryTooltips() {
    }

    /** BobMathUtil.getShortNumber (server-safe copy of K3GuiText.shortNumber). */
    public static String shortNumber(long l) {
        double res;
        String suffix;
        long abs = Math.abs(l);
        if (abs >= Math.pow(10, 18)) { res = l / Math.pow(10, 18); suffix = "E"; }
        else if (abs >= Math.pow(10, 15)) { res = l / Math.pow(10, 15); suffix = "P"; }
        else if (abs >= Math.pow(10, 12)) { res = l / Math.pow(10, 12); suffix = "T"; }
        else if (abs >= Math.pow(10, 9)) { res = l / Math.pow(10, 9); suffix = "G"; }
        else if (abs >= Math.pow(10, 6)) { res = l / Math.pow(10, 6); suffix = "M"; }
        else if (abs >= Math.pow(10, 3)) { res = l / Math.pow(10, 3); suffix = "k"; }
        else { return Long.toString(l); }
        res = res <= -100.0 ? Math.round(res * 10.0) / 10.0 : Math.round(res * 100.0) / 100.0;
        return res + suffix;
    }

    /** ItemBatteryPack.addInformation */
    public static void batteryPack(List<Component> list, long charge, long maxCharge, long chargeRate, long dischargeRate) {
        list.add(Component.literal("Energy stored: " + shortNumber(charge) + "/" + shortNumber(maxCharge)
                + "HE (" + (charge * 1000 / maxCharge / 10D) + "%)").withStyle(ChatFormatting.GREEN));
        list.add(Component.literal("Charge rate: " + shortNumber(chargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Discharge rate: " + shortNumber(dischargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Time for full charge: " + (maxCharge / chargeRate / 20 / 60D) + "min").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Charge lasts for: " + (maxCharge / dischargeRate / 20 / 60D) + "min").withStyle(ChatFormatting.GOLD));
    }

    /** ItemBattery.addInformation (no colours, no percentage). */
    public static void itemBattery(List<Component> list, long charge, long maxCharge, long chargeRate, long dischargeRate) {
        list.add(Component.literal("Energy stored: " + shortNumber(charge) + "/" + shortNumber(maxCharge) + "HE"));
        list.add(Component.literal("Charge rate: " + shortNumber(chargeRate) + "HE/t"));
        list.add(Component.literal("Discharge rate: " + shortNumber(dischargeRate) + "HE/t"));
    }

    /** ItemBatterySC.addInformation */
    public static void selfCharging(List<Component> list, long power) {
        if (power > 0) {
            list.add(Component.literal("Discharge rate: " + shortNumber(power) + "HE/t").withStyle(ChatFormatting.YELLOW));
        }
        for (String line : Component.translatable("item.atomiclegacy.battery_sc.desc").getString().split("\\$")) {
            list.add(Component.literal(line).withStyle(ChatFormatting.RED));
        }
    }
}

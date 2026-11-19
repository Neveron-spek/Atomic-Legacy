package com.atomiclegacy.util;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTraitInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HbmFluidTooltip {
    private HbmFluidTooltip() {
    }

    // [parity L2: fluid tooltip] FluidTank.renderTankInfo + FluidType.addInfo of 1.7.10 (936c60c)
    public static List<Component> forTank(HbmFluidDefinition fluid, int amount, int capacity) {
        return forTank(fluid, amount, capacity, 0);
    }

    public static List<Component> forTank(HbmFluidDefinition fluid, int amount, int capacity, int pressure) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(fluid.translationKey()));
        lines.add(Component.literal(amount + "/" + capacity + "mB"));
        if (pressure != 0) {
            lines.add(Component.literal("Pressure: " + pressure + " PU").withStyle(ChatFormatting.RED));
            boolean blink = System.currentTimeMillis() % 1000 < 500;
            lines.add(Component.literal("Pressurized, use compressor!").withStyle(blink ? ChatFormatting.RED : ChatFormatting.DARK_RED));
        }
        appendTraitInfo(lines, fluid, false);
        return lines;
    }

    /** FluidType.addInfo: the {@code advanced} flag is ignored, hidden lines follow LSHIFT exactly like 1.7.10. */
    public static void appendTraitInfo(List<Component> tooltip, HbmFluidDefinition fluid, boolean advanced) {
        if (fluid == null) {
            return;
        }
        HbmFluidTraitInfo.addInfo(tooltip, fluid, shiftHeld());
    }

    private static boolean shiftHeld() {
        try {
            return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && ClientShift.held();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ClientShift {
        static boolean held() {
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                    net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(),
                    org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT);
        }
    }

    public static String shortNumber(long number) {
        if (number < 1_000L) {
            return Long.toString(number);
        }
        if (number < 1_000_000L) {
            return String.format(Locale.ROOT, "%.2fk", number / 1_000.0D);
        }
        if (number < 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fM", number / 1_000_000.0D);
        }
        if (number < 1_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fG", number / 1_000_000_000.0D);
        }
        if (number < 1_000_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fT", number / 1_000_000_000_000.0D);
        }
        if (number < 1_000_000_000_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fE", number / 1_000_000_000_000_000.0D);
        }
        return "INFINITE";
    }
}

package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class PwrFuelItem extends LegacyVariantItem {
    private static final String LIFE_TAG = "life";

    // [parity M2: PWR] ItemPWRFuel.EnumPWRFuel. The three-argument constructor of the original never assigns its
    // yield argument, so every fuel (BFB rods included) keeps the default yield of 1,000,000,000.
    public static final List<Fuel> FUELS = List.of(
            new Fuel("meu", 5.0D, FunctionType.LOG_600_DIV_2500, 1_000_000_000D),
            new Fuel("heu233", 7.5D, FunctionType.SQRT_25, 1_000_000_000D),
            new Fuel("heu235", 7.5D, FunctionType.SQRT_22_5, 1_000_000_000D),
            new Fuel("men", 7.5D, FunctionType.LOG_675_DIV_2500, 1_000_000_000D),
            new Fuel("hen237", 7.5D, FunctionType.SQRT_27_5, 1_000_000_000D),
            new Fuel("mox", 7.5D, FunctionType.LOG_600_DIV_2500, 1_000_000_000D),
            new Fuel("mep", 7.5D, FunctionType.LOG_675_DIV_2500, 1_000_000_000D),
            new Fuel("hep239", 10.0D, FunctionType.SQRT_22_5, 1_000_000_000D),
            new Fuel("hep241", 10.0D, FunctionType.SQRT_25, 1_000_000_000D),
            new Fuel("mea", 7.5D, FunctionType.LOG_750_DIV_2500, 1_000_000_000D),
            new Fuel("hea242", 10.0D, FunctionType.SQRT_25, 1_000_000_000D),
            new Fuel("hes326", 12.5D, FunctionType.SQRT_27_5, 1_000_000_000D),
            new Fuel("hes327", 12.5D, FunctionType.SQRT_30, 1_000_000_000D),
            new Fuel("bfb_am_mix", 2.5D, FunctionType.SQRT_15, 1_000_000_000D),
            new Fuel("bfb_pu241", 2.5D, FunctionType.SQRT_15, 1_000_000_000D)
    );

    private final String baseId;

    public PwrFuelItem(Properties properties) {
        this(properties, "pwr_fuel");
    }

    public PwrFuelItem(Properties properties, String baseId) {
        // [parity M2: PWR] ItemEnumMulti keeps the default stack size of 64
        super(properties, baseId, LegacyVariantItem.variants(FUELS.stream().map(Fuel::id).toArray(String[]::new)));
        this.baseId = baseId;
    }

    public Fuel fuel(ItemStack stack) {
        String id = variant(stack).id();
        for (Fuel fuel : FUELS) {
            if (fuel.id().equals(id)) {
                return fuel;
            }
        }
        return FUELS.getFirst();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity M2: PWR] only ItemPWRFuel (pwr_fuel) adds these lines; pwr_fuel_hot/depleted are plain ItemEnumMulti
        if ("pwr_fuel".equals(baseId)) {
            Fuel fuel = fuel(stack);
            tooltip.add(Component.translatable("tooltip.atomiclegacy.pwr_fuel.heat").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(fuel.heatEmission() + " TU").withStyle(ChatFormatting.RESET)));
            tooltip.add(Component.translatable("tooltip.atomiclegacy.pwr_fuel.function").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(fuel.function().label()).withStyle(ChatFormatting.RESET)));
            tooltip.add(Component.translatable("tooltip.atomiclegacy.pwr_fuel.danger").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(fuel.function().danger()).withStyle(ChatFormatting.YELLOW)));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static int fuelIndex(ItemStack stack) {
        if (!(stack.getItem() instanceof PwrFuelItem item)) {
            return -1;
        }
        String id = item.variant(stack).id();
        for (int index = 0; index < FUELS.size(); index++) {
            if (FUELS.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    public static ItemStack stackForFuel(net.minecraft.world.level.ItemLike item, int fuelIndex) {
        return LegacyVariantItem.stackFor(item.asItem(), FUELS.get(Math.max(0, Math.min(FUELS.size() - 1, fuelIndex))).id());
    }

    public record Fuel(String id, double heatEmission, FunctionType function, double yield) {
    }

    /** [parity M2: PWR] com.hbm.util.function.Function: FunctionLogarithmic(level).withDiv(2500) and FunctionSqrt(level). */
    public enum FunctionType {
        LOG_600_DIV_2500(true, 20 * 30, 2_500D),
        LOG_675_DIV_2500(true, 22.5D * 30, 2_500D),
        LOG_750_DIV_2500(true, 25 * 30, 2_500D),
        SQRT_15(false, 15D, 1D),
        SQRT_22_5(false, 22.5D, 1D),
        SQRT_25(false, 25D, 1D),
        SQRT_27_5(false, 27.5D, 1D),
        SQRT_30(false, 30D, 1D);

        private final boolean logarithmic;
        private final double level;
        private final double div;
        private final double off;

        FunctionType(boolean logarithmic, double level, double div) {
            this.logarithmic = logarithmic;
            this.level = level;
            this.div = div;
            this.off = logarithmic ? 1D : 0D;
        }

        private double x(double x) {
            return x / div + off;
        }

        public double output(double flux) {
            if (logarithmic) return Math.log10(x(flux)) * level;
            return squirt(x(flux)) * level;
        }

        private String xName() {
            String x = "x";
            if (div != 1D) x += " / " + String.format(java.util.Locale.US, "%,.1f", div);
            if (off != 0D) x += " + " + String.format(java.util.Locale.US, "%,.1f", off);
            return x;
        }

        public String label() {
            if (logarithmic) return "log10(" + xName() + ") * " + String.format(java.util.Locale.US, "%,.1f", level);
            return "sqrt(" + xName() + ") * " + String.format(java.util.Locale.US, "%,.3f", level);
        }

        public String danger() {
            return logarithmic ? "MEDIUM / LOGARITHMIC" : "MEDIUM / SQUARE ROOT";
        }

        /** BobMathUtil.squirt */
        public static double squirt(double x) {
            return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
        }
    }
}

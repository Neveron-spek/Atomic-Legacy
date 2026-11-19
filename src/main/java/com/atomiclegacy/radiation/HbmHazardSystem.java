package com.atomiclegacy.radiation;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * [parity K1: HazardSystem] Item hazards of 1.7.10 HazardSystem: the entries of {@link HbmHazardRegistry} (the explicit
 * HazardRegistry/OreDictManager table with modifiers and transformers) summed per hazard type, and the tooltip of
 * HazardTypeBase.addHazardInformation. The former port values derived from item ids, the foundry-material and
 * fluid-container radiation and the toxic levels were not part of the 1.7.10 hazard system and are gone.
 */
public final class HbmHazardSystem {

    private HbmHazardSystem() {
    }

    public static HbmHazardData hazards(ItemStack stack) {
        return hazardsPerItem(stack).multiply(stack.getCount());
    }

    /** Levels of one item after HazardModifier.evalAllModifiers (holder null), summed per type. */
    public static HbmHazardData hazardsPerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return HbmHazardData.EMPTY;
        }
        List<HbmHazardRegistry.Entry> entries = HbmHazardRegistry.entries(stack);
        if (entries.isEmpty()) {
            return HbmHazardData.EMPTY;
        }
        double[] sum = new double[HbmHazardRegistry.Type.values().length];
        for (HbmHazardRegistry.Entry entry : entries) {
            sum[entry.type().ordinal()] += entry.eval(stack, null);
        }
        return new HbmHazardData(
                sum[HbmHazardRegistry.Type.RADIATION.ordinal()],
                0.0D,
                sum[HbmHazardRegistry.Type.DIGAMMA.ordinal()],
                sum[HbmHazardRegistry.Type.HOT.ordinal()],
                sum[HbmHazardRegistry.Type.BLINDING.ordinal()],
                sum[HbmHazardRegistry.Type.ASBESTOS.ordinal()],
                sum[HbmHazardRegistry.Type.COAL.ordinal()],
                sum[HbmHazardRegistry.Type.HYDROACTIVE.ordinal()],
                sum[HbmHazardRegistry.Type.EXPLOSIVE.ordinal()]
        );
    }

    public static double rawRadiation(ItemStack stack) {
        return hazardsPerItem(stack).radiation();
    }

    /** RADIATION level registered for a port id (BlockHazard: getHazardLevelFromStack(new ItemStack(block))). */
    public static double rawRadiationForId(String id) {
        return HbmHazardRegistry.baseLevelForId(id, HbmHazardRegistry.Type.RADIATION);
    }

    /** HazardTypeAsbestos level of one item. */
    public static double asbestosLevel(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0.0D : HbmHazardRegistry.level(stack, HbmHazardRegistry.Type.ASBESTOS);
    }

    /** HazardTypeCoal level of one item. */
    public static double coalDustLevel(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0.0D : HbmHazardRegistry.level(stack, HbmHazardRegistry.Type.COAL);
    }

    /** The toxic levels of the base were not registered by the 1.7.10 HazardRegistry; kept for the callers. */
    public static int toxicLevel(ItemStack stack) {
        return 0;
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        appendTooltip(stack, tooltip, null, null);
    }

    /** HazardSystem.addFullTooltip: every entry in order, as HazardType*.addHazardInformation. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip, Player player, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int count = stack.getCount();
        for (HbmHazardRegistry.Entry entry : HbmHazardRegistry.entries(stack, registries)) {
            switch (entry.type()) {
                case RADIATION -> {
                    float level = entry.eval(stack, player);
                    if (level < 1e-5) {
                        break;
                    }
                    tooltip.add(trait("radioactive", ChatFormatting.GREEN));
                    String rad = "" + (Math.floor(level * 1000) / 1000);
                    tooltip.add(Component.literal(rad + "RAD/s").withStyle(ChatFormatting.YELLOW));
                    if (count > 1) {
                        tooltip.add(Component.literal("Stack: " + ((Math.floor(level * 1000 * count) / 1000) + "RAD/s"))
                                .withStyle(ChatFormatting.YELLOW));
                    }
                }
                case DIGAMMA -> {
                    float level = entry.eval(stack, player);
                    float d = (float) (Math.floor(level * 10000F)) / 10F;
                    tooltip.add(trait("digamma", ChatFormatting.RED));
                    tooltip.add(Component.literal("" + d + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
                    if (count > 1) {
                        tooltip.add(Component.literal("Stack: " + ((Math.floor(level * 10000F * count) / 10F) + "mDRX/s"))
                                .withStyle(ChatFormatting.DARK_RED));
                    }
                }
                case HOT -> {
                    if (entry.eval(stack, player) > 0) {
                        tooltip.add(trait("hot", ChatFormatting.GOLD));
                    }
                }
                case BLINDING -> tooltip.add(trait("blinding", ChatFormatting.DARK_AQUA));
                case ASBESTOS -> tooltip.add(trait("asbestos", ChatFormatting.WHITE));
                case COAL -> tooltip.add(trait("coal", ChatFormatting.DARK_GRAY));
                case HYDROACTIVE -> tooltip.add(trait("hydro", ChatFormatting.RED));
                case EXPLOSIVE -> tooltip.add(trait("explosive", ChatFormatting.RED));
            }
        }
    }

    private static Component trait(String key, ChatFormatting color) {
        return Component.literal("[").append(Component.translatable("trait.atomiclegacy." + key)).append("]").withStyle(color);
    }

    /** RADIATION trait value of a fluid (FT_VentRadiation), used by the release/venting code, not by items. */
    public static double fluidRadiation(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return 0.0D;
        }
        for (String token : fluid.rawTraits().split("\\|")) {
            String clean = token.trim();
            if (clean.startsWith("RADIATION:")) {
                String value = clean.substring("RADIATION:".length());
                int nextColon = value.indexOf(':');
                if (nextColon >= 0) {
                    value = value.substring(0, nextColon);
                }
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    return 0.0D;
                }
            }
        }
        return 0.0D;
    }
}

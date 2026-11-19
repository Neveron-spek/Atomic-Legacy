package com.atomiclegacy.fluid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [parity L2: fluid tooltip] Line-by-line port of FluidType.addInfo and every FluidTrait.addInfo/addInfoHidden
 * of HbmMods 1.7.10 (936c60c), in FluidTrait.traitList order.
 */
public final class HbmFluidTraitInfo {
    private static final String[] POLLUTION_NAMES = {"soot", "poison", "heavymetal", "fallout"};
    private static final String[] HEATING_NAMES = {"boilable", "heatable", "coolantPWR", "coolantICF", "coolantPA"};
    private static final String[] COOLING_NAMES = {"steam", "coolable"};

    private HbmFluidTraitInfo() {
    }

    public static void addInfo(List<Component> info, HbmFluidDefinition fluid, boolean shiftHeld) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        int temperature = fluid.temperatureCelsius();
        if (temperature != 20) {
            if (temperature < 0) info.add(Component.literal(temperature + "°C").withStyle(ChatFormatting.BLUE));
            if (temperature > 0) info.add(Component.literal(temperature + "°C").withStyle(ChatFormatting.RED));
        }

        List<Component> hidden = new ArrayList<>();
        List<Component> scratch = new ArrayList<>();

        // corrosive
        int corrosive = intParam(fluid, "CORROSIVE", 1, -1);
        if (corrosive >= 0) {
            if (corrosive > 50) info.add(bracket(ChatFormatting.GOLD, "hbmfluid.trait.corrosiveStrong"));
            else info.add(bracket(ChatFormatting.YELLOW, "hbmfluid.trait.corrosive"));
        }
        // flammable
        if (fluid.hasTrait(HbmFluidTrait.FLAMMABLE)) {
            info.add(bracket(ChatFormatting.YELLOW, "hbmfluid.trait.flammable"));
            long energy = fluid.flammableHeatEnergy();
            if (energy > 0) {
                info.add(line(tr("hbmfluid.trait.provides", ChatFormatting.YELLOW), lit(" ", ChatFormatting.YELLOW),
                        lit(shortNumber(energy) + "TU ", ChatFormatting.RED), tr("hbmfluid.trait.perBucket", ChatFormatting.YELLOW)));
            }
        }
        // combustible
        if (fluid.hasTrait(HbmFluidTrait.COMBUSTIBLE) && fluid.combustibleFuelGrade() != null) {
            info.add(bracket(ChatFormatting.GOLD, "hbmfluid.trait.combustible"));
            long energy = fluid.combustibleHeatEnergy();
            if (energy > 0) {
                info.add(line(tr("hbmfluid.trait.provides", ChatFormatting.GOLD), lit(" ", ChatFormatting.GOLD),
                        lit(shortNumber(energy) + "HE ", ChatFormatting.RED), tr("hbmfluid.trait.perBucket", ChatFormatting.GOLD)));
                info.add(line(tr("hbmfluid.trait.fuelGrade", ChatFormatting.GOLD), lit(": ", ChatFormatting.GOLD),
                        tr("hbmfluid.trait.fuel." + gradeName(fluid.combustibleFuelGrade()), ChatFormatting.RED)));
            }
        }
        // polluting
        HbmFluidTraitData.Polluting polluting = HbmFluidTraitData.polluting(fluid);
        if (polluting != null) {
            info.add(bracket(ChatFormatting.GOLD, "hbmfluid.trait.polluting"));
            scratch.clear();
            if (any(polluting.release())) {
                scratch.add(line(tr("hbmfluid.trait.spilled", ChatFormatting.GREEN), lit(":", ChatFormatting.GREEN)));
                pollutionLines(scratch, polluting.release(), ChatFormatting.GREEN);
            }
            if (any(polluting.burn())) {
                scratch.add(line(tr("hbmfluid.trait.burned", ChatFormatting.RED), lit(":", ChatFormatting.RED)));
                pollutionLines(scratch, polluting.burn(), ChatFormatting.RED);
            }
            hidden(info, hidden, scratch, shiftHeld);
        }
        // heatable
        HbmFluidTraitData.Heatable heatable = HbmFluidTraitData.heatable(fluid);
        if (heatable != null) {
            scratch.clear();
            scratch.add(line(tr("hbmfluid.trait.thermalCapacity", ChatFormatting.RED), lit(": " + heatable.heatReq() + " ", ChatFormatting.RED),
                    tr("hbmfluid.trait.perTU", ChatFormatting.RED), lit(" " + heatable.amountReq() + "mB", ChatFormatting.RED)));
            efficiencyLines(scratch, heatable.efficiency(), HEATING_NAMES);
            hidden(info, hidden, scratch, shiftHeld);
        }
        // coolable
        HbmFluidTraitData.Coolable coolable = HbmFluidTraitData.coolable(fluid);
        if (coolable != null) {
            scratch.clear();
            scratch.add(line(tr("hbmfluid.trait.thermalCapacity", ChatFormatting.RED), lit(": " + coolable.heatEnergy() + " ", ChatFormatting.RED),
                    tr("hbmfluid.trait.perTU", ChatFormatting.RED), lit(" " + coolable.amountReq() + "mB", ChatFormatting.RED)));
            efficiencyLines(scratch, coolable.efficiency(), COOLING_NAMES);
            hidden(info, hidden, scratch, shiftHeld);
        }
        // pwrmoderator
        Double moderator = HbmFluidTraitData.pwrModerator(fluid);
        if (moderator != null) {
            info.add(bracket(ChatFormatting.BLUE, "hbmfluid.trait.pwrFluxMultiplier"));
            int mult = (int) (moderator * 100 - 100);
            scratch.clear();
            scratch.add(line(tr("hbmfluid.trait.pwrFluxCore", ChatFormatting.BLUE), lit(" " + (mult >= 0 ? "+" : "") + mult + "%", ChatFormatting.BLUE)));
            hidden(info, hidden, scratch, shiftHeld);
        }
        // poison
        if (fluid.hasTrait(HbmFluidTrait.POISON)) {
            scratch.clear();
            scratch.add(lit("[Toxic Fumes]", ChatFormatting.GREEN));
            hidden(info, hidden, scratch, shiftHeld);
        }
        // toxin
        List<HbmFluidTraitData.ToxinEntry> toxins = HbmFluidTraitData.toxin(fluid);
        if (toxins != null) {
            scratch.clear();
            scratch.add(bracket(ChatFormatting.LIGHT_PURPLE, "hbmfluid.trait.toxin"));
            for (HbmFluidTraitData.ToxinEntry entry : toxins) toxinLines(scratch, entry);
            hidden(info, hidden, scratch, shiftHeld);
        }
        // ventradiation
        if (fluid.hasTrait(HbmFluidTrait.RADIATING)) {
            info.add(bracket(ChatFormatting.YELLOW, "hbmfluid.trait.radioactive"));
        }
        // pheromone
        Integer pheromone = HbmFluidTraitData.pheromone(fluid);
        if (pheromone != null) {
            if (pheromone == 1) info.add(bracket(ChatFormatting.AQUA, "hbmfluid.trait.glyphidPheromones"));
            else info.add(bracket(ChatFormatting.BLUE, "hbmfluid.trait.modifiedPheromones"));
        }
        simpleHidden(info, hidden, fluid, HbmFluidTrait.GASEOUS, ChatFormatting.BLUE, "hbmfluid.trait.gaseous", shiftHeld);
        simpleHidden(info, hidden, fluid, HbmFluidTrait.EVAPORATES, ChatFormatting.BLUE, "hbmfluid.trait.gaseousRoom", shiftHeld);
        simpleHidden(info, hidden, fluid, HbmFluidTrait.LIQUID, ChatFormatting.BLUE, "hbmfluid.trait.liquid", shiftHeld);
        simpleHidden(info, hidden, fluid, HbmFluidTrait.VISCOUS, ChatFormatting.BLUE, "hbmfluid.trait.viscous", shiftHeld);
        if (fluid.hasTrait(HbmFluidTrait.PLASMA)) {
            scratch.clear();
            scratch.add(lit("[Plasma]", ChatFormatting.LIGHT_PURPLE));
            hidden(info, hidden, scratch, shiftHeld);
        }
        if (fluid.hasTrait(HbmFluidTrait.ANTIMATTER)) info.add(bracket(ChatFormatting.DARK_RED, "hbmfluid.trait.antimatter"));
        if (fluid.hasTrait(HbmFluidTrait.LEAD_CONTAINER)) info.add(bracket(ChatFormatting.DARK_RED, "hbmfluid.trait.leadContainer"));
        simpleHidden(info, hidden, fluid, HbmFluidTrait.DELICIOUS, ChatFormatting.DARK_GREEN, "hbmfluid.trait.delicious", shiftHeld);
        simpleHidden(info, hidden, fluid, HbmFluidTrait.UNSIPHONABLE, ChatFormatting.BLUE, "hbmfluid.trait.unsiphonable", shiftHeld);

        if (!hidden.isEmpty() && !shiftHeld) {
            info.add(line(lit("Hold <", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), lit("LSHIFT", ChatFormatting.YELLOW, ChatFormatting.ITALIC),
                    lit("> to display more info", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }
    }

    /** Parses a numeric argument of a raw port trait token (e.g. CORROSIVE:60, RADIATION:0.2, POISON:true:2). */
    public static int intParam(HbmFluidDefinition fluid, String key, int index, int fallback) {
        String[] parts = token(fluid, key);
        if (parts == null || parts.length <= index) return fallback;
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static float floatParam(HbmFluidDefinition fluid, String key, int index, float fallback) {
        String[] parts = token(fluid, key);
        if (parts == null || parts.length <= index) return fallback;
        try {
            return Float.parseFloat(parts[index].trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String[] token(HbmFluidDefinition fluid, String key) {
        if (fluid == null || fluid.rawTraits().isBlank()) return null;
        for (String token : fluid.rawTraits().split("\\|")) {
            String[] parts = token.trim().split(":");
            if (parts[0].equals(key)) return parts;
        }
        return null;
    }

    /** FT_Corrosive.isHighlyCorrosive: rating > 50. */
    public static boolean isHighlyCorrosive(HbmFluidDefinition fluid) {
        return intParam(fluid, "CORROSIVE", 1, 0) > 50;
    }

    /** FluidType.isHot: temperature >= 100. */
    public static boolean isHot(HbmFluidDefinition fluid) {
        return fluid != null && fluid.temperatureCelsius() >= 100;
    }

    private static String gradeName(CombustibleFuelGrade grade) {
        return switch (grade) {
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            case AERO -> "aviation";
            case GAS -> "gaseous";
        };
    }

    private static void simpleHidden(List<Component> info, List<Component> hidden, HbmFluidDefinition fluid, HbmFluidTrait trait,
                                     ChatFormatting color, String key, boolean shiftHeld) {
        if (!fluid.hasTrait(trait)) return;
        Component lineComponent = bracket(color, key);
        if (shiftHeld) info.add(lineComponent);
        hidden.add(lineComponent);
    }

    private static void hidden(List<Component> info, List<Component> hidden, List<Component> lines, boolean shiftHeld) {
        if (shiftHeld) info.addAll(lines);
        hidden.addAll(lines);
    }

    private static boolean any(float[] values) {
        for (float value : values) if (!Float.isNaN(value)) return true;
        return false;
    }

    private static void pollutionLines(List<Component> out, float[] values, ChatFormatting color) {
        for (int i = 0; i < values.length; i++) {
            if (Float.isNaN(values[i])) continue;
            out.add(line(lit(" - " + values[i] + " ", color), tr("pollution.trait." + POLLUTION_NAMES[i], color), lit(" ", color),
                    tr("hbmfluid.trait.perMB", color)));
        }
    }

    private static void efficiencyLines(List<Component> out, double[] efficiency, String[] names) {
        for (int i = 0; i < efficiency.length; i++) {
            double eff = efficiency[i];
            if (eff > 0) {
                out.add(line(lit("[", ChatFormatting.YELLOW), tr("hbmfluid.trait." + names[i], ChatFormatting.YELLOW), lit("] ", ChatFormatting.YELLOW),
                        tr("hbmfluid.trait.efficiency", ChatFormatting.AQUA), lit(": " + ((int) (eff * 100D)) + "%", ChatFormatting.AQUA)));
            }
        }
    }

    private static void toxinLines(List<Component> out, HbmFluidTraitData.ToxinEntry entry) {
        if (entry.directDamage()) {
            MutableComponent l = line(lit("- ", ChatFormatting.YELLOW), tr(entry.hazardLang(), ChatFormatting.YELLOW));
            ChatFormatting after = ChatFormatting.YELLOW;
            if (entry.fullBody()) {
                l.append(line(lit(" (", ChatFormatting.RED), tr("hbmfluid.trait.hazmat", ChatFormatting.RED), lit(")", ChatFormatting.RED)));
                after = ChatFormatting.RED;
            }
            l.append(lit(": ", after));
            l.append(lit(String.format(Locale.US, "%,.1f", entry.amount() * 20 / entry.delay()) + " ", ChatFormatting.YELLOW));
            l.append(tr("hbmfluid.trait.perDamage", ChatFormatting.YELLOW));
            out.add(l);
        } else {
            MutableComponent l = line(lit("- ", ChatFormatting.YELLOW), tr(entry.hazardLang(), ChatFormatting.YELLOW));
            if (entry.fullBody()) {
                l.append(line(lit(" (", ChatFormatting.RED), tr("hbmfluid.trait.hazmat", ChatFormatting.RED), lit(")", ChatFormatting.RED)));
            }
            l.append(lit(":", ChatFormatting.YELLOW));
            out.add(l);
            for (HbmFluidTraitData.Effect effect : entry.effects()) {
                MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effect.effectId()));
                MutableComponent e = line(lit("   - ", ChatFormatting.YELLOW),
                        mobEffect == null ? lit(effect.effectId(), ChatFormatting.YELLOW) : tr(mobEffect.getDescriptionId(), ChatFormatting.YELLOW));
                if (effect.amplifier() > 0) {
                    e.append(lit(" ", ChatFormatting.YELLOW)).append(tr("potion.potency." + effect.amplifier(), ChatFormatting.YELLOW));
                }
                e.append(lit(" " + ticksToElapsedTime(effect.duration()), ChatFormatting.YELLOW));
                out.add(e);
            }
        }
    }

    /** BobMathUtil.getShortNumber of 1.7.10 (server-safe copy). */
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
        else return Long.toString(l);
        if (res <= -100.0) res = Math.round(res * 10.0) / 10.0;
        else res = Math.round(res * 100.0) / 100.0;
        return res + suffix;
    }

    /** net.minecraft.util.StringUtils.ticksToElapsedTime of 1.7.10. */
    public static String ticksToElapsedTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        return seconds < 10 ? minutes + ":0" + seconds : minutes + ":" + seconds;
    }

    private static MutableComponent bracket(ChatFormatting color, String key) {
        return line(lit("[", color), tr(key, color), lit("]", color));
    }

    private static MutableComponent line(Component... parts) {
        MutableComponent root = Component.empty();
        for (Component part : parts) root.append(part);
        return root;
    }

    private static MutableComponent lit(String text, ChatFormatting... style) {
        return Component.literal(text).withStyle(style);
    }

    private static MutableComponent tr(String key, ChatFormatting... style) {
        return Component.translatable(key).withStyle(style);
    }
}

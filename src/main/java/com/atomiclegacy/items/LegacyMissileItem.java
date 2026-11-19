package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

/**
 * ItemMissile's immutable launch metadata. Launcher code can consume this
 * directly instead of inferring a missile's tier or fuel from its registry id.
 */
public class LegacyMissileItem extends Item {
    private static final Set<String> LEGACY_IDS = Set.of(
            "missile_anti_ballistic", "missile_bhole", "missile_burst", "missile_buster",
            "missile_buster_strong", "missile_cluster", "missile_cluster_strong", "missile_decoy",
            "missile_doomsday", "missile_doomsday_rusted", "missile_drill", "missile_emp",
            "missile_emp_strong", "missile_generic", "missile_incendiary", "missile_incendiary_strong",
            "missile_inferno", "missile_micro", "missile_nuclear", "missile_nuclear_cluster",
            "missile_rain", "missile_schrabidium", "missile_shuttle", "missile_stealth",
            "missile_strong", "missile_taint", "missile_test", "missile_volcano"
    );
    public enum FormFactor {
        ABM(Fuel.SOLID),
        MICRO(Fuel.SOLID),
        V2(Fuel.ETHANOL_PEROXIDE),
        STRONG(Fuel.KEROSENE_PEROXIDE),
        HUGE(Fuel.KEROSENE_LOXY),
        ATLAS(Fuel.JETFUEL_LOXY),
        OTHER(Fuel.KEROSENE_PEROXIDE);

        private final Fuel defaultFuel;

        FormFactor(Fuel defaultFuel) {
            this.defaultFuel = defaultFuel;
        }
    }

    public enum Tier {
        TIER0,
        TIER1,
        TIER2,
        TIER3,
        TIER4
    }

    public enum Fuel {
        SOLID("item.missile.fuel.solid.prefueled", "\u00a76", 0),
        ETHANOL_PEROXIDE("item.missile.fuel.ethanol_peroxide", "\u00a7b", 4_000),
        KEROSENE_PEROXIDE("item.missile.fuel.kerosene_peroxide", "\u00a79", 8_000),
        KEROSENE_LOXY("item.missile.fuel.kerosene_loxy", "\u00a7d", 12_000),
        JETFUEL_LOXY("item.missile.fuel.jetfuel_loxy", "\u00a7c", 16_000);

        private final String translationKey;
        private final String color;
        private final int defaultCapacity;

        Fuel(String translationKey, String color, int defaultCapacity) {
            this.translationKey = translationKey;
            this.color = color;
            this.defaultCapacity = defaultCapacity;
        }
    }

    private final FormFactor formFactor;
    private final Tier tier;
    private final Fuel fuel;
    private final int fuelCapacity;
    private final boolean launchable;

    public LegacyMissileItem(Properties properties, FormFactor formFactor, Tier tier) {
        this(properties, formFactor, tier, formFactor.defaultFuel, formFactor.defaultFuel.defaultCapacity, true);
    }

    public LegacyMissileItem(Properties properties, FormFactor formFactor, Tier tier, Fuel fuel, int fuelCapacity, boolean launchable) {
        super(properties.stacksTo(1));
        this.formFactor = formFactor;
        this.tier = tier;
        this.fuel = fuel;
        this.fuelCapacity = fuelCapacity;
        this.launchable = launchable;
    }

    public FormFactor formFactor() {
        return formFactor;
    }

    public Tier tier() {
        return tier;
    }

    public Fuel fuel() {
        return fuel;
    }

    public int fuelCapacity() {
        return fuelCapacity;
    }

    public boolean launchable() {
        return launchable;
    }

    public static boolean isLegacyMissileId(String id) {
        return LEGACY_IDS.contains(id);
    }

    public static LegacyMissileItem fromLegacyId(String id) {
        return switch (id) {
            case "missile_generic", "missile_incendiary", "missile_cluster", "missile_buster", "missile_decoy" ->
                    standard(FormFactor.V2, Tier.TIER1);
            case "missile_anti_ballistic" -> standard(FormFactor.ABM, Tier.TIER1);
            case "missile_strong", "missile_incendiary_strong", "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong" ->
                    standard(FormFactor.STRONG, Tier.TIER2);
            case "missile_stealth" -> standard(FormFactor.STRONG, Tier.TIER1);
            case "missile_burst", "missile_inferno", "missile_rain", "missile_drill" ->
                    standard(FormFactor.HUGE, Tier.TIER3);
            case "missile_nuclear", "missile_nuclear_cluster", "missile_volcano", "missile_doomsday" ->
                    standard(FormFactor.ATLAS, Tier.TIER4);
            case "missile_doomsday_rusted" -> new LegacyMissileItem(
                    new Properties(), FormFactor.ATLAS, Tier.TIER4, Fuel.JETFUEL_LOXY, 16_000, false);
            case "missile_taint", "missile_micro", "missile_bhole", "missile_schrabidium", "missile_emp", "missile_test" ->
                    standard(FormFactor.MICRO, Tier.TIER0);
            case "missile_shuttle" -> standard(FormFactor.OTHER, Tier.TIER3);
            default -> throw new IllegalArgumentException("Unknown 1.7.10 missile id: " + id);
        };
    }

    private static LegacyMissileItem standard(FormFactor formFactor, Tier tier) {
        return new LegacyMissileItem(new Properties(), formFactor, tier);
    }

    /** [parity P1: tooltip] ItemMissile.addInformation + ItemCustomLore (".desc" split by '$'). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(legacy("\u00a7o" + resolve("item.missile.tier." + tier.name().toLowerCase(java.util.Locale.ROOT))));
        if (!launchable) {
            tooltip.add(legacy("\u00a7c" + resolve("item.missile.desc.notLaunchable")));
            return;
        }
        tooltip.add(legacy(resolve("item.missile.desc.fuel") + ": " + fuel.color + resolve(fuel.translationKey)));
        if (fuelCapacity > 0) tooltip.add(legacy(resolve("item.missile.desc.fuelCapacity") + ": " + fuelCapacity + "mB"));
        String descKey = getDescriptionId() + ".desc";
        if (net.minecraft.locale.Language.getInstance().has(descKey))
            for (String line : resolve(descKey).split("\\$")) tooltip.add(legacy(line));
    }

    /** 1.7.10 tooltip lines after the name are drawn with a leading gray code. */
    static Component legacy(String text) {
        return Component.literal("\u00a77" + text);
    }

    static String resolve(String key) {
        return net.minecraft.locale.Language.getInstance().getOrDefault(key);
    }
}

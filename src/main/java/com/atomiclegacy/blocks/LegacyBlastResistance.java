package com.atomiclegacy.blocks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.level.block.state.BlockBehaviour;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * [parity T3: blast resistance] Effective explosion resistance of the original (HbmMods 1.7.10, 936c60c4) blocks.
 * 1.7.10 Block.setResistance(r) stores r*3, setHardness(h) raises the stored value to h*5 and
 * getExplosionResistance(entity) returns stored/5; 1.21 Properties.explosionResistance is used as is, so a port
 * block must carry the original effective value (0.6*r when setResistance is the last call).
 * Table: legacy/atomiclegacy/parity-t3-resistance.json, generated from ModBlocks by work/parity_t3_resistance.py.
 */
public final class LegacyBlastResistance {
    private static Map<String, Float> table;
    /**
     * [parity U3: hardness] Port replacements of the excluded depth ores (user decision 2026-11-16) and their deepslate
     * twins: the original BlockDepthOre is unbreakable (-1) without the excluded depth mining, so the port value stays.
     */
    public static final java.util.Set<String> KEEP_PORT_HARDNESS = java.util.Set.of("ore_borax", "ore_zirconium",
            "ore_nether_neodymium", "ore_alexandrite", "ore_deepslate_borax", "ore_deepslate_zirconium", "ore_deepslate_alexandrite");
    private static Map<String, Float> hardnessTable; // [parity U3: hardness]
    private static Map<String, String> materialTable; // [parity U3: tools]

    private LegacyBlastResistance() {
    }

    /**
     * Original block id for a port id: renames and flattened metadata variants of one original block.
     * Replacement ores of the excluded depth ores are table rows with alias_of (see the generator script).
     */
    public static String originalId(String id) {
        switch (id) {
            case "statue_elb_f": return "#undef";
            case "ore_cinnabar": return "ore_cinnebar";
            case "hev_battery_block": return "hev_battery";
            case "pwr_fuelrod": return "pwr_fuel";
            case "plant_foxglove", "plant_tobacco", "plant_nightshade": return "plant_flower";
            case "plant_hemp", "plant_mustard_willow": return "plant_tall";
            case "lightstone_tile", "lightstone_bricks", "lightstone_bricks_chiseled", "lightstone_chiseled": return "lightstone";
            default: break;
        }
        // Port-only deepslate twins of original ores carry the stone ore's values.
        if (id.startsWith("ore_deepslate_")) return originalId("ore_" + id.substring("ore_deepslate_".length()));
        if (id.startsWith("cluster_deepslate_")) return "cluster_" + id.substring("cluster_deepslate_".length());
        for (String base : new String[] {"stone_resource", "mass_storage", "platemetal", "steel_scaffold"}) {
            if (id.startsWith(base + "_")) return base;
        }
        return id;
    }

    /** Original effective resistance, or {@code fallback} for blocks without an original counterpart. */
    public static float of(String id, float fallback) {
        Float value = lookup(id);
        return value == null ? fallback : value;
    }

    public static Float lookup(String id) {
        Float value = table().get(originalId(id));
        if (value == null) {
            // Port-only stairs/slabs of an original block: 1.7.10 BlockStairs copies the parent's resistance.
            for (String suffix : new String[] {"_double_slab", "_slab", "_stairs"}) {
                if (id.endsWith(suffix)) {
                    return table().get(originalId(id.substring(0, id.length() - suffix.length())));
                }
            }
        }
        return value;
    }

    /**
     * [parity U3: hardness] Original 1.7.10 hardness (setHardness, setBlockUnbreakable = -1, constructor calls, BlockStairs
     * copies the parent). 1.21 destroyTime has the same meaning (vanilla stone 1.5 in both), so it is used as is.
     */
    public static float hardness(String id, float fallback) {
        Float value = hardnessLookup(id);
        return value == null ? fallback : value;
    }

    public static Float hardnessLookup(String id) {
        if (KEEP_PORT_HARDNESS.contains(id)) {
            return null;
        }
        table();
        Float value = hardnessTable.get(originalId(id));
        if (value == null) {
            for (String suffix : new String[] {"_double_slab", "_slab", "_stairs"}) {
                if (id.endsWith(suffix)) {
                    return hardnessTable.get(originalId(id.substring(0, id.length() - suffix.length())));
                }
            }
        }
        return value;
    }

    /**
     * [parity U3: tools] 1.7.10 Material.rock/iron/anvil/snow/craftedSnow/web require a tool (no drops, /100 speed
     * without it); every other material does not. Unknown (port-only) ids keep {@code portDefault}.
     */
    public static boolean requiresTool(String id, boolean portDefault) {
        table();
        String material = materialTable.get(originalId(id));
        if (material == null) {
            return portDefault;
        }
        return switch (material) {
            case "rock", "iron", "anvil", "snow", "craftedSnow", "web" -> true;
            default -> false;
        };
    }

    /** Adds requiresCorrectToolForDrops when the original material of {@code id} requires a tool (unknown ids: yes). */
    public static BlockBehaviour.Properties toolRule(String id, BlockBehaviour.Properties properties) {
        return requiresTool(id, true) ? properties.requiresCorrectToolForDrops() : properties;
    }

    private static synchronized Map<String, Float> table() {
        if (table == null) {
            Map<String, Float> map = new HashMap<>();
            Map<String, Float> hardness = new HashMap<>();
            Map<String, String> materials = new HashMap<>();
            try (var input = LegacyBlastResistance.class.getResourceAsStream("/legacy/atomiclegacy/parity-t3-resistance.json")) {
                if (input == null) {
                    throw new IllegalStateException("missing parity-t3-resistance.json");
                }
                for (var element : JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray()) {
                    JsonObject row = element.getAsJsonObject();
                    String text = row.get("expected").getAsString();
                    float value = "Infinity".equals(text) ? Float.POSITIVE_INFINITY : Float.parseFloat(text);
                    map.putIfAbsent(row.get("id").getAsString(), value);
                    map.putIfAbsent(row.get("field").getAsString(), value);
                    float h = row.get("hardness").getAsFloat();
                    hardness.putIfAbsent(row.get("id").getAsString(), h);
                    hardness.putIfAbsent(row.get("field").getAsString(), h);
                    if (row.has("material") && !row.get("material").isJsonNull()) {
                        materials.putIfAbsent(row.get("id").getAsString(), row.get("material").getAsString());
                        materials.putIfAbsent(row.get("field").getAsString(), row.get("material").getAsString());
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("cannot read original blast resistance table", e);
            }
            hardnessTable = hardness;
            materialTable = materials;
            table = map;
        }
        return table;
    }
}

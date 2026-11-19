package com.atomiclegacy.tileentity;

import com.atomiclegacy.items.BlueprintFolderItem;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.LegacyBombCallerItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.RtgDepletedPelletItem;
import com.atomiclegacy.items.StampBookItem;
import com.atomiclegacy.items.UniversalGrenadeItem;
import com.atomiclegacy.inventory.recipes.MkuCraftingRecipe;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HbmStructureLoot {
    private static final Map<String, List<Entry>> POOLS = new LinkedHashMap<>();
    private static final List<Entry> BACKUP_POOL = List.of(
            new Entry("minecraft:bread", 1, 3, 10),
            new Entry("minecraft:stick", 2, 5, 10),
            new Entry("atomiclegacy:scrap", 1, 3, 10),
            new Entry("atomiclegacy:dust", 2, 5, 5)
    );

    static {
        registerPilePools();
        registerCommonContainerPools();
    }

    private HbmStructureLoot() {
    }

    public static void applyPileLoot(DecoLootBlockEntity loot, String pool, RandomSource random) {
        if (!loot.items().isEmpty()) {
            return;
        }

        List<String> names = lootNames();
        int start = names.indexOf(pool);
        if (start < 0) {
            start = names.indexOf("LOOT_BONES");
        }

        for (int i = Math.max(0, start); i < names.size(); i++) {
            applyVisiblePile(loot, names.get(i), random);
            if (!loot.items().isEmpty()) {
                return;
            }
        }
        lootBones(loot, random);
    }

    /**
     * Fills a generated inventory using the legacy wand item-count range
     * (minimum inclusive, maximum exclusive), while each pool entry keeps its
     * own legacy inclusive stack-size range.
     * This is public because procedural (non-NBT) structure pieces place their
     * containers directly and must use the same validated pools as wand_loot.
     */
    public static void fillContainer(Container container, String pool, int min, int max, RandomSource random) {
        int count = Math.max(0, min);
        if (max > min) {
            count += random.nextInt(max - min);
        }
        // [parity R3: loot] WeightedRandomChestContent.generateChestContents: each roll is
        // put into a random slot (overwriting), and a roll larger than the item's max stack
        // size is split into single items, exactly as 1.7.10 did.
        int size = container.getContainerSize();
        if (size <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            ItemStack stack = randomStack(pool, random);
            if (stack.isEmpty()) {
                random.nextInt(size);
                continue;
            }
            if (stack.getCount() <= stack.getMaxStackSize()) {
                container.setItem(random.nextInt(size), stack);
            } else {
                int total = stack.getCount();
                for (int j = 0; j < total; j++) {
                    container.setItem(random.nextInt(size), stack.copyWithCount(1));
                }
            }
        }
        container.setChanged();
    }

    private static void applyVisiblePile(DecoLootBlockEntity loot, String pool, RandomSource random) {
        switch (pool) {
            case "LOOT_BOOKLET" -> loot.addItem(com.atomiclegacy.items.LegacyBookLoreItem.create(stack("atomiclegacy:book_lore", 1), "beacon", 12, 0x404040, 0xD637B3), 0.0D, 0.0D, 0.0D); // [parity R3: loot] ItemBookLore.createBook
            case "LOOT_CAPNUKE" -> {
                // [parity R3: loot] NUKE_STANDARD at (-0.25,0,-0.125), otherwise ROCKET_HEAT at (-0.25,0,-0.25).
                if (random.nextInt(5) == 0) {
                    loot.addItem(variantStack("atomiclegacy:ammo_standard", "nuke_standard"), -0.25D, 0.0D, -0.125D);
                } else {
                    loot.addItem(variantStack("atomiclegacy:ammo_standard", "rocket_heat"), -0.25D, 0.0D, -0.25D);
                }
                for (int i = 0; i < 4; i++) {
                    addItemWithDeviation(loot, random, stack("atomiclegacy:cap_nuka", 2), 0.125D, i * 0.03125D, 0.25D);
                }
                for (int i = 0; i < 2; i++) {
                    addItemWithDeviation(loot, random, stack("atomiclegacy:syringe_metal_stimpak", 1), -0.25D, i * 0.03125D, 0.25D);
                }
                for (int i = 0; i < 6; i++) {
                    addItemWithDeviation(loot, random, stack("atomiclegacy:cap_nuka", 2), 0.125D, i * 0.03125D, -0.25D);
                }
            }
            case "LOOT_MEDICINE" -> {
                for (int i = 0; i < 4; i++) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MED_SYRINGE", random), 0.125D, i * 0.03125D, 0.25D);
                }
                addItemWithDeviation(loot, random, randomStack("POOL_PILE_MED_PILLS", random), -0.25D, 0.0D, -0.125D);
            }
            case "LOOT_CAPSTASH" -> {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        int count = random.nextInt(5) + 3;
                        for (int i = 0; i < count; i++) {
                            addItemWithDeviation(loot, random, randomStack("POOL_PILE_CAPS", random), x * 0.3125D, i * 0.03125D, z * 0.3125D);
                        }
                    }
                }
            }
            case "LOOT_MAKESHIFT_GUN" -> {
                // [parity R3: loot] LootGenerator.lootMakeshiftGun
                boolean gun = random.nextBoolean();
                if (gun) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_GUN", random), 0.125D, 0.025D, 0.25D);
                }
                if (!gun || random.nextBoolean()) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_WRENCH", random), -0.25D, 0.0D, -0.28125D);
                }
                int count = random.nextInt(2) + 1;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_PLATES", random), -0.25D, i * 0.03125D, 0.3125D);
                }
                count = random.nextInt(2) + 2;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, randomStack("POOL_PILE_MAKESHIFT_WIRE", random), 0.25D, i * 0.03125D, 0.1875D);
                }
            }
            case "LOOT_NUKE_STORAGE" -> {
                for (int x = 0; x < 4; x++) {
                    for (int z = 0; z < 4; z++) {
                        if (random.nextBoolean()) {
                            loot.addItem(randomStack("POOL_PILE_NUKE_STORAGE", random), -0.375D + x * 0.25D, 0.0D, -0.375D + z * 0.25D);
                        }
                    }
                }
            }
            case "LOOT_BONES" -> lootBones(loot, random);
            case "LOOT_GLYPHID_HIVE" -> pile(loot, "POOL_PILE_HIVE", random, random.nextInt(3) + 3);
            case "LOOT_METEOR" -> {
                if (loot.getLevel() != null) {
                    ItemStack ingredient = MkuCraftingRecipe.randomClueIngredient(loot.getLevel());
                    addItemWithDeviation(loot, random, ingredient, 0.0D, 0.0D, 0.25D);
                    addItemWithDeviation(loot, random,
                            MkuCraftingRecipe.clueBook(loot.getLevel(), ingredient.getItem()),
                            0.0D, 0.0D, -0.25D);
                }
            }
            case "LOOT_FLAREGUN" -> {
                // [parity R3: loot] LootGenerator.lootFlareGun
                addItemWithDeviation(loot, random, stack("atomiclegacy:gun_flaregun", 1), 0.0D, 0.0D, -0.25D);
                int count = random.nextInt(3) + 2;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, variantStack("atomiclegacy:ammo_standard", "g26_flare"), -0.25D, i * 0.03125D, 0.25D);
                }
                count = random.nextInt(1) + 1;
                for (int i = 0; i < count; i++) {
                    addItemWithDeviation(loot, random, variantStack("atomiclegacy:ammo_standard",
                            random.nextBoolean() ? "g26_flare_supply" : "g26_flare_weapon"), 0.25D, i * 0.03125D, 0.125D);
                }
            }
            case "LOOT_SHIT" -> pile(loot, "POOL_PILE_OF_GARBAGE", random, random.nextInt(3) + 3);
            case "LOOT_MECHANICAL" -> pile(loot, "POOL_PILE_MECHANICAL", random, random.nextInt(6) + 1);
            case "LOOT_GEAR" -> pile(loot, "POOL_PILE_GEAR", random, random.nextInt(6) + 1);
            case "LOOT_SUPPLIES" -> pile(loot, "POOL_PILE_SUPPLIES", random, random.nextInt(3) + 4);
            default -> lootBones(loot, random);
        }
    }

    private static void lootBones(DecoLootBlockEntity loot, RandomSource random) {
        pile(loot, "POOL_PILE_BONES", random, random.nextInt(3) + 3);
    }

    private static void pile(DecoLootBlockEntity loot, String pool, RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            addItemWithDeviation(loot, random, randomStack(pool, random), random.nextDouble() - 0.5D, i * 0.03125D, random.nextDouble() - 0.5D);
        }
    }

    private static void addItemWithDeviation(DecoLootBlockEntity loot, RandomSource random, ItemStack stack, double x, double y, double z) {
        loot.addItem(stack, x + random.nextGaussian() * 0.02D, y, z + random.nextGaussian() * 0.02D);
    }

    public static ItemStack randomStack(String pool, RandomSource random) {
        List<Entry> entries = availablePool(pool);
        int total = entries.stream().mapToInt(Entry::weight).sum();
        int roll = random.nextInt(Math.max(1, total));
        for (Entry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.stack(random);
            }
        }
        return entries.get(0).stack(random);
    }

    /** [parity S3: JEI] ItemPool.getPool entries as (theItemId with stack size 1, itemWeight), for SatelliteHandler. */
    public static List<Map.Entry<ItemStack, Integer>> displayEntries(String pool) {
        List<Map.Entry<ItemStack, Integer>> result = new ArrayList<>();
        List<Entry> entries = POOLS.get(pool);
        if (entries == null) {
            return result;
        }
        for (Entry entry : entries) {
            ItemStack stack = entry.exists() ? new Entry(entry.id(), 1, 1, entry.weight(), entry.variant()).stack(null) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                result.add(Map.entry(stack, entry.weight()));
            }
        }
        return result;
    }

    private static List<Entry> availablePool(String pool) {
        List<Entry> entries = POOLS.get(pool);
        if (entries == null) {
            entries = BACKUP_POOL;
        }
        List<Entry> available = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.exists()) {
                available.add(entry);
            }
        }
        if (!available.isEmpty()) {
            return available;
        }
        return BACKUP_POOL;
    }

    private static ItemStack variantStack(String id, String variant) {
        Entry entry = new Entry(id, 1, 1, 1, variant);
        return entry.exists() ? entry.stack(null) : ItemStack.EMPTY;
    }

    private static ItemStack stack(String id, int count) {
        Entry entry = new Entry(id, count, count, 1);
        return entry.exists() ? entry.stack(null) : ItemStack.EMPTY;
    }

    /** [parity R3: diagnostics] name -> {entries, total weight, sum(min), sum(max)} of the registered pools. */
    public static Map<String, int[]> diagnosticPoolShapes() {
        Map<String, int[]> shapes = new LinkedHashMap<>();
        POOLS.forEach((name, entries) -> {
            int weight = 0;
            int min = 0;
            int max = 0;
            for (Entry entry : entries) {
                weight += entry.weight();
                min += entry.min();
                max += entry.max();
            }
            shapes.put(name, new int[] {entries.size(), weight, min, max});
        });
        return shapes;
    }

    /** [parity R3: diagnostics] entries whose item is not registered or whose variant cannot be built. */
    public static List<String> diagnosticEntryProblems() {
        List<String> problems = new ArrayList<>();
        POOLS.forEach((name, entries) -> {
            for (Entry entry : entries) {
                if (!entry.exists()) {
                    problems.add(name + ": item not registered " + entry.id());
                    continue;
                }
                int count = Math.max(1, entry.max());
                ItemStack built = new Entry(entry.id(), count, count, 1, entry.variant()).stack(null);
                if (built.isEmpty()) {
                    problems.add(name + ": cannot build " + entry.id() + " variant " + entry.variant());
                } else if (entry.variant() != null && built.getItem() instanceof LegacyVariantItem variantItem
                        && !variantItem.variant(built).id().equals(entry.variant())) {
                    problems.add(name + ": unknown variant " + entry.variant() + " of " + entry.id());
                }
            }
        });
        return problems;
    }

    /** [parity R3: diagnostics] every name a wand_loot block may reference (item pools and LootGenerator piles). */
    public static java.util.Set<String> diagnosticKnownPoolNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>(POOLS.keySet());
        names.addAll(lootNames());
        return names;
    }

    static List<String> lootNames() {
        return List.of(
                "LOOT_BOOKLET",
                "LOOT_CAPNUKE",
                "LOOT_MEDICINE",
                "LOOT_CAPSTASH",
                "LOOT_MAKESHIFT_GUN",
                "LOOT_NUKE_STORAGE",
                "LOOT_BONES",
                "LOOT_GLYPHID_HIVE",
                "LOOT_METEOR",
                "LOOT_FLAREGUN",
                "LOOT_MECHANICAL",
                "LOOT_GEAR",
                "LOOT_SHIT",
                "LOOT_SUPPLIES"
        );
    }

    static List<String> containerPoolNames() {
        return List.copyOf(POOLS.keySet());
    }

    // [parity R3: loot] Generated 1:1 from ItemPool*.java of the reference commit
    // (order, min, max and weight unchanged; meta values mapped to flattened ids/variants).
    private static void registerPilePools() {
        // ItemPoolsPile.POOL_PILE_HIVE
        pool("POOL_PILE_HIVE",
                e("minecraft:iron_ingot", 1, 3, 10),
                e("atomiclegacy:ingot_steel", 1, 2, 10),
                e("atomiclegacy:ingot_aluminium", 1, 2, 10),
                e("atomiclegacy:scrap", 3, 6, 10),
                e("atomiclegacy:gas_mask_m65", 1, 1, 10),
                e("atomiclegacy:steel_plate", 1, 1, 5),
                e("atomiclegacy:steel_legs", 1, 1, 5),
                e("atomiclegacy:steel_pickaxe", 1, 1, 5),
                e("atomiclegacy:steel_shovel", 1, 1, 5),
                e("atomiclegacy:gun_maresleg", 1, 1, 5),
                e("atomiclegacy:gun_light_revolver", 1, 1, 1),
                e("atomiclegacy:grenade_universal", 1, 2, 5, "frag:he:s3:frag_sleeve"),
                e("atomiclegacy:grenade_universal", 1, 2, 3, "stick:demo:impact"),
                e("atomiclegacy:ammo_standard", 4, 4, 10, "g12"),
                e("atomiclegacy:ammo_standard", 6, 12, 10, "m357_sp"),
                e("atomiclegacy:ammo_standard", 1, 1, 2, "g40_he"),
                e("atomiclegacy:bottle_nuka", 1, 2, 20),
                e("atomiclegacy:bottle_quantum", 1, 2, 1),
                e("atomiclegacy:definitelyfood", 5, 12, 20),
                e("atomiclegacy:egg_glyphid", 1, 3, 30),
                e("atomiclegacy:syringe_metal_stimpak", 1, 1, 5),
                e("atomiclegacy:iv_blood", 1, 1, 10),
                e("minecraft:experience_bottle", 1, 3, 5)
        );
        // ItemPoolsPile.POOL_PILE_BONES
        pool("POOL_PILE_BONES",
                e("minecraft:bone", 1, 1, 10),
                e("minecraft:rotten_flesh", 1, 1, 5),
                e("atomiclegacy:biomass", 1, 1, 2)
        );
        // ItemPoolsPile.POOL_PILE_CAPS
        pool("POOL_PILE_CAPS",
                e("atomiclegacy:cap_nuka", 4, 4, 20),
                e("atomiclegacy:cap_quantum", 4, 4, 3),
                e("atomiclegacy:cap_sparkle", 4, 4, 1)
        );
        // ItemPoolsPile.POOL_PILE_MED_SYRINGE
        pool("POOL_PILE_MED_SYRINGE",
                e("atomiclegacy:syringe_metal_stimpak", 1, 1, 10),
                e("atomiclegacy:syringe_metal_medx", 1, 1, 5),
                e("atomiclegacy:syringe_metal_psycho", 1, 1, 5)
        );
        // ItemPoolsPile.POOL_PILE_MED_PILLS
        pool("POOL_PILE_MED_PILLS",
                e("atomiclegacy:radaway", 1, 1, 10),
                e("atomiclegacy:radx", 1, 1, 10),
                e("atomiclegacy:iv_blood", 1, 1, 15),
                e("atomiclegacy:siox", 1, 1, 5)
        );
        // ItemPoolsPile.POOL_PILE_MAKESHIFT_GUN
        pool("POOL_PILE_MAKESHIFT_GUN",
                e("atomiclegacy:gun_maresleg", 1, 1, 10)
        );
        // ItemPoolsPile.POOL_PILE_MAKESHIFT_WRENCH
        pool("POOL_PILE_MAKESHIFT_WRENCH",
                e("atomiclegacy:wrench", 1, 1, 10)
        );
        // ItemPoolsPile.POOL_PILE_MAKESHIFT_PLATES
        pool("POOL_PILE_MAKESHIFT_PLATES",
                e("atomiclegacy:plate_steel", 1, 1, 10)
        );
        // ItemPoolsPile.POOL_PILE_MAKESHIFT_WIRE
        pool("POOL_PILE_MAKESHIFT_WIRE",
                e("atomiclegacy:wire_aluminium", 1, 1, 10)
        );
        // ItemPoolsPile.POOL_PILE_NUKE_STORAGE
        pool("POOL_PILE_NUKE_STORAGE",
                e("atomiclegacy:ammo_standard", 1, 1, 50, "nuke_standard"),
                e("atomiclegacy:ammo_standard", 1, 1, 10, "nuke_high"),
                e("atomiclegacy:ammo_standard", 1, 1, 10, "nuke_tots")
        );
        // ItemPoolsPile.POOL_PILE_OF_GARBAGE
        pool("POOL_PILE_OF_GARBAGE",
                e("atomiclegacy:pipe_iron", 0, 2, 20),
                e("atomiclegacy:scrap", 1, 5, 20),
                e("atomiclegacy:dust", 1, 3, 40),
                e("atomiclegacy:dust_tiny", 1, 7, 40),
                e("atomiclegacy:powder_cement", 1, 6, 40),
                e("atomiclegacy:nugget_lead", 0, 3, 20),
                e("atomiclegacy:wire_lead", 1, 2, 20),
                e("atomiclegacy:powder_ash_wood", 0, 1, 15),
                e("atomiclegacy:plate_lead", 0, 1, 15),
                e("minecraft:string", 0, 1, 15),
                e("atomiclegacy:bolt_lead", 0, 2, 15),
                e("atomiclegacy:pin", 0, 2, 15),
                e("atomiclegacy:cap_nuka", 0, 8, 15),
                e("atomiclegacy:plate_iron", 0, 2, 15),
                e("atomiclegacy:fallout", 0, 2, 15),
                e("atomiclegacy:coil_tungsten", 0, 2, 15),
                e("atomiclegacy:can_empty", 0, 1, 15),
                e("atomiclegacy:ingot_asbestos", 0, 1, 15),
                e("atomiclegacy:syringe_metal_empty", 0, 1, 15),
                e("atomiclegacy:syringe_empty", 0, 1, 15),
                e("atomiclegacy:pipe_lead", 0, 1, 5),
                e("atomiclegacy:motor", 0, 1, 5),
                e("atomiclegacy:canned_conserve", 0, 1, 5, "mystery")
        );
        // ItemPoolsPile.POOL_PILE_MECHANICAL
        pool("POOL_PILE_MECHANICAL",
                e("atomiclegacy:defuser", 1, 1, 30),
                e("atomiclegacy:screwdriver", 1, 1, 30),
                e("atomiclegacy:wire_copper", 8, 12, 120),
                e("atomiclegacy:plate_steel", 3, 8, 40),
                e("atomiclegacy:plate_copper", 2, 5, 40),
                e("atomiclegacy:coil_copper", 2, 5, 40),
                e("atomiclegacy:coil_tungsten", 2, 5, 40)
        );
        // ItemPoolsPile.POOL_PILE_GEAR
        pool("POOL_PILE_GEAR",
                e("atomiclegacy:defuser", 1, 1, 40),
                e("atomiclegacy:screwdriver", 1, 1, 30),
                e("atomiclegacy:canteen_vodka", 1, 1, 40),
                e("atomiclegacy:casing_small_steel", 1, 4, 30),
                e("atomiclegacy:casing_small", 3, 8, 40),
                e("atomiclegacy:casing_buckshot", 3, 8, 40),
                e("atomiclegacy:canned_conserve", 2, 5, 40, "beef"),
                e("atomiclegacy:taurun_helmet", 1, 1, 20),
                e("atomiclegacy:taurun_plate", 1, 1, 20),
                e("atomiclegacy:taurun_legs", 1, 1, 20),
                e("atomiclegacy:taurun_boots", 1, 1, 20)
        );
        // ItemPoolsPile.POOL_PILE_SUPPLIES
        pool("POOL_PILE_SUPPLIES",
                e("atomiclegacy:grenade_universal", 3, 5, 10, "frag:he:s3:frag_sleeve"),
                e("atomiclegacy:grenade_universal", 3, 5, 10, "frag:he:s3:frag_sleeve"),
                e("atomiclegacy:grenade_universal", 3, 5, 10, "frag:he:s3:frag_sleeve"),
                e("atomiclegacy:syringe_metal_stimpak", 3, 5, 30),
                e("atomiclegacy:syringe_metal_psycho", 3, 5, 30),
                e("atomiclegacy:syringe_antidote", 1, 2, 30),
                e("atomiclegacy:ammo_container", 2, 3, 40)
        );
    }

    private static void registerCommonContainerPools() {
        // ItemPoolsC130.POOL_SUPPLIES
        pool("POOL_SUPPLIES",
                e("atomiclegacy:definitelyfood", 3, 10, 25),
                e("atomiclegacy:syringe_metal_stimpak", 1, 3, 10),
                e("atomiclegacy:pill_iodine", 1, 2, 2),
                e("atomiclegacy:canister_full", 1, 4, 5, "diesel"),
                e("atomiclegacy:machine_diesel", 1, 1, 1),
                e("atomiclegacy:geiger_counter", 1, 1, 2),
                e("atomiclegacy:med_bag", 1, 1, 3),
                e("atomiclegacy:radaway", 1, 5, 10)
        );
        // ItemPoolsC130.POOL_WEAPONS
        pool("POOL_WEAPONS",
                e("atomiclegacy:gun_light_revolver", 1, 1, 100),
                e("atomiclegacy:gun_henry", 1, 1, 100),
                e("atomiclegacy:gun_maresleg", 1, 1, 100),
                e("atomiclegacy:gun_greasegun", 1, 1, 100),
                e("atomiclegacy:gun_carbine", 1, 1, 50),
                e("atomiclegacy:gun_heavy_revolver", 1, 1, 50),
                e("atomiclegacy:gun_panzerschreck", 1, 1, 20),
                e("atomiclegacy:gun_fliegerfaust", 1, 1, 20),
                e("atomiclegacy:gun_double_barrel", 1, 1, 10),
                e("atomiclegacy:gun_n_i_4_n_i", 1, 1, 1)
        );
        // ItemPoolsC130.POOL_AMMO
        pool("POOL_AMMO",
                e("atomiclegacy:ammo_standard", 12, 12, 10, "m357_sp"),
                e("atomiclegacy:ammo_standard", 6, 6, 10, "m357_fmj"),
                e("atomiclegacy:ammo_standard", 12, 12, 5, "m44_sp"),
                e("atomiclegacy:ammo_standard", 6, 6, 5, "m44_fmj"),
                e("atomiclegacy:ammo_standard", 12, 12, 10, "p9_sp"),
                e("atomiclegacy:ammo_standard", 6, 6, 10, "p9_fmj"),
                e("atomiclegacy:ammo_standard", 6, 6, 5, "r762_sp"),
                e("atomiclegacy:ammo_standard", 6, 6, 10, "g12_bp"),
                e("atomiclegacy:ammo_standard", 1, 1, 3, "rocket_he"),
                e("atomiclegacy:ammo_container", 1, 1, 1)
        );
        // ItemPoolsComponent.POOL_MACHINE_PARTS
        pool("POOL_MACHINE_PARTS",
                e("atomiclegacy:plate_steel", 1, 5, 5),
                e("atomiclegacy:shell_steel", 1, 3, 3),
                e("atomiclegacy:plate_polymer", 1, 6, 5),
                e("atomiclegacy:bolt_steel", 4, 16, 3),
                e("atomiclegacy:bolt_tungsten", 4, 16, 3),
                e("atomiclegacy:coil_tungsten", 1, 2, 5),
                e("atomiclegacy:motor", 1, 2, 4),
                e("atomiclegacy:coil_copper", 1, 3, 4),
                e("atomiclegacy:coil_copper_torus", 1, 2, 3),
                e("atomiclegacy:wire_red_copper", 1, 8, 5),
                e("atomiclegacy:piston_selenium", 1, 1, 3),
                e("atomiclegacy:battery_pack", 1, 1, 3, "battery_lead"),
                e("atomiclegacy:circuit_vacuum_tube", 1, 2, 4),
                e("atomiclegacy:circuit_pcb", 1, 3, 5),
                e("atomiclegacy:circuit_capacitor", 1, 1, 3),
                e("atomiclegacy:blade_titanium", 1, 8, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1)
        );
        // ItemPoolsComponent.POOL_NUKE_FUEL
        pool("POOL_NUKE_FUEL",
                e("atomiclegacy:billet_uranium", 1, 4, 4),
                e("atomiclegacy:billet_th232", 1, 3, 3),
                e("atomiclegacy:billet_uranium_fuel", 1, 3, 5),
                e("atomiclegacy:billet_mox_fuel", 1, 3, 5),
                e("atomiclegacy:billet_thorium_fuel", 1, 3, 3),
                e("atomiclegacy:billet_ra226be", 1, 2, 2),
                e("atomiclegacy:billet_beryllium", 1, 1, 1),
                e("atomiclegacy:nugget_u233", 1, 1, 1),
                e("atomiclegacy:nugget_uranium_fuel", 1, 1, 1),
                e("atomiclegacy:rod_zirnox_empty", 1, 3, 3),
                e("atomiclegacy:ingot_graphite", 1, 4, 3),
                e("atomiclegacy:pile_rod_nu", 1, 1, 3),
                e("atomiclegacy:pile_rod_ra226be", 1, 1, 2),
                e("atomiclegacy:pile_rod_waste", 1, 1, 5),
                e("atomiclegacy:reacher", 1, 1, 3),
                e("atomiclegacy:screwdriver", 1, 1, 2)
        );
        // ItemPoolsComponent.POOL_SILO
        pool("POOL_SILO",
                e("atomiclegacy:missile_generic", 1, 1, 4),
                e("atomiclegacy:missile_incendiary", 1, 1, 4),
                e("atomiclegacy:gas_mask_m65", 1, 1, 5),
                e("atomiclegacy:battery_pack", 1, 1, 3, "battery_lead"),
                e("atomiclegacy:designator", 1, 1, 5),
                e("atomiclegacy:thruster_small", 1, 1, 5),
                e("atomiclegacy:thruster_medium", 1, 1, 4),
                e("atomiclegacy:fuel_tank_small", 1, 1, 5),
                e("atomiclegacy:fuel_tank_medium", 1, 1, 4),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "carpet"),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "agent_orange"),
                e("atomiclegacy:bottle_nuka", 1, 3, 10)
        );
        // ItemPoolsComponent.POOL_OFFICE_TRASH
        pool("POOL_OFFICE_TRASH",
                e("minecraft:paper", 1, 12, 10),
                e("minecraft:book", 1, 3, 4),
                e("atomiclegacy:twinkie", 1, 2, 6),
                e("atomiclegacy:coffee", 1, 1, 4),
                e("atomiclegacy:flame_politics", 1, 1, 2),
                e("atomiclegacy:ring_pull", 1, 1, 4),
                e("atomiclegacy:can_empty", 1, 1, 2),
                e("atomiclegacy:can_creature", 1, 2, 2),
                e("atomiclegacy:can_smart", 1, 3, 2),
                e("atomiclegacy:can_mrsugar", 1, 2, 2),
                e("atomiclegacy:cap_nuka", 1, 16, 2),
                e("atomiclegacy:book_guide_book", 1, 1, 1),
                e("atomiclegacy:deco_computer", 1, 1, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1),
                e("atomiclegacy:coin_token", 1, 1, 2)
        );
        // ItemPoolsComponent.POOL_FILING_CABINET
        pool("POOL_FILING_CABINET",
                e("minecraft:paper", 1, 12, 240),
                e("minecraft:book", 1, 3, 90),
                e("minecraft:map", 1, 1, 50),
                e("minecraft:writable_book", 1, 1, 30),
                e("atomiclegacy:cigarette", 1, 16, 20),
                e("atomiclegacy:dust", 1, 1, 40),
                e("atomiclegacy:dust_tiny", 1, 3, 75),
                e("atomiclegacy:ink", 1, 1, 1),
                e("atomiclegacy:screwdriver", 1, 1, 10),
                e("atomiclegacy:blueprint_folder", 1, 1, 5),
                e("atomiclegacy:coin_token", 1, 1, 30)
        );
        // ItemPoolsComponent.POOL_SOLID_FUEL
        pool("POOL_SOLID_FUEL",
                e("atomiclegacy:solid_fuel", 1, 5, 1),
                e("atomiclegacy:solid_fuel_presto", 1, 2, 2),
                e("atomiclegacy:ball_dynamite", 1, 4, 2),
                e("atomiclegacy:coke_petroleum", 1, 3, 1),
                e("minecraft:redstone", 1, 3, 1),
                e("atomiclegacy:niter", 1, 3, 1)
        );
        // ItemPoolsComponent.POOL_VAULT_LAB
        pool("POOL_VAULT_LAB",
                e("atomiclegacy:blowtorch", 1, 1, 4, "empty"),
                e("atomiclegacy:chemistry_set", 1, 1, 15),
                e("atomiclegacy:screwdriver", 1, 1, 10),
                e("atomiclegacy:nugget_mercury", 1, 1, 3),
                e("atomiclegacy:morning_glory", 1, 1, 1),
                e("atomiclegacy:filter_coal", 1, 1, 5),
                e("atomiclegacy:dust", 1, 3, 25),
                e("minecraft:paper", 1, 2, 15),
                e("atomiclegacy:cell_empty", 1, 1, 5),
                e("minecraft:glass_bottle", 1, 1, 5),
                e("atomiclegacy:powder_iodine", 1, 1, 1),
                e("atomiclegacy:powder_bromine", 1, 1, 1),
                e("atomiclegacy:powder_cobalt", 1, 1, 1),
                e("atomiclegacy:powder_neodymium", 1, 1, 1),
                e("atomiclegacy:powder_boron", 1, 1, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1, "discover")
        );
        // ItemPoolsComponent.POOL_VAULT_LOCKERS
        pool("POOL_VAULT_LOCKERS",
                e("atomiclegacy:robes_helmet", 1, 1, 1),
                e("atomiclegacy:robes_plate", 1, 1, 1),
                e("atomiclegacy:robes_legs", 1, 1, 1),
                e("atomiclegacy:robes_boots", 1, 1, 1),
                e("atomiclegacy:jackt", 1, 1, 1),
                e("atomiclegacy:jackt2", 1, 1, 1),
                e("atomiclegacy:gas_mask_m65", 1, 1, 2),
                e("atomiclegacy:gas_mask_mono", 1, 1, 2),
                e("atomiclegacy:goggles", 1, 1, 2),
                e("atomiclegacy:gas_mask_filter", 1, 1, 4),
                e("atomiclegacy:flame_opinion", 1, 3, 5),
                e("atomiclegacy:flame_conspiracy", 1, 3, 5),
                e("atomiclegacy:flame_politics", 1, 3, 5),
                e("atomiclegacy:definitelyfood", 2, 7, 5),
                e("atomiclegacy:cigarette", 1, 8, 5),
                e("atomiclegacy:armor_polish", 1, 1, 3),
                e("atomiclegacy:gun_kit_1", 1, 1, 3),
                e("atomiclegacy:rag", 1, 3, 5),
                e("minecraft:paper", 1, 6, 7),
                e("minecraft:clock", 1, 1, 3),
                e("minecraft:book", 1, 5, 10),
                e("minecraft:experience_bottle", 1, 3, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1, "discover"),
                e("atomiclegacy:ammo_container", 1, 1, 1),
                e("atomiclegacy:coin_token", 1, 1, 5)
        );
        // ItemPoolsComponent.POOL_METEOR_SAFE
        pool("POOL_METEOR_SAFE",
                e("atomiclegacy:book_of_", 1, 1, 1),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing1"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing2"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing3"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing4"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing5"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing6"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing7"),
                e("atomiclegacy:stamp_book", 1, 1, 1, "printing8")
        );
        // ItemPoolsComponent.POOL_OIL_RIG
        pool("POOL_OIL_RIG",
                e("atomiclegacy:oil_detector", 1, 1, 1),
                e("atomiclegacy:canister_full", 1, 4, 5, "oil"),
                e("atomiclegacy:canister_empty", 4, 16, 10),
                e("atomiclegacy:circuit_analog", 1, 4, 1),
                e("atomiclegacy:circuit_capacitor", 1, 1, 3)
        );
        // ItemPoolsComponent.POOL_RTG
        pool("POOL_RTG",
                e("atomiclegacy:pellet_rtg_depleted", 1, 1, 40, "lead"),
                e("atomiclegacy:pellet_rtg_weak", 0, 1, 1)
        );
        // ItemPoolsComponent.POOL_REPAIR_MATERIALS
        pool("POOL_REPAIR_MATERIALS",
                e("atomiclegacy:ingot_aluminium", 2, 8, 3),
                e("atomiclegacy:ingot_steel", 0, 12, 4),
                e("atomiclegacy:plate_aluminium", 5, 12, 3),
                e("atomiclegacy:plate_iron", 6, 16, 3),
                e("atomiclegacy:plate_steel", 2, 12, 2),
                e("atomiclegacy:ingot_tungsten", 0, 2, 1),
                e("atomiclegacy:deco_aluminium", 12, 24, 4),
                e("atomiclegacy:deco_steel", 5, 12, 2),
                e("atomiclegacy:block_aluminium", 0, 2, 1),
                e("atomiclegacy:block_steel", 0, 1, 1),
                e("atomiclegacy:bolt_steel", 4, 16, 3),
                e("atomiclegacy:circuit_vacuum_tube", 1, 2, 4),
                e("atomiclegacy:circuit_analog", 1, 3, 5),
                e("atomiclegacy:circuit_capacitor", 1, 1, 3)
        );
        // ItemPoolsLegacy.POOL_GENERIC
        pool("POOL_GENERIC",
                e("minecraft:bread", 1, 5, 8),
                e("atomiclegacy:twinkie", 1, 3, 6),
                e("minecraft:iron_ingot", 2, 6, 10),
                e("atomiclegacy:ingot_steel", 2, 5, 7),
                e("atomiclegacy:ingot_beryllium", 1, 2, 4),
                e("atomiclegacy:ingot_titanium", 1, 1, 3),
                e("atomiclegacy:circuit_vacuum_tube", 1, 1, 5),
                e("atomiclegacy:gun_light_revolver", 1, 1, 3),
                e("atomiclegacy:ammo_standard", 2, 6, 4, "m357_sp"),
                e("atomiclegacy:ammo_standard", 3, 6, 3, "g12_bp"),
                e("atomiclegacy:ammo_standard", 1, 1, 1, "g26_flare_supply"),
                e("atomiclegacy:gun_kit_1", 1, 3, 4),
                e("atomiclegacy:gun_maresleg", 1, 1, 1),
                e("atomiclegacy:casing_small", 4, 10, 3),
                e("atomiclegacy:casing_shotshell", 4, 10, 3),
                e("atomiclegacy:cordite", 4, 6, 5),
                e("atomiclegacy:battery_pack", 1, 1, 1, "battery_redstone"),
                e("atomiclegacy:scrap", 1, 3, 10),
                e("atomiclegacy:dust", 2, 4, 9),
                e("atomiclegacy:bottle_opener", 1, 1, 2),
                e("atomiclegacy:bottle_nuka", 1, 3, 4),
                e("atomiclegacy:bottle_cherry", 1, 1, 2),
                e("atomiclegacy:stealth_boy", 1, 1, 1),
                e("atomiclegacy:cap_nuka", 1, 15, 7),
                e("atomiclegacy:canister_full", 1, 2, 2, "diesel"),
                e("atomiclegacy:canister_full", 1, 2, 3, "biofuel"),
                e("atomiclegacy:gas_mask_m65", 1, 1, 2, "damage:60"),
                e("atomiclegacy:gas_mask_filter", 1, 1, 3),
                e("atomiclegacy:blueprint_folder", 1, 1, 1),
                e("atomiclegacy:coin_token", 1, 1, 2)
        );
        // ItemPoolsLegacy.POOL_ANTENNA
        pool("POOL_ANTENNA",
                e("atomiclegacy:twinkie", 1, 3, 4),
                e("atomiclegacy:ingot_steel", 1, 2, 7),
                e("atomiclegacy:ingot_red_copper", 1, 1, 4),
                e("atomiclegacy:ingot_titanium", 1, 3, 5),
                e("atomiclegacy:wire_red_copper", 2, 3, 7),
                e("atomiclegacy:circuit_vacuum_tube", 1, 1, 4),
                e("atomiclegacy:circuit_capacitor", 1, 1, 2),
                e("atomiclegacy:battery_pack", 1, 1, 1, "battery_redstone"),
                e("atomiclegacy:powder_iodine", 1, 1, 1),
                e("atomiclegacy:powder_bromine", 1, 1, 1),
                e("atomiclegacy:steel_poles", 1, 4, 8),
                e("atomiclegacy:steel_scaffold", 1, 3, 8),
                e("atomiclegacy:pole_top", 1, 1, 4),
                e("atomiclegacy:pole_satellite_receiver", 1, 1, 7),
                e("atomiclegacy:scrap", 1, 3, 10),
                e("atomiclegacy:dust", 2, 4, 9),
                e("atomiclegacy:bottle_opener", 1, 1, 2),
                e("atomiclegacy:bottle_nuka", 1, 3, 4),
                e("atomiclegacy:bottle_cherry", 1, 1, 2),
                e("atomiclegacy:stealth_boy", 1, 1, 1),
                e("atomiclegacy:cap_nuka", 1, 15, 7),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "carpet"),
                e("atomiclegacy:gas_mask_filter", 1, 1, 2)
        );
        // ItemPoolsLegacy.POOL_EXPENSIVE
        pool("POOL_EXPENSIVE",
                e("atomiclegacy:chlorine_pinwheel", 1, 1, 1),
                e("atomiclegacy:circuit_vacuum_tube", 1, 1, 4),
                e("atomiclegacy:circuit_analog", 1, 1, 3),
                e("atomiclegacy:circuit_chip", 1, 1, 2),
                e("atomiclegacy:gun_kit_1", 1, 3, 6),
                e("atomiclegacy:gun_kit_2", 1, 2, 3),
                e("atomiclegacy:gun_panzerschreck", 1, 1, 4),
                e("atomiclegacy:ammo_standard", 1, 4, 5, "rocket_he"),
                e("atomiclegacy:ammo_standard", 1, 1, 5, "g26_flare_supply"),
                e("atomiclegacy:ammo_standard", 1, 1, 3, "g26_flare_weapon"),
                e("atomiclegacy:grenade_universal", 1, 1, 2, "nuke:nuclear:s7"),
                e("atomiclegacy:grenade_universal", 1, 3, 3, "frag:cluster:s7"),
                e("atomiclegacy:grenade_extra_triplex", 1, 1, 1),
                e("atomiclegacy:stealth_boy", 1, 1, 2),
                e("atomiclegacy:battery_pack", 1, 1, 1, "battery_lithium"),
                e("atomiclegacy:syringe_awesome", 1, 1, 1),
                e("atomiclegacy:fusion_core", 1, 1, 4),
                e("atomiclegacy:bottle_nuka", 1, 3, 6),
                e("atomiclegacy:bottle_quantum", 1, 1, 3),
                e("atomiclegacy:red_barrel", 1, 1, 6),
                e("atomiclegacy:canister_full", 1, 2, 2, "diesel"),
                e("atomiclegacy:canister_full", 1, 2, 3, "biofuel"),
                e("atomiclegacy:gas_mask_m65", 1, 1, 5, "damage:60"),
                e("atomiclegacy:bomb_caller", 1, 1, 2, "carpet"),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "napalm"),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "chlorine"),
                e("atomiclegacy:gas_mask_filter", 1, 1, 4),
                e("atomiclegacy:launch_code_piece", 1, 1, 1),
                e("atomiclegacy:gun_double_barrel", 1, 1, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1, "discover")
        );
        // ItemPoolsLegacy.POOL_NUKE_TRASH
        pool("POOL_NUKE_TRASH",
                e("atomiclegacy:nugget_u238", 3, 12, 5),
                e("atomiclegacy:nugget_pu240", 3, 8, 5),
                e("atomiclegacy:nugget_neptunium", 1, 4, 3),
                e("atomiclegacy:rod", 1, 1, 3, "u238"),
                e("atomiclegacy:rod_dual", 1, 1, 3, "u238"),
                e("atomiclegacy:rod_quad", 1, 1, 3, "u238"),
                e("atomiclegacy:bottle_quantum", 1, 1, 1),
                e("atomiclegacy:gas_mask_m65", 1, 1, 5, "damage:60"),
                e("atomiclegacy:hazmat_kit", 1, 1, 1),
                e("atomiclegacy:gas_mask_filter", 1, 1, 5),
                e("atomiclegacy:yellow_barrel", 1, 1, 2)
        );
        // ItemPoolsLegacy.POOL_NUKE_MISC
        pool("POOL_NUKE_MISC",
                e("atomiclegacy:nugget_u235", 3, 12, 5),
                e("atomiclegacy:nugget_pu238", 3, 12, 5),
                e("atomiclegacy:nugget_ra226", 3, 6, 5),
                e("atomiclegacy:rod", 1, 1, 3, "u235"),
                e("atomiclegacy:rod_dual", 1, 1, 3, "u235"),
                e("atomiclegacy:rod_quad", 1, 1, 3, "u235"),
                e("atomiclegacy:rod_zirnox", 1, 1, 4, "uranium_fuel"),
                e("atomiclegacy:rod_zirnox", 1, 1, 4, "mox_fuel"),
                e("atomiclegacy:rod_zirnox", 1, 1, 3, "lithium_fuel"),
                e("atomiclegacy:rod_zirnox", 1, 1, 3, "thorium_fuel"),
                e("atomiclegacy:rod_dual", 1, 1, 3, "thf"),
                e("atomiclegacy:rod_zirnox_tritium", 1, 1, 1),
                e("atomiclegacy:rod_zirnox", 1, 1, 1, "u233_fuel"),
                e("atomiclegacy:rod_zirnox", 1, 1, 1, "u235_fuel"),
                e("atomiclegacy:pellet_rtg", 1, 1, 3),
                e("atomiclegacy:powder_thorium", 1, 1, 1),
                e("atomiclegacy:powder_neptunium", 1, 1, 1),
                e("atomiclegacy:powder_strontium", 1, 1, 1),
                e("atomiclegacy:powder_cobalt", 1, 1, 1),
                e("atomiclegacy:bottle_quantum", 1, 1, 1),
                e("atomiclegacy:gas_mask_m65", 1, 1, 5, "damage:60"),
                e("atomiclegacy:hazmat_kit", 1, 1, 2),
                e("atomiclegacy:gas_mask_filter", 1, 1, 5),
                e("atomiclegacy:yellow_barrel", 1, 3, 3)
        );
        // ItemPoolsLegacy.POOL_VERTIBIRD
        pool("POOL_VERTIBIRD",
                e("atomiclegacy:t51_helmet", 1, 1, 15),
                e("atomiclegacy:t51_plate", 1, 1, 15),
                e("atomiclegacy:t51_legs", 1, 1, 15),
                e("atomiclegacy:t51_boots", 1, 1, 15),
                e("atomiclegacy:fusion_core", 1, 1, 10),
                e("atomiclegacy:gun_light_revolver", 1, 1, 4),
                e("atomiclegacy:gun_kit_1", 2, 3, 4),
                e("atomiclegacy:ammo_standard", 1, 24, 4, "m357_fmj"),
                e("atomiclegacy:ammo_standard", 1, 6, 3, "g40_he"),
                e("atomiclegacy:ammo_standard", 1, 1, 5, "g26_flare_weapon"),
                e("atomiclegacy:rod", 1, 1, 2, "u235"),
                e("atomiclegacy:billet_uranium_fuel", 1, 1, 2),
                e("atomiclegacy:ingot_uranium_fuel", 1, 1, 2),
                e("atomiclegacy:bottle_nuka", 1, 3, 6),
                e("atomiclegacy:bottle_quantum", 1, 1, 3),
                e("atomiclegacy:stealth_boy", 1, 1, 7),
                e("atomiclegacy:gas_mask_m65", 1, 1, 5),
                e("atomiclegacy:gas_mask_filter", 1, 1, 5),
                e("atomiclegacy:grenade_universal", 1, 2, 2, "nuke:nuclear:s7"),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "carpet"),
                e("atomiclegacy:bomb_caller", 1, 1, 1, "napalm"),
                e("atomiclegacy:bomb_caller", 1, 1, 2, "chlorine")
        );
        // ItemPoolsLegacy.POOL_SPACESHIP
        pool("POOL_SPACESHIP",
                e("atomiclegacy:battery_pack", 1, 1, 2, "battery_lead"),
                e("atomiclegacy:coil_copper", 2, 16, 5),
                e("atomiclegacy:wire_red_copper", 8, 32, 5),
                e("atomiclegacy:cell_deuterium", 1, 8, 5),
                e("atomiclegacy:cell_tritium", 1, 8, 5),
                e("atomiclegacy:cell_antimatter", 1, 1, 1),
                e("atomiclegacy:powder_neodymium", 1, 1, 1),
                e("atomiclegacy:powder_niobium", 1, 1, 1),
                e("atomiclegacy:wire_dense_red_copper", 2, 4, 5),
                e("atomiclegacy:wire_dense_gold", 1, 3, 5),
                e("atomiclegacy:pwr_fuel", 1, 2, 5),
                e("atomiclegacy:block_tungsten", 3, 8, 5),
                e("atomiclegacy:red_wire_coated", 4, 8, 5),
                e("atomiclegacy:red_cable", 8, 16, 5)
        );
        // ItemPoolsRedRoom.POOL_RED_PEDESTAL
        pool("POOL_RED_PEDESTAL",
                e("atomiclegacy:ballistic_gauntlet", 1, 1, 10),
                e("atomiclegacy:armor_polish", 1, 1, 10),
                e("atomiclegacy:bandaid", 1, 1, 10),
                e("atomiclegacy:serum", 1, 1, 10),
                e("atomiclegacy:quartz_plutonium", 1, 1, 10),
                e("atomiclegacy:morning_glory", 1, 1, 10),
                e("atomiclegacy:spider_milk", 1, 1, 10),
                e("atomiclegacy:ink", 1, 1, 10),
                e("atomiclegacy:heart_container", 1, 1, 10),
                e("atomiclegacy:black_diamond", 1, 1, 10),
                e("atomiclegacy:scrumpy", 1, 1, 10),
                e("atomiclegacy:wild_p", 1, 1, 5),
                e("atomiclegacy:card_aos", 1, 1, 5),
                e("atomiclegacy:card_qos", 1, 1, 5),
                e("atomiclegacy:starmetal_sword", 1, 1, 5),
                e("atomiclegacy:gem_alexandrite", 1, 1, 5),
                e("atomiclegacy:crackpipe", 1, 1, 5),
                e("atomiclegacy:flask_infusion", 1, 1, 5),
                e("atomiclegacy:boxcar", 1, 1, 5),
                e("atomiclegacy:book_of_", 1, 1, 5),
                e("atomiclegacy:gun_hangman", 1, 1, 1),
                e("atomiclegacy:gun_mas36", 1, 1, 1),
                e("atomiclegacy:item_secret_folly", 1, 1, 1),
                e("atomiclegacy:weapon_mod_special_nickel", 1, 1, 1),
                e("atomiclegacy:weapon_mod_special_doubloons", 1, 1, 1)
        );
        // ItemPoolsRedRoom.POOL_BLACK_SLAB
        pool("POOL_BLACK_SLAB",
                e("atomiclegacy:clay_tablet", 1, 1, 10)
        );
        // ItemPoolsRedRoom.POOL_BLACK_PART
        pool("POOL_BLACK_PART",
                e("atomiclegacy:item_secret_selenium_steel", 4, 4, 10),
                e("atomiclegacy:item_secret_controller", 1, 1, 10),
                e("atomiclegacy:item_secret_canister", 1, 1, 10),
                e("atomiclegacy:blueprint_folder", 1, 1, 1, "secret")
        );
        // ItemPoolsSatellite.POOL_SAT_MINER
        pool("POOL_SAT_MINER",
                e("atomiclegacy:powder_aluminium", 3, 3, 10),
                e("atomiclegacy:powder_iron", 3, 3, 10),
                e("atomiclegacy:powder_titanium", 2, 2, 8),
                e("atomiclegacy:crystal_tungsten", 2, 2, 7),
                e("atomiclegacy:powder_coal", 4, 4, 15),
                e("atomiclegacy:powder_uranium", 2, 2, 5),
                e("atomiclegacy:powder_plutonium", 1, 1, 5),
                e("atomiclegacy:powder_thorium", 2, 2, 7),
                e("atomiclegacy:powder_desh_mix", 3, 3, 5),
                e("atomiclegacy:powder_diamond", 2, 2, 7),
                e("minecraft:redstone", 5, 5, 15),
                e("atomiclegacy:powder_nitan_mix", 2, 2, 5),
                e("atomiclegacy:powder_power", 2, 2, 5),
                e("atomiclegacy:powder_copper", 5, 5, 15),
                e("atomiclegacy:powder_lead", 3, 3, 10),
                e("atomiclegacy:fluorite", 4, 4, 15),
                e("atomiclegacy:powder_lapis", 4, 4, 10),
                e("atomiclegacy:crystal_aluminium", 1, 1, 5),
                e("atomiclegacy:crystal_gold", 1, 1, 5),
                e("atomiclegacy:crystal_phosphorus", 1, 1, 10),
                e("atomiclegacy:gravel_diamond", 1, 1, 3),
                e("atomiclegacy:crystal_uranium", 1, 1, 3),
                e("atomiclegacy:crystal_plutonium", 1, 1, 3),
                e("atomiclegacy:crystal_trixite", 1, 1, 1),
                e("atomiclegacy:crystal_starmetal", 1, 1, 1),
                e("atomiclegacy:crystal_lithium", 2, 2, 4)
        );
        // ItemPoolsSatellite.POOL_SAT_LUNAR
        pool("POOL_SAT_LUNAR",
                e("atomiclegacy:moon_turf", 48, 48, 5),
                e("atomiclegacy:moon_turf", 32, 32, 7),
                e("atomiclegacy:moon_turf", 16, 16, 5),
                e("atomiclegacy:powder_lithium", 3, 3, 5),
                e("atomiclegacy:powder_iron", 3, 3, 5),
                e("atomiclegacy:crystal_iron", 1, 1, 1),
                e("atomiclegacy:crystal_lithium", 1, 1, 1)
        );
        // ItemPoolsSingle.POOL_VAULT_RUSTY
        pool("POOL_VAULT_RUSTY",
                e("minecraft:gold_ingot", 3, 14, 1),
                e("atomiclegacy:gun_heavy_revolver", 1, 1, 2),
                e("atomiclegacy:pin", 8, 8, 1),
                e("atomiclegacy:gun_am180", 1, 1, 1),
                e("atomiclegacy:bottle_quantum", 1, 3, 1),
                e("atomiclegacy:ingot_cobalt", 4, 12, 1),
                e("atomiclegacy:ammo_standard", 24, 48, 1, "bmg50_fmj"),
                e("atomiclegacy:ammo_standard", 48, 64, 2, "p9_jhp"),
                e("atomiclegacy:circuit_chip", 3, 6, 1),
                e("atomiclegacy:gas_mask_m65", 1, 1, 1),
                e("atomiclegacy:grenade_universal", 1, 1, 1, "frag:he:s3"),
                e("atomiclegacy:grenade_universal", 1, 1, 1, "frag:inc:s3"),
                e("minecraft:diamond", 1, 2, 1)
        );
        // ItemPoolsSingle.POOL_VAULT_STANDARD
        pool("POOL_VAULT_STANDARD",
                e("atomiclegacy:ingot_desh", 2, 6, 1),
                e("atomiclegacy:powder_desh_mix", 1, 5, 1),
                e("minecraft:diamond", 3, 6, 1),
                e("atomiclegacy:ammo_standard", 1, 1, 1, "nuke_standard"),
                e("atomiclegacy:ammo_container", 1, 1, 1),
                e("atomiclegacy:grenade_universal", 1, 1, 1, "nuke:nuclear:s7"),
                e("atomiclegacy:grenade_universal", 1, 6, 1, "tech:emp:s3"),
                e("atomiclegacy:powder_yellowcake", 16, 24, 1),
                e("atomiclegacy:gun_uzi", 1, 1, 1),
                e("atomiclegacy:circuit_vacuum_tube", 12, 16, 1),
                e("atomiclegacy:circuit_chip", 2, 6, 1)
        );
        // ItemPoolsSingle.POOL_VAULT_REINFORCED
        pool("POOL_VAULT_REINFORCED",
                e("atomiclegacy:ingot_desh", 6, 16, 1),
                e("atomiclegacy:powder_power", 1, 5, 1),
                e("atomiclegacy:sat_chip", 1, 1, 1),
                e("minecraft:diamond", 5, 9, 1),
                e("atomiclegacy:ammo_standard", 1, 3, 1, "nuke_standard"),
                e("atomiclegacy:ammo_container", 1, 4, 1),
                e("atomiclegacy:grenade_universal", 1, 2, 1, "nuke:nuclear:s7"),
                e("atomiclegacy:grenade_universal", 1, 1, 1, "stick:he:impact:triplex"),
                e("atomiclegacy:powder_yellowcake", 26, 42, 1),
                e("atomiclegacy:gun_heavy_revolver", 1, 1, 1),
                e("atomiclegacy:circuit_chip", 18, 32, 1),
                e("atomiclegacy:circuit_basic", 6, 12, 1)
        );
        // ItemPoolsSingle.POOL_VAULT_UNBREAKABLE
        pool("POOL_VAULT_UNBREAKABLE",
                e("atomiclegacy:ammo_container", 3, 6, 1),
                e("atomiclegacy:ammo_standard", 2, 3, 1, "nuke_demo"),
                e("atomiclegacy:gun_carbine", 1, 1, 1),
                e("atomiclegacy:ammo_standard", 16, 32, 1, "r762_du"),
                e("atomiclegacy:gun_congolake", 1, 1, 1),
                e("atomiclegacy:circuit_advanced", 6, 12, 1)
        );
        // ItemPoolsSingle.POOL_METEORITE_TREASURE
        pool("POOL_METEORITE_TREASURE",
                e("atomiclegacy:cobalt_pickaxe", 1, 1, 10),
                e("atomiclegacy:ingot_zirconium", 1, 16, 10),
                e("atomiclegacy:ingot_niobium", 1, 16, 10),
                e("atomiclegacy:ingot_cobalt", 1, 16, 10),
                e("atomiclegacy:ingot_boron", 1, 16, 10),
                e("atomiclegacy:ingot_starmetal", 1, 1, 5),
                e("atomiclegacy:crystal_gold", 1, 4, 10),
                e("atomiclegacy:circuit_vacuum_tube", 4, 8, 10),
                e("atomiclegacy:circuit_chip", 2, 4, 10),
                e("atomiclegacy:definitelyfood", 16, 32, 25),
                e("atomiclegacy:crate_can", 1, 3, 10),
                e("atomiclegacy:pill_herbal", 1, 2, 10),
                e("atomiclegacy:serum", 1, 1, 5),
                e("atomiclegacy:heart_piece", 1, 1, 5),
                e("atomiclegacy:scrumpy", 1, 1, 5),
                e("atomiclegacy:launch_code_piece", 1, 1, 5),
                e("atomiclegacy:egg_glyphid", 1, 1, 5),
                e("atomiclegacy:gem_alexandrite", 1, 1, 1),
                e("atomiclegacy:blueprint_folder", 1, 1, 1, "discover")
        );
        // ItemPoolsSingle.POOL_BLUEPRINTS
        pool("POOL_BLUEPRINTS",
                e("atomiclegacy:blueprint_folder", 1, 1, 10),
                e("atomiclegacy:blueprint_folder", 1, 1, 5, "discover"),
                e("atomiclegacy:blueprint_folder", 1, 1, 1)
        );
        // ItemPoolsSingle.POOL_BLUEPRINTS
        pool("POOL_BLUEPRINTS",
                e("atomiclegacy:blueprint_folder", 1, 1, 10),
                e("atomiclegacy:blueprint_folder", 1, 1, 5, "discover"),
                e("atomiclegacy:blueprint_folder", 1, 1, 1)
        );
        // ItemPoolsVendingMachine.POOL_SODA
        pool("POOL_SODA",
                e("atomiclegacy:bottle_nuka", 1, 1, 10),
                e("atomiclegacy:bottle_cherry", 1, 1, 5),
                e("atomiclegacy:bottle_quantum", 1, 1, 1),
                e("atomiclegacy:can_bepis", 1, 1, 10),
                e("atomiclegacy:can_luna", 1, 1, 10),
                e("atomiclegacy:can_mug", 1, 1, 10),
                e("atomiclegacy:can_breen", 1, 1, 1)
        );
        // ItemPoolsVendingMachine.POOL_SNACKS
        pool("POOL_SNACKS",
                e("atomiclegacy:definitelyfood", 1, 1, 10),
                e("atomiclegacy:canned_conserve", 1, 1, 5, "beef"),
                e("atomiclegacy:canned_conserve", 1, 1, 5, "tube"),
                e("atomiclegacy:twinkie", 1, 1, 10),
                e("atomiclegacy:chocolate", 1, 1, 10)
        );
    }

    private static Entry e(String id, int min, int max, int weight) {
        return new Entry(id, min, max, weight, null);
    }

    private static Entry e(String id, int min, int max, int weight, String variant) {
        return new Entry(id, min, max, weight, variant);
    }

    private static void pool(String name, Entry... entries) {
        POOLS.put(name, List.of(entries));
    }

    private record Entry(String id, int min, int max, int weight, String variant) {
        private Entry(String id, int min, int max, int weight) {
            this(id, min, max, weight, null);
        }

        private boolean exists() {
            ResourceLocation location = ResourceLocation.tryParse(id);
            return location != null && BuiltInRegistries.ITEM.containsKey(location);
        }

        private ItemStack stack(RandomSource random) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            Item item = location != null && BuiltInRegistries.ITEM.containsKey(location) ? BuiltInRegistries.ITEM.get(location) : Items.AIR;
            if (item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            int low = Math.max(0, min);
            int high = Math.max(low, max);
            int count = random == null || high == low ? low : low + random.nextInt(high - low + 1);
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack;
            if ("atomiclegacy:blueprint_folder".equals(id) && variant != null) {
                BlueprintFolderItem.Variant folderVariant = switch (variant) {
                    case "discover" -> BlueprintFolderItem.Variant.DISCOVER;
                    case "secret" -> BlueprintFolderItem.Variant.SECRET; // [parity R3: loot] meta 2
                    default -> BlueprintFolderItem.Variant.STANDARD;
                };
                stack = BlueprintFolderItem.stackFor(folderVariant);
            } else if (variant != null && item instanceof HbmFluidContainerItem fluidItem && fluidItem.isFilledContainer()) {
                // 1.7.10 stored the fluid as the old metadata value.  The
                // modern container keeps that value in its fluid component;
                // do not silently turn a full canister into the default fluid.
                var fluid = HbmFluids.byName(variant);
                stack = fluid.isPresent() ? fluidItem.filledStack(fluid.get()) : ItemStack.EMPTY;
            } else if (variant != null && item instanceof LegacyBombCallerItem) {
                try {
                    stack = LegacyBombCallerItem.stackFor(LegacyBombCallerItem.Type.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    stack = new ItemStack(item, count);
                }
            } else if (variant != null && item instanceof UniversalGrenadeItem) {
                String[] parts = variant.split(":", -1);
                if (parts.length >= 3) {
                    try {
                        UniversalGrenadeItem.Shell shell = UniversalGrenadeItem.Shell.valueOf(parts[0].toUpperCase(java.util.Locale.ROOT));
                        UniversalGrenadeItem.Filling filling = UniversalGrenadeItem.Filling.valueOf(parts[1].toUpperCase(java.util.Locale.ROOT));
                        UniversalGrenadeItem.Fuze fuze = UniversalGrenadeItem.Fuze.valueOf(parts[2].toUpperCase(java.util.Locale.ROOT));
                        UniversalGrenadeItem.Extra extra = parts.length > 3 && !parts[3].isBlank()
                                ? UniversalGrenadeItem.Extra.valueOf(parts[3].toUpperCase(java.util.Locale.ROOT)) : null;
                        stack = UniversalGrenadeItem.make(shell, filling, fuze, extra);
                    } catch (IllegalArgumentException ignored) {
                        stack = new ItemStack(item, count);
                    }
                } else {
                    stack = new ItemStack(item, count);
                }
            } else if (variant != null && item instanceof RtgDepletedPelletItem pellet) {
                stack = RtgDepletedPelletItem.stackFor(pellet, variant);
            } else if (variant != null && item instanceof StampBookItem stampBook) {
                try {
                    stack = StampBookItem.stackFor(stampBook,
                            com.atomiclegacy.items.StampItem.StampType.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    stack = new ItemStack(item, count);
                }
            } else if (variant != null && item instanceof com.atomiclegacy.items.LegacyConserveItem) {
                // [parity R3: loot] ItemConserve metadata = EnumFoodType ordinal.
                try {
                    stack = com.atomiclegacy.items.LegacyConserveItem.stackFor(item,
                            com.atomiclegacy.items.LegacyConserveItem.Variant.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    stack = ItemStack.EMPTY;
                }
            } else if ("empty".equals(variant) && item instanceof com.atomiclegacy.items.BlowtorchItem torch) {
                // [parity R3: loot] ItemBlowtorch.getEmptyTool: the loot torch carries no gas.
                stack = torch.emptyTool(new ItemStack(item));
            } else if (variant != null && variant.startsWith("damage:")) {
                // [parity R3: loot] plain 1.7.10 metadata on a damageable item (gas_mask_m65 meta 60).
                stack = new ItemStack(item);
                stack.setDamageValue(Integer.parseInt(variant.substring("damage:".length())));
            } else if (variant != null && item instanceof LegacyVariantItem) {
                stack = LegacyVariantItem.stackFor(item, variant);
            } else {
                stack = new ItemStack(item, count);
            }
            stack.setCount(count);
            return stack;
        }
    }
}

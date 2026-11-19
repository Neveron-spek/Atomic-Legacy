package com.atomiclegacy.tileentity;

import com.atomiclegacy.items.LegacyConserveItem;
import com.atomiclegacy.items.LegacyVariantItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 1.7.10 pedestal recipe table and its exact nine-position matcher.
 *
 * Firearm entries from the old table are intentionally omitted because HBM's
 * combat firearm system is retired in favour of TACZ.
 */
public final class LegacyPedestalRecipes {
    private static final int SLOT_COUNT = 9;
    private static final List<Recipe> RECIPES = createRecipes();

    private LegacyPedestalRecipes() {
    }

    /** [parity g2] registration order of PedestalRecipes#registerDefaults (936c60c); output item id[/variant]. */
    private static final List<String> REGISTRATION_ORDER = List.of(
            "gun_light_revolver_dani", "gun_maresleg_broken", "gun_heavy_revolver_lilmac", "gun_heavy_revolver_protege",
            "gun_amat_subtlety", "gun_amat_penance", "gun_flamer_daybreaker", "gun_autoshotgun_sexy",
            "gun_minigun_lacunae", "gun_laser_pistol_morning_glory", "gun_folly", "gun_aberrator", "gun_aberrator_eott",
            "ammo_secret/folly_sm", "ammo_secret/folly_nuke", "ammo_secret/p35_800", "ammo_secret/p35_800_bl");

    /** [parity g2] PedestalRecipes.recipeSets[set] in registration order (GUIScreenClayTablet picks one by index). */
    public static List<Recipe> recipeSet(int set) {
        List<Recipe> list = new ArrayList<>();
        for (Recipe recipe : RECIPES) {
            if (recipe.set() == set) list.add(recipe);
        }
        list.sort(java.util.Comparator.comparingInt(recipe -> {
            String key = recipe.output().itemId + (recipe.output().variant == null ? "" : "/" + recipe.output().variant);
            int index = REGISTRATION_ORDER.indexOf(key);
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        return list;
    }

    /** Number of PedestalRecipes.recipeSets. */
    public static final int RECIPE_SET_COUNT = 2;

    public static Optional<Recipe> find(Level level, BlockPos center) {
        if (!level.hasNeighborSignal(center)) {
            return Optional.empty();
        }

        ItemStack[] stacks = new ItemStack[SLOT_COUNT];
        int[][] offsets = {
                {-2, -2}, {0, -3}, {2, -2},
                {-3, 0}, {0, 0}, {3, 0},
                {-2, 2}, {0, 3}, {2, 2}
        };
        for (int i = 0; i < offsets.length; i++) {
            if (level.getBlockEntity(center.offset(offsets[i][0], 0, offsets[i][1]))
                    instanceof LegacyDisplayStandBlockEntity pedestal) {
                stacks[i] = pedestal.displayedItem();
            } else {
                stacks[i] = ItemStack.EMPTY;
            }
        }

        for (Recipe recipe : RECIPES) {
            if (!recipe.available() || !recipe.condition().matches(level, center)) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (!recipe.input()[i].matches(stacks[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static void consume(Level level, BlockPos center, Recipe recipe) {
        int[][] offsets = {
                {-2, -2}, {0, -3}, {2, -2},
                {-3, 0}, {0, 0}, {3, 0},
                {-2, 2}, {0, 3}, {2, 2}
        };
        for (int i = 0; i < offsets.length; i++) {
            if (i == 4 || recipe.input()[i].empty()) {
                continue;
            }
            if (level.getBlockEntity(center.offset(offsets[i][0], 0, offsets[i][1]))
                    instanceof LegacyDisplayStandBlockEntity pedestal) {
                pedestal.clearDisplayedItem();
            }
        }
        if (level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity pedestal) {
            pedestal.setDisplayedItem(recipe.result());
        }
    }

    private static List<Recipe> createRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        // weapons batch C1: PedestalRecipes gun_light_revolver_dani
        recipes.add(recipe(item("gun_light_revolver_dani", 1),
                empty(), tag("plates/lead", 1), empty(),
                tag("plates/gold", 1), item("gun_light_revolver", 1), tag("plates/gold", 1),
                empty(), tag("plates/lead", 1), empty(), Condition.NONE, 0));
        // [C2] PedestalRecipes gun_heavy_revolver_lilmac / gun_heavy_revolver_protege (XFactory44)
        recipes.add(recipe(item("gun_heavy_revolver_lilmac", 1),
                empty(), item("weapon_mod_special_scope", 1), empty(),
                item("powder_magic", 1), item("gun_heavy_revolver", 1), tag("plates/weaponsteel", 1),
                empty(), tag("gun_parts/grip/bone", 1), item("minecraft:apple", 3), Condition.NONE, 0));
        recipes.add(recipe(item("gun_heavy_revolver_protege", 1),
                item("dungeon_chain", 16), tag("gems/cinnabar", 1), item("dungeon_chain", 16),
                item("scrap_nuclear", 1), item("gun_heavy_revolver", 1), item("scrap_nuclear", 1),
                item("dungeon_chain", 16), tag("gems/cinnabar", 1), item("dungeon_chain", 16), Condition.NONE, 0));
        // [weapons batch D2] PedestalRecipes gun_amat_subtlety. gun_amat_penance is not registered: its
        // weapon_mod_special SILENCER and FURNITURE_BLACK inputs do not exist in the port.
        recipes.add(recipe(item("gun_amat_subtlety", 1),
                tag("ingots/starmetal", 1), tag("plates/cast_aluminium", 1), tag("ingots/starmetal", 1),
                tag("plates/cast_aluminium", 1), item("gun_amat", 1), tag("plates/cast_aluminium", 1),
                tag("ingots/starmetal", 1), tag("plates/cast_aluminium", 1), tag("ingots/starmetal", 1), Condition.NONE, 0));
        // [weapon mods G2: pedestal] PedestalRecipes gun_amat_penance (SILENCER and FURNITURE_BLACK mods)
        recipes.add(recipe(item("gun_amat_penance", 1),
                tag("ingots/starmetal", 1), tag("plates/cast_dura_steel", 1), tag("ingots/starmetal", 1),
                item("weapon_mod_special_silencer", 1), item("gun_amat", 1), item("weapon_mod_special_furniture_black", 1),
                tag("ingots/starmetal", 1), tag("plates/cast_dura_steel", 1), tag("ingots/starmetal", 1), Condition.NONE, 0));
        // [weapons batch C4] PedestalRecipes gun_maresleg_broken
        recipes.add(recipe(item("gun_maresleg_broken", 1),
                item("barbed_wire", 1), tag("plates/weaponsteel", 1), item("barbed_wire", 1),
                tag("plates/weaponsteel", 1), item("gun_maresleg", 1), tag("plates/weaponsteel", 1),
                item("barbed_wire", 1), tag("plates/weaponsteel", 1), item("barbed_wire", 1), Condition.NONE, 0));
        // [weapons batch D1] PedestalRecipes gun_autoshotgun_sexy
        recipes.add(recipe(item("gun_autoshotgun_sexy", 1),
                item("bolt_spike", 16), item("wild_p", 1), item("bolt_spike", 16),
                item("card_qos", 1), item("gun_autoshotgun", 1), item("card_aos", 1),
                item("bolt_spike", 16), tag("ingots/starmetal", 16), item("bolt_spike", 16), Condition.NONE, 0));
        // [weapons batch C3] PedestalRecipes gun_minigun_lacunae (FULL_MOON, set 0)
        recipes.add(recipe(item("gun_minigun_lacunae", 1),
                empty(), item("powder_magic", 4), empty(),
                item("item_secret_selenium_steel", 4), item("gun_minigun", 1), item("item_secret_selenium_steel", 4),
                empty(), item("powder_magic", 4), empty(), Condition.FULL_MOON, 0));
        // [weapons batch E2] PedestalRecipes gun_laser_pistol_morning_glory
        recipes.add(recipe(item("gun_laser_pistol_morning_glory", 1),
                empty(), item("morning_glory", 1), empty(),
                item("item_secret_selenium_steel", 2), item("gun_laser_pistol", 1), item("item_secret_selenium_steel", 2),
                empty(), tag("gems/emerald", 16), empty(), Condition.NONE, 0));
        // [weapons batch F4] PedestalRecipes gun_folly (FULL_MOON, set 1), gun_aberrator (set 1), gun_aberrator_eott (GOOD_KARMA, set 1).
        // BIGMT.mechanism() is the saturnite mechanism (as gun_g3_zebra in batch D2), STAR = starmetal, BSCCO = bscco.
        recipes.add(recipe(item("gun_folly", 1),
                item("item_secret_folly", 4), item("item_secret_controller", 2), item("item_secret_folly", 4),
                tag("ingots/bscco", 16), tag("storage_blocks/starmetal", 64), tag("ingots/bscco", 16),
                item("item_secret_folly", 4), item("item_secret_controller", 2), item("item_secret_folly", 4), Condition.FULL_MOON, 1));
        recipes.add(recipe(item("gun_aberrator", 1),
                empty(), item("item_secret_aberrator", 1), empty(),
                item("item_secret_aberrator", 1), tag("gun_parts/mechanism/saturnite", 4), item("item_secret_aberrator", 1),
                empty(), item("item_secret_aberrator", 1), empty(), Condition.NONE, 1));
        recipes.add(recipe(item("gun_aberrator_eott", 1),
                item("item_secret_aberrator", 1), item("item_secret_aberrator", 1), item("item_secret_aberrator", 1),
                item("item_secret_aberrator", 1), tag("gun_parts/mechanism/saturnite", 16), item("item_secret_aberrator", 1),
                item("item_secret_aberrator", 1), item("item_secret_aberrator", 1), item("item_secret_aberrator", 1), Condition.GOOD_KARMA, 1));
        // [weapons batch F1] PedestalRecipes gun_flamer_daybreaker (PedestalExtraCondition.SUN): GOLD.plateCast, canned SLIME, P_WHITE.ingot, stick_dynamite
        recipes.add(recipe(item("gun_flamer_daybreaker", 1),
                tag("plates/cast_gold", 1), conserve("slime", 1), tag("plates/cast_gold", 1),
                tag("ingots/phosphorus", 1), item("gun_flamer", 1), tag("ingots/phosphorus", 1),
                tag("plates/cast_gold", 1), item("stick_dynamite", 1), tag("plates/cast_gold", 1), Condition.SUN, 0));
        recipes.add(recipe(variant("ammo_secret", "folly_sm", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1),
                item("powder_magic", 1), item("chunk_ore_moonstone", 1), item("powder_magic", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1), Condition.FULL_MOON, 1));
        recipes.add(recipe(variant("ammo_secret", "folly_nuke", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1),
                item("powder_magic", 1), variant("ammo_standard", "nuke_high", 4), item("powder_magic", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1), Condition.FULL_MOON, 1));
        recipes.add(recipe(variant("ammo_secret", "p35_800", 5),
                empty(), empty(), empty(), empty(), item("item_secret_aberrator", 1), empty(), empty(), empty(), empty(), Condition.NONE, 1));
        recipes.add(recipe(variant("ammo_secret", "p35_800_bl", 10),
                empty(), empty(), empty(), empty(), item("item_secret_aberrator", 3), empty(), empty(), empty(), empty(), Condition.NONE, 1));

        return List.copyOf(recipes);
    }

    private static Recipe recipe(Spec output, Spec a, Spec b, Spec c, Spec d, Spec e, Spec f, Spec g, Spec h, Spec i,
                                 Condition condition, int set) {
        return new Recipe(output, new Spec[]{a, b, c, d, e, f, g, h, i}, condition, set);
    }

    private static Spec empty() {
        return new Spec(null, null, null, 0);
    }

    private static Spec item(String id, int count) {
        return new Spec(id, null, null, count);
    }

    private static Spec variant(String id, String variant, int count) {
        return new Spec(id, variant, null, count);
    }

    private static Spec tag(String path, int count) {
        return new Spec(null, null, path, count);
    }

    private static Spec conserve(String variant, int count) {
        return new Spec("canned_conserve", variant, null, count);
    }

    public record Recipe(Spec output, Spec[] input, Condition condition, int set) {
        boolean available() {
            return output.available() && java.util.Arrays.stream(input).allMatch(Spec::available);
        }

        ItemStack result() {
            return output.stack();
        }

        /** [parity g2] AStack#extractForNEI of input slot index; null when the original input is null. */
        public List<ItemStack> displayInput(int index) {
            Spec spec = input[index];
            if (spec.empty()) return null;
            if (spec.tagPath != null) {
                List<ItemStack> stacks = new ArrayList<>();
                BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", spec.tagPath)))
                        .forEach(holder -> stacks.add(new ItemStack(holder, spec.count)));
                return stacks;
            }
            ItemStack stack = spec.stack();
            return stack.isEmpty() ? List.of() : List.of(stack);
        }

        public ItemStack displayOutput() {
            return output.stack();
        }

        /** PedestalExtraCondition name. */
        public String extraName() {
            return condition.name();
        }
    }

    private record Spec(String itemId, String variant, String tagPath, int count) {
        boolean empty() {
            return count == 0;
        }

        boolean available() {
            if (empty() || tagPath != null) {
                return true;
            }
            ResourceLocation id = resolve(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                return false;
            }
            return true;
        }

        boolean matches(ItemStack stack) {
            if (empty()) {
                return stack.isEmpty();
            }
            if (stack.isEmpty() || stack.getCount() != count) {
                return false;
            }
            if (tagPath != null) {
                return stack.is(TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("c", tagPath)));
            }
            ResourceLocation id = resolve(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || !stack.is(BuiltInRegistries.ITEM.get(id))) {
                return false;
            }
            if (variant == null) {
                return true;
            }
            return variantOf(stack).equals(variant);
        }

        ItemStack stack() {
            if (empty()) {
                return ItemStack.EMPTY;
            }
            ResourceLocation id = resolve(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            ItemStack stack;
            if (item instanceof LegacyVariantItem && variant != null) {
                stack = LegacyVariantItem.stackFor(item, variant);
                stack.setCount(count);
            } else if (item instanceof LegacyConserveItem && variant != null) {
                stack = LegacyConserveItem.stackFor(item, LegacyConserveItem.Variant.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                stack.setCount(count);
            } else {
                stack = new ItemStack(item, count);
            }
            return stack;
        }

        private static String variantOf(ItemStack stack) {
            if (stack.getItem() instanceof LegacyVariantItem legacy) {
                return legacy.variant(stack).id();
            }
            if (stack.getItem() instanceof LegacyConserveItem) {
                return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("conserve");
            }
            return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("variant");
        }

        private static ResourceLocation resolve(String id) {
            if (id == null) {
                return null;
            }
            return id.indexOf(':') >= 0
                    ? ResourceLocation.tryParse(id)
                    : ResourceLocation.fromNamespaceAndPath("atomiclegacy", id);
        }
    }

    /** Diagnostics access to the 1.7.10 PedestalExtraCondition checks. */
    public static boolean conditionMatches(String condition, Level level, BlockPos center) {
        return Condition.valueOf(condition).matches(level, center);
    }

    enum Condition {
        NONE {
            @Override boolean matches(Level level, BlockPos center) { return true; }
        },
        FULL_MOON {
            @Override boolean matches(Level level, BlockPos center) {
                return isNightWindow(level) && level.getMoonPhase() == 0;
            }
        },
        NEW_MOON {
            @Override boolean matches(Level level, BlockPos center) {
                return isNightWindow(level) && level.getMoonPhase() == 4;
            }
        },
        SUN {
            @Override boolean matches(Level level, BlockPos center) {
                float angle = level.getTimeOfDay(0.0F);
                return angle <= 0.15F || angle >= 0.85F;
            }
        },
        // BlockPedestal: any player in AABB(x, y, z, x + 1, y + 1, z + 1).expand(20, 20, 20) with
        // HbmPlayerProps.reputation >= 10 (GOOD_KARMA) or <= -10 (BAD_KARMA); reputation comes from the Behemoth lantern.
        GOOD_KARMA {
            @Override boolean matches(Level level, BlockPos center) {
                return nearbyPlayers(level, center).stream().anyMatch(p -> com.atomiclegacy.player.HbmPlayerReputation.get(p) >= 10);
            }
        },
        BAD_KARMA {
            @Override boolean matches(Level level, BlockPos center) {
                return nearbyPlayers(level, center).stream().anyMatch(p -> com.atomiclegacy.player.HbmPlayerReputation.get(p) <= -10);
            }
        };

        private static List<net.minecraft.world.entity.player.Player> nearbyPlayers(Level level, BlockPos center) {
            return level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, new net.minecraft.world.phys.AABB(center).inflate(20.0D));
        }

        abstract boolean matches(Level level, BlockPos center);

        private static boolean isNightWindow(Level level) {
            float angle = level.getTimeOfDay(0.0F);
            return angle >= 0.35F && angle <= 0.65F;
        }
    }
}

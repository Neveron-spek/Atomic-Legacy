package com.atomiclegacy.event;

import com.mojang.authlib.GameProfile;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.items.FueledArmorFSBItem;
import com.atomiclegacy.items.PoweredArmorFSBItem;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.util.LegacyDamageResistance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * [parity I1] Armor sets and the damage resistance system against 1.7.10: MainRegistry/ModItemsArmor/ModItems armor
 * materials, ArmorFSB full set bonuses and flags, util/DamageResistanceHandler.initDefaults (DT/DR per category and
 * exact type, applied through the real LivingIncomingDamageEvent chain), handler/HazmatRegistry, ArmorFSBPowered /
 * ArmorFSBFueled damage handling, ArmorFSB.addInformation and crafting/ArmorRecipes.
 */
public final class ParityI1Diagnostics {
    private static final double X = -20000.5D, Y = 300.0D, Z = 20000.5D;
    private static final String[] PIECES = {"helmet", "plate", "legs", "boots"};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ParityI1Diagnostics() {
    }

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        FakePlayer fake = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("5a1b9c2e-0d4f-4e1a-9b3c-11a1d0000001"), "[parity_i1]"));
        fake.setPos(X, Y, Z);
        try {
            materials(problems, cases);
            effects(problems, cases);
            features(fake, problems, cases);
            resistances(fake, problems, cases);
            radiation(fake, problems, cases);
            charge(level, fake, problems, cases);
            tooltips(fake, problems, cases);
            recipes(server, problems, cases);
        } catch (Throwable t) {
            StackTraceElement[] trace = t.getStackTrace();
            problems.add("parity I1: exception " + t + (trace.length > 0 ? " at " + trace[0] : "") + (trace.length > 1 ? " < " + trace[1] : ""));
        }
        undress(fake);
        return cases[0];
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String what) {
        cases[0]++;
        if (!ok) problems.add("parity I1: " + what);
    }

    private static Item it(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static void undress(FakePlayer fake) {
        for (EquipmentSlot slot : SLOTS) fake.setItemSlot(slot, ItemStack.EMPTY);
    }

    private static void dress(FakePlayer fake, String... items) {
        for (int i = 0; i < 4; i++) fake.setItemSlot(SLOTS[i], items[i] == null ? ItemStack.EMPTY : new ItemStack(it(items[i])));
    }

    private static String[] set(String prefix) {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) out[i] = prefix + "_" + PIECES[i];
        return out;
    }

    /// MATERIALS: EnumHelper.addArmorMaterial(name, durability, {helmet, chest, legs, boots}, enchantability) + customCraftingMaterial ///
    private static void materials(List<String> problems, int[] cases) {
        Object[][] mats = {
                {"steel", 30, new int[] {3, 8, 6, 3}, 5, "ingot_steel"},
                {"titanium", 25, new int[] {3, 8, 6, 3}, 9, "ingot_titanium"},
                {"alloy", 40, new int[] {3, 8, 6, 3}, 12, null},
                {"cobalt", 70, new int[] {3, 8, 6, 3}, 60, "ingot_cobalt"},
                {"security", 100, new int[] {3, 8, 6, 3}, 15, "plate_kevlar"},
                {"starmetal", 150, new int[] {3, 8, 6, 3}, 100, "ingot_starmetal"},
                {"robes", 15, new int[] {2, 5, 4, 1}, 12, "minecraft:iron_ingot"},
                {"dnt", 3, new int[] {1, 1, 1, 1}, 0, "ingot_dineutronium"},
                {"cmb", 60, new int[] {3, 8, 6, 3}, 50, "ingot_combine_steel"},
                {"schrabidium", 100, new int[] {3, 8, 6, 3}, 50, "ingot_schrabidium"},
                {"bismuth", 100, new int[] {3, 8, 6, 3}, 100, "plate_bismuth"},
                {"euphemium", 0, new int[] {3, 8, 6, 3}, 100, null},
                {"hazmat", 60, new int[] {2, 5, 4, 1}, 5, "hazmat_cloth"},
                {"hazmat_paa", 75, new int[] {3, 8, 6, 3}, 25, "plate_paa"},
                {"asbestos", 20, new int[] {1, 4, 3, 1}, 5, "asbestos_cloth"},
                {"liquidator", 750, new int[] {3, 8, 6, 3}, 10, "plate_lead"},
                {"t51", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_titanium"},
                {"steamsuit", 150, new int[] {3, 8, 6, 3}, 0, "ingot_desh"},
                {"dieselsuit", 150, new int[] {3, 8, 6, 3}, 0, "plate_copper"},
                {"ajr", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_ajr"},
                {"ajro", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_ajr"},
                {"rpa", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_ajr"},
                {"ncrpa", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_ajr"},
                {"bj", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_lunar"},
                {"envsuit", 150, new int[] {3, 8, 6, 3}, 10, "plate_armor_hev"},
                {"hev", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_hev"},
                {"fau", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_fau"},
                {"dns", 150, new int[] {3, 8, 6, 3}, 0, "plate_armor_dnt"},
                {"taurun", 150, new int[] {3, 8, 6, 3}, 10, "plate_iron"},
                {"trenchmaster", 150, new int[] {3, 8, 6, 3}, 0, "plate_iron"},
        };
        int[] base = {11, 16, 15, 13};
        for (Object[] m : mats) {
            String prefix = (String) m[0];
            for (int i = 0; i < 4; i++) checkPiece(problems, cases, prefix + "_" + PIECES[i], i, (int) m[1], ((int[]) m[2])[i], (int) m[3], (String) m[4], base);
        }
        for (String c : new String[] {"_red", "_grey"}) {
            for (int i = 0; i < 4; i++) checkPiece(problems, cases, "hazmat_" + PIECES[i] + c, i, 60, new int[] {2, 5, 4, 1}[i], 5, "hazmat_cloth" + c, base);
        }
        for (int i = 1; i < 4; i++) checkPiece(problems, cases, "paa_" + PIECES[i], i, 75, new int[] {3, 8, 6, 3}[i], 25, "plate_paa", base);
        checkPiece(problems, cases, "bj_plate_jetpack", 1, 150, 8, 0, "plate_armor_lunar", base);
        checkPiece(problems, cases, "zirconium_legs", 2, 1000, 3, 1000, "ingot_zirconium", base);
        checkPiece(problems, cases, "jackt", 1, 30, 8, 5, "ingot_steel", base);
        checkPiece(problems, cases, "jackt2", 1, 30, 8, 5, "ingot_steel", base);
    }

    private static void checkPiece(List<String> problems, int[] cases, String id, int slot, int factor, int defense, int ench, String repair, int[] base) {
        Item item = it(id);
        if (!(item instanceof ArmorItem armor)) {
            check(problems, cases, false, id + " is an ArmorItem");
            return;
        }
        ItemStack stack = new ItemStack(item);
        check(problems, cases, armor.getEquipmentSlot() == SLOTS[slot], id + " slot " + SLOTS[slot]);
        check(problems, cases, armor.getDefense() == defense, id + " defense " + defense + " (is " + armor.getDefense() + ")");
        check(problems, cases, armor.getMaterial().value().enchantmentValue() == ench, id + " enchantability " + ench + " (is " + armor.getMaterial().value().enchantmentValue() + ")");
        int durability = factor * base[slot];
        check(problems, cases, stack.getMaxDamage() == durability, id + " durability " + durability + " (is " + stack.getMaxDamage() + ")");
        Ingredient ingredient = armor.getMaterial().value().repairIngredient().get();
        if (repair == null) {
            check(problems, cases, ingredient.isEmpty(), id + " is not repairable");
        } else {
            Item expected = repair.contains(":") ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(repair)) : it(repair);
            check(problems, cases, ingredient.test(new ItemStack(expected)) && ingredient.getItems().length == 1, id + " repaired only with " + repair);
        }
    }

    /// FULL SET EFFECTS: ArmorFSB.addEffect(new PotionEffect(id, duration, amplifier)) of the helmet, cloned to every piece ///
    private record E(Holder<MobEffect> effect, int duration, int amplifier) {
    }

    private static void effects(List<String> problems, int[] cases) {
        Map<String, List<E>> exp = new HashMap<>();
        exp.put("schrabidium", List.of(new E(MobEffects.DIG_SPEED, 20, 2), new E(MobEffects.DAMAGE_BOOST, 20, 2), new E(MobEffects.JUMP, 20, 1), new E(MobEffects.MOVEMENT_SPEED, 20, 2)));
        exp.put("bismuth", List.of(new E(MobEffects.JUMP, 20, 6), new E(MobEffects.MOVEMENT_SPEED, 20, 6), new E(MobEffects.REGENERATION, 20, 1), new E(MobEffects.NIGHT_VISION, 300, 0)));
        exp.put("cmb", List.of(new E(MobEffects.MOVEMENT_SPEED, 20, 2), new E(MobEffects.DIG_SPEED, 20, 2), new E(MobEffects.DAMAGE_BOOST, 20, 4)));
        exp.put("t51", List.of(new E(MobEffects.DAMAGE_BOOST, 20, 0)));
        exp.put("steamsuit", List.of(new E(MobEffects.DIG_SPEED, 20, 4)));
        exp.put("dieselsuit", List.of(new E(MobEffects.MOVEMENT_SPEED, 20, 2), new E(MobEffects.JUMP, 20, 2)));
        exp.put("ajr", List.of(new E(MobEffects.JUMP, 20, 0), new E(MobEffects.DAMAGE_BOOST, 20, 0)));
        exp.put("ajro", exp.get("ajr"));
        exp.put("rpa", List.of(new E(MobEffects.DAMAGE_BOOST, 20, 3)));
        exp.put("ncrpa", exp.get("rpa"));
        exp.put("bj", List.of(new E(MobEffects.MOVEMENT_SPEED, 20, 1), new E(MobEffects.JUMP, 20, 0), new E(MobEffects.SATURATION, 20, 0), new E(HbmMobEffects.RADX, 20, 0)));
        exp.put("envsuit", List.of(new E(MobEffects.MOVEMENT_SPEED, 20, 1), new E(MobEffects.JUMP, 20, 0)));
        exp.put("hev", exp.get("envsuit"));
        exp.put("fau", List.of(new E(MobEffects.JUMP, 20, 1)));
        exp.put("dns", List.of(new E(MobEffects.DAMAGE_BOOST, 20, 9), new E(MobEffects.DIG_SPEED, 20, 7), new E(MobEffects.JUMP, 20, 2)));
        exp.put("taurun", List.of(new E(MobEffects.DAMAGE_BOOST, 20, 0)));
        exp.put("trenchmaster", List.of(new E(MobEffects.DAMAGE_BOOST, 20, 2), new E(MobEffects.DIG_SPEED, 20, 1), new E(MobEffects.JUMP, 20, 1), new E(MobEffects.MOVEMENT_SPEED, 20, 0)));
        for (String s : new String[] {"steel", "titanium", "alloy", "cobalt", "security", "starmetal", "robes", "dnt", "hazmat", "hazmat_paa", "asbestos", "liquidator"}) exp.put(s, List.of());
        exp.forEach((prefix, list) -> {
            List<String> ids = new ArrayList<>(List.of(set(prefix)));
            if (prefix.equals("bj")) ids.add("bj_plate_jetpack");
            for (String id : ids) {
                if (!(it(id) instanceof ArmorFSBItem fsb)) {
                    check(problems, cases, false, id + " is an ArmorFSB");
                    continue;
                }
                List<E> actual = fsb.fullSetEffects().stream().map(e -> new E(e.effect(), e.duration(), e.amplifier())).toList();
                check(problems, cases, actual.equals(list), id + " full set effects " + list + " (is " + actual + ")");
            }
        });
        for (String id : new String[] {"paa_plate", "paa_legs", "paa_boots"}) {
            List<E> actual = ((ArmorFSBItem) it(id)).fullSetEffects().stream().map(e -> new E(e.effect(), e.duration(), e.amplifier())).toList();
            check(problems, cases, actual.equals(List.of(new E(MobEffects.DIG_SPEED, 20, 0))), id + " haste 0 for 20 ticks");
        }
    }

    /// FLAGS: enableVATS / enableThermalSight / setHasGeigerSound / setHasCustomGeiger / setHasHardLanding / setStepSize / setDashCount ///
    private static void features(FakePlayer fake, List<String> problems, int[] cases) {
        Object[][] flags = {
                {"t51", "VGH", 0, 0}, {"steamsuit", "H", 0, 0}, {"dieselsuit", "VT", 0, 0}, {"ajr", "VGH", 0, 0}, {"ajro", "VGH", 0, 0},
                {"rpa", "VGH", 0, 0}, {"ncrpa", "VGH", 0, 0}, {"bj", "VTGH", 0, 0}, {"hev", "GC", 0, 0}, {"fau", "GTH", 0, 0}, {"dns", "GVTH", 0, 0},
                {"dnt", "", 0, 0}, {"bismuth", "", 0, 3}, {"taurun", "", 1, 0}, {"trenchmaster", "V", 1, 0}, {"steel", "", 0, 0}, {"envsuit", "", 0, 0},
        };
        for (Object[] f : flags) {
            dress(fake, set((String) f[0]));
            ArmorFSBItem.FeatureProfile p = ArmorFSBItem.features(fake);
            String fl = (String) f[1];
            boolean ok = p.vats() == fl.contains("V") && p.thermal() == fl.contains("T") && p.geigerSound() == fl.contains("G")
                    && p.customGeiger() == fl.contains("C") && p.hardLanding() == fl.contains("H") && p.stepHeight() == (int) f[2] && p.dashCount() == (int) f[3];
            check(problems, cases, ok, f[0] + " set flags " + fl + " step " + f[2] + " dash " + f[3] + " (is " + p + ")");
        }
        // hasFSBArmor compares ArmorMaterial: aMatAJR is shared by ajr/ajro/rpa/ncrpa, aMatPaa by paa/hazmat_paa
        dress(fake, "ajr_helmet", "rpa_plate", "ncrpa_legs", "ajro_boots");
        check(problems, cases, ArmorFSBItem.hasFSBArmor(fake) && ArmorFSBItem.activeSetGroup(fake).equals("rpa"), "mixed aMatAJR pieces form an rpa set");
        dress(fake, null, "paa_plate", "hazmat_paa_legs", "hazmat_paa_boots");
        check(problems, cases, ArmorFSBItem.hasFSBArmor(fake), "paa_plate (no helmet) + hazmat_paa legs/boots is a full set");
        dress(fake, "taurun_helmet", "trenchmaster_plate", "trenchmaster_legs", "trenchmaster_boots");
        check(problems, cases, !ArmorFSBItem.hasFSBArmor(fake), "taurun and trenchmaster use different materials");
        undress(fake);
    }

    /// DAMAGE RESISTANCE: DamageResistanceHandler.initDefaults ///
    private record R(float dt, float dr) {
    }

    private static final class Spec {
        final Map<String, R> cat = new HashMap<>();
        final Map<String, R> exact = new HashMap<>();
        R other;

        Spec c(String k, float dt, float dr) { cat.put(k, new R(dt, dr)); return this; }
        Spec x(String k, float dt, float dr) { exact.put(k.toLowerCase(Locale.US), new R(dt, dr)); return this; }
        Spec o(float dt, float dr) { other = new R(dt, dr); return this; }

        R get(String exactKey, String category, boolean unblockable) {
            if (exact.containsKey(exactKey)) return exact.get(exactKey);
            if (cat.containsKey(category)) return cat.get(category);
            return unblockable ? null : other;
        }
    }

    private static void resistances(FakePlayer fake, List<String> problems, int[] cases) {
        Map<String, Spec> sets = new HashMap<>();
        sets.put("steel", new Spec().c("PHYS", 2, 0.1F));
        sets.put("titanium", new Spec().c("PHYS", 3, 0.1F));
        sets.put("alloy", new Spec().c("PHYS", 2, 0.1F));
        sets.put("cobalt", new Spec().c("PHYS", 2, 0.1F));
        sets.put("starmetal", new Spec().c("PHYS", 3, 0.25F).o(1, 0.1F));
        sets.put("dnt", new Spec());
        sets.put("cmb", new Spec().c("PHYS", 5, 0.5F).o(5, 0.25F));
        sets.put("schrabidium", new Spec().c("PHYS", 10, 0.65F).o(5, 0.5F));
        sets.put("robes", new Spec());
        sets.put("security", new Spec().c("PHYS", 5, 0.5F).c("EXPL", 2, 0.25F));
        sets.put("steamsuit", new Spec().c("PHYS", 2, 0.15F).c("FIRE", 0.5F, 0.25F).x("fall", 5, 0.25F).o(0, 0.1F));
        sets.put("dieselsuit", new Spec().c("PHYS", 1, 0.15F).c("FIRE", 0.5F, 0.5F).c("EXPL", 2, 0.15F).o(0, 0.1F));
        sets.put("t51", new Spec().c("PHYS", 2, 0.15F).c("FIRE", 0.5F, 0.35F).c("EXPL", 5, 0.25F).x("fall", 0, 1).o(0, 0.1F));
        Spec ajr = new Spec().c("PHYS", 4, 0.15F).c("FIRE", 0.5F, 0.35F).c("EXPL", 7.5F, 0.25F).x("fall", 0, 1).o(0, 0.15F);
        sets.put("ajr", ajr);
        sets.put("ajro", ajr);
        sets.put("rpa", new Spec().c("PHYS", 50, 0.75F).c("FIRE", 25, 0.9F).c("EXPL", 15, 0.25F).c("EN", 30, 0.8F).x("fall", 0, 1).o(15, 0.45F));
        sets.put("ncrpa", new Spec().c("PHYS", 25, 0.65F).c("FIRE", 10, 0.9F).c("EXPL", 15, 0.25F).c("EN", 10, 0.5F).x("fall", 0, 1).o(15, 0.25F));
        Spec bj = new Spec().c("PHYS", 5, 0.5F).c("FIRE", 2.5F, 0.5F).c("EXPL", 10, 0.25F).x("fall", 0, 1).o(2, 0.15F);
        sets.put("bj", bj);
        sets.put("envsuit", new Spec().c("FIRE", 2, 0.75F).x("drown", 0, 1).x("fall", 5, 0.75F).o(0, 0.1F));
        sets.put("hev", new Spec().c("PHYS", 2, 0.25F).c("FIRE", 0.5F, 0.5F).c("EXPL", 5, 0.25F).x("onFire", 0, 1).x("fall", 10, 0).o(2, 0.25F));
        sets.put("bismuth", new Spec().c("PHYS", 2, 0.15F).c("FIRE", 5, 0.5F).c("EXPL", 5, 0.25F).x("fall", 0, 1).o(2, 0.25F));
        sets.put("fau", new Spec().c("PHYS", 100, 0.99F).c("EXPL", 50, 0.95F).c("FIRE", 100, 1).x("LASER", 25, 0.95F).x("fall", 0, 1).o(100, 0.99F));
        sets.put("dns", new Spec().c("PHYS", 1000, 1).c("EXPL", 100, 0.99F).c("FIRE", 0, 1).o(1000, 1));
        sets.put("taurun", new Spec().c("PHYS", 2, 0.15F).c("FIRE", 0, 0.25F).c("EXPL", 0, 0.25F).x("fall", 4, 0.5F).o(2, 0.1F));
        sets.put("trenchmaster", new Spec().c("PHYS", 5, 0.5F).c("FIRE", 5, 0.5F).c("EXPL", 5, 0.25F).x("LASER", 15, 0.9F).x("fall", 10, 0.5F).o(5, 0.25F));
        sets.put("euphemium", new Spec().o(1_000_000F, 1));
        sets.put("asbestos", new Spec().c("FIRE", 10, 0.9F));
        for (String s : new String[] {"hazmat", "liquidator", "hazmat_paa"}) sets.put(s, new Spec());

        Map<String, String[]> wear = new HashMap<>();
        sets.keySet().forEach(k -> wear.put(k, set(k)));
        wear.put("bj_jetpack", new String[] {"bj_helmet", "bj_plate_jetpack", "bj_legs", "bj_boots"});
        sets.put("bj_jetpack", bj);
        wear.put("hazmat_red", new String[] {"hazmat_helmet_red", "hazmat_plate_red", "hazmat_legs_red", "hazmat_boots_red"});
        sets.put("hazmat_red", new Spec());
        wear.put("zirconium", new String[] {"zirconium_legs", "zirconium_legs", "zirconium_legs", "zirconium_legs"});
        sets.put("zirconium", new Spec().o(0, 1));

        for (Map.Entry<String, Spec> e : sets.entrySet()) {
            String name = e.getKey();
            Spec spec = e.getValue();
            dress(fake, wear.get(name));
            // sedna bullet classes (BulletConfig damage class -> exact name / typeToCategory)
            for (String cls : new String[] {"PHYSICAL", "FIRE", "EXPLOSIVE", "LASER", "PLASMA", "ELECTRIC", "SUBATOMIC"}) {
                String cat = switch (cls) { case "PHYSICAL" -> "PHYS"; case "FIRE" -> "FIRE"; case "EXPLOSIVE" -> "EXPL"; default -> "EN"; };
                R r = spec.get(cls.toLowerCase(Locale.US), cat, false);
                float[] act = LegacyDamageResistance.getDTDR(fake, cls);
                float edt = r == null ? 0 : r.dt, edr = r == null ? 0 : r.dr;
                check(problems, cases, act[0] == edt && act[1] == edr, name + " " + cls + " DT/DR " + edt + "/" + edr + " (is " + act[0] + "/" + act[1] + ")");
            }
            // vanilla sources through the real LivingIncomingDamageEvent chain (onEntityAttacked + onEntityDamaged)
            if (name.equals("trenchmaster")) continue; // 1/3 random cancel of ArmorTrenchmaster.handleAttack
            var src = fake.damageSources();
            Object[][] sources = {
                    {src.cactus(), "cactus", "PHYS", false}, {src.inFire(), "infire", "FIRE", false}, {src.explosion(null, null), "explosion", "EXPL", false},
                    {src.fall(), "fall", "fall", true}, {src.drown(), "drown", "drown", true}, {src.onFire(), "onfire", "FIRE", true},
                    {src.generic(), "generic", "generic", true}, {src.starve(), "starve", "starve", true},
            };
            for (Object[] s : sources) {
                DamageSource source = (DamageSource) s[0];
                boolean absolute = s[1].equals("starve");
                R r = absolute ? null : spec.get((String) s[1], (String) s[2], (boolean) s[3]);
                float amount = 20F;
                boolean cancel = r != null && ((r.dt > 0 && r.dt >= amount) || r.dr >= 1F);
                float expected = r == null ? amount : (r.dt >= amount ? 0F : (amount - r.dt) * (1F - r.dr));
                if (name.equals("dns") && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) cancel = true; // ArmorDNT.handleAttack
                DamageContainer container = new DamageContainer(source, amount);
                boolean cancelled = CommonHooks.onEntityIncomingDamage(fake, container);
                float got = container.getNewDamage();
                check(problems, cases, cancelled == cancel && (cancel || Math.abs(got - expected) < 1.0E-4F),
                        name + " " + s[1] + " 20 dmg -> " + (cancel ? "cancelled" : expected) + " (is " + (cancelled ? "cancelled" : got) + ")");
            }
        }
        // armor piercing: calculateDamage(dt - pierceDT, dr * clamp(1 - pierce, 0, 2))
        check(problems, cases, LegacyDamageResistance.calculateDamage(new float[] {10F, 0.5F}, 20F, 4F, 0.5F) == (20F - 6F) * (1F - 0.25F), "calculateDamage piercing");
        check(problems, cases, LegacyDamageResistance.calculateDamage(new float[] {2F, 0.5F}, 20F, 0F, -1F) == 18F * 0F, "calculateDamage negative piercing doubles DR");
        // jackt / jackt2 item stats, creeper innate stats
        dress(fake, null, "jackt", null, null);
        float[] j = LegacyDamageResistance.getDTDR(fake, "PHYSICAL");
        check(problems, cases, j[0] == 1F && j[1] == 0.2F, "jackt PHYS 1/20% (is " + j[0] + "/" + j[1] + ")");
        dress(fake, null, "jackt2", null, null);
        j = LegacyDamageResistance.getDTDR(fake, "PHYSICAL");
        check(problems, cases, j[0] == 2F && j[1] == 0.25F, "jackt2 PHYS 2/25% (is " + j[0] + "/" + j[1] + ")");
        undress(fake);
    }

    /// HAZMATREGISTRY: base value * helmet 0.2 / chest 0.4 / legs 0.3 / boots 0.1 ///
    private static void radiation(FakePlayer fake, List<String> problems, int[] cases) {
        Object[][] values = {
                {"hazmat", 0.6D}, {"liquidator", 2.4D}, {"hazmat_paa", 1.7D}, {"security", 0.825D}, {"starmetal", 1D}, {"steel", 0.045D},
                {"titanium", 0.045D}, {"cobalt", 0.125D}, {"alloy", 0.07D}, {"cmb", 1.3D}, {"schrabidium", 3D}, {"euphemium", 10D},
                {"t51", 1D}, {"steamsuit", 1.3D}, {"ajr", 1.3D}, {"ajro", 1.3D}, {"rpa", 2D}, {"ncrpa", 1.7D}, {"bj", 1D}, {"envsuit", 1D},
                {"hev", 2.3D}, {"fau", 4D}, {"dns", 5D}, {"taurun", 0.125D}, {"trenchmaster", 1D},
                {"dieselsuit", 0D}, {"dnt", 0D}, {"robes", 0D}, {"bismuth", 0D}, {"asbestos", 0D},
        };
        double[] coef = {0.2D, 0.4D, 0.3D, 0.1D};
        for (Object[] v : values) {
            String[] ids = set((String) v[0]);
            for (int i = 0; i < 4; i++) radCheck(fake, problems, cases, ids[i], (double) v[1] * coef[i]);
        }
        for (int i = 0; i < 4; i++) {
            radCheck(fake, problems, cases, "hazmat_" + PIECES[i] + "_red", 1D * coef[i]);
            radCheck(fake, problems, cases, "hazmat_" + PIECES[i] + "_grey", 2D * coef[i]);
        }
        for (int i = 1; i < 4; i++) radCheck(fake, problems, cases, "paa_" + PIECES[i], 1.7D * coef[i]);
        radCheck(fake, problems, cases, "bj_plate_jetpack", 0.4D);
        radCheck(fake, problems, cases, "jackt", 0.1D);
        radCheck(fake, problems, cases, "jackt2", 0.1D);
        radCheck(fake, problems, cases, "zirconium_legs", 0D);
    }

    private static void radCheck(FakePlayer fake, List<String> problems, int[] cases, String id, double expected) {
        double got = HbmArmorProtection.itemRadiationResistance(new ItemStack(it(id)), fake.registryAccess());
        check(problems, cases, Math.abs(got - expected) < 1.0E-9D, id + " radiation resistance " + expected + " (is " + got + ")");
    }

    /// ArmorFSBPowered / ArmorFSBFueled.setDamage: damage * consumption ///
    private static void charge(ServerLevel level, FakePlayer fake, List<String> problems, int[] cases) {
        Object[][] powered = {{"t51_plate", 1_000_000L, 1_000L}, {"ajr_plate", 2_500_000L, 2_000L}, {"bj_plate", 10_000_000L, 1_000L},
                {"envsuit_plate", 100_000L, 250L}, {"hev_plate", 1_000_000L, 2_500L}, {"fau_plate", 10_000_000L, 2_500L}, {"dns_plate", 1_000_000_000L, 100_000L}};
        for (Object[] p : powered) {
            ItemStack stack = new ItemStack(it((String) p[0]));
            PoweredArmorFSBItem item = (PoweredArmorFSBItem) stack.getItem();
            check(problems, cases, item.hbmCharge(stack) == (long) p[1], p[0] + " starts at " + p[1] + " HE");
            stack.hurtAndBreak(2, level, fake, x -> { });
            check(problems, cases, item.hbmCharge(stack) == (long) p[1] - 2 * (long) p[2] && stack.getDamageValue() == 0,
                    p[0] + " 2 armor damage drains " + 2 * (long) p[2] + " HE (is " + item.hbmCharge(stack) + ", damage " + stack.getDamageValue() + ")");
        }
        for (String id : new String[] {"steamsuit_plate", "dieselsuit_plate"}) {
            ItemStack stack = new ItemStack(it(id));
            FueledArmorFSBItem item = (FueledArmorFSBItem) stack.getItem();
            stack.hurtAndBreak(2, level, fake, x -> { });
            check(problems, cases, item.fuel(stack) == 64_000 - 100 && stack.getDamageValue() == 0, id + " 2 armor damage burns 100 mB (is " + item.fuel(stack) + ")");
        }
    }

    /// ArmorFSB.addInformation / ArmorFSBPowered.addInformation / DamageResistanceHandler.addInfo ///
    private static void tooltips(FakePlayer fake, List<String> problems, int[] cases) {
        ItemStack t51 = new ItemStack(it("t51_plate"));
        List<Component> lines = new ArrayList<>();
        t51.getItem().appendHoverText(t51, net.minecraft.world.item.Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        List<String> text = lines.stream().map(Component::getString).toList();
        List<String> expected = List.of("Charge: 1.0M / 1.0M", Component.translatable("armor.fullSetBonus").getString(),
                Component.translatable(MobEffects.DAMAGE_BOOST.value().getDescriptionId()).getString(),
                "  " + Component.translatable("armor.geigerSound").getString(), "  " + Component.translatable("armor.vats").getString(),
                "  " + Component.translatable("armor.hardLanding").getString());
        check(problems, cases, text.equals(expected), "t51_plate tooltip " + expected + " (is " + text + ")");
        ItemStack steel = new ItemStack(it("steel_helmet"));
        lines.clear();
        steel.getItem().appendHoverText(steel, net.minecraft.world.item.Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        check(problems, cases, lines.isEmpty(), "steel_helmet has no full set bonus lines");
        lines.clear();
        LegacyDamageResistance.addInfo(steel, lines);
        text = lines.stream().map(Component::getString).toList();
        check(problems, cases, text.size() == 6 && text.get(0).equals(Component.translatable("damage.inset").getString())
                && text.get(5).equals(Component.translatable("damage.category.PHYS").getString() + ": 2.0/10%"), "steel_helmet resistance tooltip (is " + text + ")");
    }

    /// crafting/ArmorRecipes ///
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        var rm = server.getRecipeManager();
        for (String p : PIECES) {
            check(problems, cases, rm.byKey(AtomicLegacy.id("legacy_rpa_" + p)).isEmpty(), "rpa_" + p + " has no crafting recipe (PrecAssRecipes only)");
            check(problems, cases, rm.byKey(AtomicLegacy.id("precision_assembler/rpa_" + p)).isPresent(), "precass rpa_" + p + " exists");
        }
        Object[][] keys = {
                {"legacy_cobalt_helmet", 0, 1, "steel_helmet", 3, 1}, {"legacy_dnt_helmet", 0, 0, "ingot_dineutronium", 3, 2},
                {"legacy_dieselsuit_plate", 1, 1, "machine_diesel", 3, 3}, {"legacy_dieselsuit_helmet", 0, 0, "minecraft:red_wool", 3, 3},
                {"legacy_fau_helmet", 0, 1, "minecraft:red_wool", 3, 3}, {"legacy_security_plate", 0, 1, "minecraft:black_wool", 3, 3},
                {"legacy_security_legs", 2, 0, "minecraft:lime_wool", 3, 3}, {"legacy_t51_helmet", 2, 0, "ingot_biorubber", 3, 3},
                {"legacy_liquidator_plate", 0, 0, "ingot_biorubber", 3, 3}, {"legacy_robes_boots", 1, 0, "ingot_biorubber", 3, 2},
                {"legacy_security_boots", 1, 0, "ingot_biorubber", 3, 2}, {"legacy_security_helmet", 1, 0, "ingot_biorubber", 3, 2},
                {"legacy_ajr_helmet", 2, 0, "ingot_bakelite", 3, 3}, {"legacy_envsuit_helmet", 2, 0, "ingot_rubber", 3, 3},
        };
        for (Object[] k : keys) {
            var holder = rm.byKey(AtomicLegacy.id((String) k[0]));
            if (holder.isEmpty() || !(holder.get().value() instanceof ShapedRecipe shaped)) {
                check(problems, cases, false, k[0] + " is a shaped recipe");
                continue;
            }
            String id = (String) k[3];
            Item item = id.contains(":") ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)) : it(id);
            int w = shaped.getWidth();
            boolean size = w == (int) k[4] && shaped.getHeight() == (int) k[5];
            boolean ok = size && shaped.getIngredients().get((int) k[1] * w + (int) k[2]).test(new ItemStack(item));
            check(problems, cases, ok, k[0] + " " + w + "x" + shaped.getHeight() + " slot " + k[1] + "," + k[2] + " accepts " + id);
        }
        var dnt = rm.byKey(AtomicLegacy.id("legacy_dnt_plate"));
        check(problems, cases, dnt.isPresent() && dnt.get().value() instanceof ShapedRecipe s
                && !s.getIngredients().get(0).test(new ItemStack(it("ingot_chainsteel"))), "dnt_plate does not take chainsteel");
        check(problems, cases, new ItemStack(Items.IRON_INGOT).getItem() == Items.IRON_INGOT, "sanity");
    }
}

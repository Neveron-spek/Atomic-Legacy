package com.atomiclegacy.event;

import com.mojang.authlib.GameProfile;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.ArmorTableMenu;
import com.atomiclegacy.items.ArmorModItem;
import com.atomiclegacy.items.BallisticGauntletItem;
import com.atomiclegacy.items.GasMaskItem;
import com.atomiclegacy.items.LegacyDefuserArmorModItem;
import com.atomiclegacy.items.LegacyJetpackItem;
import com.atomiclegacy.items.LegacyWingsItem;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.pollution.HbmArmorProtection.HazardClass;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [parity I2] Armor mods, armor table, jetpacks/wings and gas masks against 1.7.10
 * (items/armor/ItemMod*.java, ItemArmorMod.java, handler/ArmorModHandler.java, Jetpack*.java,
 * WingsMurk.java, ArmorGasMask.java, ItemModGasmask.java, tool/ItemFilter.java, util/ArmorUtil.java,
 * util/ArmorRegistry.java, inventory/container/ContainerArmorTable.java, crafting/ConsumableRecipes.java).
 */
public final class ParityI2Diagnostics {
    private static final double X = 6144.5D, Y = 300.0D, Z = 6144.5D;
    private static final int H = ArmorModHandler.HELMET_ONLY, P = ArmorModHandler.PLATE_ONLY, L = ArmorModHandler.LEGS_ONLY,
            B = ArmorModHandler.BOOTS_ONLY, SV = ArmorModHandler.SERVOS, CL = ArmorModHandler.CLADDING,
            K = ArmorModHandler.KEVLAR, EX = ArmorModHandler.EXTRA, BA = ArmorModHandler.BATTERY;

    /** id, slot, helmet, chestplate, leggings, boots (ModItems constructors). */
    private record M(String id, int slot, boolean h, boolean c, boolean l, boolean b) { }

    private static final M[] MODS = {
            new M("attachment_mask", H, true, false, false, false), new M("attachment_mask_mono", H, true, false, false, false),
            new M("back_tesla", P, false, true, false, false),
            new M("servo_set", SV, false, true, true, false), new M("servo_set_desh", SV, false, true, true, false),
            new M("pads_rubber", B, false, false, false, true), new M("pads_slime", B, false, false, false, true), new M("pads_static", B, false, false, false, true),
            new M("cladding_paint", CL, true, true, true, true), new M("cladding_rubber", CL, true, true, true, true), new M("cladding_lead", CL, true, true, true, true),
            new M("cladding_desh", CL, true, true, true, true), new M("cladding_ghiorsium", CL, true, true, true, true),
            new M("cladding_iron", CL, true, true, true, true), new M("cladding_obsidian", CL, true, true, true, true),
            new M("insert_kevlar", K, false, true, false, false), new M("insert_sapi", K, false, true, false, false), new M("insert_esapi", K, false, true, false, false),
            new M("insert_xsapi", K, false, true, false, false), new M("insert_steel", K, false, true, false, false), new M("insert_du", K, false, true, false, false),
            new M("insert_polonium", K, false, true, false, false), new M("insert_ghiorsium", K, false, true, false, false), new M("insert_era", K, false, true, false, false),
            new M("insert_yharonite", K, false, true, false, false), new M("insert_doxium", K, false, true, false, false),
            new M("armor_polish", EX, true, true, true, true), new M("bandaid", EX, true, true, true, true), new M("serum", EX, true, true, true, true),
            new M("quartz_plutonium", EX, true, true, true, true), new M("morning_glory", EX, true, true, true, true),
            new M("lodestone", EX, true, true, true, true), new M("horseshoe_magnet", EX, true, true, true, true), new M("industrial_magnet", EX, true, true, true, true),
            new M("bathwater", EX, true, true, true, true), new M("bathwater_mk2", EX, true, true, true, true),
            new M("spider_milk", EX, true, true, true, true), new M("ink", EX, true, true, true, true),
            new M("heart_piece", EX, false, true, false, false), new M("heart_container", EX, false, true, false, false),
            new M("heart_booster", EX, false, true, false, false), new M("heart_fab", EX, false, true, false, false), new M("black_diamond", EX, false, true, false, false),
            new M("wd40", EX, true, true, true, true), new M("scrumpy", EX, false, false, true, false), new M("wild_p", EX, false, false, true, false),
            new M("shackles", EX, false, false, true, false), new M("injector_5htp", EX, false, true, false, false), new M("injector_knife", EX, false, true, false, false),
            new M("medal_liquidator", P, true, true, false, false), new M("bottled_cloud", P, false, true, false, false),
            new M("protection_charm", H, true, true, false, false), new M("meteor_charm", H, true, true, false, false),
            new M("neutrino_lens", EX, true, false, false, false), new M("gas_tester", EX, true, true, true, true), new M("defuser_gold", EX, true, true, true, true),
            new M("ballistic_gauntlet", SV, false, true, false, false), new M("night_vision", H, true, false, false, false),
            new M("card_aos", H, true, true, false, false), new M("card_qos", H, true, true, false, false), new M("australium_iii", K, false, true, false, false),
            new M("armor_battery", BA, true, true, true, true), new M("armor_battery_mk2", BA, true, true, true, true), new M("armor_battery_mk3", BA, true, true, true, true),
            new M("jetpack_fly", P, false, true, false, false), new M("jetpack_break", P, false, true, false, false),
            new M("jetpack_vector", P, false, true, false, false), new M("jetpack_boost", P, false, true, false, false),
            new M("wings_murk", P, false, true, false, false), new M("wings_limp", P, false, true, false, false),
    };

    private ParityI2Diagnostics() {
    }

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        level.getChunk((int) X >> 4, (int) Z >> 4);
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("5b0f3a59-6e1c-4f4e-9b1e-2f6d1c0a9e12"), "parity_i2"));
        p.setPos(X, Y, Z);
        p.setYRot(0F);
        p.setXRot(0F);

        ItemStack[] armor = {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS)};
        for (M m : MODS) {
            Item item = it(m.id);
            check(problems, cases, item instanceof ArmorModItem, m.id + " is an armor mod");
            if (!(item instanceof ArmorModItem mod)) continue;
            check(problems, cases, mod.slotType() == m.slot, m.id + " slot " + m.slot);
            check(problems, cases, new ItemStack(item).getMaxStackSize() == 1, m.id + " stack size 1");
            boolean[] flags = {m.h, m.c, m.l, m.b};
            for (int i = 0; i < 4; i++) {
                ItemStack piece = armor[i].copy();
                ItemStack modStack = new ItemStack(item);
                boolean ok = ArmorModHandler.isApplicable(piece, modStack);
                check(problems, cases, ok == flags[i], m.id + " applicable to armor type " + i + " = " + flags[i]);
                if (ok) {
                    ArmorModHandler.applyMod(piece, modStack, server.registryAccess());
                    check(problems, cases, ArmorModHandler.pryMods(piece, server.registryAccess())[m.slot].is(item), m.id + " stored in mod_slot_" + m.slot);
                    ArmorModHandler.removeMod(piece, m.slot);
                    check(problems, cases, ArmorModHandler.pryMods(piece, server.registryAccess())[m.slot].isEmpty()
                            && !piece.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA), m.id + " removed, compound cleared");
                }
            }
            // "Applicable To:" header is always present
            check(problems, cases, tooltip(new ItemStack(item)).contains("Applicable To:") && tooltip(new ItemStack(item)).contains("Slot:"), m.id + " common tooltip block");
        }
        check(problems, cases, !BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id("jetpack_glider")), "jetpack_glider is not part of this 1.7.10 commit");

        // tooltips (exact lines)
        String[][] lines = {
                {"pads_rubber", "-50% fall damage"}, {"pads_slime", "-75% fall damage"}, {"pads_static", "Passively charges electric armor when walking"},
                {"insert_kevlar", "-10% projectile damage"}, {"insert_doxium", "+400% damage"}, {"insert_steel", "-5% speed"}, {"insert_polonium", "+100 RAD/s"},
                {"insert_era", "25/25HP"}, {"cladding_lead", "+0.1 rad-resistance"}, {"cladding_rubber", "+0.005 rad-resistance"},
                {"cladding_obsidian", "Makes dropped armor indestructible"}, {"cladding_iron", "+0.5 knockback resistance"},
                {"heart_piece", "+5.0 health"}, {"heart_fab", "+60.0 health"}, {"black_diamond", "Nostalgia"}, {"australium_iii", "+25.0 shield"},
                {"servo_set_desh", "Chestplate: Haste III / Damage +150%"}, {"industrial_magnet", "Item attraction range: 12"},
                {"scrumpy", "1 revives left"}, {"wild_p", "3 revives left"}, {"shackles", "∞ revives left"},
                {"injector_5htp", "Imported from Japsterdam."}, {"wd40", "Highly reduces damage taken by armor, +2 HP"},
                {"gas_tester", "Works in the inventory or when applied to armor"}, {"ballistic_gauntlet", "Punches fire 12 gauge shells"},
                {"card_qos", "Adds a 33% chance to tank damage with no cap."}, {"jetpack_break", "Sneaking will stop hover mode."},
                {"jetpack_fly", "Can be worn on its own!"}, {"jetpack_vector", "0mB / 16000mB"}, {"attachment_mask", "No filter installed!"},
                {"gas_mask_mono", "No filter installed!"}, {"medal_liquidator", "-10 RAD/s"}, {"protection_charm", "Halves broadcaster damage"},
        };
        for (String[] line : lines) {
            check(problems, cases, tooltip(new ItemStack(it(line[0]))).contains(line[1]), line[0] + " tooltip has \"" + line[1] + "\"");
        }
        check(problems, cases, !tooltip(new ItemStack(it("armor_battery"))).contains("capacity"), "armor_battery has no own tooltip line");
        check(problems, cases, tooltip(new ItemStack(it("gas_mask_mono"))).contains(" -"), "gas mask lists never-protected hazards");

        // effects on the fake player
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        DamageSource fall = level.damageSources().fall();
        DamageSource generic = level.damageSources().generic();
        check(problems, cases, Math.abs(mod("pads_rubber").modifyArmorDamage(p, boots, fall, 10F) - 5F) < 1e-4
                && mod("pads_rubber").modifyArmorDamage(p, boots, generic, 10F) == 10F, "pads_rubber halves only fall damage");
        check(problems, cases, Math.abs(mod("pads_slime").modifyArmorDamage(p, boots, fall, 10F) - 2.5F) < 1e-4, "pads_slime 0.25");
        p.removeAllEffects();
        mod("servo_set").tickArmor(p, chest);
        mod("servo_set").tickArmor(p, legs);
        check(problems, cases, eff(p, MobEffects.DIG_SPEED, 60, 0) && eff(p, MobEffects.JUMP, 60, 1), "servo_set: haste I / jump II for 60 ticks");
        check(problems, cases, mod("servo_set").attackDamageMultiplier(chest) == 1.5D && mod("servo_set_desh").attackDamageMultiplier(chest) == 2.5D
                && mod("servo_set").movementMultiplier(legs) == 1.25D && mod("servo_set_desh").movementMultiplier(legs) == 1.5D, "servo modifiers +50/150 % dmg, +25/50 % speed");
        p.removeAllEffects();
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        mod("serum").tickArmor(p, chest);
        check(problems, cases, !p.hasEffect(MobEffects.POISON) && eff(p, MobEffects.DAMAGE_BOOST, 100, 4), "serum: poison -> strength V 100");
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
        mod("morning_glory").tickArmor(p, chest);
        check(problems, cases, !p.hasEffect(MobEffects.WITHER), "morning_glory removes wither");
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        mod("spider_milk").tickArmor(p, chest);
        check(problems, cases, !p.hasEffect(MobEffects.WEAKNESS), "spider_milk removes bad effects");
        HbmLivingRadiation rad = HbmLivingRadiation.get(p);
        rad.setRadiation(0.2F);
        HbmLivingRadiation.set(p, rad);
        mod("medal_liquidator").tickArmor(p, chest);
        check(problems, cases, HbmLivingRadiation.get(p).getRadiation() == 0F, "medal: -0.5 RAD per tick clamped at 0");
        rad = HbmLivingRadiation.get(p);
        rad.setRadiation(15F);
        HbmLivingRadiation.set(p, rad);
        mod("quartz_plutonium").modifyArmorDamage(p, chest, generic, 1F);
        check(problems, cases, Math.abs(HbmLivingRadiation.get(p).getRadiation() - 5F) < 1e-4, "quartz: -10 RAD when hit");
        ItemStack injectorChest = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorModHandler.applyMod(injectorChest, new ItemStack(it("injector_5htp")), server.registryAccess());
        rad = HbmLivingRadiation.get(p);
        rad.setDigamma(6F);
        HbmLivingRadiation.set(p, rad);
        p.removeAllEffects();
        mod("injector_5htp").tickArmor(p, injectorChest);
        check(problems, cases, ArmorModHandler.pryMods(injectorChest, server.registryAccess())[EX].isEmpty()
                && Math.abs(HbmLivingRadiation.get(p).getDigamma() - 1F) < 1e-4 && p.hasEffect(HbmMobEffects.STABILITY), "injector_5htp: -5 digamma, stability, consumed");
        rad = HbmLivingRadiation.get(p);
        rad.setDigamma(0F);
        rad.setRadiation(0F);
        HbmLivingRadiation.set(p, rad);
        check(problems, cases, mod("heart_piece").extraHealth() == 5D && mod("heart_fab").extraHealth() == 60D && mod("wd40").extraHealth() == 4D
                && mod("black_diamond").extraHealth() == 40D, "health mods +5/+60/+40, wd40 +4");
        check(problems, cases, mod("cladding_iron").knockbackResistance(chest) == 0.5D, "cladding_iron +0.5 knockback resistance");
        check(problems, cases, mod("bottled_cloud").movementMultiplier(chest) == 1.125D && mod("bottled_cloud").extraDashes() == 3, "bottled_cloud +12.5 % speed, 3 dashes");
        boolean cancelled = false;
        for (int i = 0; i < 200 && !cancelled; i++) {
            float out = mod("card_qos").modifyArmorDamage(p, armor[0], generic, 4F);
            cancelled = ArmorModItem.consumeCancelRequest() && out == 0F;
        }
        check(problems, cases, cancelled, "card_qos cancels the hurt event");
        check(problems, cases, mod("meteor_charm").modifyArmorDamage(p, armor[0], generic, 4F) == 4F, "charm ignores non-broadcast damage");

        // defuser
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper != null) {
            creeper.moveTo(X + 2, Y, Z);
            creeper.setNoGravity(true);
            level.addFreshEntity(creeper);
            creeper.ignite();
            boolean defused = LegacyDefuserArmorModItem.defuse(creeper, p, false);
            check(problems, cases, defused && !creeper.isIgnited() && creeper.getSwellDir() == -1
                    && creeper.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.SwellGoal),
                    "defuser removes the swell goal and resets ignition");
            creeper.discard();
        }

        // jetpacks: per-tick thrust and fuel (JetpackRegular/Break/Vectorized/Booster)
        LegacyJetpackItem.setInputActive(p, true);
        jet(problems, cases, p, "jetpack_fly", 0D, true, 0.1D, 0D, 5);
        jet(problems, cases, p, "jetpack_vector", 0D, true, 0.1D, 0.1D, 3);
        jet(problems, cases, p, "jetpack_boost", 0D, true, 0.1D, 0.25D, 1);
        jet(problems, cases, p, "jetpack_fly", 0.45D, true, 0.45D, 0D, 5);
        LegacyJetpackItem.setInputActive(p, false);
        // JetpackBreak hover: not on the ground, not sneaking, motionY -0.5 -> -0.4, horizontal x1.025, fuel every 10 ticks
        ItemStack brk = new ItemStack(it("jetpack_break"));
        LegacyJetpackItem.setFuel(brk, 100);
        p.setOnGround(false);
        p.setShiftKeyDown(false);
        p.setDeltaMovement(1.0D, -0.5D, 0D);
        p.tickCount = 20;
        ((LegacyJetpackItem) it("jetpack_break")).onArmorTick(level, p, brk);
        Vec3 v = p.getDeltaMovement();
        check(problems, cases, Math.abs(v.y + 0.4D) < 1e-9 && Math.abs(v.x - 1.025D) < 1e-9 && LegacyJetpackItem.fuel(brk) == 99, "jetpack_break hover");
        ItemStack empty = new ItemStack(it("jetpack_fly"));
        LegacyJetpackItem.setInputActive(p, true);
        p.setDeltaMovement(0D, 0D, 0D);
        ((LegacyJetpackItem) it("jetpack_fly")).onArmorTick(level, p, empty);
        check(problems, cases, p.getDeltaMovement().y == 0D, "no thrust without fuel");
        LegacyJetpackItem.setInputActive(p, false);
        check(problems, cases, ((LegacyJetpackItem) it("jetpack_fly")).capacity() == 12000 && ((LegacyJetpackItem) it("jetpack_break")).capacity() == 12000
                && ((LegacyJetpackItem) it("jetpack_vector")).capacity() == 16000 && ((LegacyJetpackItem) it("jetpack_boost")).capacity() == 32000
                && ((LegacyJetpackItem) it("jetpack_boost")).acceptsFuel("balefire") && ((LegacyJetpackItem) it("jetpack_fly")).acceptsFuel("kerosene"),
                "jetpack tanks 12000/12000/16000/32000 mB, kerosene / balefire");
        ItemStack fill = new ItemStack(it("jetpack_fly"));
        LegacyJetpackItem.setFuel(fill, 11500);
        check(problems, cases, ((LegacyJetpackItem) it("jetpack_fly")).fill(fill, "kerosene", 1000) == 500 && LegacyJetpackItem.fuel(fill) == 12000, "tryFill caps at maxFuel");
        check(problems, cases, new ItemStack(it("jetpack_fly")).getEquipmentSlot() == EquipmentSlot.CHEST
                || it("jetpack_fly").getEquipmentSlot(new ItemStack(it("jetpack_fly"))) == EquipmentSlot.CHEST, "jetpack can be worn on its own");
        // wings_limp: falling capped at -0.4
        p.setOnGround(false);
        p.setDeltaMovement(0D, -1.0D, 0D);
        p.fallDistance = 5F;
        ((LegacyWingsItem) it("wings_limp")).onArmorTick(level, p, new ItemStack(it("wings_limp")));
        check(problems, cases, p.getDeltaMovement().y == -0.4D && p.fallDistance == 0F, "wings_limp caps fall speed at -0.4");
        p.setDeltaMovement(0D, -0.5D, 0D);
        ((LegacyWingsItem) it("wings_murk")).onArmorTick(level, p, new ItemStack(it("wings_murk")));
        check(problems, cases, Math.abs(p.getDeltaMovement().y + 0.3D) < 1e-9, "wings_murk glide: -0.5 + 0.2");
        p.setDeltaMovement(0D, 0D, 0D);

        // gas masks: hazard classes, blacklist and filter wear
        ItemStack mask = new ItemStack(it("gas_mask"));
        ItemStack filter = new ItemStack(it("gas_mask_filter"));
        GasMaskItem.installFilter(mask, filter, p);
        p.setItemSlot(EquipmentSlot.HEAD, mask);
        check(problems, cases, HbmArmorProtection.hasHeadProtection(p, HazardClass.GAS_LUNG) && !HbmArmorProtection.hasHeadProtection(p, HazardClass.GAS_BLISTERING)
                && HbmArmorProtection.hasHeadProtection(p, HazardClass.LIGHT) && !HbmArmorProtection.hasHeadProtection(p, HazardClass.GAS_MONOXIDE),
                "gas_mask + filter: chemical gas yes, corrosive no (blacklist), light (mask), no CO");
        HbmArmorProtection.hasHeadProtection(p, HazardClass.PARTICLE_COARSE, 3);
        check(problems, cases, GasMaskItem.getInstalledFilter(p.getItemBySlot(EquipmentSlot.HEAD), server.registryAccess()).getDamageValue() == 3, "filter wears by the hazard's damage");
        ItemStack mono = new ItemStack(it("gas_mask_mono"));
        GasMaskItem.installFilter(mono, new ItemStack(it("gas_mask_filter_combo")), p);
        p.setItemSlot(EquipmentSlot.HEAD, mono);
        check(problems, cases, !HbmArmorProtection.hasHeadProtection(p, HazardClass.GAS_LUNG) && !HbmArmorProtection.hasHeadProtection(p, HazardClass.BACTERIA)
                && HbmArmorProtection.hasHeadProtection(p, HazardClass.GAS_MONOXIDE), "gas_mask_mono blacklist: chemical gas, corrosive, bacteria");
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        ItemStack attachment = new ItemStack(it("attachment_mask"));
        GasMaskItem.installFilter(attachment, new ItemStack(it("gas_mask_filter_rag")), p);
        ArmorModHandler.applyMod(helmet, attachment, server.registryAccess());
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        check(problems, cases, HbmArmorProtection.hasHeadProtection(p, HazardClass.PARTICLE_COARSE, 1) && HbmArmorProtection.hasHeadProtection(p, HazardClass.SAND),
                "attachment_mask on a helmet protects");
        ItemStack stored = ArmorModHandler.pryMods(p.getItemBySlot(EquipmentSlot.HEAD), server.registryAccess())[H];
        check(problems, cases, GasMaskItem.getInstalledFilter(stored, server.registryAccess()).getDamageValue() == 0, "attachment filter does not wear (1.7.10 copy bug)");
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        check(problems, cases, new ItemStack(it("gas_mask_filter")).getMaxDamage() == 20000 && new ItemStack(it("gas_mask_filter")).getMaxStackSize() == 1, "filters: 20000 durability, stack 1");
        check(problems, cases, new ItemStack(it("gas_mask")).getMaxDamage() == 165 && it("gas_mask") instanceof ArmorItem a && a.getDefense() == 2
                && a.getEnchantmentValue() == 9, "gas mask: ArmorMaterial.IRON helmet (165, 2, 9)");

        check(problems, cases, !new ItemStack(it("no9")).isDamageableItem(), "no9: setMaxDamage(0)");
        check(problems, cases, new ItemStack(it("hat")).getMaxDamage() == 440 && new ItemStack(it("mask_of_infamy")).getMaxDamage() == 165, "hat 40x11, mask_of_infamy 15x11 durability");

        // armor table: slot layout and rules
        ArmorTableMenu menu = new ArmorTableMenu(0, p.getInventory());
        check(problems, cases, menu.slots.size() == 9 + 1 + 4 + 36, "armor table has 9 mod slots, 1 armor slot, 4 worn armor slots and the inventory");
        int[][] pos = {{48, 27}, {84, 27}, {120, 27}, {156, 45}, {156, 81}, {120, 99}, {84, 99}, {48, 99}, {30, 63}, {66, 63}};
        boolean layout = true;
        for (int i = 0; i < pos.length; i++) layout &= menu.slots.get(i).x == pos[i][0] && menu.slots.get(i).y == pos[i][1];
        check(problems, cases, layout, "armor table slot positions");
        check(problems, cases, !menu.slots.get(SV).mayPlace(new ItemStack(it("servo_set"))), "no mod without armor");
        menu.slots.get(9).set(new ItemStack(Items.IRON_CHESTPLATE));
        check(problems, cases, menu.slots.get(SV).mayPlace(new ItemStack(it("servo_set"))) && !menu.slots.get(H).mayPlace(new ItemStack(it("servo_set")))
                && !menu.slots.get(B).mayPlace(new ItemStack(it("pads_rubber"))), "mods only in their own slot of fitting armor");
        menu.slots.get(SV).set(new ItemStack(it("servo_set")));
        check(problems, cases, ArmorModHandler.pryMods(menu.slots.get(9).getItem(), server.registryAccess())[SV].is(it("servo_set")), "table installs the mod");
        menu.slots.get(SV).onTake(p, menu.slots.get(SV).getItem());
        check(problems, cases, ArmorModHandler.pryMods(menu.slots.get(9).getItem(), server.registryAccess())[SV].isEmpty(), "taking the mod removes it");
        check(problems, cases, menu.slots.get(10).mayPlace(new ItemStack(Items.IRON_HELMET)) && !menu.slots.get(10).mayPlace(new ItemStack(Items.IRON_CHESTPLATE))
                && menu.slots.get(11).mayPlace(new ItemStack(it("jetpack_fly"))), "worn-armor slots check the armor type");
        menu.slots.get(9).set(ItemStack.EMPTY);
        p.getInventory().clearContent();

        // recipes
        recipe(problems, cases, server, level, "injector_5htp", 3, 1, it("five_htp"), it("circuit_basic"), it("plate_saturnite"));
        check(problems, cases, craft(server, level, 3, 1, new ItemStack(it("five_htp")), new ItemStack(it("circuit_basic")), new ItemStack(it("plate_bismuth"))).isEmpty(),
                "injector_5htp needs saturnite (BIGMT) plate");
        ItemStack ducttape = new ItemStack(it("ducttape"));
        recipe(problems, cases, server, level, "ballistic_gauntlet", 3, 3, null, it("wire_copper"), it("plate_steel"), it("wire_copper"), it("ring_starmetal"), it("plate_steel"),
                null, it("ring_starmetal"), it("plate_steel"));
        recipe(problems, cases, server, level, "pads_rubber", 3, 3, it("ingot_rubber"), null, it("ingot_rubber"), it("plate_iron"), ducttape.getItem(), it("plate_iron"),
                it("ingot_rubber"), null, it("ingot_rubber"));
        recipe(problems, cases, server, level, "cladding_paint", 3, 2, it("nugget_lead"), it("nugget_lead"), it("nugget_lead"), it("nugget_lead"), Items.CLAY_BALL, Items.GLASS_BOTTLE);
        recipe(problems, cases, server, level, "gas_mask_filter", 1, 2, it("plate_iron"), it("filter_coal"));
        Item[] inkGrid = {Items.ALLIUM, it("armor_polish"), Items.POPPY, it("armor_polish"), Items.BLACK_DYE, it("armor_polish"), Items.OXEYE_DAISY, it("armor_polish"), Items.PINK_TULIP};
        recipe(problems, cases, server, level, "ink", 3, 3, inkGrid);
        inkGrid[0] = Items.DANDELION;
        check(problems, cases, craft(server, level, 3, 3, stacks(inkGrid)).isEmpty(), "ink accepts only Blocks.red_flower variants");
        check(problems, cases, server.getRecipeManager().byKey(AtomicLegacy.id("machine_armor_table")).isPresent(), "machine_armor_table recipe");

        p.getInventory().clearContent();
        p.removeAllEffects();
        return cases[0];
    }

    private static void jet(List<String> problems, int[] cases, Player p, String id, double startY, boolean active, double expectY, double expectZ, int rate) {
        LegacyJetpackItem item = (LegacyJetpackItem) it(id);
        ItemStack stack = new ItemStack(item);
        LegacyJetpackItem.setFuel(stack, 100);
        p.setDeltaMovement(0D, startY, 0D);
        p.fallDistance = 3F;
        p.tickCount = rate * 7;
        item.onArmorTick(p.level(), p, stack);
        Vec3 v = p.getDeltaMovement();
        check(problems, cases, Math.abs(v.y - expectY) < 1e-9 && Math.abs(v.z - expectZ) < 1e-9, id + " thrust from " + startY + " -> y " + expectY + " z " + expectZ + " (got " + v + ")");
        check(problems, cases, LegacyJetpackItem.fuel(stack) == 99, id + " burns 1 mB every " + rate + " ticks");
        p.tickCount = rate * 7 + 1;
        if (rate > 1) {
            item.onArmorTick(p.level(), p, stack);
            check(problems, cases, LegacyJetpackItem.fuel(stack) == 99, id + " no fuel use between intervals");
        }
        p.setDeltaMovement(0D, 0D, 0D);
    }

    private static void recipe(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, int w, int h, Item... grid) {
        ItemStack out = craft(server, level, w, h, stacks(grid));
        check(problems, cases, out.is(it(id)), "recipe " + id + " crafts from the 1.7.10 pattern");
    }

    private static ItemStack[] stacks(Item... grid) {
        ItemStack[] s = new ItemStack[grid.length];
        for (int i = 0; i < grid.length; i++) s[i] = grid[i] == null ? ItemStack.EMPTY : new ItemStack(grid[i]);
        return s;
    }

    private static ItemStack craft(MinecraftServer server, ServerLevel level, int w, int h, ItemStack... grid) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : grid) list.add(s.copyWithCount(s.isEmpty() ? 0 : 1));
        CraftingInput input = CraftingInput.of(w, h, list);
        return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input, server.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static boolean eff(Player p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance i = p.getEffect(effect);
        return i != null && i.getDuration() == duration && i.getAmplifier() == amplifier;
    }

    private static String tooltip(ItemStack stack) {
        StringBuilder b = new StringBuilder();
        for (Component c : stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL)) b.append(c.getString()).append('\n');
        return b.toString();
    }

    private static ArmorModItem mod(String id) {
        return (ArmorModItem) it(id);
    }

    private static Item it(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) problems.add("parity I2: " + message);
    }
}

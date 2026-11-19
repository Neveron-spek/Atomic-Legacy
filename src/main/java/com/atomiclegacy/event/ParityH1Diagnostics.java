package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyQuackosEntity;
import com.atomiclegacy.entity.LegacyVortexEntity;
import com.atomiclegacy.inventory.recipes.anvil.HbmAnvilRecipes;
import com.atomiclegacy.items.HbmPlayerShield;
import com.atomiclegacy.items.LegacyConserveItem;
import com.atomiclegacy.items.LegacyConsumableSupport;
import com.atomiclegacy.items.LegacySpecialFoodItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmMobEffects;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * [parity H1] Server-side parity checks of the 1.7.10 medicine and food items.
 * Every expected number is quoted from the original sources under work/original:
 * items/special/ItemSyringe.java, items/special/ItemSimpleConsumable.java, items/special/ItemCigarette.java,
 * items/food/*.java, items/tool/ItemPeas.java, potion/HbmPotion.java, crafting/ConsumableRecipes.java,
 * main/CraftingManager.java, inventory/recipes/anvil/AnvilRecipes.java and items/ModItems.java.
 */
public final class ParityH1Diagnostics {

	private ParityH1Diagnostics() { }

	private record E(Holder<MobEffect> effect, int duration, int amplifier) { }

	private static E e(Holder<MobEffect> effect, int duration, int amplifier) { return new E(effect, duration, amplifier); }

	private static final double X = 0.5D, Y = 280.0D, Z = 0.5D;

	public static int run(MinecraftServer server, List<String> problems) {
		int[] cases = { 0 };
		ServerLevel level = server.overworld();
		try {
			check(problems, cases, LegacyConsumableSupport.potionSicknessMode() == 0, "PotionConfig potionSickness defaults to OFF");
			registry(problems, cases);
			syringes(server, level, problems, cases);
			pills(server, level, problems, cases);
			drinks(server, level, problems, cases);
			smoking(server, level, problems, cases);
			foods(server, level, problems, cases);
			misc(server, level, problems, cases);
			effects(server, level, problems, cases);
			recipes(server, level, problems, cases);
			tabs(problems, cases);
		} catch(Throwable t) {
			StackTraceElement[] trace = t.getStackTrace();
			problems.add("parity H1: exception " + t + (trace.length > 0 ? " at " + trace[0] : "") + (trace.length > 1 ? " < " + trace[1] : "") + (trace.length > 2 ? " < " + trace[2] : ""));
		}
		cleanup(level);
		return cases[0];
	}

	/// REGISTRY: stack sizes, use time and animation (ModItems.java + the item classes) ///
	private static void registry(List<String> problems, int[] cases) {
		String[] ids = { "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super", "syringe_taint", "syringe_mkunicorn",
				"syringe_antidote", "syringe_poison", "syringe_awesome", "syringe_empty", "syringe_metal_empty", "med_bag", "radaway", "radaway_strong", "radaway_flush",
				"iv_empty", "iv_blood", "iv_xp_empty", "iv_xp", "radx", "siox", "pill_herbal", "xanax", "fmn", "five_htp", "pill_iodine", "plan_c", "pill_red", "chocolate",
				"cigarette", "crackpipe", "bdcl", "can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge", "can_luna", "can_bepis", "can_breen", "can_mug",
				"bottle_nuka", "bottle_cherry", "bottle_quantum", "bottle_sparkle", "bottle_rad", "bottle2_korl", "bottle2_fritz", "chocolate_milk", "coffee", "coffee_radium",
				"can_empty", "bottle_empty", "bottle2_empty", "cap_nuka", "cap_quantum", "cap_sparkle", "cap_rad", "cap_korl", "cap_fritz", "ring_pull", "can_key",
				"flask_infusion", "bomb_waffle", "schnitzel_vegan", "cotton_candy", "apple_lead", "apple_schrabidium", "apple_euphemium", "tem_flakes", "glowing_stew",
				"balefire_scrambled", "balefire_and_ham", "lemon", "definitelyfood", "med_ipecac", "med_ptsd", "med_schizophrenia", "loops", "loop_stew", "spongebob_macaroni",
				"fooditem", "twinkie", "static_sandwich", "pudding", "canteen_vodka", "pancake", "nugget", "marshmallow", "cheese", "cheese_quesadilla", "mucho_mango",
				"glyphid_meat", "glyphid_meat_grilled", "peas", "egg_glyphid", "canned_conserve", "jetpack_tank", "cbt_device", "ingot_smore", "ingot_semtex", "ingot_c4",
				"powder_cement", "bio_wafer", "crayon" };
		for(String id : ids) check(problems, cases, BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(id)), "item " + id + " is registered");
		// setMaxStackSize: cigarette 16, crackpipe 1, jetpack_tank 16, cbt_device 1, loop_stew 1, marshmallow 1, apple_euphemium 1; ItemSoup 1; everything else 64
		Object[][] stacks = { { "cigarette", 16 }, { "crackpipe", 1 }, { "jetpack_tank", 16 }, { "cbt_device", 1 }, { "loop_stew", 1 }, { "marshmallow", 1 },
				{ "apple_euphemium", 1 }, { "glowing_stew", 1 }, { "balefire_scrambled", 1 }, { "balefire_and_ham", 1 }, { "flask_infusion", 64 }, { "radx", 64 },
				{ "syringe_metal_stimpak", 64 }, { "med_bag", 64 }, { "radaway", 64 }, { "iv_empty", 64 }, { "can_smart", 64 }, { "bottle_nuka", 64 }, { "lemon", 64 },
				{ "canned_conserve", 64 }, { "mucho_mango", 64 }, { "bdcl", 64 }, { "peas", 64 }, { "coffee", 64 }, { "pancake", 64 }, { "nugget", 64 } };
		for(Object[] s : stacks) check(problems, cases, new ItemStack(it((String) s[0])).getMaxStackSize() == (int) s[1], s[0] + " stack size " + s[1] + " (is " + new ItemStack(it((String) s[0])).getMaxStackSize() + ")");
		// getMaxItemUseDuration / getItemUseAction: ItemPill 10 eat, ItemEnergy 32 drink, ItemCigarette 30 bow, ItemConserve 32 eat,
		// ItemMuchoMango 200 drink, ItemCanteen 10 drink, ItemFlask 32 drink, ItemBDCL 40 drink, ItemFood 32 eat
		Object[][] use = { { "radx", 10, UseAnim.EAT }, { "chocolate", 10, UseAnim.EAT }, { "can_smart", 32, UseAnim.DRINK }, { "bottle_rad", 32, UseAnim.DRINK },
				{ "coffee", 32, UseAnim.DRINK }, { "cigarette", 30, UseAnim.BOW }, { "crackpipe", 30, UseAnim.BOW }, { "canned_conserve", 32, UseAnim.EAT },
				{ "mucho_mango", 200, UseAnim.DRINK }, { "canteen_vodka", 10, UseAnim.DRINK }, { "flask_infusion", 32, UseAnim.DRINK }, { "bdcl", 40, UseAnim.DRINK },
				{ "lemon", 32, UseAnim.EAT }, { "glowing_stew", 32, UseAnim.EAT }, { "bomb_waffle", 32, UseAnim.EAT } };
		for(Object[] u : use) {
			ItemStack s = new ItemStack(it((String) u[0]));
			check(problems, cases, s.getUseDuration(null) == (int) u[1] && s.getUseAnimation() == u[2], u[0] + " use " + u[1] + " ticks " + u[2] + " (is " + s.getUseDuration(null) + " " + s.getUseAnimation() + ")");
		}
		// ItemSimpleConsumable.init: syringe_awesome setRarity(uncommon).setEffect(); ItemAppleEuphemium epic + glint
		ItemStack awesome = new ItemStack(it("syringe_awesome"));
		check(problems, cases, awesome.getRarity() == Rarity.UNCOMMON && awesome.hasFoil(), "syringe_awesome uncommon + glint");
		ItemStack euph = new ItemStack(it("apple_euphemium"));
		check(problems, cases, euph.getRarity() == Rarity.EPIC && euph.hasFoil(), "apple_euphemium epic + glint");
		// ItemAppleSchrabidium.hasEffect: meta 2 only (both apples); getRarity uncommon / rare / epic
		for(String apple : new String[] { "apple_schrabidium", "apple_lead" }) {
			check(problems, cases, !LegacySpecialFoodItem.stackFor(it(apple), 0).hasFoil() && !LegacySpecialFoodItem.stackFor(it(apple), 1).hasFoil()
					&& LegacySpecialFoodItem.stackFor(it(apple), 2).hasFoil(), apple + " glint only for meta 2");
			check(problems, cases, LegacySpecialFoodItem.stackFor(it(apple), 0).getRarity() == Rarity.UNCOMMON && LegacySpecialFoodItem.stackFor(it(apple), 1).getRarity() == Rarity.RARE
					&& LegacySpecialFoodItem.stackFor(it(apple), 2).getRarity() == Rarity.EPIC, apple + " rarity by meta");
		}
		// lore: ItemSyringe.addInformation literals, ItemPill / ItemLemon "item.<name>.desc" (en_US.lang), ItemCigarette, ItemPancake, ItemCanteen
		lore(problems, cases, new ItemStack(it("syringe_metal_stimpak")), "Heals 2.5 hearts");
		lore(problems, cases, new ItemStack(it("syringe_metal_medx")), "Resistance III for 4 minutes");
		lore(problems, cases, new ItemStack(it("syringe_metal_psycho")), "Resistance I for 2 minutes", "Strength I for 2 minutes");
		lore(problems, cases, new ItemStack(it("syringe_metal_super")), "Heals 25 hearts", "Slowness I for 10 seconds");
		lore(problems, cases, new ItemStack(it("med_bag")), "Full heal, regardless of max health", "Removes negative effects");
		lore(problems, cases, new ItemStack(it("syringe_taint")), "Tainted I for 60 seconds", "Nausea I for 5 seconds", "Cloud damage + taint = tainted heart effect");
		lore(problems, cases, new ItemStack(it("syringe_mkunicorn")), "?");
		lore(problems, cases, new ItemStack(it("radaway")));
		lore(problems, cases, new ItemStack(it("syringe_antidote")));
		lore(problems, cases, new ItemStack(it("iv_xp_empty")));
		lore(problems, cases, new ItemStack(it("radx")), "Increases radiation resistance by 0.2 (37%) for 3 minutes");
		lore(problems, cases, new ItemStack(it("siox")), "Reverses mesothelioma with the power of Asbestos!");
		lore(problems, cases, new ItemStack(it("pill_herbal")), "Effective treatment against lung disease and mild radiation poisoning", "Comes with side effects");
		lore(problems, cases, new ItemStack(it("xanax")), "Removes 500mDRX");
		lore(problems, cases, new ItemStack(it("fmn")), "Removes all DRX above 2,000mDRX");
		lore(problems, cases, new ItemStack(it("five_htp")), "Removes all DRX, Stability for 10 minutes");
		lore(problems, cases, new ItemStack(it("pill_iodine")), "Removes negative effects");
		lore(problems, cases, new ItemStack(it("plan_c")), "Deadly");
		lore(problems, cases, new ItemStack(it("pill_red")));
		lore(problems, cases, new ItemStack(it("chocolate")), "Radium Chocolate? Pretty sure this is just meth.");
		lore(problems, cases, new ItemStack(it("lemon")), "Eh, good enough.");
		lore(problems, cases, new ItemStack(it("med_ipecac")), "Bitter juice that will cause your stomach", "to forcefully eject its contents.");
		lore(problems, cases, new ItemStack(it("pudding")), "What if he did?", "What if he didn't?", "What if the world was made of pudding?");
		lore(problems, cases, new ItemStack(it("ingot_semtex")), "Semtex H Plastic Explosive", "Performant explosive for many applications.", "Edible");
		lore(problems, cases, new ItemStack(it("cheese_quesadilla")), "That's what a 50 year old yeast infection does to you.");
		lore(problems, cases, new ItemStack(it("cheese")));
		lore(problems, cases, new ItemStack(it("peas")));
		lore(problems, cases, new ItemStack(it("cigarette")), "✓ Asbestos filter", "✓ High in tar", "✓ Tobacco contains 100% Polonium-210", "✓ Yum");
		lore(problems, cases, new ItemStack(it("crackpipe")), "This can't be good for me, but I feel GREAT");
		lore(problems, cases, new ItemStack(it("pancake")), "Can be eaten to recharge lunar cybernetic armor", "Not for people with weak molars", "", "Half burnt and smells horrible");
		lore(problems, cases, new ItemStack(it("mucho_mango")), "The Comically Large Can");
		lore(problems, cases, new ItemStack(it("canteen_vodka")), "Cooldown: 3 minutes", "Nausea I for 10 seconds", "Strength III for 30 seconds", "",
				com.atomiclegacy.AtomicLegacy.polaroidID == 11 ? "Time to get hammered & sickled!" : "Smells like disinfectant, tastes like disinfectant."); // [parity J1] polaroidID
		lore(problems, cases, new ItemStack(it("can_breen")), "Don't drink the water. They put something in it, to make you forget.", "I don't even know how I got here.");
		lore(problems, cases, new ItemStack(it("bottle_nuka")), "Contains about 210 kcal and 1500 mSv.", "[Requires bottle opener]");
		lore(problems, cases, new ItemStack(it("jetpack_tank")), "Fills worn jetpack with up to 1000mB of kerosene");
		lore(problems, cases, LegacySpecialFoodItem.stackFor(it("tem_flakes"), 2), "Heals food of tem (expensiv)");
		lore(problems, cases, LegacyConserveItem.stackFor(it("canned_conserve"), LegacyConserveItem.Variant.ASS), "100% quality donkey meat!*");
	}

	/// ItemSyringe.java / ItemSimpleConsumable.java ///
	private static void syringes(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		// syringe_metal_stimpak: heal(5), syringe_metal_empty back, hbm:item.syringe; VersatileConfig potion sickness is off by default
		reset(p);
		p.setHealth(4F);
		ItemStack hand = use(level, p, new ItemStack(it("syringe_metal_stimpak"), 2));
		check(problems, cases, p.getHealth() == 9F && hand.is(it("syringe_metal_stimpak")) && hand.getCount() == 1 && count(p, "syringe_metal_empty") == 1
				&& !p.hasEffect(HbmMobEffects.POTION_SICKNESS), "stimpak: +5 HP, empty syringe, no sickness (hp " + p.getHealth() + ")");
		hand = use(level, p, hand);
		check(problems, cases, p.getHealth() == 14F && hand.is(it("syringe_metal_empty")) && hand.getCount() == 1, "stimpak: last one is replaced by the empty syringe");
		// potion sickness blocks the four metal syringes, med_bag, antidote and awesome
		reset(p);
		p.setHealth(4F);
		p.addEffect(new MobEffectInstance(HbmMobEffects.POTION_SICKNESS, 100));
		hand = use(level, p, new ItemStack(it("syringe_metal_stimpak"), 2));
		check(problems, cases, p.getHealth() == 4F && hand.getCount() == 2, "stimpak: blocked by potion sickness");
		hand = use(level, p, new ItemStack(it("med_bag"), 2));
		check(problems, cases, p.getHealth() == 4F && hand.getCount() == 2, "med_bag: blocked by potion sickness");
		// syringe_metal_medx: resistance 4 * 60 * 20, 2
		reset(p);
		use(level, p, new ItemStack(it("syringe_metal_medx"), 2));
		check(problems, cases, has(p, e(MobEffects.DAMAGE_RESISTANCE, 4 * 60 * 20, 2)) && count(p, "syringe_metal_empty") == 1 && effectCount(p) == 1, "medx: Resistance III 4800");
		// syringe_metal_psycho: resistance 2 * 60 * 20, 0 + damageBoost 2 * 60 * 20, 0
		reset(p);
		use(level, p, new ItemStack(it("syringe_metal_psycho"), 2));
		check(problems, cases, has(p, e(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 0), e(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 0)) && count(p, "syringe_metal_empty") == 1 && effectCount(p) == 2, "psycho: Resistance I + Strength I 2400");
		// syringe_metal_super: heal(25), moveSlowdown 10 * 20, 0
		reset(p);
		p.setHealth(1F);
		use(level, p, new ItemStack(it("syringe_metal_super"), 2));
		check(problems, cases, p.getHealth() == 20F && has(p, e(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 0)) && count(p, "syringe_metal_empty") == 1 && effectCount(p) == 1, "super stimpak: +25 HP, Slowness I 200");
		// hitEntity: the target is treated, the attacker gets the syringe back
		reset(p);
		Pig pig = pig(level);
		pig.setHealth(2F);
		ItemStack stim = new ItemStack(it("syringe_metal_stimpak"), 2);
		stim.getItem().hurtEnemy(stim, pig, p);
		check(problems, cases, pig.getHealth() == 7F && stim.getCount() == 1 && count(p, "syringe_metal_empty") == 1 && p.getHealth() == 20F, "stimpak on hit: target +5 HP, attacker keeps the syringe (pig hp " + pig.getHealth() + ")");
		// med_bag: setHealth(max), removes blindness, confusion, digSlowdown, hunger, moveSlowdown, poison, weakness, wither; no tare, no sound
		reset(p);
		p.setHealth(3F);
		for(Holder<MobEffect> bad : List.of(MobEffects.BLINDNESS, MobEffects.CONFUSION, MobEffects.DIG_SLOWDOWN, MobEffects.HUNGER, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.POISON, MobEffects.WEAKNESS, MobEffects.WITHER))
			p.addEffect(new MobEffectInstance(bad, 600, 0));
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
		p.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 600, 0));
		hand = use(level, p, new ItemStack(it("med_bag"), 2));
		check(problems, cases, p.getHealth() == p.getMaxHealth() && effectCount(p) == 2 && p.hasEffect(MobEffects.REGENERATION) && p.hasEffect(HbmMobEffects.TAINT) && hand.getCount() == 1
				&& invCount(p) == 0, "med_bag: full heal, 8 debuffs removed, taint kept, no tare (effects " + effectCount(p) + ")");
		// syringe_taint: taint 60 * 20, 0 + confusion 5 * 20, 0; syringe_metal_empty and bottle2_empty back
		reset(p);
		hand = use(level, p, new ItemStack(it("syringe_taint"), 2));
		check(problems, cases, has(p, e(HbmMobEffects.TAINT, 60 * 20, 0), e(MobEffects.CONFUSION, 5 * 20, 0)) && hand.getCount() == 1 && count(p, "syringe_metal_empty") == 1 && count(p, "bottle2_empty") == 1, "syringe_taint: Taint 1200, Nausea 100, two tares");
		pig = pig(level);
		ItemStack taint = new ItemStack(it("syringe_taint"), 1);
		taint.getItem().hurtEnemy(taint, pig, p);
		check(problems, cases, has(pig, e(HbmMobEffects.TAINT, 60 * 20, 0), e(MobEffects.CONFUSION, 5 * 20, 0)) && taint.isEmpty() && count(p, "syringe_metal_empty") == 2 && count(p, "bottle2_empty") == 2, "syringe_taint on hit");
		// syringe_mkunicorn: right click does nothing; hitEntity sets contagion 3 * 60 * 60 * 20, no tare
		reset(p);
		hand = use(level, p, new ItemStack(it("syringe_mkunicorn"), 1));
		check(problems, cases, hand.getCount() == 1 && MkuContagionEvents.contagion(p) == 0, "MKU syringe: no effect on right click");
		pig = pig(level);
		hand.getItem().hurtEnemy(hand, pig, p);
		check(problems, cases, hand.isEmpty() && MkuContagionEvents.contagion(pig) == 3 * 60 * 60 * 20 && invCount(p) == 0, "MKU syringe on hit: contagion 216000, no tare");
		// syringe_antidote: clearActivePotions, syringe_empty back
		reset(p);
		p.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0));
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
		hand = use(level, p, new ItemStack(it("syringe_antidote"), 2));
		check(problems, cases, effectCount(p) == 0 && hand.getCount() == 1 && count(p, "syringe_empty") == 1, "antidote: all effects cleared, empty syringe");
		// syringe_awesome: eleven effects, syringe_empty back
		reset(p);
		hand = use(level, p, new ItemStack(it("syringe_awesome"), 2));
		check(problems, cases, has(p, e(MobEffects.REGENERATION, 50 * 20, 9), e(MobEffects.DAMAGE_RESISTANCE, 50 * 20, 9), e(MobEffects.FIRE_RESISTANCE, 50 * 20, 0),
				e(MobEffects.DAMAGE_BOOST, 50 * 20, 24), e(MobEffects.DIG_SPEED, 50 * 20, 9), e(MobEffects.MOVEMENT_SPEED, 50 * 20, 6), e(MobEffects.JUMP, 50 * 20, 9),
				e(MobEffects.HEALTH_BOOST, 50 * 20, 9), e(MobEffects.ABSORPTION, 50 * 20, 4), e(MobEffects.CONFUSION, 5 * 20, 4), e(HbmMobEffects.RADX, 50 * 20, 9))
				&& effectCount(p) == 11 && hand.getCount() == 1 && count(p, "syringe_empty") == 1, "awesome: 11 effects (" + effectCount(p) + ")");
		// syringe_poison on another entity: ModDamageSource.euthanized 30, syringe_empty to the attacker
		reset(p);
		pig = pig(level);
		ItemStack poison = new ItemStack(it("syringe_poison"), 1);
		poison.getItem().hurtEnemy(poison, pig, p);
		check(problems, cases, pig.isDeadOrDying() && poison.isEmpty() && count(p, "syringe_empty") == 1
				&& pig.getLastDamageSource() != null && pig.getLastDamageSource().getMsgId().equals("euthanized") && pig.getLastDamageSource().getEntity() == p, "poison on hit: 30 euthanized damage");
		// syringe_poison on self: euthanizedSelf / euthanizedSelf2, 30
		ServerPlayer victim = player(server, level);
		reset(victim);
		use(level, victim, new ItemStack(it("syringe_poison"), 2));
		check(problems, cases, victim.isDeadOrDying() && victim.getLastDamageSource() != null && victim.getLastDamageSource().getMsgId().startsWith("euthanizedSelf"), "poison on self: 30 euthanizedSelf damage (hp " + victim.getHealth() + ")");
		// iv_empty: isRealPlayer only; health - 5, iv_blood; iv_blood: heal 5, iv_empty
		reset(p);
		hand = use(level, p, new ItemStack(it("iv_empty"), 2));
		check(problems, cases, p.getHealth() == 15F && hand.getCount() == 1 && count(p, "iv_blood") == 1, "iv_empty: -5 HP, blood bag (hp " + p.getHealth() + ")");
		hand = use(level, p, new ItemStack(it("iv_blood"), 2));
		check(problems, cases, p.getHealth() == 20F && hand.getCount() == 1 && count(p, "iv_empty") == 1, "iv_blood: +5 HP, empty bag");
		FakePlayer fake = FakePlayerFactory.getMinecraft(level);
		fake.setPos(X, Y, Z);
		fake.getInventory().clearContent();
		hand = use(level, fake, new ItemStack(it("iv_empty"), 2));
		check(problems, cases, hand.getCount() == 2 && count(fake, "iv_blood") == 0, "iv_empty: ignored for fake players");
		fake.getInventory().clearContent();
		// radaway 140, radaway_strong 350, radaway_flush 500, amplifier 0, stacking duration, iv_empty back
		Object[][] radaways = { { "radaway", 140 }, { "radaway_strong", 350 }, { "radaway_flush", 500 } };
		for(Object[] r : radaways) {
			reset(p);
			hand = use(level, p, new ItemStack(it((String) r[0]), 3));
			check(problems, cases, has(p, e(HbmMobEffects.RADAWAY, (int) r[1], 0)) && hand.getCount() == 2 && count(p, "iv_empty") == 1, r[0] + ": RadAway " + r[1] + " ticks level 0");
			use(level, p, hand);
			check(problems, cases, has(p, e(HbmMobEffects.RADAWAY, 2 * (int) r[1], 0)) && count(p, "iv_empty") == 2, r[0] + ": second dose extends to " + 2 * (int) r[1]);
		}
		// iv_xp_empty: needs 100 XP, becomes iv_xp; iv_xp returns 100 XP and the empty bag
		reset(p);
		hand = use(level, p, new ItemStack(it("iv_xp_empty"), 1));
		check(problems, cases, hand.is(it("iv_xp_empty")), "iv_xp_empty: nothing below 100 XP");
		p.giveExperiencePoints(100);
		int lvl = p.experienceLevel;
		float progress = p.experienceProgress;
		hand = use(level, p, hand);
		check(problems, cases, hand.is(it("iv_xp")) && p.experienceLevel == 0 && p.experienceProgress < 0.001F, "iv_xp_empty: stores 100 XP (level " + p.experienceLevel + ")");
		hand = use(level, p, hand);
		check(problems, cases, hand.is(it("iv_xp_empty")) && p.experienceLevel == lvl && Math.abs(p.experienceProgress - progress) < 0.001F && lvl == 7, "iv_xp: returns 100 XP (level " + p.experienceLevel + ")");
		// creative right click: ItemInWorldManager restores the stack size, the tare is still handed out
		reset(p);
		p.getAbilities().instabuild = true;
		hand = use(level, p, new ItemStack(it("radaway"), 2));
		check(problems, cases, hand.getCount() == 2 && p.hasEffect(HbmMobEffects.RADAWAY) && count(p, "iv_empty") == 1, "radaway in creative: stack kept");
		p.getAbilities().instabuild = false;
	}

	/// ItemPill.java ///
	private static void pills(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		// radx: HbmPotion.radx 3 * 60 * 20, 0
		reset(p);
		ItemStack hand = use(level, p, new ItemStack(it("radx"), 2));
		check(problems, cases, has(p, e(HbmMobEffects.RADX, 3 * 60 * 20, 0)) && effectCount(p) == 1 && hand.getCount() == 1 && invCount(p) == 0, "radx: Rad-X 3600 level 0, no sickness");
		// siox: asbestos 0, black lung min(current, maxBlacklung / 5)
		reset(p);
		hazards(p, 5000, HbmLivingHazards.MAX_BLACK_LUNG / 2);
		use(level, p, new ItemStack(it("siox"), 1));
		check(problems, cases, HbmLivingHazards.get(p).getAsbestos() == 0 && HbmLivingHazards.get(p).getBlackLung() == (HbmConfig.ENABLE_COAL_DUST.get() ? HbmLivingHazards.MAX_BLACK_LUNG / 5 : 0), "siox: asbestos 0, black lung capped to 1/5");
		// pill_herbal: as siox, radiation -100, confusion 200/0, weakness 12000/2, digSlowdown 12000/2, poison 100/2, potion sickness 12000
		reset(p);
		hazards(p, 5000, 100);
		radiation(p, 250F, 0F);
		use(level, p, new ItemStack(it("pill_herbal"), 1));
		check(problems, cases, HbmLivingHazards.get(p).getAsbestos() == 0 && HbmLivingHazards.get(p).getBlackLung() == (HbmConfig.ENABLE_COAL_DUST.get() ? 100 : 0)
				&& HbmLivingRadiation.get(p).getRadiation() == 150F && has(p, e(MobEffects.CONFUSION, 10 * 20, 0), e(MobEffects.WEAKNESS, 10 * 60 * 20, 2),
						e(MobEffects.DIG_SLOWDOWN, 10 * 60 * 20, 2), e(MobEffects.POISON, 5 * 20, 2), e(HbmMobEffects.POTION_SICKNESS, 10 * 60 * 20, 0))
				&& p.getEffect(HbmMobEffects.POTION_SICKNESS).getCures().isEmpty(), "pill_herbal: -100 RAD, four debuffs, 10 min incurable sickness (rad " + HbmLivingRadiation.get(p).getRadiation() + ")");
		hand = use(level, p, new ItemStack(it("radx"), 2));
		check(problems, cases, hand.getCount() == 2 && !p.hasEffect(HbmMobEffects.RADX), "pills are blocked by potion sickness");
		// xanax: digamma - 0.5 (min 0)
		reset(p);
		radiation(p, 0F, 1.25F);
		use(level, p, new ItemStack(it("xanax"), 1));
		check(problems, cases, HbmLivingRadiation.get(p).getDigamma() == 0.75F, "xanax: -0.5 DRX");
		// fmn: digamma min(d, 2), blindness 60, 0
		reset(p);
		radiation(p, 0F, 5F);
		use(level, p, new ItemStack(it("fmn"), 1));
		check(problems, cases, HbmLivingRadiation.get(p).getDigamma() == 2F && has(p, e(MobEffects.BLINDNESS, 60, 0)), "fmn: DRX capped to 2, Blindness 60");
		// five_htp: digamma 0, stability 10 * 60 * 20, 0
		reset(p);
		radiation(p, 0F, 5F);
		use(level, p, new ItemStack(it("five_htp"), 1));
		check(problems, cases, HbmLivingRadiation.get(p).getDigamma() == 0F && has(p, e(HbmMobEffects.STABILITY, 10 * 60 * 20, 0)), "five_htp: DRX 0, Stability 12000");
		// pill_iodine: removes the same eight debuffs as med_bag
		reset(p);
		for(Holder<MobEffect> bad : List.of(MobEffects.BLINDNESS, MobEffects.CONFUSION, MobEffects.DIG_SLOWDOWN, MobEffects.HUNGER, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.POISON, MobEffects.WEAKNESS, MobEffects.WITHER))
			p.addEffect(new MobEffectInstance(bad, 600, 0));
		p.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 600, 0));
		use(level, p, new ItemStack(it("pill_iodine"), 1));
		check(problems, cases, effectCount(p) == 1 && p.hasEffect(HbmMobEffects.TAINT), "pill_iodine: 8 debuffs removed, taint kept (" + effectCount(p) + ")");
		// pill_red: HbmPotion.death 60 * 60 * 20, 0
		reset(p);
		use(level, p, new ItemStack(it("pill_red"), 1));
		check(problems, cases, has(p, e(HbmMobEffects.DEATH, 60 * 60 * 20, 0)), "pill_red: Astolfization 72000");
		// chocolate: digSpeed, moveSpeed, jump 60 * 20, 3 (1 in 25: overdose 1000)
		ServerPlayer taster = player(server, level);
		reset(taster);
		use(level, taster, new ItemStack(it("chocolate"), 1));
		check(problems, cases, taster.isDeadOrDying() ? taster.getLastDamageSource() != null && taster.getLastDamageSource().getMsgId().equals("overdose")
				: has(taster, e(MobEffects.DIG_SPEED, 60 * 20, 3), e(MobEffects.MOVEMENT_SPEED, 60 * 20, 3), e(MobEffects.JUMP, 60 * 20, 3)), "chocolate: Haste/Speed/Jump IV 1200 or overdose");
		// plan_c: ten hits of euthanizedSelf / euthanizedSelf2, 1000
		ServerPlayer victim = player(server, level);
		reset(victim);
		use(level, victim, new ItemStack(it("plan_c"), 1));
		check(problems, cases, victim.isDeadOrDying() && victim.getLastDamageSource() != null && victim.getLastDamageSource().getMsgId().startsWith("euthanizedSelf"), "plan_c: lethal euthanizedSelf damage");
	}

	/// ItemEnergy.java ///
	private record Drink(String id, float heal, float rads, String cap, String container, E... effects) { }

	private static void drinks(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		int s30 = 30 * 20, s120 = 120 * 20;
		Drink[] drinks = {
				new Drink("can_smart", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DAMAGE_RESISTANCE, s30, 2), e(MobEffects.DAMAGE_BOOST, s30, 0)),
				new Drink("can_creature", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 0), e(MobEffects.DAMAGE_RESISTANCE, s30, 2), e(MobEffects.REGENERATION, s30, 1)),
				new Drink("can_redbomb", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 0), e(MobEffects.ABSORPTION, s30, 2), e(MobEffects.JUMP, s30, 1)),
				new Drink("can_mrsugar", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 0), e(MobEffects.DIG_SPEED, s30, 1), e(MobEffects.JUMP, s30, 2)),
				new Drink("can_overcharge", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DAMAGE_RESISTANCE, s30, 2), e(MobEffects.DAMAGE_BOOST, s30, 0)),
				new Drink("can_luna", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DAMAGE_RESISTANCE, s30, 2), e(MobEffects.DAMAGE_BOOST, s30, 1), e(MobEffects.REGENERATION, s30, 2)),
				new Drink("can_bepis", 0, 0, "ring_pull", "can_empty", e(MobEffects.MOVEMENT_SPEED, s30, 3), e(MobEffects.DAMAGE_RESISTANCE, s30, 3)),
				new Drink("can_breen", 0, 0, "ring_pull", "can_empty", e(MobEffects.CONFUSION, s30, 0)),
				new Drink("can_mug", 0, 0, "ring_pull", "can_empty", e(MobEffects.DAMAGE_RESISTANCE, 3 * 60 * 20, 2), e(MobEffects.REGENERATION, 60 * 20, 2)),
				new Drink("bottle_nuka", 4, 5, "cap_nuka", "bottle_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DIG_SPEED, s30, 1)),
				new Drink("bottle_cherry", 6, 5, "cap_nuka", "bottle_empty", e(MobEffects.MOVEMENT_SPEED, s30, 0), e(MobEffects.JUMP, s30, 2)),
				new Drink("bottle_quantum", 10, 15, "cap_quantum", "bottle_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DAMAGE_RESISTANCE, s30, 2), e(MobEffects.DAMAGE_BOOST, s30, 1)),
				new Drink("bottle2_korl", 6, 0, "cap_korl", "bottle2_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DIG_SPEED, s30, 2), e(MobEffects.DAMAGE_BOOST, s30, 2)),
				new Drink("bottle2_fritz", 6, 0, "cap_fritz", "bottle2_empty", e(MobEffects.MOVEMENT_SPEED, s30, 1), e(MobEffects.DAMAGE_RESISTANCE, s30, 2), e(MobEffects.JUMP, s30, 2)),
				new Drink("bottle_sparkle", 10, 5, "cap_sparkle", "bottle_empty", e(MobEffects.MOVEMENT_SPEED, s120, 1), e(MobEffects.DAMAGE_RESISTANCE, s120, 2), e(MobEffects.DAMAGE_BOOST, s120, 2), e(MobEffects.DIG_SPEED, s120, 1)),
				new Drink("bottle_rad", 10, 15, "cap_rad", "bottle_empty", e(MobEffects.MOVEMENT_SPEED, s120, 1), e(MobEffects.DAMAGE_RESISTANCE, s120, 2), e(MobEffects.FIRE_RESISTANCE, s120, 0), e(MobEffects.DAMAGE_BOOST, s120, 4), e(MobEffects.DIG_SPEED, s120, 1)),
				new Drink("coffee", 10, 0, null, null, e(MobEffects.MOVEMENT_SPEED, 60 * 20, 2)),
				new Drink("coffee_radium", 10, 500, null, null, e(MobEffects.MOVEMENT_SPEED, 60 * 20, 2)),
		};
		ServerPlayer p = player(server, level);
		for(Drink d : drinks) {
			boolean bottle = d.id.startsWith("bottle");
			reset(p);
			p.setHealth(2F);
			if(bottle) {
				// requiresOpener: no bottle opener in the inventory, no drinking
				ItemStack closed = use(level, p, new ItemStack(it(d.id), 2));
				check(problems, cases, closed.getCount() == 2 && effectCount(p) == 0 && p.getHealth() == 2F, d.id + ": needs a bottle opener");
				p.getInventory().add(new ItemStack(it("bottle_opener")));
			}
			ItemStack hand = use(level, p, new ItemStack(it(d.id), 2));
			float rad = HbmLivingRadiation.get(p).getRadiation();
			check(problems, cases, hand.getCount() == 1 && p.getHealth() == Math.min(20F, 2F + d.heal) && has(p, d.effects) && effectCount(p) == d.effects.length && rad == d.rads
					&& (d.cap == null ? invCount(p) == 0 : count(p, d.cap) == 1 && count(p, d.container) == 1 && invCount(p) == (bottle ? 3 : 2)),
					d.id + ": heal " + d.heal + ", rad " + d.rads + ", " + d.effects.length + " effects, cap " + d.cap + ", tare " + d.container
							+ " (hp " + p.getHealth() + ", rad " + rad + ", effects " + effectCount(p) + ", inventory " + invCount(p) + ")");
			if(d.container != null) {
				hand = use(level, p, hand);
				check(problems, cases, hand.is(it(d.container)) && hand.getCount() == 1 && count(p, d.cap) == 2, d.id + ": the last one leaves the tare in hand");
			}
		}
		// ContaminationUtil RAD_BYPASS: players younger than 200 ticks only get the environment readout
		reset(p);
		p.tickCount = 100;
		p.getInventory().add(new ItemStack(it("bottle_opener")));
		use(level, p, new ItemStack(it("bottle_nuka"), 2));
		check(problems, cases, HbmLivingRadiation.get(p).getRadiation() == 0F, "bottle_nuka: no dose for a player younger than 200 ticks (rad " + HbmLivingRadiation.get(p).getRadiation() + ", env " + HbmLivingRadiation.get(p).getEnvironmentRadiation() + ")");
		// creative: nothing consumed, no cap, no tare
		reset(p);
		p.getAbilities().instabuild = true;
		ItemStack kept = use(level, p, new ItemStack(it("can_smart"), 2));
		check(problems, cases, kept.getCount() == 2 && invCount(p) == 0 && effectCount(p) == 3, "can_smart in creative: stack kept, no ring pull");
		p.getAbilities().instabuild = false;
		// FakePlayer: explosion 5F at the drinker, no effects, no tare, one can gone
		FakePlayer fake = FakePlayerFactory.getMinecraft(level);
		fake.setPos(X, Y, Z);
		fake.getInventory().clearContent();
		fake.removeAllEffects();
		ItemStack fakeHand = use(level, fake, new ItemStack(it("can_smart"), 2));
		check(problems, cases, fakeHand.getCount() == 1 && count(fake, "ring_pull") == 0 && count(fake, "can_empty") == 0 && fake.getActiveEffects().isEmpty(), "can_smart: a fake player only gets the explosion");
		fake.getInventory().clearContent();
		// chocolate_milk: ExplosionVNT 50 at the drinker
		// (the test players are not part of the level, so a pig next to the drinker takes the blast)
		Pig bystander = pig(level);
		ServerPlayer drinker = player(server, level);
		reset(drinker);
		ItemStack milk = use(level, drinker, new ItemStack(it("chocolate_milk"), 2));
		check(problems, cases, milk.getCount() == 1 && (bystander.isDeadOrDying() || bystander.isRemoved()) && bystander.getLastDamageSource() != null
				&& bystander.getLastDamageSource().getMsgId().startsWith("explosion"), "chocolate_milk: explodes at the drinker (pig hp " + bystander.getHealth() + ")");
	}

	/// ItemCigarette.java ///
	private static void smoking(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		// cigarette: black lung + 2000, asbestos + 2000, radiation + 100
		reset(p);
		ItemStack hand = use(level, p, new ItemStack(it("cigarette"), 2));
		check(problems, cases, hand.getCount() == 1 && HbmLivingHazards.get(p).getBlackLung() == (HbmConfig.ENABLE_COAL_DUST.get() ? 2000 : 0)
				&& HbmLivingHazards.get(p).getAsbestos() == (HbmConfig.ENABLE_ASBESTOS.get() ? 2000 : 0) && HbmLivingRadiation.get(p).getRadiation() == 100F && invCount(p) == 0,
				"cigarette: +2000 black lung, +2000 asbestos, +100 RAD");
		// crackpipe: black lung + 500, confusion 200, 0, heal 10
		reset(p);
		p.setHealth(4F);
		hand = use(level, p, new ItemStack(it("crackpipe"), 1));
		check(problems, cases, hand.isEmpty() && HbmLivingHazards.get(p).getBlackLung() == (HbmConfig.ENABLE_COAL_DUST.get() ? 500 : 0) && HbmLivingHazards.get(p).getAsbestos() == 0
				&& has(p, e(MobEffects.CONFUSION, 200, 0)) && p.getHealth() == 14F, "crackpipe: +500 black lung, Nausea 200, +10 HP");
		// stack.stackSize-- without a creative check
		reset(p);
		p.getAbilities().instabuild = true;
		hand = use(level, p, new ItemStack(it("cigarette"), 2));
		check(problems, cases, hand.getCount() == 1, "cigarette: consumed in creative too");
		p.getAbilities().instabuild = false;
	}

	/// items/food/*.java ///
	private static void foods(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		// new ItemLemon(hunger, saturation, wolfFood) / ItemFood from ModItems.java; none of them is always edible
		Object[][] lemons = { { "lemon", 3, 0.5F }, { "definitelyfood", 3, 0.5F }, { "med_ipecac", 0, 0F }, { "med_ptsd", 0, 0F }, { "med_schizophrenia", 0, 0F },
				{ "loops", 4, 0.25F }, { "loop_stew", 10, 0.5F }, { "spongebob_macaroni", 5, 1F }, { "fooditem", 2, 5F }, { "twinkie", 3, 0.25F }, { "static_sandwich", 6, 1F },
				{ "pudding", 6, 1F }, { "nugget", 200, 1F }, { "cheese", 5, 0.75F }, { "cheese_quesadilla", 8, 1F }, { "glyphid_meat", 3, 0.5F }, { "glyphid_meat_grilled", 8, 0.75F },
				{ "ingot_semtex", 4, 5F }, { "ingot_c4", 4, 5F }, { "powder_cement", 2, 0.5F }, { "bio_wafer", 4, 2F }, { "ingot_smore", 10, 20F },
				{ "glowing_stew", 6, 0.6F }, { "balefire_scrambled", 6, 0.6F }, { "balefire_and_ham", 6, 0.6F }, { "bomb_waffle", 20, 0.6F }, { "schnitzel_vegan", 0, 0.6F } };
		for(Object[] l : lemons) {
			String id = (String) l[0];
			int nutrition = (int) l[1];
			float mod = (float) l[2];
			var food = new ItemStack(it(id)).getFoodProperties(p);
			check(problems, cases, food != null && food.nutrition() == nutrition && Math.abs(food.saturation() - nutrition * mod * 2F) < 0.001F && !food.canAlwaysEat(),
					id + ": food " + nutrition + " x " + mod + ", not always edible");
			if(id.equals("bomb_waffle") || food == null) continue; // ExplosionNukeSmall PARAMS_MEDIUM is not detonated here
			reset(p);
			p.getFoodData().setFoodLevel(20);
			ItemStack full = use(level, p, new ItemStack(it(id), 1));
			check(problems, cases, full.getCount() == 1, id + ": cannot be eaten with a full hunger bar");
			reset(p);
			p.getFoodData().setFoodLevel(0);
			p.getFoodData().setSaturation(0F);
			boolean bowl = id.equals("loop_stew") || id.equals("glowing_stew") || id.equals("balefire_scrambled") || id.equals("balefire_and_ham");
			ItemStack hand = use(level, p, new ItemStack(it(id), 1));
			int expectedFood = Math.min(20, nutrition);
			check(problems, cases, (bowl ? hand.is(Items.BOWL) && hand.getCount() == 1 : hand.isEmpty()) && p.getFoodData().getFoodLevel() == expectedFood
					&& Math.abs(p.getFoodData().getSaturationLevel() - Math.min(expectedFood, nutrition * mod * 2F)) < 0.001F,
					id + ": eaten, hunger " + expectedFood + (bowl ? ", bowl back" : "") + " (food " + p.getFoodData().getFoodLevel() + ", sat " + p.getFoodData().getSaturationLevel() + ", hand " + hand + ")");
			switch(id) {
				// ItemLemon.onFoodEaten
				case "med_ipecac", "med_ptsd" -> check(problems, cases, has(p, e(MobEffects.HUNGER, 50, 49)) && effectCount(p) == 1, id + ": Hunger L 50 ticks");
				case "loop_stew" -> check(problems, cases, has(p, e(MobEffects.REGENERATION, 20 * 20, 1), e(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 2), e(MobEffects.MOVEMENT_SPEED, 60 * 20, 1),
						e(MobEffects.DAMAGE_BOOST, 20 * 20, 2)) && effectCount(p) == 4, "loop_stew: four effects");
				// ItemSchnitzelVegan.onFoodEaten: blindness 200, confusion 600, hunger 3600/4, wither 60, setFire(100 s), motionY = 2
				case "schnitzel_vegan" -> check(problems, cases, has(p, e(MobEffects.BLINDNESS, 10 * 20, 0), e(MobEffects.CONFUSION, 30 * 20, 0), e(MobEffects.HUNGER, 3 * 60 * 20, 4),
						e(MobEffects.WITHER, 3 * 20, 0)) && effectCount(p) == 4 && p.getRemainingFireTicks() == 5 * 20 * 20 && p.getDeltaMovement().y == 2D,
						"schnitzel_vegan: four debuffs, 100 s of fire, motionY 2 (fire " + p.getRemainingFireTicks() + ", y " + p.getDeltaMovement().y + ")");
				// ItemLemon.onFoodEaten never calls ItemFood.onFoodEaten, so setPotionEffect(damageBoost, 180, 1, 1F) is dead code
				default -> check(problems, cases, effectCount(p) == 0, id + ": no effects");
			}
		}
		// ItemCottonCandy(5, false), always edible: poison 300, wither 100, weakness 500/2, moveSpeed 500/2, resistance 600/4
		reset(p);
		ItemStack hand = use(level, p, new ItemStack(it("cotton_candy"), 1));
		check(problems, cases, hand.isEmpty() && has(p, e(MobEffects.POISON, 15 * 20, 0), e(MobEffects.WITHER, 5 * 20, 0), e(MobEffects.WEAKNESS, 25 * 20, 2), e(MobEffects.MOVEMENT_SPEED, 25 * 20, 2),
				e(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 4)) && effectCount(p) == 5 && food(new ItemStack(it("cotton_candy")), p, 5, 0.6F, true), "cotton_candy: five effects, 5 x 0.6, always edible");
		// ItemAppleSchrabidium(20, 100, false), always edible
		int max = Integer.MAX_VALUE;
		E[][] apples = {
				{ e(MobEffects.REGENERATION, 600, 4), e(MobEffects.DAMAGE_RESISTANCE, 6000, 0), e(MobEffects.FIRE_RESISTANCE, 6000, 0) },
				{ e(MobEffects.REGENERATION, 1200, 4), e(MobEffects.DAMAGE_RESISTANCE, 1200, 4), e(MobEffects.FIRE_RESISTANCE, 1200, 0), e(MobEffects.DAMAGE_BOOST, 1200, 4), e(MobEffects.DIG_SPEED, 1200, 2),
						e(MobEffects.MOVEMENT_SPEED, 1200, 2), e(MobEffects.JUMP, 1200, 4), e(MobEffects.HEALTH_BOOST, 1200, 9), e(MobEffects.ABSORPTION, 1200, 4), e(MobEffects.SATURATION, 1200, 9) },
				{ e(MobEffects.REGENERATION, max, 4), e(MobEffects.DAMAGE_RESISTANCE, max, 1), e(MobEffects.FIRE_RESISTANCE, max, 0), e(MobEffects.DAMAGE_BOOST, max, 9), e(MobEffects.DIG_SPEED, max, 4),
						e(MobEffects.MOVEMENT_SPEED, max, 3), e(MobEffects.JUMP, max, 4), e(MobEffects.HEALTH_BOOST, max, 24), e(MobEffects.ABSORPTION, max, 14), e(MobEffects.SATURATION, max, 99) } };
		for(int meta = 0; meta < 3; meta++) {
			reset(p);
			hand = use(level, p, LegacySpecialFoodItem.stackFor(it("apple_schrabidium"), meta));
			check(problems, cases, hand.isEmpty() && has(p, apples[meta]) && effectCount(p) == apples[meta].length, "apple_schrabidium meta " + meta + ": " + apples[meta].length + " effects (" + effectCount(p) + ")");
		}
		check(problems, cases, food(new ItemStack(it("apple_schrabidium")), p, 20, 100F, true) && food(new ItemStack(it("apple_lead")), p, 5, 0F, true), "apples: 20 x 100 and 5 x 0, always edible");
		// apple_lead: lead 15 * 20, 2 / 60 * 20, 4 / ModDamageSource.lead 500
		reset(p);
		use(level, p, LegacySpecialFoodItem.stackFor(it("apple_lead"), 0));
		check(problems, cases, has(p, e(HbmMobEffects.LEAD_POISONING, 15 * 20, 2)) && effectCount(p) == 1, "apple_lead meta 0: Lead 300 level 2");
		reset(p);
		use(level, p, LegacySpecialFoodItem.stackFor(it("apple_lead"), 1));
		check(problems, cases, has(p, e(HbmMobEffects.LEAD_POISONING, 60 * 20, 4)) && effectCount(p) == 1, "apple_lead meta 1: Lead 1200 level 4");
		ServerPlayer victim = player(server, level);
		reset(victim);
		use(level, victim, LegacySpecialFoodItem.stackFor(it("apple_lead"), 2));
		check(problems, cases, victim.isDeadOrDying() && victim.getLastDamageSource() != null && victim.getLastDamageSource().getMsgId().equals("lead"), "apple_lead meta 2: 500 lead damage");
		// ItemAppleEuphemium(20, 100, false): resistance max/120, fireResistance max/0, saturation max/120
		reset(p);
		use(level, p, new ItemStack(it("apple_euphemium"), 1));
		check(problems, cases, has(p, e(MobEffects.DAMAGE_RESISTANCE, max, 120), e(MobEffects.FIRE_RESISTANCE, max, 0), e(MobEffects.SATURATION, max, 120)) && effectCount(p) == 3
				&& food(new ItemStack(it("apple_euphemium")), p, 20, 100F, true), "apple_euphemium: three permanent effects");
		// ItemTemFlakes(0, 0, false), always edible: heal(2)
		for(int meta = 0; meta < 3; meta++) {
			reset(p);
			p.setHealth(10F);
			hand = use(level, p, LegacySpecialFoodItem.stackFor(it("tem_flakes"), meta));
			check(problems, cases, hand.isEmpty() && p.getHealth() == 12F && p.getFoodData().getFoodLevel() == 10, "tem_flakes meta " + meta + ": +2 HP, no hunger");
		}
		// ItemPancake(20, 20, false): only with the full BJ armor set, otherwise a yellow chat line
		reset(p);
		hand = use(level, p, new ItemStack(it("pancake"), 1));
		check(problems, cases, hand.getCount() == 1 && !p.isUsingItem() && food(new ItemStack(it("pancake")), p, 20, 20F, true), "pancake: refused without lunar cybernetic armor, 20 x 20");
		// ItemMuchoMango(10): moveSpeed 200, 0
		reset(p);
		hand = use(level, p, new ItemStack(it("mucho_mango"), 1));
		check(problems, cases, hand.isEmpty() && has(p, e(MobEffects.MOVEMENT_SPEED, 200, 0)) && effectCount(p) == 1 && food(new ItemStack(it("mucho_mango")), p, 10, 0.6F, true), "mucho_mango: Speed 200, 10 x 0.6");
		// ItemCrayon(3, false), always edible; ItemWaffle(20, false)
		check(problems, cases, food(new ItemStack(it("crayon")), p, 3, 0.6F, true), "crayon: 3 x 0.6, always edible");
		// ItemConserve.EnumFoodType: (foodLevel, saturation), can_key back, random.burp
		Object[][] cans = { { "BEEF", 8, 0.75F }, { "TUNA", 4, 0.75F }, { "MYSTERY", 6, 0.5F }, { "PASHTET", 4, 0.5F }, { "CHEESE", 3, 1F }, { "SLIME", 15, 5F }, { "MILK", 5, 0.25F },
				{ "ASS", 6, 0.75F }, { "PIZZA", 8, 075F }, { "TUBE", 2, 0.25F }, { "TOMATO", 4, 0.5F }, { "ASBESTOS", 7, 1F }, { "BHOLE", 10, 1F }, { "HOTDOGS", 5, 0.75F },
				{ "LEFTOVERS", 1, 0.1F }, { "YOGURT", 3, 0.5F }, { "STEW", 5, 0.5F }, { "CHINESE", 6, 0.1F }, { "OIL", 3, 1F }, { "FIST", 6, 0.75F }, { "SPAM", 8, 1F },
				{ "FRIED", 10, 0.75F }, { "NAPALM", 6, 1F }, { "DIESEL", 6, 1F }, { "KEROSENE", 6, 1F }, { "RECURSION", 1, 1F }, { "BARK", 2, 1F } };
		check(problems, cases, LegacyConserveItem.Variant.values().length == cans.length, "canned_conserve: 27 variants");
		for(int i = 0; i < cans.length && i < LegacyConserveItem.Variant.values().length; i++) {
			LegacyConserveItem.Variant variant = LegacyConserveItem.Variant.values()[i];
			int nutrition = (int) cans[i][1];
			float mod = (float) cans[i][2];
			reset(p);
			p.getFoodData().setFoodLevel(0);
			p.getFoodData().setSaturation(0F);
			hand = use(level, p, LegacyConserveItem.stackFor(it("canned_conserve"), variant));
			boolean recursion = variant == LegacyConserveItem.Variant.RECURSION;
			int back = count(p, "canned_conserve");
			check(problems, cases, variant.name().equals(cans[i][0]) && (hand.isEmpty() || hand.is(it("can_key"))) && p.getFoodData().getFoodLevel() == nutrition
					&& Math.abs(p.getFoodData().getSaturationLevel() - Math.min(nutrition, nutrition * mod * 2F)) < 0.001F && count(p, "can_key") == 1 && (recursion ? back <= 1 : back == 0)
					&& p.getHealth() == (variant == LegacyConserveItem.Variant.FIST ? 18F : 20F),
					"canned_" + cans[i][0] + ": " + nutrition + " x " + mod + ", can key (food " + p.getFoodData().getFoodLevel() + ", sat " + p.getFoodData().getSaturationLevel() + ", hp " + p.getHealth() + ")");
			if(variant == LegacyConserveItem.Variant.BHOLE) {
				List<LegacyVortexEntity> vortex = level.getEntitiesOfClass(LegacyVortexEntity.class, new AABB(X - 4, Y - 4, Z - 4, X + 4, Y + 4, Z + 4));
				check(problems, cases, vortex.size() == 1, "canned_bhole: spawns one vortex (" + vortex.size() + ")");
				vortex.forEach(LegacyVortexEntity::discard);
			}
		}
		reset(p);
		p.getFoodData().setFoodLevel(20);
		hand = use(level, p, LegacyConserveItem.stackFor(it("canned_conserve"), LegacyConserveItem.Variant.BEEF));
		check(problems, cases, hand.getCount() == 1, "canned_conserve: needs hunger");
	}

	/// ItemCanteen.java, ItemFlask.java, ItemPeas.java, cbt_device (ItemSyringe.java) ///
	private static void misc(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		// canteen_vodka: confusion 200/0, damageBoost 600/2, damage = max damage 3 * 60, one point back per second
		reset(p);
		ItemStack hand = use(level, p, new ItemStack(it("canteen_vodka"), 1));
		check(problems, cases, hand.getMaxDamage() == 3 * 60 && hand.getDamageValue() == 3 * 60 && has(p, e(MobEffects.CONFUSION, 10 * 20, 0), e(MobEffects.DAMAGE_BOOST, 30 * 20, 2)) && effectCount(p) == 2, "canteen_vodka: Nausea 200, Strength III 600, cooldown 180");
		p.removeAllEffects();
		hand = use(level, p, hand);
		check(problems, cases, effectCount(p) == 0, "canteen_vodka: unusable during the cooldown");
		p.tickCount = 400;
		hand.inventoryTick(level, p, 0, true);
		p.tickCount = 401;
		hand.inventoryTick(level, p, 0, true);
		check(problems, cases, hand.getDamageValue() == 3 * 60 - 1, "canteen_vodka: cooldown drops once per 20 ticks (" + hand.getDamageValue() + ")");
		// flask_infusion SHIELD: maxShield + 5 (cap shieldCap), shield + 5
		reset(p);
		float before = HbmPlayerShield.get(p).maxShield();
		hand = use(level, p, new ItemStack(it("flask_infusion"), 2));
		check(problems, cases, before >= HbmPlayerShield.CAP ? hand.getCount() == 2 : hand.getCount() == 1 && HbmPlayerShield.get(p).maxShield() == Math.min(HbmPlayerShield.CAP, before + 5F), "flask_infusion: +5 max shield (" + before + " -> " + HbmPlayerShield.get(p).maxShield() + ")");
		// cbt_device: HbmPotion.bang 30, 0
		reset(p);
		hand = use(level, p, new ItemStack(it("cbt_device"), 1));
		check(problems, cases, hand.isEmpty() && has(p, e(HbmMobEffects.BANG, 30, 0)), "cbt_device: Bang 30 ticks");
		p.removeAllEffects();
		// peas: one consumed, every EntityQuackos within 50 blocks despawns with three spawn_duck
		reset(p);
		LegacyQuackosEntity duck = HbmEntityTypes.QUACKOS.get().create(level);
		if(duck != null) {
			duck.setPos(X + 20, Y, Z);
			level.addFreshEntity(duck);
		}
		hand = use(level, p, new ItemStack(it("peas"), 2));
		int eggs = 0;
		for(ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(X + 10, Y - 20, Z - 10, X + 30, Y + 30, Z + 10))) if(item.getItem().is(it("spawn_duck"))) { eggs += item.getItem().getCount(); item.discard(); }
		check(problems, cases, hand.getCount() == 1 && duck != null && duck.isRemoved() && eggs == 3, "peas: consumed, the duck leaves three spawn items (removed " + (duck != null && duck.isRemoved()) + ", items " + eggs + ")");
		if(duck != null && !duck.isRemoved()) duck.discard();
	}

	/// HbmPotion.java ///
	private static void effects(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		// isReady: taint every second tick, radaway / phosphorus every tick, bang during the last ten ticks, lead every 60 ticks
		MobEffect taint = HbmMobEffects.TAINT.get(), radaway = HbmMobEffects.RADAWAY.get(), bang = HbmMobEffects.BANG.get(), lead = HbmMobEffects.LEAD_POISONING.get(), phosphorus = HbmMobEffects.PHOSPHORUS_BURN.get();
		check(problems, cases, taint.shouldApplyEffectTickThisTick(2, 0) && !taint.shouldApplyEffectTickThisTick(3, 0) && radaway.shouldApplyEffectTickThisTick(7, 0)
				&& phosphorus.shouldApplyEffectTickThisTick(7, 0) && bang.shouldApplyEffectTickThisTick(10, 0) && !bang.shouldApplyEffectTickThisTick(11, 0)
				&& lead.shouldApplyEffectTickThisTick(120, 0) && !lead.shouldApplyEffectTickThisTick(119, 0), "potion tick schedule");
		check(problems, cases, !HbmMobEffects.RADX.get().shouldApplyEffectTickThisTick(1, 0) && !HbmMobEffects.STABILITY.get().shouldApplyEffectTickThisTick(1, 0)
				&& !HbmMobEffects.MUTATION.get().shouldApplyEffectTickThisTick(1, 0) && !HbmMobEffects.POTION_SICKNESS.get().shouldApplyEffectTickThisTick(1, 0), "radx/stability/mutation/sickness never tick");
		// registerPotion colors: taint 0x800080, bang 0x111111, mutation 0x800080, radx 0xBB4B00, lead 0x767682, radaway 0xBB4B00, phosphorus 0xFFFF00, stability 0xD0D0D0, potionsickness 0xff8080, death 0x111111
		check(problems, cases, taint.getColor() == 0x800080 && bang.getColor() == 0x111111 && HbmMobEffects.MUTATION.get().getColor() == 0x800080 && HbmMobEffects.RADX.get().getColor() == 0xBB4B00
				&& lead.getColor() == 0x767682 && radaway.getColor() == 0xBB4B00 && phosphorus.getColor() == 0xFFFF00 && HbmMobEffects.STABILITY.get().getColor() == 0xD0D0D0
				&& HbmMobEffects.POTION_SICKNESS.get().getColor() == 0xFF8080 && HbmMobEffects.DEATH.get().getColor() == 0x111111, "potion colors");
		// radaway: incrementRadiation(-(level + 1)) per tick
		ServerPlayer p = player(server, level);
		reset(p);
		radiation(p, 500F, 0F);
		radaway.applyEffectTick(p, 0);
		float one = HbmLivingRadiation.get(p).getRadiation();
		radaway.applyEffectTick(p, 9);
		check(problems, cases, one == 499F && HbmLivingRadiation.get(p).getRadiation() == 489F, "radaway: level + 1 RAD per tick");
		// phosphorus: setFire(1)
		reset(p);
		phosphorus.applyEffectTick(p, 0);
		check(problems, cases, p.getRemainingFireTicks() == 20, "phosphorus: one second of fire");
		p.clearFire();
		// bang: 1000 bang damage, health 0, non-players are removed; a cow drops 3 cheese (a calf 10)
		Pig pig = pig(level);
		bang.applyEffectTick(pig, 0);
		check(problems, cases, pig.isRemoved(), "bang: mobs are removed");
		Cow cow = EntityType.COW.create(level);
		if(cow != null) {
			cow.setPos(X + 6, Y, Z);
			level.addFreshEntity(cow);
			bang.applyEffectTick(cow, 0);
		}
		int cheese = 0;
		for(ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(X + 2, Y - 6, Z - 4, X + 10, Y + 6, Z + 4))) if(item.getItem().is(it("cheese"))) { cheese += item.getItem().getCount(); item.discard(); }
		check(problems, cases, cow != null && cow.isRemoved() && cheese == 3, "bang: a cow drops three cheese (" + cheese + ")");
		// VersatileConfig.applyPotionSickness: off by default; the effect itself has no curative items
		reset(p);
		LegacyConsumableSupport.applyPotionSickness(p, 5);
		check(problems, cases, !p.hasEffect(HbmMobEffects.POTION_SICKNESS), "potion sickness is not applied while the config is OFF");
		LegacyConsumableSupport.addIncurable(p, HbmMobEffects.POTION_SICKNESS, 100);
		check(problems, cases, p.hasEffect(HbmMobEffects.POTION_SICKNESS) && p.getEffect(HbmMobEffects.POTION_SICKNESS).getCures().isEmpty(), "potion sickness has no cures");
	}

	/// ConsumableRecipes.java, CraftingManager.java, AnvilRecipes.java ///
	private static void recipes(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		for(String id : new String[] { "apple_schrabidium", "apple_schrabidium_ingot", "apple_schrabidium_block", "apple_lead", "apple_lead_ingot", "apple_lead_block", "tem_flakes", "tem_flakes_1",
				"tem_flakes_2", "syringe_metal_stimpak", "syringe_metal_stimpak_from_nitra", "syringe_metal_stimpak_from_foxglove", "syringe_metal_super", "syringe_metal_super_nuka_rubber",
				"syringe_metal_super_cherry_leather", "syringe_metal_super_cherry_rubber", "med_bag", "med_bag_leather_iodine", "med_bag_leather_radaway", "med_bag_rubber_antidote",
				"med_bag_rubber_iodine", "med_bag_rubber_radaway", "definitelyfood", "definitelyfood_seeds", "canned_recursion", "ingot_smore", "bdcl", "quesadilla", "peas", "radaway",
				"radaway_strong", "radaway_flush", "iv_empty", "iv_xp_empty", "pill_iodine", "plan_c", "radx", "fmn", "five_htp", "cigarette", "crackpipe", "siox", "xanax", "pill_herbal" })
			check(problems, cases, server.getRecipeManager().byKey(AtomicLegacy.id(id)).isPresent(), "recipe " + id + " exists");
		ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
		ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
		ItemStack coal = tagItem("dusts/coal");
		// bottle_nuka: bottle_empty, Items.potionitem (damage 0 = water bottle), sugar, coal dust
		ItemStack out = craft(server, level, 2, 2, new ItemStack(it("bottle_empty")), water, new ItemStack(Items.SUGAR), coal);
		check(problems, cases, out.is(it("bottle_nuka")) && out.getCount() == 1, "bottle_nuka recipe with a water bottle");
		out = craft(server, level, 2, 2, new ItemStack(it("bottle_empty")), awkward, new ItemStack(Items.SUGAR), coal);
		check(problems, cases, out.isEmpty(), "bottle_nuka recipe rejects other potions (" + out + ")");
		// ingot_smore: wheat, roasted marshmallow (meta 1), cocoa
		ItemStack roasted = LegacyVariantItem.stackFor(it("marshmallow"), "roasted");
		out = craft(server, level, 3, 1, new ItemStack(Items.WHEAT), roasted, new ItemStack(Items.COCOA_BEANS));
		check(problems, cases, out.is(it("ingot_smore")), "ingot_smore recipe with a roasted marshmallow");
		out = craft(server, level, 3, 1, new ItemStack(Items.WHEAT), new ItemStack(it("marshmallow")), new ItemStack(Items.COCOA_BEANS));
		check(problems, cases, out.isEmpty(), "ingot_smore recipe rejects a raw marshmallow");
		// apples: nugget -> meta 0, ingot -> meta 1, block -> meta 2
		String[][] appleTags = { { "nuggets/", "0" }, { "ingots/", "1" }, { "storage_blocks/", "2" } };
		for(String mat : new String[] { "schrabidium", "lead" }) for(String[] t : appleTags) {
			ItemStack m = tagItem(t[0] + mat);
			out = craft(server, level, 3, 3, m, m, m, m, new ItemStack(Items.APPLE), m, m, m, m);
			int meta = Integer.parseInt(t[1]);
			CustomModelData cmd = out.get(DataComponents.CUSTOM_MODEL_DATA);
			check(problems, cases, out.is(it("apple_" + mat)) && (cmd == null ? 0 : cmd.value()) == meta && ItemStack.isSameItemSameComponents(out, LegacySpecialFoodItem.stackFor(it("apple_" + mat), meta)),
					"apple_" + mat + " from " + t[0] + mat + " gives meta " + meta + " (" + out + ")");
		}
		// tem_flakes meta 2: two gold ingots, two gold nuggets, paper
		out = craft(server, level, 3, 2, new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.PAPER), ItemStack.EMPTY);
		check(problems, cases, ItemStack.isSameItemSameComponents(out, LegacySpecialFoodItem.stackFor(it("tem_flakes"), 2)), "tem_flakes meta 2 recipe (" + out + ")");
		out = craft(server, level, 2, 2, new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.PAPER));
		check(problems, cases, ItemStack.isSameItemSameComponents(out, LegacySpecialFoodItem.stackFor(it("tem_flakes"), 1)), "tem_flakes meta 1 recipe (" + out + ")");
		// med_bag "LL", "SI", "LL": leather, syringe_metal_super, radaway
		ItemStack leather = new ItemStack(Items.LEATHER);
		out = craft(server, level, 2, 3, leather, leather, new ItemStack(it("syringe_metal_super")), new ItemStack(it("radaway")), leather, leather);
		check(problems, cases, out.is(it("med_bag")) && out.getCount() == 1, "med_bag from super stimpak + radaway");
		// med_bag "LLL", "SIS", "LLL" with ANY_RUBBER (latex) and pill_iodine
		ItemStack latex = new ItemStack(it("ingot_biorubber"));
		ItemStack stim = new ItemStack(it("syringe_metal_stimpak"));
		out = craft(server, level, 3, 3, latex, latex, latex, stim, new ItemStack(it("pill_iodine")), stim, latex, latex, latex);
		check(problems, cases, out.is(it("med_bag")), "med_bag from latex + stimpaks + iodine");
		// syringe_metal_super " N ", "PSP", "L L": bottle_cherry, steel plate, stimpak, ANY_RUBBER
		ItemStack plate = tagItem("plates/steel");
		ItemStack rubber = new ItemStack(it("ingot_rubber"));
		out = craft(server, level, 3, 3, ItemStack.EMPTY, new ItemStack(it("bottle_cherry")), ItemStack.EMPTY, plate, stim, plate, rubber, ItemStack.EMPTY, rubber);
		check(problems, cases, out.is(it("syringe_metal_super")), "super stimpak from cherry + rubber");
		// stimpak from three nitra_small + syringe_metal_empty
		ItemStack nitra = new ItemStack(it("nitra_small"));
		out = craft(server, level, 2, 2, nitra, nitra, nitra, new ItemStack(it("syringe_metal_empty")));
		check(problems, cases, out.is(it("syringe_metal_stimpak")) && out.getCount() == 1, "stimpak from nitra");
		// definitelyfood x4: ANY_RUBBER, wheat, rotten flesh, three seeds
		ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS);
		out = craft(server, level, 3, 2, latex, new ItemStack(Items.WHEAT), new ItemStack(Items.ROTTEN_FLESH), seeds, seeds, seeds);
		check(problems, cases, out.is(it("definitelyfood")) && out.getCount() == 4, "definitelyfood from seeds");
		// iv_empty x4: "S", "I", "S" with ANY_RUBBER and an iron plate
		out = craft(server, level, 1, 3, latex, tagItem("plates/iron"), latex);
		check(problems, cases, out.is(it("iv_empty")) && out.getCount() == 4, "iv_empty x4 from latex");
		// canned recursion -> canned recursion
		ItemStack recursion = LegacyConserveItem.stackFor(it("canned_conserve"), LegacyConserveItem.Variant.RECURSION);
		out = craft(server, level, 1, 1, recursion);
		check(problems, cases, ItemStack.isSameItemSameComponents(out, recursion), "canned recursion recipe");
		out = craft(server, level, 1, 1, LegacyConserveItem.stackFor(it("canned_conserve"), LegacyConserveItem.Variant.BEEF));
		check(problems, cases, out.isEmpty(), "canned recursion recipe only takes recursion");
		// AnvilSmithingRecipe(4, flask_infusion SHIELD, gem_alexandrite, bottle_nuka)
		var match = HbmAnvilRecipes.findSmithing(new ItemStack(it("gem_alexandrite")), new ItemStack(it("bottle_nuka")), 4);
		check(problems, cases, match != null && match.recipe().tier() == 4 && match.recipe().displayOutput().is(it("flask_infusion")), "anvil tier 4: alexandrite + nuka cola = shield infusion");
		check(problems, cases, HbmAnvilRecipes.findSmithing(new ItemStack(it("gem_alexandrite")), new ItemStack(it("bottle_nuka")), 3) == null, "shield infusion needs anvil tier 4");
	}

	/// ModItems.java setCreativeTab(MainRegistry.consumableTab) / partsTab / null ///
	private static void tabs(List<String> problems, int[] cases) {
		List<ItemStack> tab = HbmCreativeTabs.CONSUMABLES.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.CONSUMABLES.get().getDisplayItems());
		String[] consumable = { "syringe_empty", "syringe_metal_empty", "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super", "syringe_taint",
				"syringe_antidote", "syringe_poison", "syringe_awesome", "med_bag", "radaway", "radaway_strong", "radaway_flush", "iv_empty", "iv_blood", "iv_xp_empty", "iv_xp",
				"radx", "siox", "pill_herbal", "xanax", "fmn", "five_htp", "pill_iodine", "plan_c", "pill_red", "chocolate", "cigarette", "crackpipe", "bdcl", "jetpack_tank",
				"can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge", "can_luna", "can_bepis", "can_breen", "can_mug", "can_empty", "can_key", "ring_pull",
				"bottle_nuka", "bottle_cherry", "bottle_quantum", "bottle_sparkle", "bottle_rad", "bottle2_korl", "bottle2_fritz", "cap_nuka", "cap_quantum", "cap_sparkle", "cap_rad",
				"cap_korl", "cap_fritz", "flask_infusion", "chocolate_milk", "coffee", "coffee_radium", "bomb_waffle", "schnitzel_vegan", "cotton_candy", "apple_lead", "apple_schrabidium",
				"tem_flakes", "glowing_stew", "balefire_scrambled", "balefire_and_ham", "lemon", "definitelyfood", "med_ipecac", "med_ptsd", "med_schizophrenia", "loops", "loop_stew",
				"spongebob_macaroni", "fooditem", "twinkie", "static_sandwich", "pudding", "canteen_vodka", "pancake", "nugget", "marshmallow", "cheese", "cheese_quesadilla",
				"mucho_mango", "glyphid_meat", "glyphid_meat_grilled", "peas", "egg_glyphid", "canned_conserve" };
		for(String id : consumable) {
			boolean found = false;
			for(ItemStack s : tab) found |= s.is(it(id));
			check(problems, cases, found, id + " is on the consumables tab");
		}
		// getSubItems: three apples / tem flakes, two marshmallows, 27 cans
		Object[][] variants = { { "apple_lead", 3 }, { "apple_schrabidium", 3 }, { "tem_flakes", 3 }, { "marshmallow", 2 }, { "canned_conserve", 27 } };
		for(Object[] v : variants) {
			int n = 0;
			for(ItemStack s : tab) if(s.is(it((String) v[0]))) n++;
			check(problems, cases, n == (int) v[1], v[0] + " has " + v[1] + " tab entries (" + n + ")");
		}
		// no creative tab in ModItems: apple_euphemium, cbt_device, syringe_mkunicorn, bottle_empty, bottle2_empty
		for(String id : new String[] { "apple_euphemium", "cbt_device", "syringe_mkunicorn", "bottle_empty", "bottle2_empty" }) {
			boolean found = false;
			for(var other : BuiltInRegistries.CREATIVE_MODE_TAB) for(ItemStack s : other.getDisplayItems()) found |= s.is(it(id));
			check(problems, cases, !found, id + " is on no creative tab");
		}
		// partsTab: ingot_smore, ingot_semtex, ingot_c4, powder_cement, bio_wafer, crayon
		for(String id : new String[] { "ingot_smore", "ingot_semtex", "ingot_c4", "powder_cement", "bio_wafer", "crayon" }) {
			boolean found = false;
			for(ItemStack s : tab) found |= s.is(it(id));
			check(problems, cases, !found, id + " is not on the consumables tab");
		}
	}

	/// HELPERS ///
	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("parity H1: " + message);
	}

	private static Item it(String id) {
		return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
	}

	private static ItemStack tagItem(String path) {
		TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
		for(Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(key)) return new ItemStack(holder);
		return ItemStack.EMPTY;
	}

	/** A real (non-fake) server player: ItemEnergy and iv_empty treat FakePlayer differently. */
	private static ServerPlayer player(MinecraftServer server, ServerLevel level) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), "parity_h1");
		ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
		// like FakePlayer: a listener without a channel that swallows every packet
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
			@Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
			@Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { }
		};
		// ServerPlayer.hurt ignores damage during the 60 tick spawn protection, which only runs down in tick()
		net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
		player.setPos(X, Y, Z);
		player.setYRot(0F);
		player.setXRot(0F);
		return player;
	}

	private static void reset(ServerPlayer p) {
		p.stopUsingItem();
		p.removeAllEffects();
		p.getInventory().clearContent();
		p.getInventory().selected = 0;
		p.getAbilities().instabuild = false;
		p.setHealth(p.getMaxHealth());
		p.setAbsorptionAmount(0F);
		p.clearFire();
		p.setDeltaMovement(0D, 0D, 0D);
		p.getFoodData().setFoodLevel(10);
		p.getFoodData().setSaturation(0F);
		p.invulnerableTime = 0;
		p.tickCount = 400; // ContaminationUtil skips players younger than 200 ticks
		p.setExperienceLevels(0);
		p.setExperiencePoints(0);
		radiation(p, 0F, 0F);
		hazards(p, 0, 0);
	}

	private static void radiation(LivingEntity entity, float rad, float digamma) {
		HbmLivingRadiation data = HbmLivingRadiation.get(entity);
		data.setRadiation(rad);
		data.setDigamma(digamma);
		data.setEnvironmentRadiation(0F);
		HbmLivingRadiation.set(entity, data);
	}

	private static void hazards(LivingEntity entity, int asbestos, int blackLung) {
		HbmLivingHazards data = HbmLivingHazards.get(entity);
		data.setAsbestos(entity, asbestos);
		data.setBlackLung(entity, blackLung);
		HbmLivingHazards.set(entity, data);
	}

	/** Right click plus, when the item starts a use, the full use time (ServerPlayerGameMode.useItem + completeUsingItem). */
	private static ItemStack use(ServerLevel level, Player p, ItemStack stack) {
		p.setItemInHand(InteractionHand.MAIN_HAND, stack);
		InteractionResultHolder<ItemStack> result = stack.use(level, p, InteractionHand.MAIN_HAND);
		if(result.getObject() != stack) p.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());
		if(p.isUsingItem()) {
			ItemStack using = p.getUseItem();
			ItemStack finished = using.finishUsingItem(level, p);
			if(finished != using) p.setItemInHand(InteractionHand.MAIN_HAND, finished);
			p.stopUsingItem();
		}
		return p.getMainHandItem();
	}

	private static boolean has(LivingEntity entity, E... expected) {
		for(E e : expected) {
			MobEffectInstance instance = entity.getEffect(e.effect);
			if(instance == null || instance.getDuration() != e.duration || instance.getAmplifier() != e.amplifier) return false;
		}
		return true;
	}

	private static int effectCount(LivingEntity entity) {
		return entity.getActiveEffects().size();
	}

	private static boolean food(ItemStack stack, Player p, int nutrition, float mod, boolean always) {
		var food = stack.getFoodProperties(p);
		return food != null && food.nutrition() == nutrition && Math.abs(food.saturation() - nutrition * mod * 2F) < 0.001F && food.canAlwaysEat() == always;
	}

	/** Items of this id anywhere in the inventory. */
	private static int count(Player p, String id) {
		int n = 0;
		for(int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack s = p.getInventory().getItem(i);
			if(s.is(it(id))) n += s.getCount();
		}
		return n;
	}

	/** Occupied slots outside the selected hotbar slot. */
	private static int invCount(Player p) {
		int n = 0;
		for(int i = 0; i < p.getInventory().getContainerSize(); i++) {
			if(i == p.getInventory().selected) continue;
			if(!p.getInventory().getItem(i).isEmpty()) n++;
		}
		return n;
	}

	private static Pig pig(ServerLevel level) {
		Pig pig = EntityType.PIG.create(level);
		pig.setPos(X + 3, Y, Z);
		pig.setNoGravity(true);
		pig.setNoAi(true);
		level.addFreshEntity(pig);
		return pig;
	}

	private static ItemStack craft(MinecraftServer server, ServerLevel level, int w, int h, ItemStack... grid) {
		List<ItemStack> list = new ArrayList<>();
		for(ItemStack s : grid) list.add(s.copyWithCount(s.isEmpty() ? 0 : 1));
		CraftingInput input = CraftingInput.of(w, h, list);
		return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input, server.registryAccess())).orElse(ItemStack.EMPTY);
	}

	private static void lore(List<String> problems, int[] cases, ItemStack stack, String... expected) {
		List<Component> lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
		boolean ok = lines.size() == expected.length + 1;
		for(int i = 0; ok && i < expected.length; i++) ok = lines.get(i + 1).getString().equals(expected[i]);
		StringBuilder actual = new StringBuilder();
		for(int i = 1; i < lines.size(); i++) actual.append(i > 1 ? " | " : "").append(lines.get(i).getString());
		check(problems, cases, ok, BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + " lore, expected " + expected.length + " lines, is: " + actual);
	}

	private static void cleanup(ServerLevel level) {
		AABB area = new AABB(X - 64, Y - 64, Z - 64, X + 64, Y + 64, Z + 64);
		for(var entity : level.getEntities((net.minecraft.world.entity.Entity) null, area, e -> e instanceof Pig || e instanceof Cow || e instanceof ItemEntity || e instanceof LegacyVortexEntity || e instanceof LegacyQuackosEntity)) {
			if(entity.getY() > Y - 40) entity.discard();
		}
	}
}

package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.foundry.FoundryShape;
import com.atomiclegacy.items.FoundryMoldItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Dedicated-server checks for the 1.7.10 ItemAutogen gun parts (Mats#setAutogen), molds 22-28,
 * weapon_mod_special SCOPE, the crafting recipes of the batch A/B guns that need them
 * (WeaponRecipes lines 71-100), and the remaining ammo_standard / ammo_secret variants.
 */
final class GunPartsDiagnostics {

	private GunPartsDiagnostics() {
	}

	private static final List<String> BL = List.of("steel", "dura_steel", "desh", "tcalloy", "cdalloy", "bismuth_bronze", "arsenic_bronze", "gunmetal", "weaponsteel", "saturnite");
	private static final Map<String, List<String>> PARTS = Map.of(
			"barrel_light", BL,
			"barrel_heavy", List.of("steel", "dura_steel", "desh", "ferrouranium", "tcalloy", "cdalloy", "gunmetal", "weaponsteel", "saturnite"),
			"receiver_light", BL,
			"receiver_heavy", List.of("dura_steel", "ferrouranium", "tcalloy", "cdalloy", "bismuth_bronze", "arsenic_bronze", "gunmetal", "weaponsteel", "saturnite"),
			"mechanism", List.of("gunmetal", "weaponsteel", "saturnite"),
			"stock", List.of("wood", "desh", "gunmetal", "weaponsteel", "saturnite", "polymer", "bakelite", "pc", "pvc"),
			"grip", List.of("wood", "bone", "steel", "dura_steel", "desh", "gunmetal", "weaponsteel", "saturnite", "polymer", "bakelite", "rubber", "pc", "pvc"));
	private static final Map<String, FoundryShape> SHAPES = Map.of(
			"barrel_light", FoundryShape.BARREL_LIGHT, "barrel_heavy", FoundryShape.BARREL_HEAVY,
			"receiver_light", FoundryShape.RECEIVER_LIGHT, "receiver_heavy", FoundryShape.RECEIVER_HEAVY,
			"mechanism", FoundryShape.MECHANISM, "stock", FoundryShape.STOCK, "grip", FoundryShape.GRIP);
	private static final Map<String, Integer> MOLDS = Map.of(
			"barrel_light", 22, "barrel_heavy", 23, "receiver_light", 24, "receiver_heavy", 25, "mechanism", 26, "stock", 27, "grip", 28);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();

		/// Parts: registration, tags, crucible value, mold output ///
		int partCount = 0;
		for(var e : PARTS.entrySet()) {
			String shape = e.getKey();
			String prefix = "part_" + shape + "_";
			FoundryShape fs = SHAPES.get(shape);
			FoundryMoldItem.Mold mold = FoundryMoldItem.mold(FoundryMoldItem.stackFor(MOLDS.get(shape)));
			check(problems, cases, mold.cost() == fs.quanta() && mold.count() == 1, "mold " + shape + " cost/count");
			for(FoundryMaterial mat : FoundryMaterial.ordered()) {
				Optional<Item> item = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(prefix + mat.itemSuffix()));
				boolean expected = e.getValue().contains(mat.name());
				if(!expected) {
					check(problems, cases, item.isEmpty(), prefix + mat.itemSuffix() + " registered although Mats has no such autogen");
					if(mat.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE) check(problems, cases, mold.outputFor(mat).isEmpty(), "mold " + shape + " casts " + mat.name());
					continue;
				}
				partCount++;
				check(problems, cases, item.isPresent(), prefix + mat.itemSuffix() + " missing");
				if(item.isEmpty()) continue;
				ItemStack stack = new ItemStack(item.get());
				check(problems, cases, stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "gun_parts/" + shape + "/" + mat.name()))), prefix + mat.name() + " tag");
				if(mat.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE) {
					Optional<FoundryMaterialStack> smelt = FoundryMaterial.materialFromItem(stack);
					check(problems, cases, smelt.isPresent() && smelt.get().material() == mat && smelt.get().amount() == fs.quanta(), prefix + mat.name() + " crucible value " + smelt);
					check(problems, cases, mold.outputFor(mat).map(s -> s.is(item.get()) && s.getCount() == 1).orElse(false), "mold " + shape + " output for " + mat.name());
				}
			}
		}
		check(problems, cases, partCount == 63, "gun part count " + partCount);

		/// Crafted parts (WeaponRecipes lines 44-55) ///
		ItemStack planks = new ItemStack(Items.OAK_PLANKS);
		craft(server, level, problems, cases, "part_stock_wood", 3, 2, new ItemStack[] {planks, planks, planks, ItemStack.EMPTY, ItemStack.EMPTY, planks}, "part_stock_wood");
		craft(server, level, problems, cases, "part_grip_wood", 2, 3, new ItemStack[] {planks, ItemStack.EMPTY, ItemStack.EMPTY, planks, ItemStack.EMPTY, planks}, "part_grip_wood");
		ItemStack bone = new ItemStack(Items.BONE);
		craft(server, level, problems, cases, "part_grip_bone", 2, 3, new ItemStack[] {bone, ItemStack.EMPTY, ItemStack.EMPTY, bone, ItemStack.EMPTY, bone}, "part_grip_bone");
		for(String m : List.of("polymer", "bakelite", "pc", "pvc")) {
			ItemStack ingot = item("ingot_" + m);
			craft(server, level, problems, cases, "part_stock_" + m, 3, 2, new ItemStack[] {ingot, ingot, ingot, ItemStack.EMPTY, ItemStack.EMPTY, ingot}, "part_stock_" + m);
		}
		ItemStack rubber = item("ingot_rubber");
		craft(server, level, problems, cases, "part_grip_rubber", 2, 3, new ItemStack[] {rubber, ItemStack.EMPTY, ItemStack.EMPTY, rubber, ItemStack.EMPTY, rubber}, "part_grip_rubber");

		/// Scope (WeaponRecipes line 139) ///
		ItemStack plate = item("plate_steel"), poly = item("ingot_polymer"), pane = new ItemStack(Items.GLASS_PANE);
		craft(server, level, problems, cases, "weapon_mod_special_scope", 3, 3, new ItemStack[] {plate, poly, plate, pane, ItemStack.EMPTY, pane, plate, item("ingot_bakelite"), plate}, "weapon_mod_special_scope");
		check(problems, cases, new ItemStack(HbmItems.WEAPON_MOD_SCOPE.get()).getMaxStackSize() == 1, "scope stack size");
		ItemStack g3 = new ItemStack(HbmItems.GUN_G3.get());
		ItemStack scope = new ItemStack(HbmItems.WEAPON_MOD_SCOPE.get());
		check(problems, cases, XWeaponModManager.isApplicable(g3, scope, 0, true), "scope not applicable to gun_g3");
		XWeaponModManager.install(g3, 0, scope);
		ItemGunBaseNT g3gun = (ItemGunBaseNT) HbmItems.GUN_G3.get();
		check(problems, cases, XWeaponModManager.hasUpgrade(g3, 0, XWeaponModManager.ID_SCOPE)
				&& XFactory556mm.scope.equals(g3gun.getConfig(g3, 0).getScopeTexture(g3)) && g3gun.getConfig(g3, 0).getHideCrosshair(g3), "scope install on gun_g3");
		check(problems, cases, !XWeaponModManager.isApplicable(new ItemStack(HbmItems.GUN_UZI.get()), scope, 0, true), "scope applicable to gun_uzi");

		/// Gun recipes (WeaponRecipes lines 71-100) ///
		ItemStack E = ItemStack.EMPTY;
		gun(server, level, problems, cases, "gun_am180", 3, 2, new ItemStack[] {p("barrel_light", "dura_steel"), p("receiver_light", "dura_steel"), p("stock", "wood"), p("grip", "wood"), p("mechanism", "gunmetal"), p("grip", "wood")});
		for(String pl : List.of("polymer", "bakelite"))
			gun(server, level, problems, cases, "gun_uzi", 3, 2, new ItemStack[] {p("barrel_light", "desh"), p("receiver_light", "desh"), p("stock", pl), E, p("grip", pl), p("mechanism", "gunmetal")});
		gun(server, level, problems, cases, "gun_g3", 3, 2, new ItemStack[] {p("barrel_light", "weaponsteel"), p("receiver_light", "weaponsteel"), p("mechanism", "weaponsteel"), p("grip", "wood"), p("grip", "rubber"), p("stock", "wood")});
		gun(server, level, problems, cases, "gun_amat", 3, 3, new ItemStack[] {E, scope.copy(), E, p("barrel_heavy", "ferrouranium"), p("receiver_heavy", "ferrouranium"), p("stock", "wood"), E, p("mechanism", "weaponsteel"), p("grip", "wood")});
		gun(server, level, problems, cases, "gun_m2", 3, 3, new ItemStack[] {E, E, p("grip", "wood"), p("barrel_heavy", "ferrouranium"), p("receiver_heavy", "ferrouranium"), p("mechanism", "weaponsteel"), E, E, p("grip", "wood")});
		for(String ra : List.of("tcalloy", "cdalloy"))
			gun(server, level, problems, cases, "gun_lag", 3, 2, new ItemStack[] {p("barrel_light", ra), p("receiver_light", ra), p("mechanism", "weaponsteel"), E, E, p("grip", "polymer")});
		for(String hp : List.of("pc", "pvc"))
			gun(server, level, problems, cases, "gun_stg77", 3, 3, new ItemStack[] {E, scope.copy(), E, p("barrel_light", "saturnite"), p("receiver_light", "saturnite"), p("stock", hp), p("grip", hp), p("grip", hp), p("mechanism", "saturnite")});
		// a wrong material must not match
		ItemStack[] wrong = {p("barrel_light", "steel"), p("receiver_light", "weaponsteel"), p("mechanism", "weaponsteel"), p("grip", "wood"), p("grip", "rubber"), p("stock", "wood")};
		check(problems, cases, find(server, level, 3, 2, wrong).isEmpty(), "gun_g3 recipe accepts a steel barrel");

		/// Weapon tab: ported A_SIDE/SPECIAL guns in 1.7.10 ModItems registration order, before ammo_standard ///
		Item[] guns = { HbmItems.GUN_PEPPERBOX.get(), HbmItems.GUN_LIGHT_REVOLVER.get(), HbmItems.GUN_HENRY.get(), HbmItems.GUN_GREASEGUN.get(),
				HbmItems.GUN_MARESLEG.get(), HbmItems.GUN_HEAVY_REVOLVER.get(), HbmItems.GUN_CARBINE.get(), HbmItems.GUN_AM180.get(),
				HbmItems.GUN_LIBERATOR.get(), HbmItems.GUN_UZI.get(), HbmItems.GUN_STAR_F.get(), HbmItems.GUN_G3.get(), HbmItems.GUN_AMAT.get(),
				HbmItems.GUN_M2.get(), HbmItems.GUN_LAG.get(), HbmItems.GUN_MINIGUN.get(), HbmItems.GUN_STG77.get(), HbmItems.GUN_DOUBLE_BARREL.get(),
				HbmItems.AMMO_STANDARD.get() };
		List<ItemStack> wtab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		int prev = -1;
		boolean inOrder = true;
		for(Item g : guns) {
			int i = -1;
			for(int k = 0; k < wtab.size(); k++) if(wtab.get(k).is(g)) { i = k; break; }
			if(i <= prev) inOrder = false;
			prev = i;
		}
		check(problems, cases, inOrder, "weapon tab gun order (ModItems)");
		for(Item item : BuiltInRegistries.ITEM) {
			if(item instanceof ItemGunBaseNT gun) {
				boolean tabbed = gun.quality == ItemGunBaseNT.WeaponQuality.A_SIDE || gun.quality == ItemGunBaseNT.WeaponQuality.SPECIAL || gun.quality == ItemGunBaseNT.WeaponQuality.UTILITY;
				boolean listed = false;
				for(ItemStack s : wtab) if(s.is(item)) { listed = true; break; }
				check(problems, cases, tabbed == listed, BuiltInRegistries.ITEM.getKey(item) + " quality " + gun.quality + " tab=" + listed);
			}
		}

		/// Ammo ///
		LegacyVariantItem ammo = (LegacyVariantItem) HbmItems.AMMO_STANDARD.get();
		check(problems, cases, ammo.variantList().size() == 96, "ammo_standard variant count " + ammo.variantList().size());
		for(var v : ammo.variantList()) {
			check(problems, cases, StandardAmmoType.byId(v.id()) != StandardAmmoType.NONE, "ammo variant without StandardAmmoType: " + v.id());
		}
		List<ItemStack> weaponTab = HbmCreativeTabs.TURRETS.get() == null ? List.of() : new ArrayList<>(HbmCreativeTabs.TURRETS.get().getDisplayItems());
		List<String> tabAmmo = new ArrayList<>();
		for(ItemStack s : weaponTab) if(s.is(HbmItems.AMMO_STANDARD.get())) tabAmmo.add(StandardAmmoItem.standardType(s).id());
		check(problems, cases, tabAmmo.size() == 96 && tabAmmo.get(0).equals("stone") && tabAmmo.get(24).equals("p45_sp") && tabAmmo.get(95).equals("ct_mortar_charge"), "ammo creative order " + tabAmmo.size());
		ItemStack paper = new ItemStack(Items.PAPER), powder = new ItemStack(Items.GUNPOWDER);
		ammoCraft(server, level, problems, cases, new ItemStack(Items.COBBLESTONE), paper, powder, StandardAmmoType.STONE);
		ammoCraft(server, level, problems, cases, new ItemStack(Items.FLINT), paper, powder, StandardAmmoType.STONE_AP);
		ammoCraft(server, level, problems, cases, new ItemStack(Items.GRAVEL), paper, powder, StandardAmmoType.STONE_SHOT);
		ammoCraft(server, level, problems, cases, new ItemStack(Items.IRON_INGOT), paper, powder, StandardAmmoType.STONE_IRON);
		List<ItemStack> mortar = new ArrayList<>();
		for(int i = 0; i < 7; i++) mortar.add(StandardAmmoItem.stackFor(ammo, "ct_mortar"));
		mortar.add(item("ducttape")); mortar.add(item("ducttape"));
		var charge = find(server, level, 3, 3, mortar.toArray(new ItemStack[0]));
		check(problems, cases, charge.isPresent() && StandardAmmoItem.standardType(charge.get()) == StandardAmmoType.CT_MORTAR_CHARGE && charge.get().getCount() == 1, "ct_mortar_charge shapeless");
		int press = 0;
		for(var holder : server.getRecipeManager().getAllRecipesFor(com.atomiclegacy.registry.HbmRecipeTypes.AMMO_PRESS.get())) {
			if(holder.value().result().is(HbmItems.AMMO_STANDARD.get())) press++;
		}
		// AmmoPressRecipes registers 92 recipes, 2 of them are the existing tank shell and dgk recipes of other items
		check(problems, cases, press >= 90, "ammo press recipe count " + press);

		Item secret = HbmItems.AMMO_SECRET.get();
		check(problems, cases, secret instanceof LegacyVariantItem lv && lv.variantList().size() == 8, "ammo_secret variants");
		boolean secretInTab = false;
		for(CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
			for(ItemStack s : tab.getDisplayItems()) if(s.is(secret)) secretInTab = true;
		}
		check(problems, cases, !secretInTab, "ammo_secret is listed in a creative tab");
		ItemStack selenium = item("item_secret_selenium_steel");
		var eq = find(server, level, 2, 1, new ItemStack[] {StandardAmmoItem.stackFor(ammo, "m44_jhp"), selenium});
		check(problems, cases, eq.isPresent() && eq.get().is(secret) && eq.get().getCount() == 6, "m44_equestrian shapeless");
		return cases[0];
	}

	private static ItemStack p(String shape, String material) {
		return item("part_" + shape + "_" + material);
	}

	private static ItemStack item(String path) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(path)).map(Item::getDefaultInstance).orElse(ItemStack.EMPTY);
	}

	private static Optional<ItemStack> find(MinecraftServer server, ServerLevel level, int w, int h, ItemStack[] grid) {
		CraftingInput input = CraftingInput.of(w, h, List.of(grid));
		return server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
				.map(r -> ((CraftingRecipe) r.value()).assemble(input, server.registryAccess()));
	}

	private static void craft(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases, String recipe, int w, int h, ItemStack[] grid, String result) {
		var out = find(server, level, w, h, grid);
		check(problems, cases, out.isPresent() && out.get().is(BuiltInRegistries.ITEM.get(AtomicLegacy.id(result))) && out.get().getCount() == 1, "recipe " + recipe);
	}

	private static void gun(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases, String gun, int w, int h, ItemStack[] grid) {
		for(ItemStack s : grid) check(problems, cases, s != null, gun + " grid");
		craft(server, level, problems, cases, gun, w, h, grid, gun);
	}

	private static void ammoCraft(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases, ItemStack top, ItemStack paper, ItemStack powder, StandardAmmoType type) {
		var out = find(server, level, 1, 3, new ItemStack[] {top, paper, powder});
		check(problems, cases, out.isPresent() && StandardAmmoItem.standardType(out.get()) == type && out.get().getCount() == 6, "ammo recipe " + type.id());
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add(message);
	}
}

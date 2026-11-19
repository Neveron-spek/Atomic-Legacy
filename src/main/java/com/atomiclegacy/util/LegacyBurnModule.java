package com.atomiclegacy.util;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * [parity K3] Port of 1.7.10 {@code com.hbm.module.ModuleBurnTime} together with
 * {@code FuelHandler#getBurnTimeFromCache} (TileEntityFurnace#getItemBurnTime incl. HBM's FuelHandler).
 * <p>
 * The original classifies a stack by its ore dictionary names (first name containing "Coke", "Coal", "Lignite",
 * starting with "log", or containing "Wood"). Ore names are not available on 1.21, so the items that carried those
 * names in the reference (OreDictManager + Forge 1.7.10 vanilla entries) are listed explicitly:
 * Coke: coke_*, block_coke* (gemCoalCoke/fuelCoke/blockCoalCoke...); Coal: minecraft:coal (gemCoal), coal_block
 * (blockCoal), powder_coal (dustCoal), powder_coal_tiny (dustTinyCoal), briquette_coal (briquetteCoal); Lignite:
 * lignite, powder_lignite, briquette_lignite, ore_lignite; log: logs (logWood); Wood: planks, wooden slabs/stairs,
 * sticks (plankWood/slabWood/stairWood/stickWood), briquette_wood (briquetteWood).
 */
public final class LegacyBurnModule {
	public static final int LOG = 0;
	public static final int WOOD = 1;
	public static final int COAL = 2;
	public static final int LIGNITE = 3;
	public static final int COKE = 4;
	public static final int SOLID = 5;
	public static final int ROCKET = 6;
	public static final int BALEFIRE = 7;
	private static final String[] NAMES = {"Logs", "Wood", "Coal", "Lignite", "Coke", "Solid Fuel", "Rocket Fuel", "Balefire"};

	private final double[] modTime = new double[8];
	private final double[] modHeat = new double[8];

	public LegacyBurnModule() {
		for (int i = 0; i < 8; i++) {
			this.modTime[i] = 1.0D;
			this.modHeat[i] = 1.0D;
		}
	}

	public LegacyBurnModule time(int category, double mod) {
		this.modTime[category] = mod;
		return this;
	}

	public LegacyBurnModule heat(int category, double mod) {
		this.modHeat[category] = mod;
		return this;
	}

	/** FuelHandler.getBurnTimeFromCache: TileEntityFurnace#getItemBurnTime with HBM's IFuelHandler. */
	public static int baseBurnTime(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		String path = hbmPath(stack);
		if (path.equals("block_coke_lignite") || path.equals("block_coke_petroleum")) {
			return 200 * 160; // FuelHandler: any block_coke meta -> single * 160
		}
		int legacy = LegacyFurnaceFuels.burnTime(stack);
		if (legacy > 0) {
			return legacy;
		}
		return Math.max(0, stack.getBurnTime(RecipeType.SMELTING));
	}

	/** Index of the ModuleBurnTime category, or -1 for "def". */
	public static int category(ItemStack stack) {
		if (stack.isEmpty()) {
			return -1;
		}
		String path = hbmPath(stack);
		switch (path) {
			case "solid_fuel", "solid_fuel_presto", "solid_fuel_presto_triplet":
				return SOLID;
			case "solid_fuel_bf", "solid_fuel_presto_bf", "solid_fuel_presto_triplet_bf":
				return BALEFIRE;
			case "rocket_fuel":
				return ROCKET;
			case "powder_coal", "powder_coal_tiny", "briquette_coal":
				return COAL;
			case "lignite", "powder_lignite", "briquette_lignite", "ore_lignite":
				return LIGNITE;
			case "briquette_wood":
				return WOOD;
			default:
				break;
		}
		if (path.startsWith("coke_") || path.startsWith("block_coke")) {
			return COKE;
		}
		if (stack.is(Items.COAL) || stack.is(Items.COAL_BLOCK)) {
			return COAL;
		}
		if (stack.is(ItemTags.LOGS)) {
			return LOG;
		}
		if (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_SLABS) || stack.is(ItemTags.WOODEN_STAIRS) || stack.is(Items.STICK)) {
			return WOOD;
		}
		return -1;
	}

	public double mod(ItemStack stack, double[] mods, double def) {
		if (stack.isEmpty()) {
			return 0;
		}
		int category = category(stack);
		return category < 0 ? def : mods[category];
	}

	public int getBurnTime(ItemStack stack, double def) {
		int fuel = baseBurnTime(stack);
		if (fuel == 0) {
			return 0;
		}
		return (int) (fuel * mod(stack, this.modTime, def));
	}

	public int getBurnTime(ItemStack stack) {
		return getBurnTime(stack, 1.0D);
	}

	/** Note: the original ignores {@code def} here and always uses 1.0 for unclassified stacks. */
	public int getBurnHeat(int base, ItemStack stack) {
		if (base <= 0) {
			return 0;
		}
		return (int) (base * mod(stack, this.modHeat, 1.0D));
	}

	public List<Component> timeDesc() {
		return desc(ChatFormatting.GOLD + "Burn time bonuses:", this.modTime);
	}

	public List<Component> heatDesc() {
		return desc(ChatFormatting.RED + "Burn heat bonuses:", this.modHeat);
	}

	private static List<Component> desc(String title, double[] mods) {
		List<Component> list = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			if (mods[i] != 1.0D) {
				list.add(Component.literal(ChatFormatting.YELLOW + "- " + NAMES[i] + ": " + percent(mods[i])));
			}
		}
		if (!list.isEmpty()) {
			list.add(0, Component.literal(title));
		}
		return list;
	}

	private static String percent(double mod) {
		mod -= 1.0D;
		String num = ((int) (mod * 100)) + "%";
		if (mod < 0) {
			num = ChatFormatting.RED + num;
		} else {
			num = ChatFormatting.GREEN + "+" + num;
		}
		return num;
	}

	private static String hbmPath(ItemStack stack) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && AtomicLegacy.MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
	}
}

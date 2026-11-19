package com.atomiclegacy.items.weapon.sedna.impl;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryDrill;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.atomiclegacy.registry.HbmFluids;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * [weapons batch F3] 1.7.10 com.hbm.items.weapon.sedna.impl.ItemGunDrill.
 *
 * <p>IFillableItem is the NeoForge item fluid capability here (HbmCapabilities registers {@link #createFluidHandler}):
 * tryFill accepted at most 50 mB per call, the handler caps fill the same way. IBatteryItem / MagazineElectricEngine
 * belong to the ENGINE_ELECTRIC weapon mod, which is not ported.
 */
public class ItemGunDrill extends ItemGunBaseNT implements com.atomiclegacy.items.HbmChargeableItem { // [weapon mods G3: IBatteryItem]

	public ItemGunDrill(Properties properties, WeaponQuality quality, GunConfig... cfg) {
		super(properties, quality, cfg);
	}

	/**
	 * getHarvestLevel(stack, toolClass) = ToolMaterial.IRON.getHarvestLevel() (2, moddable) for every tool class, and
	 * canHarvestBlock = true for blocks without a harvest tool: 1.21 only asks for blocks that require the correct tool,
	 * where the harvest level is the vanilla tier tag.
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		int level = XFactoryDrill.getModdableHarvestLevel(stack, 2);
		if(level <= 0) return !state.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL);
		if(level == 1) return !state.is(BlockTags.INCORRECT_FOR_STONE_TOOL);
		if(level == 2) return !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);
		if(level == 3) return !state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
		return !state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
	}

	private static MagazineLiquidEngine engine(ItemStack stack) {
		IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		return mag instanceof MagazineLiquidEngine engine ? engine : null;
	}

	/** IFillableItem#acceptsFluid */
	public static boolean acceptsFluid(HbmFluidDefinition type, ItemStack stack) {
		MagazineLiquidEngine engine = engine(stack);
		if(engine != null) for(HbmFluidDefinition acc : engine.acceptedTypes) if(type == acc) return true;
		return false;
	}

	/** IFillableItem#getFill */
	public static int getFill(ItemStack stack) {
		MagazineLiquidEngine engine = engine(stack);
		return engine != null ? engine.getAmount(stack, null) : 0;
	}

	/** IFillableItem#getFirstFluidType */
	public static HbmFluidDefinition getFirstFluidType(ItemStack stack) {
		MagazineLiquidEngine engine = engine(stack);
		return engine != null ? engine.getType(stack, null) : HbmFluids.none();
	}

	// [weapon mods G3: IBatteryItem] 1.7.10 IBatteryItem of ItemGunDrill (only acts on the MagazineElectricEngine of the ENGINE_ELECTRIC mod)
	private static com.atomiclegacy.items.weapon.sedna.mags.MagazineElectricEngine electricEngine(ItemStack stack) {
		IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		return mag instanceof com.atomiclegacy.items.weapon.sedna.mags.MagazineElectricEngine engine ? engine : null;
	}

	/** IBatteryItem#getCharge */
	@Override
	public long hbmCharge(ItemStack stack) {
		var engine = electricEngine(stack);
		return engine != null ? engine.getAmount(stack, null) : 0;
	}

	/** IBatteryItem#getMaxCharge */
	@Override
	public long hbmCapacity(ItemStack stack) {
		var engine = electricEngine(stack);
		return engine != null ? engine.getCapacity(stack) : 0;
	}

	/** IBatteryItem#getChargeRate */
	@Override public long hbmChargeRate(ItemStack stack) { return 50_000; }
	/** IBatteryItem#getDischargeRate */
	@Override public long hbmDischargeRate(ItemStack stack) { return 0; }

	/** IBatteryItem#setCharge; chargeBattery / dischargeBattery clamp to [0, capacity] */
	@Override
	public void hbmSetCharge(ItemStack stack, long charge) {
		var engine = electricEngine(stack);
		if(engine != null) engine.setAmount(stack, (int) Math.max(0, Math.min(engine.capacity, charge)));
	}
	// [weapon mods G3: IBatteryItem end]

	public IFluidHandlerItem createFluidHandler(ItemStack stack) {
		return new Handler(stack);
	}

	private static final class Handler implements IFluidHandlerItem {
		private final ItemStack container;

		private Handler(ItemStack container) {
			this.container = container;
		}

		@Override public ItemStack getContainer() { return this.container; }
		@Override public int getTanks() { return 1; }

		@Override
		public FluidStack getFluidInTank(int tank) {
			int fill = getFill(this.container);
			return tank == 0 && fill > 0 ? HbmFluids.toNeoStack(getFirstFluidType(this.container), fill) : FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			MagazineLiquidEngine engine = engine(this.container);
			return tank == 0 && engine != null ? engine.getCapacity(this.container) : 0;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return tank == 0 && !stack.isEmpty() && HbmFluids.fromNeoFluid(stack.getFluid()).map(type -> acceptsFluid(type, this.container)).orElse(false);
		}

		/** IFillableItem#tryFill: toFill = min(amount, 50), capped by the remaining capacity; returns the amount taken. */
		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if(resource.isEmpty() || this.container.getCount() != 1) return 0;
			HbmFluidDefinition type = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(null);
			if(type == null || !acceptsFluid(type, this.container)) return 0;
			MagazineLiquidEngine engine = engine(this.container);
			if(engine == null) return 0;
			int toFill = Math.min(resource.getAmount(), 50);
			toFill = Math.min(toFill, engine.getCapacity(this.container) - getFill(this.container));
			if(toFill <= 0) return 0;
			if(action == IFluidHandler.FluidAction.EXECUTE) engine.setAmount(this.container, getFill(this.container) + toFill);
			return toFill;
		}

		/** IFillableItem#providesFluid = false, tryEmpty returns the request untouched. */
		@Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
		@Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
	}
}

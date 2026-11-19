package com.atomiclegacy.items.weapon.sedna.impl;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.entity.LegacyChemicalEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFluid;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * [weapons batch F1] 1.7.10 ItemGunChemthrower: a gun whose magazine (MagazineFluid) holds 3,000 mB of any fluid; every
 * shot spawns an EntityChemical of that fluid, uses 3 mB and adds 1 wear.
 *
 * <p>IFillableItem (acceptsFluid / tryFill / providesFluid / tryEmpty, transfer speed 50 mB per operation) is exposed to
 * the tanks of this port as an {@link IFluidHandlerItem} ({@link #createFluidHandler}, registered by GunBatchF1Capabilities),
 * so FluidTank.loadTank/unloadTank of 1.7.10 becomes HbmFluidContainerTransfer#fillFromTank / drainIntoTank.
 */
public class ItemGunChemthrower extends ItemGunBaseNT {

	public static final int CONSUMPTION = 3;

	public ItemGunChemthrower(Properties properties, WeaponQuality quality, GunConfig... cfg) {
		super(properties, quality, cfg);
	}

	public boolean acceptsFluid(HbmFluidDefinition type, ItemStack stack) {
		return getFluidType(stack) == type || getMagCount(stack) == 0;
	}

	public static final int transferSpeed = 50;

	/** Fills up to transferSpeed of the given amount and returns the remainder. */
	public int tryFill(HbmFluidDefinition type, int amount, ItemStack stack) {

		if(!acceptsFluid(type, stack)) return amount;
		if(getMagCount(stack) == 0) setMagType(stack, type.oldId());

		int fill = getMagCount(stack);
		int req = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCapacity(stack) - fill;
		int toFill = Math.min(amount, req);
		toFill = Math.min(toFill, transferSpeed);
		setMagCount(stack, fill + toFill);

		return amount - toFill;
	}

	public HbmFluidDefinition getFluidType(ItemStack stack) {
		return HbmFluids.byOldId(getMagType(stack)).orElse(HbmFluids.none());
	}

	public boolean providesFluid(HbmFluidDefinition type, ItemStack stack) {
		return getFluidType(stack) == type;
	}

	/** Drains up to transferSpeed of the given amount and returns what was drained. */
	public int tryEmpty(HbmFluidDefinition type, int amount, ItemStack stack) {
		int fill = getMagCount(stack);
		int toUnload = Math.min(fill, amount);
		toUnload = Math.min(toUnload, transferSpeed);
		setMagCount(stack, fill - toUnload);
		return toUnload;
	}

	public HbmFluidDefinition getFirstFluidType(ItemStack stack) { return getFluidType(stack); }
	public int getFill(ItemStack stack) { return getMagCount(stack); }

	public static int getMagType(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, MagazineFluid.KEY_MAG_TYPE + 0); }
	public static void setMagType(ItemStack stack, int value) { ItemGunBaseNT.setValueInt(stack, MagazineFluid.KEY_MAG_TYPE + 0, value); }
	public static int getMagCount(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0); }
	public static void setMagCount(ItemStack stack, int value) { ItemGunBaseNT.setValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0, value); }

	public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) >= CONSUMPTION; };

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		int index = ctx.configIndex;
		ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE, ctx.configIndex);

		Receiver primary = ctx.config.getReceivers(stack)[0];
		IMagazine<?> mag = primary.getMagazine(stack);

		Vec3 offset = primary.getProjectileOffset(stack);
		double forwardOffset = offset.x;
		double heightOffset = offset.y;
		double sideOffset = offset.z;

		LegacyChemicalEntity chem = new LegacyChemicalEntity(entity.level(), entity, sideOffset, heightOffset, forwardOffset);
		chem.setFluid((HbmFluidDefinition) mag.getType(stack, ctx.inventory));
		entity.level().addFreshEntity(chem);

		mag.useUpAmmo(stack, ctx.inventory, CONSUMPTION);
		ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + 1F, ctx.config.getDurability(stack)));
	};

	public IFluidHandlerItem createFluidHandler(ItemStack stack) {
		return new Handler(stack);
	}

	/** The IFillableItem methods above behind the NeoForge fluid item capability; every call moves at most transferSpeed. */
	private final class Handler implements IFluidHandlerItem {
		private final ItemStack stack;

		private Handler(ItemStack stack) {
			this.stack = stack;
		}

		private int capacity() {
			return getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCapacity(stack);
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			if(tank != 0) return FluidStack.EMPTY;
			HbmFluidDefinition fluid = getFluidType(stack);
			return fluid.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, getMagCount(stack));
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? capacity() : 0;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack resource) {
			return tank == 0 && HbmFluids.fromNeoFluid(resource.getFluid()).filter(fluid -> !fluid.isNone() && acceptsFluid(fluid, stack)).isPresent();
		}

		@Override
		public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
			if(resource.isEmpty() || !isFluidValid(0, resource)) return 0;
			HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
			int toFill = Math.min(Math.min(resource.getAmount(), capacity() - getMagCount(stack)), transferSpeed);
			if(toFill <= 0) return 0;
			if(action.execute()) tryFill(fluid, toFill, stack);
			return toFill;
		}

		@Override
		public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
			if(resource.isEmpty()) return FluidStack.EMPTY;
			HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
			if(fluid.isNone() || !providesFluid(fluid, stack)) return FluidStack.EMPTY;
			return drain(resource.getAmount(), action);
		}

		@Override
		public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
			HbmFluidDefinition fluid = getFluidType(stack);
			if(fluid.isNone()) return FluidStack.EMPTY;
			int toUnload = Math.min(Math.min(getMagCount(stack), maxDrain), transferSpeed);
			if(toUnload <= 0) return FluidStack.EMPTY;
			if(action.execute()) tryEmpty(fluid, toUnload, stack);
			return HbmFluids.toNeoStack(fluid, toUnload);
		}

		@Override
		public ItemStack getContainer() {
			return stack;
		}
	}
}

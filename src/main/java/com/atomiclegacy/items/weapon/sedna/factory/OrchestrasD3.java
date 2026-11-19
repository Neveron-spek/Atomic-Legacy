package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** 1.7.10 Orchestras for weapons batch D3: ORCHESTRA_FLAREGUN, ORCHESTRA_CONGOLAKE and ORCHESTRA_MK108. */
public class OrchestrasD3 {

	/**
	 * Ordinals of TurretVisualClientEffects.CasingConfig G26_FLARE ... G40_PHOS (SpentCasing "g26Flare",
	 * "g26FlareSupply", "g26FlareWeapon", "g40", "g40heat", "g40demo", "g40inc", "g40phos"). Must follow the enum order.
	 */
	/** Ordinal of CasingConfig.G26_FLARE, the first of the eight consecutive D3 entries (shift here on merge). */
	public static final int CASING_D3_FIRST = 32;
	public static final int CASING_G26_FLARE = CASING_D3_FIRST;
	public static final int CASING_G26_FLARE_SUPPLY = CASING_D3_FIRST + 1;
	public static final int CASING_G26_FLARE_WEAPON = CASING_D3_FIRST + 2;
	public static final int CASING_G40 = CASING_D3_FIRST + 3;
	public static final int CASING_G40_HEAT = CASING_D3_FIRST + 4;
	public static final int CASING_G40_DEMO = CASING_D3_FIRST + 5;
	public static final int CASING_G40_INC = CASING_D3_FIRST + 6;
	public static final int CASING_G40_PHOS = CASING_D3_FIRST + 7;

	/** SpentCasing colors of the casing ordinals above (renderers tint the loaded shell with them). */
	public static int casingColor(int casing) {
		return switch(casing) {
			case CASING_G26_FLARE -> 0x9E1616;
			case CASING_G26_FLARE_SUPPLY -> 0x3C80F0;
			case CASING_G26_FLARE_WEAPON -> 0x278400;
			case CASING_G40 -> 0x777777;
			case CASING_G40_HEAT -> 0x5E6854;
			case CASING_G40_DEMO -> 0xE30000;
			case CASING_G40_INC -> 0xE86F20;
			case CASING_G40_PHOS -> 0xC8C8C8;
			default -> 0x515151; // SpentCasing.COLOR_CASE_40MM
		};
	}

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAREGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
			if(timer == 4) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				if(mag.getAmountAfterReload(stack) > 0) {
					int casing = mag.getCasing(stack, ctx.inventory);
					if(casing >= 0) OrchestrasC4.casing(entity, 0.625, -0.125, aiming ? -0.125 : -0.375D, -0.12, 0.18, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 7.5F, (float)entity.getRandom().nextGaussian() * 5F, casing, true, 60, 0.5D, 20);
					mag.setAmountBeforeReload(stack, 0);
				}
			}
			if(timer == 16) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 24) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 10) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
			if(timer == 29) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 12) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
			if(timer == 12) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CONGOLAKE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 15) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				int casing = mag.getCasing(stack, ctx.inventory);
				if(casing >= 0) OrchestrasC4.casing(entity, 0.625, aiming ? -0.0625 : -0.25, aiming ? 0 : -0.375D, 0, 0.18, 0.12, 0.01, -5F + (float)entity.getRandom().nextGaussian() * 3.5F, -10F + entity.getRandom().nextFloat() * 5F, casing, true, 60, 0.5D, 20);
			}
		}
		if(type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_GRENADE_RELOAD, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 9) sound(entity, HbmSoundEvents.GUN_GRENADE_OPEN, 1F, 1F);
			if(timer == 27) sound(entity, HbmSoundEvents.GUN_GRENADE_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MK108 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 2) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) OrchestrasC4.casing(entity, 0.5, aiming ? -0.125 : -0.3125, aiming ? -0.375 : -0.3125D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 2.5F, (float)entity.getRandom().nextGaussian() * -20F + 15F, casing, true, 60, 0.5D, 10);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 0.75F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.65F);
			if(timer == 10) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 0.75F);
			if(timer == 40) sound(entity, HbmSoundEvents.GUN_MAG_REMOVE, 1F, 0.75F);
			if(timer == 60) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.5F, 1F);
			if(timer == 90) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 0.75F);
			if(timer == 100) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 0.75F);
			if(timer == 125) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.65F);

			if(timer == 60) ctx.config.getReceivers(stack)[0].getMagazine(stack).reloadAction(stack, ctx.inventory);
		}

		if(type == GunAnimation.INSPECT) {
			int yeetHorizontal = 750;
			int untilImpact = yeetHorizontal * 9 / 15;
			int delay = 250;

			for(int i = 0; i < 3; i++) {
				if(timer == (untilImpact + delay * i) / 50) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.5F, 1.5F);
			}
		}
	};
}

package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * [weapons batch E1] 1.7.10 Orchestras: ORCHESTRA_PANERSCHRECK (sic), ORCHESTRA_QUADRO, ORCHESTRA_FLIEGERFAUST,
 * ORCHESTRA_MISSILE_LAUNCHER and ORCHESTRA_BOLTER.
 */
public class OrchestrasE1 {

	/** Ordinal of TurretVisualClientEffects.CasingConfig.B75 (SpentCasing "b75", "b75inc", "b75exp"; shift here on merge). */
	public static final int CASING_B75 = 40;

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_PANERSCHRECK = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_QUADRO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLIEGERFAUST = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 20) sound(entity, HbmSoundEvents.GUN_BOLT_OPEN, 1F, 0.85F);
			if(timer == 62) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 64) sound(entity, HbmSoundEvents.GUN_BOLT_CLOSE, 1F, 0.85F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 25) sound(entity, HbmSoundEvents.GUN_BOLT_OPEN, 1F, 0.85F);
			if(timer == 32) sound(entity, HbmSoundEvents.GUN_BOLT_CLOSE, 1F, 0.85F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MISSILE_LAUNCHER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1.25F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_BOLT_OPEN, 1F, 0.9F);
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 42) sound(entity, HbmSoundEvents.GUN_BOLT_CLOSE, 1F, 0.9F);
		}

		if(type == GunAnimation.JAMMED || type == GunAnimation.INSPECT) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_BOLT_OPEN, 1F, 0.9F);
			if(timer == 27) sound(entity, HbmSoundEvents.GUN_BOLT_CLOSE, 1F, 0.9F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_BOLTER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 1) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.5, aiming ? 0 : -0.125, aiming ? -0.0625 : -0.25D, 0, 0.18, -0.12, 0.01,
						-10F + (float) entity.getRandom().nextGaussian() * 5F, 10F + entity.getRandom().nextFloat() * 10F, casing);
			}
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 26) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
		}
	};
}

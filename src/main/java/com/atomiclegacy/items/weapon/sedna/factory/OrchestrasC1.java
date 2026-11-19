package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 Orchestras: ORCHESTRA_ATLAS, ORCHESTRA_DANI and ORCHESTRA_PEPPERBOX (weapons batch C1).
 * Sound timings and pitches are verbatim; none of these guns ejects casings.
 */
public class OrchestrasC1 {

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_ATLAS = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
			if(timer == 44) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.9F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.9F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 24) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 34) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
	};

	/** Identical to ORCHESTRA_ATLAS in the original, runs once per config (index 0 and 1). */
	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DANI = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
			if(timer == 44) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.9F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.9F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 24) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 34) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_PEPPERBOX = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 24) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
			if(timer == 55) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 21) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.6F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.8F);
			if(timer == 11) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.6F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 3) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 28) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.75F);
			if(timer == 45) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.6F);
		}
	};
}

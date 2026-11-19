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

/** [weapons batch E2] 1.7.10 Orchestras: ORCHESTRA_TESLA, ORCHESTRA_LASER_PISTOL and ORCHESTRA_LASRIFLE. */
public class OrchestrasE2 {

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_TESLA = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_SHREDDER_CYCLE, 0.25F, 1.25F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_SHREDDER_CYCLE, 0.25F, 1.25F);
		}
		if(type == GunAnimation.INSPECT) {
			// NTMSounds.BLOCK_PLUSHY (hbm:block.squeakyToy)
			if(timer == 12) sound(entity, HbmSoundEvents.PLUSHIE_SQUEAK, 0.25F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LASER_PISTOL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1.5F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 10) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 1.25F);
			if(timer == 34) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 1.25F);
			if(timer == 40) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 1.25F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 10) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 15) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 1.25F);
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.25F, 1.5F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LASRIFLE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1.5F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 18) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.25F, 1F);
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
			if(timer == 38) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 1F);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 12) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
			if(timer == 20) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 1F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 22) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};
}

package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 1.7.10 Orchestras for weapons batch D1: ORCHESTRA_SPAS, ORCHESTRA_SHREDDER (gun_autoshotgun and
 * gun_autoshotgun_shredder) and ORCHESTRA_SHREDDER_SEXY.
 */
public class OrchestrasD1 {

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SPAS = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE || type == GunAnimation.ALT_CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_SHOTGUN_COCK, 1F, 1F);
			if(timer == 10) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory); //turns out there's a reason why stovepipes look like that
				if(casing >= 0) GunEffects.casing(entity, 0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -3F + (float)entity.getRandom().nextGaussian() * 2.5F, -15F + entity.getRandom().nextFloat() * -5F, casing);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_SHOTGUN_COCK, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD) {
			IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
			if(mag.getAmount(stack, ctx.inventory) == 0) {
				if(timer == 0) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);
				if(timer == 7) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 1F);
			}
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_SHOTGUN_OPEN, 1F, 1F);
			if(timer == 18) sound(entity, HbmSoundEvents.GUN_SHOTGUN_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 18) sound(entity, HbmSoundEvents.GUN_WHACK, 1F, 1F);
			if(timer == 25) sound(entity, HbmSoundEvents.GUN_WHACK, 1F, 1F);
			if(timer == 29) sound(entity, HbmSoundEvents.GUN_SHOTGUN_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SHREDDER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_SHREDDER_CYCLE, 0.25F, 1.5F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_SHREDDER_CYCLE, 0.25F, 1.5F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 32) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 28) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SHREDDER_SEXY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				GunEffects.muzzleFlash(entity);
				if(ctx.config.getReceivers(stack)[0].getMagazine(stack).getType(stack, null) == XFactory12ga.g12_equestrian_bj) {
					ItemGunBaseNT.setTimer(stack, 0, 20);
				}
			}

			if(timer == 2) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) OrchestrasC4.casing(entity, 0.375, aiming ? -0.0625 : -0.125, aiming ? -0.125 : -0.25D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 2.5F, (float)entity.getRandom().nextGaussian() * -20F + 15F, casing, false, 60, 0.5D, 20);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 4) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 16) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 55) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.5F, 1F);
			if(timer == 65) sound(entity, HbmSoundEvents.GUN_MAG_INSERT, 1F, 1F);
			if(timer == 74) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 88) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 100) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 1F);

			if(timer == 55) ctx.config.getReceivers(stack)[0].getMagazine(stack).reloadAction(stack, ctx.inventory);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 20) sound(entity, HbmSoundEvents.PLAYER_GULP, 1F, 1F);
			if(timer == 25) sound(entity, HbmSoundEvents.PLAYER_GULP, 1F, 1F);
			if(timer == 30) sound(entity, HbmSoundEvents.PLAYER_GULP, 1F, 1F);
			if(timer == 35) sound(entity, HbmSoundEvents.PLAYER_GULP, 1F, 1F);
			if(timer == 50) sound(entity, HbmSoundEvents.PLAYER_GROAN, 1F, 1F);
			if(timer == 60) {
				entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 2));
				entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
				entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
			}
		}
	};
}

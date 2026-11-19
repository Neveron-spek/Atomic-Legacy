package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunStinger;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** [weapons batch F4] 1.7.10 Orchestras.ORCHESTRA_STINGER, ORCHESTRA_FOLLY and ORCHESTRA_ABERRATOR. */
public final class OrchestrasF4 {

	private OrchestrasF4() {
	}

	/** Ordinal of TurretVisualClientEffects.CasingConfig.P35_800 (SpentCasing "35-800"; shift here on merge). */
	public static final int CASING_P35_800 = 41;

	/**
	 * Client hook of the GUN_LOCKON AudioWrapper loop (MainRegistry.proxy.getLoopedSound(GUN_LOCKON, pos, 1F, 15F, 1F, 10)):
	 * set by GunBatchF4ClientEffects; true keeps the loop alive for the entity, false stops it.
	 */
	public static BiConsumer<LivingEntity, Boolean> LOCKON_LOOP = (entity, active) -> { };

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STINGER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(entity.level().isClientSide) {
			// start / keep alive the loop while locking on, stop it once locked on or idle (timeout)
			LOCKON_LOOP.accept(entity, ItemGunStinger.getLockonProgress(stack) > 0 && !ItemGunStinger.getIsLockedOn(stack));
			return;
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FOLLY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 20) sound(entity, HbmSoundEvents.GUN_SCREW, 1F, 1F);
			if(timer == 80) sound(entity, HbmSoundEvents.GUN_ROCKET_INSERT, 1F, 1F);
			if(timer == 120) sound(entity, HbmSoundEvents.GUN_SCREW, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_ABERRATOR = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE, 1F, 0.75F);
			if(timer == 32) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 0.75F);
			if(timer == 42) sound(entity, HbmSoundEvents.GUN_PISTOL_COCK, 1F, 0.75F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 1) {
				int cba = (stack.getItem() == HbmItems.GUN_ABERRATOR_EOTT.get() && ctx.configIndex == 0) ? -1 : 1;
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.5, aiming ? 0 : -0.125, aiming ? -0.0625 : -0.25D * cba, -0.05, 0.25, -0.05 * cba, 0.01,
						-10F + (float) entity.getRandom().nextGaussian() * 10F, (float) entity.getRandom().nextGaussian() * 12.5F, casing);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 1) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 9) sound(entity, HbmSoundEvents.GUN_PISTOL_COCK, 1F, 0.75F);
		}
	};
}

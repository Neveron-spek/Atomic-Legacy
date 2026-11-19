package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 Orchestras, limited to the ported guns. */
public class Orchestras {

	/** [parity d2] gun_debug. Sound timings and casing offsets are verbatim (CasingCreator default pitch / yaw multipliers 5F / 10F, no smoke). */
	public static BiConsumer<ItemStack, LambdaContext> DEBUG_ORCHESTRA = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 3) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 1F);
			if(timer == 10) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 34) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
			if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);

			if(timer == 16) {
				com.atomiclegacy.items.weapon.sedna.mags.IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				int casing = mag.getCasing(stack, ctx.inventory);
				if(casing >= 0) for(int i = 0; i < mag.getCapacity(stack); i++) GunEffects.casing(entity, 0.25, -0.125, -0.125, -0.05, 0, 0, 0.01, 5F, 10F, casing);
			}
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 11) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 11) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 3) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_GREASEGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LATCH_OPEN.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 2) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.55, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -7.5F + (float)entity.getRandom().nextGaussian() * 5F, 12F + (float)entity.getRandom().nextGaussian() * 5F, casing);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.8F);
			if(timer == 11) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 24) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1.25F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 11) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LAG = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 1) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.375, aiming ? 0 : -0.0625, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 5F, 10F + entity.getRandom().nextFloat() * 10F, casing);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_IMPACT.get(), 0.5F, 1.6F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_UZI = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LATCH_OPEN.get(), 1F, 1.25F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 1) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -2.5F + (float)entity.getRandom().nextGaussian() * 5F, 10F + (float)entity.getRandom().nextFloat() * 15F, casing);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 4) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 17) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
			if(timer == 31) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_UZI_AKIMBO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.EQUIP) {
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LATCH_OPEN.get(), 1F, 1.25F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 1) {
				int mult = ctx.configIndex == 0 ? -1 : 1;
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.375, -0.125, -0.375D * mult, 0, 0.18, -0.12 * mult, 0.01, -2.5F + (float)entity.getRandom().nextGaussian() * 5F, (10F + (float)entity.getRandom().nextFloat() * 15F) * mult, casing);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 4) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 17) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
			if(timer == 31) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_G3 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean scoped = stack.getItem() == com.atomiclegacy.registry.HbmItems.GUN_G3_ZEBRA.get() || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack) && !scoped;

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.5, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 12.5F + (float)entity.getRandom().nextFloat() * 5F, casing);
				GunEffects.muzzleFlash(entity);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.8F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 4) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
			if(timer == 32) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 28) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
			if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
			if(timer == 30) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STG77 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(HbmClientConfig.gunAnimsLegacy()) {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					GunEffects.muzzleFlash(entity);
					int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing >= 0) GunEffects.casing(entity, aiming ? 0.125 : 0.125, aiming ? -0.125 : -0.25, aiming ? -0.125 : -0.25D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 7.5F + entity.getRandom().nextFloat() * 5F, casing);
				}
				if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 0.25F, 1.25F);
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.8F);
				if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);
				if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 0.25F, 1.25F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
				if(timer == 10) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
				if(timer == 24) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
				if(timer == 34) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
				if(timer == 10) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);

				if(timer == 114) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
				if(timer == 124) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			}
		} else {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					GunEffects.muzzleFlash(entity);
					int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing >= 0) GunEffects.casing(entity, aiming ? 0.125 : 0.25, aiming ? -0.125 : -0.25, aiming ? -0.125 : -0.25D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 7.5F + entity.getRandom().nextFloat() * 5F, casing);
				}
				if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 0.25F, 1.25F);
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.8F);
				if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);
				if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 0.25F, 1.25F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
				if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
				if(timer == 32) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_IMPACT.get(), 0.25F, 1.25F);
				if(timer == 38) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
				if(timer == 43) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
				if(timer == 11) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);

				if(timer == 72) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
				if(timer == 84) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			}
		}
	};
}

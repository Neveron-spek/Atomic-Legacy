package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.network.TurretCasingEffectPayload;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 Orchestras: ORCHESTRA_CARBINE, ORCHESTRA_MINIGUN and ORCHESTRA_MAS36 (weapons batch C3).
 * Sound timings, casing offsets, motions and casing smoke are verbatim.
 */
public class OrchestrasC3 {

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CARBINE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 1) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) casing(entity, 0.3125, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.21, -0.06, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 2.5F, 2.5F + (float)entity.getRandom().nextGaussian() * 2F, casing, true, 60, 0.5D, 20);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
			if(timer == 31) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 6) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 30) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 0.9F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MINIGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				GunEffects.muzzleFlash(entity);
				int rounds = XWeaponModManager.hasUpgrade(stack, ctx.configIndex, XFactory762mm.ID_MINIGUN_SPEED) ? 3 : 1;
				for(int i = 0; i < rounds; i++) {
					int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing >= 0) GunEffects.casing(entity, aiming ? 0.125 : 0.5, aiming ? -0.125 : -0.25, aiming ? -0.25 : -0.5D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 15F, (float)entity.getRandom().nextGaussian() * 15F, casing);
				}
			}
			if(timer == (XWeaponModManager.hasUpgrade(stack, 0, XFactory762mm.ID_MINIGUN_SLOWDOWN) ? 3 : 1)) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.75F);
			if(timer == 1) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
	};

	/** [parity d2] gun_minigun_dual: config 0 is the left gun (casings mirrored to the other side). */
	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MINIGUN_DUAL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				GunEffects.muzzleFlash(entity);
				int index = ctx.configIndex == 0 ? -1 : 1;
				int rounds = XWeaponModManager.hasUpgrade(stack, ctx.configIndex, XFactory762mm.ID_MINIGUN_SPEED) ? 3 : 1;
				for(int i = 0; i < rounds; i++) {
					int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing >= 0) GunEffects.casing(entity, 0.25, -0.25, -0.5D * index, 0, 0.18, -0.12 * index, 0.01, (float)entity.getRandom().nextGaussian() * 15F, (float)entity.getRandom().nextGaussian() * 15F, casing);
				}
			}
			if(timer == (XWeaponModManager.hasUpgrade(stack, 0, XFactory762mm.ID_MINIGUN_SLOWDOWN) ? 3 : 1)) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.75F);
			if(timer == 1) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_SPIN.get(), 1F, 0.75F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MAS36 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack) && !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);

		if(type == GunAnimation.EQUIP) {
			if(timer == 10) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LATCH_OPEN.get(), 1F, 1F);
			if(timer == 18) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 7) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
			if(timer == 12) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) casing(entity,
						0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D,
						-0.05, 0.2, -0.025,
						0.01, -10F + (float) entity.getRandom().nextGaussian() * 10F, (float) entity.getRandom().nextGaussian() * 12.5F, casing, true, 60, 0.5D, 10);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.75F);
			if(timer == 7) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 1F, 1F);
			if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_RIFLE_COCK.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 1F, 1F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 23) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 17) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
		}
	};

	/**
	 * CasingCreator#composeEffect(world, player, front, height, side, frontMotion, heightMotion, sideMotion,
	 * motionVariance, mPitch, mYaw, casing, smoking, smokeLife, smokeLift, nodeLife); sent within 50 blocks.
	 * Same as {@link GunEffects#casing} plus the casing smoke trail.
	 */
	public static void casing(LivingEntity player, double frontOffset, double heightOffset, double sideOffset,
			double frontMotion, double heightMotion, double sideMotion, double motionVariance,
			float mPitch, float mYaw, int casing, boolean smoking, int smokeLife, double smokeLift, int nodeLife) {
		if(!(player.level() instanceof ServerLevel level)) return;

		if(player.isShiftKeyDown()) heightOffset -= 0.075F;

		float pitch = player.getXRot();
		float yaw = player.getYRot();
		Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset)
				.xRot(-pitch / 180F * (float) Math.PI)
				.yRot(-yaw / 180F * (float) Math.PI);

		double x = player.getX() + offset.x;
		double y = player.getEyeY() + offset.y;
		double z = player.getZ() + offset.z;

		Vec3 motion = new Vec3(sideMotion, heightMotion, frontMotion)
				.xRot(-pitch / 180F * (float) Math.PI)
				.yRot(-yaw / 180F * (float) Math.PI);

		Vec3 velocity = player.getDeltaMovement();
		double mX = velocity.x + motion.x + player.getRandom().nextGaussian() * motionVariance;
		double mY = velocity.y + motion.y + player.getRandom().nextGaussian() * motionVariance;
		double mZ = velocity.z + motion.z + player.getRandom().nextGaussian() * motionVariance;

		if(player instanceof Player p && p.getAbilities().flying) mY -= 0.04D;

		TurretCasingEffectPayload payload = new TurretCasingEffectPayload(x, y, z, mX, mY, mZ,
				pitch, yaw, mPitch, mYaw, casing, smoking, smokeLife, smokeLift, nodeLife);
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, 50, payload);
	}
}

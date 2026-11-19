package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
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
 * 1.7.10 Orchestras: ORCHESTRA_AM180, ORCHESTRA_STAR_F, ORCHESTRA_STAR_F_AKIMBO,
 * ORCHESTRA_AMAT and ORCHESTRA_M2 (weapons batch B; the shared Orchestras class
 * holds the other guns). Sound timings, casing offsets and motions are verbatim.
 */
public class OrchestrasB {

	/** Casing effect ids (ordinals of TurretVisualClientEffects.CasingConfig). */
	public static final int CASING_BMG50 = 1;
	public static final int CASING_BMG50_STEEL = 9;
	public static final int CASING_P22 = 11;
	public static final int CASING_P22_STEEL = 12;

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_AM180 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		// ClientConfig.GUN_ANIMS_LEGACY is read on the logical server as well, exactly like the original
		if(HbmClientConfig.gunAnimsLegacy()) {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					GunEffects.muzzleFlash(entity);
					int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing >= 0) GunEffects.casing(entity, 0.4375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, -0.06, 0, 0.01, (float)entity.getRandom().nextGaussian() * 10F, (float)entity.getRandom().nextGaussian() * 10F, casing);
				}
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
				if(timer == 6) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
				if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_IMPACT.get(), 0.25F, 1F);
				if(timer == 32) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
				if(timer == 40) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);
			}
			if(type == GunAnimation.JAMMED) {
				if(timer == 15) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
				if(timer == 35) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			}
		} else {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					GunEffects.muzzleFlash(entity);
					int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing >= 0) GunEffects.casing(entity, 0.4375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, -0.06, 0, 0.01, (float)entity.getRandom().nextGaussian() * 10F, (float)entity.getRandom().nextGaussian() * 10F, casing);
				}
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
				if(timer == 6) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 6) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
				if(timer == 26) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_IMPACT.get(), 0.25F, 1F);
				if(timer == 48) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
				if(timer == 54) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.9F);
			}
			if(type == GunAnimation.JAMMED) {
				if(timer == 6) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 0.8F);
				if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1.0F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 6) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
				if(timer == 53) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STAR_F = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.3125, aiming ? 0 : -0.125, aiming ? 0 : -0.1875D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 12.5F + (float)entity.getRandom().nextFloat() * 5F, casing);
				GunEffects.muzzleFlash(entity);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.9F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1.1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 22) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 30) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 15) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 19) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
			if(timer == 23) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 27) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 7) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 30) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STAR_F_AKIMBO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				int side = ctx.configIndex == 0 ? -1 : 1;
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.3125, aiming ? 0 : -0.125, aiming ? 0 : -0.1875D * side, 0, 0.18, -0.12 * side, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 12.5F + (float)entity.getRandom().nextFloat() * 5F, casing);
				GunEffects.muzzleFlash(entity);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 0.9F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_PISTOL_COCK.get(), 1F, 1.1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 22) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 30) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 15) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 19) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
			if(timer == 23) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 27) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 7) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 30) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1.1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_AMAT = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 10) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 0.5F, 1.25F);
			if(timer == 15) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 0.5F, 1.25F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 7) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
			if(timer == 12) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) smokingCasing(entity,
						0.375, aiming ? 0 : -0.125, -0.25D,
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
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_REMOVE.get(), 1F, 1F);
			if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_INSERT.get(), 1F, 1F);
			if(timer == 32) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 41) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_OPEN.get(), 0.5F, 1F);
			if(timer == 23) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_BOLT_CLOSE.get(), 0.5F, 1F);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 0.5F, 1F);
			if(timer == 45) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 0.5F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_M2 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.TURRET_HOWARD_RELOAD.get(), 1F, 1F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				GunEffects.muzzleFlash(entity);
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) GunEffects.casing(entity, 0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.3125D, 0, 0.06, -0.18, 0.01, (float)entity.getRandom().nextGaussian() * 20F, 12.5F + (float)entity.getRandom().nextGaussian() * 7.5F, casing);
			}
		}
	};

	/**
	 * CasingCreator#composeEffect(world, player, ..., mPitch, mYaw, casing, smoking, smokeLife, smokeLift, nodeLife),
	 * the smoking variant used by ORCHESTRA_AMAT; same transform as {@link GunEffects#casing}.
	 */
	public static void smokingCasing(LivingEntity player, double frontOffset, double heightOffset, double sideOffset,
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

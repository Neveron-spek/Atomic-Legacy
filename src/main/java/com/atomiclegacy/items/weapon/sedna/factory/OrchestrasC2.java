package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.network.TurretCasingEffectPayload;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 Orchestras: ORCHESTRA_HENRY, ORCHESTRA_NOPIP and ORCHESTRA_HANGMAN (weapons batch C2,
 * XFactory44). Sound timings, casing offsets and motions are verbatim.
 */
public class OrchestrasC2 {

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_HENRY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 8) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 1F);
			if(timer == 16) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT.get(), 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.9F);
			if(timer == 12 && ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmountBeforeReload(stack) <= 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LEVER_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.9F);
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LEVER_COCK.get(), 1F, 1F);
			if(timer == 36) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LEVER_COCK.get(), 1F, 1F);
			if(timer == 44) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LEVER_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 14) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) casingWithSmoke(entity, 0.5, -0.125, aiming ? -0.125 : -0.375D, 0, 0.12, -0.12, 0.01, -7.5F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 1.5F, casing, true, 60, 0.5D, 20);
			}
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LEVER_COCK.get(), 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
			if(timer == 12) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_LEVER_COCK.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_NOPIP = (stack, ctx) -> {
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
				Receiver rec = ctx.config.getReceivers(stack)[0];
				IMagazine<?> mag = rec.getMagazine(stack);
				int casing = mag.getCasing(stack, ctx.inventory);
				if(casing >= 0) for(int i = 0; i < mag.getCapacity(stack); i++) GunEffects.casing(entity, 0.25, -0.125, -0.125, -0.05, 0, 0, 0.01, -6.5F + (float)entity.getRandom().nextGaussian() * 3F, (float)entity.getRandom().nextGaussian() * 5F, casing);
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

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_HANGMAN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_DRY_FIRE.get(), 1F, 1F);
		}

		if(type == GunAnimation.RELOAD) {

			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
			if(timer == 5) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.8F);
			if(timer == 25) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 35) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.75F);

			if(timer == 10) {
				Receiver rec = ctx.config.getReceivers(stack)[0];
				IMagazine<?> mag = rec.getMagazine(stack);
				int casing = mag.getCasing(stack, ctx.inventory);
				if(casing >= 0) for(int i = 0; i < mag.getCapacity(stack); i++) GunEffects.casing(entity, 0.25, -0.25, -0.125, -0.05, 0, 0, 0.01, -6.5F + (float)entity.getRandom().nextGaussian() * 3F, (float)entity.getRandom().nextGaussian() * 5F, casing);
			}
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 16 && ctx.getPlayer() != null) {
				HitResult mop = getMouseOver(ctx.getPlayer(), 3.0D);
				if(mop != null) {
					if(mop.getType() == HitResult.Type.ENTITY) {
						Entity hit = ((EntityHitResult) mop).getEntity();
						float damage = HbmConfig.hangmanSmackDamage();
						hit.hurt(entity.damageSources().playerAttack(ctx.getPlayer()), damage);
						Vec3 motion = hit.getDeltaMovement();
						hit.setDeltaMovement(motion.x * 2, motion.y, motion.z * 2);
						hit.hurtMarked = true;
						entity.level().playSound(null, hit.getX(), hit.getY(), hit.getZ(), HbmSoundEvents.GUN_SMACK.get(), hit.getSoundSource(), 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
					}
					if(mop.getType() == HitResult.Type.BLOCK) {
						BlockHitResult blockHit = (BlockHitResult) mop;
						BlockState b = entity.level().getBlockState(blockHit.getBlockPos());
						Vec3 hitVec = mop.getLocation();
						entity.level().playSound(null, hitVec.x, hitVec.y, hitVec.z, b.getSoundType(entity.level(), blockHit.getBlockPos(), entity).getStepSound(), SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
					}
				}
			}
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 10) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.8F);
			if(timer == 15) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_MAG_SMALL_REMOVE.get(), 1F, 0.8F);
			if(timer == 20) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE.get(), 1F, 1F);
			if(timer == 25) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 0.75F);
		}
	};

	/**
	 * CasingCreator#composeEffect(world, player, front, height, side, frontMotion, heightMotion, sideMotion,
	 * motionVariance, mPitch, mYaw, casing, smoking, smokeLife, smokeLift, nodeLife); sent within 50 blocks.
	 */
	public static void casingWithSmoke(LivingEntity player, double frontOffset, double heightOffset, double sideOffset,
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

	/**
	 * EntityDamageUtil#getMouseOver(attacker, reach): block ray trace from the eye, then the closest pickable
	 * living entity along the look vector (search box grown by 1, hitbox grown by the 1.7.10 collision border 0.1).
	 */
	public static HitResult getMouseOver(Player attacker, double reach) {
		return getMouseOver(attacker, reach, 0D);
	}

	/** [parity c1] EntityDamageUtil#getMouseOver(attacker, reach, threshold): the hitboxes are grown by border + threshold. */
	public static HitResult getMouseOver(Player attacker, double reach, double threshold) {
		Vec3 pos = attacker.getEyePosition(1F);
		Vec3 look = attacker.getViewVector(1F);
		Vec3 end = pos.add(look.x * reach, look.y * reach, look.z * reach);
		HitResult objectMouseOver = attacker.level().clip(new ClipContext(pos, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, attacker));

		Entity pointedEntity = null;
		Vec3 hitvec = null;
		float grace = 1.0F;
		List<Entity> list = attacker.level().getEntities(attacker, attacker.getBoundingBox().expandTowards(look.x * reach, look.y * reach, look.z * reach).inflate(grace, grace, grace));

		double closest = reach;

		for(Entity entity : list) {
			if(entity.isPickable() && entity.isAlive()) {
				double borderSize = 0.1F + threshold; // Entity#getCollisionBorderSize of 1.7.10
				AABB axisalignedbb = entity.getBoundingBox().inflate(borderSize, borderSize, borderSize);
				java.util.Optional<Vec3> intercept = axisalignedbb.clip(pos, end);

				if(axisalignedbb.contains(pos)) {
					if(0.0D <= closest) {
						pointedEntity = entity;
						hitvec = intercept.orElse(pos);
						closest = 0.0D;
					}
				} else if(intercept.isPresent()) {
					double dist = pos.distanceTo(intercept.get());

					if(dist < closest || closest == 0.0D) {
						if(entity == attacker.getVehicle() && !entity.canRiderInteract()) {
							if(closest == 0.0D) {
								pointedEntity = entity;
								hitvec = intercept.get();
							}
						} else {
							pointedEntity = entity;
							hitvec = intercept.get();
							closest = dist;
						}
					}
				}
			}
		}

		// the 1.7.10 ray trace returns a MISS result (never null), so an entity only wins when closer than the reach
		if(pointedEntity != null && closest < reach) {
			objectMouseOver = new EntityHitResult(pointedEntity, hitvec);
		}

		return objectMouseOver;
	}
}

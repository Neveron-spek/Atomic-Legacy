package com.atomiclegacy.items.weapon.sedna.factory;

import com.atomiclegacy.network.GunMuzzleFlashPayload;
import com.atomiclegacy.network.TurretCasingEffectPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side senders for 1.7.10 CasingCreator, MuzzleFlashPacket and the gun sound calls. */
public final class GunEffects {

	private GunEffects() {
	}

	/** Casing effect ids, ordinals of TurretVisualClientEffects.CasingConfig. */
	public static final int CASING_P9 = 2;
	public static final int CASING_P9_STEEL = 10;

	/**
	 * CasingCreator#composeEffect(world, player, front, height, side, frontMotion, heightMotion,
	 * sideMotion, motionVariance, mPitch, mYaw, casing) without smoke; sent within 50 blocks.
	 */
	public static void casing(LivingEntity player, double frontOffset, double heightOffset, double sideOffset,
			double frontMotion, double heightMotion, double sideMotion, double motionVariance,
			float mPitch, float mYaw, int casing) {
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
				pitch, yaw, mPitch, mYaw, casing, false, 0, 0.0D, 0);
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, 50, payload);
	}

	/** MuzzleFlashPacket sent to all around within 100 blocks. */
	public static void muzzleFlash(LivingEntity entity) {
		if(!(entity.level() instanceof ServerLevel level)) return;
		PacketDistributor.sendToPlayersNear(level, null, entity.getX(), entity.getY(), entity.getZ(), 100, new GunMuzzleFlashPayload(entity.getId()));
	}

	/** World#playSoundEffect at the entity position. */
	public static void playSound(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), volume, pitch);
	}

	/** World#playSoundAtEntity. */
	public static void playSoundAtEntity(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), volume, pitch);
	}
}

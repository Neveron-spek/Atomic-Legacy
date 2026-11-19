package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
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
 * 1.7.10 Orchestras for weapons batch C4: ORCHESTRA_DOUBLE_BARREL, ORCHESTRA_MARESLEG,
 * ORCHESTRA_MARESLEG_SHORT, ORCHESTRA_MARESLEG_AKIMBO and ORCHESTRA_LIBERATOR.
 */
public class OrchestrasC4 {

	/**
	 * Ordinals of TurretVisualClientEffects.CasingConfig G10 ... G12_EQUESTRIAN (SpentCasing
	 * "10GA", "10GAShrapnel", "10GADU", "10GASlug", "10GAEXP", "12GA_BP" (also BP_MAGNUM/BP_SLUG,
	 * identical), "12GA", "12GA_SLUG", "12GA_FLECHETTE", "12GA_MAGNUM", "12GA_EXPLOSIVE",
	 * "12GA_PHOSPHORUS", "12gaEquestrianTKR"). Must follow the enum order.
	 */
	public static final int CASING_G10 = 16;
	public static final int CASING_G10_SHRAPNEL = 17;
	public static final int CASING_G10_DU = 18;
	public static final int CASING_G10_SLUG = 19;
	public static final int CASING_G10_EXP = 20;
	public static final int CASING_G12_BP = 21;
	public static final int CASING_G12 = 22;
	public static final int CASING_G12_SLUG = 23;
	public static final int CASING_G12_FLECHETTE = 24;
	public static final int CASING_G12_MAGNUM = 25;
	public static final int CASING_G12_EXPLOSIVE = 26;
	public static final int CASING_G12_PHOSPHORUS = 27;
	public static final int CASING_G12_EQUESTRIAN = 28;

	/**
	 * CasingCreator#composeEffect(world, player, front, height, side, frontMotion, heightMotion, sideMotion,
	 * motionVariance, mPitch, mYaw, casing, smoking, smokeLife, smokeLift, nodeLife), sent within 50 blocks.
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

	private static void sound(LivingEntity entity, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DOUBLE_BARREL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 19) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 0.9F);
			if(timer == 29) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.8F);

			if(timer == 12) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
				int casing = mag.getCasing(stack, ctx.inventory);
				if(casing >= 0) for(int i = 0; i < toEject; i++) casing(entity, 0, -0.1875, -0.375D, -0.24, 0.18, 0, 0.01, -20F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing, true, 60, 0.5D, 20);
			}
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 19) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 16) sound(entity, HbmSoundEvents.GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.7F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.7F);
			if(timer == 17) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
			if(timer == 29) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 14) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) casing(entity, 0.3125, -0.125, aiming ? -0.125 : -0.375D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing, true, 60, 0.5D, 20);
			}
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG_SHORT = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 16) sound(entity, HbmSoundEvents.GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.7F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.7F);
			if(timer == 17) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
			if(timer == 29) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 14) {
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) casing(entity, 0.3125, -0.125, aiming ? -0.125 : -0.375D, 0, -0.08, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing, true, 60, 0.5D, 20);
			}
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG_AKIMBO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
			if(timer == 14) {
				int offset = ctx.configIndex == 0 ? -1 : 1;
				int casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing >= 0) casing(entity, 0.3125, -0.125, aiming ? -0.125 * offset : -0.375D * offset, 0, -0.08, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing, true, 60, 0.5D, 20);
			}
			if(timer == 8) sound(entity, HbmSoundEvents.GUN_LEVER_COCK, 1F, 0.8F);
			return;
		}

		ORCHESTRA_MARESLEG_SHORT.accept(stack, ctx);
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LIBERATOR = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 4) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
				int casing = mag.getCasing(stack, ctx.inventory);
				if(casing >= 0) for(int i = 0; i < toEject; i++) casing(entity, 0.625, -0.1875, -0.375D, -0.12, 0.18, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 7.5F, (float)entity.getRandom().nextGaussian() * 5F, casing, true, 60, 0.5D, 20);
			}
			if(timer == 15) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 5) sound(entity, HbmSoundEvents.GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.9F);
			if(timer == 12) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 26) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_REVOLVER_COCK, 1F, 0.75F);
			IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
			int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
			if(timer == 4 && toEject > 0) {
				int casing = mag.getCasing(stack, ctx.inventory);
				// the original multiplies (not adds) the -15 pitch base with the gaussian here
				if(casing >= 0) for(int i = 0; i < toEject; i++) casing(entity, 0.625, -0.1875, -0.375D, -0.12, 0.18, 0, 0.01, -15F * (float)entity.getRandom().nextGaussian() * 7.5F, (float)entity.getRandom().nextGaussian() * 5F, casing, true, 60, 0.5D, 20);
				mag.setAmountAfterReload(stack, 0);
			}
			if(timer == 20) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
	};
}

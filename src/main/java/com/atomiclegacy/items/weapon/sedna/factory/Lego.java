package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.SmokeNode;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmStats;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 com.hbm.items.weapon.sedna.factory.Lego, limited to the prefabs used
 * by the ported guns (standard fire/reload/aim, wear, spread, smoke).
 */
public class Lego {

	public static final Random ANIM_RAND = new Random();

	/** If IDLE and the mag of receiver 0 can be loaded, set state to RELOADING. Used by keybinds. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_RELOAD = (stack, ctx) -> {

		Player player = ctx.getPlayer();
		Receiver rec = ctx.config.getReceivers(stack)[0];
		GunState state = ItemGunBaseNT.getState(stack, ctx.configIndex);

		if(state == GunState.IDLE) {

			ItemGunBaseNT.setIsAiming(stack, false);
			IMagazine<?> mag = rec.getMagazine(stack);

			if(mag.canReload(stack, ctx.inventory)) {
				int loaded = mag.getAmount(stack, ctx.inventory);
				mag.setAmountBeforeReload(stack, loaded);
				ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.RELOADING);
				ItemGunBaseNT.setTimer(stack, ctx.configIndex, rec.getReloadBeginDuration(stack) + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
				ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD, ctx.configIndex);
				if(ctx.config.getReloadChangesType(stack)) mag.initNewType(stack, ctx.inventory);
			} else {
				ItemGunBaseNT.playAnimation(player, stack, GunAnimation.INSPECT, ctx.configIndex);
				if(!ctx.config.getInspectCancel(stack)) {
					ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.DRAWING);
					ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
				}
			}
		}
	};

	/** If IDLE and ammo is loaded, fire and set to JUST_FIRED. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_PRIMARY = (stack, ctx) -> { clickReceiver(stack, ctx, 0); };

	public static void clickReceiver(ItemStack stack, LambdaContext ctx, int receiver) {

		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		Receiver rec = ctx.config.getReceivers(stack)[receiver];
		int index = ctx.configIndex;
		GunState state = ItemGunBaseNT.getState(stack, index);

		if(state == GunState.IDLE) {

			if(rec.getCanFire(stack).apply(stack, ctx)) {
				rec.getOnFire(stack).accept(stack, ctx);

				if(rec.getFireSound(stack) != null)
					GunEffects.playSound(entity, rec.getFireSound(stack).get(), rec.getFireVolume(stack), rec.getFirePitch(stack));

				int remaining = rec.getRoundsPerCycle(stack) - 1;
				for(int i = 0; i < remaining; i++) if(rec.getCanFire(stack).apply(stack, ctx)) rec.getOnFire(stack).accept(stack, ctx);

				ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
				ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
			} else {

				if(rec.getDoesDryFire(stack)) {
					ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
					ItemGunBaseNT.setState(stack, index, rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
					ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
				}
			}
		}

		if(state == GunState.RELOADING) {
			ItemGunBaseNT.setReloadCancel(stack, true);
		}
	}

	/** Toggles the fire mode (mode 0/1) when IDLE. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_SECONDARY = (stack, ctx) -> {

		LivingEntity entity = ctx.entity;
		int index = ctx.configIndex;
		GunState state = ItemGunBaseNT.getState(stack, index);

		if(state == GunState.IDLE) {
			int mode = ItemGunBaseNT.getMode(stack, 0);
			ItemGunBaseNT.setMode(stack, index, 1 - mode);
			if(mode == 0)
				GunEffects.playSound(entity, com.atomiclegacy.registry.HbmSoundEvents.GUN_SWITCHMODE1.get(), 1F, 1F);
			else
				GunEffects.playSound(entity, com.atomiclegacy.registry.HbmSoundEvents.GUN_SWITCHMODE2.get(), 1F, 1F);
		}
	};

	/** Default smoke. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_SMOKE = (stack, ctx) -> {
		handleStandardSmoke(ctx.entity, stack, 2000, 0.025D, 1.15D, ctx.configIndex);
	};

	public static void handleStandardSmoke(LivingEntity entity, ItemStack stack, int smokeDuration, double alphaDecay, double widthGrowth, int index) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		long lastShot = gun.lastShot[index];
		List<SmokeNode> smokeNodes = gun.getConfig(stack, index).smokeNodes;

		boolean smoking = lastShot + smokeDuration > System.currentTimeMillis();
		if(!smoking && !smokeNodes.isEmpty()) smokeNodes.clear();

		if(smoking) {
			Vec3 velocity = entity.getDeltaMovement();
			Vec3 prev = new Vec3(-velocity.x, -velocity.y, -velocity.z).yRot((float) (entity.getYRot() * Math.PI / 180D));
			double accel = 15D;
			double side = (entity.getYRot() - entity.yHeadRotO) * 0.1D;
			double waggle = 0.025D;

			for(SmokeNode node : smokeNodes) {
				node.forward += -prev.z * accel + entity.level().random.nextGaussian() * waggle;
				node.lift += prev.y + 1.5D;
				node.side += prev.x * accel + entity.level().random.nextGaussian() * waggle + side;
				if(node.alpha > 0) node.alpha -= alphaDecay;
				node.width *= widthGrowth;
			}

			double alpha = (System.currentTimeMillis() - lastShot) / (double) smokeDuration;
			alpha = (1 - alpha) * 0.5D;

			if(ItemGunBaseNT.getState(stack, index) == GunState.RELOADING || smokeNodes.size() == 0) alpha = 0;
			smokeNodes.add(new SmokeNode(alpha));
		}
	}

	/** Toggles isAiming. Used by keybinds. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM = (stack, ctx) -> { ItemGunBaseNT.setIsAiming(stack, !ItemGunBaseNT.getIsAiming(stack)); };

	/** Returns true if the mag has ammo in it. Used by keybind functions on whether to fire, and deciders on whether to trigger a refire. */
	public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_STANDARD_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0; };

	// [weapons batch F4: lockon]
	/** Returns true if the mag has ammo in it, and the gun is in the locked on state */
	public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_LOCKON_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0 && ItemGunBaseNT.getIsLockedOn(stack); };
	/** Spawns an EntityBulletBaseMK4 with the loaded bulletcfg, then resets lockon progress */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_LOCKON_FIRE = (stack, ctx) -> {
		doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
		ItemGunBaseNT.setIsLockedOn(stack, false);
	};
	/** Spawns a bullet with the loaded bulletcfg */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_FIRE = (stack, ctx) -> {
		doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
	};
	/** [parity d2] Returns true if the mag of receiver 1 has ammo in it (gun_debug). */
	public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_SECOND_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[1].getMagazine(stack).getAmount(stack, ctx.inventory) > 0; };
	/** [parity d2] LAMBDA_STANDARD_FIRE for receiver 1 (gun_debug). */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_FIRE = (stack, ctx) -> {
		doStandardFire(stack, ctx, GunAnimation.CYCLE, 1, true);
	};
	/** [parity d2] anims for the DEBUG revolver, mostly a copy of the li'lpip but with some fixes regarding the cylinder movement */
	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, com.atomiclegacy.render.anim.BusAnimation> LAMBDA_DEBUG_ANIMS = (stack, type) -> {
		switch(type) {
		case CYCLE: return new com.atomiclegacy.render.anim.BusAnimation()
				.addBus("RECOIL", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
				.addBus("HAMMER", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
				.addBus("DRUM", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
		case CYCLE_DRY: return new com.atomiclegacy.render.anim.BusAnimation()
				.addBus("HAMMER", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300 + 100).addPos(0, 0, 0, 200))
				.addBus("DRUM", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
		case EQUIP: return new com.atomiclegacy.render.anim.BusAnimation().addBus("ROTATE", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(-360, 0, 0, 350));
		case RELOAD: return new com.atomiclegacy.render.anim.BusAnimation()
					.addBus("RELAOD_TILT", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
					.addBus("RELOAD_CYLINDER", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
					.addBus("RELOAD_LIFT", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250).addPos(-45, 0, 0, 350).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050).addPos(0, 0, 0, 100))
					.addBus("RELOAD_JOLT", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50).addPos(0, 0, 0, 100))
					.addBus("RELOAD_BULLETS", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300).addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
					.addBus("RELOAD_BULLETS_CON", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950).addPos(0, 0, 0, 1));
		case INSPECT:
		case JAMMED: return new com.atomiclegacy.render.anim.BusAnimation()
					.addBus("RELAOD_TILT", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
					.addBus("RELOAD_CYLINDER", new com.atomiclegacy.render.anim.BusAnimationSequence().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
		}

		return null;
	};
	/** [parity c1] Spawns an EntityBulletBaseMK4 with the loaded bulletcfg, ignores wear */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE = (stack, ctx) -> {
		doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
	};

	public static void doStandardFire(ItemStack stack, LambdaContext ctx, GunAnimation anim, int receiver, boolean calcWear) {
		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		int index = ctx.configIndex;
		if(anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);

		boolean aim = ItemGunBaseNT.getIsAiming(stack);
		Receiver primary = ctx.config.getReceivers(stack)[receiver];
		IMagazine<?> mag = primary.getMagazine(stack);
		BulletConfig config = (BulletConfig) mag.getType(stack, ctx.inventory);

		Vec3 offset = ItemGunBaseNT.getIsAiming(stack) ? primary.getProjectileOffsetScoped(stack) : primary.getProjectileOffset(stack);
		double forwardOffset = offset.x;
		double heightOffset = offset.y;
		double sideOffset = offset.z;

		int projectiles = config.projectilesMin();
		if(config.projectilesMax() > config.projectilesMin()) projectiles += entity.getRandom().nextInt(config.projectilesMax() - config.projectilesMin() + 1);
		projectiles = (int) (projectiles * primary.getSplitProjectiles(stack));

		for(int i = 0; i < projectiles; i++) {
			float damage = calcDamage(ctx, stack, primary, calcWear, index);
			float spread = calcSpread(ctx, stack, primary, config, calcWear, index, aim);
			// [weapons batch C3] ProjectileType.BEAM -> EntityBulletBeamBase
			if(config instanceof com.atomiclegacy.items.weapon.sedna.BeamBulletConfig beam) {
				entity.level().addFreshEntity(new com.atomiclegacy.entity.LegacyBulletBeamEntity(entity, beam, damage, spread, sideOffset, heightOffset, forwardOffset));
				continue;
			}
			LegacyBulletEntity mk4 = spawnBullet(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
			if(ItemGunBaseNT.getIsLockedOn(stack)) mk4.setLockonTarget(entity.level().getEntity(ItemGunBaseNT.getLockonTarget(stack))); // [weapons batch E1]
			if(i == 0 && config.ammo.blackPowder()) com.atomiclegacy.particle.helper.BlackPowderCreator.composeEffect(entity.level(), mk4.getX(), mk4.getY(), mk4.getZ(), mk4.getDeltaMovement().x, mk4.getDeltaMovement().y, mk4.getDeltaMovement().z, 10, 0.25F, 0.5F, 10, 0.25F);
		}

		if(player != null) player.awardStat(HbmStats.NTM_BULLETS.get());
		mag.useUpAmmo(stack, ctx.inventory, 1);
		if(calcWear) ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear, ctx.config.getDurability(stack)));
	}

	/**
	 * EntityBulletBaseMK4(entity, config, baseDamage, gunSpread, side, height, front): starts at the eye
	 * position plus the rotated offset and travels along the view direction.
	 */
	public static LegacyBulletEntity spawnBullet(LivingEntity entity, BulletConfig config, float baseDamage, float gunSpread, double sideOffset, double heightOffset, double frontOffset) {
		float yaw = entity.getYRot();
		float pitch = entity.getXRot();
		Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset)
				.xRot(-pitch / 180F * (float) Math.PI)
				.yRot(-yaw / 180F * (float) Math.PI);
		double x = entity.getX() + offset.x;
		double y = entity.getEyeY() + offset.y;
		double z = entity.getZ() + offset.z;
		Vec3 direction = new Vec3(
				-Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI),
				-Mth.sin(pitch / 180.0F * (float) Math.PI),
				Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI));
		LegacyBulletEntity bullet = new LegacyBulletEntity(entity.level(), x, y, z, direction, config.ammo, baseDamage, entity, gunSpread);
		entity.level().addFreshEntity(bullet);
		return bullet;
	}

	public static float getStandardWearSpread(ItemStack stack, GunConfig config, int index) {
		float percent = (float) ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
		if(percent < 0.5F) return 0F;
		return (percent - 0.5F) * 2F;
	}

	/** Returns the standard multiplier for damage based on wear */
	public static float getStandardWearDamage(ItemStack stack, GunConfig config, int index) {
		float percent = (float) ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
		if(percent < 0.75F) return 1F;
		return 1F - (percent - 0.75F) * 2F;
	}

	/** Returns the full calculated damage based on guncfg and wear */
	public static float calcDamage(LambdaContext ctx, ItemStack stack, Receiver primary, boolean calcWear, int index) {
		return primary.getBaseDamage(stack) * (calcWear ? getStandardWearDamage(stack, ctx.config, index) : 1);
	}

	public static float calcSpread(LambdaContext ctx, ItemStack stack, Receiver primary, BulletConfig config, boolean calcWear, int index, boolean aim) {
		// the gun's innate spread, SMGs will have poor accuracy no matter what
		float spreadInnate = primary.getInnateSpread(stack);
		// the ammo's spread (for example for buckshot) multiplied with the gun's ammo modifier (choke or sawed off barrel)
		float spreadAmmo = config.spread() * primary.getAmmoSpread(stack);
		// hipfire penalty, i.e. extra spread when not aiming
		float spreadHipfire = aim ? 0F : primary.getHipfireSpread(stack);
		// extra spread caused by weapon durability, [0;0.125] by default
		float spreadWear = !calcWear ? 0F : (getStandardWearSpread(stack, ctx.config, index) * primary.getDurabilitySpread(stack));

		return spreadInnate + spreadAmmo + spreadHipfire + spreadWear;
	}
}

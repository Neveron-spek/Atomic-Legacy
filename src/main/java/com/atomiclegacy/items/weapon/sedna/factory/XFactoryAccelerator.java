package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyCoinEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunNI4NI;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineInfinite;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F2] 1.7.10 XFactoryAccelerator: gun_tau (XVL1456 tau cannon), gun_coilgun and gun_n_i_4_n_i with the
 * beams tau_uranium / tau_uranium_charge / ni4ni_arc and the coil slugs coil_tungsten / coil_ferrouranium.
 *
 * <p>setCasing(plate_lead x2, 16) of the tau beams feeds the casing bag through LegacyCasingTable [parity b1].
 * setDamageFalloffByPen(false) of the beams and slugs is the StandardAmmoType damageFalloffByPenetration flag
 * (TAU_URANIUM, COIL_*: penetrates, no falloff). The beam renderers are LegoClient.RENDER_TAU, RENDER_TAU_CHARGE
 * and RENDER_NI4NI_BOLT; the slugs use RENDER_AP_BULLET.
 */
public class XFactoryAccelerator {

	public static final MagazineBelt tauChargeMag = new MagazineBelt();

	public static final BeamBulletConfig tau_uranium = new BeamBulletConfig(StandardAmmoType.TAU_URANIUM).setupDamageClass(DamageClass.SUBATOMIC).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
			.setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setBeamRenderer(BeamBulletConfig.RENDER_TAU);
	public static final BeamBulletConfig tau_uranium_charge = new BeamBulletConfig(StandardAmmoType.TAU_URANIUM).setupDamageClass(DamageClass.SUBATOMIC).setLife(5).setRenderRotations(false).setDoesPenetrate(true).setSpectral(true)
			.setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setBeamRenderer(BeamBulletConfig.RENDER_TAU_CHARGE);

	public static final BulletConfig coil_tungsten = new BulletConfig(StandardAmmoType.COIL_TUNGSTEN);
	public static final BulletConfig coil_ferrouranium = new BulletConfig(StandardAmmoType.COIL_FERROURANIUM);

	/** No ammo item in 1.7.10 (new BulletConfig() without setItem); StandardAmmoType.NONE keeps damageMult 1, knockback 0.1. */
	public static final BeamBulletConfig ni4ni_arc = new BeamBulletConfig(StandardAmmoType.NONE).setupDamageClass(DamageClass.PHYSICAL).setLife(5).setThresholdNegation(10F).setArmorPiercing(0.2F).setRenderRotations(false).setDoesPenetrate(false)
			.setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setBeamRenderer(BeamBulletConfig.RENDER_NI4NI_BOLT);

	static {
		// coil_tungsten / coil_ferrouranium: setVel(7.5F).setLife(50).setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true)
		StandardAmmoType.COIL_TUNGSTEN.setVel(7.5D).setLife(50).setSpectral(true).setOnUpdate(bullet -> breakInPath(bullet, 1.25F));
		StandardAmmoType.COIL_FERROURANIUM.setVel(7.5D).setLife(50).setSpectral(true).setOnUpdate(bullet -> breakInPath(bullet, 2.5F));
		tauChargeMag.addConfigs(tau_uranium_charge);
	}

	/**
	 * XFactoryAccelerator.breakInPath: samples the tick's displacement every 0.5 blocks backwards from the slug and
	 * breaks every non-air block with 0 <= hardness < threshold without drops. The original ran on both sides and
	 * spawned a "fireworks" (EntityFireworkSparkFX, no motion) particle per sample on the client; the port's bullets
	 * are not simulated on the client, so the server sends the same vanilla FIREWORK particles to players within
	 * 100 blocks instead.
	 */
	public static void breakInPath(LegacyBulletEntity entity, float threshold) {
		if(!(entity.level() instanceof ServerLevel level)) return;

		Vec3 vec = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
		double motion = Math.max(vec.length(), 0.1);
		vec = vec.normalize();

		for(double d = 0; d < motion; d += 0.5) {

			double dX = entity.getX() - vec.x * d;
			double dY = entity.getY() - vec.y * d;
			double dZ = entity.getZ() - vec.z * d;

			for(ServerPlayer player : level.players()) {
				if(player.distanceToSqr(dX, dY, dZ) <= 100D * 100D) {
					level.sendParticles(player, ParticleTypes.FIREWORK, true, dX, dY, dZ, 0, 0D, 0D, 0D, 0D);
				}
			}

			BlockPos pos = new BlockPos(Mth.floor(dX), Mth.floor(dY), Mth.floor(dZ));
			BlockState b = level.getBlockState(pos);
			float hardness = b.getDestroySpeed(level, pos);
			if(!b.isAir() && hardness >= 0 && hardness < threshold) {
				level.destroyBlock(pos, false);
			}
		}
	}

	/** Lego.LAMBDA_NOWEAR_FIRE: doStandardFire without wear (gun_n_i_4_n_i has 0 durability). */
	public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE = (stack, ctx) -> {
		Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
	};

	public static ItemGunBaseNT createTau(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(6_400).draw(10).inspect(10).crosshair(Crosshair.CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::tauCannonDamage).spreadHipfire(0F).delay(4).auto(true).spread(0F)
						.mag(new MagazineBelt().addConfigs(tau_uranium))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_TAU))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
				.rp(LAMBDA_TAU_PRIMARY_RELEASE)
				.ps(LAMBDA_TAU_SECONDARY_PRESS)
				.rs(LAMBDA_TAU_SECONDARY_RELEASE)
				.pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_TAU_ANIMS).orchestra(OrchestrasF2.ORCHESTRA_TAU)
				).setDefaultAmmo(StandardAmmoType.TAU_URANIUM, 15);
	}

	public static ItemGunBaseNT createCoilgun(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SPECIAL, new GunConfig()
				.dura(400).draw(5).inspect(39).crosshair(Crosshair.L_CIRCUMFLEX)
				.rec(new Receiver(0)
						.dmg(HbmConfig::coilgunDamage).delay(5).reload(20).jam(33).sound(HbmSoundEvents.GUN_COIL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(coil_tungsten, coil_ferrouranium))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_COILGUN))
				.setupStandardConfiguration()
				.anim(LAMBDA_COILGUN_ANIMS).orchestra(OrchestrasF2.ORCHESTRA_COILGUN)
				).setDefaultAmmo(StandardAmmoType.COIL_TUNGSTEN, 5);
	}

	public static ItemGunBaseNT createNI4NI(Item.Properties properties) {
		return new ItemGunNI4NI(properties, WeaponQuality.SPECIAL, new GunConfig()
				.dura(0).draw(5).inspect(39).crosshair(Crosshair.CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::ni4niDamage).delay(10).sound(HbmSoundEvents.GUN_COIL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineInfinite(ni4ni_arc))
						.offset(0.75, -0.0625, -0.1875D)
						.setupStandardFire().fire(LAMBDA_NOWEAR_FIRE))
				.setupStandardConfiguration()
				.ps(LAMBDA_NI4NI_SECONDARY_PRESS)
				.anim(LAMBDA_NI4NI_ANIMS).orchestra(OrchestrasF2.ORCHESTRA_COILGUN)
				);
	}

	/** Tau stop-fire spark sound when the primary is released after a CYCLE. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_PRIMARY_RELEASE = (stack, ctx) -> {
		if(ctx.getPlayer() == null || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) != GunAnimation.CYCLE) return;
		Player player = ctx.getPlayer();
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.GUN_TAU_STOPFIRE.get(), player.getSoundSource(), 1F, 1F);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_SECONDARY_PRESS = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		if(ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) <= 0) return;
		ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.SPINUP, ctx.configIndex);
		MagazineBelt.getMagType(stack); //caches the last loaded ammo
	};

	/**
	 * Charge release: after at least 10 ticks of SPINUP one tau_uranium_charge beam with
	 * wearDamage * units * GUN_TAU_CANNON_CHARGE_DAMAGE, units = 1 + min(12, timer / 10), and config.wear * units wear.
	 */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TAU_SECONDARY_RELEASE = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(timer >= 10 && ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimation.SPINUP) {
			ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.ALT_CYCLE, ctx.configIndex);
			int unitsUsed = 1 + Math.min(12, timer / 10);

			LivingEntity entity = ctx.entity;
			int index = ctx.configIndex;

			Receiver primary = ctx.config.getReceivers(stack)[0];
			BulletConfig config = tauChargeMag.getFirstConfig(stack, ctx.inventory);

			Vec3 offset = primary.getProjectileOffset(stack);
			double forwardOffset = offset.x;
			double heightOffset = offset.y;
			double sideOffset = offset.z;

			float damage = Lego.getStandardWearDamage(stack, ctx.config, index) * unitsUsed * HbmConfig.tauCannonChargeDamage();
			float spread = Lego.calcSpread(ctx, stack, primary, config, true, index, false);
			LegacyBulletBeamEntity mk4 = new LegacyBulletBeamEntity(entity, (BeamBulletConfig) config, damage, spread, sideOffset, heightOffset, forwardOffset);
			entity.level().addFreshEntity(mk4);

			ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear * unitsUsed, ctx.config.getDurability(stack)));

		} else {
			ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex);
		}
	};

	/** Throws one coin (look vector * 0.8, +0.5 upwards) from the eye position; random.orb pitch 1 + rand * 0.25. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NI4NI_SECONDARY_PRESS = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		Player player = ctx.getPlayer();

		if(ItemGunNI4NI.getCoinCount(stack) > 0) {
			Vec3 vec = player.getLookAngle().scale(0.8D);
			LegacyCoinEntity coin = new LegacyCoinEntity(player.level());
			// EntityCoin.setPosition(posX, posY + eyeHeight - yOffset(0.5) - 0.125, posZ) with the box center at posY: bottom = eye - 1.125
			coin.setPos(player.getX(), player.getEyeY() - 0.5D - 0.125D - 0.5D, player.getZ());
			coin.setDeltaMovement(vec.x, vec.y + 0.5D, vec.z);
			coin.setYRot(player.getYRot());
			coin.setThrower(player);
			player.level().addFreshEntity(coin);

			GunEffects.playSoundAtEntity(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1F + player.getRandom().nextFloat() * 0.25F);

			ItemGunNI4NI.setCoinCount(stack, ItemGunNI4NI.getCoinCount(stack) - 1);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_TAU = (stack, ctx) -> { };

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_COILGUN = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TAU_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -5, 50, IType.SIN_DOWN).addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
		case ALT_CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -3, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -5, 50, IType.SIN_DOWN).addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
		case CYCLE_DRY: return new BusAnimation();
		case INSPECT: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(2, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, -360 * 3, 500 * 3, IType.SIN_DOWN));
		case SPINUP: return new BusAnimation()
				.addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 360 * 6, 3000, IType.SIN_UP).addPos(0, 0, 0, 0).addPos(0, 0, 360 * 40, 500 * 20));
		}

		return null;
	};

	public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_COILGUN_ANIMS = (stack, type) -> {
		if(type == GunAnimation.EQUIP) return new BusAnimation().addBus("RELOAD", new BusAnimationSequence().addPos(1, 0, 0, 0).addPos(0, 0, 0, 250));
		if(type == GunAnimation.CYCLE) return new BusAnimation().addBus("RECOIL", new BusAnimationSequence().addPos(ItemGunBaseNT.getIsAiming(stack) ? 0.5 : 1, 0, 0, 100).addPos(0, 0, 0, 200));
		if(type == GunAnimation.RELOAD) return new BusAnimation().addBus("RELOAD", new BusAnimationSequence().addPos(1, 0, 0, 250).addPos(1, 0, 0, 500).addPos(0, 0, 0, 250));
		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_NI4NI_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-360 * 2, 0, 0, 500));
		case CYCLE:
			boolean aiming = ItemGunBaseNT.getIsAiming(stack);
			return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(aiming ? -5 : -30, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("DRUM", new BusAnimationSequence().hold(50).addPos(0, 0, 120, 300, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-360 * 3, 0, 0, 750).hold(100).addPos(0, 0, 0, 750));
		}

		return null;
	};
}

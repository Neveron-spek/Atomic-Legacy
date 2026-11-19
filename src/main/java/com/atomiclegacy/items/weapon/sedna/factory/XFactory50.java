package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBuildingEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.network.GunTinyExplosionPayload;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 XFactory50: gun_amat, gun_amat_subtlety, gun_amat_penance (weapons batch D2, with the
 * ammo_secret rounds bmg50_equestrian and bmg50_black) and gun_m2. Projectile values of the
 * .50 BMG rounds are carried by the port's StandardAmmoType entries.
 */
public class XFactory50 {

	public static final ResourceLocation scope = AtomicLegacy.id("textures/misc/scope_amat.png");
	public static final ResourceLocation scope_thermal = AtomicLegacy.id("textures/misc/scope_penance.png");

	/** Ordinal of TurretVisualClientEffects.CasingConfig.BMG50_EQUESTRIAN (casing50, COLOR_CASE_EQUESTRIAN). */
	public static final int CASING_BMG50_EQUESTRIAN = 31;

	/** LAMBDA_BUILDING: an EntityBuilding 50 blocks above the hit vector; the bullet dies. */
	public static final BulletImpact LAMBDA_BUILDING = (bullet, mop) -> {
		Vec3 hit = mop.getLocation();
		LegacyBuildingEntity silver = new LegacyBuildingEntity(bullet.level(), new Vec3(hit.x, hit.y + 50, hit.z));
		bullet.level().addFreshEntity(silver);
		return true;
	};

	static {
		StandardAmmoType.BMG50_BLACK.setSpectral(true);
		StandardAmmoType.BMG50_EQUESTRIAN.setOnImpact(LAMBDA_BUILDING);
	}

	public static final BulletConfig bmg50_sp = new BulletConfig(StandardAmmoType.BMG50_SP).setCasingEffect(OrchestrasB.CASING_BMG50);
	public static final BulletConfig bmg50_fmj = new BulletConfig(StandardAmmoType.BMG50_FMJ).setCasingEffect(OrchestrasB.CASING_BMG50);
	public static final BulletConfig bmg50_jhp = new BulletConfig(StandardAmmoType.BMG50_JHP).setCasingEffect(OrchestrasB.CASING_BMG50);
	public static final BulletConfig bmg50_ap = new BulletConfig(StandardAmmoType.BMG50_AP).setCasingEffect(OrchestrasB.CASING_BMG50_STEEL);
	public static final BulletConfig bmg50_du = new BulletConfig(StandardAmmoType.BMG50_DU).setCasingEffect(OrchestrasB.CASING_BMG50_STEEL);
	public static final BulletConfig bmg50_he = new BulletConfig(StandardAmmoType.BMG50_HE).setWear(3F).setCasingEffect(OrchestrasB.CASING_BMG50_STEEL);
	public static final BulletConfig bmg50_sm = new BulletConfig(StandardAmmoType.BMG50_SM).setWear(10F).setCasingEffect(OrchestrasB.CASING_BMG50_STEEL);
	/** BulletConfig.setItem(EnumAmmoSecret.BMG50_BLACK): matches ammo_secret:bmg50_black. */
	public static final BulletConfig bmg50_black = new XFactory44.SecretBulletConfig(StandardAmmoType.BMG50_BLACK, "bmg50_black").setWear(5F).setCasingEffect(CASING_BMG50_EQUESTRIAN);
	/** BulletConfig.setItem(EnumAmmoSecret.BMG50_EQUESTRIAN): matches ammo_secret:bmg50_equestrian. */
	public static final BulletConfig bmg50_equestrian = new XFactory44.SecretBulletConfig(StandardAmmoType.BMG50_EQUESTRIAN, "bmg50_equestrian").setCasingEffect(CASING_BMG50_EQUESTRIAN);

	public static ItemGunBaseNT createAmat(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(350).draw(20).inspect(50).crosshair(Crosshair.CIRCLE).scopeTexture(scope).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::antiMaterielDamage).delay(25).dry(25).spreadHipfire(0.05F).reload(51).jam(43).sound(HbmSoundEvents.GUN_AMAT_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 7).addConfigs(bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm, bmg50_he))
						.offset(1, -0.0625 * 1.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_AMAT))
				.setupStandardConfiguration()
				.anim(LAMBDA_AMAT_ANIMS).orchestra(OrchestrasB.ORCHESTRA_AMAT)
				).setDefaultAmmo(StandardAmmoType.BMG50_SP, 7);
	}

	public static ItemGunBaseNT createAmatSubtlety(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(1_000).draw(20).inspect(50).crosshair(Crosshair.CIRCLE).scopeTexture(scope).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::subtletyDamage).delay(25).dry(25).spreadHipfire(0.05F).reload(51).jam(43).sound(HbmSoundEvents.GUN_AMAT_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 7).addConfigs(bmg50_equestrian, bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm, bmg50_he))
						.offset(1, -0.0625 * 1.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_AMAT))
				.setupStandardConfiguration()
				.anim(LAMBDA_AMAT_ANIMS).orchestra(OrchestrasB.ORCHESTRA_AMAT)
				).setDefaultAmmo(StandardAmmoType.BMG50_JHP, 7);
	}

	public static ItemGunBaseNT createAmatPenance(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(5_000).draw(20).inspect(50).crosshair(Crosshair.CIRCLE).scopeTexture(scope_thermal).thermalSights(true).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::penanceDamage).delay(25).dry(25).spreadHipfire(0F).reload(51).jam(43).sound(HbmSoundEvents.GUN_AMAT_SILENCER, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 7).addConfigs(bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm, bmg50_he, bmg50_black))
						.offset(1, -0.0625 * 1.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_AMAT))
				.setupStandardConfiguration()
				.anim(LAMBDA_AMAT_ANIMS).orchestra(OrchestrasB.ORCHESTRA_AMAT)
				).setDefaultAmmo(StandardAmmoType.BMG50_JHP, 7);
	}

	public static ItemGunBaseNT createM2(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(3_000).draw(10).inspect(31).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::m2Damage).delay(2).dry(10).auto(true).spread(0.005F).sound(HbmSoundEvents.TURRET_CHEKHOV_FIRE, 1.0F, 1.0F)
						.mag(new MagazineBelt().addConfigs(bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_he))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_M2))
				.setupStandardConfiguration()
				.anim(LAMBDA_M2_ANIMS).orchestra(OrchestrasB.ORCHESTRA_M2)
				).setDefaultAmmo(StandardAmmoType.BMG50_FMJ, 25);
	}

	/**
	 * LAMBDA_STANDARD_EXPLODE (bmg50_he onImpact): ignored for the shooter during the
	 * first 3 ticks, otherwise Lego.tinyExplode(bullet, mop, 2F) and the bullet dies.
	 * Returns false when the standard hit handling has to continue.
	 */
	public static boolean standardExplode(LegacyBulletEntity bullet, Entity entityHit, Vec3 hitVec, Direction sideHit, float damage) {
		if(entityHit != null && bullet.tickCount < 3 && entityHit == bullet.ownerEntity()) return false;
		tinyExplode(bullet, hitVec, sideHit, damage, 2F, 1F);
		return true;
	}

	/**
	 * Lego.tinyExplode: ExplosionVNT(range) at the hit vector moved 0.25 along the hit side
	 * (an entity hit has sideHit 0 = DOWN in 1.7.10), with no block allocator/processor,
	 * EntityProcessorCrossSmooth(0.5, damage * damageMod) using the round's piercing values and
	 * knockback 0.25, PlayerProcessorStandard and ExplosionEffectTiny.
	 */
	public static void tinyExplode(LegacyBulletEntity bullet, Vec3 hitVec, Direction sideHit, float damage, float range, float damageMod) {
		if(!(bullet.level() instanceof ServerLevel level)) return;
		Direction dir = sideHit == null ? Direction.DOWN : sideHit;
		double x = hitVec.x + dir.getStepX() * 0.25D;
		double y = hitVec.y + dir.getStepY() * 0.25D;
		double z = hitVec.z + dir.getStepZ() * 0.25D;
		Entity exploder = bullet.ownerEntity();
		StandardAmmoType ammo = bullet.ammoType();

		/// EntityProcessorCross(0.5) + EntityProcessorCrossSmooth ///
		float fixedDamage = damage * damageMod;
		double nodeDist = 0.5D;
		double knockbackMult = 0.25D;
		float size = range * 2.0F;
		AABB area = new AABB(x - size - 1.0D, y - size - 1.0D, z - size - 1.0D, x + size + 1.0D, y + size + 1.0D, z + size + 1.0D);
		// setAllowSelfDamage: the exploder is not excluded
		List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
		Explosion compat = new Explosion(level, exploder, x, y, z, range, false, Explosion.BlockInteraction.KEEP);
		EventHooks.onExplosionDetonate(level, compat, list, size);

		Vec3 center = new Vec3(x, y, z);
		Vec3[] nodes = new Vec3[7];
		Direction[] dirs = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		for(int i = 0; i < 6; i++) nodes[i] = new Vec3(x + dirs[i].getStepX() * nodeDist, y + dirs[i].getStepY() * nodeDist, z + dirs[i].getStepZ() * nodeDist);
		nodes[6] = center; // ForgeDirection.UNKNOWN

		Map<Entity, Float> damageMap = new LinkedHashMap<>();
		HashMap<ServerPlayer, Vec3> affectedPlayers = new HashMap<>();

		for(Entity entity : list) {
			AABB box = entity.getBoundingBox();
			double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
			double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
			double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
			double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
			double distanceScaled = dist / size;

			if(distanceScaled <= 1.0D) {
				double deltaX = entity.getX() - x;
				double deltaY = entity.getEyeY() - y;
				double deltaZ = entity.getZ() - z;
				double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

				if(distance != 0.0D) {
					deltaX /= distance;
					deltaY /= distance;
					deltaZ /= distance;

					double density = 0;
					for(Vec3 vec : nodes) {
						double d = Explosion.getSeenPercent(vec, entity);
						if(d > density) density = d;
					}

					double knockback = (1.0D - distanceScaled) * density;
					float dmg = density < 0.125 ? 0F : (float) (fixedDamage * (1 - distanceScaled));
					if(!damageMap.containsKey(entity) || damageMap.get(entity) < dmg) damageMap.put(entity, dmg);
					// EnchantmentProtection.func_92092_a: blast protection, i.e. EXPLOSION_KNOCKBACK_RESISTANCE in 1.21
					double enchKnockback = entity instanceof LivingEntity living ? knockback * (1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) : knockback;

					if(!(entity instanceof LegacyBulletEntity) && !(entity instanceof Player)) {
						entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * enchKnockback * knockbackMult, deltaY * enchKnockback * knockbackMult, deltaZ * enchKnockback * knockbackMult));
						entity.hurtMarked = true;
					}

					if(entity instanceof ServerPlayer player) {
						affectedPlayers.put(player, new Vec3(deltaX * knockback * knockbackMult, deltaY * knockback * knockbackMult, deltaZ * knockback * knockbackMult));
					}
				}
			}
		}

		for(Map.Entry<Entity, Float> entry : damageMap.entrySet()) {
			Entity entity = entry.getKey();
			float amount = entry.getValue();
			if(!entity.isAlive()) continue;
			if(exploder == entity) amount *= 0.5F;
			// BulletConfig.getDamage(null, exploder, EXPLOSIVE)
			DamageSource dmg = level.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, null, exploder instanceof LivingEntity ? exploder : null);
			if(entity instanceof LivingEntity living) {
				amount = LegacyProjectileUtil.sednaExplosiveDamage(living, amount, ammo.armorThresholdNegation(), ammo.armorPiercingPercent());
				// attackEntityFromNT(..., knockback 0F, ...): no vanilla knockback
				hurtWithoutKnockback(living, dmg, amount);
			} else {
				entity.hurt(dmg, amount);
			}
		}

		/// PlayerProcessorStandard: ExplosionKnockbackPacket adds the motion on the client ///
		for(Map.Entry<ServerPlayer, Vec3> entry : affectedPlayers.entrySet()) {
			ServerPlayer player = entry.getKey();
			player.setDeltaMovement(player.getDeltaMovement().add(entry.getValue()));
			player.hurtMarked = true;
		}

		/// ExplosionEffectTiny ///
		level.playSound(null, x, y, z, HbmSoundEvents.WEAPON_EXPLOSION_TINY.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, 100, new GunTinyExplosionPayload(x, y, z, 1.5F, 1));
	}

	/** EntityDamageUtil.attackEntityFromNT with ignoreIFrame and knockbackMultiplier 0: velocity is kept. */
	public static void hurtWithoutKnockback(LivingEntity living, DamageSource source, float amount) {
		Vec3 motion = living.getDeltaMovement();
		living.invulnerableTime = 0;
		living.hurt(source, amount);
		living.setDeltaMovement(motion);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
		Lego.handleStandardSmoke(ctx.entity, stack, 2000, 0.05D, 1.1D, 0);
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AMAT = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(12.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_M2 = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_AMAT_ANIMS = (stack, type) -> {
		double turn = -60;
		double pullAmount = -2.5;
		double side = 4;
		double down = -2;
		double detach = 0.5;
		double apex = 7;

		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("BIPOD", new BusAnimationSequence().hold(500).addPos(80, 0, 0, 350).addPos(80, 25, 0, 150));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
				.addBus("LIFT", new BusAnimationSequence().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
				.addBus("LIFT", new BusAnimationSequence().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case RELOAD: return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence().addPos(0, -10, 0, 350, IType.SIN_UP).addPos(0, 0, 0, 650, IType.SIN_UP))
				.addBus("LIFT", new BusAnimationSequence().hold(1000).addPos(-2, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL).hold(450).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(1500).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(1600).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().hold(250).addPos(-15, 0, 0, 500, IType.SIN_FULL).holdUntil(1650).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("BOLT_TURN", new BusAnimationSequence().hold(250).addPos(0, 0, turn, 150).holdUntil(1250).addPos(0, 0, 0, 150))
				.addBus("BOLT_PULL", new BusAnimationSequence().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR));
		case INSPECT: return new BusAnimation()
				.addBus("SCOPE_THROW", new BusAnimationSequence().addPos(0, detach, 0, 100, IType.SIN_FULL).addPos(side, down, 0, 500, IType.SIN_FULL).addPos(side, down - 0.5, 0, 100).addPos(side, apex, 0, 350, IType.SIN_FULL).addPos(side, down - 0.5, 0, 350, IType.SIN_DOWN).addPos(side, down, 0, 100).hold(250).addPos(0, detach, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SCOPE_SPIN", new BusAnimationSequence().hold(700).addPos(-360, 0, 0, 700));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_M2_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(80, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.25, 25).addPos(0, 0, 0, 75));
		}

		return null;
	};
}

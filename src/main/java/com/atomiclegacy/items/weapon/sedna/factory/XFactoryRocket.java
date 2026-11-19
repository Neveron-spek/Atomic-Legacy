package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.BulletUpdate;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm.CloneBulletConfig;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunStinger; // [weapons batch F4: stinger]
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch E1] 1.7.10 XFactoryRocket: rocket_template (HE, HEAT, DEMO, INC, PHOSPHORUS), the per-gun clones
 * rocket_rpzb / rocket_qd (life 400, steering) / rocket_ml, rocket_ff, gun_panzerschreck, gun_quadro, gun_fliegerfaust
 * and gun_missile_launcher. [weapons batch F4] gun_stinger (ItemGunStinger, SPECIAL: setupLockonFire on rocket_rpzb, the
 * secondary key holds the lock-on). [parity c1] rocket_ncrpa / rocket_ncrpa_steer are ported (ncrpaConfigs). Previously: the rocket_ncrpa configs of the NCR power
 * armor.
 *
 * <p>Rocket flight follows EntityBulletBaseMK4: the stored motion vector (LegacyBulletEntity#legacyMotion) is moved by
 * motion * (velocity + accel) every tick, the lock-on homing runs after the move and before config.onUpdate. The
 * port's per-tick delta movement is therefore recomputed at the end of every onUpdate lambda.
 */
public class XFactoryRocket {

	/// 1.7.10 float-precision vector math (Vec3#lengthVector / normalize use MathHelper.sqrt_double) ///
	private static double len(double x, double y, double z) { return (double) (float) Math.sqrt(x * x + y * y + z * z); }
	private static double len(Vec3 v) { return len(v.x, v.y, v.z); }
	/** Vec3#normalize: below 1.0E-4 the zero vector. */
	private static Vec3 normalize(Vec3 v) {
		double d = len(v);
		return d < 1.0E-4D ? Vec3.ZERO : new Vec3(v.x / d, v.y / d, v.z / d);
	}

	/**
	 * EntityBulletBaseMK4#onUpdate after the move: lock-on homing (Vec3NT interp towards the target center with
	 * turn = min(0.005 * ticksExisted, 1), normalizeSelf, times the old speed), then config.onUpdate, then the
	 * displacement of the next tick (motionMult = velocity + accel).
	 */
	private static BulletUpdate rocketUpdate(Consumer<LegacyBulletEntity> onUpdate) {
		return bullet -> {
			if(bullet.legacyMotion == null) {
				// reloaded from NBT: the saved motion is kept (normalized here, the scale is lost), accel restarts at 0
				Vec3 d = bullet.getDeltaMovement();
				bullet.legacyMotion = d.lengthSqr() > 0 ? normalize(d) : Vec3.ZERO;
				bullet.accel = 0D;
			}
			Entity target = bullet.lockonTarget();
			if(target != null && target.isAlive()) {
				Vec3 motion = bullet.legacyMotion;
				double vel = len(motion);
				double dx = target.getX() - bullet.getX();
				double dy = target.getY() + target.getBbHeight() / 2D - bullet.getY();
				double dz = target.getZ() - bullet.getZ();
				float turn = Math.min(0.005F * bullet.tickCount, 1F);
				double nx = motion.x + (dx - motion.x) * turn;
				double ny = motion.y + (dy - motion.y) * turn;
				double nz = motion.z + (dz - motion.z) * turn;
				double l = len(nx, ny, nz);
				Vec3 n = l < 1.0E-4D ? Vec3.ZERO : new Vec3(nx * (1D / l), ny * (1D / l), nz * (1D / l));
				bullet.legacyMotion = n.scale(vel);
			}
			if(onUpdate != null) onUpdate.accept(bullet);
			bullet.setDeltaMovement(bullet.legacyMotion.scale(bullet.ammoType().projectileSpeed() + bullet.accel));
		};
	}

	// FLYING
	public static final Consumer<LegacyBulletEntity> LAMBDA_STANDARD_ACCELERATE = (bullet) -> {
		if(bullet.accel < 7) bullet.accel += 0.4D;
	};
	public static final Consumer<LegacyBulletEntity> LAMBDA_STEERING_ACCELERATE = (bullet) -> {
		if(!(bullet.ownerEntity() instanceof Player player)) {
			if(bullet.accel < 7) bullet.accel += 0.4D;
			return;
		}
		ItemStack held = player.getMainHandItem();
		steeringAccelerate(bullet, held.isEmpty() || !(held.getItem() instanceof ItemGunBaseNT) || !ItemGunBaseNT.getIsAiming(held));
	};

	public static void steeringAccelerate(LegacyBulletEntity bullet, boolean noSteer) {
		if(bullet.accel < 4) bullet.accel += 0.4D;
		if(!(bullet.ownerEntity() instanceof Player player)) return;

		if(len(bullet.getX() - player.getX(), bullet.getY() - player.getY(), bullet.getZ() - player.getZ()) > 100) return;
		if(noSteer) return;

		// Library.rayTrace(player, 200, 1)
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1F);
		Vec3 hitVec = rayTraceLegacy(player.level(), eye, eye.add(look.x * 200, look.y * 200, look.z * 200));
		if(hitVec == null) return;

		Vec3 vec = new Vec3(hitVec.x - bullet.getX(), hitVec.y - bullet.getY(), hitVec.z - bullet.getZ());
		if(len(vec) < 3) return;
		vec = normalize(vec);

		double speed = len(bullet.legacyMotion);
		bullet.legacyMotion = new Vec3(vec.x * speed, vec.y * speed, vec.z * speed);
	}

	/**
	 * World#func_147447_a(start, end, false, false, true) as used by Library.rayTrace: walks at most 201 block
	 * boundaries, returns the first selection-box hit; air and liquids (canCollideCheck false) only record the
	 * miss result, whose hit vector is the mutated start vector, i.e. the last boundary point reached.
	 */
	static Vec3 rayTraceLegacy(Level level, Vec3 start, Vec3 end) {
		double sx = start.x, sy = start.y, sz = start.z;
		if(Double.isNaN(sx) || Double.isNaN(sy) || Double.isNaN(sz) || Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z)) return null;
		int ex = Mth.floor(end.x), ey = Mth.floor(end.y), ez = Mth.floor(end.z);
		int l = Mth.floor(sx), i1 = Mth.floor(sy), j1 = Mth.floor(sz);
		Vec3 first = collide(level, l, i1, j1, new Vec3(sx, sy, sz), end);
		if(first != null && first != MISS) return first;
		boolean last = false;
		int k1 = 200;
		while(k1-- >= 0) {
			if(Double.isNaN(sx) || Double.isNaN(sy) || Double.isNaN(sz)) return null;
			if(l == ex && i1 == ey && j1 == ez) return last ? new Vec3(sx, sy, sz) : null;
			boolean fx = true, fy = true, fz = true;
			double d0 = 999.0D, d1 = 999.0D, d2 = 999.0D;
			if(ex > l) d0 = l + 1.0D; else if(ex < l) d0 = l + 0.0D; else fx = false;
			if(ey > i1) d1 = i1 + 1.0D; else if(ey < i1) d1 = i1 + 0.0D; else fy = false;
			if(ez > j1) d2 = j1 + 1.0D; else if(ez < j1) d2 = j1 + 0.0D; else fz = false;
			double d3 = 999.0D, d4 = 999.0D, d5 = 999.0D;
			double d6 = end.x - sx, d7 = end.y - sy, d8 = end.z - sz;
			if(fx) d3 = (d0 - sx) / d6;
			if(fy) d4 = (d1 - sy) / d7;
			if(fz) d5 = (d2 - sz) / d8;
			byte b0;
			if(d3 < d4 && d3 < d5) {
				b0 = (byte) (ex > l ? 4 : 5);
				sx = d0; sy += d7 * d3; sz += d8 * d3;
			} else if(d4 < d5) {
				b0 = (byte) (ey > i1 ? 0 : 1);
				sx += d6 * d4; sy = d1; sz += d8 * d4;
			} else {
				b0 = (byte) (ez > j1 ? 2 : 3);
				sx += d6 * d5; sy += d7 * d5; sz = d2;
			}
			l = Mth.floor(sx); if(b0 == 5) --l;
			i1 = Mth.floor(sy); if(b0 == 1) --i1;
			j1 = Mth.floor(sz); if(b0 == 3) --j1;
			Vec3 hit = collide(level, l, i1, j1, new Vec3(sx, sy, sz), end);
			if(hit == MISS) last = true;
			else if(hit != null) return hit;
		}
		return last ? new Vec3(sx, sy, sz) : null;
	}

	private static final Vec3 MISS = new Vec3(Double.NaN, Double.NaN, Double.NaN);

	/** Block#canCollideCheck(meta, false) + collisionRayTrace: MISS for air/liquids, null for no hit, else the hit vector. */
	private static Vec3 collide(Level level, int x, int y, int z, Vec3 from, Vec3 to) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = level.getBlockState(pos);
		if(state.isAir() || state.getBlock() instanceof LiquidBlock) return MISS;
		BlockHitResult r = state.getShape(level, pos).clip(from, to, pos);
		return r == null ? null : r.getLocation();
	}

	// IMPACT
	/** Entity hits during the first 3 ticks are ignored (any entity), the rocket keeps flying. */
	private static boolean early(LegacyBulletEntity bullet, HitResult hit) {
		return hit instanceof EntityHitResult && bullet.tickCount < 3;
	}

	/** baseRocket.setOnEntityHit(null).setOnRicochet(null): nothing happens after an impact lambda that did not kill the rocket. */
	public static final BulletImpact LAMBDA_NONE = (bullet, hit) -> false;

	public static final BulletImpact LAMBDA_STANDARD_EXPLODE = (bullet, hit) -> {
		if(early(bullet, hit)) return false;
		XFactory12ga.standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), 5F, 1F);
		return true;
	};
	/** LAMBDA_MICRO_EXPLODE: shooter protection for 3 ticks, Lego.tinyExplode(bullet, mop, 1.5F). */
	public static final BulletImpact LAMBDA_MICRO_EXPLODE = XFactory762mm::tinyExplodeImpact;
	/**
	 * LAMBDA_STANDARD_EXPLODE_HEAT: Lego.standardExplode(3.5F), then EntityDamageUtil.attackEntityFromNT(living, EXPLOSIVE,
	 * damage * 3, true, true, 0.5F, 5F, 0.2F), or attackEntityFrom for non-living entities.
	 */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_HEAT = (bullet, hit) -> {
		if(early(bullet, hit)) return false;
		XFactory12ga.standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), 3.5F, 1F);
		if(hit instanceof EntityHitResult ehr) {
			Entity entity = ehr.getEntity();
			Entity thrower = bullet.ownerEntity();
			DamageSource source = thrower instanceof LivingEntity shooter
					? bullet.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, bullet, shooter)
					: bullet.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE);
			float amount = bullet.baseDamage() * 3F;
			if(entity instanceof LivingEntity living) {
				XFactory40mm.attackExplosiveNT(living, source, amount, 0.5D, 5F, 0.2F);
			} else {
				entity.hurt(source, amount);
			}
		}
		return true;
	};
	/** LAMBDA_STANDARD_EXPLODE_DEMO: the same ExplosionVNT(5F) setup as XFactory40mm g40_demo. */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_DEMO = (bullet, hit) -> {
		if(early(bullet, hit)) return false;
		XFactory40mm.demoExplode(bullet, hit.getLocation(), 5F);
		return true;
	};
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_INC = (bullet, hit) -> spawnFire(bullet, hit, false, 300);
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE_PHOSPHORUS = (bullet, hit) -> spawnFire(bullet, hit, true, 600);

	/**
	 * spawnFire: Lego.standardExplode(3F), EntityFireLingering setArea(6, 2) of the given duration and type at the hit
	 * vector, then fire in every air block of the 5x5x5 cube around the hit that touches a flammable face.
	 */
	public static boolean spawnFire(LegacyBulletEntity bullet, HitResult hit, boolean phosphorus, int duration) {
		if(early(bullet, hit)) return false;
		Level world = bullet.level();
		Vec3 vec = hit.getLocation();
		XFactory12ga.standardExplode(bullet, vec, bullet.baseDamage(), 3F, 1F);
		LegacyProjectileUtil.spawnLingeringFire(bullet, vec, phosphorus ? LegacyLingeringFireEntity.FireType.PHOSPHORUS : LegacyLingeringFireEntity.FireType.DIESEL,
				6F, 2F, duration, false);
		for(int dx = -2; dx <= 2; dx++) {
			for(int dy = -2; dy <= 2; dy++) {
				for(int dz = -2; dz <= 2; dz++) {
					BlockPos pos = new BlockPos(Mth.floor(vec.x) + dx, Mth.floor(vec.y) + dy, Mth.floor(vec.z) + dz);
					if(world.getBlockState(pos).isAir()) for(Direction dir : Direction.values()) { // ForgeDirection.VALID_DIRECTIONS order
						BlockPos n = pos.relative(dir);
						if(world.getBlockState(n).isFlammable(world, n, dir.getOpposite())) {
							world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
							break;
						}
					}
				}
			}
		}
		return true;
	}

	/// rocket_template order: HE, HEAT, DEMO, INC, PHOSPHORUS ///
	private static final StandardAmmoType[] ITEMS = { StandardAmmoType.ROCKET_HE, StandardAmmoType.ROCKET_HEAT, StandardAmmoType.ROCKET_DEMO, StandardAmmoType.ROCKET_INC, StandardAmmoType.ROCKET_PHOSPHORUS };
	private static final BulletImpact[] IMPACTS = { LAMBDA_STANDARD_EXPLODE, LAMBDA_STANDARD_EXPLODE_HEAT, LAMBDA_STANDARD_EXPLODE_DEMO, LAMBDA_STANDARD_EXPLODE_INC, LAMBDA_STANDARD_EXPLODE_PHOSPHORUS };
	public static final StandardAmmoType[] RPZB = { StandardAmmoType.ROCKET_RPZB_HE, StandardAmmoType.ROCKET_RPZB_HEAT, StandardAmmoType.ROCKET_RPZB_DEMO, StandardAmmoType.ROCKET_RPZB_INC, StandardAmmoType.ROCKET_RPZB_PHOSPHORUS };
	public static final StandardAmmoType[] QD = { StandardAmmoType.ROCKET_QD_HE, StandardAmmoType.ROCKET_QD_HEAT, StandardAmmoType.ROCKET_QD_DEMO, StandardAmmoType.ROCKET_QD_INC, StandardAmmoType.ROCKET_QD_PHOSPHORUS };
	public static final StandardAmmoType[] ML = { StandardAmmoType.ROCKET_ML_HE, StandardAmmoType.ROCKET_ML_HEAT, StandardAmmoType.ROCKET_ML_DEMO, StandardAmmoType.ROCKET_ML_INC, StandardAmmoType.ROCKET_ML_PHOSPHORUS };

	public static final BulletUpdate UPDATE_STANDARD = rocketUpdate(LAMBDA_STANDARD_ACCELERATE);
	public static final BulletUpdate UPDATE_STEERING = rocketUpdate(LAMBDA_STEERING_ACCELERATE);
	/** [parity c1] LAMBDA_NCR_ACCELERATE: steeringAccelerate(entity, false) */
	public static final Consumer<LegacyBulletEntity> LAMBDA_NCR_ACCELERATE = (bullet) -> steeringAccelerate(bullet, false);
	public static final BulletUpdate UPDATE_NCR = rocketUpdate(LAMBDA_NCR_ACCELERATE);
	/** [parity c1] rocket_ncrpa / rocket_ncrpa_steer projectile types (gun_pa_ranged, ArmorNCRPARanged). */
	public static final StandardAmmoType[] NCRPA = { StandardAmmoType.ROCKET_NCRPA_HE, StandardAmmoType.ROCKET_NCRPA_HEAT, StandardAmmoType.ROCKET_NCRPA_DEMO, StandardAmmoType.ROCKET_NCRPA_INC, StandardAmmoType.ROCKET_NCRPA_PHOSPHORUS };
	public static final StandardAmmoType[] NCRPA_STEER = { StandardAmmoType.ROCKET_NCRPA_STEER_HE, StandardAmmoType.ROCKET_NCRPA_STEER_HEAT, StandardAmmoType.ROCKET_NCRPA_STEER_DEMO, StandardAmmoType.ROCKET_NCRPA_STEER_INC, StandardAmmoType.ROCKET_NCRPA_STEER_PHOSPHORUS };

	/** baseRocket: setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0D).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_STANDARD_ACCELERATE) */
	private static void baseRocket(StandardAmmoType type) {
		type.setLife(300).setSelfDamageDelay(10).setVel(0D).setGrav(0D).setOnEntityHit(LAMBDA_NONE).setOnRicochet(LAMBDA_NONE).setOnUpdate(UPDATE_STANDARD);
	}

	static {
		for(int i = 0; i < ITEMS.length; i++) {
			baseRocket(RPZB[i]); RPZB[i].setOnImpact(IMPACTS[i]);                                   // makeRPZB: clone
			baseRocket(QD[i]); QD[i].setOnImpact(IMPACTS[i]).setLife(400).setOnUpdate(UPDATE_STEERING); // makeQD
			baseRocket(ML[i]); ML[i].setOnImpact(IMPACTS[i]);                                       // makeML: clone
			baseRocket(NCRPA_STEER[i]); NCRPA_STEER[i].setOnImpact(IMPACTS[i]).setLife(400).setOnUpdate(UPDATE_NCR); // makeNCR
			baseRocket(NCRPA[i]); NCRPA[i].setOnImpact(IMPACTS[i]);                                 // makeRPZB: clone
		}
		// rocket_ff = baseRocket.clone().setItem(ROCKET_MICRO).setOnUpdate(null).setVel(7F).setOnImpact(LAMBDA_MICRO_EXPLODE)
		baseRocket(StandardAmmoType.ROCKET_FF);
		StandardAmmoType.ROCKET_FF.setOnUpdate(null).setVel(7D).setOnImpact(LAMBDA_MICRO_EXPLODE);
	}

	private static BulletConfig[] clones(StandardAmmoType[] types) {
		BulletConfig[] out = new BulletConfig[types.length];
		for(int i = 0; i < types.length; i++) out[i] = new CloneBulletConfig(types[i], ITEMS[i]);
		return out;
	}

	public static final BulletConfig[] rocket_rpzb = clones(RPZB);
	public static final BulletConfig[] rocket_qd = clones(QD);
	public static final BulletConfig[] rocket_ml = clones(ML);
	public static final BulletConfig rocket_ff = new CloneBulletConfig(StandardAmmoType.ROCKET_FF, StandardAmmoType.ROCKET_MICRO);

	/**
	 * [parity c1] rocket_ncrpa_steer / rocket_ncrpa. Created on first use (ArmorNCRPARanged fills its belts lazily in
	 * 1.7.10 as well), so the BulletConfig.configs indices stored in existing guns do not move.
	 */
	private static BulletConfig[] rocket_ncrpa_steer;
	private static BulletConfig[] rocket_ncrpa;
	public static synchronized BulletConfig[] ncrpaConfigs(boolean steer) {
		if(rocket_ncrpa_steer == null) {
			rocket_ncrpa_steer = clones(NCRPA_STEER);
			rocket_ncrpa = clones(NCRPA);
		}
		return steer ? rocket_ncrpa_steer : rocket_ncrpa;
	}

	public static ItemGunBaseNT createPanzerschreck(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(300).draw(7).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX)
				.rec(new Receiver(0)
						.dmg(HbmConfig::panzerschreckDamage).delay(5).reload(50).jam(40).sound(HbmSoundEvents.GUN_ROCKET_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(rocket_rpzb))
						.offset(1, -0.0625 * 1.5, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ROCKET))
				.setupStandardConfiguration()
				.anim(LAMBDA_PANZERSCHRECK_ANIMS).orchestra(OrchestrasE1.ORCHESTRA_PANERSCHRECK)
				).setDefaultAmmo(StandardAmmoType.ROCKET_HE, 3);
	}

	// [weapons batch F4: stinger]
	public static ItemGunStinger createStinger(Item.Properties properties) {
		return (ItemGunStinger) new ItemGunStinger(properties, WeaponQuality.SPECIAL, new GunConfig()
				.dura(300).draw(7).inspect(40).crosshair(Crosshair.L_BOX_OUTLINE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::stingerDamage).delay(5).reload(50).jam(40).sound(HbmSoundEvents.GUN_ROCKET_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(rocket_rpzb))
						.offset(1, -0.0625 * 1.5, -0.1875D)
						.setupLockonFire().recoil(LAMBDA_RECOIL_ROCKET))
				.setupStandardConfiguration().ps(LAMBDA_STINGER_SECONDARY_PRESS).rs(LAMBDA_STINGER_SECONDARY_RELEASE)
				.anim(LAMBDA_PANZERSCHRECK_ANIMS).orchestra(OrchestrasF4.ORCHESTRA_STINGER)
				).setDefaultAmmo(StandardAmmoType.ROCKET_HEAT, 3);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STINGER_SECONDARY_PRESS = (stack, ctx) -> { ItemGunStinger.setIsLockingOn(stack, true); };
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STINGER_SECONDARY_RELEASE = (stack, ctx) -> { ItemGunStinger.setIsLockingOn(stack, false); };
	public static ItemGunBaseNT createQuadro(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(400).draw(7).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
				.rec(new Receiver(0)
						.dmg(HbmConfig::quadroDamage).spreadHipfire(0F).delay(10).reload(55).jam(40).sound(HbmSoundEvents.GUN_ROCKET_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 4).addConfigs(rocket_qd))
						.offset(1, -0.0625 * 1.5, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ROCKET))
				.setupStandardConfiguration()
				.anim(LAMBDA_QUADRO_ANIMS).orchestra(OrchestrasE1.ORCHESTRA_QUADRO)
				).setDefaultAmmo(StandardAmmoType.ROCKET_HE, 4);
	}

	public static ItemGunBaseNT createFliegerfaust(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SPECIAL, new GunConfig()
				.dura(9 * 25).draw(7).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX)
				.rec(new Receiver(0)
						.dmg(HbmConfig::fliegerfaustDamage).spread(0.015F).spreadHipfire(0.015F).auto(true).delay(4).reload(85).jam(60).sound(HbmSoundEvents.WEAPON_ROCKET_FLAME, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 9).addConfigs(rocket_ff))
						.offset(1, -0.0625 * 1.5, -0.1875D).offsetScoped(1, -0.0625 * 1.5, -0.125D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ROCKET))
				.setupStandardConfiguration().ps(LAMBDA_FLIEGERFAUST_SECONDARY)
				.anim(LAMBDA_FLIEGERFAUST_ANIMS).orchestra(OrchestrasE1.ORCHESTRA_FLIEGERFAUST)
				).setDefaultAmmo(StandardAmmoType.ROCKET_MICRO, 9);
	}

	public static ItemGunBaseNT createMissileLauncher(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(500).draw(20).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
				.rec(new Receiver(0)
						.dmg(HbmConfig::missileLauncherDamage).spreadHipfire(0F).delay(5).reload(48).jam(33).sound(HbmSoundEvents.GUN_ROCKET_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(rocket_ml))
						.offset(1, -0.0625 * 1.5, -0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ROCKET))
				.setupStandardConfiguration().pp(LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS)
				.anim(LAMBDA_MISSILE_LAUNCHER_ANIMS).orchestra(OrchestrasE1.ORCHESTRA_MISSILE_LAUNCHER)
				).setDefaultAmmo(StandardAmmoType.ROCKET_HEAT, 5);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FLIEGERFAUST_SECONDARY = (stack, ctx) -> {

		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		Receiver rec = ctx.config.getReceivers(stack)[0];
		int index = ctx.configIndex;
		GunState state = ItemGunBaseNT.getState(stack, index);

		if(state == GunState.IDLE) {

			boolean soundPlayed = false;

			for(int i = 0; i < 9; i++) {
				if(rec.getCanFire(stack).apply(stack, ctx)) {
					rec.getOnFire(stack).accept(stack, ctx);

					if(rec.getFireSound(stack) != null && !soundPlayed) {
						GunEffects.playSound(entity, rec.getFireSound(stack).get(), rec.getFireVolume(stack), rec.getFirePitch(stack));
						soundPlayed = true;
					}
				} else {

					if(rec.getDoesDryFire(stack)) {
						ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
						ItemGunBaseNT.setState(stack, index, rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
						ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
					}
				}
			}

			if(soundPlayed) {
				ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
				ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
			}
		}

		if(state == GunState.RELOADING) {
			ItemGunBaseNT.setReloadCancel(stack, true);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS = (stack, ctx) -> {
		if(ItemGunBaseNT.getIsAiming(stack)) {
			int target = getLockonTarget(ctx.getPlayer(), 150D, 20D);
			if(target != -1) {
				ItemGunBaseNT.setLockonTarget(stack, target);
				ItemGunBaseNT.setIsLockedOn(stack, true);
			}
		}
		Lego.LAMBDA_STANDARD_CLICK_PRIMARY.accept(stack, ctx);
		ItemGunBaseNT.setIsLockedOn(stack, false);
	};

	/**
	 * ItemGunStinger#getLockonTarget: the closest entity (height >= 0.5, collidable) by angle to the view vector
	 * below the threshold, searched in the AABB spanned by the eye, the look point at the given distance and the two
	 * Vec3NT points rotated around the world Y axis (as in 1.7.10, the absolute position is rotated) +/-10 blocks.
	 */
	public static int getLockonTarget(Player player, double distance, double angleThreshold) {

		if(player == null) return -1;

		double x = player.getX();
		double y = player.getEyeY();
		double z = player.getZ();

		Vec3 view = player.getViewVector(1F);
		double[] delta = { view.x * distance, view.y * distance, view.z * distance };
		double[] look = { delta[0] + x, delta[1] + y, delta[2] + z };
		double[] left = rotY(look, -angleThreshold); left[1] += 10;
		double[] right = rotY(look, angleThreshold); right[1] += -10;
		double[] pos = { x, y, z };

		AABB aabb = new AABB(Math.min(Math.min(look[0], left[0]), Math.min(right[0], pos[0])), Math.min(Math.min(look[1], left[1]), Math.min(right[1], pos[1])), Math.min(Math.min(look[2], left[2]), Math.min(right[2], pos[2])),
				Math.max(Math.max(look[0], left[0]), Math.max(right[0], pos[0])), Math.max(Math.max(look[1], left[1]), Math.max(right[1], pos[1])), Math.max(Math.max(look[2], left[2]), Math.max(right[2], pos[2])));
		List<Entity> entities = player.level().getEntities(player, aabb);
		Entity closestEntity = null;
		double closestAngle = 360D;

		double deltaLen = len(delta[0], delta[1], delta[2]);
		for(Entity entity : entities) {
			if(entity.getBbHeight() < 0.5F || !entity.isPickable()) continue;
			double tx = entity.getX() - x, ty = entity.getY() + entity.getBbHeight() / 2D - y, tz = entity.getZ() - z;

			double vecProd = tx * delta[0] + ty * delta[1] + tz * delta[2];
			double bot = len(tx, ty, tz) * deltaLen;
			double angle = Math.abs(Math.acos(vecProd / bot) * 180 / Math.PI);

			if(angle < closestAngle && angle < angleThreshold) {
				closestAngle = angle;
				closestEntity = entity;
			}
		}

		return closestEntity == null ? -1 : closestEntity.getId();
	}

	/** Vec3NT#rotateAroundYDeg on a copy. */
	private static double[] rotY(double[] v, double deg) {
		double alpha = deg / 180D * Math.PI;
		double cos = Math.cos(alpha);
		double sin = Math.sin(alpha);
		return new double[] { v[0] * cos + v[2] * sin, v[1], v[2] * cos - v[0] * sin };
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ROCKET = (stack, ctx) -> { };

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_PANZERSCHRECK_ANIMS = (stack, type) -> {
		// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
		boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) <= 0;
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case RELOAD: return new BusAnimation()
				.addBus("RELOAD", new BusAnimationSequence().addPos(90, 0, 0, 750, IType.SIN_FULL).addPos(90, 0, 0, 1000).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("ROCKET", new BusAnimationSequence().addPos(0, -3, -6, 0).addPos(0, -3, -6, 750).addPos(0, 0, -6.5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP));
		case JAMMED: empty = false;
		case INSPECT:
			return new BusAnimation()
				.addBus("RELOAD", new BusAnimationSequence().addPos(90, 0, 0, 750, IType.SIN_FULL).addPos(90, 0, 0, 500).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("ROCKET", new BusAnimationSequence().addPos(0, empty ? -3 : 0, 0, 0));
		}
		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_QUADRO_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50).addPos(0, 0, 0, 50));
		case RELOAD: return new BusAnimation()
				.addBus("RELOAD_ROTATE", new BusAnimationSequence().addPos(0, 0, 60, 500, IType.SIN_FULL).addPos(0, 0, 60, 1500).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("RELOAD_PUSH", new BusAnimationSequence().addPos(-1, -1, 0, 0).addPos(-1, -1, 0, 500).addPos(-1, 0, 0, 350).addPos(0, 0, 0, 1000));
		case JAMMED:
		case INSPECT: return new BusAnimation()
				.addBus("RELOAD_ROTATE", new BusAnimationSequence().addPos(0, 0, 60, 750, IType.SIN_FULL).addPos(0, 0, 60, 500).addPos(0, 0, 0, 750, IType.SIN_FULL));
		}
		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLIEGERFAUST_ANIMS = (stack, type) -> {
		boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) <= 0;
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.125, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
		case RELOAD: return new BusAnimation()
				.addBus("RELOAD", new BusAnimationSequence().addPos(75, 0, 0, 750, IType.SIN_FULL).hold(2750).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("DRUM", new BusAnimationSequence().hold(1000).addPos(0, 0, -1, 500, IType.SIN_FULL).addPos(0, -4, -1, 500, IType.SIN_UP).hold(500).addPos(0, 0, -1, 500, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_UP))
				.addBus("DRUMROT", new BusAnimationSequence().hold(1500).addPos(-90, 0, 0, 500, IType.SIN_UP).hold(500).addPos(0, 0, 0, 500, IType.SIN_DOWN))
				.addBus("RECOIL", new BusAnimationSequence().hold(3250).addPos(0, 0, 0.25, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("UNLOADING", new BusAnimationSequence().setPos(1, 0, 0).hold(2500).setPos(0, 0, 0));
		case JAMMED: return new BusAnimation()
				.addBus("RELOAD", new BusAnimationSequence().hold(500).addPos(75, 0, 0, 500, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("DRUM", new BusAnimationSequence().hold(1250).addPos(0, 0, -1, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP))
				.addBus("RECOIL", new BusAnimationSequence().hold(1750).addPos(0, 0, 0.25, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("UNLOADING", new BusAnimationSequence().setPos(1, 0, 0).hold(1750).setPos(0, 0, 0));
		case INSPECT:
			return new BusAnimation()
				.addBus("RELOAD", new BusAnimationSequence().addPos(75, 0, 0, 750, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 750, IType.SIN_FULL))
				.addBus("ROCKET", new BusAnimationSequence().addPos(0, empty ? -3 : 0, 0, 0));
		}
		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MISSILE_LAUNCHER_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
		case RELOAD: return new BusAnimation()
				.addBus("BARREL", new BusAnimationSequence().addPos(0, 0, 1.5, 150).addPos(0, 0, 1.5, 2100).addPos(0, 0, 0, 150))
				.addBus("OPEN", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 1000).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 2250).addPos(-1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("MISSILE", new BusAnimationSequence().addPos(-10, 0, 0, 0).addPos(-10, 0, 0, 750).addPos(3, 0, 2, 0).addPos(0, 0, -6, 350, IType.SIN_FULL).addPos(0, 0, 0, 350, IType.SIN_UP));
		case JAMMED:
		case INSPECT: return new BusAnimation()
				.addBus("BARREL", new BusAnimationSequence().addPos(0, 0, 1.5, 150).addPos(0, 0, 1.5, 1350).addPos(0, 0, 0, 150))
				.addBus("OPEN", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 250).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 1500).addPos(-1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_UP));
		}
		return null;
	};
}

package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.network.GunPlasmaPulsePayload;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * [weapons batch E2] 1.7.10 XFactoryEnergy: gun_tesla_cannon, gun_laser_pistol, gun_laser_pistol_pew_pew,
 * gun_laser_pistol_morning_glory and gun_lasrifle with the capacitor beams (energy_tesla*, energy_las*, energy_emerald*).
 *
 * <p>setCasing(ingot_polymer x2, 4) of the beam configs feeds the casing bag through LegacyCasingTable [parity b1].
 * setSpread(0.0F) equals the spread of the capacitor ammo types. The beam renderers are the LegoClient
 * RENDER_LIGHTNING / RENDER_LIGHTNING_SUB / RENDER_LASER_RED / RENDER_LASER_EMERALD of GunFactoryClient.
 */
public class XFactoryEnergy {

	public static final ResourceLocation scope_luna = AtomicLegacy.id("textures/misc/scope_amat.png");

	/** MainRegistry.proxy.me().inventory for LAMBDA_TESLA_ANIMS (set by the client setup; null on the server). */
	public static Supplier<Container> LOCAL_INVENTORY = () -> null;

	/** XWeaponModManager ids read by ItemRenderLasrifle; the LAS_SHOTGUN / LAS_CAPACITOR / LAS_AUTO mods are not ported. */
	public static final int ID_LAS_SHOTGUN = 216;
	public static final int ID_LAS_CAPACITOR = 217;
	public static final int ID_LAS_AUTO = 218;

	public static final BeamBulletConfig energy_tesla = new BeamBulletConfig(StandardAmmoType.CAPACITOR).setupDamageClass(DamageClass.ELECTRIC).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
			.setOnBeamImpact(XFactoryEnergy::lightningHit).setBeamRenderer(BeamBulletConfig.RENDER_LIGHTNING);
	public static final BeamBulletConfig energy_tesla_overcharge = new BeamBulletConfig(StandardAmmoType.CAPACITOR_OVERCHARGE).setupDamageClass(DamageClass.ELECTRIC).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
			.setDamage(1.5F).setOnBeamImpact(XFactoryEnergy::lightningHit).setBeamRenderer(BeamBulletConfig.RENDER_LIGHTNING);
	public static final BeamBulletConfig energy_tesla_ir = new BeamBulletConfig(StandardAmmoType.CAPACITOR_IR).setupDamageClass(DamageClass.ELECTRIC).setLife(5).setRenderRotations(false)
			.setDamage(0.8F).setOnBeamImpact(XFactoryEnergy::lightningSplit).setBeamRenderer(BeamBulletConfig.RENDER_LIGHTNING);
	public static final BeamBulletConfig energy_tesla_ir_sub = (BeamBulletConfig) new BeamBulletConfig(StandardAmmoType.CAPACITOR_IR).setupDamageClass(DamageClass.ELECTRIC).setLife(3).setRenderRotations(false).setDoesPenetrate(true)
			.setDamage(0.5F).setOnBeamImpact(BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setBeamRenderer(BeamBulletConfig.RENDER_LIGHTNING_SUB).setWear(3F);

	public static final BeamBulletConfig energy_las = las(StandardAmmoType.CAPACITOR, DamageClass.LASER, false, BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT);
	public static final BeamBulletConfig energy_las_overcharge = las(StandardAmmoType.CAPACITOR_OVERCHARGE, DamageClass.LASER, true, BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT);
	public static final BeamBulletConfig energy_las_ir = las(StandardAmmoType.CAPACITOR_IR, DamageClass.FIRE, false, XFactoryEnergy::irHit);

	// energy_las*.clone().setArmorPiercing(0.5F).setThresholdNegation(50F / 75F / 50F), rendered with RENDER_LASER_EMERALD
	public static final BeamBulletConfig energy_emerald = las(StandardAmmoType.CAPACITOR, DamageClass.LASER, false, BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setArmorPiercing(0.5F).setThresholdNegation(50F).setLaserColor(0x15, 0x80, 0x15);
	public static final BeamBulletConfig energy_emerald_overcharge = las(StandardAmmoType.CAPACITOR_OVERCHARGE, DamageClass.LASER, true, BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT).setArmorPiercing(0.5F).setThresholdNegation(75F).setLaserColor(0x15, 0x80, 0x15);
	public static final BeamBulletConfig energy_emerald_ir = las(StandardAmmoType.CAPACITOR_IR, DamageClass.FIRE, false, XFactoryEnergy::irHit).setArmorPiercing(0.5F).setThresholdNegation(50F).setLaserColor(0x15, 0x80, 0x15);

	/** energy_las configs: setBeam().setSpread(0.0F).setLife(5).setRenderRotations(false), RENDER_LASER_RED. */
	private static BeamBulletConfig las(StandardAmmoType ammo, DamageClass clazz, boolean penetrate, BiConsumer<LegacyBulletBeamEntity, HitResult> impact) {
		BeamBulletConfig cfg = new BeamBulletConfig(ammo).setupDamageClass(clazz).setLife(5).setRenderRotations(false).setOnBeamImpact(impact).setLaserColor(0x80, 0x15, 0x15);
		if(penetrate) cfg.setDoesPenetrate(true);
		return cfg;
	}

	public static ItemGunBaseNT createTeslaCannon(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(1_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::teslaCannonDamage).delay(20).spreadHipfire(1.5F).reload(44).jam(19).sound(HbmSoundEvents.GUN_TESLA_FIRE, 1.0F, 1.0F)
						.mag(new MagazineBelt().addConfigs(energy_tesla, energy_tesla_overcharge, energy_tesla_ir))
						.offset(0.75, 0, -0.375).offsetScoped(0.75, 0, -0.25)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
				.setupStandardConfiguration()
				.anim(LAMBDA_TESLA_ANIMS).orchestra(OrchestrasE2.ORCHESTRA_TESLA)
				).setDefaultAmmo(StandardAmmoType.CAPACITOR, 15);
	}

	public static ItemGunBaseNT createLaserPistol(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::laserPistolDamage).delay(5).spread(1F).spreadHipfire(1F).reload(45).jam(37).sound(HbmSoundEvents.GUN_LASER_PISTOL_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 30).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
						.offset(0.75, -0.0625 * 1.5, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
				.setupStandardConfiguration()
				.anim(LAMBDA_LASER_PISTOL).orchestra(OrchestrasE2.ORCHESTRA_LASER_PISTOL)
				).setDefaultAmmo(StandardAmmoType.CAPACITOR, 15);
	}

	public static ItemGunBaseNT createLaserPistolPewPew(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE, new GunConfig()
				.dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::pewPewDamage).rounds(5).delay(10).spread(0.25F).spreadHipfire(1F).reload(45).jam(37).sound(HbmSoundEvents.GUN_LASER_PISTOL_FIRE, 1.0F, 0.8F)
						.mag(new MagazineFullReload(0, 10).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
						.offset(0.75, -0.0625 * 1.5, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
				.setupStandardConfiguration()
				.anim(LAMBDA_LASER_PISTOL).orchestra(OrchestrasE2.ORCHESTRA_LASER_PISTOL)
				).setDefaultAmmo(StandardAmmoType.CAPACITOR_OVERCHARGE, 10);
	}

	public static ItemGunBaseNT createLaserPistolMorningGlory(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(1_500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::morningGloryDamage).delay(7).spread(0F).spreadHipfire(0.5F).reload(45).jam(37).sound(HbmSoundEvents.GUN_LASER_PISTOL_FIRE, 1.0F, 1.1F)
						.mag(new MagazineFullReload(0, 20).addConfigs(energy_emerald, energy_emerald_overcharge, energy_emerald_ir))
						.offset(0.75, -0.0625 * 1.5, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
				.setupStandardConfiguration()
				.anim(LAMBDA_LASER_PISTOL).orchestra(OrchestrasE2.ORCHESTRA_LASER_PISTOL)
				).setDefaultAmmo(StandardAmmoType.CAPACITOR_OVERCHARGE, 20);
	}

	public static ItemGunBaseNT createLasrifle(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE).scopeTexture(scope_luna)
				.rec(new Receiver(0)
						.dmg(HbmConfig::laserRifleDamage).delay(8).spreadHipfire(1F).reload(44).jam(36).sound(HbmSoundEvents.GUN_LASER_RIFLE_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 24).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
						.offset(0.75, -0.0625 * 1.5, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_ENERGY))
				.setupStandardConfiguration()
				.anim(LAMBDA_LASRIFLE).orchestra(OrchestrasE2.ORCHESTRA_LASRIFLE)
				).setDefaultAmmo(StandardAmmoType.CAPACITOR, 24);
	}

	/**
	 * LAMBDA_LIGHTNING_HIT: block hits move the hit vector 0.5 off the hit face; ExplosionVNT(2F) with the shooter as exploder,
	 * EntityProcessorCrossSmooth(1, beam.damage) of the beam's damage class (ELECTRIC), PlayerProcessorStandard and no block
	 * processing or SFX; ufoBlast and fireworks sounds, three plasmablast particles; living entities that were hit directly get
	 * slowness and mining fatigue X for 60 ticks.
	 */
	public static void lightningHit(LegacyBulletBeamEntity beam, HitResult mop) {
		if(!(beam.level() instanceof ServerLevel level)) return;

		Vec3 hit = mop.getLocation();
		if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {
			Direction dir = bhr.getDirection();
			hit = hit.add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
		}

		lightningExplosion(level, beam.getThrower(), hit.x, hit.y, hit.z, 2F, beam.damage, beam.config.dmgClass);
		level.playSound(null, hit.x, hit.y, hit.z, HbmSoundEvents.ENTITY_UFO_BLAST.get(), SoundSource.HOSTILE, 5.0F, 0.9F + level.random.nextFloat() * 0.2F);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 5.0F, 0.5F);

		float yaw = level.random.nextFloat() * 180F;
		for(int i = 0; i < 3; i++) {
			PacketDistributor.sendToPlayersNear(level, null, hit.x, hit.y, hit.z, 100, new GunPlasmaPulsePayload(hit.x, hit.y, hit.z, 0.5F, 0.5F, 1.0F, -60F + 60F * i, yaw, 2F));
		}

		if(mop instanceof EntityHitResult ehr) {
			if(ehr.getEntity() instanceof LivingEntity living) {
				living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
				living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 9));
			}
		}
	}

	/**
	 * LAMBDA_LIGHTNING_SPLIT: LAMBDA_LIGHTNING_HIT, then for entity hits one energy_tesla_ir_sub beam (damage = beam damage,
	 * times its own 0.5) from the hit point to every other living entity within 20 blocks, in random order.
	 */
	public static void lightningSplit(LegacyBulletBeamEntity beam, HitResult mop) {
		lightningHit(beam, mop);
		if(!(mop instanceof EntityHitResult ehr)) return;

		Vec3 hit = mop.getLocation();
		double range = 20;
		List<LivingEntity> potentialTargets = new ArrayList<>(beam.level().getEntitiesOfClass(LivingEntity.class, new AABB(hit, hit).inflate(range, range, range)));
		Collections.shuffle(potentialTargets);

		for(LivingEntity target : potentialTargets) {
			if(target == beam.thrower) continue;
			if(target == ehr.getEntity()) continue;

			Vec3 delta = new Vec3(target.getX() - hit.x, target.getY() + target.getBbHeight() / 2 - hit.y, target.getZ() - hit.z);
			if(delta.length() > 20) continue;
			LegacyBulletBeamEntity sub = new LegacyBulletBeamEntity(beam.thrower, energy_tesla_ir_sub, beam.damage);
			sub.setPos(hit.x, hit.y, hit.z);
			sub.setRotationsFromVector(delta);
			sub.performHitscanExternal(delta.length());
			beam.level().addFreshEntity(sub);
		}
	}

	/** LAMBDA_IR_HIT (identical to the lacunae IR impact of XFactory762mm). */
	public static void irHit(LegacyBulletBeamEntity beam, HitResult mop) {
		XFactory762mm.irHit(beam, mop);
	}

	/**
	 * ExplosionVNT(size, exploder) with EntityProcessorCrossSmooth(1, fixedDamage).setDamageClass(clazz) (setAllowSelfDamage, the
	 * exploder takes half damage, no piercing, no knockback from the attack itself) and PlayerProcessorStandard. Knockback: 1.7.10
	 * EntityProcessorCross (multiplier 1, not applied to bullets and grenades; player knockback through PlayerProcessorStandard only).
	 */
	public static void lightningExplosion(ServerLevel level, Entity exploder, double x, double y, double z, float range, float fixedDamage, DamageClass clazz) {
		double nodeDist = 1D;
		double knockbackMult = 1D;
		float size = range * 2.0F;
		AABB area = new AABB(x - size - 1.0D, y - size - 1.0D, z - size - 1.0D, x + size + 1.0D, y + size + 1.0D, z + size + 1.0D);
		List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
		Explosion compat = new Explosion(level, exploder, x, y, z, range, false, Explosion.BlockInteraction.KEEP);
		EventHooks.onExplosionDetonate(level, compat, list, size);

		Vec3[] nodes = new Vec3[7];
		Direction[] dirs = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		for(int i = 0; i < 6; i++) nodes[i] = new Vec3(x + dirs[i].getStepX() * nodeDist, y + dirs[i].getStepY() * nodeDist, z + dirs[i].getStepZ() * nodeDist);
		nodes[6] = new Vec3(x, y, z); // ForgeDirection.UNKNOWN

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
					double enchKnockback = entity instanceof LivingEntity living ? knockback * (1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) : knockback;

					if(!(entity instanceof LegacyBulletEntity) && !(entity instanceof UniversalGrenadeEntity) && !(entity instanceof Player)) {
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
			// BulletConfig.getDamage(null, exploder, clazz)
			DamageSource dmg = level.damageSources().source(damageType(clazz), null, exploder instanceof LivingEntity ? exploder : null);
			if(entity instanceof LivingEntity living) {
				BeamBulletConfig.attackEntityFromNT(living, dmg, clazz, amount, 0D, 0F, 0F);
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
	}

	private static net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> damageType(DamageClass clazz) {
		return switch(clazz) {
			case ELECTRIC -> HbmDamageTypes.SEDNA_ELECTRIC;
			case FIRE -> HbmDamageTypes.SEDNA_FIRE;
			case PLASMA -> HbmDamageTypes.SEDNA_PLASMA;
			case LASER -> HbmDamageTypes.SEDNA_LASER;
			case SUBATOMIC -> HbmDamageTypes.SEDNA_SUBATOMIC; // [weapons batch F2: damage classes]
			case PHYSICAL -> HbmDamageTypes.SEDNA_PHYSICAL; // [weapons batch F2: damage classes]
		};
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ENERGY = (stack, ctx) -> { };

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_TESLA_ANIMS = (stack, type) -> {
		int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, LOCAL_INVENTORY.get());
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350))
				.addBus("COUNT", new BusAnimationSequence().addPos(amount, 0, 0, 0));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350));
		case INSPECT: return new BusAnimation()
				.addBus("YOMI", new BusAnimationSequence().addPos(8, -4, 0, 0).addPos(4, -1, 0, 500, IType.SIN_DOWN).addPos(4, -1, 0, 1000).addPos(6, -6, 0, 500, IType.SIN_UP))
				.addBus("SQUEEZE", new BusAnimationSequence().addPos(1, 1, 1, 0).addPos(1, 1, 1, 750).addPos(1, 1, 0.5, 125).addPos(1, 1, 1, 125));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASER_PISTOL = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
		case RELOAD: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(0, -20, 0, 100).hold(1900).addPos(0, 0, 0, 100))
				.addBus("LIFT", new BusAnimationSequence().hold(100).addPos(-45, 0, 0, 250, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("JOLT", new BusAnimationSequence().hold(350).addPos(0, 0, 0.5, 100, IType.SIN_FULL).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL).holdUntil(2100).addPos(-0.0625, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BATTERY", new BusAnimationSequence().hold(550).addPos(0, 0, 5, 250).hold(550).setPos(0, -2, -2).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP));
		case JAMMED: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().hold(500).addPos(0, -20, 0, 100).hold(250).addPos(0, 0, 0, 100))
				.addBus("JOLT", new BusAnimationSequence().hold(950).addPos(-0.0625, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("EQUIP", new BusAnimationSequence().hold(1500).addPos(7.5, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("SWIRL", new BusAnimationSequence().addPos(-720, 0, 0, 750, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 750, IType.SIN_FULL));
		}
		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LASRIFLE = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
		case RELOAD: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence().addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 1500).addPos(0, 0, 0, 350, IType.SIN_UP))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, -5, 0, 350, IType.SIN_UP).addPos(0, -5, 0, 500).addPos(0, -0.25, 0, 500, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 1700).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
		case JAMMED: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence().addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
				.addBus("EQUIP", new BusAnimationSequence().addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
		}

		return null;
	};
}

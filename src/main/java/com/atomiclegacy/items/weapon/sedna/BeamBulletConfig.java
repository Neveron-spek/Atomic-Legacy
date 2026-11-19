package com.atomiclegacy.items.weapon.sedna;

import java.util.function.BiConsumer;

import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.items.PoweredArmorFSBItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.registry.HbmDamageTypes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 BulletConfig with {@code setBeam()} (ProjectileType.BEAM): the fields the original
 * read from the config of an {@code EntityBulletBeamBase}. Damage multiplier, penetration,
 * threshold negation, armor piercing and knockback stay on the {@link StandardAmmoType}.
 */
public class BeamBulletConfig extends BulletConfig {

	/** DamageResistanceHandler.DamageClass subset used by the ported beams. */
	public enum DamageClass { LASER, FIRE, PLASMA, /** [weapons batch E2] */ ELECTRIC, /** [weapons batch F2] */ SUBATOMIC, PHYSICAL }

	public DamageClass dmgClass = DamageClass.LASER;
	/** BulletConfig.expires */
	public int expires = 30;
	/** BulletConfig.renderRotations */
	public boolean renderRotations = true;
	/** BulletConfig.impactsEntities */
	public boolean impactsEntities = true;
	/** BulletConfig.isSpectral */
	public boolean spectral = false;
	/** BulletConfig.onImpactBeam */
	public BiConsumer<LegacyBulletBeamEntity, HitResult> onImpactBeam;
	/** LegoClient.renderStandardLaser color of the config's rendererBeam (RENDER_LASER_*), -1 for none. */
	public int laserColor = -1;
	/** [weapons batch E2] rendererBeam other than renderStandardLaser: LegoClient.RENDER_LIGHTNING / RENDER_LIGHTNING_SUB. */
	public static final int RENDER_LIGHTNING = 1;
	public static final int RENDER_LIGHTNING_SUB = 2;
	/** [weapons batch F2] LegoClient.RENDER_TAU / RENDER_TAU_CHARGE / RENDER_NI4NI_BOLT. */
	public static final int RENDER_TAU = 3;
	public static final int RENDER_TAU_CHARGE = 4;
	public static final int RENDER_NI4NI_BOLT = 5;
	public int beamRenderer = 0;
	/** [weapons batch F4] rendererBeam LegoClient.RENDER_FOLLY (folly_sm), RENDER_CRACKLE (p35800), RENDER_BLACK_LIGHTNING (p35800_bl). */
	public static final int RENDER_FOLLY = 3;
	public static final int RENDER_CRACKLE = 4;
	public static final int RENDER_BLACK_LIGHTNING = 5;
	/** [weapons batch F4] BulletConfig.onUpdate of a beam config (EntityBulletBeamBase#onUpdate runs it before Entity#onUpdate). */
	public java.util.function.Consumer<LegacyBulletBeamEntity> onUpdateBeam;
	public BeamBulletConfig setOnUpdateBeam(java.util.function.Consumer<LegacyBulletBeamEntity> lambda) { this.onUpdateBeam = lambda; return this; }
	/** [weapons batch F4] BulletConfig.setSpectral */
	public BeamBulletConfig setSpectral(boolean spectral) { this.spectral = spectral; return this; }
	/**
	 * [weapons batch E2] BulletConfig.damageMult / doesPenetrate / armorPiercingPercent / armorThresholdNegation of beam configs
	 * that set them differently from their ammo item (XFactoryEnergy); null keeps the StandardAmmoType value.
	 */
	public Float damageOverride;
	public Boolean penetrateOverride;
	public Float armorPiercingOverride;
	public Float thresholdNegationOverride;

	public BeamBulletConfig(StandardAmmoType ammo) {
		super(ammo);
	}

	public BeamBulletConfig setupDamageClass(DamageClass clazz) { this.dmgClass = clazz; return this; }
	public BeamBulletConfig setLife(int expires) { this.expires = expires; return this; }
	public BeamBulletConfig setRenderRotations(boolean rot) { this.renderRotations = rot; return this; }
	public BeamBulletConfig setOnBeamImpact(BiConsumer<LegacyBulletBeamEntity, HitResult> lambda) { this.onImpactBeam = lambda; return this; }
	public BeamBulletConfig setLaserColor(int r, int g, int b) { this.laserColor = (r << 16) | (g << 8) | b; return this; }
	public BeamBulletConfig setBeamRenderer(int renderer) { this.beamRenderer = renderer; return this; }
	public BeamBulletConfig setDamage(float damageMult) { this.damageOverride = damageMult; return this; }
	public BeamBulletConfig setDoesPenetrate(boolean pen) { this.penetrateOverride = pen; return this; }
	public BeamBulletConfig setArmorPiercing(float armorPiercingPercent) { this.armorPiercingOverride = armorPiercingPercent; return this; }
	public BeamBulletConfig setThresholdNegation(float armorThresholdNegation) { this.thresholdNegationOverride = armorThresholdNegation; return this; }

	@Override public float damageMult() { return this.damageOverride != null ? this.damageOverride : super.damageMult(); }
	public boolean doesPenetrate() { return this.penetrateOverride != null ? this.penetrateOverride : this.ammo.penetrates(); }
	public float armorPiercing() { return this.armorPiercingOverride != null ? this.armorPiercingOverride : this.ammo.armorPiercingPercent(); }
	public float thresholdNegation() { return this.thresholdNegationOverride != null ? this.thresholdNegationOverride : this.ammo.armorThresholdNegation(); }

	/**
	 * BulletConfig.LAMBDA_STANDARD_BEAM_HIT: EntityDamageUtil.attackEntityFromNT(living, getDamage(beam, thrower, dmgClass),
	 * damage, ignoreIFrame, allowSpecialCancel, knockbackMult, armorThresholdNegation, armorPiercingPercent).
	 * ConfettiUtil.decideConfetti (ash/skeleton kill particles) is not ported.
	 */
	public static final BiConsumer<LegacyBulletBeamEntity, HitResult> LAMBDA_STANDARD_BEAM_HIT = (bullet, mop) -> {

		if(mop instanceof EntityHitResult ehr) {
			Entity entity = ehr.getEntity();

			if(entity instanceof LivingEntity living && living.getHealth() <= 0) return;

			DamageSource source = getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);

			if(!(entity instanceof LivingEntity living)) {
				LegacyProjectileUtil.hurtNoIFrame(entity, source, bullet.damage);
				return;
			}

			StandardAmmoType ammo = bullet.config.ammo;
			attackEntityFromNT(living, source, bullet.config.dmgClass, bullet.damage, ammo.knockbackMultiplier(), bullet.config.thresholdNegation(), bullet.config.armorPiercing());
		}
	};

	/** BulletConfig.getDamage: DamageSourceSednaWithAttacker / NoAttacker of the damage class. */
	public static DamageSource getDamage(Entity projectile, LivingEntity shooter, DamageClass dmgClass) {
		var key = dmgClass == DamageClass.FIRE ? HbmDamageTypes.SEDNA_FIRE : dmgClass == DamageClass.PLASMA ? HbmDamageTypes.SEDNA_PLASMA
				: dmgClass == DamageClass.ELECTRIC ? HbmDamageTypes.SEDNA_ELECTRIC
				: dmgClass == DamageClass.SUBATOMIC ? HbmDamageTypes.SEDNA_SUBATOMIC // [weapons batch F2]
				: dmgClass == DamageClass.PHYSICAL ? HbmDamageTypes.SEDNA_PHYSICAL : HbmDamageTypes.SEDNA_LASER;
		return shooter != null ? projectile.damageSources().source(key, projectile, shooter) : projectile.damageSources().source(key);
	}

	/**
	 * EntityDamageUtil.attackEntityFromNT with DamageResistanceHandler: the attack is cancelled when the remaining
	 * threshold covers the damage or the resistance reaches 100%, otherwise DT/DR are applied with the piercing values,
	 * then (LASER, armor bypassing source) the 1.7.10 armor formula with armor piercing. FIRE uses the port's
	 * sedna_fire type (vanilla armor and fire resistance, like LegacyProjectileUtil.hurtSednaFire).
	 */
	public static boolean attackEntityFromNT(LivingEntity living, DamageSource source, DamageClass dmgClass, float amount, double knockbackMultiplier, float pierceDT, float pierce) {
		Entity attacker = source.getEntity();
		if(living instanceof ServerPlayer target && attacker instanceof Player player && !target.canHarmPlayer(player)) return false;

		// [weapons batch E2] ELECTRIC is CATEGORY_ENERGY without exact entries, like PLASMA
		// [weapons batch F2] SUBATOMIC is CATEGORY_ENERGY without exact entries (like LASER), PHYSICAL is CATEGORY_PHYSICAL
		// [parity I1: DamageResistanceHandler] single 1.7.10 table (sets, jackt, creepers); the static per-group tables below are kept for older diagnostics
		float[] res = com.atomiclegacy.util.LegacyDamageResistance.getDTDR(living, dmgClass.name());
		float dt = Math.max(0F, res[0] - pierceDT);
		float dr = res[1] * Mth.clamp(1F - pierce, 0F, 2F);
		if((dt > 0 && dt >= amount) || res[1] - pierce >= 1F) return false; // DamageResistanceHandler.onEntityAttacked
		// [weapons batch E2] DamageResistanceHandler.onEntityDamaged: "electric" damage x5 against an ArmorFSBPowered chestplate
		if(dmgClass == DamageClass.ELECTRIC && living.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PoweredArmorFSBItem) amount *= 5;
		amount -= dt;
		amount *= (1F - dr);
		if(dmgClass == DamageClass.LASER || dmgClass == DamageClass.PLASMA || dmgClass == DamageClass.ELECTRIC || dmgClass == DamageClass.SUBATOMIC || dmgClass == DamageClass.PHYSICAL) { // [weapons batch F2]
			// EntityDamageUtil.applyArmorCalculationsNT
			amount = amount * (25F - living.getArmorValue() * (1F - pierce)) / 25F;
		}

		Vec3 motion = living.getDeltaMovement();
		living.invulnerableTime = 0;
		boolean registered = living.hurt(source, amount);
		living.setDeltaMovement(motion);
		if(registered && attacker != null && knockbackMultiplier > 0) knockBack(living, attacker, knockbackMultiplier);
		living.hurtMarked = true;
		return registered;
	}

	/** [weapons batch F2] CATEGORY_PHYSICAL full set resistances, the table LegacyProjectileUtil uses for the physical sedna bullets. {threshold, resistance} */
	public static float[] physicalResistance(String group) {
		return switch(group) {
			case "steel" -> new float[] {2F, 0.10F};
			case "titanium" -> new float[] {3F, 0.10F};
			case "alloy", "cobalt" -> new float[] {2F, 0.10F};
			case "starmetal" -> new float[] {3F, 0.25F};
			case "security", "cmb" -> new float[] {5F, 0.50F};
			case "schrabidium" -> new float[] {10F, 0.65F};
			default -> new float[] {0F, 0F};
		};
	}

	/** EntityDamageUtil.knockBack */
	public static void knockBack(LivingEntity living, Entity attacker, double multiplier) {
		double motionX = attacker.getX() - living.getX();
		double motionZ;
		for(motionZ = attacker.getZ() - living.getZ(); motionX * motionX + motionZ * motionZ < 1.0E-4D; motionZ = (Math.random() - Math.random()) * 0.01D) {
			motionX = (Math.random() - Math.random()) * 0.01D;
		}
		if(living.getRandom().nextDouble() >= living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) {
			living.hasImpulse = true;
			double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
			double magnitude = 0.4D * multiplier;
			Vec3 m = living.getDeltaMovement().scale(0.5D);
			double mY = m.y + magnitude;
			if(mY > 0.2D) mY = 0.2D * multiplier;
			living.setDeltaMovement(m.x - motionX / horizontal * magnitude, mY, m.z - motionZ / horizontal * magnitude);
		}
	}

	/**
	 * DamageResistanceHandler.initDefaults, full armor sets: CATEGORY_ENERGY entry or exact LASER entry, otherwise the
	 * set's "other" resistance. {threshold, resistance}
	 */
	public static float[] energyResistance(String group) {
		return switch(group) {
			case "rpa" -> new float[] {30F, 0.8F};
			case "ncrpa" -> new float[] {10F, 0.5F};
			case "fau" -> new float[] {25F, 0.95F};
			case "trenchmaster" -> new float[] {15F, 0.9F};
			case "starmetal" -> new float[] {1F, 0.1F};
			case "zirconium" -> new float[] {0F, 1F};
			case "cmb" -> new float[] {5F, 0.25F};
			case "schrabidium" -> new float[] {5F, 0.5F};
			case "steamsuit", "dieselsuit", "t51", "envsuit" -> new float[] {0F, 0.1F};
			case "ajr", "ajro" -> new float[] {0F, 0.15F};
			case "blackjack" -> new float[] {2F, 0.15F};
			case "hev", "bismuth" -> new float[] {2F, 0.25F};
			case "dns" -> new float[] {1000F, 1F};
			case "taurun" -> new float[] {2F, 0.1F};
			default -> new float[] {0F, 0F};
		};
	}

	/**
	 * [weapons batch D1] DamageClass.PLASMA (CATEGORY_ENERGY, no exact entries): like {@link #energyResistance}, except that
	 * fau and trenchmaster only have an exact LASER entry, so PLASMA falls back to their "other" resistance.
	 */
	public static float[] plasmaResistance(String group) {
		return switch(group) {
			case "fau" -> new float[] {100F, 0.99F};
			case "trenchmaster" -> new float[] {5F, 0.25F};
			default -> energyResistance(group);
		};
	}

	/** DamageResistanceHandler.initDefaults, full armor sets: CATEGORY_FIRE entry, otherwise the set's "other" resistance. */
	public static float[] fireResistance(String group) {
		return switch(group) {
			case "steamsuit" -> new float[] {0.5F, 0.25F};
			case "dieselsuit", "hev" -> new float[] {0.5F, 0.5F};
			case "t51", "ajr", "ajro" -> new float[] {0.5F, 0.35F};
			case "rpa" -> new float[] {25F, 0.9F};
			case "ncrpa" -> new float[] {10F, 0.9F};
			case "blackjack" -> new float[] {2.5F, 0.5F};
			case "envsuit" -> new float[] {2F, 0.75F};
			case "bismuth", "trenchmaster" -> new float[] {5F, 0.5F};
			case "fau" -> new float[] {100F, 1F};
			case "dns" -> new float[] {0F, 1F};
			case "taurun" -> new float[] {0F, 0.25F};
			case "asbestos" -> new float[] {10F, 0.9F};
			case "starmetal" -> new float[] {1F, 0.1F};
			case "zirconium" -> new float[] {0F, 1F};
			case "cmb" -> new float[] {5F, 0.25F};
			case "schrabidium" -> new float[] {5F, 0.5F};
			default -> new float[] {0F, 0F};
		};
	}
}

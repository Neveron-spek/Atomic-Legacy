package com.atomiclegacy.entity;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTrait;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.pollution.HbmArmorProtection.HazardClass;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmHazardSystem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.blocks.LegacyVariantSlabBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F1] 1.7.10 com.hbm.entity.projectile.EntityChemical (on EntityThrowableNT): the chemthrower spray.
 *
 * <p>TYPE INFO (1.7.10):
 * if ANTIMATTER: ignore all other traits, become a gamma beam with no gravity;
 * if HOT: set fire and deal extra fire damage, scaling with the temperature;
 * if COLD: freeze, duration scaling with temperature, assuming COMBUSTIBLE does not apply;
 * if GAS / EVAP: short range with the spread going up;
 * if LIQUID: if EVAP doesn't apply, create a narrow spray with long range affected by gravity;
 * if COMBUSTIBLE: auto-ignite; if FLAMMABLE: with GAS or EVAP the same as COMBUSTIBLE, otherwise a neutral spray that adds the "soaked" (oil) effect;
 * if CORROSIVE: apply extra acid damage, poison effect as well as armor degradation.
 *
 * <p>Fluid traits are read from the port's {@link HbmFluidDefinition}: FT_Flammable/FT_Combustible energies from the fuel
 * properties, FT_Corrosive from the CORROSIVE:n token, FT_VentRadiation from RADIATION:n, FT_Poison from POISON:withering:n,
 * FT_Toxin from the five 1.7.10 toxin fluids (chlorine, phosgene, mustardgas, estradiol, redmud) and FT_Pheromone from the
 * two pheromone fluids. Not ported: IRepairable#tryExtinguish of burning multiblocks (no IRepairable in this port).
 */
public class LegacyChemicalEntity extends Entity {

	private static final EntityDataAccessor<Integer> FLUID_ID = SynchedEntityData.defineId(LegacyChemicalEntity.class, EntityDataSerializers.INT);

	/** Client hook (GunBatchF1ClientEffects): the per-tick particles of EntityChemical#onUpdate on the client. */
	public static BiConsumer<LegacyChemicalEntity, ChemicalStyle> CLIENT_EFFECTS = (chem, style) -> { };

	/** HbmLivingProps.oil (EntityChemical: setOil(living, 300)); consumed by GunBatchF1Events like EntityEffectHandler. */
	public static final String OIL_TICKS_TAG = LegacyMistEntity.OIL_TICKS_TAG;

	@Nullable private UUID throwerUuid;
	@Nullable private LivingEntity throwerCache;
	public int ticksInAir;

	public LegacyChemicalEntity(EntityType<? extends LegacyChemicalEntity> type, Level level) {
		super(type, level);
		// [parity S4: culling] ignoreFrustumCheck: LegacyChemicalEntityRenderer#shouldRender uses the finite style envelope.
	}

	/** EntityThrowableNT(World, EntityLivingBase): the offsets are unused in 1.7.10 as well. */
	public LegacyChemicalEntity(Level level, LivingEntity thrower, double sideOffset, double heightOffset, double frontOffset) {
		this(HbmEntityTypes.LEGACY_CHEMICAL.get(), level);
		this.throwerUuid = thrower.getUUID();
		this.throwerCache = thrower;
		float yaw = thrower.getYRot();
		float pitch = thrower.getXRot();
		moveTo(thrower.getX(), thrower.getY() + thrower.getEyeHeight(), thrower.getZ(), yaw, pitch);
		double x = getX() - (double) (Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F);
		double y = getY() - 0.1D;
		double z = getZ() - (double) (Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F);
		setPos(x, y, z);
		float velocity = 0.4F;
		double motionX = (double) (-Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * velocity);
		double motionZ = (double) (Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * velocity);
		double motionY = (double) (-Mth.sin((pitch + throwAngle()) / 180.0F * (float) Math.PI) * velocity);
		setThrowableHeading(motionX, motionY, motionZ, throwForce(), 1.0F);
	}

	protected float throwForce() { return 1.5F; }
	protected double headingForceMult() { return 0.0075D; }
	protected float throwAngle() { return 0.0F; }
	protected double motionMult() { return 1.0D; }

	/** EntityThrowableNT#setThrowableHeading */
	public void setThrowableHeading(double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
		float throwLen = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
		motionX /= (double) throwLen;
		motionY /= (double) throwLen;
		motionZ /= (double) throwLen;
		motionX += this.random.nextGaussian() * headingForceMult() * (double) inaccuracy;
		motionY += this.random.nextGaussian() * headingForceMult() * (double) inaccuracy;
		motionZ += this.random.nextGaussian() * headingForceMult() * (double) inaccuracy;
		motionX *= (double) velocity;
		motionY *= (double) velocity;
		motionZ *= (double) velocity;
		setDeltaMovement(motionX, motionY, motionZ);
		float hyp = Mth.sqrt((float) (motionX * motionX + motionZ * motionZ));
		setYRot((float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI));
		setXRot((float) (Math.atan2(motionY, (double) hyp) * 180.0D / Math.PI));
		this.yRotO = getYRot();
		this.xRotO = getXRot();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(FLUID_ID, 0);
	}

	public LegacyChemicalEntity setFluid(HbmFluidDefinition fluid) {
		this.entityData.set(FLUID_ID, fluid.oldId());
		return this;
	}

	public HbmFluidDefinition getFluid() {
		return HbmFluids.byOldId(this.entityData.get(FLUID_ID)).orElse(HbmFluids.none());
	}

	@Nullable
	public LivingEntity getThrower() {
		if(this.throwerCache != null && this.throwerCache.isAlive()) return this.throwerCache;
		if(this.throwerUuid != null && level() instanceof ServerLevel server && server.getEntity(this.throwerUuid) instanceof LivingEntity living) {
			this.throwerCache = living;
			return living;
		}
		return this.throwerCache;
	}

	@Override
	public void tick() {
		super.tick();

		if(!level().isClientSide) {

			if(this.tickCount > this.getMaxAge()) {
				discard();
			}

			HbmFluidDefinition type = this.getFluid();

			if(type.hasTrait(HbmFluidTrait.GASEOUS) || type.hasTrait(HbmFluidTrait.EVAPORATES)) {

				double intensity = 1D - (double) this.tickCount / (double) this.getMaxAge();
				LivingEntity thrower = getThrower();
				List<Entity> affected = level().getEntities(thrower, getBoundingBox().inflate(intensity * 2.5, intensity * 2.5, intensity * 2.5), e -> e != this);

				for(Entity e : affected) {
					this.affect(e, intensity);
				}
			}

		} else {
			CLIENT_EFFECTS.accept(this, getStyle());
		}

		throwableUpdate();
	}

	/** EntityThrowableNT#onUpdate (never stuck in the ground: EntityChemical does not call getStuck). */
	private void throwableUpdate() {
		++this.ticksInAir;

		Vec3 motion = getDeltaMovement();
		Vec3 pos = position();
		Vec3 nextPos = pos.add(motion.x * motionMult(), motion.y * motionMult(), motion.z * motionMult());
		// World#func_147447_a(pos, next, false, true, false): collision boxes only, no liquids
		HitResult mop = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if(mop.getType() == HitResult.Type.MISS) mop = null;
		pos = position();
		nextPos = pos.add(motion.x * motionMult(), motion.y * motionMult(), motion.z * motionMult());

		if(mop != null) {
			nextPos = mop.getLocation();
		}

		if(!level().isClientSide && this.doesImpactEntities()) {

			Entity hitEntity = null;
			List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(motion.x * motionMult(), motion.y * motionMult(), motion.z * motionMult()).inflate(1.0D, 1.0D, 1.0D));
			double nearest = 0.0D;
			LivingEntity thrower = this.getThrower();
			Vec3 nonPenImpact = null;

			for(Entity entity : list) {

				if(entity.isPickable() && (entity != thrower || this.ticksInAir >= this.selfDamageDelay()) && entity.isAlive()) {
					double hitbox = 0.3F;
					AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
					Vec3 hitVec = aabb.clip(pos, nextPos).orElse(null);

					if(hitVec != null) {
						double dist = pos.distanceTo(hitVec);

						if(dist < nearest || nearest == 0.0D) {
							hitEntity = entity;
							nearest = dist;
							nonPenImpact = hitVec;
						}
					}
				}
			}

			if(hitEntity != null) {
				mop = new EntityHitResult(hitEntity, nonPenImpact);
			}
		}

		if(mop != null) {
			if(mop instanceof BlockHitResult bhr && level().getBlockState(bhr.getBlockPos()).getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock portal) {
				setAsInsidePortal(portal, bhr.getBlockPos()); // Blocks.portal -> setInPortal()
			} else {
				this.onImpact(mop);
			}
		}

		if(!this.onGround()) {
			float hyp = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
			setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

			for(setXRot((float) (Math.atan2(motion.y, (double) hyp) * 180.0D / Math.PI)); getXRot() - this.xRotO < -180.0F; this.xRotO -= 360.0F);

			while(getXRot() - this.xRotO >= 180.0F) this.xRotO += 360.0F;
			while(getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
			while(getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;

			setXRot(this.xRotO + (getXRot() - this.xRotO) * 0.2F);
			setYRot(this.yRotO + (getYRot() - this.yRotO) * 0.2F);
		}

		float drag = this.getAirDrag();
		double gravity = this.getGravityVelocity();

		// fullBlockCollisions() is false: the position is advanced directly
		double x = getX() + motion.x * motionMult();
		double y = getY() + motion.y * motionMult();
		double z = getZ() + motion.z * motionMult();

		if(this.isInWater()) {
			for(int i = 0; i < 4; ++i) {
				float f = 0.25F;
				level().addParticle(ParticleTypes.BUBBLE, x - motion.x * (double) f, y - motion.y * (double) f, z - motion.z * (double) f, motion.x, motion.y, motion.z);
			}

			drag = this.getWaterDrag();
		}

		setDeltaMovement(motion.x * (double) drag, motion.y * (double) drag - gravity, motion.z * (double) drag);
		setPos(x, y, z);
	}

	public boolean doesImpactEntities() { return true; }
	public int selfDamageDelay() { return 5; }

	protected void affect(Entity e, double intensity) {

		ChemicalStyle style = getStyle();
		HbmFluidDefinition type = getFluid();
		LivingEntity living = e instanceof LivingEntity ? (LivingEntity) e : null;
		int temperature = type.temperatureCelsius();

		if(style == ChemicalStyle.LIQUID || style == ChemicalStyle.BURNING) //ignore range penalty for liquids
			intensity = 1D;

		if(style == ChemicalStyle.AMAT) {
			LegacyProjectileUtil.hurtNoIFrame(e, damageSources().source(HbmDamageTypes.RADIATION), 1F);
			if(living != null) {
				contaminate(living, 50F * (float) intensity);
				return;
			}
		}

		if(style == ChemicalStyle.LIGHTNING) {
			LegacyProjectileUtil.hurtNoIFrame(e, damageSources().source(HbmDamageTypes.ELECTRICITY), 0.5F);
			if(living != null) {
				living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9));
				return;
			}
		}

		if(temperature >= 100) {
			LegacyProjectileUtil.hurtNoIFrame(e, getDamage(HbmDamageTypes.BOIL), Math.min(0.25F + (temperature - 100) * 0.001F, 15F)); //.25 damage at 100°C with one extra damage every 1000°C

			if(temperature >= 500) {
				e.igniteForSeconds(10); //afterburn for 10 seconds
			}
		}

		if(style == ChemicalStyle.LIQUID || style == ChemicalStyle.GAS) {
			if(temperature < -20) {
				if(living != null) { //only living things are affected
					LegacyProjectileUtil.hurtNoIFrame(e, getDamage(HbmDamageTypes.BOIL), Math.min(0.25F + (-temperature) * 0.01F, 2F));
				}
			}

			if(type.hasTrait(HbmFluidTrait.DELICIOUS)) {
				if(living != null && living.isAlive()) {
					living.heal(2F * (float) intensity);
				}
			}
		}

		if(style == ChemicalStyle.LIQUID) {

			if(type.hasTrait(HbmFluidTrait.FLAMMABLE)) {
				if(living != null) {
					living.getPersistentData().putInt(OIL_TICKS_TAG, 300); //doused in oil for 15 seconds
				}
			}
			if(type.hasTrait(HbmFluidTrait.DELICIOUS)) {
				if(living != null && living.isAlive()) {
					living.heal(2F * (float) intensity);
				}
			}
		}

		if(this.isExtinguishing()) {
			e.clearFire();
		}

		if(style == ChemicalStyle.BURNING) {
			boolean combustible = type.hasTrait(HbmFluidTrait.COMBUSTIBLE);
			LegacyProjectileUtil.hurtNoIFrame(e, getDamage(HbmDamageTypes.FLAMETHROWER), 0.2F + (combustible ? (Math.min(type.combustibleHeatEnergy() / 100_000F, 15F)) : 0));
			e.igniteForSeconds(5);
		}

		if(style == ChemicalStyle.GASFLAME) {
			boolean flammable = type.hasTrait(HbmFluidTrait.FLAMMABLE);
			boolean combustible = type.hasTrait(HbmFluidTrait.COMBUSTIBLE);

			float heat = Math.max(flammable ? type.flammableHeatEnergy() / 50_000F : 0, combustible ? Math.min(type.combustibleHeatEnergy() / 100_000F, 15F) : 0);
			heat *= intensity;
			LegacyProjectileUtil.hurtNoIFrame(e, getDamage(HbmDamageTypes.FLAMETHROWER), (0.2F + heat) * (float) intensity);
			e.igniteForSeconds((int) Math.ceil(5 * intensity));
		}

		if(type.hasTrait(HbmFluidTrait.CORROSIVE)) {
			int rating = traitInteger(type, "CORROSIVE", 0);

			if(living != null) {
				LegacyProjectileUtil.hurtNoIFrame(living, getDamage(HbmDamageTypes.ACID), rating / 50F);
				int armorDamage = rating / 40;
				if(armorDamage > 0) {
					for(net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] { net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET }) {
						living.getItemBySlot(slot).hurtAndBreak(armorDamage, living, slot);
					}
				}
			}
		}

		double radPerMB = HbmHazardSystem.fluidRadiation(type);
		if(radPerMB > 0) {
			if(living != null) {
				contaminate(living, (float) radPerMB * 5);
			}
			if(level() instanceof ServerLevel server) ChunkRadiationData.get(server).incrementRadiation(BlockPos.containing(e.getX(), e.getY(), e.getZ()), radPerMB * 5);
		}

		if(type.hasTrait(HbmFluidTrait.POISON)) {
			boolean withering = traitBoolean(type, "POISON", false);

			if(living != null) {
				living.addEffect(new MobEffectInstance(withering ? MobEffects.WITHER : MobEffects.POISON, (int) (5 * 20 * intensity)));
			}
		}

		if(living != null) {
			affectToxin(type, living, intensity);
		}

		int pheromone = type.name().equals("pheromone") ? 1 : type.name().equals("pheromone_m") ? 2 : 0;
		if(pheromone != 0) {

			if(living != null) {
				living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 2));
				living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 60 * 20, 1));
				living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2 * 60 * 20, 4));

				if(living instanceof GlyphidEntity && pheromone == 1) {
					living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 5 * 60 * 20, 4));
					living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60 * 20, 0));
					living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 19));

				} else if(living instanceof Player && pheromone == 2) {
					living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 2));
				}
			}
		}

		if(type.name().equals("xpjuice")) {

			if(e instanceof Player player) {
				player.giveExperiencePoints(1);
				discard();
			}
		}

		if(type.name().equals("enderjuice")) {
			this.teleportRandomly(e);
		}
	}

	/**
	 * FT_Toxin entries of the pinned 1.7.10 Fluids: CHLORINE ToxinDirectDamage(cloud, 2, 20, GAS_LUNG), PHOSGENE (cloud, 4, 20, GAS_LUNG),
	 * MUSTARDGAS (cloud, 4, 10, GAS_BLISTERING) + ToxinEffects(GAS_BLISTERING, hazmat: wither 100/1, nausea 100/0),
	 * ESTRADIOL ToxinEffects(PARTICLE_FINE: death 60 min), REDMUD ToxinEffects(GAS_BLISTERING: wither 30 s/2).
	 */
	private void affectToxin(HbmFluidDefinition type, LivingEntity living, double intensity) {
		switch(type.name()) {
		case "chlorine" -> toxinDirectDamage(living, intensity, 2F, 20, HazardClass.GAS_LUNG, false);
		case "phosgene" -> toxinDirectDamage(living, intensity, 4F, 20, HazardClass.GAS_LUNG, false);
		case "mustardgas" -> {
			toxinDirectDamage(living, intensity, 4F, 10, HazardClass.GAS_BLISTERING, false);
			if(!toxinProtected(living, HazardClass.GAS_BLISTERING, true)) {
				living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
				living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
			}
		}
		case "estradiol" -> { if(!toxinProtected(living, HazardClass.PARTICLE_FINE, false)) living.addEffect(new MobEffectInstance(HbmMobEffects.DEATH, 60 * 60 * 20, 0)); }
		case "redmud" -> { if(!toxinProtected(living, HazardClass.GAS_BLISTERING, false)) living.addEffect(new MobEffectInstance(MobEffects.WITHER, 30 * 20, 2)); }
		default -> { }
		}
	}

	/** ToxinEntry#isProtected: ArmorRegistry.hasAllProtection(entity, head, clazz) + ArmorUtil.damageGasMaskFilter(entity, 1); fullBody also needs a hazmat. */
	private static boolean toxinProtected(LivingEntity entity, HazardClass clazz, boolean fullBody) {
		boolean hasMask = clazz == null;
		boolean hasSuit = !fullBody;
		if(clazz != null && HbmArmorProtection.hasHeadProtection(entity, clazz, 1)) hasMask = true;
		if(fullBody && HbmArmorProtection.hasLegacyHazmatProtection(entity)) hasSuit = true;
		return hasMask && hasSuit;
	}

	/** ToxinDirectDamage#poison: ModDamageSource.cloud every delay ticks, amount * intensity. */
	private void toxinDirectDamage(LivingEntity entity, double intensity, float amount, int delay, HazardClass clazz, boolean fullBody) {
		if(toxinProtected(entity, clazz, fullBody)) return;
		if(delay == 0 || level().getGameTime() % delay == 0) {
			entity.hurt(damageSources().source(HbmDamageTypes.CLOUD), (float) (amount * intensity));
		}
	}

	/** ContaminationUtil.contaminate(living, RADIATION, CREATIVE, amount): env dose, creative/new players and rad-immune mobs skipped, armor bonuses apply. */
	private static void contaminate(LivingEntity living, float amount) {
		// [parity K2: chemical] the shared ContaminationUtil (Rad-X, mutation and the full immune list included)
		com.atomiclegacy.util.ContaminationUtil.contaminate(living, com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
				com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, amount);
	}

	/** ContaminationUtil.isRadImmune (the list used by the port's legacy contact sources). */
	private static boolean isLegacyRadiationImmune(LivingEntity target) {
		return target instanceof LegacyNuclearCreeperEntity
				|| target instanceof LegacyTaintedCreeperEntity
				|| target instanceof LegacyCyberCrabEntity
				|| target instanceof LegacyMaskManEntity
				|| target instanceof LegacyRadBeastEntity
				|| target instanceof LegacyUfoEntity
				|| target instanceof LegacyChopperEntity
				|| target instanceof LegacyWormHeadEntity
				|| target instanceof LegacyWormBodyEntity
				|| target instanceof MushroomCow
				|| target instanceof Zombie
				|| target instanceof Skeleton
				|| target instanceof LegacyQuackosEntity
				|| target instanceof Ocelot;
	}

	private static int traitInteger(HbmFluidDefinition fluid, String trait, int fallback) {
		for(String token : fluid.rawTraits().split("\\|")) {
			String[] parts = token.trim().split(":");
			if(parts.length >= 2 && parts[0].equalsIgnoreCase(trait)) {
				try {
					return Integer.parseInt(parts[1]);
				} catch(NumberFormatException ignored) {
					return fallback;
				}
			}
		}
		return fallback;
	}

	private static boolean traitBoolean(HbmFluidDefinition fluid, String trait, boolean fallback) {
		for(String token : fluid.rawTraits().split("\\|")) {
			String[] parts = token.trim().split(":");
			if(parts.length >= 2 && parts[0].equalsIgnoreCase(trait)) {
				return Boolean.parseBoolean(parts[1]);
			}
		}
		return fallback;
	}

	/* whether this type should extinguish entities */
	protected boolean isExtinguishing() {
		return this.getStyle() == ChemicalStyle.LIQUID && this.getFluid().temperatureCelsius() < 50 && !this.getFluid().hasTrait(HbmFluidTrait.FLAMMABLE);
	}

	/** EnumExtinguishType of the fire extinguisher: CO2 for carbondioxide, WATER for water / heavywater / coolant. */
	@Nullable
	protected ExtinguishType getExtinguishingType(HbmFluidDefinition type) {
		String name = type.name();
		return name.equals("carbondioxide") ? ExtinguishType.CO2 : name.equals("water") || name.equals("heavywater") || name.equals("coolant") ? ExtinguishType.WATER : null;
	}

	public enum ExtinguishType {
		WATER,
		CO2
	}

	/** EntityDamageSourceIndirect(name, this, thrower) */
	protected DamageSource getDamage(ResourceKey<DamageType> name) {
		LivingEntity thrower = getThrower();
		if(thrower != null) {
			return damageSources().source(name, this, thrower);
		} else {
			return damageSources().source(name);
		}
	}

	//terribly copy-pasted from EntityEnderman.class
	public boolean teleportRandomly(Entity e) {
		double x = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
		double y = this.getY() + (double) (this.random.nextInt(64) - 32);
		double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
		return this.teleportTo(e, x, y, z);
	}

	public boolean teleportTo(Entity e, double x, double y, double z) {

		double targetX = e.getX();
		double targetY = e.getY();
		double targetZ = e.getZ();
		double posX = x;
		double posY = y;
		double posZ = z;
		boolean flag = false;
		int i = Mth.floor(posX);
		int j = Mth.floor(posY);
		int k = Mth.floor(posZ);

		if(e.level().hasChunkAt(new BlockPos(i, j, k))) {
			boolean flag1 = false;

			while(!flag1 && j > e.level().getMinBuildHeight()) {
				BlockState block = e.level().getBlockState(new BlockPos(i, j - 1, k));

				if(block.blocksMotion()) {
					flag1 = true;
				} else {
					--posY;
					--j;
				}
			}

			if(flag1) {
				e.setPos(posX, posY, posZ);

				if(e.level().noCollision(e, e.getBoundingBox()) && !e.level().containsAnyLiquid(e.getBoundingBox())) {
					flag = true;
				}
			}
		}

		if(!flag) {
			e.setPos(targetX, targetY, targetZ);
			return false;
		} else {
			short short1 = 128;

			for(int l = 0; l < short1; ++l) {
				double d6 = (double) l / ((double) short1 - 1.0D);
				float f = (this.random.nextFloat() - 0.5F) * 0.2F;
				float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
				float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
				double d7 = targetX + (e.getX() - targetX) * d6 + (this.random.nextDouble() - 0.5D) * (double) e.getBbWidth() * 2.0D;
				double d8 = targetY + (e.getY() - targetY) * d6 + this.random.nextDouble() * (double) e.getBbHeight();
				double d9 = targetZ + (e.getZ() - targetZ) * d6 + (this.random.nextDouble() - 0.5D) * (double) e.getBbWidth() * 2.0D;
				if(e.level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.PORTAL, d7, d8, d9, 1, (double) f, (double) f1, (double) f2, 1.0D);
			}

			e.level().playSound(null, targetX, targetY, targetZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
			e.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
			return true;
		}
	}

	protected void onImpact(HitResult mop) {

		if(!level().isClientSide) {

			if(mop instanceof EntityHitResult ehr) {
				this.affect(ehr.getEntity(), 1D - (double) this.tickCount / (double) this.getMaxAge());
			}

			if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {

				HbmFluidDefinition type = getFluid();
				BlockPos hit = bhr.getBlockPos();
				int x = hit.getX();
				int y = hit.getY();
				int z = hit.getZ();

				double radPerMB = HbmHazardSystem.fluidRadiation(type);
				if(radPerMB > 0 && level() instanceof ServerLevel server) {
					ChunkRadiationData.get(server).incrementRadiation(hit, radPerMB * 5);
				}

				ChemicalStyle style = getStyle();

				if(style == ChemicalStyle.BURNING || style == ChemicalStyle.GASFLAME) {

					for(Direction dir : Direction.values()) {

						Block fire = type.name().equals("balefire") ? HbmBlocks.BALEFIRE.get() : Blocks.FIRE;
						BlockPos pos = hit.relative(dir);

						if(level().getBlockState(pos).isAir()) {
							level().setBlock(pos, fire.defaultBlockState(), 3);
						}
					}
				}

				if(this.isExtinguishing()) {

					for(Direction dir : Direction.values()) {
						BlockPos pos = hit.relative(dir);

						if(level().getBlockState(pos).is(Blocks.FIRE)) {
							level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}

				ExtinguishType fext = this.getExtinguishingType(type);
				if(fext != null) {
					// [parity L3: refinery damage] IRepairable#tryExtinguish of a burning refinery core
					net.minecraft.world.level.block.entity.BlockEntity hitBe = level().getBlockEntity(hit);
					if(hitBe instanceof com.atomiclegacy.tileentity.MachineDummyBlockEntity dummyBe) hitBe = level().getBlockEntity(dummyBe.getCorePos());
					if(hitBe instanceof com.atomiclegacy.tileentity.RefineryBlockEntity refinery) {
						refinery.tryExtinguish(fext == ExtinguishType.CO2, fext == ExtinguishType.WATER);
					}

					if(fext == ExtinguishType.WATER && style == ChemicalStyle.LIQUID) {
						for(int i = -2; i <= 2; i++) {
							for(int j = 0; j <= 1; j++) {
								for(int k = -2; k <= 2; k++) {
									BlockPos pos = new BlockPos(x + i, y + j, z + k);
									if(level().getBlockState(pos).is(HbmBlocks.FALLOUT.get())) {
										level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
									}
								}
							}
						}
					}
				}

				if(type.name().equals("seedslurry")) {

					for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) for(int k = -1; k <= 1; k++) {
						BlockPos pos = new BlockPos(x + i, y + j, z + k);
						BlockState state = level().getBlockState(pos);
						Block block = state.getBlock();
						if(block == Blocks.DIRT || block == HbmBlocks.WASTE_EARTH.get() || block == HbmBlocks.DIRT_DEAD.get() || block == HbmBlocks.DIRT_OILY.get()) {

							BlockPos above = pos.above();
							if(level().getMaxLocalRawBrightness(above) >= 9 && level().getBlockState(above).getLightBlock(level(), above) <= 2) {
								level().setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
							}
						}
						if(block == Blocks.COBBLESTONE) level().setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
						if(block == Blocks.STONE_BRICKS) level().setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
						if(block == HbmBlocks.WASTE_EARTH.get()) level().setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
						if(block == HbmBlocks.BRICK_CONCRETE.get()) level().setBlock(pos, HbmBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState(), 3);
						if(block == HbmBlocks.CONCRETE_BRICK_SLAB.get() && state.hasProperty(LegacyVariantSlabBlock.VARIANT) && state.getValue(LegacyVariantSlabBlock.VARIANT) == 0) level().setBlock(pos, state.setValue(LegacyVariantSlabBlock.VARIANT, 1), 3);
						if(block == HbmBlocks.BRICK_CONCRETE_STAIRS.get()) level().setBlock(pos, copyProperties(state, HbmBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get().defaultBlockState()), 3);
					}
				}

				discard();
			}
		}
	}

	/** Metadata-preserving block swap of the concrete brick stairs. */
	private static BlockState copyProperties(BlockState from, BlockState to) {
		for(Property<?> property : from.getProperties()) {
			if(to.hasProperty(property)) to = copyProperty(from, to, property);
		}
		return to;
	}

	private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
		return to.setValue(property, from.getValue(property));
	}

	protected float getAirDrag() {

		ChemicalStyle type = getStyle();

		if(type == ChemicalStyle.AMAT) return 1F;
		if(type == ChemicalStyle.LIGHTNING) return 1F;
		if(type == ChemicalStyle.GAS) return 0.95F;

		return 0.99F;
	}

	protected float getWaterDrag() {

		ChemicalStyle type = getStyle();

		if(type == ChemicalStyle.AMAT) return 1F;
		if(type == ChemicalStyle.LIGHTNING) return 1F;
		if(type == ChemicalStyle.GAS) return 1F;

		return 0.8F;
	}

	public int getMaxAge() {

		switch(this.getStyle()) {
		case AMAT: return 100;
		case LIGHTNING: return 5;
		case BURNING: return 600;
		case GAS: return 60;
		case GASFLAME: return 20;
		case LIQUID: return 600;
		default: return 100;
		}
	}

	public double getGravityVelocity() {

		ChemicalStyle type = getStyle();

		if(type == ChemicalStyle.AMAT) return 0D;
		if(type == ChemicalStyle.LIGHTNING) return 0D;
		if(type == ChemicalStyle.GAS) return 0D;
		if(type == ChemicalStyle.GASFLAME) return -0.01D;

		return 0.03D;
	}

	public ChemicalStyle getStyle() {
		return getStyleFromType(this.getFluid());
	}

	public static ChemicalStyle getStyleFromType(HbmFluidDefinition type) {

		if(type.name().equals("iongel")) {
			return ChemicalStyle.LIGHTNING;
		}

		if(type.hasTrait(HbmFluidTrait.ANTIMATTER)) {
			return ChemicalStyle.AMAT;
		}

		if(type.hasTrait(HbmFluidTrait.GASEOUS) || type.hasTrait(HbmFluidTrait.EVAPORATES)) {

			if(type.hasTrait(HbmFluidTrait.FLAMMABLE) || type.hasTrait(HbmFluidTrait.COMBUSTIBLE)) {
				return ChemicalStyle.GASFLAME;
			} else {
				return ChemicalStyle.GAS;
			}
		}

		if(type.hasTrait(HbmFluidTrait.LIQUID)) {

			if(type.hasTrait(HbmFluidTrait.COMBUSTIBLE)) {
				return ChemicalStyle.BURNING;
			} else {
				return ChemicalStyle.LIQUID;
			}
		}

		return ChemicalStyle.NULL;
	}

	/**
	 * The general type of the chemical, determines rendering and movement
	 */
	public static enum ChemicalStyle {
		AMAT,		//renders as beam
		LIGHTNING,	//renders as beam
		LIQUID,		//no renderer, fluid particles
		GAS,		//renders as particles
		GASFLAME,	//renders as fire particles
		BURNING,	//no renderer, fire particles
		NULL
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(FLUID_ID, tag.getInt("fluid"));
		this.ticksInAir = tag.getInt("ticksInAir");
		this.throwerUuid = tag.hasUUID("thrower") ? tag.getUUID("thrower") : null;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("fluid", this.entityData.get(FLUID_ID));
		tag.putInt("ticksInAir", this.ticksInAir);
		if(this.throwerUuid != null) tag.putUUID("thrower", this.throwerUuid);
	}

	/** EntityThrowableNT#isInRangeToRenderDist: 4 x the average edge length (0.25) x 64. */
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double perimeter = getBoundingBox().getSize() * 4.0D * 64.0D;
		return distance < perimeter * perimeter;
	}
}

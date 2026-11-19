package com.atomiclegacy.entity;

import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.radiation.ChunkRadiationData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 EntityRADBeast attributes and melee targeting. */
public final class LegacyRadBeastEntity extends Monster {
    private static final EntityDataAccessor<Integer> UNFORTUNATE_SOUL =
            SynchedEntityData.defineId(LegacyRadBeastEntity.class, EntityDataSerializers.INT);
    private float heightOffset = 0.5F;
    private int heightOffsetUpdateTime;
    private int attackCooldown;
    /** [parity J1] EntityCreature old AI (path following with strafing, wander, idle look). */
    private final com.atomiclegacy.entity.ai.LegacyCreatureAi legacyAi;

    public LegacyRadBeastEntity(EntityType<? extends LegacyRadBeastEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 30;
        legacyAi = new com.atomiclegacy.entity.ai.LegacyCreatureAi(this,
                (com.atomiclegacy.entity.ai.LegacyPathNavigation) navigation);
        moveControl = com.atomiclegacy.entity.ai.LegacyCreatureAi.noMoveControl(this);
        lookControl = com.atomiclegacy.entity.ai.LegacyCreatureAi.headFollowsYaw(this);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.atomiclegacy.entity.ai.LegacyPathNavigation(this, level, true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(UNFORTUNATE_SOUL, 0);
    }

    /** The target id synchronized by EntityRADBeast for the client beam renderer. */
    public Entity getUnfortunateSoul() {
        int id = entityData.get(UNFORTUNATE_SOUL);
        return id == 0 ? null : level().getEntity(id);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 8.0D)
                // [parity D1] old (non-task) AI: EntityLivingBase.applyEntityAttributes sets movementSpeed to 0.1
                // when isAIEnabled() is false, and EntityRADBeast never overrides it.
                .add(Attributes.MOVEMENT_SPEED, 0.10000000149011612D);
    }

    /** HBM 1.7.10 EntityRADBeast.makeLeader(). */
    public LegacyRadBeastEntity makeLeader() {
        setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        Item radiationCoin = BuiltInRegistries.ITEM.get(AtomicLegacy.id("coin_radiation"));
        if (radiationCoin == Items.AIR) {
            throw new IllegalStateException("Missing required legacy item atomiclegacy:coin_radiation");
        }
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(radiationCoin));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(360.0D);
        setHealth(getMaxHealth());
        return this;
    }

    /**
     * [parity D1/J1] EntityRADBeast runs the old EntityCreature AI: targeting, pathing, wandering, path following
     * and attacks are driven from {@link #tickLegacyCreatureAi()}; there are no goals.
     */
    @Override
    protected void registerGoals() {
    }

    /** Old AI: moveForward is the movementSpeed attribute and getAIMoveSpeed() is the constant 0.1. */
    @Override
    public float getSpeed() {
        return 0.1F;
    }

    /** [parity D1] 1.7.10 applyArmorCalculations with getTotalArmorValue() == 8: damage * (25 - armor) / 25. */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return amount;
        }
        hurtArmor(source, amount);
        return amount * (25 - 8) / 25.0F;
    }

    /** EntityMob.attackEntityFrom: whoever damages the mob becomes entityToAttack. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this
                && attacker != getVehicle() && !getPassengers().contains(attacker)) {
            setTarget(attacker);
        }
        return hurt;
    }

    /**
     * EntityCreature.updateEntityActionState + EntityMob.findPlayerToAttack + EntityRADBeast.attackEntity:
     * closest vulnerable player within 16 blocks that can be seen; the target is kept until it dies or turns
     * creative; attacks only with line of sight; while the target is seen within 30 blocks (hasAttacked) no new
     * path is requested, otherwise one is requested when there is none or with a 1/20 chance per tick.
     */
    private void tickLegacyCreatureAi() {
        legacyAi.tickFleeing();
        boolean hasAttacked = false;
        LivingEntity target = getTarget();
        if (target == null) {
            Player player = level().getNearestPlayer(net.minecraft.world.entity.ai.targeting.TargetingConditions
                    .forCombat().range(16.0D).ignoreLineOfSight(), this);
            if (player != null && hasLineOfSight(player)) {
                setTarget(player);
                target = player;
                legacyAi.navigation().setLegacyPath(legacyAi.navigation().pathToEntity(player, 16.0F, false));
            }
        } else if (target.isAlive()) {
            if (hasLineOfSight(target)) {
                hasAttacked = legacyAttackEntity(target, distanceTo(target));
            }
        } else {
            setTarget(null);
            target = null;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            setTarget(null);
            target = null;
        }
        // EntityMob.getBlockPathWeight: 0.5 - brightness (getWalkTargetValue)
        legacyAi.repath(target, hasAttacked, false,
                () -> legacyAi.updateWanderPath(pos -> getWalkTargetValue(pos, level())));
        legacyAi.followPath(target, hasAttacked, getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
    }

    /** EntityRADBeast.attackEntity; returns the hasAttacked flag (movement ceased for this tick). */
    private boolean legacyAttackEntity(LivingEntity target, float distance) {
        if (attackCooldown <= 0 && distance < 2.0F
                && target.getBoundingBox().maxY > getBoundingBox().minY
                && target.getBoundingBox().minY < getBoundingBox().maxY) {
            attackCooldown = 20;
            doHurtTarget(target);
            return false;
        } else if (distance < 30.0F) {
            if (attackCooldown == 0) {
                ChunkRadiationData.get((ServerLevel) level()).incrementRadiation(blockPosition(), 100.0D);
                // [parity D1] ModDamageSource.radiation: a plain source without attacker or direct entity
                target.hurt(damageSources().source(HbmDamageTypes.RADIATION), 16.0F);
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                playSound(getAmbientSound(), getSoundVolume(), getVoicePitch());
                attackCooldown = 20;
            }
            double deltaX = target.getX() - getX();
            double deltaZ = target.getZ() - getZ();
            float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
            return true;
        }
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return switch (random.nextInt(6)) {
            case 0 -> HbmSoundEvents.GEIGER_1.get();
            case 1 -> HbmSoundEvents.GEIGER_2.get();
            case 2 -> HbmSoundEvents.GEIGER_3.get();
            case 3 -> HbmSoundEvents.GEIGER_4.get();
            case 4 -> HbmSoundEvents.GEIGER_5.get();
            default -> HbmSoundEvents.GEIGER_6.get();
        };
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.BLAZE_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return HbmSoundEvents.STEP_IRON.get();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getLightLevelDependentMagicValue() {
        // EntityRADBeast#getBrightness returned 1.0F and
        // getBrightnessForRender returned full-bright (15728880), so the
        // modern light-dependent hook must not sample the world light.
        return 1.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (isInWaterOrRain()) hurt(damageSources().drown(), 1.0F);
            if (--heightOffsetUpdateTime <= 0) {
                heightOffsetUpdateTime = 100;
                heightOffset = 0.5F + (float) random.nextGaussian() * 3.0F;
            }
            LivingEntity target = getTarget();
            if (target != null && target.getEyeY() > getEyeY() + heightOffset) {
                setDeltaMovement(getDeltaMovement().add(0.0D,
                        (0.3D - getDeltaMovement().y) * 0.3D, 0.0D));
            }
            if (target != null && attackCooldown < 10) {
                entityData.set(UNFORTUNATE_SOUL, target.getId());
            } else {
                entityData.set(UNFORTUNATE_SOUL, 0);
            }
            // EntityLivingBase.onEntityUpdate: attackTime counts down once per tick
            if (attackCooldown > 0) attackCooldown--;
            if (isAlive()) {
                tickLegacyCreatureAi();
            }
        }
        if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }
        if (level().isClientSide) {
            if (getMaxHealth() <= 150.0D) {
                for (int i = 0; i < 6; i++) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.MYCELIUM /* [parity D1] "townaura" */,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5D,
                            getY() + random.nextDouble() * getBbHeight(),
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5D,
                            0.0D, 0.0D, 0.0D);
                }
                if (random.nextInt(6) == 0) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            getY() + random.nextDouble() * getBbHeight() * 0.75D,
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            0.0D, 0.0D, 0.0D);
                }
            } else {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                        getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                        getY() + random.nextDouble() * getBbHeight() * 0.75D,
                        getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                        0.0D, 0.0D, 0.0D);
            }
        }
        super.tick();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (getMaxHealth() > 150.0F && level() instanceof ServerLevel level) {
            HbmAdvancements.awardNearby(level, getBoundingBox().inflate(50.0D), "boss_meltdown");
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // [parity D1] dropEquipment still runs: the leader's coin_radiation (drop chance 1.0) was lost without it
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (!recentlyHit) return;
        int looting = legacyLootingLevel();
        if (looting > 0) {
            spawnLegacyDrop(level, "nugget_polonium", looting);
        }
        int count = random.nextInt(3) + 1;
        for (int i = 0; i < count; i++) {
            int choice = random.nextInt(3);
            if (isInWaterOrRain()) {
                spawnLegacyDrop(level, choice == 0 ? "waste_uranium" : choice == 1 ? "waste_mox" : "waste_plutonium", 2);
            } else {
                spawnLegacyDrop(level, choice == 0 ? "rod_zirnox_uranium_fuel_depleted" :
                        choice == 1 ? "rod_zirnox_mox_fuel_depleted" : "rod_zirnox_plutonium_fuel_depleted", 1);
            }
        }
    }

    private void spawnLegacyDrop(ServerLevel level, String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(com.atomiclegacy.AtomicLegacy.id(id));
        if (item != Items.AIR) spawnAtLocation(new ItemStack(item, count));
    }

    private int legacyLootingLevel() {
        LivingEntity killer = getLastHurtByMob();
        if (killer == null) {
            return 0;
        }
        ItemStack weapon = killer.getMainHandItem();
        if (weapon.isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                killer.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.LOOTING), weapon);
    }
}

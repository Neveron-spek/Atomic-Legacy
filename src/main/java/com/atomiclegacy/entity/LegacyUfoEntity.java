package com.atomiclegacy.entity;

import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.explosion.LegacyMukeExplosion;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Direct port of EntityUFO's target scan, attack cycle, beam and crash sequence. */
public final class LegacyUfoEntity extends Monster {
    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(LegacyUfoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WAYPOINT_X =
            SynchedEntityData.defineId(LegacyUfoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Y =
            SynchedEntityData.defineId(LegacyUfoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Z =
            SynchedEntityData.defineId(LegacyUfoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEAM_GROUND_Y =
            SynchedEntityData.defineId(LegacyUfoEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.atomiclegacy.entity_ufo"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private int courseChangeCooldown;
    private int scanCooldown;
    private int hurtCooldown;
    private int beamTimer;
    private UUID targetUuid;
    private final List<UUID> secondaryTargets = new ArrayList<>();
    private boolean crashResolved;

    public LegacyUfoEntity(EntityType<? extends LegacyUfoEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        deathTime = -30;
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20_000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void setScanCooldown(int value) {
        this.scanCooldown = Math.max(0, value);
    }

    public boolean beamActive() { return entityData.get(BEAM); }
    public int beamGroundY() { return entityData.get(BEAM_GROUND_Y); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM, false);
        builder.define(WAYPOINT_X, 0);
        builder.define(WAYPOINT_Y, 0);
        builder.define(WAYPOINT_Z, 0);
        builder.define(BEAM_GROUND_Y, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hurtCooldown > 0 || isDeadOrDying()) {
            return false;
        }
        boolean hit = super.hurt(source, amount);
        if (hit) {
            hurtCooldown = 5;
        }
        return hit;
    }

    // [parity A2: ufo] EntityUFO#updateEntityActionState runs before EntityFlying#moveEntityWithHeading;
    // the modern equivalents are customServerAiStep (skipped while dying) and travel.
    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (!level().isClientSide) {
            bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        }
    }

    @Override
    protected void customServerAiStep() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (hurtCooldown > 0) hurtCooldown--;
        if (courseChangeCooldown > 0) courseChangeCooldown--;
        if (scanCooldown > 0) scanCooldown--;
        Entity target = target();
        if (target == null || !target.isAlive()) {
            targetUuid = null;
            target = null;
        }
        if (scanCooldown <= 0) {
            target = scanTargets();
            scanCooldown = 50;
        }
        // EntityUFO (unlike EntityUFOBase) has no course without a target: it hovers in place.
        if (target != null && courseChangeCooldown <= 0) {
            chooseWaypoint(target);
            courseChangeCooldown = 40 + random.nextInt(20);
        }
        updateBeam(target);
        if (tickCount % 300 < 200) {
            if (tickCount % 4 == 0) fireAtSecondaryOr(target, true);
            else if (tickCount % 4 == 2) fireLaser(target);
        } else if (tickCount % 20 == 0) {
            fireAtSecondaryOr(target, false);
        } else if (tickCount % 20 == 10) {
            fireRocket(target);
        }
        moveTowardWaypoint(target);
    }

    /** EntityFlying#moveEntityWithHeading: no gravity, 0.91 drag (0.8 in water, 0.5 in lava). */
    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(0.02F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.8D));
        } else if (isInLava()) {
            moveRelative(0.02F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.5D));
        } else {
            float friction = 0.91F;
            if (onGround()) {
                friction = level().getBlockState(getBlockPosBelowThatAffectsMyMovement()).getFriction(level(), getBlockPosBelowThatAffectsMyMovement(), this) * 0.91F;
            }
            float acceleration = 0.16277136F / (friction * friction * friction);
            moveRelative(onGround() ? 0.1F * acceleration : 0.02F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(friction));
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
    }

    private Entity scanTargets() {
        secondaryTargets.clear();
        Entity selected = null;
        List<Entity> entities = level().getEntities(this, getBoundingBox().inflate(100.0D, 50.0D, 100.0D), Entity::isAlive);
        for (Entity entity : entities) {
            // canAttackClass: not another UFO, not an NPC bullet
            if (entity instanceof LegacyUfoEntity || entity instanceof LegacyBossProjectileEntity) continue;
            if (entity instanceof Player player) {
                if (player.isCreative() || player.isSpectator() || player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) continue;
                if (selected == null || distanceToSqr(entity) < distanceToSqr(selected)) selected = entity;
            }
            if (entity instanceof LivingEntity && distanceToSqr(entity) < 10_000.0D && canSee(entity) && entity != selected) {
                secondaryTargets.add(entity.getUUID());
            }
        }
        if (selected == null && !secondaryTargets.isEmpty() && level() instanceof ServerLevel serverLevel) {
            selected = serverLevel.getEntity(secondaryTargets.get(random.nextInt(secondaryTargets.size())));
        }
        targetUuid = selected == null ? null : selected.getUUID();
        return selected;
    }

    private boolean canSee(Entity entity) {
        HitResult result = level().clip(new ClipContext(getEyePosition(), entity.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS;
    }

    private void chooseWaypoint(Entity target) {
        Vec3 delta = new Vec3(getX() - target.getX(), 0.0D, getZ() - target.getZ());
        if (delta.lengthSqr() < 0.001D) delta = new Vec3(1.0D, 0.0D, 0.0D);
        if (random.nextInt(3) > 0) {
            double angle = Math.PI * 2.0D * random.nextFloat();
            delta = new Vec3(delta.x * Math.cos(angle) - delta.z * Math.sin(angle), 0.0D,
                    delta.x * Math.sin(angle) + delta.z * Math.cos(angle));
        }
        delta = delta.normalize();
        int x = Mth.floor(target.getX() - delta.x * 35.0D);
        int z = Mth.floor(target.getZ() - delta.z * 35.0D);
        int surface = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        entityData.set(WAYPOINT_X, x);
        entityData.set(WAYPOINT_Y, Math.max(surface + 20 + random.nextInt(15), Mth.floor(target.getY()) + 15));
        entityData.set(WAYPOINT_Z, z);
    }

    private void moveTowardWaypoint(Entity target) {
        Vec3 waypoint = new Vec3(entityData.get(WAYPOINT_X), entityData.get(WAYPOINT_Y), entityData.get(WAYPOINT_Z));
        Vec3 delta = waypoint.subtract(position());
        double distance = delta.length();
        if (courseChangeCooldown > 0 && distance > 5.0D) {
            if (courseTraversable(delta, distance)) {
                double speed = target instanceof Player ? 5.0D : 2.0D;
                Vec3 motion = delta.scale(speed / distance);
                setDeltaMovement(motion);
                updateRotation(motion);
                return;
            }
            courseChangeCooldown = 0;
        }
        setDeltaMovement(Vec3.ZERO);
    }

    private boolean courseTraversable(Vec3 delta, double length) {
        Vec3 step = delta.scale(1.0D / Math.max(1.0D, length));
        AABB box = getBoundingBox();
        for (int i = 1; i < length; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) return false;
        }
        return true;
    }

    private void updateBeam(Entity target) {
        if (beamTimer <= 0 && beamActive()) entityData.set(BEAM, false);
        if (target != null && Math.abs(target.getX() - getX()) + Math.abs(target.getZ() - getZ()) < 25.0D) {
            beamTimer = 30;
        }
        if (beamTimer-- <= 0) return;
        if (!beamActive()) {
            entityData.set(BEAM, true);
            level().playSound(null, blockPosition(), HbmSoundEvents.ENTITY_UFO_BEAM.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        }
        int groundY = findGroundY();
        entityData.set(BEAM_GROUND_Y, groundY);
        if (groundY >= getY()) return;
        AABB beamBox = new AABB(getX(), groundY, getZ(), getX(), getY(), getZ()).inflate(5.0D, 0.0D, 5.0D);
        for (Entity entity : level().getEntities(this, beamBox)) {
            if (entity instanceof LegacyUfoEntity || entity instanceof LegacyBossProjectileEntity) continue;
            // [parity A2] ModDamageSource.causeCombineDamage(ufo, victim): "cmb", projectile, bypasses armor
            entity.hurt(damageSources().source(com.atomiclegacy.registry.HbmDamageTypes.CMB, this, entity), 1000.0F);
            entity.igniteForSeconds(5.0F);
            // [parity K2: ufo] EntityUFO beam: contaminate(RADIATION, CREATIVE, 5F)
            if (entity instanceof LivingEntity living) {
                com.atomiclegacy.util.ContaminationUtil.contaminate(living, com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                        com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, 5F);
            }
        }
    }

    private int findGroundY() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(getX()), Mth.ceil(getY()), Mth.floor(getZ()));
        // The old EntityUFO beam searched down to y=0, even in dimensions
        // whose modern build height extends below zero.
        for (int y = Mth.ceil(getY()); y >= 0; y--) {
            pos.setY(y);
            if (!level().getBlockState(pos).isAir()) return y;
        }
        return 0;
    }

    private void fireLaser(Entity target) {
        if (target == null) return;
        Vec3 horizontal = new Vec3(getX() - target.getX(), 0.0D, getZ() - target.getZ());
        if (horizontal.lengthSqr() < 0.001D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        double angle = Math.toRadians(-80 + random.nextInt(160));
        horizontal = new Vec3(horizontal.x * Math.cos(angle) - horizontal.z * Math.sin(angle), 0.0D,
                horizontal.x * Math.sin(angle) + horizontal.z * Math.cos(angle)).normalize();
        Vec3 origin = position().subtract(horizontal.scale(10.0D)).add(0.0D, 0.5D, 0.0D);
        Vec3 direction = new Vec3(target.getX() - origin.x,
                target.getY() + target.getBbHeight() * 0.5D - origin.y,
                target.getZ() - origin.z);
        level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin, direction,
                LegacyBossProjectileEntity.Type.UFO_LASER, target, 0.02F));
        level().playSound(null, blockPosition(), HbmSoundEvents.WEAPON_BALLS_LASER.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
    }

    private void fireRocket(Entity target) {
        if (target == null) return;
        Vec3 origin = position().add(0.0D, -0.5D, 0.0D);
        Vec3 direction = new Vec3(target.getX() - getX(),
                target.getY() + target.getBbHeight() * 0.5D - getY() - 0.5D,
                target.getZ() - getZ());
        level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin,
                direction, LegacyBossProjectileEntity.Type.UFO_ROCKET, target, 0.02F));
        level().playSound(null, blockPosition(), HbmSoundEvents.TURRET_RICHARD_FIRE.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
    }

    /** EntityUFO: a random secondary is fired at; a dead pick is only removed (no shot); the target is the fallback. */
    private void fireAtSecondaryOr(Entity fallback, boolean laser) {
        if (!secondaryTargets.isEmpty() && level() instanceof ServerLevel serverLevel) {
            UUID pick = secondaryTargets.get(random.nextInt(secondaryTargets.size()));
            Entity selected = serverLevel.getEntity(pick);
            if (selected == null || !selected.isAlive()) {
                secondaryTargets.remove(pick);
            } else if (laser) {
                fireLaser(selected);
            } else {
                fireRocket(selected);
            }
        } else if (laser) {
            fireLaser(fallback);
        } else {
            fireRocket(fallback);
        }
    }

    private Entity target() {
        return targetUuid == null || !(level() instanceof ServerLevel serverLevel) ? null : serverLevel.getEntity(targetUuid);
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D) return;
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) -Math.toDegrees(Math.atan2(motion.y, horizontal)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public void die(DamageSource source) {
        entityData.set(BEAM, false);
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        if (beamActive()) entityData.set(BEAM, false);
        // motionY -= 0.05 per tick on top of EntityFlying's 0.91 drag (see travel)
        setDeltaMovement(getDeltaMovement().add(0.0D, -0.05D, 0.0D));
        if (deathTime == -10) {
            level().playSound(null, blockPosition(), HbmSoundEvents.ENTITY_CHOPPER_DAMAGE.get(),
                    SoundSource.HOSTILE, 10.0F, 1.0F);
        }
        if (deathTime == 19 && !crashResolved && level() instanceof ServerLevel serverLevel) {
            crashResolved = true;
            LegacyProjectileUtil.standardExplosion(this, position(), 10.0F, 1.0F, true, true);
            LegacyMukeExplosion.detonateMediumMiniNuke(serverLevel, this, position());
            // boundingBox.expand(200, 200, 200)
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200.0D))) {
                HbmAdvancements.award(player, "boss_ufo");
                player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("coin_ufo"))));
            }
        }
        super.tickDeath();
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000.0D;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.atomiclegacy.entity_ufo");
    }
}

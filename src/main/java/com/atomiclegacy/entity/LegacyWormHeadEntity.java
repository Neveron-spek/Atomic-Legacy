package com.atomiclegacy.entity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** [parity A2: BOT prime] EntityBOTPrimeHead: 74 segments, burrowing pursuit, five lasers every 30 ticks, passive repair. */
public final class LegacyWormHeadEntity extends LegacyWormBaseEntity {
    public static final int SEGMENT_COUNT = 74;
    public static final double ATTACK_RANGE = 150.0D;
    public static final double MAX_SPEED = 1.0D;
    public static final double FALL_SPEED = 0.006D;
    public static final int SURFACE_Y = 60;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.atomiclegacy.entity_bot_prime_head"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos spawnPoint = BlockPos.ZERO;
    private int dmgCooldown;
    private int aggroCooldown;
    private int courseChangeCooldown;
    private boolean wasNearGround;
    private boolean segmentsCreated;

    public LegacyWormHeadEntity(EntityType<? extends LegacyWormHeadEntity> type, Level level) {
        super(type, level);
        xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15_000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestPlayerTargetGoal(this, 128.0D));
    }

    @Override
    public boolean isWormHead() {
        return true;
    }

    @Override
    protected float attackStrength(Entity target) {
        return 1000.0F;
    }

    public void createSegments(ServerLevel level) {
        if (segmentsCreated) return;
        segmentsCreated = true;
        headUuid = getUUID();
        spawnPoint = blockPosition();
        UUID previous = getUUID();
        for (int part = 0; part < SEGMENT_COUNT; part++) {
            LegacyWormBodyEntity body = new LegacyWormBodyEntity(HbmEntityTypes.LEGACY_WORM_BODY.get(), level);
            body.initialize(getUUID(), previous, part);
            // EntityBOTPrimeHead#onSpawnWithEgg places every body part at the floored head position.
            body.setPos(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ());
            level.addFreshEntity(body);
            previous = body.getUUID();
        }
    }

    /** EntityBOTPrimeHead#onSpawnWithEgg: floor the position, create the 74 parts, remember the wandering origin. */
    public void initializeLegacySpawn(ServerLevel level) {
        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());
        setPos(x, y, z);
        createSegments(level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (super.hurt(source, amount)) {
            dmgCooldown = 10;
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        float horizontal = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        setXRot((float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI));
        yRotO = getYRot();
        xRotO = getXRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        if (!level().isClientSide) {
            bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        }
    }

    /** EntityBOTPrimeHead#updateAITasks. */
    @Override
    protected void customServerAiStep() {
        if (headUuid == null) headUuid = getUUID();
        if (!segmentsCreated && level() instanceof ServerLevel server) createSegments(server);
        wormActionState();
        updateHeadMovement();
        if (getHealth() < getMaxHealth() && tickCount % 6 == 0) {
            if (targetedEntity != null) {
                heal(1.0F);
            } else if (lastHurtByPlayerTime == 0) {
                heal(4.0F);
            }
        }
        if (targetedEntity != null && targetedEntity.distanceToSqr(this) < ATTACK_RANGE * ATTACK_RANGE) {
            if (canSeeThroughNonSolids(targetedEntity)) {
                attackCounter++;
                if (attackCounter == 30) {
                    laserAttack(targetedEntity, true);
                    attackCounter = 0;
                }
            } else {
                attackCounter = 0;
            }
        } else {
            attackCounter = 0;
        }
    }

    private boolean isCourseTraversable() {
        return isInsideOpaqueBlock(); // canFly is false
    }

    /** EntityBOTPrimeHead#updateHeadMovement. */
    private void updateHeadMovement() {
        double deltaX = waypointX - getX();
        double deltaY = waypointY - getY();
        double deltaZ = waypointZ - getZ();
        double deltaSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        Vec3 motion = getDeltaMovement();
        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += random.nextInt(5) + 2;
            deltaSq = Math.sqrt(deltaSq);
            if (motion.lengthSqr() < MAX_SPEED && deltaSq > 0.0D) {
                if (!isCourseTraversable()) {
                    deltaSq *= 8.0D;
                }
                double moverSpeed = getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
                motion = motion.add(deltaX / deltaSq * moverSpeed, deltaY / deltaSq * moverSpeed, deltaZ / deltaSq * moverSpeed);
            }
        }
        if (!isCourseTraversable()) {
            motion = motion.add(0.0D, -FALL_SPEED, 0.0D);
        }
        setDeltaMovement(motion);
        if (dmgCooldown > 0) {
            dmgCooldown--;
        }
        aggroCooldown--;
        if (getTarget() != null) {
            if (aggroCooldown <= 0) {
                targetedEntity = getTarget();
                aggroCooldown = 20;
            }
        } else if (targetedEntity == null) {
            waypointX = spawnPoint.getX() - 50 + random.nextInt(100);
            waypointY = spawnPoint.getY() - 30 + random.nextInt(60);
            waypointZ = spawnPoint.getZ() - 50 + random.nextInt(100);
        }
        if (targetedEntity != null && targetedEntity.distanceToSqr(this) < ATTACK_RANGE * ATTACK_RANGE) {
            if (wasNearGround) {
                waypointX = targetedEntity.getX();
                waypointY = targetedEntity.getY();
                waypointZ = targetedEntity.getZ();
                if (random.nextInt(80) == 0 && getY() > SURFACE_Y && !isCourseTraversable()) {
                    wasNearGround = false;
                }
            } else {
                waypointX = targetedEntity.getX();
                waypointY = 10.0D;
                waypointZ = targetedEntity.getZ();
                if (getY() < 15.0D) {
                    wasNearGround = true;
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            Item coin = BuiltInRegistries.ITEM.get(AtomicLegacy.id("coin_worm"));
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200.0D))) {
                HbmAdvancements.award(player, "boss_worm");
                if (coin != net.minecraft.world.item.Items.AIR) {
                    player.getInventory().add(new ItemStack(coin));
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("spawn", spawnPoint.asLong());
        tag.putBoolean("nearGround", wasNearGround);
        tag.putBoolean("segmentsCreated", segmentsCreated);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spawnPoint = BlockPos.of(tag.getLong("spawn"));
        wasNearGround = tag.getBoolean("nearGround");
        segmentsCreated = tag.getBoolean("segmentsCreated");
        headUuid = getUUID();
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
        return Component.translatable("entity.atomiclegacy.entity_bot_prime_head");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityBOTPrimeBase sets renderDistanceWeight = 15; the head is 3 blocks wide (2,880-block radius).
        return distance < 8_294_400.0D;
    }
}

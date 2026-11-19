package com.atomiclegacy.entity;

import com.atomiclegacy.client.sound.C130ClientSounds;
import com.atomiclegacy.explosion.LegacyVnt;
import com.atomiclegacy.registry.HbmChunkTickets;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.HbmStructureLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/**
 * [parity W1: airdrop] 1.7.10 EntityC130 together with its EntityPlaneBase.
 * Server: moves by its motion each tick, 50 HP (synced, DataWatcher 17), crash
 * fall -0.025/tick with gas flames, ExplosionVNT(15).makeStandard() on contact,
 * removed after 200 ticks, forces its own chunk. At tick lifetime/2 = 100 with
 * HP > 0 the parachute crate is released 10 blocks below and 7 ticks of motion
 * behind the plane, filled from ItemPoolsC130.
 */
public final class LegacyC130Entity extends Entity {
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(LegacyC130Entity.class, EntityDataSerializers.FLOAT);

    /** EntityPlaneBase#getMaxHealth / #getLifetime. */
    public static final float MAX_HEALTH = 50.0F;
    public static final int LIFETIME = 200;
    /** TrackerUtil.setTrackingRange(world, c130, 250) in XFactory40mm#spawnPlane. */
    public static final int FLARE_TRACKING_RANGE = 250;

    public enum PayloadType { SUPPLIES, WEAPONS, A_FUCKING_FUEL_TRUCK }

    public PayloadType payload = PayloadType.SUPPLIES;
    /** EntityPlaneBase#health: server-side field, mirrored into HEALTH every tick (never restored from NBT). */
    public float health = MAX_HEALTH;
    private final int timer = LIFETIME;
    /** Tracker-entry override (not persisted, like EntityTrackerEntry#blocksDistanceThreshold); 0 = registration range. */
    private int trackingRangeOverride;

    // EntityPlaneBase client interpolation (setPositionAndRotation2 with turnProgress).
    private int turnProgress;
    private double syncPosX;
    private double syncPosY;
    private double syncPosZ;
    private double syncYaw;
    private double syncPitch;

    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;

    public LegacyC130Entity(EntityType<? extends LegacyC130Entity> type, Level level) {
        super(type, level);
    }

    public LegacyC130Entity(Level level) {
        this(HbmEntityTypes.LEGACY_C130.get(), level);
    }

    /** EntityC130#fac: random horizontal heading of speed 2, starting 100 ticks before the target, 100 blocks up. */
    public void fac(Level level, double x, double y, double z, PayloadType payload) {
        Vec3 vector = new Vec3(level.random.nextDouble() - 0.5D, 0.0D, level.random.nextDouble() - 0.5D).normalize();
        double vx = vector.x * 2.0D;
        double vz = vector.z * 2.0D;
        this.payload = payload;
        this.moveTo(x - vx * 100.0D, y + 100.0D, z - vz * 100.0D, 0.0F, 0.0F);
        this.setDeltaMovement(vx, 0.0D, vz);
        this.rotation();
    }

    /** WorldUtil.loadAndSpawnEntityInWorld: loads the 5x5 chunks around the entity, then spawns it. */
    public static void loadAndSpawn(ServerLevel level, Entity entity) {
        int chunkX = Mth.floor(entity.getX() / 16.0D);
        int chunkZ = Mth.floor(entity.getZ() / 16.0D);
        int loadRadius = 2;
        for (int k = chunkX - loadRadius; k <= chunkX + loadRadius; ++k) {
            for (int l = chunkZ - loadRadius; l <= chunkZ + loadRadius; ++l) {
                level.getChunk(k, l, ChunkStatus.FULL, true);
            }
        }
        if (entity instanceof LegacyC130Entity plane) {
            // EntityPlaneBase#init: the ForgeChunkManager entity ticket forces a chunk from construction on.
            plane.updateForcedChunk();
        }
        level.addFreshEntity(entity);
    }

    public void setTrackingRange(int range) {
        this.trackingRangeOverride = range;
    }

    public int trackingRangeOverride() {
        return trackingRangeOverride;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        // 1.7.10 EntityTrackerEntry: |dx| <= threshold && |dz| <= threshold (square), then chunk-watching.
        if (trackingRangeOverride > 0) {
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            if (dx < -trackingRangeOverride || dx > trackingRangeOverride
                    || dz < -trackingRangeOverride || dz > trackingRangeOverride) {
                return false;
            }
        }
        return super.broadcastToPlayer(player);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, MAX_HEALTH);
    }

    public float syncedHealth() {
        return entityData.get(HEALTH);
    }

    public boolean isFlying() {
        return entityData.get(HEALTH) > 0.0F;
    }

    @Override
    public boolean isPickable() {
        return this.health > 0.0F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(HbmDamageTypes.NUCLEAR_BLAST)) return false;
        if (this.isInvulnerable()) return false;
        if (!this.isRemoved() && !this.level().isClientSide && this.health > 0.0F) {
            health -= amount;
            if (this.health <= 0.0F) this.killPlane();
        }
        return true;
    }

    /** EntityPlaneBase#killPlane. */
    private void killPlane() {
        LegacyProjectileUtil.sendSmallExplosionEffect(level(), position(), 25, 3.5F, 2.0F);
        level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_PLANE_SHOT_DOWN.get(),
                SoundSource.HOSTILE, 25.0F, 1.0F);
    }

    @Override
    public void tick() {
        // EntityPlaneBase#onUpdate does not call Entity#onUpdate.
        if (!level().isClientSide) {
            entityData.set(HEALTH, health);
        } else {
            health = entityData.get(HEALTH);
        }

        if (level().isClientSide) {
            if (this.turnProgress > 0) {
                double interpX = getX() + (this.syncPosX - getX()) / (double) this.turnProgress;
                double interpY = getY() + (this.syncPosY - getY()) / (double) this.turnProgress;
                double interpZ = getZ() + (this.syncPosZ - getZ()) / (double) this.turnProgress;
                double d = Mth.wrapDegrees(this.syncYaw - (double) getYRot());
                setYRot((float) ((double) getYRot() + d / (double) this.turnProgress));
                setXRot((float) ((double) getXRot() + (this.syncPitch - (double) getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                setPos(interpX, interpY, interpZ);
            } else {
                setPos(getX(), getY(), getZ());
            }
        } else {
            Vec3 motion = getDeltaMovement();
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

            this.rotation();

            if (this.health <= 0.0F) {
                motion = motion.add(0.0D, -0.025D, 0.0D);
                setDeltaMovement(motion);
                for (int i = 0; i < 10; i++) {
                    spawnGasFlame((ServerLevel) level(),
                            getX() + random.nextGaussian() * 0.5D - motion.x * 2.0D,
                            getY() + random.nextGaussian() * 0.5D - motion.y * 2.0D,
                            getZ() + random.nextGaussian() * 0.5D - motion.z * 2.0D,
                            0.0D, 0.1D, 0.0D);
                }
                BlockPos block = new BlockPos((int) getX(), (int) getY(), (int) getZ());
                if (!level().getBlockState(block).isAir() || getY() < 0.0D) {
                    this.discard();
                    LegacyVnt.at((ServerLevel) level(), getX(), getY(), getZ(), 15.0F).makeStandard().explode();
                    level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.ENTITY_PLANE_CRASH.get(),
                            SoundSource.HOSTILE, 25.0F, 1.0F);
                    return;
                }
            } else {
                setDeltaMovement(motion.x, 0.0D, motion.z);
            }

            if (this.tickCount > timer) this.discard();
            if (!this.isRemoved()) updateForcedChunk();
        }

        // EntityC130#onUpdate
        if (level().isClientSide) {
            C130ClientSounds.tick(this);
        }

        if (!level().isClientSide && this.tickCount == LIFETIME / 2 && this.health > 0.0F) {
            Vec3 motion = getDeltaMovement();
            LegacyParachuteCrateEntity crate = new LegacyParachuteCrateEntity(level());
            crate.setPos(getX() - motion.x * 7.0D, getY() - 10.0D, getZ() - motion.z * 7.0D);

            if (this.payload == PayloadType.SUPPLIES) {
                for (int i = 0; i < 5; i++) crate.items.add(HbmStructureLoot.randomStack("POOL_SUPPLIES", this.random));
            }
            if (this.payload == PayloadType.WEAPONS) {
                int amount = 1 + random.nextInt(2);
                for (int i = 0; i < amount; i++) crate.items.add(HbmStructureLoot.randomStack("POOL_WEAPONS", this.random));
                for (int i = 0; i < 6; i++) crate.items.add(HbmStructureLoot.randomStack("POOL_AMMO", this.random));
            }

            level().addFreshEntity(crate);
        }
    }

    /** EntityPlaneBase#rotation. */
    private void rotation() {
        Vec3 motion = getDeltaMovement();
        float motionHorizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        setXRot((float) (Math.atan2(motion.y, motionHorizontal) * 180.0D / Math.PI) - 90.0F);
        while (getXRot() - this.xRotO < -180.0F) this.xRotO -= 360.0F;
        while (getXRot() - this.xRotO >= 180.0F) this.xRotO += 360.0F;
        while (getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
    }

    /** ParticleUtil.spawnGasFlame: AuxParticlePacketNT "gasfire" (scale 6.5) to players within 150 blocks. */
    private static void spawnGasFlame(ServerLevel level, double x, double y, double z, double mX, double mY, double mZ) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                HbmParticleTypes.GAS_FLARE_FLAME.get(), true, x, y, z, (float) mX, (float) mY, (float) mZ, 1.0F, 0);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= 150.0D * 150.0D) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        // EntityPlaneBase#setPositionAndRotation2
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yRot;
        this.syncPitch = xRot;
        this.turnProgress = steps;
    }

    @Override
    public double lerpTargetX() {
        return turnProgress > 0 ? syncPosX : getX();
    }

    @Override
    public double lerpTargetY() {
        return turnProgress > 0 ? syncPosY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return turnProgress > 0 ? syncPosZ : getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return turnProgress > 0 ? (float) syncPitch : getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return turnProgress > 0 ? (float) syncYaw : getYRot();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) { return true; }

    @Override
    public void remove(RemovalReason reason) {
        // EntityPlaneBase#setDead -> clearChunkLoader
        if (reason.shouldDestroy()) releaseForcedChunk();
        super.remove(reason);
    }

    private void updateForcedChunk() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        int chunkX = Mth.floor(getX() / 16.0D);
        int chunkZ = Mth.floor(getZ() / 16.0D);
        if (chunkX == forcedChunkX && chunkZ == forcedChunkZ) return;
        releaseForcedChunk();
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(serverLevel, this, chunkX, chunkZ, true, true);
        forcedChunkX = chunkX;
        forcedChunkZ = chunkZ;
    }

    private void releaseForcedChunk() {
        if (!(level() instanceof ServerLevel serverLevel) || forcedChunkX == Integer.MIN_VALUE) return;
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(serverLevel, this, forcedChunkX, forcedChunkZ, false, true);
        forcedChunkX = Integer.MIN_VALUE;
        forcedChunkZ = Integer.MIN_VALUE;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.tickCount = nbt.getInt("ticksExisted");
        this.entityData.set(HEALTH, nbt.getFloat("health"));
        PayloadType[] values = PayloadType.values();
        this.payload = values[Math.abs(nbt.getInt("payload") % values.length)];
        // Technical: the persisted NeoForge ticket must be released when the plane later moves on.
        if (nbt.contains("forcedChunkX")) {
            this.forcedChunkX = nbt.getInt("forcedChunkX");
            this.forcedChunkZ = nbt.getInt("forcedChunkZ");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("ticksExisted", this.tickCount);
        nbt.putFloat("health", this.entityData.get(HEALTH));
        nbt.putInt("payload", this.payload.ordinal());
        if (forcedChunkX != Integer.MIN_VALUE) {
            nbt.putInt("forcedChunkX", forcedChunkX);
            nbt.putInt("forcedChunkZ", forcedChunkZ);
        }
    }
}

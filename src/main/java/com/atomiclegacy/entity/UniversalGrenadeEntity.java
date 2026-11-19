package com.atomiclegacy.entity;

import com.atomiclegacy.items.UniversalGrenadeItem;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * [parity H2: grenade_universal] 1.7.10 {@code EntityGrenadeUniversal} (extends {@code EntityThrowableInterp} /
 * {@code EntityThrowableNT}): the server simulates the flight, the client only interpolates. The fuze and extra
 * lambdas of {@code ItemGrenadeFuze} / {@code ItemGrenadeExtra} are inlined here, the filling explosions are in
 * {@link UniversalGrenadeFillings}.
 */
public final class UniversalGrenadeEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(UniversalGrenadeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> BOUNCES =
            SynchedEntityData.defineId(UniversalGrenadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAIL =
            SynchedEntityData.defineId(UniversalGrenadeEntity.class, EntityDataSerializers.INT);

    public static final int TRAIL_TRIPLET = 1;

    /** Client render state of EntityGrenadeUniversal. */
    public double prevSpin;
    public double spin;

    /* EntityThrowableNT state */
    @Nullable
    private LivingEntity thrower;
    @Nullable
    private UUID throwerId;
    private BlockPos stuckPos = BlockPos.ZERO;
    private Block stuckBlock = Blocks.AIR;
    private boolean inGround;
    private int ticksInGround;
    private int ticksInAir;

    /* EntityThrowableInterp sync state (setPositionAndRotation2) */
    private int turnProgress;
    private double syncPosX;
    private double syncPosY;
    private double syncPosZ;
    private double syncYaw;
    private double syncPitch;

    public UniversalGrenadeEntity(EntityType<? extends UniversalGrenadeEntity> type, Level level) {
        super(type, level);
    }

    /** EntityGrenadeUniversal(world, grenade): dispensers, the FBI drone and triplex children. */
    public UniversalGrenadeEntity(Level level, ItemStack stack) {
        this(HbmEntityTypes.UNIVERSAL_GRENADE.get(), level);
        setGrenade(stack);
    }

    /** EntityGrenadeUniversal(world, thrower, grenade) */
    public UniversalGrenadeEntity(Level level, LivingEntity thrower, ItemStack stack) {
        this(level, stack);
        setThrower(thrower);
        // new Vec3NT(0.25, -0.25, 0).rotateAroundYDeg(-thrower.rotationYaw + 180)
        Vec3 offset = new Vec3(0.25D, -0.25D, 0.0D).yRot((float) Math.toRadians(-thrower.getYRot() + 180.0F));
        setPos(thrower.getX() + offset.x, thrower.getEyeY() + offset.y, thrower.getZ() + offset.z);
        Vec3 yeet = thrower.getLookAngle().normalize();
        setThrowableHeading(yeet.x, yeet.y, yeet.z, (float) shell().throwForce(), 0.0F);
    }

    public UniversalGrenadeEntity(Level level, ItemStack stack, @Nullable UUID ownerId, Vec3 motion) {
        this(level, stack);
        this.throwerId = ownerId;
        setDeltaMovement(motion);
    }

    /** Entity-owned throw used by the legacy FBI drone (EntityFBIDrone): new EntityGrenadeUniversal(world, stack) + setThrower. */
    public UniversalGrenadeEntity(Level level, Entity owner, ItemStack stack, Vec3 motion) {
        this(level, stack);
        if (owner instanceof LivingEntity living) setThrower(living);
        setDeltaMovement(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GRENADE, ItemStack.EMPTY);
        builder.define(BOUNCES, 0);
        builder.define(TRAIL, 0);
    }

    public UniversalGrenadeEntity setTrail(int trail) {
        entityData.set(TRAIL, trail);
        return this;
    }

    public ItemStack grenadeStack() { return entityData.get(GRENADE); }
    public int getBounces() { return entityData.get(BOUNCES); }
    public int getTrail() { return entityData.get(TRAIL); }
    public UniversalGrenadeItem.Shell shell() { return UniversalGrenadeItem.shell(grenadeStack()); }
    public UniversalGrenadeItem.Filling filling() { return UniversalGrenadeItem.filling(grenadeStack()); }
    public UniversalGrenadeItem.Fuze fuze() { return UniversalGrenadeItem.fuze(grenadeStack()); }
    @Nullable public UniversalGrenadeItem.Extra extra() { return UniversalGrenadeItem.extra(grenadeStack()); }

    /** getTimer(): ticksInAir + ticksInGround */
    public int getTimer() { return this.ticksInAir + this.ticksInGround; }
    public boolean isStuck() { return this.inGround; }

    public void setThrower(@Nullable LivingEntity thrower) {
        this.thrower = thrower;
        this.throwerId = thrower == null ? null : thrower.getUUID();
    }

    @Nullable
    public LivingEntity getThrower() {
        if (this.thrower == null && this.throwerId != null && level() instanceof ServerLevel server
                && server.getEntity(this.throwerId) instanceof LivingEntity living) {
            this.thrower = living;
        }
        return this.thrower;
    }

    /** EntityThrowableNT#setThrowableHeading (headingForceMult 0.0075) */
    public void setThrowableHeading(double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        float throwLen = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= throwLen;
        motionY /= throwLen;
        motionZ /= throwLen;
        motionX += this.random.nextGaussian() * 0.0075D * (double) inaccuracy;
        motionY += this.random.nextGaussian() * 0.0075D * (double) inaccuracy;
        motionZ += this.random.nextGaussian() * 0.0075D * (double) inaccuracy;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        setDeltaMovement(motionX, motionY, motionZ);
        float hyp = Mth.sqrt((float) (motionX * motionX + motionZ * motionZ));
        setYRot((float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI));
        setXRot((float) (Math.atan2(motionY, hyp) * 180.0D / Math.PI));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.ticksInGround = 0;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yRot;
        this.syncPitch = xRot;
        this.turnProgress = steps;
    }

    @Override
    public void tick() {
        double lastX = getX();
        double lastZ = getZ();
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();

        if (level().isClientSide) {
            // EntityThrowableInterp: the client never simulates the flight
            if (this.turnProgress > 0) {
                double interpX = getX() + (this.syncPosX - getX()) / (double) this.turnProgress;
                double interpY = getY() + (this.syncPosY - getY()) / (double) this.turnProgress;
                double interpZ = getZ() + (this.syncPosZ - getZ()) / (double) this.turnProgress;
                double d = Mth.wrapDegrees(this.syncYaw - (double) getYRot());
                setYRot((float) ((double) getYRot() + d / (double) this.turnProgress));
                setXRot((float) ((double) getXRot() + (this.syncPitch - (double) getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                setPos(interpX, interpY, interpZ);
            }

            this.prevSpin = this.spin;
            if (getBounces() <= 0) {
                this.spin += 15;
            } else {
                double dx = lastX - getX();
                double dz = lastZ - getZ();
                this.spin += Math.min(15, Math.sqrt(dx * dx + dz * dz) * 50);
            }
            if (this.spin >= 360) {
                this.prevSpin -= 360;
                this.spin -= 360;
            }
            if (getTrail() == TRAIL_TRIPLET) {
                // effectNT "vanillaExt" / "flame" without motion
                level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (grenadeStack().isEmpty()) {
            discard();
            return;
        }

        throwableTick();

        // EntityGrenadeUniversal#onUpdate: both lambdas run without a dead check, as in 1.7.10
        fuzeUpdateTick();
        extraUpdateTick();
    }

    /** EntityThrowableNT#onUpdate (server side) */
    private void throwableTick() {
        if (this.inGround) {
            if (level().getBlockState(this.stuckPos).getBlock() == this.stuckBlock) {
                ++this.ticksInGround;
                if (this.ticksInGround == 1200) { // groundDespawn()
                    discard();
                }
                return;
            }
            this.inGround = false;
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * (double) (this.random.nextFloat() * 0.2F),
                    motion.y * (double) (this.random.nextFloat() * 0.2F),
                    motion.z * (double) (this.random.nextFloat() * 0.2F));
            this.ticksInGround = 0;
            this.ticksInAir = 0;
            return;
        }

        ++this.ticksInAir;

        Vec3 motion = getDeltaMovement();
        Vec3 pos = position();
        Vec3 nextPos = pos.add(motion);
        // World.func_147447_a(pos, nextPos, false, true, false): no liquids, blocks without collision box are ignored
        HitResult mop = null;
        BlockHitResult blockHit = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            mop = blockHit;
            nextPos = blockHit.getLocation();
        }

        Entity hitEntity = null;
        Vec3 nonPenImpact = null;
        List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(motion).inflate(1.0D, 1.0D, 1.0D));
        double nearest = 0.0D;
        LivingEntity thrower = getThrower();
        for (Entity entity : list) {
            // selfDamageDelay() = 5
            if (entity.isPickable() && (entity != thrower || this.ticksInAir >= 5) && entity.isAlive()) {
                double hitbox = 0.3F;
                AABB aabb = entity.getBoundingBox().inflate(hitbox, hitbox, hitbox);
                Vec3 hitVec = aabb.clip(pos, nextPos).orElse(null);
                if (hitVec != null) {
                    double dist = pos.distanceTo(hitVec);
                    if (dist < nearest || nearest == 0.0D) {
                        hitEntity = entity;
                        nearest = dist;
                        nonPenImpact = hitVec;
                    }
                }
            }
        }
        if (hitEntity != null) {
            mop = new EntityHitResult(hitEntity, nonPenImpact);
        }

        if (mop != null) {
            onImpact(mop);
        }

        motion = getDeltaMovement();
        float hyp = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, hyp) * 180.0D / Math.PI);
        while (pitch - this.xRotO < -180.0F) this.xRotO -= 360.0F;
        while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
        while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;
        setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
        setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);

        // fullBlockCollisions() is false: plain displacement
        double x = getX() + motion.x;
        double y = getY() + motion.y;
        double z = getZ() + motion.z;

        float drag = isInWater() ? 0.8F : 0.99F; // getWaterDrag() / getAirDrag()
        setDeltaMovement(motion.x * (double) drag, motion.y * (double) drag - 0.03D, motion.z * (double) drag); // getGravityVelocity() = 0.03
        setPos(x, y, z);
    }

    /** EntityGrenadeUniversal#onImpact */
    private void onImpact(HitResult mop) {
        // ItemGrenadeFuze.FUZE_IMPACT: on block/entity impact, 0.5s safety
        if (fuze() == UniversalGrenadeItem.Fuze.IMPACT && getTimer() >= 10) {
            setPos(mop.getLocation());
            explode();
        }
        // ItemGrenadeExtra.EXTRA_GLUE
        if (extra() == UniversalGrenadeItem.Extra.GLUE && mop instanceof BlockHitResult block) {
            setPos(mop.getLocation());
            getStuck(block.getBlockPos());
        }

        if (isRemoved()) return; // we assume the grenade has gone off by this point

        if (mop instanceof BlockHitResult block) {
            Direction dir = block.getDirection();
            Vec3 hit = mop.getLocation();
            setPos(hit.x + dir.getStepX() * 0.05, hit.y + dir.getStepY() * 0.05, hit.z + dir.getStepZ() * 0.05);
            double bounce = shell().bounce();
            Vec3 motion = getDeltaMovement();
            if (motion.length() > 0.2) {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundEvents.WEAPON_GRENADE_BOUNCE_UNIVERSAL.get(), SoundSource.BLOCKS, 1F, 1F);
            }
            setDeltaMovement(
                    dir.getStepX() != 0 ? motion.x * -bounce : motion.x * 0.8,
                    dir.getStepY() != 0 ? motion.y * -bounce : motion.y * 0.8,
                    dir.getStepZ() != 0 ? motion.z * -bounce : motion.z * 0.8);
            sendTeleport();
            entityData.set(BOUNCES, getBounces() + 1);
        }
    }

    /** ItemGrenadeFuze: FUZE_3S / FUZE_7S / FUZE_15S / FUZE_AIRBURST */
    private void fuzeUpdateTick() {
        switch (fuze()) {
            case S3 -> { if (getTimer() >= 60) explode(); }
            case S7 -> { if (getTimer() >= 140) explode(); }
            case S15 -> { if (getTimer() >= 300) explode(); }
            case IMPACT -> { }
            case AIRBURST -> {
                // 30 ticks of safety, then explodes with a block within 10 blocks below
                if (getTimer() >= 30) {
                    // func_147447_a(start, end, false, false, true): blocks without collision box count, liquids do not
                    BlockHitResult mop = level().clip(new ClipContext(position(), position().add(0, -10, 0),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
                    if (mop.getType() == HitResult.Type.BLOCK) explode();
                }
            }
        }
    }

    /** ItemGrenadeExtra.EXTRA_PROXY: additional 10m EntityLivingBase triggered fuze */
    private void extraUpdateTick() {
        if (extra() != UniversalGrenadeItem.Extra.PROXY_FUZE) return;
        if (getTimer() >= 10 && getTimer() % 3 == 0) {
            List<LivingEntity> living = level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(10, 10, 10));
            for (LivingEntity e : living) {
                if (e == getThrower()) continue;
                if (e.distanceTo(this) <= 10) {
                    explode();
                    return;
                }
            }
        }
    }

    /** EntityThrowableNT#getStuck */
    private void getStuck(BlockPos pos) {
        this.stuckPos = pos.immutable();
        this.stuckBlock = level().getBlockState(pos).getBlock();
        this.inGround = true;
        setDeltaMovement(Vec3.ZERO);
        sendTeleport();
    }

    /** TrackerUtil.sendTeleport */
    private void sendTeleport() {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            server.getChunkSource().broadcast(this, new ClientboundTeleportEntityPacket(this));
        }
    }

    /** EntityGrenadeUniversal#explode: no guard against a second call, as in 1.7.10. */
    public void explode() {
        discard();
        if (!(level() instanceof ServerLevel server)) return;
        UniversalGrenadeFillings.explode(this, server);
        UniversalGrenadeFillings.extraExplode(this, server);
    }

    private void setGrenade(ItemStack stack) {
        entityData.set(GRENADE, stack.copyWithCount(1));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!grenadeStack().isEmpty()) tag.put("grenade", grenadeStack().save(registryAccess()));
        tag.putInt("bounces", getBounces());
        tag.putInt("trail", getTrail());
        tag.putLong("stuck_pos", this.stuckPos.asLong());
        tag.putBoolean("inGround", this.inGround);
        tag.putInt("ticksInGround", this.ticksInGround);
        tag.putInt("ticksInAir", this.ticksInAir);
        if (this.throwerId != null) tag.putUUID("owner", this.throwerId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setGrenade(ItemStack.parseOptional(registryAccess(), tag.getCompound("grenade")));
        entityData.set(BOUNCES, tag.getInt("bounces"));
        entityData.set(TRAIL, tag.getInt("trail"));
        this.stuckPos = BlockPos.of(tag.getLong("stuck_pos"));
        this.inGround = tag.getBoolean("inGround");
        this.stuckBlock = this.inGround ? level().getBlockState(this.stuckPos).getBlock() : Blocks.AIR;
        this.ticksInGround = tag.getInt("ticksInGround");
        this.ticksInAir = tag.contains("ticksInAir") ? tag.getInt("ticksInAir") : tag.getInt("timer");
        this.throwerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        this.thrower = null;
    }

    /** isInRangeToRenderDist: average edge length (0.25) * 4 * 64 */
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 4096.0D; }
}

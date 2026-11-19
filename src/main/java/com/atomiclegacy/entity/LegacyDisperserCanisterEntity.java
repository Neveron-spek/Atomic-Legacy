package com.atomiclegacy.entity;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Direct EntityDisperserCanister port: impact creates a 10x5, 80-tick mist. */
public final class LegacyDisperserCanisterEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Integer> FLUID_ID =
            SynchedEntityData.defineId(LegacyDisperserCanisterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GLYPHID_GLAND =
            SynchedEntityData.defineId(LegacyDisperserCanisterEntity.class, EntityDataSerializers.BOOLEAN);
    private UUID ownerId;
    private int ticksInAir;

    public LegacyDisperserCanisterEntity(EntityType<? extends LegacyDisperserCanisterEntity> type, Level level) {
        super(type, level);
    }

    public LegacyDisperserCanisterEntity(Level level, LivingEntity owner, HbmFluidDefinition fluid, boolean gland) {
        this(HbmEntityTypes.DISPERSER_CANISTER.get(), level);
        this.ownerId = owner.getUUID();
        entityData.set(FLUID_ID, fluid.oldId());
        entityData.set(GLYPHID_GLAND, gland);
        // [parity H2: disperser_canister] EntityThrowable(world, thrower): eye position moved 0.16 to the right of
        // the view yaw and 0.1 down; setThrowableHeading(..., 1.5F, 1.0F) adds the 0.0075 Gaussian spread before
        // the velocity is applied.
        float yawRad = owner.getYRot() / 180.0F * (float) Math.PI;
        float pitchRad = owner.getXRot() / 180.0F * (float) Math.PI;
        setPos(owner.getX() - (double) (Mth.cos(yawRad) * 0.16F), owner.getEyeY() - 0.10000000149011612D,
                owner.getZ() - (double) (Mth.sin(yawRad) * 0.16F));
        Vec3 heading = new Vec3(-Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad), Mth.cos(yawRad) * Mth.cos(pitchRad)).normalize();
        Vec3 motion = heading.add(
                random.nextGaussian() * 0.007499999832361937D,
                random.nextGaussian() * 0.007499999832361937D,
                random.nextGaussian() * 0.007499999832361937D).scale(1.5D);
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FLUID_ID, 0);
        builder.define(GLYPHID_GLAND, false);
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        ticksInAir++;
        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(motion);
        // [parity H2: disperser_canister] EntityThrowable#onUpdate: world.rayTraceBlocks(start, end) also stops at
        // blocks without a collision box (tall grass), liquids are ignored
        HitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = level().isClientSide ? null : findEntityHit(start, entityEnd);
        if (entityHit != null || blockHit.getType() != HitResult.Type.MISS) {
            if (!level().isClientSide) {
                if (entityHit != null) {
                    // EntityGrenadeBase#onImpact: attackEntityFrom(causeThrownDamage(this, thrower), 0)
                    entityHit.getEntity().hurt(damageSources().thrown(this, owner()), 0.0F);
                }
                detonate();
            }
            return;
        }
        // plain displacement: the vanilla throwable never used block collisions
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation(motion);
        double drag = isInWater() ? 0.8D : 0.99D;
        setDeltaMovement(motion.scale(drag).add(0.0D, -0.03D, 0.0D));
    }

    /** EntityDisperserCanister#explode: the mist appears at the canister, not at the hit vector. */
    private void detonate() {
        HbmFluidDefinition fluid = HbmFluids.byOldId(entityData.get(FLUID_ID)).orElse(HbmFluids.none());
        if (!fluid.isNone()) {
            level().addFreshEntity(new LegacyMistEntity(
                    level(), getX(), getY(), getZ(), fluid, 10.0F, 5.0F, 80));
        }
        discard();
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        Entity owner = owner();
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        // canBeCollidedWith() && (entity != thrower || ticksInAir >= 5), hit box grown by 0.3
        return level().getEntities(this, area, entity -> entity.isPickable()
                        && (entity != owner || ticksInAir >= 5))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.3D).clip(start, end)
                        .map(point -> new EntityHitResult(entity, point)))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())))
                .orElse(null);
    }

    private Entity owner() {
        return ownerId != null && level() instanceof net.minecraft.server.level.ServerLevel server
                ? server.getEntity(ownerId) : null;
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        yRotO = getYRot();
        xRotO = getXRot();
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) Math.toDegrees(Math.atan2(motion.y, horizontal)));
    }

    @Override
    public ItemStack getItem() {
        HbmFluidDefinition fluid = HbmFluids.byOldId(entityData.get(FLUID_ID)).orElse(HbmFluids.none());
        return HbmFluidContainerItem.makeFull(
                entityData.get(GLYPHID_GLAND) ? HbmItems.GLYPHID_GLAND::get : HbmItems.DISPERSER_CANISTER::get,
                fluid);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("fluid", entityData.get(FLUID_ID));
        tag.putBoolean("gland", entityData.get(GLYPHID_GLAND));
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(FLUID_ID, tag.getInt("fluid"));
        entityData.set(GLYPHID_GLAND, tag.getBoolean("gland"));
        ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }
}

package com.atomiclegacy.train;

import org.jetbrains.annotations.Nullable;

import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] EntityRailCarRidable.SeatDummyEntity: dynamic passenger seat (0.5 x 0.1) that moves and rotates
 * with the train; the rider is placed from the train's render position to avoid tick-order lag.
 */
public class SeatDummyEntity extends Entity {
    private static final EntityDataAccessor<Integer> TRAIN_ID = SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEAT_INDEX = SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);

    private int turnProgress;
    private double trainX;
    private double trainY;
    private double trainZ;
    @Nullable
    public RailCarRidableEntity train;

    public SeatDummyEntity(EntityType<? extends SeatDummyEntity> type, Level world) {
        super(type, world);
    }

    public SeatDummyEntity(Level world, @Nullable RailCarRidableEntity train, int index) {
        this(HbmEntityTypes.TRAIN_SEAT_DUMMY.get(), world);
        this.train = train;
        if (train != null) this.entityData.set(TRAIN_ID, train.getId());
        this.entityData.set(SEAT_INDEX, index);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIN_ID, 0);
        builder.define(SEAT_INDEX, 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) { }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.discard();
    }

    @Override
    public void tick() {
        this.firstTick = false;
        if (!level().isClientSide) {
            if (this.train == null || this.train.isRemoved()) {
                this.discard();
            }
        } else {
            if (this.turnProgress > 0) {
                double x = this.getX() + (this.trainX - this.getX()) / (double) this.turnProgress;
                double y = this.getY() + (this.trainY - this.getY()) / (double) this.turnProgress;
                double z = this.getZ() + (this.trainZ - this.getZ()) / (double) this.turnProgress;
                --this.turnProgress;
                this.setPos(x, y, z);
            }
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.trainX = x;
        this.trainY = y;
        this.trainZ = z;
        this.turnProgress = steps + 2;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
    }

    /** updateRiderPosition */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        if (train == null) {
            Entity entity = level().getEntity(this.entityData.get(TRAIN_ID));
            if (entity instanceof RailCarRidableEntity ridable) {
                train = ridable;
            }
        }

        //fallback for when train is null
        if (train == null) {
            callback.accept(passenger, getX(), getY() + 1 - RailCarRidableEntity.riderEyeOffset(passenger), getZ());
            return;
        }

        int index = this.entityData.get(SEAT_INDEX);
        LegacyVec3 rot = this.train.getPassengerSeats()[index];
        rot.rotateAroundX((float) (train.getXRot() * Math.PI / 180));
        rot.rotateAroundY((float) (-train.getYRot() * Math.PI / 180));
        double x = train.renderX + rot.xCoord;
        double y = train.renderY + rot.yCoord;
        double z = train.renderZ + rot.zCoord;
        callback.accept(passenger, x, y - RailCarRidableEntity.riderEyeOffset(passenger), z);
    }
}

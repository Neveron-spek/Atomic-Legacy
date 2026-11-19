package com.atomiclegacy.train;

import org.jetbrains.annotations.Nullable;

import com.atomiclegacy.registry.HbmEntityTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * [parity S1: trains] EntityRailCarBase.BoundingBoxDummyEntity: invisible entities that make up the dynamic bounding
 * structure of the train, forwarding attacks and clicks to it. Never saved (writeToNBTOptional false).
 */
public class BoundingBoxDummyEntity extends Entity {
    private static final EntityDataAccessor<Integer> TRAIN_ID = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);

    private int turnProgress;
    private double trainX;
    private double trainY;
    private double trainZ;
    @Nullable
    public RailCarEntity train;

    public BoundingBoxDummyEntity(EntityType<? extends BoundingBoxDummyEntity> type, Level world) {
        super(type, world);
    }

    public BoundingBoxDummyEntity(Level world, @Nullable RailCarEntity train, float width, float height) {
        this(HbmEntityTypes.TRAIN_BOUNDING_DUMMY.get(), world);
        this.setDummySize(width, height);
        this.train = train;
        if (train != null) this.entityData.set(TRAIN_ID, train.getId());
    }

    public void setDummySize(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WIDTH.equals(key) || HEIGHT.equals(key)) this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIN_ID, 0);
        builder.define(WIDTH, 1F);
        builder.define(HEIGHT, 1F);
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
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (train != null) return train.hurt(source, amount);
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // 1.7.10: the client dummy has no train reference and falls back to Entity.interactFirst (false)
        if (train != null) return train.interactFirst(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        return InteractionResult.PASS;
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

    @Nullable
    public Entity trainEntity() {
        return train != null ? train : level().getEntity(this.entityData.get(TRAIN_ID));
    }
}

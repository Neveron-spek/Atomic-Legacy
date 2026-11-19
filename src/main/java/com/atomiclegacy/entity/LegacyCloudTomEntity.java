package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** [parity d2] 1.7.10 EntityCloudTom (entity_moonstone_blast): the expanding blast ring of the Tom impact. */
public final class LegacyCloudTomEntity extends Entity {
    /** EntityTom: new EntityCloudTom(worldObj, 500). */
    public static final int TOM_MAX_AGE = 500;
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(LegacyCloudTomEntity.class, EntityDataSerializers.INT);

    /** Counted separately on each side, exactly like the unsynchronised field of the original. */
    public int age;

    public LegacyCloudTomEntity(EntityType<? extends LegacyCloudTomEntity> type, Level level) {
        super(type, level);
    }

    public static LegacyCloudTomEntity spawn(ServerLevel level, double x, double y, double z, int maxAge) {
        LegacyCloudTomEntity cloud = new LegacyCloudTomEntity(HbmEntityTypes.LEGACY_CLOUD_TOM.get(), level);
        cloud.setMaxAge(maxAge);
        cloud.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(cloud);
        return cloud;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 0);
    }

    @Override
    public void tick() {
        this.age++;
        this.level().setSkyFlashTime(2); // worldObj.lastLightningBolt = 2
        if (!this.level().isClientSide && this.age >= this.getMaxAge()) {
            this.discard();
        }
    }

    public void setMaxAge(int maxAge) {
        this.entityData.set(MAX_AGE, maxAge);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getShort("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) this.age);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}

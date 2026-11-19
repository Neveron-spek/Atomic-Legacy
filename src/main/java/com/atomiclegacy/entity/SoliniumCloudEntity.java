package com.atomiclegacy.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public final class SoliniumCloudEntity extends Entity {
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(SoliniumCloudEntity.class, EntityDataSerializers.LONG);
    // [parity M3: dfc] EntityCloudFleijaRainbow shares the 1.7.10 name "entity_cloud_rainbow" and the same
    // age/maxAge/lightning logic; only RenderCloudRainbow differs, so it is a flag on this entity.
    private static final EntityDataAccessor<Boolean> RAINBOW = SynchedEntityData.defineId(SoliniumCloudEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifetime;

    public SoliniumCloudEntity(EntityType<? extends SoliniumCloudEntity> type, Level level) {
        super(type, level);
        // [parity S4: culling] ignoreFrustumCheck: SoliniumCloudRenderer#shouldRender uses the finite sphere radius.
    }

    public void initialize(int radius) {
        lifetime = radius;
        entityData.set(START, level().getGameTime());
    }

    public void setRainbow(boolean rainbow) {
        entityData.set(RAINBOW, rainbow);
    }

    public boolean rainbow() {
        return entityData.get(RAINBOW);
    }

    public int age() {
        return (int) Math.max(0, level().getGameTime() - entityData.get(START));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START, 0L);
        builder.define(RAINBOW, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(getX(), getY() + 200, getZ());
                server.addFreshEntity(bolt);
            }
            if (age() >= lifetime) discard();
        }
    }

    /** [parity S4: culling] EntityCloudFleijaRainbow#isInRangeToRenderDist; EntityCloudSolinium keeps the vanilla rule. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return rainbow() ? distance < 25000 : super.shouldRenderAtSqrDistance(distance);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", age());
        tag.putInt("lifetime", lifetime);
        tag.putBoolean("rainbow", rainbow());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifetime = tag.getInt("lifetime");
        entityData.set(RAINBOW, tag.getBoolean("rainbow"));
        entityData.set(START, level().getGameTime() - tag.getInt("age"));
    }
}

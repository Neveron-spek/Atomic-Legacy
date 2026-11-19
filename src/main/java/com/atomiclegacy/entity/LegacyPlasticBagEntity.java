package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Direct 1.7.10 EntityPlasticBag movement and drop behaviour. */
public final class LegacyPlasticBagEntity extends WaterAnimal {
    private float rotation;
    private float previousRotation;
    private float randomMotionSpeed;
    private float rotationVelocity;
    private float randomMotionX;
    private float randomMotionY;
    private float randomMotionZ;

    public LegacyPlasticBagEntity(EntityType<? extends LegacyPlasticBagEntity> type, Level level) {
        super(type, level);
        rotationVelocity = 1.0F / (random.nextFloat() + 1.0F) * 0.2F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                // EntityPlasticBag leaves EntityWaterMob's inherited
                // attributes untouched in 1.7.10.
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void tick() {
        super.tick();

        previousRotation = rotation;
        rotation += rotationVelocity;
        if (rotation > Math.PI * 2.0F) {
            rotation -= Math.PI * 2.0F;
            if (random.nextInt(10) == 0) {
                rotationVelocity = 1.0F / (random.nextFloat() + 1.0F) * 0.2F;
            }
        }

        boolean inWater = isLegacyInWater();
        if (inWater) {
            float phase = rotation < Math.PI
                    ? rotation / (float) Math.PI
                    : 0.0F;
            if (phase > 0.75F) {
                randomMotionSpeed = 0.1F;
            } else if (rotation >= Math.PI) {
                randomMotionSpeed *= 0.999F;
            }
            if (!level().isClientSide) {
                setDeltaMovement(randomMotionX * randomMotionSpeed,
                        randomMotionY * randomMotionSpeed,
                        randomMotionZ * randomMotionSpeed);
            }
            Vec3 motion = getDeltaMovement();
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (horizontal > 1.0E-7D) {
                float desiredYaw = (float) (-Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
                float smoothedYaw = yBodyRot
                        + (float) ((desiredYaw - yBodyRot) * 0.1D);
                setYRot(smoothedYaw);
                yBodyRot = smoothedYaw;
                yHeadRot = smoothedYaw;
            }
            setXRot((float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI));
        } else if (!level().isClientSide) {
            setDeltaMovement(0.0D, (getDeltaMovement().y - 0.08D) * 0.98D, 0.0D);
        }

        updateRandomMotion(isInWater()); // [parity D1] updateEntityActionState reads the vanilla inWater field
    }

    @Override
    public void travel(Vec3 ignored) {
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isAlive()) {
            discard();
            LegacyBuoyantItemEntity drop = new LegacyBuoyantItemEntity(level(), getX(), getY(), getZ(),
                    new net.minecraft.world.item.ItemStack(HbmItems.PLASTIC_BAG.get()));
            drop.setPickUpDelay(10);
            level().addFreshEntity(drop);
        }
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        // [parity D1] the 1/10 roll of getCanSpawnHere is made once, by the registered spawn placement rule
        if (reason == MobSpawnType.NATURAL && (getY() <= 45.0D || getY() >= 63.0D)) {
            return false;
        }
        return super.checkSpawnRules(level, reason);
    }

    /** EntityPlasticBag#canTriggerWalking: the bag never activates block triggers. */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    private void updateRandomMotion(boolean inWater) {
        // EntityPlasticBag used EntityLiving.entityAge, which is the
        // modern mob's noActionTime counter (and is reset by the normal
        // nearby-player despawn check), rather than the immutable world
        // tickCount.
        if (noActionTime > 100) {
            randomMotionX = randomMotionY = randomMotionZ = 0.0F;
        } else if (random.nextInt(50) == 0 || !inWater
                || randomMotionX == 0.0F && randomMotionY == 0.0F && randomMotionZ == 0.0F) {
            float angle = random.nextFloat() * (float) Math.PI * 2.0F;
            randomMotionX = (float) Math.cos(angle) * 0.2F;
            randomMotionY = -0.1F + random.nextFloat() * 0.2F;
            randomMotionZ = (float) Math.sin(angle) * 0.2F;
        }
    }

    /**
     * [parity D1] EntityPlasticBag.isInWater(): World.handleMaterialAcceleration(boundingBox.expand(0, -0.6, 0),
     * water). The squid's 0.6 shrink turns the 0.45 high box inside out, so only the block layers
     * floor(minY + 0.6) .. floor(maxY - 0.6 + 1) - 1 are examined (none for most heights): the bag only "swims" in
     * a narrow band and sinks otherwise, exactly like the original.
     */
    private boolean isLegacyInWater() {
        AABB box = getBoundingBox();
        int minX = net.minecraft.util.Mth.floor(box.minX);
        int maxX = net.minecraft.util.Mth.floor(box.maxX + 1.0D);
        int minY = net.minecraft.util.Mth.floor(box.minY + 0.6D);
        int maxY = net.minecraft.util.Mth.floor(box.maxY - 0.6D + 1.0D);
        int minZ = net.minecraft.util.Mth.floor(box.minZ);
        int maxZ = net.minecraft.util.Mth.floor(box.maxZ + 1.0D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    net.minecraft.world.level.material.FluidState fluid = level().getFluidState(pos.set(x, y, z));
                    if (fluid.is(FluidTags.WATER) && maxY >= y + fluid.getHeight(level(), pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** EntityWaterMob.onEntityUpdate air logic, driven by the overridden isInWater() above. */
    @Override
    protected void handleAirSupply(int airSupply) {
        if (isAlive() && !isLegacyInWater()) {
            setAirSupply(airSupply - 1);
            if (getAirSupply() == -20) {
                setAirSupply(0);
                hurt(damageSources().drown(), 2.0F);
            }
        } else {
            setAirSupply(300);
        }
    }

}

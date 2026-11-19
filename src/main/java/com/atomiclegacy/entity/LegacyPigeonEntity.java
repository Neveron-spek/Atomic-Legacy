package com.atomiclegacy.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 EntityPigeon, with the vanilla animal flight-free movement base. */
public final class LegacyPigeonEntity extends PathfinderMob {
    private static final EntityDataAccessor<Byte> FAT = SynchedEntityData.defineId(
            LegacyPigeonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FLYING = SynchedEntityData.defineId(
            LegacyPigeonEntity.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float prevFallTime;
    public float dest;
    public float prevDest;
    public float offGroundTimer = 1.0F;

    public LegacyPigeonEntity(EntityType<? extends LegacyPigeonEntity> type, Level level) {
        super(type, level);
        moveControl = new com.atomiclegacy.entity.ai.LegacyMoveControl(this); // [parity K1] EntityMoveHelper (new AI)
        // EntityAISwimmingConditional#setCanSwim(true) enabled water
        // navigation for the legacy pigeon.
        getNavigation().setCanFloat(true);
        setNoGravity(false);
        xpReward = 0;
    }

    /** [parity K1] PathNavigate of the new-AI EntityPigeon uses the 1.7.10 path finder. */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.atomiclegacy.entity.ai.LegacyPathNavigation(this, level, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                // EntityPigeon never overrides EntityCreature attributes in
                // 1.7.10: EntityLivingBase registers maxHealth 20 and
                // movementSpeed 0.699999988079071 (no animal 0.25 override).
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.699999988079071D); // [parity P3: inherited 1.7.10 default]
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this) {
            @Override public boolean canUse() {
                return getFlyingState() == 0 && super.canUse();
            }
        });
        goalSelector.addGoal(2, new EatBreadGoal());
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.2D) {
            @Override public boolean canUse() {
                return getFlyingState() == 0 && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return getFlyingState() == 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FAT, (byte) 0);
        builder.define(FLYING, (byte) 0);
    }

    public boolean isFat() {
        return entityData.get(FAT) != 0;
    }

    public void setFat(boolean fat) {
        entityData.set(FAT, (byte) (fat ? 1 : 0));
    }

    public int getFlyingState() {
        return entityData.get(FLYING);
    }

    public void setFlyingState(int state) {
        entityData.set(FLYING, (byte) (state == 0 ? 0 : 1));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Fat", isFat());
        tag.putByte("Flying", (byte) getFlyingState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFat(tag.getBoolean("Fat"));
        setFlyingState(tag.getByte("Flying"));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            // [parity D1] EntityAIStartFlying / EntityAIStopFlying are tasks: EntityAITasks only tries to start
            // tasks on every third tick (tickRate 3), start before stop.
            if (legacyTaskTick++ % 3 == 0) {
                if (getFlyingState() == 0 && (getLastHurtByMob() != null || isOnFire() || random.nextInt(600) == 0)) {
                    setFlyingState(1);
                }
                if (getFlyingState() != 0 && random.nextInt(200) == 0) {
                    setFlyingState(0);
                }
            }
            // EntityPigeon's fat-flight action used ItemFertilizer to force
            // growth in the first viable block up to 25 blocks below it,
            // then had a 10% chance to lose its fat state.
            if (isFat() && getFlyingState() != 0 && random.nextInt(50) == 0
                    && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockPos origin = blockPosition().below();
                for (int i = 0; i < 25; i++) {
                    BlockPos target = origin.below(i);
                    if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), serverLevel, target)) {
                        serverLevel.levelEvent(2005, target, 0);
                        break;
                    }
                }
                // [parity D1] AuxParticlePacketNT "sweat": 3 white wool block-dust particles in the pigeon's box
                for (int i = 0; i < 3; i++) {
                    net.minecraft.world.phys.AABB box = getBoundingBox();
                    serverLevel.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                                    net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState()),
                            box.minX - 0.2D + (box.getXsize() + 0.4D) * random.nextDouble(),
                            box.minY + (box.getYsize() + 0.2D) * random.nextDouble(),
                            box.minZ - 0.2D + (box.getZsize() + 0.4D) * random.nextDouble(),
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                if (random.nextInt(10) == 0) {
                    setFat(false);
                }
            }
        }
        if (getFlyingState() != 0) {
            setNoGravity(false);
            int surface = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    getBlockX(), getBlockZ());
            boolean ceiling = getY() - surface > 10.0D;
            double motionY = random.nextGaussian() * 0.05D + (ceiling ? 0.0D : 0.04D)
                    + (isInWater() ? 0.2D : 0.0D);
            if (onGround()) motionY = Math.abs(motionY) + 0.1D;
            if (random.nextInt(20) == 0) setYRot(getYRot() + (float) random.nextGaussian() * 30.0F);
            // EntityPigeon sets moveForward=1.5; airborne LivingEntity travel
            // applies that input with the legacy 0.02 air acceleration.
            moveRelative(0.02F, new Vec3(0.0D, 0.0D, 1.5D));
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, motionY, motion.z);
        } else {
            setNoGravity(false);
            if (!onGround() && getDeltaMovement().y < 0.0D) {
                setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.8D, 1.0D));
            }
        }
        prevFallTime = fallTime;
        prevDest = dest;
        dest += (onGround() ? -1.0F : 4.0F) * 0.3F;
        dest = Math.max(0.0F, Math.min(1.0F, dest));
        if (!onGround() && offGroundTimer < 1.0F) {
            offGroundTimer = 1.0F;
        }
        offGroundTimer *= 0.9F;
        if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        }
        fallTime += offGroundTimer * 2.0F;
    }

    // [parity D1] EntityPigeon overrides neither fall() nor canTriggerWalking(): it takes ordinary fall damage
    // (fallDistance keeps growing through the whole flight) and triggers pressure plates like any creature.

    private int legacyTaskTick;

    /** EntityAIEatBread (mutex 3, speed 0.4): first bread item within 10 blocks, eaten at distance <= 1. */
    private final class EatBreadGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private ItemEntity item;

        private EatBreadGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isFat() || getFlyingState() != 0) return false;
            for (ItemEntity candidate : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10.0D))) {
                if (candidate.getItem().is(Items.BREAD)) {
                    item = candidate;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive() && canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(item, 30.0F, getMaxHeadXRot());
            if (distanceTo(item) > 1.0F) {
                getNavigation().moveTo(item, 0.4D);
                return;
            }
            if (random.nextInt(3) == 0) {
                // a stack larger than one is replaced by a new item entity of count - 1 at the same position
                ItemStack stack = item.getItem();
                if (stack.getCount() > 1) {
                    ItemEntity remainder = new ItemEntity(level(), item.getX(), item.getY(), item.getZ(),
                            stack.copyWithCount(stack.getCount() - 1));
                    level().addFreshEntity(remainder);
                }
                item.discard();
            }
            setFat(true);
            playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT,
                    0.5F + 0.5F * random.nextInt(2),
                    (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // 1.7.10 EntityPigeon used the chicken step sound at this exact
        // volume, rather than inheriting the generic animal step.
        playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (amount >= getMaxHealth() * 2.0F && !level().isClientSide) {
            discard();
            for (int i = 0; i < 10; i++) {
                // [parity D1] feather at pos + unit vector, motion = vector * 0.5
                Vec3 offset = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                ItemEntity feather = new ItemEntity(level(), getX() + offset.x, getY() + getBbHeight() / 2.0D + offset.y,
                        getZ() + offset.z, new ItemStack(Items.FEATHER));
                feather.setDeltaMovement(offset.scale(0.5D));
                level().addFreshEntity(feather);
            }
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        int looting = 0;
        if (getLastHurtByMob() != null && !getLastHurtByMob().getMainHandItem().isEmpty()) {
            looting = EnchantmentHelper.getItemEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING),
                    getLastHurtByMob().getMainHandItem());
        }
        int count = random.nextInt(3) + random.nextInt(1 + looting);
        for (int i = 0; i < count; i++) spawnAtLocation(Items.FEATHER);
        spawnAtLocation(isOnFire() ? Items.COOKED_CHICKEN : Items.CHICKEN, isFat() ? 3 : 1);
    }

}

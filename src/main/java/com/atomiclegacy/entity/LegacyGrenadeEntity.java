package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Direct port of EntityGrenadeBouncyGeneric. The old entity only collided with
 * blocks, reflected the impacted motion axis, and exploded after its item fuse.
 */
public final class LegacyGrenadeEntity extends Entity {
    private static final int FUSE_TICKS = 60;
    private static final float THROW_SPEED = 1.5F;
    private static final float BOUNCE_MODIFIER = 0.5F;
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(LegacyGrenadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(LegacyGrenadeEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;

    public LegacyGrenadeEntity(EntityType<? extends LegacyGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public LegacyGrenadeEntity(Level level, Player owner, Kind kind) {
        this(HbmEntityTypes.LEGACY_GRENADE.get(), level);
        setKind(kind);
        this.ownerId = owner.getUUID();
        setFuse(kind.fuseTicks());
        // [parity H2: stick_dynamite] EntityGrenadeBouncyBase(world, living): eye position moved 0.16 to the right
        // of the view yaw and 0.1 down; heading from yaw / pitch, setThrowableHeading(..., 1.5F, 1.0F) adds the
        // 0.0075 Gaussian spread before the velocity is applied; the pitch starts at 0.
        float yawRad = owner.getYRot() / 180.0F * (float) Math.PI;
        float pitchRad = owner.getXRot() / 180.0F * (float) Math.PI;
        setPos(owner.getX() - (double) (Mth.cos(yawRad) * 0.16F), owner.getEyeY() - 0.10000000149011612D,
                owner.getZ() - (double) (Mth.sin(yawRad) * 0.16F));
        Vec3 heading = new Vec3(-Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad), Mth.cos(yawRad) * Mth.cos(pitchRad)).normalize();
        Vec3 motion = heading.add(
                random.nextGaussian() * 0.007499999832361937D,
                random.nextGaussian() * 0.007499999832361937D,
                random.nextGaussian() * 0.007499999832361937D
        ).scale(THROW_SPEED);
        setDeltaMovement(motion);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        setYRot(yaw);
        this.yRotO = yaw;
        setXRot(0.0F);
        this.xRotO = 0.0F;
    }

    public LegacyGrenadeEntity(Level level, Kind kind, Vec3 position, Vec3 motion) {
        this(HbmEntityTypes.LEGACY_GRENADE.get(), level);
        setKind(kind);
        setFuse(kind.fuseTicks());
        setPos(position.x, position.y, position.z);
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    public Kind kind() {
        return Kind.byOrdinal(entityData.get(KIND));
    }

    public ItemStack renderStack() {
        return new ItemStack(kind() == Kind.FISHING ? HbmItems.STICK_DYNAMITE_FISHING.get() : HbmItems.STICK_DYNAMITE.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // The old entity synchronised its grenade ItemStack. Modern component
        // data carries the equivalent kind and fuse explicitly for both sides.
        builder.define(KIND, Kind.DYNAMITE.ordinal());
        builder.define(FUSE, FUSE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement();
        xRotO = getXRot();
        setXRot(getXRot() - (float) (motion.length() * 25.0D));
        HitResult hit = level().clip(new ClipContext(position(), position().add(motion),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean bounce = false;
        if (hit instanceof BlockHitResult blockHit) {
            Vec3 hitLocation = blockHit.getLocation();
            setPos(
                    getX() + (hitLocation.x - getX()) * 0.6D,
                    getY() + (hitLocation.y - getY()) * 0.6D,
                    getZ() + (hitLocation.z - getZ()) * 0.6D
            );
            motion = reflect(motion, blockHit.getDirection());
            bounce = true;
            if (!level().isClientSide && motion.length() > 0.05D) {
                level().playSound(null, blockPosition(), HbmSoundEvents.WEAPON_GRENADE_BOUNCE.get(),
                        SoundSource.PLAYERS, 2.0F, 1.0F);
            }
            motion = motion.scale(BOUNCE_MODIFIER);
        }
        if (!bounce) {
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        }

        updateRotation(motion);
        if (isInWater() && level().isClientSide) {
            for (int index = 0; index < 4; index++) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        getX() - motion.x * 0.25D, getY() - motion.y * 0.25D, getZ() - motion.z * 0.25D,
                        motion.x, motion.y, motion.z);
            }
        }
        setDeltaMovement(motion.scale(isInWater() ? 0.8D : 0.99D).add(0.0D, -0.03D, 0.0D));

        if (!level().isClientSide && setFuseAndCheck()) {
            detonate();
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (kind() == Kind.DYNAMITE) {
            // [parity H2: stick_dynamite] ItemGrenadeDynamite.explode: ExplosionVNT(5, thrower) with only
            // EntityProcessorCrossSmooth(1, 15), PlayerProcessorStandard and ExplosionEffectWeapon(10, 2.5F, 1F):
            // no block damage, at most 15 damage, the thrower takes half.
            UniversalGrenadeFillings.standardExplode(serverLevel, owner(), getX(), getY(), getZ(), 5.0F, 15.0F, 0.0F, 0.0F);
        } else {
            // [parity H2: stick_dynamite_fishing] ItemGrenadeFishing.explode: world.newExplosion(null, x, y + 0.25, z, 3F, false, false)
            level().explode(null, getX(), getY() + 0.25D, getZ(), 3.0F, false, Level.ExplosionInteraction.NONE);
            spawnFishingLoot(serverLevel);
        }
        discard();
    }

    private void spawnFishingLoot(ServerLevel level) {
        int iX = Mth.floor(getX());
        int iY = Mth.floor(getY());
        int iZ = Mth.floor(getZ());
        for (int index = 0; index < 15; index++) {
            BlockPos pos = new BlockPos(
                    iX + level.random.nextInt(15) - 7,
                    iY + level.random.nextInt(15) - 7,
                    iZ + level.random.nextInt(15) - 7
            );
            if (!level.getBlockState(pos).is(Blocks.WATER)) {
                continue;
            }
            // [parity H2: stick_dynamite_fishing] FishingHooks.getRandomFishable(rand, chance, luck 0, speed 100): the
            // speed removes the junk and treasure chances, so only the fish category (cod 60, salmon 25, clownfish 2,
            // pufferfish 13) remains - the vanilla fishing/fish table.
            LootParams.Builder parameters = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD));
            List<ItemStack> loot = level.getServer().reloadableRegistries()
                    .getLootTable(BuiltInLootTables.FISHING_FISH)
                    .getRandomItems(parameters.create(LootContextParamSets.FISHING));
            for (ItemStack stack : loot) {
                if (!stack.isEmpty()) {
                    // new EntityItemBuoyant(world, rX + 0.5, rY + 0.5, rZ + 0.5, loot) with motionY = 1
                    LegacyBuoyantItemEntity item = new LegacyBuoyantItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy());
                    item.setDeltaMovement(level.random.nextDouble() * 0.2D - 0.1D, 1.0D, level.random.nextDouble() * 0.2D - 0.1D);
                    level.addFreshEntity(item);
                    break;
                }
            }
        }
    }

    private LivingEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(ownerId) instanceof LivingEntity living ? living : null;
    }

    private static Vec3 reflect(Vec3 motion, Direction side) {
        return switch (side.getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        };
    }

    private void updateRotation(Vec3 motion) {
        float rawYaw = (float) Math.toDegrees(Math.atan2(motion.x, motion.z));
        while (rawYaw - yRotO < -180.0F) {
            yRotO -= 360.0F;
        }
        while (rawYaw - yRotO >= 180.0F) {
            yRotO += 360.0F;
        }
        setYRot(yRotO + (rawYaw - yRotO) * 0.2F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("kind", kind().id);
        tag.putInt("fuse", fuse());
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(Kind.byId(tag.getString("kind")));
        setFuse(tag.getInt("fuse"));
        ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    private int fuse() {
        return entityData.get(FUSE);
    }

    private void setFuse(int value) {
        entityData.set(FUSE, value);
    }

    private boolean setFuseAndCheck() {
        int next = fuse() - 1;
        setFuse(next);
        return next <= 0;
    }

    private void setKind(Kind value) {
        entityData.set(KIND, value.ordinal());
    }

    public enum Kind {
        DYNAMITE("dynamite", FUSE_TICKS),
        FISHING("fishing", FUSE_TICKS);

        private final String id;
        private final int fuseTicks;

        Kind(String id, int fuseTicks) {
            this.id = id;
            this.fuseTicks = fuseTicks;
        }

        private int fuseTicks() {
            return fuseTicks;
        }

        private static Kind byId(String id) {
            for (Kind value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return DYNAMITE;
        }

        private static Kind byOrdinal(int ordinal) {
            Kind[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DYNAMITE;
        }
    }
}

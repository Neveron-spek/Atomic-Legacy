package com.atomiclegacy.entity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.BroadcasterBlock;
import com.atomiclegacy.blocks.RadioRecBlock;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * [parity R1: FX entities] 1.7.10 EntityChlorineFX / EntityCloudFX / EntityPinkCloudFX / EntityOrangeFX
 * (EntityModFX subclasses registered as real entities "entity_chlorine_fx", "entity_cloud_fx",
 * "entity_pink_cloud_fx", "entity_agent_orange"). They move on both sides; the gas effects
 * (ExplosionChaos.poison / c / pc) and block changes run on the server.
 */
public class LegacyCloudFxEntity extends Entity {
    public enum Kind {
        CHLORINE(700, 101),
        CLOUD(900, 301),
        PINK(900, 301),
        ORANGE(900, 301);

        final int minAge;
        final int ageVariance;

        Kind(int minAge, int ageVariance) {
            this.minAge = minAge;
            this.ageVariance = ageVariance;
        }
    }

    private final Kind kind;
    public int particleAge;
    public int maxAge;

    public LegacyCloudFxEntity(EntityType<? extends LegacyCloudFxEntity> type, Level level) {
        super(type, level);
        this.kind = kindOf(type);
        this.noPhysics = true;
    }

    private static Kind kindOf(EntityType<?> type) {
        if (type == HbmEntityTypes.CHLORINE_FX.get()) return Kind.CHLORINE;
        if (type == HbmEntityTypes.PINK_CLOUD_FX.get()) return Kind.PINK;
        if (type == HbmEntityTypes.AGENT_ORANGE_FX.get()) return Kind.ORANGE;
        return Kind.CLOUD;
    }

    public Kind kind() {
        return kind;
    }

    public static EntityType<? extends LegacyCloudFxEntity> typeFor(Kind kind) {
        return switch (kind) {
            case CHLORINE -> HbmEntityTypes.CHLORINE_FX.get();
            case CLOUD -> HbmEntityTypes.CLOUD_FX.get();
            case PINK -> HbmEntityTypes.PINK_CLOUD_FX.get();
            case ORANGE -> HbmEntityTypes.AGENT_ORANGE_FX.get();
        };
    }

    /**
     * EntityXxxFX(world, x, y, z, mX, mY, mZ): EntityModFX picks a random direction of speed
     * (rand + rand + 1) * 0.15 * 0.4 plus 0.1 upwards, the subclass scales it by 0.1 and adds the
     * given motion.
     */
    public static LegacyCloudFxEntity create(Level level, Kind kind, double x, double y, double z,
                                             double motionX, double motionY, double motionZ) {
        LegacyCloudFxEntity fx = new LegacyCloudFxEntity(typeFor(kind), level);
        fx.setPos(x, y, z);
        fx.xo = x;
        fx.yo = y;
        fx.zo = z;
        double mx = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double my = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double mz = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        float f = (float) (Math.random() + Math.random() + 1.0D) * 0.15F;
        float f1 = Mth.sqrt((float) (mx * mx + my * my + mz * mz));
        mx = mx / f1 * f * 0.4000000059604645D;
        my = my / f1 * f * 0.4000000059604645D + 0.10000000149011612D;
        mz = mz / f1 * f * 0.4000000059604645D;
        double scale = kind == Kind.ORANGE ? 0.1D : 0.10000000149011612D;
        fx.setDeltaMovement(mx * scale + motionX, my * scale + motionY, mz * scale + motionZ);
        return fx;
    }

    /** ExplosionChaos.spawnPoisonCloud(world, x, y, z, count, speed, type): 1 cloud, 2 pink, else agent orange. */
    public static void spawnPoisonCloud(Level level, double x, double y, double z, int count, double speed, int type) {
        Kind kind = type == 1 ? Kind.CLOUD : type == 2 ? Kind.PINK : Kind.ORANGE;
        for (int i = 0; i < count; i++) {
            LegacyCloudFxEntity fx = create(level, kind, x, y, z, 0.0D, 0.0D, 0.0D);
            double motionY = level.random.nextGaussian() * speed;
            double motionX = level.random.nextGaussian() * speed;
            double motionZ = level.random.nextGaussian() * speed;
            fx.setDeltaMovement(motionX, motionY, motionZ);
            level.addFreshEntity(fx);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        // EntityXxxFX.onUpdate replaces Entity.onUpdate entirely (no base entity tick).
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        if (this.maxAge < kind.minAge) {
            this.maxAge = this.random.nextInt(kind.ageVariance) + kind.minAge;
        }
        if (!level().isClientSide && this.random.nextInt(50) == 0) {
            int ix = (int) getX();
            int iy = (int) getY();
            int iz = (int) getZ();
            switch (kind) {
                case CHLORINE, ORANGE -> poison((ServerLevel) level(), ix, iy, iz, 2.0D);
                case CLOUD -> cloud((ServerLevel) level(), ix, iy, iz, 2.0D);
                case PINK -> pinkCloud((ServerLevel) level(), ix, iy, iz, 2.0D);
            }
        }
        this.particleAge++;
        boolean dead = this.particleAge >= this.maxAge;

        double motionX = getDeltaMovement().x;
        double motionY = getDeltaMovement().y;
        double motionZ = getDeltaMovement().z;
        if (kind == Kind.ORANGE) {
            motionX *= 0.86D;
            motionY *= 0.86D;
            motionZ *= 0.86D;
            motionY -= 0.1D;
        } else {
            motionX *= 0.7599999785423279D;
            motionY *= 0.7599999785423279D;
            motionZ *= 0.7599999785423279D;
            // onGround never becomes true: the FX never calls moveEntity.
            if (level().isRaining() && level().canSeeSky(BlockPos.containing(getX(), getY(), getZ()))) {
                motionY -= 0.01D;
            }
        }

        double x = getX();
        double y = getY();
        double z = getZ();
        double subdivisions = 4;
        for (int i = 0; i < subdivisions; i++) {
            x += motionX / subdivisions;
            y += motionY / subdivisions;
            z += motionZ / subdivisions;
            BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
            if (kind == Kind.ORANGE) {
                if (!level().getBlockState(pos).isAir()) {
                    dead = true;
                    if (!level().isClientSide) {
                        for (int a = -1; a < 2; a++) {
                            for (int b = -1; b < 2; b++) {
                                for (int c = -1; c < 2; c++) {
                                    BlockPos target = new BlockPos((int) x + a, (int) y + b, (int) z + c);
                                    if (level().getBlockState(target).is(Blocks.GRASS_BLOCK)) {
                                        level().setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                                    } else {
                                        solinium((ServerLevel) level(), target);
                                    }
                                }
                            }
                        }
                    }
                }
                continue;
            }
            if (kind == Kind.PINK && !level().isClientSide) {
                BlockState state = level().getBlockState(pos);
                if (state.is(HbmBlocks.RADIOREC.get())) {
                    dead = true;
                    level().setBlock(pos, HbmBlocks.BROADCASTER_PC.get().defaultBlockState()
                            .setValue(BroadcasterBlock.FACING, state.getValue(RadioRecBlock.FACING)), 2);
                }
            }
            BlockState state = level().getBlockState(pos);
            if (state.isRedstoneConductor(level(), pos)) {
                if (!level().isClientSide && this.random.nextInt(5) != 0) {
                    dead = true;
                }
                x -= motionX / subdivisions;
                y -= motionY / subdivisions;
                z -= motionZ / subdivisions;
                motionX = 0.0D;
                motionY = 0.0D;
                motionZ = 0.0D;
            }
        }
        setDeltaMovement(motionX, motionY, motionZ);
        setPos(x, y, z);
        if (dead && !level().isClientSide) {
            discard();
        }
    }

    /** ExplosionChaos.poison: hasAnyProtection(GAS_LUNG, GAS_BLISTERING) -> filter damage 1, else the chlorine effects. */
    public static void poison(ServerLevel level, double x, double y, double z, double range) {
        for (LivingEntity entity : affected(level, x, y, z, range)) {
            if (HbmArmorProtection.hasHeadProtection(entity, HbmArmorProtection.HazardClass.GAS_LUNG, 1)
                    || HbmArmorProtection.hasHeadProtection(entity, HbmArmorProtection.HazardClass.GAS_BLISTERING, 1)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 1 * 20, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
        }
    }

    /** ExplosionChaos.pc: damageSuit(0..3, 25), then ModDamageSource.pc 5. */
    public static void pinkCloud(ServerLevel level, double x, double y, double z, double range) {
        for (LivingEntity entity : affected(level, x, y, z, range)) {
            damageSuit(entity);
            entity.hurt(level.damageSources().source(HbmDamageTypes.PC), 5.0F);
        }
    }

    /** ExplosionChaos.c: damageSuit(0..3, 25); hazmat skips; taint becomes an hour of mutation; ModDamageSource.cloud 5. */
    public static void cloud(ServerLevel level, double x, double y, double z, double range) {
        for (LivingEntity entity : affected(level, x, y, z, range)) {
            damageSuit(entity);
            if (HbmArmorProtection.hasLegacyHazmatProtection(entity)) {
                continue;
            }
            if (entity.hasEffect(HbmMobEffects.TAINT)) {
                entity.removeEffect(HbmMobEffects.TAINT);
                entity.addEffect(new MobEffectInstance(HbmMobEffects.MUTATION, 1 * 60 * 60 * 20, 0, false, true));
            }
            entity.hurt(level.damageSources().source(HbmDamageTypes.CLOUD), 5.0F);
        }
    }

    private static java.util.List<LivingEntity> affected(ServerLevel level, double x, double y, double z, double range) {
        AABB box = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> Math.sqrt(entity.distanceToSqr(x, y, z)) <= range);
    }

    /** ArmorUtil.damageSuit for slots 0..3 (boots, legs, chest, helmet). */
    private static void damageSuit(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stack.hurtAndBreak(25, entity, slot);
            }
        }
    }

    /** ExplosionNukeGeneric.solinium. */
    public static void solinium(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block b = state.getBlock();
        if (b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM || b == block("waste_earth") || b == block("waste_mycelium")) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            return;
        }
        if (isSoliniumMaterial(state)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * Material.cactus, coral, leaves, plants, sponge, vine, gourd and wood of 1.7.10, mapped to the
     * corresponding 1.21 blocks (wood = logs, planks and wooden building blocks).
     */
    static boolean isSoliniumMaterial(BlockState state) {
        Block b = state.getBlock();
        return b instanceof CactusBlock
                || state.is(BlockTags.LEAVES)
                || b instanceof BushBlock || b instanceof SugarCaneBlock || b instanceof CocoaBlock
                || b instanceof VineBlock
                || b == Blocks.SPONGE || b == Blocks.WET_SPONGE
                || b == Blocks.PUMPKIN || b == Blocks.CARVED_PUMPKIN || b == Blocks.JACK_O_LANTERN || b == Blocks.MELON
                || state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.WOODEN_PRESSURE_PLATES)
                || state.is(BlockTags.ALL_SIGNS) || b == Blocks.CRAFTING_TABLE || b == Blocks.CHEST
                || b == Blocks.TRAPPED_CHEST || b == Blocks.BOOKSHELF || b == Blocks.NOTE_BLOCK
                || b == Blocks.JUKEBOX
                || b == Blocks.BROWN_MUSHROOM_BLOCK || b == Blocks.RED_MUSHROOM_BLOCK || b == Blocks.MUSHROOM_STEM;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) this.particleAge);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.particleAge = tag.getShort("age");
        if (kind == Kind.ORANGE) {
            // EntityOrangeFX: writeToNBTOptional false and setDead on load.
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return kind != Kind.ORANGE && super.shouldBeSaved();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}

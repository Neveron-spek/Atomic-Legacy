package com.atomiclegacy.tileentity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyEmitterBlock;
import com.atomiclegacy.entity.LegacyCloudFxEntity;
import com.atomiclegacy.entity.LegacyShrapnelEntity;
import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LegacyEmitterBlockEntity extends BlockEntity {
    private int timer;

    public LegacyEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_EMITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LegacyEmitterBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)
                || !(state.getBlock() instanceof LegacyEmitterBlock emitter)) {
            return;
        }

        if (emitter.kind().isGeyser()) {
            blockEntity.tickGeyser(serverLevel, pos, state, emitter.kind());
        } else if (emitter.kind().isVent()) {
            blockEntity.tickVent(serverLevel, pos, emitter.kind());
        } else if (emitter.kind() == LegacyEmitterBlock.Kind.VENT_CHLORINE_SEAL
                && level.hasNeighborSignal(pos)) {
            blockEntity.spreadChlorine(serverLevel, pos);
        }
    }

    private void tickGeyser(ServerLevel level, BlockPos pos, BlockState state, LegacyEmitterBlock.Kind kind) {
        if (!level.isEmptyBlock(pos.above())) {
            return;
        }

        timer--;
        boolean wasActive = state.getValue(LegacyEmitterBlock.ACTIVE);
        if (timer <= 0) {
            timer = geyserDelay(level.random, kind, wasActive);
            level.setBlock(pos, state.setValue(LegacyEmitterBlock.ACTIVE, !wasActive), Block.UPDATE_CLIENTS);
        }

        // The old tile entity performs once more on the transition tick because it reads metadata before toggling.
        if (wasActive) {
            if (kind == LegacyEmitterBlock.Kind.GEYSER_CHLORINE) {
                // [parity R1: FX entities] TileEntityGeysir.chlorine(): three EntityOrangeFX, motion overwritten.
                for (int i = 0; i < 3; i++) {
                    LegacyCloudFxEntity fx = LegacyCloudFxEntity.create(level, LegacyCloudFxEntity.Kind.ORANGE,
                            pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
                    double motionX = level.random.nextGaussian() * 0.45D;
                    double motionZ = level.random.nextGaussian() * 0.45D;
                    fx.setDeltaMovement(motionX, timer * 0.3D, motionZ);
                    level.addFreshEntity(fx);
                }
            } else {
                eruptNether(level, pos);
            }
        }
    }

    private static int geyserDelay(RandomSource random, LegacyEmitterBlock.Kind kind, boolean active) {
        if (kind == LegacyEmitterBlock.Kind.GEYSER_CHLORINE) {
            return active ? 400 + random.nextInt(100) : 20;
        }
        return active ? 80 + random.nextInt(60) : random.nextBoolean() ? 300 : 450;
    }

    private void eruptNether(ServerLevel level, BlockPos pos) {
        AABB range = new AABB(pos).inflate(32.0D);
        if (level.getEntitiesOfClass(Player.class, range, Player::isAlive).isEmpty()) {
            return;
        }
        if (level.random.nextInt(3) == 0) {
            level.addFreshEntity(new LegacyShrapnelEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.5D,
                    pos.getZ() + 0.5D,
                    new Vec3(
                            level.random.nextGaussian() * 0.05D,
                            0.5D + level.random.nextDouble() * timer * 0.01D,
                            level.random.nextGaussian() * 0.05D
                    ),
                    true
            ));
        }
        if ((timer & 1) == 0) {
            sendParticle(level, HbmParticleTypes.GEYSER_FIRE.get(),
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    level.random.nextGaussian() * 0.05D, 0.2D,
                    level.random.nextGaussian() * 0.05D);
        }
    }

    private void tickVent(ServerLevel level, BlockPos pos, LegacyEmitterBlock.Kind kind) {
        if (!level.hasNeighborSignal(pos)) {
            return;
        }
        double spread = kind == LegacyEmitterBlock.Kind.VENT_CHLORINE ? 1.5D
                : kind == LegacyEmitterBlock.Kind.VENT_CLOUD ? 1.75D : 2.0D;
        double x = level.random.nextGaussian() * spread;
        double y = level.random.nextGaussian() * spread;
        double z = level.random.nextGaussian() * spread;
        BlockPos spawnBlock = pos.offset((int) x, (int) y, (int) z);
        // [parity R1: FX entities] TileEntityVent: !isNormalCube, then a real FX entity with motion (x/2, y/2, z/2).
        if (level.getBlockState(spawnBlock).isRedstoneConductor(level, spawnBlock)) {
            return;
        }
        LegacyCloudFxEntity.Kind cloudKind = switch (kind) {
            case VENT_CHLORINE -> LegacyCloudFxEntity.Kind.CHLORINE;
            case VENT_CLOUD -> LegacyCloudFxEntity.Kind.CLOUD;
            case VENT_PINK_CLOUD -> LegacyCloudFxEntity.Kind.PINK;
            default -> throw new IllegalStateException("Unsupported vent kind " + kind);
        };
        level.addFreshEntity(LegacyCloudFxEntity.create(level, cloudKind,
                spawnBlock.getX(), spawnBlock.getY(), spawnBlock.getZ(),
                x / 2.0D, y / 2.0D, z / 2.0D));
    }

    private static void sendParticle(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle,
                                     double x, double y, double z,
                                     double motionX, double motionY, double motionZ) {
        level.sendParticles(particle, x, y, z, 0, motionX, motionY, motionZ, 1.0D);
    }

    private void spreadChlorine(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int step = 0; step <= 50; step++) {
            BlockState state = level.getBlockState(cursor);
            if (state.canBeReplaced()) {
                level.setBlock(cursor, HbmBlocks.CHLORINE_GAS.get().defaultBlockState(), Block.UPDATE_ALL);
                state = level.getBlockState(cursor);
            }
            if (!state.is(HbmBlocks.CHLORINE_GAS.get()) && !state.is(HbmBlocks.VENT_CHLORINE_SEAL.get())) {
                return;
            }
            cursor.move(Direction.getRandom(level.random));
        }
    }
}

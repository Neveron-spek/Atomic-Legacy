package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Direct port of the four PartEmitter effect modes and their legacy parameters. */
public final class PartEmitterBlockEntity extends BlockEntity {
    public static final int RANGE = 150;
    public static final int EFFECT_COUNT = 4;

    private int effect;

    public PartEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PART_EMITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PartEmitterBlockEntity emitter) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            emitter.tickServer(serverLevel);
        }
    }

    public void cycleEffect() {
        effect = (effect + 1) % EFFECT_COUNT;
        sync();
    }

    private void tickServer(ServerLevel level) {
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;

        if (effect == 1) {
            // ParticleUtil.spawnGasFlame: TargetPoint(x, y, z, 150) around the particle position
            double fx = worldPosition.getX() + level.random.nextDouble();
            double fy = worldPosition.getY() + 4.5D + level.random.nextDouble();
            double fz = worldPosition.getZ() + level.random.nextDouble();
            sendAllAround(level, HbmParticleTypes.GEYSER_FIRE.get(), fx, fy, fz, fx, fy, fz,
                    level.random.nextGaussian() * 0.2D, 0.1D, level.random.nextGaussian() * 0.2D);
        } else if (effect == 2) {
            sendAllAround(level, HbmParticleTypes.PART_EMITTER_TOWER_SMALL.get(), x, y, z,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 0.0D, 0.0D, 0.0D);
        } else if (effect == 3) {
            double tx = x + level.random.nextDouble() * 3.0D - 1.5D;
            double ty = worldPosition.getY() + 1.0D;
            double tz = z + level.random.nextDouble() * 3.0D - 1.5D;
            // TargetPoint(dimension, xCoord, yCoord, zCoord, range): centred on the block corner
            sendAllAround(level, HbmParticleTypes.PART_EMITTER_TOWER_LARGE.get(), tx, ty, tz,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * [parity S4: part_emitter] AuxParticlePacketNT via createAllAroundThreadedPacket(TargetPoint(..., 150)): every
     * player of this dimension within RANGE of the target point. The forced (long-distance) particle packet keeps
     * both the server's 32-block limit and the client's 32-block camera cull out of the way, as the 1.7.10 effectNT
     * handler added the particle directly.
     */
    private static void sendAllAround(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle,
                                      double x, double y, double z, double centerX, double centerY, double centerZ,
                                      double motionX, double motionY, double motionZ) {
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (player.distanceToSqr(centerX, centerY, centerZ) < (double) RANGE * RANGE) {
                level.sendParticles(player, particle, true, x, y, z, 0, motionX, motionY, motionZ, 1.0D);
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("effect", effect);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        effect = Math.floorMod(tag.getInt("effect"), EFFECT_COUNT);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

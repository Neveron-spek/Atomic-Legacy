package com.atomiclegacy.blocks;

import com.atomiclegacy.explosion.LegacyChaosEffects;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Exact redstone trigger semantics for BombFloat, EMP Device and Flame War in a Box. */
public final class LegacyRedstoneBombBlock extends Block {
    public enum Kind {
        FLOAT,
        EMP,
        FLAME_WAR
    }

    private final Kind kind;

    public LegacyRedstoneBombBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            detonate((ServerLevel) level, pos);
        }
    }

    /** IBomb.explode of BombFloat (float_bomb, emp_bomb) and BombFlameWar. */
    public void detonate(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        if (kind == Kind.FLAME_WAR) {
            // BombFlameWar.explode: world.func_147480_a(x, y, z, false), no trigger sound
            level.destroyBlock(pos, false);
            flameWar(level, pos, level.random);
            return;
        }
        // BombFloat.explode: world.playSoundEffect(x, y, z, "hbm:weapon.sparkShoot", 5.0f, rand * 0.2F + 0.9F)
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                com.atomiclegacy.registry.HbmSoundEvents.WEAPON_SPARK_SHOOT.get(), SoundSource.BLOCKS,
                5.0F, level.random.nextFloat() * 0.2F + 0.9F);
        level.removeBlock(pos, false);
        if (kind == Kind.FLOAT) {
            LegacyChaosEffects.floatAndLift(level, pos, 15, 50);
        } else {
            dischargeEmp(level, pos);
            // EntityEMPBlast wave = new EntityEMPBlast(world, 50) at the block centre
            com.atomiclegacy.entity.LegacyEmpEntity.spawnBlast(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 50);
        }
    }

    /**
     * 1.7.10 iterated every loaded position in the EMP sphere, cleared HBM energy
     * handlers, then converted one fifth of them into electrical scrap.  The modern
     * power endpoint owns its stored energy, so applyPower with a saturated output
     * is the direct equivalent without touching unrelated block entities.
     */
    public static void dischargeEmp(ServerLevel level, BlockPos origin) {
        dischargeEmp(level, origin, 50);
    }

    /** Radius-specific legacy EMP pulse used by the Tier-2 missile. */
    public static void dischargeEmp(ServerLevel level, BlockPos origin, int radius) {
        int radiusSquaredHalf = radius * radius / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z >= radiusSquaredHalf) {
                        continue;
                    }
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockEntity entity = level.getBlockEntity(cursor);
                    if (!(entity instanceof PowerEndpoint endpoint)) {
                        continue;
                    }
                    endpoint.applyPower(Long.MAX_VALUE, 0L);
                    if (level.random.nextInt(5) == 0) {
                        level.setBlock(cursor, HbmBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void flameWar(ServerLevel level, BlockPos origin, RandomSource random) {
        // 150 x ExplosionVNT size 4 without block allocator: EntityProcessorCrossSmooth(1, 25) + ExplosionEffectTiny
        for (int index = 0; index < 150; index++) {
            double x = origin.getX() + random.nextInt(51) - 25;
            double y = origin.getY() + random.nextInt(11) - 5;
            double z = origin.getZ() + random.nextInt(51) - 25;
            com.atomiclegacy.explosion.LegacyVnt.at(level, x, y, z, 4.0F)
                    .entitiesCrossSmooth(1.0D, 25.0F, 0.0F, 0.0F)
                    .sfx(() -> {
                        level.playSound(null, x, y, z,
                                com.atomiclegacy.registry.HbmSoundEvents.WEAPON_EXPLOSION_TINY.get(),
                                SoundSource.BLOCKS, 15.0F, 1.0F);
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, null, x, y, z, 100,
                                new com.atomiclegacy.network.GunTinyExplosionPayload(x, y, z, 1.5F, 1));
                    })
                    .explode();
        }
        // ExplosionVNT 15F: BlockAllocatorStandard(32), setNoDrop(), EntityProcessorCrossSmooth(2, 200)
        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(origin);
        com.atomiclegacy.explosion.LegacyVnt.at(level, center, 15.0F).allocStandard(32).noDrop()
                .entitiesCrossSmooth(2.0D, 200.0F, 0.0F, 0.0F).explode();
        // ExplosionCreator.composeEffectSmall(world, x + 0.5, y + 0.5, z + 0.5)
        com.atomiclegacy.entity.LegacyProjectileUtil.composeExplosionEffect(level, center,
                10, 2.0F, 0.5F, 25.0F, 5, 8, 20, 0.75F, 1.0F, -2.0F, 150.0F);
    }
}

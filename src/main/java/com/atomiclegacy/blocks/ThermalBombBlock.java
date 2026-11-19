package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** Direct modern counterpart of the 1.7.10 endothermic and exothermic bombs. */
public final class ThermalBombBlock extends Block {
    private static final int TERRAIN_STRENGTH = 15;
    private static final int ENTITY_RADIUS = 20;

    private final Kind kind;

    public ThermalBombBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // BombThermo has no onBlockAdded trigger.
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            detonate(level, pos, null);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        // BombThermo is a plain Block: another explosion only destroys it.
        super.onBlockExploded(state, level, pos, explosion);
    }

    public void detonate(Level level, BlockPos pos, @Nullable Entity source) {
        if (!(level instanceof ServerLevel server) || !level.getBlockState(pos).is(this)) {
            return;
        }
        level.removeBlock(pos, false);
        transformTerrain(server, pos);
        affectEntities(server, pos);
        // world.createExplosion(null, x, y, z, 5.0F, true): smoking, not flaming
        com.atomiclegacy.explosion.LegacyVnt.vanilla(server, null, pos.getX(), pos.getY(), pos.getZ(), 5.0F, false, true);
    }

    private void transformTerrain(ServerLevel level, BlockPos center) {
        int radius = TERRAIN_STRENGTH * 2;
        int radiusSquared = radius * radius;
        int innerRadiusSquared = radiusSquared / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z >= innerRadiusSquared + level.random.nextInt(innerRadiusSquared / 2)) {
                        continue;
                    }
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (this.kind == Kind.ENDOTHERMIC) {
                        freeze(level, cursor);
                    } else {
                        scorch(level, cursor);
                    }
                }
            }
        }
    }

    private static void freeze(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState replacement = null;
        if (state.is(HbmBlocks.VOLCANIC_LAVA_BLOCK.get())) {
            replacement = Blocks.COBBLESTONE.defaultBlockState();
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            replacement = HbmBlocks.FROZEN_GRASS.get().defaultBlockState();
        } else if (state.is(Blocks.DIRT)) {
            replacement = HbmBlocks.FROZEN_DIRT.get().defaultBlockState();
        } else if (state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(HbmBlocks.WASTE_LOG.get())) {
            replacement = HbmBlocks.FROZEN_LOG.get().defaultBlockState();
        } else if (state.is(net.minecraft.tags.BlockTags.PLANKS) || state.is(HbmBlocks.WASTE_PLANKS.get())) {
            replacement = HbmBlocks.FROZEN_PLANKS.get().defaultBlockState();
        } else if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICKS)) {
            replacement = Blocks.PACKED_ICE.defaultBlockState();
        } else if (state.getBlock() instanceof LeavesBlock) {
            replacement = Blocks.SNOW_BLOCK.defaultBlockState();
        } else if (state.getFluidState().is(Fluids.LAVA)) {
            replacement = Blocks.OBSIDIAN.defaultBlockState();
        } else if (state.getFluidState().is(Fluids.WATER)) {
            replacement = Blocks.ICE.defaultBlockState();
        }
        if (replacement != null) {
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
        }
    }

    private static void scorch(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState replacement = null;
        if (state.is(Blocks.GRASS_BLOCK) || state.is(HbmBlocks.FROZEN_GRASS.get()) || state.is(HbmBlocks.FROZEN_DIRT.get())) {
            replacement = Blocks.DIRT.defaultBlockState();
        } else if (state.is(Blocks.DIRT)) {
            replacement = Blocks.NETHERRACK.defaultBlockState();
        } else if (state.is(Blocks.NETHERRACK) || state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.OBSIDIAN)) {
            replacement = Blocks.LAVA.defaultBlockState();
        } else if (state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(HbmBlocks.FROZEN_LOG.get())) {
            replacement = HbmBlocks.WASTE_LOG.get().defaultBlockState();
        } else if (state.is(net.minecraft.tags.BlockTags.PLANKS) || state.is(HbmBlocks.FROZEN_PLANKS.get())) {
            replacement = HbmBlocks.WASTE_PLANKS.get().defaultBlockState();
        } else if (state.getBlock() instanceof LeavesBlock || state.getFluidState().is(Fluids.WATER) || state.is(Blocks.ICE)) {
            replacement = Blocks.AIR.defaultBlockState();
        } else if (state.is(Blocks.PACKED_ICE)) {
            replacement = Blocks.WATER.defaultBlockState();
        }
        if (replacement != null) {
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
        }
    }

    private void affectEntities(ServerLevel level, BlockPos center) {
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        if (this.kind == Kind.ENDOTHERMIC) {
            // ExplosionThermo.freezer(world, x, y, z, 20): eye distance below 20, ocelots/cats spared
            AABB area = new AABB(x - ENTITY_RADIUS - 1.0D, y - ENTITY_RADIUS - 1.0D, z - ENTITY_RADIUS - 1.0D,
                    x + ENTITY_RADIUS + 1.0D, y + ENTITY_RADIUS + 1.0D, z + ENTITY_RADIUS + 1.0D);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                double dx = living.getX() - x;
                double dy = living.getEyeY() - y;
                double dz = living.getZ() - z;
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) >= ENTITY_RADIUS
                        || living instanceof net.minecraft.world.entity.animal.Ocelot
                        || living instanceof net.minecraft.world.entity.animal.Cat) {
                    continue;
                }
                freezeAround(level, new BlockPos((int) living.getX(), (int) living.getY(), (int) living.getZ()));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90 * 20, 2));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3 * 60 * 20, 2));
            }
            return;
        }
        // ExplosionThermo.setEntitiesOnFire(world, x, y, z, 20): every entity burns, living ones are weakened
        AABB area = new AABB(x - ENTITY_RADIUS, y - ENTITY_RADIUS, z - ENTITY_RADIUS,
                x + ENTITY_RADIUS, y + ENTITY_RADIUS, z + ENTITY_RADIUS);
        for (Entity entity : level.getEntities((Entity) null, area)) {
            if (Math.sqrt(entity.distanceToSqr(x, y, z)) > ENTITY_RADIUS) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 4));
            }
            entity.igniteForSeconds(10.0F);
        }
    }

    private static void freezeAround(ServerLevel level, BlockPos base) {
        for (int x = -2; x <= 0; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(base.offset(x, y, z), Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    public enum Kind {
        ENDOTHERMIC,
        EXOTHERMIC
    }
}

package com.atomiclegacy.blocks;

import com.atomiclegacy.entity.LegacyProjectileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

/** Six-way plastic explosive with the redstone detonation path from 1.7.10. */
public final class PlasticExplosiveBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PlasticExplosiveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // BlockPlasticExplosive has no onBlockAdded trigger.
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.hasNeighborSignal(pos)) {
            detonate(level, pos, null);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.Explosion explosion) {
        // BlockDetonatable.onBlockDestroyedByExplosion: the block is removed and a fuse-0 primed entity
        // calls explodeEntity -> explode on its first update.
        if (level instanceof ServerLevel server) {
            level.removeBlock(pos, false);
            com.atomiclegacy.explosion.LegacyDeferredDetonations.defer(server, () -> explodeAt(server, pos));
        }
    }

    /**
     * BlockPlasticExplosive.explode: {@code new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 20).makeStandard()
     * .setBlockProcessor(new BlockProcessorStandard().setNoDrop()).explode()}. The block itself is not removed
     * first; its own blast destroys it, which queues the second blast exactly as in 1.7.10.
     */
    private static void explodeAt(ServerLevel server, BlockPos pos) {
        com.atomiclegacy.explosion.LegacyVnt.at(server, pos.getCenter(), 20.0F).makeStandard().noDrop().explode();
    }

    public boolean canDropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return false;
    }

    public void detonate(Level level, BlockPos pos, @Nullable Entity owner) {
        if (!(level instanceof ServerLevel server) || !level.getBlockState(pos).is(this)) {
            return;
        }
        explodeAt(server, pos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}

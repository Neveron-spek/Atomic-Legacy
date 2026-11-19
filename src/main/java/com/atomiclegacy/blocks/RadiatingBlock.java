package com.atomiclegacy.blocks;

import com.atomiclegacy.radiation.ChunkRadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity K2: block radiation] Placed radioactive block. For the 1.7.10 BlockHazard family the emission follows
 * {@link LegacyBlockHazardRadiation} (0.1 x item radiation every 20 ticks after placement); ore_schrabidium is a
 * BlockOre with rad 0.1F on random/update ticks; every other block (ores, BlockOutgas, meteor blocks) has no world
 * emission. In 1.7.10 none of them irradiates entities that touch it, so the former contact dose (rad / 20 per tick)
 * and the emission of 10 % every 60..559 ticks with a cap were inventions of the port base and are removed.
 */
public class RadiatingBlock extends Block {
    public RadiatingBlock(Properties properties, double radiation) {
        super(properties);
    }

    private boolean isOreSchrabidium() {
        return LegacyBlockHazardRadiation.ORE_SCHRABIDIUM.equals(LegacyBlockHazardRadiation.id(this));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.getBlock() == state.getBlock() || level.isClientSide) {
            return;
        }
        if (LegacyBlockHazardRadiation.isBlockHazard(this)) {
            LegacyBlockHazardRadiation.onBlockAdded(this, level, pos);
        } else if (isOreSchrabidium()) {
            level.scheduleTick(pos, this, LegacyBlockHazardRadiation.TICK_RATE); // BlockOre.onBlockAdded
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isOreSchrabidium()) {
            oreUpdateTick(level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (LegacyBlockHazardRadiation.isBlockHazard(this)) {
            LegacyBlockHazardRadiation.updateTick(this, level, pos);
        } else if (isOreSchrabidium()) {
            oreUpdateTick(level, pos);
        }
    }

    /** BlockOre.updateTick (radiation branch). */
    private void oreUpdateTick(ServerLevel level, BlockPos pos) {
        ChunkRadiationData.get(level).incrementRadiation(pos, LegacyBlockHazardRadiation.ORE_SCHRABIDIUM_RAD);
        level.scheduleTick(pos, this, LegacyBlockHazardRadiation.TICK_RATE);
    }
}

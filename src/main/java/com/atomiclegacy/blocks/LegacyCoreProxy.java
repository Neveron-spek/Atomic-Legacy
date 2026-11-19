package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * [parity U3: dummy parts] Port-only technical cells of a multiblock (heavy_door_part, machine_dummy, floodlight_dummy,
 * dummy_plate_cargo). In 1.7.10 every cell of a BlockDummyable is the machine block itself, so hardness, explosion
 * resistance and breaking speed of a cell are those of the machine. The port keeps one shared cell block, which
 * therefore answers with the values of the core block it belongs to.
 */
public interface LegacyCoreProxy {
    /** Core position of the cell at {@code pos}, or null when the link is unknown. */
    @Nullable
    BlockPos legacyCorePos(BlockGetter level, BlockPos pos);

    /** The state whose values the cell carries: its core, or the cell itself when the core is missing. */
    static BlockState valueState(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LegacyCoreProxy proxy) {
            BlockPos core = proxy.legacyCorePos(level, pos);
            if (core != null && !core.equals(pos)) {
                BlockState coreState = level.getBlockState(core);
                if (!coreState.isAir() && !(coreState.getBlock() instanceof LegacyCoreProxy)) {
                    return coreState;
                }
            }
        }
        return state;
    }

    /** [parity V4: step sounds] A 1.7.10 cell is the machine block: it steps, breaks and places with the core's sound. */
    static net.minecraft.world.level.block.SoundType coreSound(net.minecraft.world.level.LevelReader level, BlockPos pos,
                                                               BlockState state, @Nullable net.minecraft.world.entity.Entity entity) {
        BlockState core = valueState(level, pos, state);
        if (core != state && state.getBlock() instanceof LegacyCoreProxy proxy) {
            BlockPos corePos = proxy.legacyCorePos(level, pos);
            if (corePos != null) {
                return core.getSoundType(level, corePos, entity);
            }
        }
        return state.getSoundType();
    }

    /** Position-less explosion resistance as the nukes read it (1.7.10 getExplosionResistance(null)), core-resolved. */
    static float blockResistance(BlockGetter level, BlockPos pos, BlockState state) {
        return valueState(level, pos, state).getBlock().getExplosionResistance();
    }
}

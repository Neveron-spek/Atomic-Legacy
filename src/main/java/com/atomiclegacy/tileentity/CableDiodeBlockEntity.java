package com.atomiclegacy.tileentity;

import com.atomiclegacy.power.PowerGraphNode;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * [parity L1: diode] 1.7.10 {@code CableDiode.TileEntityDiode extends TileEntityCableBaseNT}
 * at commit 936c60c is a deprecated plain conductor: it connects on all six
 * sides and has no direction, throughput level or priority.
 */
public final class CableDiodeBlockEntity extends BlockEntity implements PowerGraphNode {
    public CableDiodeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    @Override
    public BlockPos getGraphPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getRemotePowerLinks(Level level) {
        return List.of();
    }
}

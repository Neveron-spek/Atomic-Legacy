package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity h1] TileEntityMachineIGenerator (tileentity_igenerator): super(21) slots that no side exposes
 * (TileEntityMachineBase#getAccessibleSlotsFromSide = {}) and no GUI opens (MachineIGenerator#onBlockActivated has the
 * openGui call commented out), empty updateEntity. It only carries the RenderIGenerator TESR, so the port keeps an
 * inventory-less, non-ticking block entity for the renderer.
 */
public final class IndustrialGeneratorBlockEntity extends BlockEntity {
    public IndustrialGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.INDUSTRIAL_GENERATOR.get(), pos, state);
    }
}

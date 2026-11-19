package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FloodlightBeamBlockEntity extends BlockEntity {
    private BlockPos source;
    private int index;

    public FloodlightBeamBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FLOODLIGHT_BEAM.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FloodlightBeamBlockEntity beam) {
        if (level.isClientSide || level.getGameTime() % 5L != 0L) {
            return;
        }
        if (beam.source != null && !level.isLoaded(beam.source)) {
            return; // [e3] FloodlightBeam: an unloaded source chunk is neither loaded nor treated as a missing lamp
        }
        if (beam.source == null || !(level.getBlockEntity(beam.source) instanceof FloodlightBlockEntity light)
                || !light.isOn() || !light.isLightPosition(pos, beam.index)) {
            level.removeBlock(pos, false);
        }
    }

    public void setSource(BlockPos source, int index) {
        this.source = source.immutable();
        this.index = index;
        setChanged();
    }

    public boolean isSource(FloodlightBlockEntity light, int index) {
        return source != null && source.equals(light.getBlockPos()) && this.index == index;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (source != null) {
            tag.putInt("SourceX", source.getX());
            tag.putInt("SourceY", source.getY());
            tag.putInt("SourceZ", source.getZ());
        }
        tag.putInt("Index", index);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SourceX")) {
            source = new BlockPos(tag.getInt("SourceX"), tag.getInt("SourceY"), tag.getInt("SourceZ"));
        }
        index = tag.getInt("Index");
    }
}

package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * [parity X1: custom machines] TileEntityProxyCombo().inventory().power().fluid() of cm_port: the controller sets
 * cachedPosition in TileEntityCustomMachine#checkStructure ("hasPos"/"pX"/"pY"/"pZ"). As a dummy part it lets cables,
 * ducts and item handlers reach the controller; an unbound port resolves to itself and connects to nothing.
 */
public class CustomMachinePortBlockEntity extends MachineDummyBlockEntity {
    private boolean hasPos;

    public CustomMachinePortBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CUSTOM_MACHINE_PORT.get(), pos, state);
    }

    /** proxy.cachedPosition = controller; proxy.markDirty() */
    public void bind(BlockPos controller) {
        if (this.hasPos && super.getCorePos().equals(controller)) {
            setChanged();
            return;
        }
        this.hasPos = true;
        setCorePos(controller);
    }

    public boolean hasPos() {
        return this.hasPos;
    }

    @Override
    public BlockPos getCorePos() {
        return this.hasPos ? super.getCorePos() : this.worldPosition;
    }

    /** TileEntityProxyBase#getTE: the tile at cachedPosition unless it is missing or another proxy. */
    @Nullable
    @Override
    public BlockEntity core() {
        if (!this.hasPos || this.level == null) return null;
        BlockEntity te = this.level.getBlockEntity(super.getCorePos());
        return te == null || te instanceof MachineDummyBlockEntity ? null : te;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("hasPos", this.hasPos);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hasPos = tag.getBoolean("hasPos");
    }
}

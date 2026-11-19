package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.IcfAssembledLaserBlock;
import com.atomiclegacy.power.PowerGraphNode;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores the exact raw ICF part and controller position while it is assembled. */
public final class IcfAssembledLaserBlockEntity extends BlockEntity implements PowerGraphNode {
    private Block originalBlock = HbmBlocks.ICF_LASER_COMPONENT.get();
    private int originalVariant;
    private BlockPos controllerPos = BlockPos.ZERO;

    public IcfAssembledLaserBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_ASSEMBLED_LASER.get(), pos, state);
    }

    public void configure(Block originalBlock, int originalVariant, BlockPos controllerPos) {
        this.originalBlock = originalBlock;
        this.originalVariant = originalVariant;
        this.controllerPos = controllerPos.immutable();
        setChanged();
    }

    /** Rewrites the persisted controller link after a Create contraption move. */
    public void transformAssemblyLink(java.util.function.UnaryOperator<BlockPos> transform) {
        this.controllerPos = transform.apply(this.controllerPos).immutable();
        setChanged();
    }

    public BlockPos controllerPos() {
        return this.controllerPos;
    }

    public void restoreOriginal() {
        if (this.level == null || this.level.isClientSide || this.originalBlock == null) {
            return;
        }
        BlockState restored = this.originalBlock.defaultBlockState();
        if (restored.hasProperty(com.atomiclegacy.blocks.LegacyVariantBlock.VARIANT)) {
            restored = restored.setValue(com.atomiclegacy.blocks.LegacyVariantBlock.VARIANT, this.originalVariant);
        }
        // [parity A3: icf_block] BlockICF#breakBlock: world.removeTileEntity before placing the raw part (otherwise the
        // assembled block entity stays orphaned on the restored part and is saved with the chunk).
        this.level.removeBlockEntity(this.worldPosition);
        this.level.setBlock(this.worldPosition, restored, Block.UPDATE_ALL);
        if (this.level.getBlockEntity(this.controllerPos) instanceof IcfControllerBlockEntity controller) {
            controller.setAssembled(false);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfAssembledLaserBlockEntity entity) {
        // [parity A3: icf_block] TileEntityBlockICF: an unloaded controller chunk neither restores nor loads anything.
        if (level.getGameTime() % 20L != 0L || !level.isLoaded(entity.controllerPos)) {
            return;
        }
        if (!(level.getBlockEntity(entity.controllerPos) instanceof IcfControllerBlockEntity controller)
                || !controller.assembled()) {
            entity.restoreOriginal();
        }
    }

    @Override
    public BlockPos getGraphPos() {
        return this.worldPosition;
    }

    @Override
    public boolean isPowerGraphEnabled(LevelAccessor level) {
        return getBlockState().getValue(com.atomiclegacy.blocks.LegacyVariantBlock.VARIANT) == 1
                && level.getBlockEntity(controllerPos) instanceof IcfControllerBlockEntity controller
                && controller.assembled();
    }

    @Override
    public java.util.List<BlockPos> getRemotePowerLinks(Level level) {
        return isPowerGraphEnabled(level) ? java.util.List.of(controllerPos) : java.util.List.of();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("OriginalBlock", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(originalBlock).toString());
        tag.putInt("OriginalVariant", originalVariant);
        tag.putInt("ControllerX", controllerPos.getX());
        tag.putInt("ControllerY", controllerPos.getY());
        tag.putInt("ControllerZ", controllerPos.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        originalBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                net.minecraft.resources.ResourceLocation.parse(tag.getString("OriginalBlock"))).orElse(HbmBlocks.ICF_LASER_COMPONENT.get());
        originalVariant = tag.getInt("OriginalVariant");
        controllerPos = new BlockPos(tag.getInt("ControllerX"), tag.getInt("ControllerY"), tag.getInt("ControllerZ"));
    }
}

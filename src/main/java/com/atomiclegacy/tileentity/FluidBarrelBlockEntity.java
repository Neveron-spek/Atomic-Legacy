package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.FluidBarrelBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidRelease;
import com.atomiclegacy.fluid.HbmFluidTrait;
import com.atomiclegacy.fluid.HbmFluidTraitInfo;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FluidBarrelBlockEntity extends FluidTankBlockEntity {
    public FluidBarrelBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FLUID_BARREL.get(), pos, blockState, kind(blockState).capacity(), "container.atomiclegacy.barrel");
    }

    @Override
    protected boolean canAcceptFluid(HbmFluidDefinition fluid) {
        return super.canAcceptFluid(fluid);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidBarrelBlockEntity barrel) {
        FluidTankBlockEntity.tick(level, pos, state, barrel);
        if (barrel.tank().amount() > 0 && !barrel.tank().type().isNone()) {
            barrel.checkFluidInteraction(level, pos);
        }
    }

    @Override
    public List<Port> ports(LevelAccessor level) {
        return Arrays.stream(Direction.values())
                .map(direction -> new Port(this.worldPosition, direction))
                .toList();
    }

    @Override
    protected boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        return queriedPos.equals(this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        dropInventoryContents(level, pos);
    }

    // [parity L2: barrels] TileEntityBarrel.checkFluidInteraction of 1.7.10 (936c60c)
    private void checkFluidInteraction(Level level, BlockPos pos) {
        HbmFluidDefinition fluid = this.tank().type();
        FluidBarrelBlock.Kind kind = kind(this.getBlockState());

        //for when you fill antimatter into a matter tank
        if (kind != FluidBarrelBlock.Kind.ANTIMATTER && fluid.hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5.0F, true, Level.ExplosionInteraction.BLOCK);
            return;
        }

        //for when you fill hot or corrosive liquids into a plastic tank
        if (kind == FluidBarrelBlock.Kind.PLASTIC
                && (fluid.hasTrait(HbmFluidTrait.CORROSIVE) || HbmFluidTraitInfo.isHot(fluid))) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

        if (kind == FluidBarrelBlock.Kind.CORRODED) {
            if (level.random.nextInt(3) == 0) {
                this.tank().setAmount(this.tank().amount() - 1);
                HbmFluidRelease.onRelease(level, pos, fluid, HbmPollution.ReleaseType.SPILL, 1.0D);
                this.sync();
            }
            if (level.random.nextInt(3 * 60 * 20) == 0) {
                level.destroyBlock(pos, false);
                return;
            }
        }

        if (kind == FluidBarrelBlock.Kind.STEEL && HbmFluidTraitInfo.isHighlyCorrosive(fluid)) {
            int fill = this.tank().amount();
            level.setBlock(pos, HbmBlocks.BARREL_CORRODED.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) {
                barrel.tank().setType(fluid);
                barrel.tank().setAmount(fill);
                barrel.sync();
            }
        }
        // TomSaveData firestorm (water barrel explodes under open sky): the impact/firestorm system is not ported.
    }

    @Override
    public boolean usesBarrelPriority() {
        return true;
    }

    /** TileEntityBarrel.getProviderSpeed / getReceiverSpeed: 1_000_000_000 in the allowed modes. */
    @Override
    protected int maxProviderTransfer() {
        return 1_000_000_000;
    }

    @Override
    protected int maxReceiverTransfer() {
        return 1_000_000_000;
    }

    private static FluidBarrelBlock.Kind kind(BlockState state) {
        if (state.getBlock() instanceof FluidBarrelBlock barrel) {
            return barrel.kind();
        }
        return FluidBarrelBlock.Kind.PLASTIC;
    }
}

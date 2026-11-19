package com.atomiclegacy.integration.create;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.BlastDoorBlock;
import com.atomiclegacy.blocks.BlastDoorDummyBlock;
import com.atomiclegacy.blocks.ElectrolyzerBlock;
import com.atomiclegacy.blocks.FloodlightBlock;
import com.atomiclegacy.blocks.FloodlightDummyBlock;
import com.atomiclegacy.blocks.GasTurbineBlock;
import com.atomiclegacy.blocks.HbmHeavyDoorBlock;
import com.atomiclegacy.blocks.HbmHeavyDoorPartBlock;
import com.atomiclegacy.blocks.LargeFluidTankBlock;
import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.blocks.LegacyTurretBlock;
import com.atomiclegacy.blocks.MachineDummyBlock;
import com.atomiclegacy.blocks.MiningLaserBlock;
import com.atomiclegacy.blocks.PowerPylonBlock;
import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.blocks.SatelliteDockBlock;
import com.atomiclegacy.blocks.SatelliteDockDummyBlock;
import com.atomiclegacy.blocks.StirlingGeneratorBlock;
import com.atomiclegacy.blocks.TurretChekhovBlock;
import com.atomiclegacy.blocks.TurretJeremyBlock;
import com.atomiclegacy.blocks.WatzBlock;
import com.atomiclegacy.blocks.WatzPumpBlock;
import com.atomiclegacy.blocks.WoodBurnerBlock;
import com.atomiclegacy.tileentity.IcfAssembledLaserBlockEntity;
import com.atomiclegacy.tileentity.IcfControllerBlockEntity;
import com.atomiclegacy.tileentity.PwrBlockEntity;
import com.atomiclegacy.tileentity.PwrControllerBlockEntity;
import com.atomiclegacy.registry.HbmBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stops Create contraptions from capturing an already formed HBM multiblock.
 * Unassembled construction blocks remain movable; only cores, their proxy
 * cells, and parts carrying an active assembly link are rejected.
 */
public final class HbmCreateMultiblockCompat {
    private static boolean installed;

    private HbmCreateMultiblockCompat() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        BlockMovementChecks.registerMovementAllowedCheck(HbmCreateMultiblockCompat::movementAllowed);
        AtomicLegacy.LOGGER.info("Installed Create movement guard for formed HBM multiblocks and movable dud bombs");
    }

    private static BlockMovementChecks.CheckResult movementAllowed(
            BlockState state, Level level, BlockPos pos) {
        // Preserve the existing high-version gameplay exception for dud bombs.
        if (state.is(HbmBlocks.CRASHED_BOMB.get())) {
            return BlockMovementChecks.CheckResult.SUCCESS;
        }
        return isFormedMultiblock(state, level, pos)
                ? BlockMovementChecks.CheckResult.FAIL
                : BlockMovementChecks.CheckResult.PASS;
    }

    private static boolean isFormedMultiblock(BlockState state, Level level, BlockPos pos) {
        Block block = state.getBlock();
        if (isProxyBlock(block) || isPlacedMultiblockCore(block)) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PwrBlockEntity part) {
            return part.isAssembledPart();
        }
        if (blockEntity instanceof PwrControllerBlockEntity controller) {
            return controller.assembled();
        }
        if (blockEntity instanceof IcfAssembledLaserBlockEntity) {
            return true;
        }
        return blockEntity instanceof IcfControllerBlockEntity controller
                && controller.assembled();
    }

    private static boolean isProxyBlock(Block block) {
        return block instanceof MachineDummyBlock
                || block instanceof BlastDoorDummyBlock
                || block instanceof HbmHeavyDoorPartBlock
                || block instanceof SatelliteDockDummyBlock
                || block instanceof FloodlightDummyBlock;
    }

    private static boolean isPlacedMultiblockCore(Block block) {
        return block instanceof LargeMachineBlock
                || block instanceof BlastDoorBlock
                || block instanceof ElectrolyzerBlock
                || block instanceof FloodlightBlock
                || block instanceof GasTurbineBlock
                || block instanceof HbmHeavyDoorBlock
                || block instanceof LargeFluidTankBlock
                || block instanceof LegacyTurretBlock
                || block instanceof MiningLaserBlock
                || block instanceof PowerPylonBlock
                || block instanceof RbmkComponentBlock
                || block instanceof SatelliteDockBlock
                || block instanceof StirlingGeneratorBlock
                || block instanceof TurretChekhovBlock
                || block instanceof TurretJeremyBlock
                || block instanceof WatzBlock
                || block instanceof WatzPumpBlock
                || block instanceof WoodBurnerBlock;
    }
}

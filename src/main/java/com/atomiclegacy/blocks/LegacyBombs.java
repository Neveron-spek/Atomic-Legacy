package com.atomiclegacy.blocks;

import javax.annotation.Nullable;

import com.atomiclegacy.tileentity.BombMultiBlockEntity;
import com.atomiclegacy.tileentity.LegacyNukeBlockEntity;
import com.atomiclegacy.tileentity.NukeBoyBlockEntity;
import com.atomiclegacy.tileentity.WallChargeExplosions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity H3] 1.7.10 {@code com.hbm.interfaces.IBomb}: remote triggers (detonators, dead man's switch)
 * call {@link #explode} and report the returned {@link BombReturnCode}.
 *
 * <p>Covered implementers: the nuke blocks, BombMulti, BlockChargeBase, ExplosiveCharge (det_charge, det_nuke),
 * DetMiner, Landmine, BlockCrashedBomb, BlockPlasticExplosive, BombFloat, BombFlameWar and BombThermo.
 * DetCord and BlockTNTBase were never IBomb. Launch pads, doors, seals and wand blocks also implemented
 * IBomb in 1.7.10 but belong to other systems and are not dispatched here.</p>
 */
public final class LegacyBombs {

    /** IBomb.BombReturnCode with the 1.7.10 success flags; messages are the bomb.* lines of en_US.lang. */
    public enum BombReturnCode {
        UNDEFINED(false, ""),
        DETONATED(true, "message.atomiclegacy.bomb.detonated"),
        TRIGGERED(true, "message.atomiclegacy.bomb.triggered"),
        LAUNCHED(true, "message.atomiclegacy.bomb.launched"),
        ERROR_MISSING_COMPONENT(false, "message.atomiclegacy.bomb.missing_component"),
        ERROR_INCOMPATIBLE(false, "message.atomiclegacy.bomb.incompatible"),
        ERROR_NO_BOMB(false, "message.atomiclegacy.bomb.nobomb");

        private final boolean success;
        private final String messageKey;

        BombReturnCode(boolean success, String messageKey) {
            this.success = success;
            this.messageKey = messageKey;
        }

        public boolean wasSuccessful() {
            return this.success;
        }

        public String getUnlocalizedMessage() {
            return this.messageKey;
        }
    }

    private LegacyBombs() {
    }

    /** {@code world.getBlock(x, y, z) instanceof IBomb}. */
    public static boolean isBomb(ServerLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof DetonatableBlock detonatable) {
            return detonatable.kind() != DetonatableBlock.Kind.CORD;
        }
        return block instanceof LegacyNukeBlock
                || level.getBlockEntity(pos) instanceof NukeBoyBlockEntity
                || level.getBlockEntity(pos) instanceof LegacyNukeBlockEntity
                || block instanceof BombMultiBlock
                || block instanceof WallChargeBlock
                || block instanceof LandmineBlock
                || block instanceof CrashedBombBlock
                || block instanceof PlasticExplosiveBlock
                || block instanceof LegacyRedstoneBombBlock
                || block instanceof ThermalBombBlock;
    }

    /** {@code ((IBomb) block).explode(world, x, y, z)}; ERROR_NO_BOMB when the block is not an IBomb. */
    public static BombReturnCode explode(ServerLevel level, BlockPos pos, @Nullable Entity owner) {
        if (!isBomb(level, pos)) {
            return BombReturnCode.ERROR_NO_BOMB;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof NukeBoyBlockEntity nukeBoy) {
            if (!nukeBoy.isReady()) {
                return BombReturnCode.ERROR_MISSING_COMPONENT;
            }
            nukeBoy.detonate();
            return BombReturnCode.DETONATED;
        }
        if (entity instanceof LegacyNukeBlockEntity nuke) {
            if (!nuke.isReady()) {
                return BombReturnCode.ERROR_MISSING_COMPONENT;
            }
            nuke.detonate();
            return BombReturnCode.DETONATED;
        }
        if (block instanceof BombMultiBlock) {
            if (!(entity instanceof BombMultiBlockEntity bombMulti) || !bombMulti.isLoaded()) {
                return BombReturnCode.ERROR_MISSING_COMPONENT;
            }
            bombMulti.detonate();
            return BombReturnCode.DETONATED;
        }
        if (block instanceof WallChargeBlock charge) {
            WallChargeExplosions.detonate(level, pos, charge.kind());
        } else if (block instanceof DetonatableBlock detonatable) {
            detonatable.detonate(level, pos, owner);
        } else if (block instanceof LandmineBlock mine) {
            mine.detonate(level, pos);
        } else if (block instanceof CrashedBombBlock) {
            CrashedBombBlock.detonate(level, pos, state);
        } else if (block instanceof PlasticExplosiveBlock plastic) {
            plastic.detonate(level, pos, owner);
        } else if (block instanceof LegacyRedstoneBombBlock bomb) {
            bomb.detonate(level, pos);
        } else if (block instanceof ThermalBombBlock thermal) {
            thermal.detonate(level, pos, owner);
        } else {
            return BombReturnCode.ERROR_NO_BOMB;
        }
        return BombReturnCode.DETONATED;
    }
}

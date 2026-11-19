package com.atomiclegacy.blocks;

import com.atomiclegacy.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class WasteMyceliumBlock extends WasteEarthBlock {
    public WasteMyceliumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.7.10 did not let the mycelium block itself flood-fill nearby
        // terrain.  Spread is driven by HbmMushroomBlock and its config flag.
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        // [parity J1] WasteEarth.onEntityWalking (waste_mycelium): HbmPotion.radiation 30 * 20, amplifier 3.
        if (entity instanceof LivingEntity living && LegacyWalkingTrigger.onEntityWalking(entity)) {
            com.atomiclegacy.util.ContaminationUtil.addRadiationEffect(living, 30 * 20, 3);
        }
    }
}

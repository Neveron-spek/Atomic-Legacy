package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ToxicBlock extends Block {
    private static final Vec3 STUCK_MULTIPLIER = new Vec3(0.25D, 0.05D, 0.25D);

    public ToxicBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.makeStuckInBlock(state, STUCK_MULTIPLIER);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        // [parity K2: fluids] ToxicBlock.onEntityCollidedWithBlock: contaminate(RADIATION, CREATIVE, 1.0F)
        com.atomiclegacy.util.ContaminationUtil.contaminate(living, com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, 1.0F);
    }
}

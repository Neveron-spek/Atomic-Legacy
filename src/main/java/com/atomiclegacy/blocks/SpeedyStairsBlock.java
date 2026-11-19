package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Exact 1.7.10 BlockSpeedyStairs movement behavior. */
public final class SpeedyStairsBlock extends LegacyStairsBlock {
    private final double speed;

    public SpeedyStairsBlock(BlockState baseState, Properties properties, double speed) {
        super(baseState, properties);
        this.speed = speed;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.literal("Increases speed by "
                + net.minecraft.util.Mth.floor((speed - 1) * 100) + "%")
                .withStyle(net.minecraft.ChatFormatting.BLUE));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.zza != 0.0F || player.xxa != 0.0F) {
            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * speed, motion.y, motion.z * speed);
        }
    }
}

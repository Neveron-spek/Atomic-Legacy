package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * [parity V4: dig speed] 1.7.10 ForgeHooks.blockStrength = player.getBreakSpeed / hardness / (canHarvest ? 30 : 100),
 * with getBreakSpeed starting from the held item's getDigSpeed. Used where the 1.21 tool tags cannot give the 1.7.10
 * item speed without side effects (minecraft:leaves also speeds up hoes, minecraft:rails also pickaxes). The 1.21
 * Player.getDigSpeed chain (efficiency if speed > 1, haste, fatigue, block_break_speed, water, air) is kept:
 * only its starting item speed is replaced.
 */
public final class LegacyDigSpeed {
    private LegacyDigSpeed() {
    }

    public static float progress(BlockState state, Player player, BlockGetter level, BlockPos pos, float itemSpeed) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness == -1.0F) {
            return 0.0F;
        }
        int divisor = EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return speed(player, state, pos, itemSpeed) / hardness / divisor;
    }

    static float speed(Player player, BlockState state, BlockPos pos, float itemSpeed) {
        float efficiency = (float) player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        float vanillaItem = player.getInventory().getDestroySpeed(state);
        float vanillaStart = vanillaItem > 1.0F ? vanillaItem + efficiency : vanillaItem;
        float legacyStart = itemSpeed > 1.0F ? itemSpeed + efficiency : itemSpeed;
        if (vanillaStart <= 0.0F) {
            return 0.0F;
        }
        return player.getDigSpeed(state, pos) / vanillaStart * legacyStart;
    }
}

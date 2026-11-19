package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;

/** [parity R4: deco] 1.7.10 BlockPistonBase#determineOrientation (server-side player, posY at the feet). */
public final class LegacyPistonOrientation {
    private LegacyPistonOrientation() {
    }

    public static Direction of(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return Direction.NORTH;
        }
        BlockPos pos = context.getClickedPos();
        if (Mth.abs((float) player.getX() - pos.getX()) < 2.0F && Mth.abs((float) player.getZ() - pos.getZ()) < 2.0F) {
            double eye = player.getY() + 1.82D;
            if (eye - pos.getY() > 2.0D) {
                return Direction.UP;
            }
            if (pos.getY() - eye > 0.0D) {
                return Direction.DOWN;
            }
        }
        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (l) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }
}

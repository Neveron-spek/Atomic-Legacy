package com.atomiclegacy.blocks;

import com.atomiclegacy.items.ScrewdriverItem;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import com.atomiclegacy.tileentity.RbmkPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** [parity T2] onBlockActivated / IToolable.onScrew of RBMKMiniPanelBase and its subclasses. */
public final class RbmkPanelInteraction {
    private RbmkPanelInteraction() {
    }

    /** RBMKKeyPad, RBMKLever and RBMKTerminal.onBlockActivated; the other panels kept Block's {@code false}. */
    public static InteractionResult use(RbmkComponentBlockEntity rbmk, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        RbmkComponentBlock.Kind kind = rbmk.kind();
        if (kind == RbmkComponentBlock.Kind.KEY_PAD || kind == RbmkComponentBlock.Kind.LEVER) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) return InteractionResult.PASS;
            if (!(rbmk instanceof RbmkPanelBlockEntity panel)) return InteractionResult.PASS;
            Vec3 location = hit.getLocation();
            double hitX = location.x - pos.getX();
            double hitY = location.y - pos.getY();
            double hitZ = location.z - pos.getZ();
            Direction side = hit.getDirection();
            if (hitX != 0 && hitX != 1 && hitZ != 0 && hitZ != 1 && side != Direction.DOWN && side != Direction.UP) {
                Direction facing = panel.facing();
                int indexHit = 0;
                if (facing == Direction.NORTH && hitX < 0.5) indexHit = 1;
                if (facing == Direction.SOUTH && hitX > 0.5) indexHit = 1;
                if (facing == Direction.WEST && hitZ > 0.5) indexHit = 1;
                if (facing == Direction.EAST && hitZ < 0.5) indexHit = 1;
                if (kind == RbmkComponentBlock.Kind.KEY_PAD) {
                    if (hitY < 0.5) indexHit += 2;
                    if (!panel.keys[indexHit].active) return InteractionResult.PASS;
                    panel.keys[indexHit].click();
                } else {
                    if (!panel.levers[indexHit].active) return InteractionResult.PASS;
                    panel.levers[indexHit].click();
                }
            }
            return InteractionResult.CONSUME;
        }
        if (kind == RbmkComponentBlock.Kind.TERMINAL) {
            if (player.isShiftKeyDown()) return InteractionResult.PASS;
            if (level.isClientSide && rbmk instanceof RbmkPanelBlockEntity panel) {
                com.atomiclegacy.client.RbmkPanelClientHooks.open(panel);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * ItemTooling.onItemUse -> onScrew for RBMKGauge, RBMKKeyPad, RBMKLever, RBMKIndicator, RBMKNumitron and RBMKGraph.
     * Returns null when the clicked block is not one of these panels; PASS when the block's own activation runs first.
     */
    public static InteractionResult screw(ItemStack stack, Level level, Player player, InteractionHand hand, BlockPos pos) {
        if (ScrewdriverItem.isHandDrill(stack) || ScrewdriverItem.isDefuser(stack)) return null; // ToolType.SCREWDRIVER only
        if (!(level.getBlockEntity(pos) instanceof RbmkComponentBlockEntity rbmk)) return null;
        RbmkComponentBlock.Kind kind = rbmk.kind();
        switch (kind) {
            case KEY_PAD, LEVER -> {
                // onBlockActivated consumed every non-sneaking click before the item was used.
                if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            }
            case GAUGE, INDICATOR, NUMITRON, GRAPH -> { }
            case TERMINAL, DISPLAY_BLANK -> {
                return InteractionResult.PASS;
            }
            default -> {
                return null;
            }
        }
        if (!(rbmk instanceof RbmkPanelBlockEntity panel)) return null;
        if (level.isClientSide) {
            com.atomiclegacy.client.RbmkPanelClientHooks.open(panel);
        } else {
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

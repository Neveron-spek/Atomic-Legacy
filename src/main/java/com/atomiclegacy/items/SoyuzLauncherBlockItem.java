package com.atomiclegacy.items;

import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.blocks.SoyuzLauncherBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity P2: soyuz_launcher] SoyuzLauncher#onBlockPlacedBy: the core goes four blocks above
 * the clicked position, always facing EAST; the clicked position itself stays empty.
 */
public final class SoyuzLauncherBlockItem extends BlockItem {
    public SoyuzLauncherBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof SoyuzLauncherBlock machine)) return super.useOn(context);
        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos corePos = placeContext.getClickedPos().above(SoyuzLauncherBlock.HEIGHT);
        if (!level.getWorldBorder().isWithinBounds(corePos)
                || !level.getBlockState(corePos).canBeReplaced(placeContext)
                || !machine.canPlaceMachineFootprintAt(level, corePos, Direction.EAST, placeContext)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState state = machine.defaultBlockState().setValue(LargeMachineBlock.FACING, Direction.EAST);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        machine.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}

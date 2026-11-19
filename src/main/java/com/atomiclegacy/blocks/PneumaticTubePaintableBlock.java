package com.atomiclegacy.blocks;

import com.atomiclegacy.items.ScrewdriverItem;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * [parity V2: pneumatic_tube_paintable] PneumoTubePaintableBlock: an opaque full cube (Material.iron, default bounds)
 * with the TileEntityPneumoTube mechanics. Paint: a held ItemBlock accepted by BlockCablePaintable.allowedPaint is
 * applied when no paint is set (the item is not consumed); the hand drill removes the paint (onScrew returns false,
 * no tool damage); the defuser toggles metadata 0/1, i.e. hides the overlay pass (onScrew returns true, tool damage).
 * The screwdriver keeps the shared PneumoTube side cycling (ScrewdriverItem).
 */
public final class PneumaticTubePaintableBlock extends PneumaticTubeBlock {

    public PneumaticTubePaintableBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            if (allowedPaint(blockItem.getBlock(), this) && tube.paintBlock() == null) {
                if (!level.isClientSide) tube.setPaintBlock(blockItem.getBlock());
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (ScrewdriverItem.isHandDrill(stack)) {
            if (!level.isClientSide && tube.paintBlock() != null) tube.setPaintBlock(null);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide) {
                tube.toggleOverlayHidden();
                ScrewdriverItem.damageTool(stack, level, player, hand);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isScrewdriver(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION; // onBlockActivated returns false for tools
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** BlockCablePaintable.allowedPaint: no grass, only normal full blocks, not itself (same rule as red_cable_paintable). */
    public static boolean allowedPaint(Block paint, Block self) {
        if (paint == self || paint == Blocks.GRASS_BLOCK || paint.defaultBlockState().getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        return paint.defaultBlockState().isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}

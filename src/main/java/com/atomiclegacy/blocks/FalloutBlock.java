package com.atomiclegacy.blocks;

import com.atomiclegacy.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FalloutBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);

    public FalloutBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) {
            return false;
        }
        if (below.getBlock() instanceof LeavesBlock) {
            return true;
        }
        return below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        // [parity J1] BlockFallout.onEntityWalking: HbmPotion.radiation 10 * 60 * 20, amplifier 0, empty curative
        // item list (milk does not remove it); creative players are skipped.
        if (!level.isClientSide && entity instanceof LivingEntity living
                && !(living instanceof Player player && com.atomiclegacy.util.ContaminationUtil.isCreativeLike(player))
                && LegacyWalkingTrigger.onEntityWalking(entity)) {
            net.minecraft.world.effect.MobEffectInstance effect = new net.minecraft.world.effect.MobEffectInstance(
                    com.atomiclegacy.registry.HbmMobEffects.RADIATION, 10 * 60 * 20, 0);
            effect.getCures().clear();
            living.addEffect(effect);
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        super.attack(state, level, pos, player);
        // [parity J1] BlockFallout.onBlockClicked: HbmLivingProps.addCont(player, new ContaminationEffect(1F, 200, false)).
        if (!level.isClientSide) {
            com.atomiclegacy.util.ContaminationUtil.addContamination(player, 1.0F, 200, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

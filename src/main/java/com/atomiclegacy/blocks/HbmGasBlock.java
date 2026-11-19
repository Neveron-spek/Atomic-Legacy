package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public abstract class HbmGasBlock extends Block {
    protected HbmGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!tryMove(level, pos, firstDirection(level, pos, random))
                && !tryMove(level, pos, secondDirection(level, pos, random))) {
            level.scheduleTick(pos, this, delay(level, random));
        }
    }

    protected abstract Direction firstDirection(Level level, BlockPos pos, RandomSource random);

    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return firstDirection(level, pos, random);
    }

    protected int delay(Level level, RandomSource random) {
        return 2;
    }

    protected Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    protected boolean tryMove(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (level.isEmptyBlock(target)) {
            level.removeBlock(pos, false);
            level.setBlock(target, defaultBlockState(), 3);
            level.scheduleTick(target, this, delay(level, level.random));
            return true;
        }
        return false;
    }

    /**
     * [parity R1: ashglasses gas cloud] BlockGasBase.randomDisplayTick: only when the local player
     * (proxy.me()) wears ashglasses in the helmet slot, a "vanillaExt" cloud tinted with the gas
     * colour passed to the BlockGasBase constructor.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide || !net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return;
        }
        if (com.atomiclegacy.client.LocalPlayerGear.wearsOnHead(HbmItems.ASHGLASSES.get())) {
            float[] color = cloudColor();
            level.addParticle(HbmParticleTypes.GAS_CLOUD.get(),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    color[0], color[1], color[2]);
        }
    }

    /** [parity R1] The (red, green, blue) passed to the 1.7.10 BlockGasBase constructor. */
    protected float[] cloudColor() {
        return new float[] {0.7F, 0.8F, 0.6F};
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}

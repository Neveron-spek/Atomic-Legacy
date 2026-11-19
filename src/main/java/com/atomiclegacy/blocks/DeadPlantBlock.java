package com.atomiclegacy.blocks;

import com.mojang.serialization.MapCodec;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

public class DeadPlantBlock extends BushBlock {
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    private static final MapCodec<DeadPlantBlock> CODEC = simpleCodec(DeadPlantBlock::new);
    // [parity m1] BlockDeadPlant extends BlockEnumMulti (not BlockBush): default full-cube selection bounds
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.block();

    public DeadPlantBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(KIND, Kind.GENERIC));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // [parity m1] canPlaceBlockOn: grass, dirt, waste_earth, dirt_oily, dirt_dead
        return state.is(HbmBlocks.WASTE_EARTH.get())
                || state.is(HbmBlocks.DIRT_OILY.get())
                || state.is(HbmBlocks.DIRT_DEAD.get())
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** [parity m1] getPickBlock: damageDropped(meta) = meta. */
    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        if (this.asItem() instanceof com.atomiclegacy.items.LegacyVariantBlockItem item) {
            return com.atomiclegacy.items.LegacyVariantBlockItem.stackFor(item, state.getValue(KIND).ordinal());
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(KIND);
    }

    /** [parity m1] EnumDeadPlantType order = metadata / item variant index: GENERIC, GRASS, FLOWER, BIGFLOWER, FERN. */
    public enum Kind implements StringRepresentable {
        GENERIC,
        GRASS,
        FLOWER,
        BIG_FLOWER,
        FERN;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}

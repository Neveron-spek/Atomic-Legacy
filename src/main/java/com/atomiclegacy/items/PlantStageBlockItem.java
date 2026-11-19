package com.atomiclegacy.items;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * [parity d2: plants] Creative-only later growth stages of the 1.7.10 metadata plants: plant_flower CD1 and
 * plant_tall WEED/CD2/CD3/CD4. BlockTallPlant#canPlaceBlockAt needs air above; onBlockPlacedBy adds the upper half.
 */
public final class PlantStageBlockItem extends BlockItem {
    private final IntegerProperty ageProperty;
    private final int age;
    private final boolean tall;
    private final String descriptionId;

    public PlantStageBlockItem(Block block, IntegerProperty ageProperty, int age, boolean tall, String descriptionId,
                               Properties properties) {
        super(block, properties);
        this.ageProperty = ageProperty;
        this.age = age;
        this.tall = tall;
        this.descriptionId = descriptionId;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = this.getBlock().defaultBlockState()
                .setValue(this.ageProperty, this.age)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        if (this.tall && !context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            return null;
        }
        return this.canPlace(context, state) ? state : null;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (this.tall) {
            level.setBlock(pos.above(),
                    state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        }
        return level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public String getDescriptionId() {
        return this.descriptionId;
    }

    /** The base flower item stays the block's item (drops, pick block). */
    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
    }
}

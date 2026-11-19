package com.atomiclegacy.blocks;

import com.atomiclegacy.items.SellafieldBlockItem;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Radioactive Sellafield material with the six-stage 1.7.10 decay state. */
public final class SellafieldBlock extends Block {
    public static final int LEVELS = 6;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, LEVELS - 1);

    public SellafieldBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    // BlockSellafield: rad = 0.5F and "@Override public void onBlockAdded(...) { }" - unlike other BlockHazard blocks
    // the rad field is never replaced by 0.1 x item radiation and no update is scheduled; only random ticks emit
    // rad * (meta + 1).
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(LEVEL);
        ChunkRadiationData.get(level).incrementRadiation(pos, 0.5F * (stage + 1));
        if (random.nextInt(stage == 0 ? 25 : 15) != 0) {
            return;
        }
        if (stage == 0) {
            level.setBlock(pos, HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, state.setValue(LEVEL, stage - 1), Block.UPDATE_ALL);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        // [parity J1] BlockSellafield.onEntityWalking: HbmPotion.radiation 30 * 20, amplifier meta < 5 ? meta : meta * 2.
        if (!(entity instanceof LivingEntity living) || !LegacyWalkingTrigger.onEntityWalking(entity)) {
            return;
        }
        int meta = state.getValue(LEVEL);
        com.atomiclegacy.util.ContaminationUtil.addRadiationEffect(living, 30 * 20, meta < 5 ? meta : meta * 2);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (asItem() instanceof SellafieldBlockItem item) {
            return List.of(SellafieldBlockItem.stackFor(item, state.getValue(LEVEL)));
        }
        return super.getDrops(state, params);
    }
}

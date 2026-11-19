package com.atomiclegacy.blocks;

import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity b1: uranium ore outgas] 1.7.10 {@code BlockOutgas(Material.rock, true, 5, true)} of ore_uranium,
 * ore_uranium_scorched, ore_gneiss_uranium(_scorched) and ore_nether_uranium(_scorched), whose getGas() is gas_radon:
 * every random tick one of the six directions is picked and radon is placed there if it is air; harvesting the ore
 * (dropBlockAsItemWithChance, i.e. not creative and not silk touch) leaves radon in its place. The walking branch
 * of BlockOutgas only applies to asbestos gas.
 */
public class RadonOutgassingOreBlock extends Block {
    public RadonOutgassingOreBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos target = pos.relative(Direction.values()[random.nextInt(6)]);
        if (level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level.isClientSide || silkTouch(level, tool)) {
            return;
        }
        level.setBlock(pos, HbmBlocks.GAS_RADON.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean silkTouch(Level level, ItemStack tool) {
        return level.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(Enchantments.SILK_TOUCH))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, tool) > 0)
                .orElse(false);
    }
}

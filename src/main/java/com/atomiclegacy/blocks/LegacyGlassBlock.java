package com.atomiclegacy.blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/** Port of HBM's non-opaque BlockNTMGlass/BlockNTMGlassCT. */
public class LegacyGlassBlock extends Block {
    private final boolean dropsSelf;

    public LegacyGlassBlock(Properties properties, boolean dropsSelf) {
        super(properties);
        this.dropsSelf = dropsSelf;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean silk = tool != null && !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
                params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
        return dropsSelf || silk ? super.getDrops(state, params) : List.of();
    }

}

package com.atomiclegacy.blocks;

import com.atomiclegacy.tileentity.GlyphidSpawnerBlockEntity;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The three metadata variants of BlockGlyphidSpawner from 1.7.10. */
public final class GlyphidSpawnerBlock extends LegacyVariantBlock implements EntityBlock {
    public GlyphidSpawnerBlock(Properties properties) {
        super(properties, 2);
    }

    /** BlockGlyphidSpawner.quantityDropped: 1 + rand.nextInt(3) + fortune. */
    public static int eggCount(RandomSource random, int fortune) {
        return 1 + random.nextInt(3) + fortune;
    }

    /**
     * BlockGlyphidSpawner.getItemDropped / quantityDropped: glyphid eggs, never the block itself (a
     * BlockContainer cannot be silk-harvested). Explosions keep each egg with a 1 / size chance, as in
     * Block.dropBlockAsItemWithChance.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item egg = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("atomiclegacy", "egg_glyphid"));
        RandomSource random = params.getLevel().getRandom();
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        int fortune = tool == null || tool.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(
                params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), tool);
        Float explosionRadius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        int count = 0;
        for (int index = eggCount(random, fortune); index > 0; index--) {
            if (explosionRadius == null || random.nextFloat() <= 1.0F / explosionRadius) {
                count++;
            }
        }
        return count > 0 ? List.of(new ItemStack(egg, count)) : List.of();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlyphidSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.GLYPHID_SPAWNER.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) ->
                GlyphidSpawnerBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                        (GlyphidSpawnerBlockEntity) entity);
    }
}

package com.atomiclegacy.inventory.recipes;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.atomiclegacy.items.ModMinecartItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * [parity S1: carts] ToolRecipes: {@code addRecipe(fromOne(cart, CRATE), "C", "S", 'C', crate_steel, 'S', minecart)
 * .func_92100_c()}. func_92100_c makes ShapedRecipes copy the NBT of the last ingredient that has one onto the
 * result; here that is the custom data (crate contents) and the custom name of that ingredient.
 */
public class CrateCartRecipe extends ShapedRecipe {

    public CrateCartRecipe() {
        super("", CraftingBookCategory.MISC, ShapedRecipePattern.of(Map.of(
                'C', Ingredient.of(HbmBlocks.CRATE_STEEL.get()),
                'S', Ingredient.of(Items.MINECART)), "C", "S"), result());
    }

    private static ItemStack result() {
        // DictFrame.fromOne(ModItems.cart, EnumMinecart.CRATE): damage only, no cartBase tag (VANILLA)
        ItemStack stack = new ItemStack(HbmItems.CART.get());
        stack.set(com.atomiclegacy.items.LegacyItemComponents.CART_TYPE.get(), ModMinecartItem.EnumMinecart.CRATE.ordinal());
        return stack;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = result();
        for (int i = 0; i < input.size(); ++i) {
            ItemStack ingredient = input.getItem(i);
            if (!ingredient.isEmpty() && !ingredient.getComponentsPatch().isEmpty()) {
                result.remove(DataComponents.CUSTOM_DATA);
                result.remove(DataComponents.CUSTOM_NAME);
                if (ingredient.has(DataComponents.CUSTOM_DATA)) result.set(DataComponents.CUSTOM_DATA, ingredient.get(DataComponents.CUSTOM_DATA));
                if (ingredient.has(DataComponents.CUSTOM_NAME)) result.set(DataComponents.CUSTOM_NAME, ingredient.get(DataComponents.CUSTOM_NAME));
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CRATE_CART_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<CrateCartRecipe> {
        private static final MapCodec<CrateCartRecipe> CODEC = MapCodec.unit(CrateCartRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, CrateCartRecipe> STREAM_CODEC = StreamCodec.of((buffer, recipe) -> { }, buffer -> new CrateCartRecipe());

        @Override
        public MapCodec<CrateCartRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrateCartRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

package com.atomiclegacy.inventory.recipes.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.stream.Stream;

/** The original container{quantity}{fluid} ore-dictionary entry, including exact capacity. */
public record LegacyFluidContainerIngredient(String fluid, int amount) implements ICustomIngredient {
    public static final MapCodec<LegacyFluidContainerIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("fluid").forGetter(LegacyFluidContainerIngredient::fluid),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(LegacyFluidContainerIngredient::amount)
    ).apply(instance, LegacyFluidContainerIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        if (stack.getItem() instanceof HbmFluidContainerItem item) {
            return item.isFilledContainer() && item.kind().capacity() == amount
                    && HbmFluidContainerItem.fluid(stack).name().equals(fluid);
        }
        if (!HbmConfig.ENABLE_FLUID_CONTAINER_COMPAT.get()) {
            return false;
        }
        var definition = HbmFluids.byName(fluid);
        return definition.isPresent() && FluidUtil.getFluidContained(stack).filter(contained ->
                contained.getAmount() == amount && contained.is(HbmFluids.toNeoStack(definition.get(), amount).getFluid())
        ).isPresent();
    }

    @Override
    public Stream<ItemStack> getItems() {
        var definition = HbmFluids.byName(fluid);
        if (definition.isEmpty()) {
            return Stream.empty();
        }
        var stacks = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
        BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof HbmFluidContainerItem container
                        && container.isFilledContainer() && container.kind().capacity() == amount
                        && container.kind().allows(definition.get()))
                .map(item -> ((HbmFluidContainerItem) item).filledStack(definition.get()))
                .filter(this::test).forEach(stacks::add);
        return stacks.stream();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return HbmRecipeTypes.LEGACY_FLUID_CONTAINER_INGREDIENT.get();
    }
}

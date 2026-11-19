package com.atomiclegacy.inventory.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record CrystallizerRecipe(
        String group,
        Ingredient ingredient,
        int inputCount,
        AcidStack acid,
        int duration,
        float productivity, // [parity K3: crystallizer] CrystallizerRecipe.productivity (prod()), base of the EFFECT free chance
        ItemStack result
) implements Recipe<CrystallizerRecipe.Input> {
    public CrystallizerRecipe {
        if (result.isEmpty()) {
            result = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.item().getCount() >= this.inputCount
                && this.ingredient.test(input.item())
                && (this.acid.isEmpty()
                || input.acid().type() == this.acid.type()
                && input.acid().pressure() == this.acid.pressure()
                && input.acid().amount() >= this.acid.amount());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CRYSTALLIZER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CRYSTALLIZER.get();
    }

    public record AcidStack(HbmFluidDefinition type, int amount, int pressure) {
        private static final AcidStack EMPTY = new AcidStack(HbmFluids.none(), 0, 0);
        private static final Codec<AcidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(0, 1_000_000).fieldOf("amount").forGetter(AcidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(AcidStack::pressure)
        ).apply(instance, AcidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, AcidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public AcidStack decode(RegistryFriendlyByteBuf buffer) {
                HbmFluidDefinition fluid = HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none());
                int amount = buffer.readVarInt();
                int pressure = buffer.readVarInt();
                return new AcidStack(fluid, amount, pressure);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, AcidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        private static AcidStack fromName(String name, int amount, int pressure) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            return new AcidStack(fluid, amount, pressure);
        }

        public boolean isEmpty() {
            return this.type.isNone() || this.amount <= 0;
        }
    }

    public record Input(ItemStack item, HbmFluidStack acid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.item : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<CrystallizerRecipe> {
        private static final MapCodec<CrystallizerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CrystallizerRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrystallizerRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(CrystallizerRecipe::inputCount),
                AcidStack.CODEC.optionalFieldOf("acid", AcidStack.EMPTY).forGetter(CrystallizerRecipe::acid),
                Codec.intRange(1, 1_000_000).optionalFieldOf("duration", 600).forGetter(CrystallizerRecipe::duration),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("productivity", 0.0F).forGetter(CrystallizerRecipe::productivity),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrystallizerRecipe::result)
        ).apply(instance, CrystallizerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrystallizerRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group());
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                    buffer.writeVarInt(recipe.inputCount());
                    AcidStack.STREAM_CODEC.encode(buffer, recipe.acid());
                    buffer.writeVarInt(recipe.duration());
                    buffer.writeFloat(recipe.productivity());
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
                },
                buffer -> new CrystallizerRecipe(
                        ByteBufCodecs.STRING_UTF8.decode(buffer),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        AcidStack.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        buffer.readFloat(),
                        ItemStack.STREAM_CODEC.decode(buffer))
        );

        @Override
        public MapCodec<CrystallizerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrystallizerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

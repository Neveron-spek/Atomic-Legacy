package com.atomiclegacy.inventory.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.items.BlueprintItem;
import com.atomiclegacy.items.FluidIconItem;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ChemicalPlantRecipe(
        String group,
        List<String> blueprintPools,
        int duration,
        int power,
        ItemStack icon,
        List<CountedIngredient> inputItems,
        List<ChemicalFluidStack> inputFluids,
        List<ItemOutput> outputs,
        List<ChemicalFluidStack> outputFluids
) implements Recipe<ChemicalPlantRecipe.Input> {
    public static final int ITEM_LIMIT = 3;
    public static final int FLUID_LIMIT = 3;

    public ChemicalPlantRecipe {
        blueprintPools = List.copyOf(blueprintPools);
        inputItems = List.copyOf(inputItems);
        inputFluids = List.copyOf(inputFluids);
        outputs = List.copyOf(outputs);
        outputFluids = List.copyOf(outputFluids);
    }

    /** Display stacks of the item outputs (first possibility of each; identical to the output for plain stacks). */
    public List<ItemStack> outputItems() {
        List<ItemStack> stacks = new ArrayList<>(this.outputs.size());
        for (ItemOutput output : this.outputs) {
            stacks.add(output.pool().getFirst().stack());
        }
        return stacks;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return canCraft(input.items(), input.fluids(), this.inputItems, this.inputFluids);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.outputs.isEmpty() ? ItemStack.EMPTY : this.outputs.getFirst().pool().getFirst().stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputs.isEmpty() ? ItemStack.EMPTY : this.outputs.getFirst().pool().getFirst().stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient ingredient : this.inputItems) {
            list.add(ingredient.ingredient());
        }
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CHEMICAL_PLANT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CHEMICAL_PLANT.get();
    }

    public ItemStack displayIcon() {
        if (!this.icon.isEmpty()) {
            return this.icon.copy();
        }
        if (!this.outputs.isEmpty() && !this.outputs.getFirst().pool().getFirst().stack().isEmpty()) {
            return this.outputs.getFirst().pool().getFirst().stack().copy();
        }
        if (!this.outputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.outputFluids.getFirst().type());
        }
        if (!this.inputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.inputFluids.getFirst().type());
        }
        return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
    }

    public boolean isVisibleForPool(Optional<String> installedPool) {
        return BlueprintItem.isRecipeVisibleForPool(this.blueprintPools, installedPool);
    }

    public static boolean canCraft(
            List<ItemStack> itemStacks,
            List<HbmFluidStack> fluidStacks,
            List<CountedIngredient> itemInputs,
            List<ChemicalFluidStack> fluidInputs
    ) {
        if (itemInputs.size() > itemStacks.size() || fluidInputs.size() > fluidStacks.size()) {
            return false;
        }

        for (int slot = 0; slot < itemInputs.size(); slot++) {
            CountedIngredient ingredient = itemInputs.get(slot);
            ItemStack stack = itemStacks.get(slot);
            if (stack.isEmpty() || stack.getCount() < ingredient.count() || !ingredient.ingredient().test(stack)) {
                return false;
            }
        }

        for (int index = 0; index < fluidInputs.size(); index++) {
            ChemicalFluidStack required = fluidInputs.get(index);
            HbmFluidStack held = fluidStacks.get(index);
            if (held.isEmpty()
                    || held.type() != required.type()
                    || held.pressure() != required.pressure()
                    || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        private static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).fieldOf("count").forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CountedIngredient decode(RegistryFriendlyByteBuf buffer) {
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int count = buffer.readVarInt();
                return new CountedIngredient(ingredient, count);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient countedIngredient) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, countedIngredient.ingredient());
                buffer.writeVarInt(countedIngredient.count());
            }
        };
    }

    public record ChemicalFluidStack(HbmFluidDefinition type, int amount, int configuredPressure,
                                     boolean pressure528Only, String type528Name) {
        private static final Codec<ChemicalFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type.name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(ChemicalFluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(ChemicalFluidStack::configuredPressure),
                Codec.BOOL.optionalFieldOf("pressure_528_only", false).forGetter(ChemicalFluidStack::pressure528Only),
                Codec.STRING.optionalFieldOf("fluid_528", "").forGetter(ChemicalFluidStack::type528Name)
        ).apply(instance, ChemicalFluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, ChemicalFluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ChemicalFluidStack decode(RegistryFriendlyByteBuf buffer) {
                HbmFluidDefinition fluid = HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none());
                int amount = buffer.readVarInt();
                int pressure = buffer.readVarInt();
                // Recipe packets carry the server-resolved pressure so JEI and
                // selector screens cannot disagree with the server's 528 mode.
                return new ChemicalFluidStack(fluid, amount, pressure, false, "");
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ChemicalFluidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        private static ChemicalFluidStack fromName(String name, int amount, int pressure,
                                                   boolean pressure528Only, String type528Name) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            return new ChemicalFluidStack(fluid, amount, pressure, pressure528Only, type528Name);
        }

        /** Some old recipes replaced an input fluid entirely when 528 mode was active. */
        @Override
        public HbmFluidDefinition type() {
            if (!this.type528Name.isEmpty() && is528ModeEnabledSafely()) {
                return HbmFluids.byName(this.type528Name).orElse(this.type);
            }
            return this.type;
        }

        /**
         * HBM 1.7.10 only applies selected pressure requirements in 528 mode.
         * Resolve that condition at every machine/UI call site through the
         * existing pressure accessor, while retaining the configured value for
         * codec serialization and recipe synchronization.
         */
        public int pressure() {
            return this.pressure528Only && !is528ModeEnabledSafely() ? 0 : this.configuredPressure;
        }

        private static boolean is528ModeEnabledSafely() {
            try {
                return HbmConfig.pressurizedRecipes();
            } catch (IllegalStateException ignored) {
                return false;
            }
        }

        public boolean isEmpty() {
            return this.type.isNone() || this.amount <= 0;
        }
    }

    /**
     * GenericRecipes.IOutput of the 1.7.10 original: {@code multi == false} is a ChanceOutput (one pool entry),
     * {@code multi == true} is a ChanceOutputMulti (weighted pool of ChanceOutputs).
     * JSON: a plain item stack, {@code {"single": {stack, chance, weight}}} or {@code {"multi": [{stack, chance, weight}]}}.
     */
    public record ItemOutput(boolean multi, List<Entry> pool) {
        public record Entry(ItemStack stack, float chance, int weight) {
            private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.STRICT_CODEC.fieldOf("stack").forGetter(Entry::stack),
                    Codec.floatRange(0F, 1F).optionalFieldOf("chance", 1F).forGetter(Entry::chance),
                    Codec.intRange(0, 1_000_000).optionalFieldOf("weight", 0).forGetter(Entry::weight)
            ).apply(instance, Entry::new));

            /** ChanceOutput#collapse. */
            public ItemStack collapse(net.minecraft.util.RandomSource random) {
                if (this.chance >= 1F) {
                    return this.stack.copy();
                }
                int finalSize = 0;
                for (int i = 0; i < this.stack.getCount(); i++) {
                    if (random.nextFloat() <= this.chance) {
                        finalSize++;
                    }
                }
                return finalSize <= 0 ? ItemStack.EMPTY : this.stack.copyWithCount(finalSize);
            }
        }

        private static final Codec<List<Entry>> MULTI_CODEC = Entry.CODEC.listOf(1, 64).fieldOf("multi").codec();
        private static final Codec<Entry> SINGLE_CODEC = Entry.CODEC.fieldOf("single").codec();

        public static final Codec<ItemOutput> CODEC = Codec.either(ItemStack.STRICT_CODEC, Codec.either(MULTI_CODEC, SINGLE_CODEC)).xmap(
                either -> either.map(ItemOutput::of, inner -> inner.map(
                        pool -> new ItemOutput(true, pool), entry -> new ItemOutput(false, List.of(entry)))),
                output -> output.multi
                        ? com.mojang.datafixers.util.Either.right(com.mojang.datafixers.util.Either.left(output.pool))
                        : output.isPlain()
                        ? com.mojang.datafixers.util.Either.left(output.pool.getFirst().stack())
                        : com.mojang.datafixers.util.Either.right(com.mojang.datafixers.util.Either.right(output.pool.getFirst())));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ItemOutput decode(RegistryFriendlyByteBuf buffer) {
                boolean multi = buffer.readBoolean();
                int size = buffer.readVarInt();
                List<Entry> pool = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                    pool.add(new Entry(stack, buffer.readFloat(), buffer.readVarInt()));
                }
                return new ItemOutput(multi, pool);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ItemOutput output) {
                buffer.writeBoolean(output.multi);
                buffer.writeVarInt(output.pool.size());
                for (Entry entry : output.pool) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.stack());
                    buffer.writeFloat(entry.chance());
                    buffer.writeVarInt(entry.weight());
                }
            }
        };

        public ItemOutput {
            pool = List.copyOf(pool);
            if (pool.isEmpty() || (!multi && pool.size() != 1)) {
                throw new IllegalArgumentException("Invalid chemical plant item output pool");
            }
        }

        public static ItemOutput of(ItemStack stack) {
            return new ItemOutput(false, List.of(new Entry(stack, 1F, 0)));
        }

        private boolean isPlain() {
            return !this.multi && this.pool.getFirst().chance() >= 1F && this.pool.getFirst().weight() == 0;
        }

        /** IOutput#possibleMultiOutput: true for ChanceOutputMulti with a pool size above one. */
        public boolean possibleMultiOutput() {
            return this.multi && this.pool.size() > 1;
        }

        /** IOutput#getSingle: empty when the output has to be decided by weight. */
        public ItemStack getSingle() {
            return possibleMultiOutput() ? ItemStack.EMPTY : this.pool.getFirst().stack().copy();
        }

        public int totalWeight() {
            int total = 0;
            for (Entry entry : this.pool) {
                total += entry.weight();
            }
            return total;
        }

        /** IOutput#collapse; the multi branch is WeightedRandom.getRandomItem of 1.7.10. */
        public ItemStack collapse(net.minecraft.util.RandomSource random) {
            if (!this.multi) {
                return this.pool.getFirst().collapse(random);
            }
            int total = totalWeight();
            if (total <= 0) {
                throw new IllegalArgumentException();
            }
            int roll = random.nextInt(total);
            for (Entry entry : this.pool) {
                roll -= entry.weight();
                if (roll < 0) {
                    return entry.collapse(random);
                }
            }
            return ItemStack.EMPTY;
        }

        /** IOutput#getLabel without the colour codes: count, name and chance of every possible result. */
        public List<String> labels() {
            List<String> lines = new ArrayList<>();
            if (!this.multi) {
                Entry entry = this.pool.getFirst();
                lines.add(entry.stack().getCount() + "x " + entry.stack().getHoverName().getString()
                        + (entry.chance() >= 1F ? "" : " (" + (int) (entry.chance() * 1000) / 10F + "%)"));
                return lines;
            }
            int total = totalWeight();
            for (Entry entry : this.pool) {
                float chance = (float) entry.weight() / (float) total * entry.chance();
                lines.add(entry.stack().getCount() + "x " + entry.stack().getHoverName().getString()
                        + " (" + (int) (chance * 1000F) / 10F + "%)");
            }
            return lines;
        }
    }

    public record Input(List<ItemStack> items, List<HbmFluidStack> fluids) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<ChemicalPlantRecipe> {
        private static final MapCodec<ChemicalPlantRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ChemicalPlantRecipe::group),
                Codec.STRING.listOf().optionalFieldOf("blueprint_pools", List.of()).forGetter(ChemicalPlantRecipe::blueprintPools),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(ChemicalPlantRecipe::duration),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(ChemicalPlantRecipe::power),
                ItemStack.STRICT_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(ChemicalPlantRecipe::icon),
                CountedIngredient.CODEC.listOf().optionalFieldOf("input_items", List.of()).forGetter(ChemicalPlantRecipe::inputItems),
                ChemicalFluidStack.CODEC.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(ChemicalPlantRecipe::inputFluids),
                ItemOutput.CODEC.listOf().optionalFieldOf("output_items", List.of()).forGetter(ChemicalPlantRecipe::outputs),
                ChemicalFluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(ChemicalPlantRecipe::outputFluids)
        ).apply(instance, ChemicalPlantRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ChemicalPlantRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ChemicalPlantRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                List<String> blueprintPools = readStringList(buffer);
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                List<CountedIngredient> inputItems = readList(buffer, CountedIngredient.STREAM_CODEC);
                List<ChemicalFluidStack> inputFluids = readList(buffer, ChemicalFluidStack.STREAM_CODEC);
                List<ItemOutput> outputItems = readList(buffer, ItemOutput.STREAM_CODEC);
                List<ChemicalFluidStack> outputFluids = readList(buffer, ChemicalFluidStack.STREAM_CODEC);
                return new ChemicalPlantRecipe(group, blueprintPools, duration, power, icon, inputItems, inputFluids, outputItems, outputFluids);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ChemicalPlantRecipe recipe) {
                buffer.writeUtf(recipe.group());
                writeStringList(buffer, recipe.blueprintPools());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.icon());
                writeList(buffer, recipe.inputItems(), CountedIngredient.STREAM_CODEC);
                writeList(buffer, recipe.inputFluids(), ChemicalFluidStack.STREAM_CODEC);
                writeList(buffer, recipe.outputs(), ItemOutput.STREAM_CODEC);
                writeList(buffer, recipe.outputFluids(), ChemicalFluidStack.STREAM_CODEC);
            }
        };

        @Override
        public MapCodec<ChemicalPlantRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChemicalPlantRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            int size = buffer.readVarInt();
            List<T> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(codec.decode(buffer));
            }
            return List.copyOf(values);
        }

        private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            buffer.writeVarInt(values.size());
            for (T value : values) {
                codec.encode(buffer, value);
            }
        }

        private static List<String> readStringList(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<String> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(buffer.readUtf());
            }
            return List.copyOf(values);
        }

        private static void writeStringList(RegistryFriendlyByteBuf buffer, List<String> values) {
            buffer.writeVarInt(values.size());
            for (String value : values) {
                buffer.writeUtf(value);
            }
        }
    }

    public static Optional<ChemicalPlantRecipe> recipeValue(Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<ChemicalPlantRecipe>> holder) {
        return holder.map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }
}

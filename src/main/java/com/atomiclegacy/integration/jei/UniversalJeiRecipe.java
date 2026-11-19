package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * [parity S3: JEI] One NEIUniversalHandler.RecipeSet: input/output stacks and the machine shown at 75,31.
 * Kept free of JEI classes so the dedicated server diagnostics can inspect the same data.
 */
public record UniversalJeiRecipe(List<Slot> inputs, List<Slot> outputs, List<ItemStack> machines) {
    public UniversalJeiRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        machines = machines.stream().map(ItemStack::copy).toList();
    }

    /**
     * One PositionedStack. ItemFluidIcon stacks of the 1.7.10 handlers become JEI fluid stacks
     * ({@code pressure} is ItemFluidIcon's "PU" tag); {@code lines} are the lore lines ItemStackUtil.addTooltipToStack
     * attached to the whole slot, {@code itemLines} (parallel to {@code items}, may be empty) the lines of single
     * alternatives, e.g. the per-entry chance of a ChanceOutputMulti. [parity T4: JEI pressure/itemLines]
     */
    public record Slot(List<ItemStack> items, @Nullable HbmFluidDefinition fluid, int amount, int pressure,
                       List<Component> lines, List<List<Component>> itemLines) {
        public Slot {
            items = items.stream().map(ItemStack::copy).toList();
            lines = List.copyOf(lines);
            itemLines = itemLines.stream().map(List::copyOf).toList();
        }

        public Slot(List<ItemStack> items, @Nullable HbmFluidDefinition fluid, int amount, List<Component> lines) {
            this(items, fluid, amount, 0, lines, List.of());
        }

        public static Slot of(ItemStack... stacks) {
            return new Slot(List.of(stacks), null, 0, List.of());
        }

        public static Slot of(List<ItemStack> stacks) {
            return new Slot(stacks, null, 0, List.of());
        }

        public static Slot of(List<ItemStack> stacks, List<Component> lines) {
            return new Slot(stacks, null, 0, lines);
        }

        public static Slot fluid(HbmFluidDefinition fluid, int amount) {
            return new Slot(List.of(), fluid, amount, List.of());
        }

        /** ItemFluidIcon.make(fluid, amount, pressure). */
        public static Slot fluid(HbmFluidDefinition fluid, int amount, int pressure) {
            return new Slot(List.of(), fluid, amount, pressure, List.of(), List.of());
        }

        /** ChanceOutputMulti#getAllPossibilities: every alternative with its own red "x.y%" line (none at 100%). */
        public static Slot weighted(List<ItemStack> stacks, List<Integer> weights) {
            int total = 0;
            for (int weight : weights) total += weight;
            List<List<Component>> lines = new ArrayList<>();
            for (int weight : weights) {
                float chance = (float) weight / (float) total;
                lines.add(chance >= 1 ? List.of() : List.of(percent(chance)));
            }
            return new Slot(stacks, null, 0, 0, List.of(), lines);
        }

        /** GenericRecipes.ChanceOutput#getAllPossibilities: red "(int)(chance * 1000) / 10F %" below 100%. */
        public static Slot chance(ItemStack stack, float chance) {
            return new Slot(List.of(stack), null, 0, 0, chance >= 1F ? List.of() : List.of(percent(chance)), List.of());
        }

        static Component percent(float chance) {
            return Component.literal((int) (chance * 1000) / 10F + "%").withStyle(ChatFormatting.RED);
        }

        public boolean isEmpty() {
            return fluid == null && items.stream().allMatch(ItemStack::isEmpty);
        }
    }
}

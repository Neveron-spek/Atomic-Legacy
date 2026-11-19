package com.atomiclegacy.integration.jei;

import com.atomiclegacy.items.BlueprintItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * [parity T4: JEI] Base for categories whose 1.7.10 handler extends NEIGenericRecipeHandler (Assembly Machine,
 * Precision Assembler, Chemical Plant, Blast Furnace, PUREX, Rock Mill, Supercomputer, Space Assembler): gui_nei.png,
 * getInputSlotPositions/getOutputSlotPositions (+ the X offsets of AssemblyMachineRecipeHandler), item inputs before
 * fluid inputs, blueprint template at 75,10 with the tall frame, machine at 75,31 (75,38 with a template) and
 * GenericRecipe#printNEIExtras.
 */
public abstract class NeiGenericCategory<T> implements IRecipeCategory<T> {
    /** GenericRecipe#printNEIExtras variants. */
    public enum Extras { POWER, NO_POWER, PUREX }

    /**
     * One NEIGenericRecipeHandler.RecipeSet: inputs = inputItem then inputFluid, outputs = outputItem then outputFluid,
     * pools = GenericRecipe#getPools (empty if not pooled).
     */
    public record Layout(List<UniversalJeiRecipe.Slot> inputs, List<UniversalJeiRecipe.Slot> outputs, List<String> pools,
                         int duration, long power) {
    }

    private final RecipeType<T> type;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final List<ItemStack> machines;
    private final Extras extras;
    private final boolean assemblyOffsets;
    private final Map<T, Layout> layouts = Collections.synchronizedMap(new WeakHashMap<>());

    protected NeiGenericCategory(IGuiHelper helper, RecipeType<T> type, Component title, List<ItemStack> machines,
                                 Extras extras, boolean assemblyOffsets) {
        this.type = type;
        this.title = title;
        this.background = helper.createDrawable(UniversalJeiCategory.TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(machines.getFirst());
        this.machines = machines.stream().map(ItemStack::copy).toList();
        this.extras = extras;
        this.assemblyOffsets = assemblyOffsets;
    }

    protected abstract Layout layout(T recipe);

    public final Layout cached(T recipe) {
        return this.layouts.computeIfAbsent(recipe, this::layout);
    }

    /** ClientConfig.NEI_HIDE_SECRETS (default true): recipes in a "secret." pool are not listed. */
    public static <R> Predicate<R> notSecret(java.util.function.Function<R, List<String>> pools) {
        return recipe -> pools.apply(recipe).stream().noneMatch(pool -> pool.startsWith(BlueprintItem.POOL_PREFIX_SECRET));
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    private int inputOffset(int inputs) {
        return !this.assemblyOffsets ? 0 : inputs > 12 ? -9 : inputs > 9 ? 18 : 0;
    }

    /** AssemblyMachineRecipeHandler#getOffset (input count) for outputs and machine. */
    private int machineOffset(int inputs) {
        return !this.assemblyOffsets ? 0 : inputs > 12 ? 27 : inputs > 9 ? 18 : 0;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        Layout layout = cached(recipe);
        int inOff = inputOffset(layout.inputs().size());
        int off = machineOffset(layout.inputs().size());
        int[][] in = inputPositions(layout.inputs().size());
        for (int i = 0; i < in.length; i++) {
            UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.INPUT, in[i][0] + inOff, in[i][1]), layout.inputs().get(i));
        }
        int[][] out = outputPositions(layout.outputs().size());
        for (int i = 0; i < out.length; i++) {
            UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.OUTPUT, out[i][0] + off, out[i][1]), layout.outputs().get(i));
        }
        boolean template = !layout.pools().isEmpty();
        if (template) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 75 + off, 10)
                    .addItemStacks(layout.pools().stream().map(BlueprintItem::stackFor).toList());
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 75 + off, template ? 38 : 31).addItemStacks(this.machines);
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        Layout layout = cached(recipe);
        int inOff = inputOffset(layout.inputs().size());
        int off = machineOffset(layout.inputs().size());
        for (int[] pos : inputPositions(layout.inputs().size())) {
            graphics.blit(UniversalJeiCategory.TEXTURE, pos[0] + inOff - 1, pos[1] - 1, 5, 87, 18, 18);
        }
        for (int[] pos : outputPositions(layout.outputs().size())) {
            graphics.blit(UniversalJeiCategory.TEXTURE, pos[0] + off - 1, pos[1] - 1, 5, 87, 18, 18);
        }
        if (layout.pools().isEmpty()) {
            graphics.blit(UniversalJeiCategory.TEXTURE, 74 + off, 14, 59, 87, 18, 36);
        } else {
            graphics.blit(UniversalJeiCategory.TEXTURE, 74 + off, 7, 77, 87, 18, 50);
        }
        Font font = Minecraft.getInstance().font;
        int side = 164;
        switch (this.extras) {
            case POWER -> {
                NeiUniversalCategory.right(graphics, font, NeiUniversalCategory.shortNumber(layout.duration()) + " ticks", side, 45, 0x404040);
                NeiUniversalCategory.right(graphics, font, NeiUniversalCategory.shortNumber(layout.power()) + "HE/t", side, 57, 0x404040);
            }
            case NO_POWER -> NeiUniversalCategory.right(graphics, font, NeiUniversalCategory.shortNumber(layout.duration()) + " ticks", side, 45, 0x404040);
            case PUREX -> NeiUniversalCategory.right(graphics, font, NeiUniversalCategory.shortNumber(layout.duration()) + " ticks   "
                    + NeiUniversalCategory.shortNumber(layout.power()) + "HE/t", side, 57, 0x306030);
        }
    }

    /** NEIGenericRecipeHandler#getInputSlotPositions. */
    public static int[][] inputPositions(int count) {
        if (count == 1) return new int[][] {{48, 24}};
        if (count == 2) return new int[][] {{30, 24}, {48, 24}};
        if (count == 3) return new int[][] {{12, 24}, {30, 24}, {48, 24}};
        if (count == 4) return new int[][] {{30, 15}, {48, 15}, {30, 33}, {48, 33}};
        if (count == 5) return new int[][] {{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}};
        if (count == 6) return new int[][] {{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}, {48, 33}};
        int[][] slots = new int[count][2];
        int cols = (count + 2) / 3;
        for (int i = 0; i < count; i++) {
            slots[i][0] = 12 + (i % cols) * 18 - (cols == 4 ? 18 : 0);
            slots[i][1] = 6 + (i / cols) * 18;
        }
        return slots;
    }

    /** NEIGenericRecipeHandler#getOutputSlotPositions (counts above 8 collapse to 0,0 as in NEI). */
    public static int[][] outputPositions(int count) {
        if (count >= 1 && count <= 8) return UniversalJeiCategory.outputCoords(count);
        return new int[count][2];
    }
}

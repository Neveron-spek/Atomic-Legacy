package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * [parity S3: JEI] Exact NEIUniversalHandler layout: gui_nei.png at (5, 11, 166, 65), an 18x18 slot frame
 * (5, 87) behind every input/output position of getInputCoords/getOutputCoords, the operation frame
 * (59, 87, 18, 36) at 74, 14 and the machine stack at 75, 31.
 */
public final class UniversalJeiCategory implements IRecipeCategory<UniversalJeiRecipe> {
    static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/nei/gui_nei.png");

    private final RecipeType<UniversalJeiRecipe> type;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public UniversalJeiCategory(IGuiHelper helper, RecipeType<UniversalJeiRecipe> type, Component title, ItemStack icon) {
        this.type = type;
        this.title = title;
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(icon);
    }

    @Override
    public RecipeType<UniversalJeiRecipe> getRecipeType() {
        return this.type;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, UniversalJeiRecipe recipe, IFocusGroup focuses) {
        build(builder, recipe);
    }

    /** NEIUniversalHandler.RecipeSet positions. [parity T4: shared by NeiUniversalCategory] */
    public static void build(IRecipeLayoutBuilder builder, UniversalJeiRecipe recipe) {
        int[][] in = inputCoords(recipe.inputs().size());
        for (int i = 0; i < in.length; i++) {
            fill(builder.addSlot(RecipeIngredientRole.INPUT, in[i][0], in[i][1]), recipe.inputs().get(i));
        }
        int[][] out = outputCoords(recipe.outputs().size());
        for (int i = 0; i < out.length; i++) {
            fill(builder.addSlot(RecipeIngredientRole.OUTPUT, out[i][0], out[i][1]), recipe.outputs().get(i));
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31).addItemStacks(recipe.machines());
    }

    @Override
    public void draw(UniversalJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        drawFrames(graphics, recipe.inputs().size(), recipe.outputs().size());
    }

    /** NEIUniversalHandler#drawBackground after the base texture. */
    public static void drawFrames(GuiGraphics graphics, int inputs, int outputs) {
        for (int[] pos : inputCoords(inputs)) {
            graphics.blit(TEXTURE, pos[0] - 1, pos[1] - 1, 5, 87, 18, 18);
        }
        for (int[] pos : outputCoords(outputs)) {
            graphics.blit(TEXTURE, pos[0] - 1, pos[1] - 1, 5, 87, 18, 18);
        }
        graphics.blit(TEXTURE, 74, 14, 59, 87, 18, 36);
    }

    static void fill(IRecipeSlotBuilder slot, UniversalJeiRecipe.Slot content) {
        if (content.fluid() != null) {
            Fluid fluid = HbmFluids.toNeoFluid(content.fluid());
            if (fluid != Fluids.EMPTY) {
                long amount = Math.max(1, content.amount());
                slot.setFluidRenderer(amount, false, 16, 16).addFluidStack(fluid, amount);
                if (content.pressure() > 0) { // [parity T4: ItemFluidIcon#addInformation pressure lines]
                    int pressure = content.pressure();
                    slot.addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.literal(pressure + "PU").withStyle(ChatFormatting.RED));
                        tooltip.add(Component.literal("Pressurized, use compressor!").withStyle(
                                System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.RED : ChatFormatting.DARK_RED));
                    });
                }
                HbmJeiFluidTooltips.addTo(slot);
            }
        } else {
            slot.addItemStacks(content.items());
        }
        if (!content.lines().isEmpty()) {
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.addAll(content.lines()));
        }
        if (!content.itemLines().isEmpty()) { // [parity T4: per-alternative chance lines]
            slot.addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(shown -> {
                for (int i = 0; i < content.items().size() && i < content.itemLines().size(); i++) {
                    if (ItemStack.matches(content.items().get(i), shown)) {
                        tooltip.addAll(content.itemLines().get(i));
                        return;
                    }
                }
            }));
        }
    }

    /** NEIUniversalHandler#getInputCoords. */
    public static int[][] inputCoords(int count) {
        return switch (count) {
            case 1 -> new int[][] {{48, 24}};
            case 2 -> new int[][] {{48, 24}, {30, 24}};
            case 3 -> new int[][] {{48, 24}, {30, 24}, {12, 24}};
            case 4 -> new int[][] {{48, 24 - 9}, {30, 24 - 9}, {48, 24 + 9}, {30, 24 + 9}};
            case 5 -> new int[][] {{48, 24 - 9}, {30, 24 - 9}, {12, 24}, {48, 24 + 9}, {30, 24 + 9}};
            case 6 -> new int[][] {{48, 24 - 9}, {30, 24 - 9}, {12, 24 - 9}, {48, 24 + 9}, {30, 24 + 9}, {12, 24 + 9}};
            case 7 -> new int[][] {{48, 24 - 18}, {30, 24 - 9}, {12, 24 - 9}, {48, 24}, {30, 24 + 9}, {12, 24 + 9}, {48, 24 + 18}};
            case 8 -> new int[][] {{48, 24 - 18}, {30, 24 - 18}, {12, 24 - 9}, {48, 24}, {30, 24}, {12, 24 + 9}, {48, 24 + 18}, {30, 24 + 18}};
            case 9 -> new int[][] {{48, 24 - 18}, {30, 24 - 18}, {12, 24 - 18}, {48, 24}, {30, 24}, {12, 24}, {48, 24 + 18}, {30, 24 + 18}, {12, 24 + 18}};
            default -> grid(count);
        };
    }

    /** NEIUniversalHandler#getOutputCoords. */
    public static int[][] outputCoords(int count) {
        return switch (count) {
            case 1 -> new int[][] {{102, 24}};
            case 2 -> new int[][] {{102, 24}, {120, 24}};
            case 3 -> new int[][] {{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][] {{102, 24 - 9}, {120, 24 - 9}, {102, 24 + 9}, {120, 24 + 9}};
            case 5 -> new int[][] {{102, 24 - 9}, {120, 24 - 9}, {102, 24 + 9}, {120, 24 + 9}, {138, 24}};
            case 6 -> new int[][] {{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}};
            case 7 -> new int[][] {{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}};
            case 8 -> new int[][] {{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}, {138, 42}};
            default -> grid(count);
        };
    }

    private static int[][] grid(int count) {
        int[][] slots = new int[count][2];
        for (int i = 0; i < count; i++) {
            slots[i] = new int[] {i % 4 * 18, i / 4 * 18};
        }
        return slots;
    }
}

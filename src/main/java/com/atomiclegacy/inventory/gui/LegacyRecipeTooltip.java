package com.atomiclegacy.inventory.gui;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.atomiclegacy.client.search.JechSearchCompat;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * [parity K1] GenericRecipe#print building blocks and GUIElements#drawHoveringTextRecipe of 1.7.10: the recipe
 * tooltip shown by GUIScreenRecipeSelector and the machine GUIs (GuiInfoContainerProcessor, GUICrucible,
 * GUIFusionTorus, GUIMachinePlasmaForge).
 */
public final class LegacyRecipeTooltip {
    /** GUIElements.STANDARD_COLOR_BACKGROUND (-0xFEFFFF0). */
    private static final int COLOR_BACKGROUND = 0xF0100010;
    private static final int RECIPE_COLOR_LINE0 = 0xFFFF8000;
    private static final int RECIPE_COLOR_LINE1 = 0xFFFFFF00;
    private static final int HEADER_OFFSET = 6;
    private static final int LINE_DIST = 10;

    private LegacyRecipeTooltip() {
    }

    /** GUIElements#drawHoveringTextRecipe(lines, x, y, font, itemRender, width, height). */
    public static void render(GuiGraphics graphics, Font font, List<Component> lines, int x, int y) {
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int boundX = x + 12;
        int boundY = y - 12;
        int height = 6 + HEADER_OFFSET;
        if (lines.size() > 1) {
            height += 2 + (lines.size() - 1) * LINE_DIST;
        }
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        if (boundX + width + 4 > guiWidth) boundX -= 28 + width;
        if (boundY + height + 6 > guiHeight) boundY = guiHeight - height - 6;
        if (boundX < 4) boundX = 4;
        if (boundY < 4) boundY = 4;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        int bg = COLOR_BACKGROUND;
        graphics.fillGradient(boundX - 3, boundY - 4, boundX + width + 3, boundY - 3, bg, bg);
        graphics.fillGradient(boundX - 3, boundY + height + 3, boundX + width + 3, boundY + height + 4, bg, bg);
        graphics.fillGradient(boundX - 3, boundY - 3, boundX + width + 3, boundY + height + 3, bg, bg);
        graphics.fillGradient(boundX - 4, boundY - 3, boundX - 3, boundY + height + 3, bg, bg);
        graphics.fillGradient(boundX + width + 3, boundY - 3, boundX + width + 4, boundY + height + 3, bg, bg);
        graphics.fillGradient(boundX - 3, boundY - 3 + 1, boundX - 3 + 1, boundY + height + 3 - 1, RECIPE_COLOR_LINE0, RECIPE_COLOR_LINE1);
        graphics.fillGradient(boundX + width + 2, boundY - 3 + 1, boundX + width + 3, boundY + height + 3 - 1, RECIPE_COLOR_LINE0, RECIPE_COLOR_LINE1);
        graphics.fillGradient(boundX - 3, boundY - 3, boundX + width + 3, boundY - 3 + 1, RECIPE_COLOR_LINE0, RECIPE_COLOR_LINE0);
        graphics.fillGradient(boundX - 3, boundY + height + 2, boundX + width + 3, boundY + height + 3, RECIPE_COLOR_LINE1, RECIPE_COLOR_LINE1);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), boundX, boundY, -1, true);
            if (i == 0) boundY += HEADER_OFFSET;
            boundY += LINE_DIST;
        }
        graphics.pose().popPose();
    }

    /** GuiScreen#drawCreativeTabHoveringText with YELLOW gui.recipe.setRecipe (no recipe selected). */
    public static List<Component> setRecipe() {
        return List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW));
    }

    /** GenericRecipe#matchesSearch: localized name substring only. */
    public static boolean matchesSearch(Component localizedName, String normalizedQuery) {
        return normalizedQuery.isBlank() || JechSearchCompat.contains(localizedName.getString(), normalizedQuery);
    }

    public static boolean leftShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getWindow() != null && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    /** GenericRecipe#header. */
    public static void header(List<Component> list, Component localizedName, String internalName) {
        list.add(localizedName.copy().withStyle(ChatFormatting.YELLOW));
        if (leftShiftDown()) {
            list.add(Component.literal("Internal: " + internalName).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** GenericRecipe#autoSwitch: I18nUtil.resolveKeyArray("autoswitch", resolveKey(group)) in gold. */
    public static void autoSwitch(List<Component> list, String autoSwitchGroup) {
        if (autoSwitchGroup == null || autoSwitchGroup.isBlank()) {
            return;
        }
        String text = Component.translatable("autoswitch", Component.translatable(autoSwitchGroup).getString()).getString();
        for (String line : text.split("\\$")) {
            list.add(Component.literal(line).withStyle(ChatFormatting.GOLD));
        }
    }

    /** GenericRecipe#duration. */
    public static void duration(List<Component> list, int duration) {
        if (duration > 0) {
            list.add(Component.translatable("gui.recipe.duration").append(": " + (duration / 20D) + "s").withStyle(ChatFormatting.RED));
        }
    }

    /** GenericRecipe#power. */
    public static void power(List<Component> list, long power) {
        if (power > 0) {
            list.add(Component.translatable("gui.recipe.consumption").append(": " + K3GuiText.shortNumber(power) + "HE/t").withStyle(ChatFormatting.RED));
        }
    }

    public static void inputHeader(List<Component> list) {
        list.add(Component.translatable("gui.recipe.input").append(":").withStyle(ChatFormatting.BOLD));
    }

    public static void outputHeader(List<Component> list) {
        list.add(Component.translatable("gui.recipe.output").append(":").withStyle(ChatFormatting.BOLD));
    }

    /** AStack#extractForCyclingDisplay(20): one alternative per second. */
    public static ItemStack cycling(Ingredient ingredient, int count) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        int cycle = 20 * 50;
        ItemStack display = stacks[(int) (System.currentTimeMillis() % ((long) cycle * stacks.length) / cycle)].copy();
        display.setCount(count);
        return display;
    }

    /** GenericRecipe#setIconToFirstIngredient: inputItem[0].extractForNEI().get(0). */
    public static ItemStack firstItem(Ingredient ingredient, int count) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack icon = stacks[0].copy();
        icon.setCount(count);
        return icon;
    }

    /** GenericRecipe#input item line: "  " GRAY count "x " name. */
    public static void inputItem(List<Component> list, Ingredient ingredient, int count) {
        ItemStack display = cycling(ingredient, count);
        Component name = display.isEmpty() ? Component.translatable("item.atomiclegacy.nothing") : display.getHoverName();
        list.add(Component.literal("  ").append(Component.literal(count + "x ").append(name).withStyle(ChatFormatting.GRAY)));
    }

    /** GenericRecipe#input / #output fluid line: "  " BLUE fill "mB " name [" at " RED pressure " PU"]. */
    public static void fluid(List<Component> list, HbmFluidDefinition type, int amount, int pressure) {
        MutableComponent line = Component.literal(amount + "mB ").append(Component.translatable(type.translationKey()));
        if (pressure != 0) {
            line.append(" ").append(Component.translatable("gui.recipe.atPressure")).append(" ")
                    .append(Component.literal(pressure + " PU").withStyle(ChatFormatting.RED));
        }
        list.add(Component.literal("  ").append(line.withStyle(ChatFormatting.BLUE)));
    }

    /** ChanceOutput#getLabel prefixed by GenericRecipe#output: "  " GRAY count "x " name [" (chance%)"]. */
    public static void chanceOutput(List<Component> list, ItemStack stack, float chance) {
        MutableComponent line = Component.literal(stack.getCount() + "x ").append(stack.getHoverName());
        if (chance < 1F) {
            line.append(" (" + (int) (chance * 1000) / 10F + "%)");
        }
        list.add(Component.literal("  ").append(line.withStyle(ChatFormatting.GRAY)));
    }

    /** ChanceOutputMulti#getLabel prefixed by GenericRecipe#output. */
    public static void multiOutput(List<Component> list, List<ItemStack> stacks, List<Integer> weights, List<Float> chances) {
        list.add(Component.literal("  One of:"));
        int total = 0;
        for (int weight : weights) total += weight;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            float chance = (float) weights.get(i) / (float) total * (chances == null ? 1F : chances.get(i));
            list.add(Component.literal("    ").append(Component.literal(stack.getCount() + "x ").append(stack.getHoverName())
                    .append(" (" + (int) (chance * 1000F) / 10F + "%)").withStyle(ChatFormatting.GRAY)));
        }
    }
}

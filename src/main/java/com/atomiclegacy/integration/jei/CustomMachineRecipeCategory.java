package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.config.CustomMachineConfigJSON.MachineConfiguration;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes.ChanceStack;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [parity X1: custom machines] CustomMachineHandler: one category per configuration (NEIConfig registers a handler for
 * every niceList entry), titled with localizedName, gui_nei_custom.png; fluids at 12 + i * 18, 6, items at 12 + i * 18,
 * 24 / 42, outputs at 102 + i * 18 (6 / 24 / 42), the controller at 75,42; drawExtras: radiation (green, right-aligned to
 * 160, y 63), pollution (0x404040, y 75), flux (centered on 83, y 16) and heat (red, centered on 83, y 8).
 * Also adds the controllers' core recipes to the crafting category (they are matched by a special recipe).
 */
public final class CustomMachineRecipeCategory extends NeiTemplateCategory<CustomMachineRecipe> {
    private static final Map<MachineConfiguration, RecipeType<CustomMachineRecipe>> TYPES = new HashMap<>();
    private final MachineConfiguration conf;

    private CustomMachineRecipeCategory(IGuiHelper helper, MachineConfiguration conf) {
        super(helper, type(conf), Component.literal(conf.localizedName), CustomMachineConfigJSON.stackFor(conf), "gui_nei_custom");
        this.conf = conf;
    }

    private static RecipeType<CustomMachineRecipe> type(MachineConfiguration conf) {
        return TYPES.computeIfAbsent(conf, c -> RecipeType.create(AtomicLegacy.MOD_ID, "custom_machine_" + c.id(), CustomMachineRecipe.class));
    }

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        TYPES.clear();
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            registration.addRecipeCategories(new CustomMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper(), conf));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            List<CustomMachineRecipe> recipes = CustomMachineRecipes.recipes.get(conf.recipeKey);
            if (recipes != null) registration.addRecipes(type(conf), recipes);
        }
        List<RecipeHolder<CraftingRecipe>> core = new ArrayList<>();
        for (Map.Entry<MachineConfiguration, ShapedRecipe> entry : CustomMachineConfigJSON.coreRecipes.entrySet()) {
            core.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, "custom_machine/" + entry.getKey().id()), entry.getValue()));
        }
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.CRAFTING, core);
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
            registration.addRecipeCatalyst(CustomMachineConfigJSON.stackFor(conf), type(conf));
        }
    }

    private static ItemStack withChance(ChanceStack pair, List<Component> lines) {
        if (pair.chance() != 1) {
            lines.add(Component.literal(ChatFormatting.RED + "" + (((int) (pair.chance() * 1000)) / 10D) + "%"));
        }
        return pair.stack().copy();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CustomMachineRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < 3; i++) if (recipe.inputFluids.length > i) UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.INPUT, 12 + i * 18, 6),
                UniversalJeiRecipe.Slot.fluid(recipe.inputFluids[i].type(), recipe.inputFluids[i].amount(), recipe.inputFluids[i].pressure()));
        for (int i = 0; i < 3; i++) if (recipe.inputItems.length > i) builder.addSlot(RecipeIngredientRole.INPUT, 12 + i * 18, 24).addItemStacks(recipe.inputItems[i].extractForNEI());
        for (int i = 3; i < 6; i++) if (recipe.inputItems.length > i) builder.addSlot(RecipeIngredientRole.INPUT, 12 + (i - 3) * 18, 42).addItemStacks(recipe.inputItems[i].extractForNEI());

        for (int i = 0; i < 3; i++) if (recipe.outputFluids.length > i) UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.OUTPUT, 102 + i * 18, 6),
                UniversalJeiRecipe.Slot.fluid(recipe.outputFluids[i].type(), recipe.outputFluids[i].amount(), recipe.outputFluids[i].pressure()));

        for (int i = 0; i < 6; i++) if (recipe.outputItems.length > i) {
            List<Component> lines = new ArrayList<>();
            ItemStack out = withChance(recipe.outputItems[i], lines);
            UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.OUTPUT, 102 + (i % 3) * 18, i < 3 ? 24 : 42), UniversalJeiRecipe.Slot.of(List.of(out), lines));
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 42).addItemStack(CustomMachineConfigJSON.stackFor(conf));
    }

    /** GuiDraw.drawString(text, x, y, colour) draws with shadow. */
    @Override
    public void draw(CustomMachineRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int side = 83;
        if (recipe.radiationAmount != 0) {
            String radiation = "Radiation:" + recipe.radiationAmount + "";
            graphics.drawString(font, radiation, 160 - font.width(radiation), 63, 0x08FF00, true);
        }
        if (recipe.pollutionAmount != 0) {
            String pollution = recipe.pollutionType + ":" + recipe.pollutionAmount + "";
            graphics.drawString(font, pollution, 160 - font.width(pollution), 75, 0x404040, true);
        }
        if (conf.fluxMode) {
            String flux = "Flux:" + recipe.flux + "";
            graphics.drawString(font, flux, side - font.width(flux) / 2, 16, 0x08FF00, true);
        }
        int heat = conf.maxHeat > 0 && recipe.heat > 0 ? recipe.heat : 0;
        if (conf.maxHeat > 0 && heat > 0) {
            String text = "Heat:" + heat + "";
            graphics.drawString(font, text, side - font.width(text) / 2, 8, 0xFF0000, true);
        }
    }
}

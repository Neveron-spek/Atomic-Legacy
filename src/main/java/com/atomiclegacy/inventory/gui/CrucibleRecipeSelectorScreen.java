package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.CrucibleMenu;
import com.atomiclegacy.inventory.recipes.CrucibleRecipe;
import com.atomiclegacy.items.ScrapsItem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for CrucibleRecipes. */
public class CrucibleRecipeSelectorScreen extends LegacyRecipeSelectorScreen<CrucibleRecipe> {
    private final CrucibleMenu menu;

    public CrucibleRecipeSelectorScreen(CrucibleScreen previousScreen, CrucibleMenu menu) {
        super(Component.translatable("container.atomiclegacy.machine_crucible.recipe_selector"), previousScreen,
                menu.crucibleRecipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<CrucibleRecipe> holder) {
        return holder.value().icon().copy();
    }

    @Override
    protected Component localizedName(RecipeHolder<CrucibleRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<CrucibleRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    /** GenericRecipe#getLocalizedName: CrucibleRecipes are not setNamed, so the icon name is shown. */
    public static Component localizedNameOf(RecipeHolder<CrucibleRecipe> holder) {
        return holder.value().icon().getHoverName();
    }

    /** [parity K1] CrucibleRecipe#print: header, input, output (no duration/power). */
    public static List<Component> describeRecipe(RecipeHolder<CrucibleRecipe> holder) {
        CrucibleRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder), recipe.group());
        LegacyRecipeTooltip.inputHeader(lines);
        for (CrucibleRecipe.MaterialIngredient ingredient : recipe.input()) {
            lines.add(materialLine(ingredient));
        }
        LegacyRecipeTooltip.outputHeader(lines);
        for (CrucibleRecipe.MaterialIngredient ingredient : recipe.output()) {
            lines.add(materialLine(ingredient));
        }
        return lines;
    }

    /** resolveKey(material) + ": " + Mats.formatAmount(amount, LSHIFT). */
    private static Component materialLine(CrucibleRecipe.MaterialIngredient ingredient) {
        Component amount = LegacyRecipeTooltip.leftShiftDown()
                ? Component.literal((ingredient.amount() * 2) + "mB")
                : ScrapsItem.formatAmountComponent(ingredient.amount());
        return Component.translatable(ingredient.material().translationKey()).append(": ").append(amount);
    }
}

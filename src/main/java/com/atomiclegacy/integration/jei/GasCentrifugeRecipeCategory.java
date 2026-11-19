package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.GasCentrifugeRecipe;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity T4: JEI] GasCentrifugeRecipeHandler over GasCentrifugeRecipes.getGasCentrifugeRecipes(): gui_nei_centrifuge_gas.png,
 * fluid input 47,24, four outputs (padded with "nothing") at 129/147 x 15/33, battery (MachineRecipes#getBatteries) at 3,42,
 * power bar drawProgressBar(3, 6, 176, 0, 16, 34, 480, 7), progress drawProgressBar(74, 17, 208, 0, 44, 37, 80 or 150, 0),
 * the high speed overlay (25, 24, 192, 0, 16, 16) and the green "N G. Cents" label.
 */
public class GasCentrifugeRecipeCategory extends NeiTemplateCategory<RecipeHolder<GasCentrifugeRecipe>> {
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressFast;
    private final IDrawableAnimated progressSlow;

    public GasCentrifugeRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.GAS_CENTRIFUGE, Component.literal("Gas Centrifuge"), new ItemStack(HbmBlocks.MACHINE_GASCENT.get()),
                "gui_nei_centrifuge_gas");
        this.powerBar = progress(helper, 176, 0, 16, 34, 480, 7);
        this.progressFast = progress(helper, 208, 0, 44, 37, 150 - 70, 0);
        this.progressSlow = progress(helper, 208, 0, 44, 37, 150, 0);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GasCentrifugeRecipe> holder, IFocusGroup focuses) {
        GasCentrifugeRecipe recipe = holder.value();
        UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.INPUT, 47, 24),
                UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), recipe.input().amount()));
        builder.addSlot(RecipeIngredientRole.CATALYST, 3, 42).addItemStacks(batteries());
        ItemStack nothing = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("nothing")));
        for (int i = 0; i < 4; i++) {
            ItemStack out = i < recipe.results().size() ? recipe.results().get(i).copy() : nothing.copy();
            builder.addSlot(RecipeIngredientRole.OUTPUT, i % 2 == 0 ? 129 : 147, i < 2 ? 15 : 33).addItemStack(out);
        }
    }

    @Override
    public void draw(RecipeHolder<GasCentrifugeRecipe> holder, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        boolean highSpeed = holder.value().highSpeed();
        if (highSpeed) {
            graphics.blit(this.texture, 25, 24, 192, 0, 16, 16);
        }
        this.powerBar.draw(graphics, 3, 6);
        (highSpeed ? this.progressFast : this.progressSlow).draw(graphics, 74, 17);
        Font font = Minecraft.getInstance().font;
        String centrifuges = GasCentrifugeJeiRecipes.centrifuges(holder.id()) + " G. Cents";
        graphics.drawString(font, centrifuges, 50 - font.width(centrifuges) / 2, 10, 65280, false);
    }

    /** MachineRecipes#getBatteries: every EnumBatteryPack, every EnumBatterySC, battery_creative. */
    private static List<ItemStack> batteries() {
        List<ItemStack> list = new ArrayList<>();
        for (String id : List.of("battery_redstone", "battery_lead", "battery_lithium", "battery_sodium", "battery_schrabidium", "battery_quantum")) {
            list.add(LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, id));
        }
        list.add(new ItemStack(HbmItems.CAPACITOR_COPPER.get()));
        list.add(new ItemStack(HbmItems.CAPACITOR_GOLD.get()));
        list.add(new ItemStack(HbmItems.CAPACITOR_NIOBIUM.get()));
        list.add(new ItemStack(HbmItems.CAPACITOR_TANTALUM.get()));
        list.add(new ItemStack(HbmItems.CAPACITOR_BISMUTH.get()));
        list.add(new ItemStack(HbmItems.CAPACITOR_SPARK.get()));
        for (String id : List.of("empty", "waste", "ra226", "tc99", "co60", "pu238", "po210", "au198", "pb209", "am241")) {
            list.add(LegacyVariantItem.stackFor(HbmItems.BATTERY_SC, id));
        }
        list.add(new ItemStack(HbmItems.BATTERY_CREATIVE.get()));
        return list;
    }
}

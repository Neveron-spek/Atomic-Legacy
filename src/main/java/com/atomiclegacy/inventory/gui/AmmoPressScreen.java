package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.AmmoPressBlockEntity;
import com.atomiclegacy.inventory.gui.widget.SearchEditBox;
import com.atomiclegacy.inventory.container.AmmoPressMenu;
import com.atomiclegacy.inventory.recipes.AmmoPressRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AmmoPressScreen extends AbstractContainerScreen<AmmoPressMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_ammo_press.png");
    private static final float GHOST_ALPHA = 0.5F; // [parity K3: the ghost item is covered by the slot texture at alpha 0.5]
    private static final int RECIPES_PER_PAGE = 12;

    private SearchEditBox search;
    private List<RecipeHolder<AmmoPressRecipe>> filteredRecipes = List.of();
    private int page;

    public AmmoPressScreen(AmmoPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.search = new SearchEditBox(this.font, this.leftPos + 10, this.topPos + 75, 66, 12, Component.translatable("container.atomiclegacy.ammo_press.search"));
        this.search.setBordered(false);
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setMaxLength(25); // [parity g1: GUIMachineAmmoPress.initGui]
        this.search.setResponder(text -> {
            this.page = 0;
            refreshRecipes();
        });
        addRenderableWidget(this.search);
        refreshRecipes();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshRecipes();
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        // [parity K3: recipe tooltips are skipped while the mouse is over a filled slot]
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        RecipeHolder<AmmoPressRecipe> hovered = recipeAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(this.font, hovered.value().result(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 7, this.topPos + 17, 9, 54)) {
            graphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 17, 176, 0, 9, 54);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 88, this.topPos + 17, 9, 54)) {
            graphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 17, 185, 0, 9, 54);
        }
        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 72, 176, 54, 70, 16);
        }

        renderRecipeButtons(graphics);
        renderGhostInputs(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            setFocused(this.search);
            return true;
        }
        // [parity g1: GuiTextField.mouseClicked - clicking elsewhere drops the focus]
        this.search.setFocused(false);
        if (getFocused() == this.search) {
            setFocused(null);
        }
        // [parity K3: GUIMachineAmmoPress.mouseClicked - arrows always click, paging is clamped]
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 7, this.topPos + 17, 9, 54)) {
            K3GuiText.click();
            if (this.page > 0) this.page--;
            return true;
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 88, this.topPos + 17, 9, 54)) {
            K3GuiText.click();
            if (this.page < maxPage()) this.page++;
            return true;
        }

        RecipeHolder<AmmoPressRecipe> recipe = recipeAt(mouseX, mouseY);
        if (recipe != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int id = this.menu.buttonIdForRecipe(recipe.id());
            // [parity g1: GUIMachineAmmoPress.mouseClicked - clicking the selected recipe clears the selection]
            if (id > 0 && id - 1 == this.menu.selectedRecipeIndex()) {
                id = 0;
            }
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            K3GuiText.click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [parity g1: GUIMachineAmmoPress.keyTyped - a focused search field consumes typed keys, only Esc falls through]
        if (this.search.isFocused() && (this.search.keyPressed(keyCode, scanCode, modifiers) || keyCode != 256)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // [parity g1: GUIMachineAmmoPress.drawScreen - wheel paging only inside the GUI and not over a slot]
        if (!K3GuiText.hover(mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth, this.imageHeight) || this.hoveredSlot != null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollY > 0.0D && this.page > 0) {
            this.page--;
            return true;
        }
        if (scrollY < 0.0D && this.page < maxPage()) {
            this.page++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void refreshRecipes() {
        String filter = this.search == null ? "" : this.search.getValue().toLowerCase(Locale.ROOT);
        this.filteredRecipes = this.menu.recipes().stream()
                .filter(holder -> holder.value().result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        this.page = Math.min(this.page, maxPage());
    }

    private void renderRecipeButtons(GuiGraphics graphics) {
        // The legacy GUI advances three entries per page while showing twelve.
        int start = this.page * 3;
        int selected = this.menu.selectedRecipeIndex();
        for (int local = 0; local < RECIPES_PER_PAGE; local++) {
            int index = start + local;
            if (index >= this.filteredRecipes.size()) {
                break;
            }

            int column = local / 3;
            int row = local % 3;
            int x = this.leftPos + 16 + column * 18;
            int y = this.topPos + 17 + row * 18;
            RecipeHolder<AmmoPressRecipe> recipe = this.filteredRecipes.get(index);

            graphics.renderItem(recipe.value().result(), x + 1, y + 1);
            boolean isSelected = selected >= 0 && this.menu.recipes().get(selected).id().equals(recipe.id());
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            graphics.blit(TEXTURE, x, y, isSelected ? 194 : 212, 0, 18, 18);
            graphics.pose().popPose();
            // [parity K3: the output count is always drawn at half scale at the slot centre]
            graphics.pose().pushPose();
            graphics.pose().translate(x + 1 + 8, y + 1 + 8, 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.renderItemDecorations(this.font, recipe.value().result(), 0, 0, Integer.toString(recipe.value().result().getCount()));
            graphics.pose().popPose();
        }
    }

    private void renderGhostInputs(GuiGraphics graphics) {
        Optional<RecipeHolder<AmmoPressRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) {
            return;
        }
        for (int index = 0; index < 9; index++) {
            AmmoPressRecipe.SlotIngredient required = selected.get().value().input().get(index);
            if (required.isEmpty()) {
                continue;
            }

            Slot slot = this.menu.getSlot(index);
            if (slot.hasItem()) {
                continue;
            }

            ItemStack[] candidates = required.ingredient().orElseThrow().getItems();
            if (candidates.length == 0) {
                continue;
            }
            ItemStack ghost = candidates[(int) ((System.currentTimeMillis() / 1000L) % candidates.length)].copy();
            ghost.setCount(required.count());
            GhostItemRenderer.render(graphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, GHOST_ALPHA);
        }
    }

    private RecipeHolder<AmmoPressRecipe> recipeAt(double mouseX, double mouseY) {
        int start = this.page * 3;
        for (int local = 0; local < RECIPES_PER_PAGE; local++) {
            int index = start + local;
            if (index >= this.filteredRecipes.size()) {
                return null;
            }
            int x = 16 + (local / 3) * 18;
            int y = 17 + (local % 3) * 18;
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, 18, 18)) {
                return this.filteredRecipes.get(index);
            }
        }
        return null;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFFFFFF, false); // [parity K3: white title]
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    private int maxPage() {
        return Math.max(0, (int) Math.ceil((this.filteredRecipes.size() - RECIPES_PER_PAGE) / 3.0D));
    }
}

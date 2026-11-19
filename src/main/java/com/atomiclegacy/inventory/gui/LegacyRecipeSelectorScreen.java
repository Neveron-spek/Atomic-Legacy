package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.search.JechSearchCompat;
import com.atomiclegacy.inventory.gui.widget.SearchEditBox;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * [parity K1] GUIScreenRecipeSelector of 1.7.10: 8x5 recipe grid in registration order, search by localized
 * name, paging (buttons, wheel, arrow/page/home/end keys), current selection slot, recipe tooltips drawn with
 * GUIElements#drawHoveringTextRecipe. The selection is sent to the server when the screen is left
 * (onGuiClosed), and every close plays the button click.
 */
public abstract class LegacyRecipeSelectorScreen<R extends Recipe<?>> extends Screen {
    protected static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_recipe_selector.png");
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 132;

    protected final Screen previousScreen;
    private final List<RecipeHolder<R>> recipeSet;
    private final List<RecipeHolder<R>> recipes = new ArrayList<>();
    @Nullable
    private ResourceLocation selection;
    @Nullable
    private final ResourceLocation initialSelection;
    private SearchEditBox search;
    private String searchText = "";
    private int guiLeft;
    private int guiTop;
    private int pageIndex;
    private int size;
    private boolean closing;

    protected LegacyRecipeSelectorScreen(Component title, Screen previousScreen, List<RecipeHolder<R>> recipeSet,
            @Nullable ResourceLocation selection) {
        super(title);
        this.previousScreen = previousScreen;
        this.recipeSet = List.copyOf(recipeSet);
        this.selection = selection;
        this.initialSelection = selection;
        search("");
    }

    /** GenericRecipe#getIcon. */
    protected abstract ItemStack icon(RecipeHolder<R> holder);

    /** GenericRecipe#getLocalizedName (search key). */
    protected abstract Component localizedName(RecipeHolder<R> holder);

    /** GenericRecipe#print. */
    protected abstract List<Component> describe(RecipeHolder<R> holder);

    /** NBTControlPacket "selection" of onGuiClosed; null is GUIScreenRecipeSelector.NULL_SELECTION. */
    protected abstract void sendSelection(@Nullable ResourceLocation selection);

    @Override
    protected void init() {
        this.guiLeft = (this.width - X_SIZE) / 2;
        this.guiTop = (this.height - Y_SIZE) / 2;
        boolean focused = this.search != null && this.search.isFocused();
        this.search = new SearchEditBox(this.font, this.guiLeft + 28, this.guiTop + 111, 102, 12,
                Component.translatable("container.atomiclegacy.recipe_selector.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setValue(this.searchText);
        this.search.setFocused(focused);
        this.search.setResponder(text -> {
            this.searchText = text;
            search(text);
        });
    }

    private void search(String text) {
        this.recipes.clear();
        String query = JechSearchCompat.normalizeQuery(text);
        for (RecipeHolder<R> holder : this.recipeSet) {
            if (query.isBlank() || LegacyRecipeTooltip.matchesSearch(localizedName(holder), query)) {
                this.recipes.add(holder);
            }
        }
        this.pageIndex = 0;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8D));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderSelectorBackground(graphics, mouseX, mouseY);
        this.search.render(graphics, mouseX, mouseY, partialTick);

        if (inside(mouseX, mouseY, 7, 17, 144, 90)) {
            for (int i = this.pageIndex * 8; i < this.pageIndex * 8 + 40 && i < this.recipes.size(); i++) {
                int ind = i - this.pageIndex * 8;
                if (inside(mouseX, mouseY, 7 + 18 * (ind % 8), 17 + 18 * (ind / 8), 18, 18)) {
                    LegacyRecipeTooltip.render(graphics, this.font, describe(this.recipes.get(i)), mouseX, mouseY);
                }
            }
        }
        if (inside(mouseX, mouseY, 151, 71, 18, 18)) {
            RecipeHolder<R> selected = selectedRecipe();
            if (selected != null) {
                LegacyRecipeTooltip.render(graphics, this.font, describe(selected), mouseX, mouseY);
            }
        }
        if (inside(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.renderTooltip(this.font, Component.translatable("gui.recipe.close").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
        }
        if (inside(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.renderTooltip(this.font, Component.translatable("gui.recipe.clear_search").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
        }
        if (inside(mouseX, mouseY, 8, 108, 16, 16)) {
            graphics.renderTooltip(this.font, Component.translatable("gui.recipe.search_focus").withStyle(ChatFormatting.ITALIC), mouseX, mouseY);
        }
    }

    private void renderSelectorBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, X_SIZE, Y_SIZE);
        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, this.guiLeft + 26, this.guiTop + 108, 0, 132, 106, 16);
        }
        int[][] buttons = {{152, 18, 0}, {152, 36, 16}, {152, 90, 32}, {134, 108, 48}, {8, 108, 64}};
        for (int[] button : buttons) {
            if (inside(mouseX, mouseY, button[0], button[1], 16, 16)) {
                graphics.blit(TEXTURE, this.guiLeft + button[0], this.guiTop + button[1], 176, button[2], 16, 16);
            }
        }
        for (int i = this.pageIndex * 8; i < this.pageIndex * 8 + 40 && i < this.recipes.size(); i++) {
            int ind = i - this.pageIndex * 8;
            if (this.recipes.get(i).id().equals(this.selection)) {
                graphics.blit(TEXTURE, this.guiLeft + 7 + 18 * (ind % 8), this.guiTop + 17 + 18 * (ind / 8), 192, 0, 18, 18);
            }
        }
        for (int i = this.pageIndex * 8; i < this.pageIndex * 8 + 40 && i < this.recipes.size(); i++) {
            int ind = i - this.pageIndex * 8;
            renderIcon(graphics, icon(this.recipes.get(i)), 8 + 18 * (ind % 8), 18 + 18 * (ind / 8));
        }
        RecipeHolder<R> selected = selectedRecipe();
        if (selected != null) {
            renderIcon(graphics, icon(selected), 152, 72);
        }
    }

    /** GUIScreenRecipeSelector#renderItem: renderItemAndEffectIntoGUI, no stack size overlay. */
    private void renderIcon(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, this.guiLeft + x, this.guiTop + y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;
        // GuiTextField#mouseClicked: focus follows the click
        boolean onSearch = this.search.isMouseOver(mouseX, mouseY);
        this.search.setFocused(onSearch);
        if (onSearch) {
            this.search.mouseClicked(mouseX, mouseY, button);
        }

        if (inside(x, y, 152, 18, 16, 16)) {
            click();
            if (this.pageIndex > 0) this.pageIndex--;
            return true;
        }
        if (inside(x, y, 152, 36, 16, 16)) {
            click();
            if (this.pageIndex < this.size) this.pageIndex++;
            return true;
        }
        if (inside(x, y, 134, 108, 16, 16)) {
            this.search.setValue("");
            this.searchText = "";
            search("");
            this.search.setFocused(true);
            return true;
        }
        for (int i = this.pageIndex * 8; i < this.pageIndex * 8 + 40 && i < this.recipes.size(); i++) {
            int ind = i - this.pageIndex * 8;
            if (inside(x, y, 7 + 18 * (ind % 8), 17 + 18 * (ind / 8), 18, 18)) {
                ResourceLocation newSelection = this.recipes.get(i).id();
                this.selection = newSelection.equals(this.selection) ? null : newSelection;
                click();
                return true;
            }
        }
        if (inside(x, y, 151, 71, 18, 18) && this.selection != null) {
            this.selection = null;
            click();
            return true;
        }
        if (inside(x, y, 152, 90, 16, 16)) {
            closeSelector();
            return true;
        }
        return onSearch;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // GUIScreenRecipeSelector#handleScroll: anywhere on the screen
        if (scrollY > 0 && this.pageIndex > 0) this.pageIndex--;
        if (scrollY < 0 && this.pageIndex < this.size) this.pageIndex++;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.search.setFocused(!this.search.isFocused());
            return true;
        }
        if (this.search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE && this.search.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        boolean paging = true;
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> this.pageIndex--;
            case GLFW.GLFW_KEY_DOWN -> this.pageIndex++;
            case GLFW.GLFW_KEY_PAGE_UP -> this.pageIndex -= 5;
            case GLFW.GLFW_KEY_PAGE_DOWN -> this.pageIndex += 5;
            case GLFW.GLFW_KEY_HOME -> this.pageIndex = 0;
            case GLFW.GLFW_KEY_END -> this.pageIndex = this.size;
            default -> paging = false;
        }
        this.pageIndex = Mth.clamp(this.pageIndex, 0, this.size);
        if (paging) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || (!this.search.isFocused() && this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            closeSelector();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.search.isFocused() && this.search.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        closeSelector();
    }

    private void closeSelector() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    /** GUIScreenRecipeSelector#onGuiClosed. */
    @Override
    public void removed() {
        if (!this.closing) {
            this.closing = true;
            if (!java.util.Objects.equals(this.selection, this.initialSelection)) {
                sendSelection(this.selection);
            }
            click();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    private RecipeHolder<R> selectedRecipe() {
        if (this.selection == null) {
            return null;
        }
        for (RecipeHolder<R> holder : this.recipeSet) {
            if (holder.id().equals(this.selection)) {
                return holder;
            }
        }
        return null;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.guiLeft + x <= mouseX && this.guiLeft + x + width > mouseX
                && this.guiTop + y < mouseY && this.guiTop + y + height >= mouseY;
    }

    protected void click() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}

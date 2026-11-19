package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.search.JechSearchCompat;
import com.atomiclegacy.inventory.gui.widget.SearchEditBox;
import com.atomiclegacy.inventory.container.HbmAnvilMenu;
import com.atomiclegacy.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.atomiclegacy.inventory.recipes.anvil.AnvilIngredient;
import com.atomiclegacy.inventory.recipes.anvil.AnvilOutput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** [parity e2] GUIAnvil of the 1.7.10 reference (176x222, gui/processing/gui_anvil.png). */
public class HbmAnvilScreen extends AbstractContainerScreen<HbmAnvilMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_anvil.png");

    private final Inventory playerInventory;
    private final List<AnvilConstructionRecipe> originRecipes;
    private final List<AnvilConstructionRecipe> recipes = new ArrayList<>();
    private SearchEditBox search;
    private int page;
    private int pageCount;
    private int selection = -1;
    private int detailWidth = 1;
    // [parity e2] GUIAnvil.state: recipe list filter cycled by the button at 88,53
    private AnvilConstructionRecipe.OverlayType state = AnvilConstructionRecipe.OverlayType.NONE;

    public HbmAnvilScreen(HbmAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.playerInventory = playerInventory;
        this.originRecipes = List.copyOf(menu.constructionRecipes());
        regenerateRecipes();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 61 - this.font.width(this.title) / 2;
        this.titleLabelY = 8;
        String previousQuery = this.search == null ? "" : this.search.getValue();
        boolean focused = this.search != null && this.search.isFocused();
        this.search = new SearchEditBox(this.font, this.leftPos + 10, this.topPos + 111, 84, 12, Component.empty());
        this.search.setTextColor(-1);
        this.search.setTextColorUneditable(-1);
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.search.setValue(previousQuery);
        this.search.setFocused(focused);
        this.search.setResponder(this::search);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.search.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isInside(mouseX, mouseY, 88, 53, 18, 18)) {
            String text = switch (this.state) {
                case NONE -> "All recipes";
                case SMITHING -> "Smithing";
                case CONSTRUCTION -> "Construction";
                default -> "Recycling";
            };
            guiGraphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int slide = Mth.clamp(this.detailWidth - 42, 0, 1000);
        int multiplier = 1;
        while (slide >= 51 * multiplier) {
            guiGraphics.blit(TEXTURE, this.leftPos + 125 + 51 * multiplier, this.topPos + 17, 125, 17, 54, 108);
            multiplier++;
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 125 + slide, this.topPos + 17, 125, 17, 54, 108);

        if (this.search != null && this.search.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 108, 168, 222, 88, 16);
        }
        if (isInside(mouseX, mouseY, 7, 71, 9, 36)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 71, 176, 186, 9, 36);
        }
        if (isInside(mouseX, mouseY, 106, 71, 9, 36)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 106, this.topPos + 71, 185, 186, 9, 36);
        }
        if (isInside(mouseX, mouseY, 52, 53, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 52, this.topPos + 53, 176, 150, 18, 18);
        }

        int stateU = switch (this.state) {
            case SMITHING -> 200;
            case CONSTRUCTION -> 218;
            case RECYCLING -> 236;
            default -> -1;
        };
        if (stateU >= 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 53, stateU, 0, 18, 18);
            if (isInside(mouseX, mouseY, 88, 53, 18, 18)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 53, stateU, 18, 18, 18);
            }
        }

        for (int recipeIndex = this.page * 2; recipeIndex < this.page * 2 + 10 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.page * 2;
            int x = 16 + 18 * (visibleIndex / 2);
            int y = 71 + 18 * (visibleIndex % 2);
            AnvilConstructionRecipe recipe = this.recipes.get(recipeIndex);
            ItemStack display = recipe.getDisplay();
            if (!display.isEmpty()) {
                guiGraphics.renderItem(display, this.leftPos + x + 1, this.topPos + y + 1);
            }
            // zLevel 300: overlay marker and selection frame are drawn above the item
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
            guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 18 + 18 * recipe.overlay().ordinal(), 222, 18, 18);
            if (this.selection == recipeIndex) {
                guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 0, 222, 18, 18);
            }
            guiGraphics.pose().popPose();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        if (this.selection < 0 || this.selection >= this.recipes.size()) {
            this.detailWidth = 0;
            return;
        }

        List<String> lines = recipeToList(this.recipes.get(this.selection));
        int longest = 0;
        for (String line : lines) {
            longest = Math.max(longest, this.font.width(line));
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        int y = 50;
        for (String line : lines) {
            guiGraphics.drawString(this.font, line, 260, y, 0xFFFFFF, false);
            y += 9;
        }
        guiGraphics.pose().popPose();
        this.detailWidth = (int) (longest * 0.5D);
    }

    /** GUIAnvil#recipeToList: inputs the player lacks are red, ore dictionary inputs cycle once per second */
    private List<String> recipeToList(AnvilConstructionRecipe recipe) {
        List<String> list = new ArrayList<>();
        list.add("§eInputs:");
        for (AnvilIngredient input : recipe.inputs()) {
            if (input.isFluid()) {
                list.add(input.describeLine().getString());
                continue;
            }
            List<ItemStack> variants = input.searchStacks();
            if (variants.isEmpty()) {
                list.add("I AM ERROR");
                continue;
            }
            int amount = 0;
            for (ItemStack stack : this.playerInventory.items) {
                if (!stack.isEmpty() && input.matchesItem(stack)) {
                    amount += stack.getCount();
                }
            }
            ItemStack shown = variants.get((int) (Math.abs(System.currentTimeMillis() / 1000) % variants.size()));
            String line = ">" + input.count() + "x " + shown.getHoverName().getString();
            list.add(amount >= input.count() ? line : "§c" + line);
        }
        list.add("");
        list.add("§eOutputs:");
        for (AnvilOutput output : recipe.outputs()) {
            ItemStack stack = output.stack();
            list.add(">" + stack.getCount() + "x " + stack.getHoverName().getString()
                    + (output.chance() != 1.0F ? (" (" + (output.chance() * 100) + "%)") : ""));
        }
        return list;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.search.setFocused(this.search.mouseClicked(mouseX, mouseY, button));

        int x = (int) mouseX;
        int y = (int) mouseY;
        if (isInside(x, y, 7, 71, 9, 36)) {
            K3GuiText.click();
            if (this.page > 0) {
                this.page--;
            }
            return true;
        }
        if (isInside(x, y, 106, 71, 9, 36)) {
            K3GuiText.click();
            if (this.page < this.pageCount) {
                this.page++;
            }
            return true;
        }
        if (isInside(x, y, 52, 53, 18, 18)) {
            if (this.selection >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
                int buttonId = selectedRecipeButtonId();
                if (buttonId >= 0) {
                    K3GuiText.click();
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
                }
            }
            return true;
        }
        if (isInside(x, y, 88, 53, 18, 18)) {
            K3GuiText.click();
            AnvilConstructionRecipe.OverlayType[] values = AnvilConstructionRecipe.OverlayType.values();
            this.state = values[(this.state.ordinal() + 1) % values.length];
            regenerateRecipes();
            return true;
        }

        int recipeIndex = recipeAt(x, y);
        if (recipeIndex >= 0) {
            this.selection = this.selection == recipeIndex ? -1 : recipeIndex;
            K3GuiText.click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // the reference scrolls while the cursor is inside the window and not over a slot
        if (isInside((int) mouseX, (int) mouseY, 0, 0, this.imageWidth, this.imageHeight) && this.hoveredSlot == null) {
            if (scrollY > 0.0D && this.page > 0) {
                this.page--;
                return true;
            }
            if (scrollY < 0.0D && this.page < this.pageCount) {
                this.page++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.search.isFocused()) {
            if (this.search.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == 256) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.search.isFocused() && this.search.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean matchState(AnvilConstructionRecipe recipe) {
        return this.state == AnvilConstructionRecipe.OverlayType.NONE || recipe.overlay() == this.state;
    }

    /** GUIAnvil#regenerateRecipes: filter by the overlay state only */
    private void regenerateRecipes() {
        this.recipes.clear();
        for (AnvilConstructionRecipe recipe : this.originRecipes) {
            if (matchState(recipe)) {
                this.recipes.add(recipe);
            }
        }
        resetPaging();
    }

    /** GUIAnvil#search: an empty query falls back to the state filter, a non-empty one searches all recipes */
    private void search(String query) {
        this.recipes.clear();
        String normalized = JechSearchCompat.normalizeQuery(query);
        for (AnvilConstructionRecipe recipe : this.originRecipes) {
            if (normalized.isBlank() ? matchState(recipe) : matchesSearch(recipe, normalized)) {
                this.recipes.add(recipe);
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        this.page = 0;
        this.selection = -1;
        this.pageCount = Math.max(0, (int) Math.ceil((this.recipes.size() - 10) / 2.0D));
    }

    private static boolean matchesSearch(AnvilConstructionRecipe recipe, String query) {
        for (var input : recipe.inputs()) {
            for (ItemStack stack : input.searchStacks()) {
                if (stackMatchesSearch(stack, query)) {
                    return true;
                }
            }
        }
        for (var output : recipe.outputs()) {
            if (stackMatchesSearch(output.stack(), query)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stackMatchesSearch(ItemStack stack, String query) {
        if (stack.isEmpty()) {
            return false;
        }
        if (JechSearchCompat.contains(stack.getHoverName().getString(), query)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return JechSearchCompat.contains(id.toString(), query)
                || JechSearchCompat.contains(id.getPath(), query);
    }

    private int recipeAt(int mouseX, int mouseY) {
        for (int recipeIndex = this.page * 2; recipeIndex < this.page * 2 + 10 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.page * 2;
            int x = 16 + 18 * (visibleIndex / 2);
            int y = 71 + 18 * (visibleIndex % 2);
            if (isInside(mouseX, mouseY, x, y, 18, 18)) {
                return recipeIndex;
            }
        }
        return -1;
    }

    private int selectedRecipeButtonId() {
        if (this.selection < 0 || this.selection >= this.recipes.size()) {
            return -1;
        }

        int originIndex = this.originRecipes.indexOf(this.recipes.get(this.selection));
        if (originIndex < 0) {
            return -1;
        }
        // Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : 0
        boolean shift = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
        return originIndex * 2 + (shift ? 1 : 0);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX
                && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY
                && this.topPos + y + height >= mouseY;
    }
}

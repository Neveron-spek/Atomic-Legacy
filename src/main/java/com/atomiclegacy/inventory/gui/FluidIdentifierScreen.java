package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.search.JechSearchCompat;
import com.atomiclegacy.inventory.gui.widget.SearchEditBox;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.FluidIdentifierItem;
import com.atomiclegacy.network.SetFluidBookmarksPayload;
import com.atomiclegacy.network.SetFluidIdentifierPayload;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** [parity e2] GUIScreenFluid of the 1.7.10 reference: gui_fluid.png 176x72, two rows of nine, bookmarks on middle click. */
public class FluidIdentifierScreen extends Screen {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/machine/gui_fluid.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 72;
    private static final int SLOTS = 18;

    private final ItemStack stack;
    private final HbmFluidDefinition[] searchArray = new HbmFluidDefinition[SLOTS];
    /** Bookmarks(capacity 18): insertion ordered, the eldest entry is dropped on overflow. */
    private final List<HbmFluidDefinition> bookmarks = new ArrayList<>();
    private SearchEditBox search;
    private int leftPos;
    private int topPos;
    private HbmFluidDefinition primary;
    private HbmFluidDefinition secondary;
    private boolean loaded;

    public FluidIdentifierScreen(ItemStack stack) {
        super(Component.translatable("gui.atomiclegacy.fluid_identifier"));
        this.stack = stack;
        this.primary = FluidIdentifierItem.primary(stack);
        this.secondary = FluidIdentifierItem.secondary(stack);
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new FluidIdentifierScreen(stack));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - HEIGHT) / 2;
        String previous = this.search == null ? "" : this.search.getValue();
        this.search = new SearchEditBox(this.font, this.leftPos + 46, this.topPos + 11, 86, 12, Component.translatable("gui.atomiclegacy.fluid_identifier.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setValue(previous);
        this.search.setFocused(true);
        if (!this.loaded) {
            this.loaded = true;
            List<HbmFluidDefinition> stored = FluidIdentifierItem.bookmarks(this.stack);
            for (int i = 0; i < Math.min(stored.size(), SLOTS); i++) {
                addBookmark(stored.get(i));
                this.searchArray[i] = stored.get(i);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderWindow(guiGraphics);
        this.search.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderWindow(GuiGraphics guiGraphics) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        if (this.search.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 43, this.topPos + 7, 166, 72, 90, 18);
        }
        for (int k = 0; k < SLOTS; k++) {
            HbmFluidDefinition fluid = this.searchArray[k];
            if (fluid == null) {
                return;
            }
            int x = this.leftPos + 7 + (k % 9) * 18;
            int y = this.topPos + 29 + (k / 9) * 18;
            int color = fluid.color();
            guiGraphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
            guiGraphics.blit(TEXTURE, this.leftPos + 12 + (k % 9) * 18, this.topPos + 31 + (k / 9) * 18, 12 + (k % 9) * 18, 74, 8, 14);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (fluid == this.primary && fluid == this.secondary) {
                guiGraphics.blit(TEXTURE, x, y, 176, 36, 18, 18);
            } else if (fluid == this.primary) {
                guiGraphics.blit(TEXTURE, x, y, 176, 0, 18, 18);
            } else if (fluid == this.secondary) {
                guiGraphics.blit(TEXTURE, x, y, 176, 18, 18, 18);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (!(this.minecraft.player.getMainHandItem().getItem() instanceof FluidIdentifierItem)
                && !(this.minecraft.player.getOffhandItem().getItem() instanceof FluidIdentifierItem)) {
            this.minecraft.setScreen(null);
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int index = fluidAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        HbmFluidDefinition fluid = this.searchArray[index];
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(fluid.translationKey()));
        HbmFluidTooltip.appendTraitInfo(lines, fluid, false);
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.search.mouseClicked(mouseX, mouseY, button);
        this.search.setFocused(this.search.isMouseOver(mouseX, mouseY));
        int index = fluidAt((int) mouseX, (int) mouseY);
        if (index >= 0) {
            HbmFluidDefinition fluid = this.searchArray[index];
            if (button == 0) {
                setFluid(fluid, true);
            } else if (button == 1) {
                setFluid(fluid, false);
            } else if (button == 2) {
                click();
                if (addBookmark(fluid)) {
                    this.bookmarks.remove(fluid);
                }
                if (this.search.getValue().trim().isEmpty()) {
                    showBookmarks();
                }
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) {
            updateSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.search.charTyped(codePoint, modifiers)) {
            updateSearch();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** GUIScreenFluid#onGuiClosed: the bookmark list goes back to the held identifier. */
    @Override
    public void removed() {
        List<String> names = new ArrayList<>();
        for (HbmFluidDefinition fluid : this.bookmarks) {
            names.add(fluid.name());
        }
        FluidIdentifierItem.setBookmarks(this.stack, names);
        PacketDistributor.sendToServer(new SetFluidBookmarksPayload(names));
        super.removed();
    }

    private void click() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void setFluid(HbmFluidDefinition fluid, boolean primarySlot) {
        click();
        if (primarySlot) {
            this.primary = fluid;
        } else {
            this.secondary = fluid;
        }
        FluidIdentifierItem.setType(this.stack, fluid, primarySlot);
        PacketDistributor.sendToServer(new SetFluidIdentifierPayload(fluid.name(), primarySlot));
    }

    /** Bookmarks#add: true if the fluid was already bookmarked. */
    private boolean addBookmark(HbmFluidDefinition fluid) {
        if (this.bookmarks.contains(fluid)) {
            return true;
        }
        this.bookmarks.add(fluid);
        if (this.bookmarks.size() > SLOTS) {
            this.bookmarks.remove(0);
        }
        return false;
    }

    private void showBookmarks() {
        Arrays.fill(this.searchArray, null);
        for (int i = 0; i < this.bookmarks.size(); i++) {
            this.searchArray[i] = this.bookmarks.get(i);
        }
    }

    /** GUIScreenFluid#updateSearch: bookmarks for an empty query, otherwise the first 18 name matches in nice order. */
    private void updateSearch() {
        String query = this.search.getValue();
        if (!this.bookmarks.isEmpty() && query.trim().isEmpty()) {
            showBookmarks();
            return;
        }
        Arrays.fill(this.searchArray, null);
        String normalized = JechSearchCompat.normalizeQuery(query);
        int next = 0;
        for (HbmFluidDefinition fluid : HbmFluids.niceOrder()) {
            if (!fluid.allowsFluidIdentifier()) {
                continue;
            }
            if (normalized.isBlank() || JechSearchCompat.contains(Component.translatable(fluid.translationKey()).getString(), normalized)) {
                this.searchArray[next++] = fluid;
                if (next >= SLOTS) {
                    return;
                }
            }
        }
    }

    /** 1.7.10 hit test: x in [x, x + 18), y in (y, y + 18]; stops at the first empty cell. */
    private int fluidAt(int mouseX, int mouseY) {
        for (int k = 0; k < SLOTS; k++) {
            if (this.searchArray[k] == null) {
                return -1;
            }
            int x = this.leftPos + 7 + (k % 9) * 18;
            int y = this.topPos + 29 + (k / 9) * 18;
            if (x <= mouseX && x + 18 > mouseX && y < mouseY && y + 18 >= mouseY) {
                return k;
            }
        }
        return -1;
    }
}

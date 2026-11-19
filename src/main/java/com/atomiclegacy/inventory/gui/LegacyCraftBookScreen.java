package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyCraftBookItem;
import com.atomiclegacy.inventory.container.LegacyCraftBookMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original 176x166 Book of Boxcars and Lemegeton layouts. */
public final class LegacyCraftBookScreen extends AbstractContainerScreen<LegacyCraftBookMenu> {
    private static final ResourceLocation BOOK = AtomicLegacy.id("textures/gui/processing/gui_book.png");
    private static final ResourceLocation LEMEGETON = AtomicLegacy.id("textures/gui/processing/gui_lemegeton.png");

    public LegacyCraftBookScreen(LegacyCraftBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = menu.kind() == LegacyCraftBookItem.Kind.BOXCARS ? BOOK : LEMEGETON;
        graphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (menu.getSlot(0).hasItem()) {
            if (menu.kind() == LegacyCraftBookItem.Kind.BOXCARS) {
                graphics.blit(texture, this.leftPos + 29, this.topPos + 16, 176, 0, 54, 54);
            } else {
                graphics.blit(texture, this.leftPos + 7, this.topPos + 22, 0, 166, 162, 42);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String heading = menu.kind() == LegacyCraftBookItem.Kind.BOXCARS
                ? "Extended 4-Slot Crafting"
                : "Material Upgrade Conversion";
        // [parity S3: books] GUIBook / GUILemegeton draw both labels with standardGalacticFontRenderer.
        graphics.drawString(this.font, galactic(heading), 28, 6, 4210752, false);
        graphics.drawString(this.font, galactic("Standard Inventory"), 8, this.inventoryLabelY, 4210752, false);
    }

    private static net.minecraft.network.chat.Component galactic(String text) {
        return net.minecraft.network.chat.Component.literal(text)
                .withStyle(style -> style.withFont(net.minecraft.client.Minecraft.ALT_FONT));
    }
}

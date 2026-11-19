package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.GasCentrifugeBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.GasCentrifugeMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class GasCentrifugeScreen extends AbstractContainerScreen<GasCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_centrifuge_gas.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public GasCentrifugeScreen(GasCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 206;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // [parity e2] GUIMachineGasCent#drawScreen: pseudo tank name (+ high speed colour) and "fill / max mB"; no progress/extra tooltip
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 15, this.topPos + 15, 24, 55)) {
            boolean highSpeed = "heuf6".equals(this.menu.inputFluid().name());
            ChatFormatting colour = !highSpeed ? null
                    : this.menu.processTime() > GasCentrifugeBlockEntity.BASE_PROCESSING_SPEED - 70 ? ChatFormatting.DARK_RED : ChatFormatting.GOLD;
            guiGraphics.renderComponentTooltip(this.font, pseudoTankInfo(this.menu.inputFluid(), this.menu.inputAmount(), colour), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 137, this.topPos + 15, 25, 55)) {
            ChatFormatting colour = "heuf6".equals(this.menu.outputFluid().name()) ? ChatFormatting.GOLD : null;
            guiGraphics.renderComponentTooltip(this.font, pseudoTankInfo(this.menu.outputFluid(), this.menu.outputAmount(), colour), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 182, this.topPos + 69 - 52, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font,
                    List.of(K3GuiText.electricity(this.menu.energy(), GasCentrifugeBlockEntity.MAX_POWER)), mouseX, mouseY);
        }

        if (isHovering(-12, 16, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.gasCent.enrichment", ChatFormatting.GREEN),
                    this.leftPos - 8, this.topPos + 32);
        }
        if (isHovering(-12, 32, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, tooltipLines("desc.gui.gasCent.output", ChatFormatting.GOLD),
                    this.leftPos - 8, this.topPos + 48);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerPixels = this.menu.energyScaled(52);
        if (powerPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 182, this.topPos + 69 - powerPixels, 206, 52 - powerPixels, 16, powerPixels);
        }

        int progressPixels = this.menu.progressScaled(36);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 70, this.topPos + 35, 206, 52, progressPixels, 13);
        }

        pseudoTank(guiGraphics, 16, 16, this.menu.inputAmount());
        pseudoTank(guiGraphics, 32, 16, this.menu.inputAmount());
        pseudoTank(guiGraphics, 138, 16, this.menu.outputAmount());
        pseudoTank(guiGraphics, 154, 16, this.menu.outputAmount());
        drawInfoPanel(guiGraphics, -12, 16, 3);
        drawInfoPanel(guiGraphics, -12, 32, 2);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0xFF404040, false);
    }

    /**
     * [parity e2] GUIMachineGasCent#renderTank(x, y, z, 6, 52, fill, maxFill): a quad growing DOWN from the top edge,
     * sampling the still bound GUI texture with minU = 0, maxU = width / 16, minV = 1 (top), maxV = 1 - i / 16 (bottom).
     */
    private void pseudoTank(GuiGraphics graphics, int x, int y, int fluid) {
        int i = (fluid * 52) / GasCentrifugeBlockEntity.TANK_CAPACITY;
        if (i <= 0) {
            return;
        }
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 6, i, 0.0F, 256.0F, 96, -16 * i, 256, 256);
        RenderSystem.disableBlend();
    }

    /** [parity e2] PseudoFluidType#getName: hbmpseudofluid.<name>; the port stores the stages as real fluids. */
    private static List<Component> pseudoTankInfo(HbmFluidDefinition fluid, int amount, ChatFormatting colour) {
        String key = switch (fluid.name()) {
            case "uf6" -> "nuf6";
            case "puf6" -> "pf6";
            case "watz" -> "mud";
            case "watz_heavy" -> "mud_heavy";
            case "leuf6", "meuf6", "heuf6" -> fluid.name();
            default -> "none";
        };
        net.minecraft.network.chat.MutableComponent name = Component.translatable("hbmpseudofluid." + key);
        if (colour != null) {
            name = name.withStyle(colour);
        }
        return List.of(name, Component.literal(amount + " / " + GasCentrifugeBlockEntity.TANK_CAPACITY + " mB"));
    }

    private void drawInfoPanel(GuiGraphics graphics, int x, int y, int type) {
        int sourceX = switch (type) {
            case 3, 7, 11 -> 24;
            case 2, 6, 10 -> 8;
            default -> 0;
        };
        int sourceY = switch (type) {
            case 2, 3 -> 0;
            case 6, 7 -> 16;
            case 10, 11 -> 32;
            default -> 0;
        };
        graphics.blit(UTILITY, this.leftPos + x, this.topPos + y, sourceX, sourceY, 16, 16, 256, 256);
    }

    private List<Component> tooltipLines(String key, ChatFormatting headingColor) {
        String[] lines = Component.translatable(key).getString().split("\\$", -1);
        List<Component> result = new ArrayList<>(lines.length);
        for (int index = 0; index < lines.length; index++) {
            result.add(index == 0
                    ? Component.literal(lines[index]).withStyle(headingColor)
                    : Component.literal(lines[index]));
        }
        return result;
    }
}

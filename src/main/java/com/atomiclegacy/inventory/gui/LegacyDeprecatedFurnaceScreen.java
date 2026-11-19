package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyDeprecatedFurnaceBlock.Kind;
import com.atomiclegacy.inventory.container.LegacyDeprecatedFurnaceMenu;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.LegacyDeprecatedFurnaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** [parity d2] GUIDiFurnace, GUIMachineDiFurnaceRTG and GUIRtgFurnace of 1.7.10. */
public final class LegacyDeprecatedFurnaceScreen extends AbstractContainerScreen<LegacyDeprecatedFurnaceMenu> {
    private static final ResourceLocation DIFURNACE = AtomicLegacy.id("textures/gui/guidifurnace.png");
    private static final ResourceLocation DIFURNACE_RTG = AtomicLegacy.id("textures/gui/processing/gui_rtg_difurnace.png");
    private static final ResourceLocation RTG_FURNACE = AtomicLegacy.id("textures/gui/rtgfurnace.png");
    private static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    public LegacyDeprecatedFurnaceScreen(LegacyDeprecatedFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.menu.kind() == Kind.DIFURNACE_RTG) {
            if (isHovering(-15, 52, 16, 16, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, split("desc.gui.rtgBFurnace.desc"), this.leftPos - 8, this.topPos + 52);
            }
            if (isHovering(58, 36, 18, 16, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, split(Component.translatable("desc.gui.rtg.heat", this.menu.power()).getString()), mouseX, mouseY);
            }
            if (isHovering(-15, 36, 16, 16, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, pelletText(), this.leftPos - 8, this.topPos + 52);
            }
        }
        if (this.menu.getCarried().isEmpty()) {
            for (int i = 0; i < this.menu.sidedSlots(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    String direction = Direction.from3DDataValue(this.menu.side(i)).getName().toUpperCase(Locale.ROOT);
                    graphics.renderComponentTooltip(this.font,
                            List.of(Component.literal("Accepts items from: " + direction).withStyle(ChatFormatting.YELLOW)),
                            mouseX, mouseY - (slot.hasItem() ? 15 : 0));
                    return;
                }
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private List<Component> split(String keyOrText) {
        String text = keyOrText.startsWith("desc.") ? Component.translatable(keyOrText).getString() : keyOrText;
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\\$")) {
            lines.add(Component.literal(line));
        }
        return lines;
    }

    /** ItemRTGPellet.pelletList in registration order with ItemRTGPellet#getHeat. */
    private List<Component> pelletText() {
        List<Component> result = new ArrayList<>();
        result.add(Component.translatable("desc.gui.rtg.pellets"));
        for (Item pellet : List.of(
                HbmItems.PELLET_RTG_RADIUM.get(), HbmItems.PELLET_RTG_WEAK.get(), HbmItems.PELLET_RTG.get(),
                HbmItems.PELLET_RTG_STRONTIUM.get(), HbmItems.PELLET_RTG_COBALT.get(), HbmItems.PELLET_RTG_ACTINIUM.get(),
                HbmItems.PELLET_RTG_AMERICIUM.get(), HbmItems.PELLET_RTG_POLONIUM.get(), HbmItems.PELLET_RTG_GOLD.get(),
                HbmItems.PELLET_RTG_LEAD.get())) {
            result.add(Component.translatable("desc.gui.rtg.pelletHeat",
                    Component.translatable(pellet.getDescriptionId()), ((RtgPelletItem) pellet).heat(new ItemStack(pellet))));
        }
        return result;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        switch (this.menu.kind()) {
            case DIFURNACE -> {
                graphics.blit(DIFURNACE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
                boolean hasPower = this.menu.power() > 0;
                if (hasPower) {
                    int i1 = this.menu.power() * 52 / LegacyDeprecatedFurnaceBlockEntity.DIFURNACE_MAX_FUEL;
                    graphics.blit(DIFURNACE, this.leftPos + 44, this.topPos + 70 - i1, 201, 53 - i1, 16, i1);
                }
                int j1 = this.menu.progress() * 24 / LegacyDeprecatedFurnaceBlockEntity.DIFURNACE_PROCESSING_SPEED;
                graphics.blit(DIFURNACE, this.leftPos + 101, this.topPos + 35, 176, 14, j1 + 1, 17);
                if (hasPower && (this.menu.canProcess() || j1 > 0)) {
                    graphics.blit(DIFURNACE, this.leftPos + 63, this.topPos + 37, 176, 0, 14, 14);
                }
            }
            case DIFURNACE_RTG -> {
                graphics.blit(DIFURNACE_RTG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
                if (this.menu.power() >= 15) {
                    graphics.blit(DIFURNACE_RTG, this.leftPos + 58, this.topPos + 36, 176, 31, 18, 16);
                }
                int p = this.menu.progress() * 24 / LegacyDeprecatedFurnaceBlockEntity.DIFURNACE_RTG_TIME_REQUIRED;
                graphics.blit(DIFURNACE_RTG, this.leftPos + 101, this.topPos + 35, 176, 14, p + 1, 17);
                // GuiInfoContainer#drawInfoPanel types 2 and 3
                graphics.blit(UTILITY, this.leftPos - 15, this.topPos + 36, 8, 0, 16, 16, 256, 256);
                graphics.blit(UTILITY, this.leftPos - 15, this.topPos + 52, 24, 0, 16, 16, 256, 256);
            }
            case RTG_FURNACE -> {
                graphics.blit(RTG_FURNACE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
                if (this.menu.power() != 0) {
                    graphics.blit(RTG_FURNACE, this.leftPos + 55, this.topPos + 35, 176, 0, 18, 16);
                }
                int j1 = this.menu.progress() * 24 / LegacyDeprecatedFurnaceBlockEntity.RTG_FURNACE_PROCESSING_SPEED;
                graphics.blit(RTG_FURNACE, this.leftPos + 79, this.topPos + 34, 176, 16, j1 + 1, 17);
            }
        }
    }
}

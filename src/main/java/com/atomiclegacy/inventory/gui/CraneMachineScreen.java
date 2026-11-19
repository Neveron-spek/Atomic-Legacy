package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.CraneMachineBlock;
import com.atomiclegacy.inventory.container.CraneMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [parity R2] The 1.7.10 crane GUIs (GUICraneBoxer, GUICraneExtractor, GUICraneGrabber, GUICraneInserter,
 * GUICraneRouter, GUICraneUnboxer) with their original textures, buttons and tooltips. Button ids sent through
 * clickMenuButton keep the NBTControlPacket semantics: 0 = toggle/whitelist/destroyer, 1 = maxEject,
 * router 0..5 = toggle(j * 3 + k).
 */
public final class CraneMachineScreen extends AbstractContainerScreen<CraneMachineMenu> {
    private static final ResourceLocation BOXER = texture("gui_crane_boxer");
    private static final ResourceLocation EJECTOR = texture("gui_crane_ejector");
    private static final ResourceLocation GRABBER = texture("gui_crane_grabber");
    private static final ResourceLocation INSERTER = texture("gui_crane_inserter");
    private static final ResourceLocation ROUTER = texture("gui_crane_router");
    private static final ResourceLocation UNBOXER = texture("gui_crane_unboxer");

    public CraneMachineScreen(CraneMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.atomiclegacy.crane_" + menu.kind().name().toLowerCase(Locale.ROOT)));
        imageWidth = switch (menu.kind()) {
            case ROUTER -> 256;
            case EXTRACTOR -> 212;
            default -> 176;
        };
        imageHeight = menu.kind() == CraneMachineBlock.Kind.ROUTER ? 201 : 185;
    }

    private static ResourceLocation texture(String name) {
        return AtomicLegacy.id("textures/gui/storage/" + name + ".png");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        switch (menu.kind()) {
            case EXTRACTOR -> {
                patternTooltips(graphics, mouseX, mouseY);
                if (hover(mouseX, mouseY, 187, 34, 18, 18)) {
                    tip(graphics, mouseX, mouseY, "Only take maximum possible: " + (menu.maxEject() ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF"));
                }
            }
            case GRABBER -> patternTooltips(graphics, mouseX, mouseY);
            case INSERTER -> {
                if (hover(mouseX, mouseY, 151, 34, 18, 18)) {
                    tip(graphics, mouseX, mouseY, "Destroy overflow: " + (menu.primaryMode() != 0 ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF"));
                }
            }
            case ROUTER -> {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 3; k++) {
                        if (hover(mouseX, mouseY, 7 + j * 222, 16 + k * 26, 18, 18)) {
                            switch (menu.routerMode(j * 3 + k)) {
                                case 0 -> tip(graphics, mouseX, mouseY, "OFF");
                                case 1 -> tip(graphics, mouseX, mouseY, "WHITELIST", "Route if filter matches");
                                case 2 -> tip(graphics, mouseX, mouseY, "BLACKLIST", "Route if filter doesn't match");
                                case 3 -> tip(graphics, mouseX, mouseY, "WILDCARD", "Route if no other route is valid");
                                default -> { }
                            }
                        }
                    }
                }
                patternTooltips(graphics, mouseX, mouseY);
            }
            default -> { }
        }
    }

    /** "Right click to change" + ModulePatternMatcher.getLabel, drawn 30px above the cursor while nothing is held. */
    private void patternTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) return;
        for (Slot slot : menu.slots) {
            if (!menu.isFilterSlot(slot) || !isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) continue;
            String mode = menu.filterMode(slot.index);
            if (mode == null) continue;
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("tooltip.atomiclegacy.autocrafter.change").withStyle(ChatFormatting.RED),
                    modeLabel(mode)), mouseX, mouseY - 30);
        }
    }

    private static Component modeLabel(String mode) {
        return switch (mode) {
            case "exact" -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.exact").withStyle(ChatFormatting.YELLOW);
            case "wildcard" -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.wildcard").withStyle(ChatFormatting.YELLOW);
            case "bedrock" -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.bedrock").withStyle(ChatFormatting.YELLOW);
            default -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.tag", mode).withStyle(ChatFormatting.YELLOW);
        };
    }

    private void tip(GuiGraphics graphics, int mouseX, int mouseY, String... lines) {
        List<Component> text = new ArrayList<>();
        for (String line : lines) text.add(Component.literal(line));
        graphics.renderComponentTooltip(font, text, mouseX, mouseY);
    }

    /** GuiInfoContainer hover rule: x <= mouse < x + w, y < mouse <= y + h. */
    private boolean hover(double mouseX, double mouseY, int x, int y, int width, int height) {
        return K3BGui.hover((int) mouseX, (int) mouseY, leftPos + x, topPos + y, width, height);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        switch (menu.kind()) {
            case BOXER -> {
                graphics.blit(BOXER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
                graphics.blit(BOXER, leftPos + 151, topPos + 34, 176, menu.primaryMode() * 18, 18, 18);
            }
            case EXTRACTOR -> {
                graphics.blit(EJECTOR, leftPos, topPos, 0, 0, imageWidth, imageHeight);
                if (menu.maxEject()) graphics.blit(EJECTOR, leftPos + 187, topPos + 34, 212, 0, 18, 18);
                graphics.blit(EJECTOR, leftPos + 139, topPos + (menu.primaryMode() != 0 ? 33 : 47), 212, 18, 3, 6);
            }
            case GRABBER -> {
                graphics.blit(GRABBER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
                graphics.blit(GRABBER, leftPos + 108, topPos + (menu.primaryMode() != 0 ? 33 : 47), 176, 0, 3, 6);
            }
            case INSERTER -> {
                graphics.blit(INSERTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
                if (menu.primaryMode() != 0) graphics.blit(INSERTER, leftPos + 151, topPos + 34, 176, 0, 18, 18);
            }
            case ROUTER -> {
                graphics.blit(ROUTER, leftPos, topPos, 0, 0, 256, 93);
                graphics.blit(ROUTER, leftPos + 39, topPos + 93, 39, 93, 176, 108);
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 3; k++) {
                        graphics.blit(ROUTER, leftPos + 7 + j * 222, topPos + 16 + k * 26, 238, 93 + menu.routerMode(j * 3 + k) * 18, 18, 18);
                    }
                }
            }
            case UNBOXER -> graphics.blit(UNBOXER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case PARTITIONER, SPLITTER -> { }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        CraneMachineBlock.Kind kind = menu.kind();
        int titleX = imageWidth / 2 - font.width(title) / 2 - (kind == CraneMachineBlock.Kind.INSERTER ? 18 : 0);
        int titleY = kind == CraneMachineBlock.Kind.INSERTER || kind == CraneMachineBlock.Kind.ROUTER ? 5 : 6;
        graphics.drawString(font, title, titleX, titleY, 0x404040, false);
        int invX = switch (kind) {
            case ROUTER -> 8 + 39;
            case EXTRACTOR -> 26;
            default -> 8;
        };
        graphics.drawString(font, playerInventoryTitle, invX, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        switch (menu.kind()) {
            case BOXER, INSERTER -> {
                if (hover(mouseX, mouseY, 151, 34, 18, 18)) press(0);
            }
            case EXTRACTOR -> {
                if (hover(mouseX, mouseY, 187, 34, 18, 18)) press(1);
                if (hover(mouseX, mouseY, 128, 30, 14, 26)) press(0);
            }
            case GRABBER -> {
                if (hover(mouseX, mouseY, 97, 30, 14, 26)) press(0);
            }
            case ROUTER -> {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 3; k++) {
                        if (hover(mouseX, mouseY, 7 + j * 222, 16 + k * 26, 18, 18)) press(j * 3 + k);
                    }
                }
            }
            default -> { }
        }
        return handled;
    }

    private void press(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}

package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.PneumaticTubeMenu;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** [parity R2] GUIPneumoTube: compressor (gui_pneumatic_pipe) and endpoint-only (gui_pneumatic_endpoint) views. */
public final class PneumaticTubeScreen extends AbstractContainerScreen<PneumaticTubeMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/storage/gui_pneumatic_pipe.png");
    private static final ResourceLocation TEXTURE_ENDPOINT = AtomicLegacy.id("textures/gui/storage/gui_pneumatic_endpoint.png");

    public PneumaticTubeScreen(PneumaticTubeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.atomiclegacy.pneumatic_tube"));
        imageWidth = 176;
        imageHeight = 185;
    }

    @Nullable
    private PneumaticTubeBlockEntity tube() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.pos()) instanceof PneumaticTubeBlockEntity t ? t : null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        PneumaticTubeBlockEntity tube = tube();
        if (tube == null) return;
        if (!menu.endpointOnly()) {
            if (hover(mouseX, mouseY, 7, 16, 18, 18)) {
                graphics.renderComponentTooltip(font, HbmFluidTooltip.forTank(tube.compair().type(), tube.compair().amount(),
                        tube.compair().capacity(), tube.compair().pressure()), mouseX, mouseY);
            }
            if (hover(mouseX, mouseY, 7, 52, 18, 18)) {
                tip(graphics, mouseX, mouseY, (tube.redstone() ? ChatFormatting.GREEN + "ON " : ChatFormatting.RED + "OFF ") + ChatFormatting.RESET + "with Redstone");
            }
            if (hover(mouseX, mouseY, 6, 36, 20, 8)) {
                tip(graphics, mouseX, mouseY, "Compressor: " + tube.compair().pressure() + " PU",
                        "Max range: " + PneumaticTubeBlockEntity.getRangeFromPressure(tube.compair().pressure()) + "m");
            }
            if (hover(mouseX, mouseY, 151, 16, 18, 18)) {
                tip(graphics, mouseX, mouseY, ChatFormatting.YELLOW + "Receiver order:",
                        tube.receiveOrder() == PneumaticNetworkManager.RECEIVE_ROBIN ? "Round robin" : "Random");
            }
            if (hover(mouseX, mouseY, 151, 52, 18, 18)) {
                tip(graphics, mouseX, mouseY, ChatFormatting.YELLOW + "Provider slot order:",
                        tube.sendOrder() == PneumaticNetworkManager.SEND_FIRST ? "First to last"
                                : tube.sendOrder() == PneumaticNetworkManager.SEND_LAST ? "Last to first" : "Random");
            }
        }
        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < PneumaticTubeBlockEntity.FILTER_SLOTS; i++) {
                Slot slot = menu.getSlot(i);
                String mode = tube.mode(i);
                if (mode != null && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    graphics.renderComponentTooltip(font, List.of(
                            Component.translatable("tooltip.atomiclegacy.autocrafter.change").withStyle(ChatFormatting.RED),
                            modeLabel(mode)), mouseX, mouseY - 30);
                }
            }
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

    private boolean hover(double mouseX, double mouseY, int x, int y, int width, int height) {
        return K3BGui.hover((int) mouseX, (int) mouseY, leftPos + x, topPos + y, width, height);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = menu.endpointOnly() ? TEXTURE_ENDPOINT : TEXTURE;
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        PneumaticTubeBlockEntity tube = tube();
        if (tube == null) return;
        graphics.blit(texture, leftPos + 139, topPos + (tube.whitelist() ? 33 : 47), 176, 0, 3, 6);
        if (!menu.endpointOnly()) {
            if (tube.redstone()) graphics.blit(texture, leftPos + 7, topPos + 52, 179, 0, 18, 18);
            graphics.blit(texture, leftPos + 151, topPos + 16, 197, 18 * tube.receiveOrder(), 18, 18);
            graphics.blit(texture, leftPos + 151, topPos + 52, 215, 18 * tube.sendOrder(), 18, 18);
            graphics.blit(texture, leftPos + 6 + 4 * (tube.compair().pressure() - 1), topPos + 36, 179, 18, 4, 8);
            K3LegacyGauge.draw(graphics, leftPos + 16, topPos + 25,
                    (double) tube.compair().amount() / (double) tube.compair().capacity(), 5, 2, 1, 0xCA6C43, 0xAB4223);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 5, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!menu.endpointOnly()) {
            if (hover(mouseX, mouseY, 7, 52, 18, 18)) press(1);
            if (hover(mouseX, mouseY, 6, 36, 20, 8)) press(2);
            if (hover(mouseX, mouseY, 151, 16, 18, 18)) press(4);
            if (hover(mouseX, mouseY, 151, 52, 18, 18)) press(3);
        }
        if (hover(mouseX, mouseY, 128, 30, 14, 26)) press(0);
        return handled;
    }

    private void press(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}

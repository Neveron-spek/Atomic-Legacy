package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.RttyConfigMenu;
import com.atomiclegacy.inventory.gui.widget.SearchEditBox;
import com.atomiclegacy.network.RadioTorchControlPayload;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** [parity R2] GUICounterTorch: 218x238, three 10-character frequencies, three pattern slots, polling and save. */
public final class RadioTorchCounterScreen extends AbstractContainerScreen<RttyConfigMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/machine/gui_rtty_counter.png");
    private final EditBox[] frequency = new EditBox[3];

    public RadioTorchCounterScreen(RttyConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.atomiclegacy.rtty_counter"));
        this.imageWidth = 218;
        this.imageHeight = 238;
    }

    @Override
    protected void init() {
        super.init();
        RadioTorchBlockEntity counter = counter();
        for (int i = 0; i < 3; i++) {
            SearchEditBox field = new SearchEditBox(font, leftPos + 29, topPos + 21 + 44 * i, 86, 14, Component.empty());
            field.setTextColor(0x00FF00);
            field.setTextColorUneditable(0x00FF00);
            field.setBordered(false);
            field.setMaxLength(10);
            field.setValue(counter == null ? "" : counter.counterChannel(i));
            frequency[i] = field;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        RadioTorchBlockEntity counter = counter();
        if (counter != null) {
            if (K3BGui.hover(mouseX, mouseY, leftPos + 193, topPos + 8, 18, 18)) {
                graphics.renderComponentTooltip(font, List.of(Component.literal(counter.polling() ? "Polling" : "State Change")), mouseX, mouseY);
            }
            if (K3BGui.hover(mouseX, mouseY, leftPos + 193, topPos + 30, 18, 18)) {
                graphics.renderComponentTooltip(font, List.of(Component.literal("Save Settings")), mouseX, mouseY);
            }
            if (menu.getCarried().isEmpty()) {
                for (int i = 0; i < 3; i++) {
                    Slot slot = menu.getSlot(i);
                    String mode = counter.counterMode(i);
                    if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && mode != null) {
                        graphics.renderComponentTooltip(font, List.of(
                                Component.translatable("tooltip.atomiclegacy.autocrafter.change").withStyle(ChatFormatting.RED),
                                modeLabel(mode)), mouseX, mouseY - 30);
                    }
                }
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static Component modeLabel(String mode) {
        return switch (mode) {
            case "exact" -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.exact").withStyle(ChatFormatting.YELLOW);
            case "wildcard" -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.wildcard").withStyle(ChatFormatting.YELLOW);
            case "bedrock" -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.bedrock").withStyle(ChatFormatting.YELLOW);
            default -> Component.translatable("tooltip.atomiclegacy.autocrafter.mode.tag", mode).withStyle(ChatFormatting.YELLOW);
        };
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        RadioTorchBlockEntity counter = counter();
        if (counter != null && counter.polling()) graphics.blit(TEXTURE, leftPos + 193, topPos + 8, 218, 0, 18, 18);
        for (EditBox box : frequency) box.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 184 / 2 - font.width(title) / 2, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 16, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        for (EditBox box : frequency) box.setFocused(box.mouseClicked(mouseX, mouseY, button));
        if (K3BGui.hover((int) mouseX, (int) mouseY, leftPos + 193, topPos + 8, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", true);
            send(data);
            return true;
        }
        if (K3BGui.hover((int) mouseX, (int) mouseY, leftPos + 193, topPos + 30, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            for (int j = 0; j < 3; j++) data.putString("c" + j, frequency[j].getValue());
            send(data);
            return true;
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256) {
            for (EditBox box : frequency) {
                if (box.isFocused()) {
                    box.keyPressed(keyCode, scanCode, modifiers);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : frequency) if (box.isFocused()) return box.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    private void send(CompoundTag data) {
        PacketDistributor.sendToServer(new RadioTorchControlPayload(menu.blockPos(), data));
    }

    @Nullable
    private RadioTorchBlockEntity counter() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioTorchBlockEntity radio ? radio : null;
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}

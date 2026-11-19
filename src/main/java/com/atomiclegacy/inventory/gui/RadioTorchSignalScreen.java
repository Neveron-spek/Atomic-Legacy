package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.RadioTorchBlock;
import com.atomiclegacy.inventory.container.RttyConfigMenu;
import com.atomiclegacy.inventory.gui.widget.SearchEditBox;
import com.atomiclegacy.network.RadioTorchControlPayload;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * [parity R2] GUIScreenRadioTorch (sender / receiver) and GUIScreenRadioTorchLogic (logic receiver):
 * 256x204, frequency field, 16 mapping fields, mapping/order, polling and save buttons.
 */
public final class RadioTorchSignalScreen extends AbstractContainerScreen<RttyConfigMenu> {
    private static final ResourceLocation TEXTURE_SENDER = AtomicLegacy.id("textures/gui/machine/gui_rtty_sender.png");
    private static final ResourceLocation TEXTURE_RECEIVER = AtomicLegacy.id("textures/gui/machine/gui_rtty_receiver.png");
    private static final ResourceLocation TEXTURE_LOGIC = AtomicLegacy.id("textures/gui/machine/gui_rtty_logic_receiver.png");
    private static final int MAX_CHAN_LENGTH = 15;

    private final boolean logic;
    private EditBox frequency;
    private final EditBox[] remap = new EditBox[16];
    private final int[] conditions = new int[16];

    public RadioTorchSignalScreen(RttyConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable(switch (menu.kind()) {
            case SENDER -> "container.atomiclegacy.rtty_sender";
            case LOGIC -> "container.atomiclegacy.rtty_logic";
            default -> "container.atomiclegacy.rtty_receiver";
        }));
        this.logic = menu.kind() == RadioTorchBlock.Kind.LOGIC;
        this.imageWidth = 256;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        RadioTorchBlockEntity radio = radio();
        int oX = 4;
        int oY = 4;
        int in = menu.kind() == RadioTorchBlock.Kind.SENDER ? 18 : 0;
        frequency = field(leftPos + 25 + oX, topPos + 17 + oY, 90 - oX * 2, MAX_CHAN_LENGTH, radio == null ? "" : radio.channel());
        for (int i = 0; i < 16; i++) {
            String value = radio == null ? "" : radio.mapping(i);
            if (logic) {
                remap[i] = field(leftPos + 7 + (130 * (i / 8)) + oX + 18, topPos + 53 + (18 * (i % 8)) + oY, 54 - oX * 2, 15, value);
                conditions[i] = radio == null ? 0 : radio.condition(i);
            } else {
                remap[i] = field(leftPos + 7 + (130 * (i / 8)) + oX + in, topPos + 53 + (18 * (i % 8)) + oY, 90 - oX * 2, 32, value);
            }
        }
    }

    private EditBox field(int x, int y, int width, int maxLength, String value) {
        SearchEditBox field = new SearchEditBox(font, x, y, width, 14, Component.empty());
        field.setTextColor(0x00FF00);
        field.setTextColorUneditable(0x00FF00);
        field.setBordered(false);
        field.setMaxLength(maxLength);
        field.setValue(value == null ? "" : value);
        return field;
    }

    private boolean customMap() {
        RadioTorchBlockEntity radio = radio();
        return radio != null && radio.customMap();
    }

    private boolean fieldsActive() {
        return logic || customMap();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        frequency.render(graphics, mouseX, mouseY, partialTick);
        if (fieldsActive()) for (EditBox box : remap) box.render(graphics, mouseX, mouseY, partialTick);
        tooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RadioTorchBlockEntity radio = radio();
        boolean polling = radio != null && radio.polling();
        if (logic) {
            graphics.blit(TEXTURE_LOGIC, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            if (radio != null && radio.descending()) graphics.blit(TEXTURE_LOGIC, leftPos + 137, topPos + 17, 0, 204, 18, 18);
            if (polling) graphics.blit(TEXTURE_LOGIC, leftPos + 173, topPos + 17, 0, 222, 18, 18);
            for (int i = 0; i < 16; i++) {
                int x = leftPos + 7 + (130 * (i / 8));
                int y = topPos + 53 + (18 * (i % 8));
                if (radio == null || radio.mapping(i).isEmpty()) {
                    if (conditions[i] != 0) graphics.blit(TEXTURE_LOGIC, x, y, 18 + conditions[i] * 18, 222, 18, 18);
                } else {
                    graphics.blit(TEXTURE_LOGIC, x, y, 18 + conditions[i] * 18, 204, 18, 18);
                    graphics.blit(TEXTURE_LOGIC, leftPos + 85 + (130 * (i / 8)), topPos + 57 + (18 * (i % 8)), 198, 204, 14, 10);
                }
            }
            return;
        }
        ResourceLocation texture = menu.kind() == RadioTorchBlock.Kind.SENDER ? TEXTURE_SENDER : TEXTURE_RECEIVER;
        if (customMap()) {
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            graphics.blit(texture, leftPos + 137, topPos + 17, 0, 204, 18, 18);
            if (polling) graphics.blit(texture, leftPos + 173, topPos + 17, 0, 222, 18, 18);
        } else {
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, 35);
            graphics.blit(texture, leftPos, topPos + 35, 0, 197, imageWidth, 7);
            if (polling) graphics.blit(texture, leftPos + 173, topPos + 17, 0, 222, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
    }

    private void tooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        RadioTorchBlockEntity radio = radio();
        if (radio == null) return;
        if (hover(mouseX, mouseY, 137)) {
            String text = logic ? (radio.descending() ? "Descending Order" : "Ascending Order")
                    : (radio.customMap() ? "Custom Mapping" : "Redstone Passthrough");
            graphics.renderComponentTooltip(font, List.of(Component.literal(text)), mouseX, mouseY);
        }
        if (hover(mouseX, mouseY, 173)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal(radio.polling() ? "Polling" : "State Change")), mouseX, mouseY);
        }
        if (hover(mouseX, mouseY, 209)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal("Save Settings")), mouseX, mouseY);
        }
        if (logic) {
            int j = conditionAt(mouseX, mouseY);
            if (j >= 0) graphics.renderComponentTooltip(font,
                    List.of(Component.translatable("gui.atomiclegacy.rtty_logic.cond" + conditions[j])), mouseX, mouseY);
        }
    }

    private boolean hover(double mouseX, double mouseY, int x) {
        return K3BGui.hover((int) mouseX, (int) mouseY, leftPos + x, topPos + 17, 18, 18);
    }

    private int conditionAt(double x, double y) {
        for (int j = 0; j < 16; j++) {
            int bx = leftPos + 7 + (130 * (j / 8));
            int by = topPos + 53 + (18 * (j % 8));
            if (bx <= x && bx + 18 > x && by <= y && by + 18 > y) return j;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = false;
        frequency.setFocused(frequency.mouseClicked(mouseX, mouseY, button));
        handled |= frequency.isFocused();
        for (EditBox box : remap) {
            boolean focus = fieldsActive() && box.mouseClicked(mouseX, mouseY, button);
            box.setFocused(focus);
            handled |= focus;
        }
        RadioTorchBlockEntity radio = radio();
        if (radio != null && hover(mouseX, mouseY, 137)) {
            click();
            CompoundTag data = new CompoundTag();
            if (logic) data.putBoolean("d", !radio.descending());
            else data.putBoolean("m", !radio.customMap());
            send(data);
            handled = true;
        }
        if (radio != null && hover(mouseX, mouseY, 173)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("p", !radio.polling());
            send(data);
            handled = true;
        }
        if (hover(mouseX, mouseY, 209)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putString("c", frequency.getValue());
            for (int j = 0; j < 16; j++) data.putString("m" + j, remap[j].getValue());
            if (logic) for (int j = 0; j < 16; j++) data.putInt("c" + j, conditions[j]);
            send(data);
            handled = true;
        }
        if (logic) {
            int j = conditionAt(mouseX, mouseY);
            if (j >= 0) {
                click();
                conditions[j] = (conditions[j] + 1) % 10;
                handled = true;
            }
        }
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (logic) {
            int j = conditionAt(mouseX, mouseY);
            if (j >= 0) {
                if (scrollY > 0) conditions[j] = (conditions[j] + 1) % 10;
                if (scrollY < 0) conditions[j] = (conditions[j] + 9) % 10;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256) {
            if (frequency.isFocused() && frequency.keyPressed(keyCode, scanCode, modifiers)) return true;
            for (EditBox box : remap) if (box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (frequency.isFocused()) return true;
            for (EditBox box : remap) if (box.isFocused()) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (frequency.isFocused()) return frequency.charTyped(codePoint, modifiers);
        for (EditBox box : remap) if (box.isFocused()) return box.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    private void send(CompoundTag data) {
        PacketDistributor.sendToServer(new RadioTorchControlPayload(menu.blockPos(), data));
    }

    @Nullable
    private RadioTorchBlockEntity radio() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioTorchBlockEntity radio ? radio : null;
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

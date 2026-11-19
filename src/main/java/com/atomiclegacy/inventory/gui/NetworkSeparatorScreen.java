package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.network.NetworkSeparatorControlPayload;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** [parity U1: network_separator] GUIDiode (GuiScreen without container), layout and strings of 936c60c. */
public class NetworkSeparatorScreen extends Screen {
    protected final NetworkSeparatorBlockEntity diode;
    private EditBox textThroughput;
    private int priority;

    public NetworkSeparatorScreen(NetworkSeparatorBlockEntity diode) {
        super(Component.empty());
        this.diode = diode;
        this.priority = diode.priority.ordinal();
    }

    @Override
    protected void init() {
        textThroughput = new EditBox(font, this.width / 2 - 150, 100, 90, 20, Component.empty());
        textThroughput.setMaxLength(11);
        textThroughput.setValue("" + diode.configuredLimit);
        addRenderableWidget(textThroughput);
        addRenderableWidget(Button.builder(Component.literal(diode.priority.name()), button -> {
            this.priority++;
            if (priority >= PowerEndpoint.ConnectionPriority.VALUES.length) priority = 0;
            button.setMessage(Component.literal(NetworkSeparatorBlockEntity.byOrdinal(priority).name()));
        }).bounds(this.width / 2 + 20, 100, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, "Throughput:", this.width / 2 - 150, 80, 0xA0A0A0);
        graphics.drawString(font, "(max. 10,000,000,000 HE)", this.width / 2 - 150, 90, 0xA0A0A0);
        graphics.drawString(font, "Priority:", this.width / 2 + 20, 80, 0xA0A0A0);
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putByte("priority", (byte) priority);
        try {
            data.putLong("limit", Long.parseLong(textThroughput.getValue()));
        } catch (Exception ignored) {
        }
        PacketDistributor.sendToServer(new NetworkSeparatorControlPayload(diode.getBlockPos(), data));
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textThroughput.isFocused() && keyCode != 256) {
            return textThroughput.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

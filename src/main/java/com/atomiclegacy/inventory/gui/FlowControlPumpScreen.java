package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.network.FlowControlPumpControlPayload;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** [parity T1: flow_control_pump] GUIPump (GuiScreen without container), layout and strings of 936c60c. */
public class FlowControlPumpScreen extends Screen {
    protected final FlowControlPumpBlockEntity pump;
    private EditBox textPlacementPriority;
    private Button buttonPressure;
    private Button buttonPriority;
    private int pressure;
    private int priority;

    public FlowControlPumpScreen(FlowControlPumpBlockEntity pump) {
        super(Component.empty());
        this.pump = pump;
        this.pressure = pump.tank.pressure();
        this.priority = pump.priority.ordinal();
    }

    @Override
    protected void init() {
        textPlacementPriority = new EditBox(font, this.width / 2 - 150, 100, 90, 20, Component.empty());
        textPlacementPriority.setMaxLength(5);
        textPlacementPriority.setValue("" + pump.bufferSize);
        addRenderableWidget(textPlacementPriority);
        buttonPressure = addRenderableWidget(Button.builder(Component.literal(pressure + " PU"), button -> {
            this.pressure++;
            if (pressure > 5) pressure = 0;
            button.setMessage(Component.literal(pressure + " PU"));
        }).bounds(this.width / 2 - 50, 100, 90, 20).build());
        buttonPriority = addRenderableWidget(Button.builder(Component.literal(pump.priority.name()), button -> {
            this.priority++;
            if (priority >= PowerEndpoint.ConnectionPriority.VALUES.length) priority = 0;
            button.setMessage(Component.literal(FlowControlPumpBlockEntity.byOrdinal(priority).name()));
        }).bounds(this.width / 2 + 50, 100, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, "Throughput:", this.width / 2 - 150, 80, 0xA0A0A0);
        graphics.drawString(font, "(max. 10,000mB)", this.width / 2 - 150, 90, 0xA0A0A0);
        graphics.drawString(font, "Pressure:", this.width / 2 - 50, 80, 0xA0A0A0);
        graphics.drawString(font, "Priority:", this.width / 2 + 50, 80, 0xA0A0A0);
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putByte("pressure", (byte) pressure);
        data.putByte("priority", (byte) priority);
        try {
            data.putInt("capacity", Integer.parseInt(textPlacementPriority.getValue()));
        } catch (Exception ignored) {
        }
        PacketDistributor.sendToServer(new FlowControlPumpControlPayload(pump.getBlockPos(), data));
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textPlacementPriority.isFocused() && keyCode != 256) {
            return textPlacementPriority.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
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

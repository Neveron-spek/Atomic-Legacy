package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.BatterySocketBlockEntity;
import com.atomiclegacy.inventory.container.BatterySocketMenu;
import com.atomiclegacy.network.BatterySocketControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.NumberFormat;
import java.util.Locale;

public class BatterySocketScreen extends AbstractContainerScreen<BatterySocketMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/storage/gui_battery_socket.png");
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    public BatterySocketScreen(BatterySocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 181;
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

        // [parity e2] GUIBatterySocket#drawScreen: charge / delta and priority tooltips
        BatterySocketBlockEntity socket = this.menu.socket();
        if (socket == null) {
            return;
        }
        if (socket.capacity() > 0L && K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + 69 - 52, 34, 52)) {
            long delta = socket.delta();
            String deltaText = K3GuiText.shortNumber(Math.abs(delta)) + "HE/s";
            Component deltaLine;
            if (delta > 0L) {
                deltaLine = Component.literal("+" + deltaText).withStyle(net.minecraft.ChatFormatting.GREEN);
            } else if (delta < 0L) {
                deltaLine = Component.literal("-" + deltaText).withStyle(net.minecraft.ChatFormatting.RED);
            } else {
                deltaLine = Component.literal("+" + deltaText).withStyle(net.minecraft.ChatFormatting.YELLOW);
            }
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(K3GuiText.shortNumber(socket.power()) + "/" + K3GuiText.shortNumber(socket.capacity()) + "HE"),
                    deltaLine), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 125, this.topPos + 35, 16, 16)) {
            String lang = switch (socket.priority()) {
                case LOW -> "low";
                case HIGH -> "high";
                default -> "normal";
            };
            java.util.List<Component> priority = new java.util.ArrayList<>();
            priority.add(Component.translatable("battery.priority." + lang));
            priority.add(Component.translatable("battery.priority.recommended"));
            priority.addAll(LegacyInfoText.lines("battery.priority." + lang + ".desc"));
            guiGraphics.renderComponentTooltip(this.font, priority, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        BatterySocketBlockEntity socket = this.menu.socket();
        long power = socket == null ? 0L : socket.power();
        long capacity = socket == null ? 0L : socket.capacity();
        if (capacity > 0L) {
            long scaledPower = power;
            long scaledCapacity = capacity;
            if (scaledPower > Long.MAX_VALUE / 100L) {
                scaledPower /= 100L;
                scaledCapacity /= 100L;
            }
            int fill = (int) (scaledPower * 52L / Math.max(1L, scaledCapacity));
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 69 - fill, 176, 52 - fill, 34, fill);
        }
        int low = socket == null ? 0 : socket.redLow();
        int high = socket == null ? 2 : socket.redHigh();
        int priority = socket == null ? 1 : socket.priority().ordinal();
        graphics.blit(TEXTURE, this.leftPos + 106, this.topPos + 16, 176, 52 + low * 18, 18, 18);
        graphics.blit(TEXTURE, this.leftPos + 106, this.topPos + 52, 176, 52 + high * 18, 18, 18);
        graphics.blit(TEXTURE, this.leftPos + 125, this.topPos + 35, 194, 52 + priority * 16 - 16, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        // [parity e2] the socket GUI has no on-screen charge text (that is GUIBatteryREDD)
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, 106, 16, 18, 18)) {
            send(BatterySocketControlPayload.ACTION_LOW);
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, 106, 52, 18, 18)) {
            send(BatterySocketControlPayload.ACTION_HIGH);
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, 125, 35, 16, 16)) {
            send(BatterySocketControlPayload.ACTION_PRIORITY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new BatterySocketControlPayload(this.menu.blockPos(), action));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX
                && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY
                && this.topPos + y + height >= mouseY;
    }
}

package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.LegacyBatteryMenu;
import com.atomiclegacy.network.BatterySocketControlPayload;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.tileentity.LegacyBatteryBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** [parity d2] GUIMachineBattery of 1.7.10. */
public final class LegacyBatteryScreen extends AbstractContainerScreen<LegacyBatteryMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/storage/gui_battery.png");

    public LegacyBatteryScreen(LegacyBatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private long power() {
        LegacyBatteryBlockEntity battery = this.menu.battery();
        return battery == null ? 0L : battery.getPower();
    }

    private long maxPower() {
        LegacyBatteryBlockEntity battery = this.menu.battery();
        return battery == null ? 0L : battery.getMaxPower();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        LegacyBatteryBlockEntity battery = this.menu.battery();
        if (battery == null) {
            return;
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + 17, 52, 52)) {
            long delta = battery.delta();
            String deltaText = K3GuiText.shortNumber(Math.abs(delta)) + "HE/s";
            Component deltaLine = delta > 0 ? Component.literal("+" + deltaText).withStyle(ChatFormatting.GREEN)
                    : delta < 0 ? Component.literal("-" + deltaText).withStyle(ChatFormatting.RED)
                    : Component.literal("+" + deltaText).withStyle(ChatFormatting.YELLOW);
            graphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(battery.getPower(), battery.getMaxPower()), deltaLine), mouseX, mouseY);
        }
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 152, this.topPos + 35, 16, 16)) {
            String lang = switch (battery.priority()) {
                case LOW -> "low";
                case NORMAL -> "normal";
                case HIGH -> "high";
                default -> null;
            };
            List<Component> priority = new ArrayList<>();
            priority.add(Component.translatable("battery.priority." + lang));
            priority.add(Component.translatable("battery.priority.recommended"));
            for (String line : Component.translatable("battery.priority." + lang + ".desc").getString().split("\\$")) {
                priority.add(Component.literal(line));
            }
            graphics.renderComponentTooltip(this.font, priority, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (K3GuiText.hover(x, y, this.leftPos + 133, this.topPos + 16, 18, 18)) {
            send(BatterySocketControlPayload.ACTION_LOW);
        }
        if (K3GuiText.hover(x, y, this.leftPos + 133, this.topPos + 52, 18, 18)) {
            send(BatterySocketControlPayload.ACTION_HIGH);
        }
        if (K3GuiText.hover(x, y, this.leftPos + 152, this.topPos + 35, 16, 16)) {
            send(BatterySocketControlPayload.ACTION_PRIORITY);
        }
        return super.mouseClicked(x, y, button);
    }

    private void send(int action) {
        K3GuiText.click();
        PacketDistributor.sendToServer(new BatterySocketControlPayload(this.menu.blockPos(), action));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String name = this.title.getString() + " (" + power() + " HE)";
        graphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        LegacyBatteryBlockEntity battery = this.menu.battery();
        if (battery != null && battery.getPower() > 0) {
            int i = (int) battery.getPowerRemainingScaled(52);
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 69 - i, 176, 52 - i, 52, i);
        }
        int low = battery == null ? 0 : battery.redLow();
        graphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 16, 176, 52 + low * 18, 18, 18);
        int high = battery == null ? 2 : battery.redHigh();
        graphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 52, 176, 52 + high * 18, 18, 18);
        PowerEndpoint.ConnectionPriority priority = battery == null ? PowerEndpoint.ConnectionPriority.LOW : battery.priority();
        graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 35, 194, 52 + priority.ordinal() * 16 - 16, 16, 16);
    }
}

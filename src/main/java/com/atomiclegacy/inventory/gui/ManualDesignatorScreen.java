package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyManualDesignatorItem;
import com.atomiclegacy.network.ManualDesignatorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** [parity P1: designator_manual] 1.7.10 GUIScreenDesignator: 176x178, two rows of +/- 1/5/10/50/100 per axis. */
public final class ManualDesignatorScreen extends Screen {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_designator.png");
    private static final int X_SIZE = 176, Y_SIZE = 178;
    private final Player player;
    private final List<FolderButton> buttons = new ArrayList<>();
    private int guiLeft, guiTop, shownX, shownZ;

    private ManualDesignatorScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    public static void open(Player player) {
        Minecraft.getInstance().setScreen(new ManualDesignatorScreen(player));
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;
        shownX = 0;
        shownZ = 0;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof LegacyManualDesignatorItem && stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            shownX = tag.getInt("xCoord");
            shownZ = tag.getInt("zCoord");
        }
        buttons.clear();
        int[] values = {1, 5, 10, 50, 100};
        for (int r = 0; r < 2; r++) {
            int dy = r * 72;
            for (int i = 0; i < 5; i++) buttons.add(new FolderButton(guiLeft + 25 + 27 * i, guiTop + 26 + dy, i, 0, r, values[i], null));
            for (int i = 0; i < 5; i++) buttons.add(new FolderButton(guiLeft + 25 + 27 * i, guiTop + 62 + dy, 5 + i, 1, r, values[i], null));
            buttons.add(new FolderButton(guiLeft + 133, guiTop + 44 + dy, 10, 2, r, 0,
                    r == 0 ? "Set coord to current X position..." : "Set coord to current Z position..."));
        }
    }

    @Override
    public void tick() {
        if (!(player.getMainHandItem().getItem() instanceof LegacyManualDesignatorItem)) onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) { // [parity g2] keyTyped
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(TEXTURE, guiLeft, guiTop, 0, 0, X_SIZE, Y_SIZE);
        for (FolderButton b : buttons)
            g.blit(TEXTURE, b.x, b.y, b.isMouseOn(mouseX, mouseY) ? 176 + 18 : 176, b.type * 18, 18, 18);
        String x = "X: " + shownX, z = "Z: " + shownZ;
        g.drawString(font, x, guiLeft + X_SIZE / 2 - font.width(x) / 2, guiTop + 50, 4210752, false);
        g.drawString(font, z, guiLeft + X_SIZE / 2 - font.width(z) / 2, guiTop + 50 + 18 * 4, 4210752, false);
        for (FolderButton b : buttons)
            if (b.isMouseOn(mouseX, mouseY) && b.info != null) g.renderTooltip(font, Component.literal(b.info), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (FolderButton b : buttons) if (b.isMouseOn((int) mouseX, (int) mouseY)) b.execute();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private final class FolderButton {
        final int x, y, type, operator, reference, value;
        final String info;

        FolderButton(int x, int y, int type, int operator, int reference, int value, String info) {
            this.x = x; this.y = y; this.type = type; this.operator = operator; this.reference = reference; this.value = value; this.info = info;
        }

        boolean isMouseOn(int mx, int my) {
            return x <= mx && x + 18 > mx && y < my && y + 18 >= my;
        }

        void execute() {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketDistributor.sendToServer(new ManualDesignatorPayload(operator, value, reference));
            if (operator == 2) {
                if (reference == 0) shownX = (int) Math.round(player.getX());
                else shownZ = (int) Math.round(player.getZ());
                return;
            }
            int result = operator == 0 ? value : -value;
            if (reference == 0) shownX += result;
            else shownZ += result;
        }
    }
}

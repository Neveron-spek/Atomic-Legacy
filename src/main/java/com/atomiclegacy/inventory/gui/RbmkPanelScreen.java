package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.network.RbmkPanelControlPayload;
import com.atomiclegacy.tileentity.RbmkPanelBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * [parity T2] GUIScreenRBMKGauge, GUIScreenRBMKKeyPad, GUIScreenRBMKLever, GUIScreenRBMKIndicator,
 * GUIScreenRBMKDisplay (numitron) and GUIScreenRBMKGraph: client-only screens without a container.
 */
public abstract class RbmkPanelScreen extends Screen {
    /** GUIScreenRadioTorch.MAX_CHAN_LENGTH */
    public static final int MAX_CHAN_LENGTH = 15;

    protected final RbmkPanelBlockEntity panel;
    private final ResourceLocation texture;
    protected final int xSize;
    protected final int ySize;
    protected int guiLeft;
    protected int guiTop;

    protected RbmkPanelScreen(RbmkPanelBlockEntity panel, String texture, String titleKey, int xSize, int ySize) {
        super(Component.translatable(titleKey));
        this.panel = panel;
        this.texture = AtomicLegacy.id("textures/gui/machine/" + texture + ".png");
        this.xSize = xSize;
        this.ySize = ySize;
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
        initFields(4, 4);
    }

    protected abstract void initFields(int oX, int oY);

    protected abstract void drawToggles(GuiGraphics graphics, ResourceLocation texture);

    /** Returns true when a toggle or the save button handled the click. */
    protected abstract boolean clickToggles(double x, double y);

    /** GUIScreenRBMKKeyPad.setupTextFieldStandard */
    protected EditBox field(int x, int y, int w, int h, int length, String def) {
        EditBox box = new EditBox(font, x, y, w, h, Component.empty());
        box.setTextColor(0x00ff00);
        box.setTextColorUneditable(0x00ff00);
        box.setBordered(false);
        box.setMaxLength(length);
        box.setValue(def != null ? def : "");
        addRenderableWidget(box);
        return box;
    }

    protected static boolean inside(double x, double y, int left, int w, int top, int h) {
        return left <= x && left + w > x && top < y && top + h >= y;
    }

    protected void press(float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    protected void send(CompoundTag data) {
        PacketDistributor.sendToServer(new RbmkPanelControlPayload(panel.getBlockPos(), data));
    }

    protected static void putHexColor(CompoundTag data, String key, EditBox box) {
        try { data.putInt(key, Integer.parseInt(box.getValue(), 16)); } catch (Exception ignored) { }
    }

    protected static void putInt(CompoundTag data, String key, EditBox box) {
        try { data.putInt(key, Integer.parseInt(box.getValue())); } catch (Exception ignored) { }
    }

    protected static String hex(int color) {
        String col = Integer.toHexString(color);
        while (col.length() < 6) col = "0" + col;
        return col;
    }

    protected static byte mask(boolean[] flags) {
        byte mask = 0;
        for (int i = 0; i < flags.length; i++) if (flags[i]) mask |= (byte) (1 << i);
        return mask;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBackgroundTexture(graphics, texture);
        drawToggles(graphics, texture);
    }

    protected void drawBackgroundTexture(GuiGraphics graphics, ResourceLocation texture) {
        graphics.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        String name = title.getString();
        graphics.drawString(font, name, guiLeft + xSize / 2 - font.width(name) / 2, guiTop + 6, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickToggles(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = getFocused() instanceof EditBox box && box.isFocused();
        if (typing && getFocused().keyPressed(keyCode, scanCode, modifiers)) return true;
        if (!typing && minecraft != null && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* ------------------------------------------------------------------ */

    public static final class Gauge extends RbmkPanelScreen {
        private final EditBox[] color = new EditBox[4];
        private final EditBox[] label = new EditBox[4];
        private final EditBox[] rtty = new EditBox[4];
        private final EditBox[] min = new EditBox[4];
        private final EditBox[] max = new EditBox[4];
        private final boolean[] active = new boolean[4];
        private final boolean[] polling = new boolean[4];

        public Gauge(RbmkPanelBlockEntity panel) {
            super(panel, "gui_rbmk_gauge", "container.rbmkGauge", 256, 204);
        }

        @Override
        protected void initFields(int oX, int oY) {
            for (int i = 0; i < 4; i++) {
                RbmkPanelBlockEntity.GaugeUnit unit = panel.gauges[i];
                color[i] = field(guiLeft + 27 + oX, guiTop + 55 + oY + i * 36, 72 - oX * 2, 14, 6, hex(unit.color));
                label[i] = field(guiLeft + 175 + oX, guiTop + 55 + oY + i * 36, 72 - oX * 2, 14, 15, unit.label);
                rtty[i] = field(guiLeft + 27 + oX, guiTop + 73 + oY + i * 36, 72 - oX * 2, 14, MAX_CHAN_LENGTH, unit.rtty);
                min[i] = field(guiLeft + 121 + oX, guiTop + 73 + oY + i * 36, 52 - oX * 2, 14, 32, unit.min + "");
                max[i] = field(guiLeft + 195 + oX, guiTop + 73 + oY + i * 36, 52 - oX * 2, 14, 32, unit.max + "");
                active[i] = unit.active;
                polling[i] = unit.polling;
            }
        }

        @Override
        protected void drawToggles(GuiGraphics graphics, ResourceLocation texture) {
            for (int i = 0; i < 4; i++) {
                if (active[i]) graphics.blit(texture, guiLeft + 111, guiTop + i * 36 + 54, 18, 204, 16, 16);
                if (polling[i]) graphics.blit(texture, guiLeft + 128, guiTop + i * 36 + 53, 0, 204, 18, 18);
            }
        }

        @Override
        protected boolean clickToggles(double x, double y) {
            for (int i = 0; i < 4; i++) {
                if (inside(x, y, guiLeft + 111, 16, guiTop + i * 36 + 54, 16)) {
                    active[i] = !active[i];
                    press(0.5F + (active[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 128, 18, guiTop + i * 36 + 53, 18)) {
                    polling[i] = !polling[i];
                    press(0.5F + (polling[i] ? 0.25F : 0F));
                    return true;
                }
            }
            if (inside(x, y, guiLeft + 209, 18, guiTop + 17, 18)) {
                press(1.0F);
                CompoundTag data = new CompoundTag();
                data.putByte("active", mask(active));
                data.putByte("polling", mask(polling));
                for (int i = 0; i < 4; i++) {
                    putHexColor(data, "color" + i, color[i]);
                    data.putString("label" + i, label[i].getValue());
                    data.putString("rtty" + i, rtty[i].getValue());
                    putInt(data, "min" + i, min[i]);
                    putInt(data, "max" + i, max[i]);
                }
                send(data);
                return true;
            }
            return false;
        }
    }

    public static final class KeyPad extends RbmkPanelScreen {
        private final EditBox[] color = new EditBox[4];
        private final EditBox[] label = new EditBox[4];
        private final EditBox[] rtty = new EditBox[4];
        private final EditBox[] cmd = new EditBox[4];
        private final boolean[] active = new boolean[4];
        private final boolean[] polling = new boolean[4];

        public KeyPad(RbmkPanelBlockEntity panel) {
            super(panel, "gui_rbmk_keypad", "container.rbmkKeyPad", 256, 204);
        }

        @Override
        protected void initFields(int oX, int oY) {
            for (int i = 0; i < 4; i++) {
                RbmkPanelBlockEntity.KeyUnit unit = panel.keys[i];
                color[i] = field(guiLeft + 27 + oX, guiTop + 55 + oY + i * 36, 72 - oX * 2, 14, 6, hex(unit.color));
                label[i] = field(guiLeft + 175 + oX, guiTop + 55 + oY + i * 36, 72 - oX * 2, 14, 15, unit.label);
                rtty[i] = field(guiLeft + 27 + oX, guiTop + 73 + oY + i * 36, 72 - oX * 2, 14, MAX_CHAN_LENGTH, unit.rtty);
                cmd[i] = field(guiLeft + 121 + oX, guiTop + 73 + oY + i * 36, 126 - oX * 2, 14, 32, unit.command);
                active[i] = unit.active;
                polling[i] = unit.polling;
            }
        }

        @Override
        protected void drawToggles(GuiGraphics graphics, ResourceLocation texture) {
            for (int i = 0; i < 4; i++) {
                if (active[i]) graphics.blit(texture, guiLeft + 111, guiTop + i * 36 + 54, 18, 204, 16, 16);
                if (polling[i]) graphics.blit(texture, guiLeft + 128, guiTop + i * 36 + 53, 0, 204, 18, 18);
            }
        }

        @Override
        protected boolean clickToggles(double x, double y) {
            for (int i = 0; i < 4; i++) {
                if (inside(x, y, guiLeft + 111, 16, guiTop + i * 36 + 54, 16)) {
                    active[i] = !active[i];
                    press(0.5F + (active[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 128, 18, guiTop + i * 36 + 53, 18)) {
                    polling[i] = !polling[i];
                    press(0.5F + (polling[i] ? 0.25F : 0F));
                    return true;
                }
            }
            if (inside(x, y, guiLeft + 209, 18, guiTop + 17, 18)) {
                press(1.0F);
                CompoundTag data = new CompoundTag();
                data.putByte("active", mask(active));
                data.putByte("polling", mask(polling));
                for (int i = 0; i < 4; i++) {
                    putHexColor(data, "color" + i, color[i]);
                    data.putString("label" + i, label[i].getValue());
                    data.putString("rtty" + i, rtty[i].getValue());
                    data.putString("cmd" + i, cmd[i].getValue());
                }
                send(data);
                return true;
            }
            return false;
        }
    }

    public static final class Lever extends RbmkPanelScreen {
        private final EditBox[] label = new EditBox[2];
        private final EditBox[] rtty = new EditBox[2];
        private final EditBox[] cmdOn = new EditBox[2];
        private final EditBox[] cmdOff = new EditBox[2];
        private final boolean[] active = new boolean[2];
        private final boolean[] polling = new boolean[2];

        public Lever(RbmkPanelBlockEntity panel) {
            super(panel, "gui_rbmk_lever", "container.rbmkLever", 256, 150);
        }

        @Override
        protected void initFields(int oX, int oY) {
            for (int i = 0; i < 2; i++) {
                RbmkPanelBlockEntity.LeverUnit unit = panel.levers[i];
                rtty[i] = field(guiLeft + 27 + oX, guiTop + 55 + oY + i * 54, 72 - oX * 2, 14, MAX_CHAN_LENGTH, unit.rtty);
                label[i] = field(guiLeft + 175 + oX, guiTop + 55 + oY + i * 54, 72 - oX * 2, 14, 15, unit.label);
                cmdOn[i] = field(guiLeft + 45 + oX, guiTop + 73 + oY + i * 54, 81 - oX * 2, 14, 32, unit.commandOn);
                cmdOff[i] = field(guiLeft + 166 + oX, guiTop + 73 + oY + i * 54, 81 - oX * 2, 14, 32, unit.commandOff);
                active[i] = unit.active;
                polling[i] = unit.polling;
            }
        }

        @Override
        protected void drawToggles(GuiGraphics graphics, ResourceLocation texture) {
            for (int i = 0; i < 2; i++) {
                if (active[i]) graphics.blit(texture, guiLeft + 111, guiTop + i * 54 + 54, 18, 150, 16, 16);
                if (polling[i]) graphics.blit(texture, guiLeft + 128, guiTop + i * 54 + 53, 0, 150, 18, 18);
            }
        }

        @Override
        protected boolean clickToggles(double x, double y) {
            for (int i = 0; i < 2; i++) {
                if (inside(x, y, guiLeft + 111, 16, guiTop + i * 54 + 54, 16)) {
                    active[i] = !active[i];
                    press(0.5F + (active[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 128, 18, guiTop + i * 54 + 53, 18)) {
                    polling[i] = !polling[i];
                    press(0.5F + (polling[i] ? 0.25F : 0F));
                    return true;
                }
            }
            if (inside(x, y, guiLeft + 209, 18, guiTop + 17, 18)) {
                press(1.0F);
                CompoundTag data = new CompoundTag();
                data.putByte("active", mask(active));
                data.putByte("polling", mask(polling));
                for (int i = 0; i < 2; i++) {
                    data.putString("label" + i, label[i].getValue());
                    data.putString("rtty" + i, rtty[i].getValue());
                    data.putString("cmdOn" + i, cmdOn[i].getValue());
                    data.putString("cmdOff" + i, cmdOff[i].getValue());
                }
                send(data);
                return true;
            }
            return false;
        }
    }

    public static final class Indicator extends RbmkPanelScreen {
        private final EditBox[] color = new EditBox[6];
        private final EditBox[] label = new EditBox[6];
        private final EditBox[] rtty = new EditBox[6];
        private final EditBox[] min = new EditBox[6];
        private final EditBox[] max = new EditBox[6];
        private final boolean[] active = new boolean[6];
        private final boolean[] polling = new boolean[6];

        public Indicator(RbmkPanelBlockEntity panel) {
            super(panel, "gui_rbmk_indicator", "container.rbmkIndicator", 256, 258);
        }

        @Override
        protected void initFields(int oX, int oY) {
            for (int i = 0; i < 6; i++) {
                RbmkPanelBlockEntity.IndicatorUnit unit = panel.indicators[i];
                color[i] = field(guiLeft + 27 + oX, guiTop + 37 + oY + i * 36, 72 - oX * 2, 14, 6, hex(unit.color));
                label[i] = field(guiLeft + 175 + oX, guiTop + 37 + oY + i * 36, 72 - oX * 2, 14, 15, unit.label);
                rtty[i] = field(guiLeft + 27 + oX, guiTop + 55 + oY + i * 36, 72 - oX * 2, 14, MAX_CHAN_LENGTH, unit.rtty);
                min[i] = field(guiLeft + 121 + oX, guiTop + 55 + oY + i * 36, 52 - oX * 2, 14, 32, unit.min + "");
                max[i] = field(guiLeft + 195 + oX, guiTop + 55 + oY + i * 36, 52 - oX * 2, 14, 32, unit.max + "");
                active[i] = unit.active;
                polling[i] = unit.polling;
            }
        }

        @Override
        protected void drawBackgroundTexture(GuiGraphics graphics, ResourceLocation texture) {
            graphics.blit(texture, guiLeft, guiTop, 0, 0, 256, 143);
            graphics.blit(texture, guiLeft, guiTop + 143, 0, 35, 256, 115);
        }

        @Override
        protected void drawToggles(GuiGraphics graphics, ResourceLocation texture) {
            for (int i = 0; i < 6; i++) {
                graphics.blit(texture, guiLeft + 102, guiTop + 112 + 36 + i * 36, 34 + 8 * i, 150, 8, 8);
                if (active[i]) graphics.blit(texture, guiLeft + 111, guiTop + i * 36 + 36, 18, 150, 16, 16);
                if (polling[i]) graphics.blit(texture, guiLeft + 128, guiTop + i * 36 + 35, 0, 150, 18, 18);
            }
        }

        @Override
        protected boolean clickToggles(double x, double y) {
            for (int i = 0; i < 6; i++) {
                if (inside(x, y, guiLeft + 111, 16, guiTop + i * 36 + 36, 16)) {
                    active[i] = !active[i];
                    press(0.5F + (active[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 128, 18, guiTop + i * 36 + 35, 18)) {
                    polling[i] = !polling[i];
                    press(0.5F + (polling[i] ? 0.25F : 0F));
                    return true;
                }
            }
            if (inside(x, y, guiLeft + 209, 18, guiTop + 8, 18)) {
                press(1.0F);
                CompoundTag data = new CompoundTag();
                data.putByte("active", mask(active));
                data.putByte("polling", mask(polling));
                for (int i = 0; i < 6; i++) {
                    putHexColor(data, "color" + i, color[i]);
                    data.putString("label" + i, label[i].getValue());
                    data.putString("rtty" + i, rtty[i].getValue());
                    putInt(data, "min" + i, min[i]);
                    putInt(data, "max" + i, max[i]);
                }
                send(data);
                return true;
            }
            return false;
        }
    }

    public static final class Numitron extends RbmkPanelScreen {
        private final EditBox[] label = new EditBox[2];
        private final EditBox[] rtty = new EditBox[2];
        private final boolean[] active = new boolean[2];
        private final boolean[] polling = new boolean[2];
        private final boolean[] shortenNumber = new boolean[2];
        private final boolean[] leadingZeroes = new boolean[2];

        public Numitron(RbmkPanelBlockEntity panel) {
            super(panel, "gui_rbmk_numitron", "container.rbmkNumitron", 256, 150);
        }

        @Override
        protected void initFields(int oX, int oY) {
            for (int i = 0; i < 2; i++) {
                RbmkPanelBlockEntity.DisplayUnit unit = panel.displays[i];
                label[i] = field(guiLeft + 27 + oX, guiTop + 73 + oY + i * 54, 85 - oX * 2, 14, 30, unit.label);
                rtty[i] = field(guiLeft + 27 + oX, guiTop + 55 + oY + i * 54, 85 - oX * 2, 14, MAX_CHAN_LENGTH, unit.rtty);
                active[i] = unit.active;
                polling[i] = unit.polling;
                shortenNumber[i] = unit.shortenNumber;
                leadingZeroes[i] = unit.leadingZeroes;
            }
        }

        @Override
        protected void drawToggles(GuiGraphics graphics, ResourceLocation texture) {
            for (int i = 0; i < 2; i++) {
                if (active[i]) graphics.blit(texture, guiLeft + 124, guiTop + i * 54 + 54, 18, 150, 16, 16);
                if (polling[i]) graphics.blit(texture, guiLeft + 159, guiTop + i * 54 + 53, 0, 150, 18, 18);
                if (shortenNumber[i]) graphics.blit(texture, guiLeft + 195, guiTop + i * 54 + 53, 34, 150, 18, 18);
                if (leadingZeroes[i]) graphics.blit(texture, guiLeft + 231, guiTop + i * 54 + 53, 52, 150, 18, 18);
            }
        }

        @Override
        protected boolean clickToggles(double x, double y) {
            for (int i = 0; i < 2; i++) {
                if (inside(x, y, guiLeft + 124, 16, guiTop + i * 54 + 54, 16)) {
                    active[i] = !active[i];
                    press(0.5F + (active[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 159, 18, guiTop + i * 54 + 53, 18)) {
                    polling[i] = !polling[i];
                    press(0.5F + (polling[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 195, 18, guiTop + i * 54 + 53, 18)) {
                    shortenNumber[i] = !shortenNumber[i];
                    press(0.5F + (shortenNumber[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 231, 18, guiTop + i * 54 + 53, 18)) {
                    leadingZeroes[i] = !leadingZeroes[i];
                    press(0.5F + (leadingZeroes[i] ? 0.25F : 0F));
                    return true;
                }
            }
            if (inside(x, y, guiLeft + 209, 18, guiTop + 17, 18)) {
                press(1.0F);
                CompoundTag data = new CompoundTag();
                data.putByte("active", mask(active));
                data.putByte("polling", mask(polling));
                data.putByte("shorten_number", mask(shortenNumber));
                data.putByte("leading_zeroes", mask(leadingZeroes));
                for (int i = 0; i < 2; i++) {
                    data.putString("label" + i, label[i].getValue());
                    data.putString("rtty" + i, rtty[i].getValue());
                }
                send(data);
                return true;
            }
            return false;
        }
    }

    public static final class Graph extends RbmkPanelScreen {
        private final EditBox[] label = new EditBox[2];
        private final EditBox[] rtty = new EditBox[2];
        private final EditBox[] min = new EditBox[2];
        private final EditBox[] max = new EditBox[2];
        private final boolean[] active = new boolean[2];
        private final boolean[] polling = new boolean[2];

        public Graph(RbmkPanelBlockEntity panel) {
            super(panel, "gui_rbmk_graph", "container.rbmkGraph", 256, 150);
        }

        @Override
        protected void initFields(int oX, int oY) {
            for (int i = 0; i < 2; i++) {
                RbmkPanelBlockEntity.GraphUnit unit = panel.graphs[i];
                label[i] = field(guiLeft + 27 + oX, guiTop + 73 + oY + i * 54, 72 - oX * 2, 14, 30, unit.label);
                rtty[i] = field(guiLeft + 27 + oX, guiTop + 55 + oY + i * 54, 72 - oX * 2, 14, MAX_CHAN_LENGTH, unit.rtty);
                min[i] = field(guiLeft + 175 + oX, guiTop + 55 + oY + i * 54, 72 - oX * 2, 14, 15, unit.minBound ? unit.min + "" : "");
                max[i] = field(guiLeft + 175 + oX, guiTop + 73 + oY + i * 54, 72 - oX * 2, 14, 15, unit.maxBound ? unit.max + "" : "");
                active[i] = unit.active;
                polling[i] = unit.polling;
            }
        }

        @Override
        protected void drawToggles(GuiGraphics graphics, ResourceLocation texture) {
            for (int i = 0; i < 2; i++) {
                if (active[i]) graphics.blit(texture, guiLeft + 111, guiTop + i * 54 + 54, 18, 150, 16, 16);
                if (polling[i]) graphics.blit(texture, guiLeft + 128, guiTop + i * 54 + 53, 0, 150, 18, 18);
            }
        }

        @Override
        protected boolean clickToggles(double x, double y) {
            for (int i = 0; i < 2; i++) {
                if (inside(x, y, guiLeft + 111, 16, guiTop + i * 54 + 54, 16)) {
                    active[i] = !active[i];
                    press(0.5F + (active[i] ? 0.25F : 0F));
                    return true;
                }
                if (inside(x, y, guiLeft + 128, 18, guiTop + i * 54 + 53, 18)) {
                    polling[i] = !polling[i];
                    press(0.5F + (polling[i] ? 0.25F : 0F));
                    return true;
                }
            }
            if (inside(x, y, guiLeft + 209, 18, guiTop + 17, 18)) {
                press(1.0F);
                CompoundTag data = new CompoundTag();
                data.putByte("active", mask(active));
                data.putByte("polling", mask(polling));
                for (int i = 0; i < 2; i++) {
                    data.putString("label" + i, label[i].getValue());
                    data.putString("rtty" + i, rtty[i].getValue());
                    try { if (!min[i].getValue().isEmpty()) data.putLong("min" + i, Long.parseLong(min[i].getValue())); } catch (Exception ignored) { }
                    try { if (!max[i].getValue().isEmpty()) data.putLong("max" + i, Long.parseLong(max[i].getValue())); } catch (Exception ignored) { }
                }
                send(data);
                return true;
            }
            return false;
        }
    }

    /** GUIScreenRBMKTerminal: no background, help text at half scale, one invisible always-focused line. */
    public static final class Terminal extends Screen {
        public static RbmkPanelBlockEntity lastTerminal;
        private EditBox line;

        public Terminal(RbmkPanelBlockEntity terminal) {
            super(Component.empty());
            lastTerminal = terminal;
        }

        @Override
        protected void init() {
            super.init();
            line = new EditBox(font, 0, 0, 0, 0, Component.empty());
            line.setMaxLength(50);
            line.setFocused(true);
        }

        public static String getWorkingLine(RbmkPanelBlockEntity terminal) {
            if (terminal != lastTerminal) return "";
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof Terminal gui && gui.line != null) {
                return gui.line.getValue();
            }
            return "";
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (lastTerminal.isRemoved()) {
                onClose();
                return;
            }
            graphics.pose().pushPose();
            graphics.pose().scale(0.5F, 0.5F, 1F);
            graphics.drawString(font, "[Esc] - Quit", 2, 2, 0xffffff, false);
            graphics.drawString(font, "chan <channel> - Set selected channel", 2, 12, 0xffffff, false);
            graphics.drawString(font, "send <cmd> - Send single signal over selected channel", 2, 22, 0xffffff, false);
            graphics.drawString(font, "start <cmd> - Continuously send signal over selected channel", 2, 32, 0xffffff, false);
            graphics.drawString(font, "stop - Stop continuous sending", 2, 42, 0xffffff, false);
            graphics.drawString(font, "clear - Delete command history", 2, 52, 0xffffff, false);
            graphics.pose().popPose();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                CompoundTag data = new CompoundTag();
                data.putString("cmd", line.getValue());
                PacketDistributor.sendToServer(new RbmkPanelControlPayload(lastTerminal.getBlockPos(), data));
                line.setValue("");
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_LEFT
                    || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_END) {
                return true;
            }
            line.moveCursorToEnd(false);
            return line.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            line.moveCursorToEnd(false);
            return line.charTyped(codePoint, modifiers);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}

package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import java.util.List;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.RadioTelexBlockEntity;
import com.atomiclegacy.inventory.container.RadioTelexMenu;
import com.atomiclegacy.network.RadioTelexControlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Random;

/** Pixel-layout port of GUIScreenRadioTelex and its five-line terminal editor. */
public final class RadioTelexScreen extends AbstractContainerScreen<RadioTelexMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/machine/gui_telex.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private final String[] transmit = new String[RadioTelexBlockEntity.LINE_COUNT];
    private EditBox transmitChannel;
    private EditBox receiveChannel;
    private boolean textFocus;
    private int cursorLine;

    public RadioTelexScreen(RadioTelexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 244;
        Arrays.fill(transmit, "");
    }

    @Override
    protected void init() {
        super.init();
        RadioTelexBlockEntity telex = telex();
        if (telex != null) {
            String[] serverLines = telex.txBuffer();
            System.arraycopy(serverLines, 0, transmit, 0, transmit.length);
            for (int index = transmit.length - 1; index > 0; index--) {
                if (!transmit[index].isEmpty()) {
                    cursorLine = index;
                    break;
                }
            }
        }

        transmitChannel = channelBox(leftPos + 29, topPos + 110,
                telex == null ? "" : telex.txChannel());
        receiveChannel = channelBox(leftPos + 29, topPos + 224,
                telex == null ? "" : telex.rxChannel());
        addRenderableWidget(transmitChannel);
        addRenderableWidget(receiveChannel);
    }

    private EditBox channelBox(int x, int y, String value) {
        EditBox box = new EditBox(font, x, y, 90, 14, Component.empty());
        box.setTextColor(0x00FF00);
        box.setTextColorUneditable(0x00FF00);
        box.setBordered(false);
        box.setMaxLength(RadioTelexBlockEntity.CHANNEL_WIDTH);
        box.setValue(value);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int line = 0; line < transmit.length; line++) {
            drawTransmitLine(graphics, transmit[line], 11 + 14 * line);
            if (System.currentTimeMillis() % 1000L < 500L && textFocus) {
                int x = Math.max(11 + 7 * (transmit[line].length() - 1) + 7, 11);
                if (cursorLine == line) graphics.drawString(font, "|", x, 11 + 14 * line, 0x00FF00, false);
            }
        }
        RadioTelexBlockEntity telex = telex();
        if (telex != null) {
            String[] receive = telex.rxBuffer();
            for (int line = 0; line < receive.length; line++) {
                drawReceiveLine(graphics, receive[line], 145 + 14 * line);
            }
            drawWaveform(graphics, telex.sendingChar());
        }
    }

    /** FontRenderer#getCharWidth of 1.7.10: the section sign measures -1. */
    private int charWidth(char c) {
        if (c == '\u00a7') return -1;
        return font.width(FormattedCharSequence.forward(String.valueOf(c), Style.EMPTY));
    }

    /** "\u00a7X" prefix as a style; the glyph after it is drawn literally like 1.7.10 does for a trailing section sign. */
    private static Style legacyStyle(String format) {
        ChatFormatting formatting = format.length() > 1 ? ChatFormatting.getByCode(format.charAt(1)) : null;
        return formatting == null ? Style.EMPTY : Style.EMPTY.applyLegacyFormat(formatting);
    }

    /** [parity g2] GUIScreenRadioTelex TX buffer: fixed 7 px cells, section signs stay visible, control characters red. */
    private void drawTransmitLine(GuiGraphics graphics, String text, int y) {
        String format = "\u00a7r";
        for (int index = 0; index < text.length(); index++) {
            int x = 11 + 7 * index;
            char c = text.charAt(index);
            x += (7 - charWidth(c)) / 2;
            if (c == '\u00a7' && text.length() > index + 1) {
                format = "\u00a7" + text.charAt(index + 1);
                x -= 3;
            }
            Style style = legacyStyle(format);
            String glyph = String.valueOf(c);
            if (c == RadioTelexBlockEntity.BELL) { glyph = "B"; style = Style.EMPTY.withColor(ChatFormatting.RED); }
            if (c == RadioTelexBlockEntity.PRINT) { glyph = "P"; style = Style.EMPTY.withColor(ChatFormatting.RED); }
            if (c == RadioTelexBlockEntity.CLEAR) { glyph = "<"; style = Style.EMPTY.withColor(ChatFormatting.RED); }
            if (c == RadioTelexBlockEntity.PAUSE) { glyph = "W"; style = Style.EMPTY.withColor(ChatFormatting.RED); }
            graphics.drawString(font, FormattedCharSequence.forward(glyph, style), x, y, 0x00FF00, false);
        }
    }

    /** [parity g2] GUIScreenRadioTelex RX buffer: the centring offset accumulates, format codes take no cell. */
    private void drawReceiveLine(GuiGraphics graphics, String text, int y) {
        String format = "\u00a7r";
        int x = 11;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            x += (7 - charWidth(c)) / 2;
            if (c == '\u00a7' && text.length() > index + 1) {
                format = "\u00a7" + text.charAt(index + 1);
                c = ' ';
            } else if (c == '\u00a7') {
                c = ' ';
            } else if (index > 0 && text.charAt(index - 1) == '\u00a7') {
                c = ' ';
                x -= 14;
            }
            graphics.drawString(font, FormattedCharSequence.forward(String.valueOf(c), legacyStyle(format)), x, y, 0x00FF00, false);
            x += 7;
        }
    }

    /** [parity g2] GL_LINES strip, glLineWidth(3) (screen pixels), Gaussian offsets clamped to +-7 around y 93.5. */
    private void drawWaveform(GuiGraphics graphics, char sendingChar) {
        Random rand = new Random(sendingChar);
        float half = (float) (1.5D / Minecraft.getInstance().getWindow().getGuiScale());
        graphics.flush();
        Matrix4f pose = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double offset = 0;
        for (int i = 0; i < 48; i++) {
            float x0 = 199 + i;
            float y0 = (float) (93.5D + offset);
            if (sendingChar != ' ' && i > 4 && i < 43) offset = rand.nextGaussian() * 7; else offset = 0;
            offset = Mth.clamp(offset, -7D, 7D);
            float x1 = 199 + i + 1;
            float y1 = (float) (93.5D + offset);
            float dx = x1 - x0;
            float dy = y1 - y0;
            float length = Mth.sqrt(dx * dx + dy * dy);
            float nx = -dy / length * half;
            float ny = dx / length * half;
            buffer.addVertex(pose, x0 + nx, y0 + ny, 10.0F).setColor(0, 255, 0, 255);
            buffer.addVertex(pose, x1 + nx, y1 + ny, 10.0F).setColor(0, 255, 0, 255);
            buffer.addVertex(pose, x1 - nx, y1 - ny, 10.0F).setColor(0, 255, 0, 255);
            buffer.addVertex(pose, x0 - nx, y0 - ny, 10.0F).setColor(0, 255, 0, 255);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // [parity g2] literal colours of GUIScreenRadioTelex#drawGuiContainerForegroundLayer
        tooltip(graphics, mouseX, mouseY, 7, 85, ChatFormatting.GOLD, "gui.atomiclegacy.telex.bell", "gui.atomiclegacy.telex.bell.detail");
        tooltip(graphics, mouseX, mouseY, 27, 85, ChatFormatting.GOLD, "gui.atomiclegacy.telex.print_control", "gui.atomiclegacy.telex.print_control.detail");
        tooltip(graphics, mouseX, mouseY, 47, 85, ChatFormatting.GOLD, "gui.atomiclegacy.telex.clear_control", "gui.atomiclegacy.telex.clear_control.detail");
        tooltip(graphics, mouseX, mouseY, 67, 85, ChatFormatting.GOLD, "gui.atomiclegacy.telex.format", "gui.atomiclegacy.telex.format.detail");
        tooltip(graphics, mouseX, mouseY, 87, 85, ChatFormatting.GOLD, "gui.atomiclegacy.telex.pause", "gui.atomiclegacy.telex.pause.detail");
        tooltip(graphics, mouseX, mouseY, 127, 105, ChatFormatting.GREEN, "gui.atomiclegacy.telex.save_id");
        tooltip(graphics, mouseX, mouseY, 147, 105, ChatFormatting.YELLOW, "gui.atomiclegacy.telex.send");
        tooltip(graphics, mouseX, mouseY, 167, 105, ChatFormatting.RED, "gui.atomiclegacy.telex.delete_transmit");
        tooltip(graphics, mouseX, mouseY, 127, 219, ChatFormatting.GREEN, "gui.atomiclegacy.telex.save_id");
        tooltip(graphics, mouseX, mouseY, 147, 219, ChatFormatting.AQUA, "gui.atomiclegacy.telex.print_receive");
        tooltip(graphics, mouseX, mouseY, 167, 219, ChatFormatting.RED, "gui.atomiclegacy.telex.clear_receive");
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void tooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, ChatFormatting color, String... keys) {
        if (!button(mouseX, mouseY, x, y)) return;
        List<Component> lines = new java.util.ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            lines.add(i == 0 ? Component.translatable(keys[i]).withStyle(color) : Component.translatable(keys[i]));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button); // [parity g2] any mouse button

        if (inside(mouseX, mouseY, 7, 7, 242, 74)) {
            textFocus = true;
            transmitChannel.setFocused(false);
            receiveChannel.setFocused(false);
            return true;
        }
        textFocus = false;

        if (button(mouseX, mouseY, 7, 85)) return insert(RadioTelexBlockEntity.BELL);
        if (button(mouseX, mouseY, 27, 85)) return insert(RadioTelexBlockEntity.PRINT);
        if (button(mouseX, mouseY, 47, 85)) return insert(RadioTelexBlockEntity.CLEAR);
        if (button(mouseX, mouseY, 67, 85)) return insert('\u00a7');
        if (button(mouseX, mouseY, 87, 85)) return insert(RadioTelexBlockEntity.PAUSE);
        if (button(mouseX, mouseY, 127, 105) || button(mouseX, mouseY, 127, 219)) {
            return send(RadioTelexControlPayload.SAVE_CHANNELS,
                    transmitChannel.getValue(), receiveChannel.getValue());
        }
        if (button(mouseX, mouseY, 147, 105)) {
            return send(RadioTelexControlPayload.SEND, serializedTransmit(), "");
        }
        if (button(mouseX, mouseY, 167, 105)) {
            Arrays.fill(transmit, ""); // [parity g2] cursorPos is kept
            return send(RadioTelexControlPayload.DELETE_TRANSMIT, "", "");
        }
        if (button(mouseX, mouseY, 147, 219)) {
            return send(RadioTelexControlPayload.PRINT_RECEIVE, "", "");
        }
        if (button(mouseX, mouseY, 167, 219)) {
            return send(RadioTelexControlPayload.CLEAR_RECEIVE, "", "");
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [parity g2] keyTyped: a focused channel field consumes every key but Esc; the TX editor every key
        if (transmitChannel.isFocused() || receiveChannel.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers);
            (transmitChannel.isFocused() ? transmitChannel : receiveChannel).keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (textFocus) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                textFocus = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                cursorLine = Math.max(0, cursorLine - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                cursorLine = Math.min(transmit.length - 1, cursorLine + 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !transmit[cursorLine].isEmpty()) {
                transmit[cursorLine] = transmit[cursorLine].substring(0, transmit[cursorLine].length() - 1);
                return true;
            }
            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) return true; // typed as a character
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (transmitChannel.isFocused() || receiveChannel.isFocused()) {
            return super.charTyped(character, modifiers);
        }
        if (textFocus && StringUtil.isAllowedChatCharacter(character)) {
            submit(character);
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private boolean insert(char character) {
        textFocus = true;
        transmitChannel.setFocused(false);
        receiveChannel.setFocused(false);
        submit(character);
        click();
        return true;
    }

    private void submit(char character) {
        if (transmit[cursorLine].length() < RadioTelexBlockEntity.LINE_WIDTH) {
            transmit[cursorLine] += character;
        }
    }

    private boolean send(int action, String transmitData, String receiveData) {
        PacketDistributor.sendToServer(new RadioTelexControlPayload(menu.blockPos(), action,
                transmitData, receiveData));
        click();
        return true;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean button(double mouseX, double mouseY, int x, int y) {
        return inside(mouseX, mouseY, x, y, 18, 18);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private String serializedTransmit() {
        return String.join("\n", transmit);
    }

    private RadioTelexBlockEntity telex() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioTelexBlockEntity value
                ? value : null;
    }

    @Override
    public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new RadioTelexControlPayload(menu.blockPos(),
                    RadioTelexControlPayload.UPDATE_TRANSMIT, serializedTransmit(), ""));
        }
        super.removed();
    }
}

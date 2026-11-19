package com.atomiclegacy.inventory.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * [parity M2] Integer mode of com.hbm.module.NumberDisplay: seven-segment digits (vertical 5, horizontal 4,
 * thickness 1, padding 3), right-aligned in digitLength cells, optional half-second blink.
 */
public final class M2NumberDisplay {

    private final int displayX;
    private final int displayY;
    private final int color;
    private final int padding = 3;
    private final int verticalLength = 5;
    private final int horizontalLength = 4;
    private final int thickness = 1;
    private int digitLength = 3;
    private boolean blink;
    private int dispOffset;

    public M2NumberDisplay(int x, int y, int color) {
        this.displayX = x;
        this.displayY = y;
        this.color = 0xFF000000 | color;
    }

    public M2NumberDisplay setDigitLength(int length) {
        this.digitLength = length;
        return this;
    }

    public M2NumberDisplay setBlinks(boolean blinks) {
        this.blink = blinks;
        return this;
    }

    public void drawNumber(GuiGraphics graphics, int guiLeft, int guiTop, Number number) {
        char[] num = Long.toString(Math.round(number.doubleValue())).toCharArray();
        if (blink && !(System.currentTimeMillis() % 1000 < 500)) return;
        int gap = digitLength - num.length;
        for (int i = 0; i < num.length; i++) {
            if (num[i] == '.') gap--;
            dispOffset = (padding + horizontalLength + 2 * thickness) * (i + gap);
            drawChar(graphics, guiLeft, guiTop, num[i]);
        }
    }

    private void drawChar(GuiGraphics g, int l, int t, char c) {
        switch (c) {
            case '1' -> { v(g, l, t, 1, 0); v(g, l, t, 1, 1); }
            case '2' -> { h(g, l, t, 0); v(g, l, t, 1, 0); h(g, l, t, 1); v(g, l, t, 0, 1); h(g, l, t, 2); }
            case '3' -> { h(g, l, t, 0); h(g, l, t, 1); h(g, l, t, 2); v(g, l, t, 1, 0); v(g, l, t, 1, 1); }
            case '4' -> { v(g, l, t, 0, 0); v(g, l, t, 1, 0); v(g, l, t, 1, 1); h(g, l, t, 1); }
            case '5' -> { h(g, l, t, 0); h(g, l, t, 1); h(g, l, t, 2); v(g, l, t, 0, 0); v(g, l, t, 1, 1); }
            case '6' -> { h(g, l, t, 0); h(g, l, t, 1); h(g, l, t, 2); v(g, l, t, 0, 0); v(g, l, t, 0, 1); v(g, l, t, 1, 1); }
            case '7' -> { h(g, l, t, 0); v(g, l, t, 1, 0); v(g, l, t, 1, 1); }
            case '8' -> { h(g, l, t, 0); h(g, l, t, 1); h(g, l, t, 2); v(g, l, t, 0, 0); v(g, l, t, 1, 0); v(g, l, t, 0, 1); v(g, l, t, 1, 1); }
            case '9' -> { h(g, l, t, 0); h(g, l, t, 1); h(g, l, t, 2); v(g, l, t, 0, 0); v(g, l, t, 1, 0); v(g, l, t, 1, 1); }
            case '0' -> { h(g, l, t, 0); h(g, l, t, 2); v(g, l, t, 0, 0); v(g, l, t, 0, 1); v(g, l, t, 1, 0); v(g, l, t, 1, 1); }
            case '-' -> h(g, l, t, 1);
            default -> { h(g, l, t, 0); h(g, l, t, 1); h(g, l, t, 2); v(g, l, t, 0, 0); v(g, l, t, 0, 1); }
        }
    }

    private void h(GuiGraphics g, int l, int t, int pos) {
        int offset = pos * (verticalLength + thickness);
        segment(g, l + displayX + dispOffset + thickness, t + displayY + offset, horizontalLength, thickness);
    }

    private void v(GuiGraphics g, int l, int t, int posX, int posY) {
        int offsetX = posX * (horizontalLength + thickness);
        int offsetY = posY * (verticalLength + thickness);
        segment(g, l + displayX + offsetX + dispOffset, t + displayY + offsetY + thickness, thickness, verticalLength);
    }

    private void segment(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, color);
    }
}

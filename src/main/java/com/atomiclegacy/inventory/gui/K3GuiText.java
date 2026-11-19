package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * [parity K3] Text helpers mirroring GuiInfoContainer of the 1.7.10 reference: drawElectricityInfo
 * (BobMathUtil.getShortNumber(power) + "/" + getShortNumber(max) + "HE"), getUpgradeInfo (upgrade.gui.* keys)
 * and drawInfoPanel type 8 (small blue star at gui_utility 0,32).
 */
public final class K3GuiText {
    public static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

    private K3GuiText() {
    }

    /** BobMathUtil.getShortNumber */
    public static String shortNumber(long l) {
        double res;
        String suffix;
        long abs = Math.abs(l);
        if (abs >= Math.pow(10, 18)) {
            res = l / Math.pow(10, 18);
            suffix = "E";
        } else if (abs >= Math.pow(10, 15)) {
            res = l / Math.pow(10, 15);
            suffix = "P";
        } else if (abs >= Math.pow(10, 12)) {
            res = l / Math.pow(10, 12);
            suffix = "T";
        } else if (abs >= Math.pow(10, 9)) {
            res = l / Math.pow(10, 9);
            suffix = "G";
        } else if (abs >= Math.pow(10, 6)) {
            res = l / Math.pow(10, 6);
            suffix = "M";
        } else if (abs >= Math.pow(10, 3)) {
            res = l / Math.pow(10, 3);
            suffix = "k";
        } else {
            return Long.toString(l);
        }
        if (res <= -100.0) {
            res = Math.round(res * 10.0) / 10.0;
        } else {
            res = Math.round(res * 100.0) / 100.0;
        }
        return res + suffix;
    }

    public static Component electricity(long power, long maxPower) {
        return Component.literal(shortNumber(power) + "/" + shortNumber(maxPower) + "HE");
    }

    /** Upgrade types in UpgradeType.values() order with their maximum level; a 0 entry is skipped. */
    public static List<Component> upgradeInfo(int speed, int power, int effect, int afterburn, int overdrive) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("upgrade.gui.title"));
        if (speed > 0) lines.add(Component.translatable("upgrade.gui.speed", speed));
        if (power > 0) lines.add(Component.translatable("upgrade.gui.power", power));
        if (effect > 0) lines.add(Component.translatable("upgrade.gui.effectiveness", effect));
        if (afterburn > 0) lines.add(Component.translatable("upgrade.gui.afterburner", afterburn));
        if (overdrive > 0) lines.add(Component.translatable("upgrade.gui.overdrive", overdrive));
        return lines;
    }

    /** drawInfoPanel(x, y, 8, 8, 8): small blue star */
    public static void drawSmallBlueInfo(GuiGraphics graphics, int x, int y) {
        graphics.blit(UTILITY, x, y, 0, 32, 8, 8, 256, 256);
    }

    /** "gui.button.press" at volume 1 */
    public static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** GuiInfoContainer hover test: x <= mouse < x + w, y < mouse <= y + h (absolute coordinates) */
    public static boolean hover(double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY;
    }
}

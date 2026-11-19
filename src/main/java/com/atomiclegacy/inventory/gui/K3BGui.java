package com.atomiclegacy.inventory.gui;

import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineElectricEngine;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * [parity K3: group B] Helpers mirroring com.hbm.inventory.gui.GuiInfoContainer (1.7.10) for the shredder, centrifuge and
 * crystallizer screens: drawElectricityInfo, drawCustomInfoStat hover rules and drawInfoPanel (gui_utility.png).
 */
final class K3BGui {

	static final ResourceLocation UTILITY = AtomicLegacy.id("textures/gui/gui_utility.png");

	private K3BGui() {
	}

	/** GuiInfoContainer hover test: x <= mouseX < x + width, y < mouseY <= y + height (absolute screen coordinates). */
	static boolean hover(int mouseX, int mouseY, int x, int y, int width, int height) {
		return x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY;
	}

	/** drawElectricityInfo: BobMathUtil.getShortNumber(power) + "/" + getShortNumber(maxPower) + "HE". */
	static void electricityInfo(GuiGraphics graphics, Font font, int mouseX, int mouseY, int x, int y, int width, int height, long power, long maxPower) {
		if(hover(mouseX, mouseY, x, y, width, height)) {
			graphics.renderComponentTooltip(font, List.of(Component.literal(MagazineElectricEngine.getShortNumber(power) + "/" + MagazineElectricEngine.getShortNumber(maxPower) + "HE")), mouseX, mouseY);
		}
	}

	/** drawCustomInfoStat: tooltip lines drawn at (tPosX, tPosY) while the mouse is over the area. */
	static void customInfo(GuiGraphics graphics, Font font, int mouseX, int mouseY, int x, int y, int width, int height, int tPosX, int tPosY, List<Component> text) {
		if(hover(mouseX, mouseY, x, y, width, height)) {
			graphics.renderComponentTooltip(font, text, tPosX, tPosY);
		}
	}

	/** drawInfoPanel(x, y, w, h, type) for the panel types used by these screens. */
	static void infoPanel(GuiGraphics graphics, int x, int y, int type) {
		switch(type) {
		case 2 -> graphics.blit(UTILITY, x, y, 8, 0, 16, 16);
		case 3 -> graphics.blit(UTILITY, x, y, 24, 0, 16, 16);
		case 6 -> graphics.blit(UTILITY, x, y, 8, 16, 16, 16);
		case 7 -> graphics.blit(UTILITY, x, y, 24, 16, 16, 16);
		default -> {
		}
		}
	}
}

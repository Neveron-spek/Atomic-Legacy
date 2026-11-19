package com.atomiclegacy.client.gun;

import com.atomiclegacy.inventory.gui.WeaponTableScreen;

import net.minecraft.client.Minecraft;

/** Minecraft.getMinecraft().currentScreen instanceof GUIWeaponTable */
final class WeaponTableScreenAccess {
	private WeaponTableScreenAccess() {
	}

	static boolean isOpen() {
		return Minecraft.getInstance().screen instanceof WeaponTableScreen;
	}
}

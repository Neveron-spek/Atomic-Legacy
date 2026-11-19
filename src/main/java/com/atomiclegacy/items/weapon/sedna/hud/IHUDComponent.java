package com.atomiclegacy.items.weapon.sedna.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 IHUDComponent. Client only; components are attached to the gun configs
 * by the client setup (GunFactoryClient). Rendered during the HOTBAR overlay pass.
 */
public interface IHUDComponent {

	public int getComponentHeight(Player player, ItemStack stack);

	public void renderHUDComponent(GuiGraphics graphics, Player player, ItemStack stack, int bottomOffset, int gunIndex);
}

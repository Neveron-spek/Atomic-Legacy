package com.atomiclegacy.items.weapon.sedna.hud;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 HUDComponentAmmoCounter. */
public class HUDComponentAmmoCounter implements IHUDComponent {

	protected int receiver;
	protected boolean mirrored;
	protected boolean noCounter;

	public HUDComponentAmmoCounter(int receiver) {
		this.receiver = receiver;
	}

	public HUDComponentAmmoCounter mirror() {
		this.mirrored = true;
		return this;
	}

	public HUDComponentAmmoCounter noCounter() {
		this.noCounter = true;
		return this;
	}

	@Override
	public int getComponentHeight(Player player, ItemStack stack){
		return 17;
	}

	@Override
	public void renderHUDComponent(GuiGraphics graphics, Player player, ItemStack stack, int bottomOffset, int gunIndex) {

		Minecraft mc = Minecraft.getInstance();

		int pX = graphics.guiWidth() / 2 + (mirrored ? -(62 + 36 + 52) : (62 + 36)) + (noCounter ? 14 : 0);
		int pZ = graphics.guiHeight() - bottomOffset - 18;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		IMagazine<?> mag = gun.getConfig(stack, gunIndex).getReceivers(stack)[this.receiver].getMagazine(stack);

		if(!noCounter) graphics.drawString(mc.font, mag.reportAmmoStateForHUD(stack, player), pX + 17, pZ + 6, 0xFFFFFF, false);

		ItemStack icon = mag.getIconForHUD(stack, player);
		if(!icon.isEmpty()) graphics.renderItem(icon, pX, pZ);
	}
}

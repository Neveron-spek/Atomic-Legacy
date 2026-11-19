package com.atomiclegacy.items.weapon.sedna.hud;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 HUDComponentDurabilityBar. */
public class HUDComponentDurabilityBar implements IHUDComponent {

	public static final ResourceLocation misc = AtomicLegacy.id("textures/misc/overlay_misc.png");

	protected boolean mirrored = false;

	public HUDComponentDurabilityBar() {
		this(false);
	}
	public HUDComponentDurabilityBar(boolean mirror) {
		this.mirrored = mirror;
	}

	@Override
	public int getComponentHeight(Player player, ItemStack stack) {
		return 5;
	}

	@Override
	public void renderHUDComponent(GuiGraphics graphics, Player player, ItemStack stack, int bottomOffset, int gunIndex) {

		int pX = graphics.guiWidth() / 2 + (mirrored ? -(62 + 36 + 52) : (62 + 36));
		int pZ = graphics.guiHeight() - 21;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		int dura = (int) (50 * ItemGunBaseNT.getWear(stack, gunIndex) / gun.getConfig(stack, gunIndex).getDurability(stack));

		graphics.setColor(1F, 1F, 1F, 1F);
		graphics.blit(misc, pX, pZ + 16, 94, 0, 52, 3);
		graphics.blit(misc, pX + 1, pZ + 16, 95, 3, 50 - dura, 3);
	}
}

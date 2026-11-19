package com.atomiclegacy.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * [weapons batch F2] 1.7.10 com.hbm.items.ICustomizable. The original /customize command (CommandCustomize) that
 * invoked it is not part of this port; the interface is kept so the item logic stays identical.
 */
public interface ICustomizable {
	void customize(Player player, ItemStack stack, String... args);
}

package com.atomiclegacy.items;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * [weapons batch F3] 1.7.10 ammo_fireext = ItemAmmo(AmmoFireExt.class): WATER ("ammo_fireext"), FOAM ("ammo_fireext_foam"),
 * SAND ("ammo_fireext_sand"). Names: item.atomiclegacy.ammo_fireext.water / .foam / .sand. ItemAmmo showed no base name
 * tooltip (the AmmoFireExt entries carry no traits), so the LegacyVariantItem tooltip line is omitted.
 */
public class FireExtAmmoItem extends LegacyVariantItem {

	public FireExtAmmoItem(Properties properties) {
		super(properties, "ammo_fireext", variants("water", "foam", "sand"));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
	}
}

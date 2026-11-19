package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory556mm;

import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 WeaponModScope. The heavy revolver (scope_lilmac) and UTILITY-quality
 * (XFactoryTool.scope) branches apply to guns that are not ported yet; every
 * ported gun using this mod takes the XFactory556mm scope like the original.
 */
public class WeaponModScope extends WeaponModBase {

	public WeaponModScope(int id) {
		super(id, "SCOPE");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.O_SCOPETEXTURE) {
			if(gun.getItem() == com.atomiclegacy.registry.HbmItems.GUN_HEAVY_REVOLVER.get()) return cast(com.atomiclegacy.items.weapon.sedna.factory.XFactory44.scope_lilmac, base); // [C2]
			return cast(XFactory556mm.scope, base);
		}
		if(key == GunConfig.B_HIDECROSSHAIR) return cast(true, base); // just in case
		return base;
	}
}

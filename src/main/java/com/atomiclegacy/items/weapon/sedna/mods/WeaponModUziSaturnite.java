package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModUziSaturnite (slot FURNITURE, PRIORITY_ADDITIVE; gun_uzi, gun_uzi_akimbo). */
public class WeaponModUziSaturnite extends WeaponModBase {

	public WeaponModUziSaturnite(int id) {
		super(id, "FURNITURE");
		this.setPriority(PRIORITY_ADDITIVE);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.F_DURABILITY) return cast((Float) base * 5F, base);
		if(key == Receiver.F_BASEDAMAGE) return cast((Float) base + 3F, base);
		return base;
	}
}

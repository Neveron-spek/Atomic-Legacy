package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModSawedOff (slot BARREL; gun_maresleg, gun_double_barrel). */
public class WeaponModSawedOff extends WeaponModBase {

	public WeaponModSawedOff(int id) {
		super(id, "BARREL");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.F_SPREADINNATE) { return cast(Math.max(0.025F, (Float) base), base); }
		if(key == Receiver.F_SPREADAMMO) { return cast((Float) base * 1.5F, base); }
		if(key == Receiver.F_BASEDAMAGE) { return cast((Float) base * 1.35F, base); }

		if(gun.getItem() == HbmItems.GUN_MARESLEG.get()) {
			if(key == GunConfig.FUN_ANIMNATIONS) return (T) XFactory12ga.LAMBDA_MARESLEG_SHORT_ANIMS;
			if(key == GunConfig.I_DRAWDURATION) return cast(5, base);
		}

		return base;
	}
}

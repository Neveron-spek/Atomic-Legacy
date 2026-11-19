package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModPolymerFurniture (slot FURNITURE; gun_g3): halved recoil. */
public class WeaponModPolymerFurniture extends WeaponModBase {

	public WeaponModPolymerFurniture(int id) {
		super(id, "FURNITURE");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.CON_ONRECOIL) return (T) LAMBDA_RECOIL_G3;
		return base;
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125));
	};
}

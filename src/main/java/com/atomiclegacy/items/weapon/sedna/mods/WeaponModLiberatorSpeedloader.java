package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiFunction;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModLiberatorSpeedloader (slot SPEEDLOADER; gun_liberator): full reload magazine and animations. */
public class WeaponModLiberatorSpeedloader extends WeaponModBase {

	public static MagazineFullReload MAG = new MagazineFullReload(0, 4);

	public WeaponModLiberatorSpeedloader(int id) {
		super(id, "SPEEDLOADER");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.FUN_ANIMNATIONS) { return (T) LAMBDA_LIBERATOR_ANIMS; }
		if(parent instanceof Receiver && base instanceof IMagazine && key == Receiver.O_MAGAZINE) {
			MagazineSingleReload originalMag = (MagazineSingleReload) base;
			if(MAG.acceptedBullets.isEmpty()) MAG.acceptedBullets.addAll(originalMag.acceptedBullets);
			return (T) MAG;
		}
		return base;
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LIBERATOR_ANIMS = (stack, type) -> {
		switch(type) {
		case RELOAD: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
				.addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
				.addBus("SHELL1", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
				.addBus("SHELL2", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
				.addBus("SHELL3", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
				.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
		case RELOAD_END: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50))
				.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP));
		case JAMMED: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50).addPos(0, 0, 0, 550).addPos(15, 0, 0, 100).addPos(15, 0, 0, 600).addPos(0, 0, 0, 50))
				.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP).addPos(0, 0, 0, 600).addPos(45, 0, 0, 250, IType.SIN_DOWN).addPos(45, 0, 0, 300).addPos(0, 0, 0, 150, IType.SIN_UP));
		}
		return XFactory12ga.LAMBDA_LIBERATOR_ANIMS.apply(stack, type);
	};
}

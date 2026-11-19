package com.atomiclegacy.items.weapon.sedna.mods;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineElectricEngine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.atomiclegacy.registry.HbmFluids;

import net.minecraft.world.item.ItemStack;

/** [weapon mods G3] 1.7.10 WeaponModEngine (gun_drill engines, slot ENGINE): replaces the magazine and the fire delay. */
public class WeaponModEngine extends WeaponModBase {

	public static final MagazineLiquidEngine ENGINE_DIESEL = new MagazineLiquidEngine(0, 4_000, fluid("diesel"), fluid("diesel_crack"), fluid("lightoil"));
	public static final MagazineLiquidEngine ENGINE_AVIATION = new MagazineLiquidEngine(0, 4_000, fluid("kerosene"), fluid("lpg"));
	public static final MagazineElectricEngine ENGINE_ELECTRIC = new MagazineElectricEngine(0, 1_000_000);
	public static final MagazineLiquidEngine ENGINE_TURBO = new MagazineLiquidEngine(0, 4_000, fluid("kerosene_reform"), fluid("reformate"));

	protected IMagazine<?> mag;
	protected int delay;

	public WeaponModEngine(int id) {
		super(id, "ENGINE");
		this.setPriority(PRIORITY_SET);
	}

	public WeaponModEngine mag(IMagazine<?> mag) { this.mag = mag; return this; }
	public WeaponModEngine delay(int delay) { this.delay = delay; return this; }

	private static HbmFluidDefinition fluid(String name) {
		return HbmFluids.byName(name).orElseThrow(() -> new IllegalStateException("WeaponModEngine: missing fluid " + name));
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(key == Receiver.O_MAGAZINE && mag != null) return cast(mag, base);
		if(key == Receiver.I_DELAYAFTERFIRE) return cast((Integer) delay, base);

		return base;
	}

	@Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
	@Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}

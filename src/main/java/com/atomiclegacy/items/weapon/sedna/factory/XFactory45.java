package com.atomiclegacy.items.weapon.sedna.factory;

import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;

/**
 * 1.7.10 XFactory45: the .45 rounds of the P45 caliber conversion kit (the factory has no gun).
 * The original casing9 (STRAIGHT, COLOR_CASE_BRASS, scale 1/1/0.75; COLOR_CASE_44 for AP and DU)
 * is identical to the 9mm casing, so the P9 casing effects are reused. Projectile values come
 * from the StandardAmmoType P45 entries.
 */
public class XFactory45 {

	public static final BulletConfig p45_sp = new BulletConfig(StandardAmmoType.P45_SP).setCasingEffect(GunEffects.CASING_P9);
	public static final BulletConfig p45_fmj = new BulletConfig(StandardAmmoType.P45_FMJ).setCasingEffect(GunEffects.CASING_P9);
	public static final BulletConfig p45_jhp = new BulletConfig(StandardAmmoType.P45_JHP).setCasingEffect(GunEffects.CASING_P9);
	public static final BulletConfig p45_ap = new BulletConfig(StandardAmmoType.P45_AP).setCasingEffect(GunEffects.CASING_P9_STEEL);
	public static final BulletConfig p45_du = new BulletConfig(StandardAmmoType.P45_DU).setCasingEffect(GunEffects.CASING_P9_STEEL);

	/** Loads the class at a fixed point of the item registration, so the BulletConfig indices stay stable. */
	public static void init() { }
}

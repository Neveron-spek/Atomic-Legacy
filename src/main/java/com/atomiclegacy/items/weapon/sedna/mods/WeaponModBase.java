package com.atomiclegacy.items.weapon.sedna.mods;

/** 1.7.10 WeaponModBase. */
public abstract class WeaponModBase implements IWeaponMod {

	public static final int PRIORITY_SET = 1_000_000; // used to be MAX_VALUE, but that would break when doing Comparator math
	public static final int PRIORITY_MULTIPLICATIVE = 1_000;
	public static final int PRIORITY_ADDITIVE = 500;
	public static final int PRIORITY_MULT_FINAL = -1;

	public String[] slots;
	public int priority = 0;

	public WeaponModBase(int id, String... slots) { this.slots = slots; XWeaponModManager.idToMod.put(id, this); }
	public WeaponModBase setPriority(int priority) { this.priority = priority; return this; }

	@Override public int getModPriority() { return priority; }

	@Override public String[] getSlots() { return slots; }

	/** Implicit cast helper of the original (casts via Object to the generic type). */
	@SuppressWarnings("unchecked")
	public <T> T cast(Object arg, T castTo) { return (T) arg; }
}

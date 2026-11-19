package com.atomiclegacy.items.weapon.sedna;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.world.item.ItemStack;

/**
 * Player-gun view of a 1.7.10 sedna BulletConfig. The projectile properties
 * (damage multiplier, armor piercing, penetration, headshot, velocity, lifetime,
 * ricochet) are already carried by the port's {@link StandardAmmoType}, which
 * {@code LegacyBulletEntity} uses; this class adds the gun-side fields the
 * original BulletConfig held for these rounds (wear, reload count, casing).
 *
 * <p>{@link #configs} keeps the original global registration list so magazine
 * NBT can store an index exactly like {@code MagazineSingleTypeBase}.
 */
public class BulletConfig {

	public static final List<BulletConfig> configs = new ArrayList<>();

	public final StandardAmmoType ammo;
	public float wear = 1F;
	public int ammoReloadCount = 1;
	/** Ordinal of {@code TurretVisualClientEffects.CasingConfig}, -1 for none. */
	public int casingEffect = -1;

	/** [parity b1: casing bag] 1.7.10 casingItem / casingAmount: what the casing bag collects, one item per casingAmount * 2 shots. */
	public LegacyCasingTable.Casing casing;

	public BulletConfig(StandardAmmoType ammo) {
		this.ammo = ammo;
		this.casing = LegacyCasingTable.BY_AMMO.get(ammo.id());
		configs.add(this);
	}

	/** The casingItem stack, empty when the round leaves no collectable casing. */
	public ItemStack casingStack() {
		if(this.casing == null) return ItemStack.EMPTY;
		return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.atomiclegacy.AtomicLegacy.id(this.casing.item())), this.casing.count());
	}

	public BulletConfig setWear(float wear) { this.wear = wear; return this; }
	public BulletConfig setReloadCount(int ammoReloadCount) { this.ammoReloadCount = ammoReloadCount; return this; }
	public BulletConfig setCasingEffect(int casingEffect) { this.casingEffect = casingEffect; return this; }

	public float damageMult() { return this.ammo.damageMultiplier(); }
	public float spread() { return this.ammo.projectileSpread(); }
	public int projectilesMin() { return this.ammo.projectileCount(); }
	public int projectilesMax() { return this.ammo.projectileCountMax(); }

	/** ComparableStack#matchesRecipe(stack, true) for the round's ammo_standard variant. */
	public boolean matches(ItemStack stack) {
		return !stack.isEmpty() && stack.is(HbmItems.AMMO_STANDARD.get()) && StandardAmmoItem.standardType(stack) == this.ammo;
	}

	public ItemStack toStack() {
		return StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), this.ammo);
	}
}

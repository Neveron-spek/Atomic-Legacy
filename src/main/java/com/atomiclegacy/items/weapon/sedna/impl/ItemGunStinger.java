package com.atomiclegacy.items.weapon.sedna.impl;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryRocket;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * [weapons batch F4] 1.7.10 com.hbm.items.weapon.sedna.impl.ItemGunStinger: the lock-on state machine of gun_stinger.
 * While the secondary key is held (lockingon), the gun is aimed and loaded, the closest entity within 10 degrees and 150
 * blocks is tracked; after 60 ticks of progress the gun is locked on (TECH_BLEEP). The client HUD (crosshair hidden until
 * fully aimed, lock-on bar) is GunClientHandler.onOverlay + GunBatchF4ClientEffects.renderStingerLockon; the lock-on
 * sound loop is OrchestrasF4.ORCHESTRA_STINGER.
 */
public class ItemGunStinger extends ItemGunBaseNT {

	public static final String KEY_LOCKINGON = "lockingon";
	public static final String KEY_LOCKONPROGRESS = "lockonprogress";

	/** Client lock-on bar interpolation (RenderScreenOverlay.renderStingerLockon). */
	public static float prevLockon;
	public static float lockon;

	public ItemGunStinger(Properties properties, WeaponQuality quality, GunConfig... cfg) {
		super(properties, quality, cfg);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {
		super.inventoryTick(stack, world, entity, slot, isHeld);

		if(entity instanceof Player player) {
			if(!world.isClientSide && !isHeld && getIsLockingOn(stack)) {
				setIsLockingOn(stack, false);
			}

			prevLockon = lockon;

			if(!world.isClientSide) {
				int prevTarget = getLockonTarget(stack);
				if(isHeld && getIsLockingOn(stack) && getIsAiming(stack) && this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player.getInventory()) > 0) {
					int newLockonTarget = XFactoryRocket.getLockonTarget(player, 150D, 10D);

					if(newLockonTarget == -1) {
						if(!getIsLockedOn(stack)) resetLockon(world, stack);
					} else {
						if(!getIsLockedOn(stack) && newLockonTarget != prevTarget) {
							resetLockon(world, stack);
							setLockonTarget(stack, newLockonTarget);
						}
						progressLockon(world, stack);

						if(getLockonProgress(stack) >= 60 && !getIsLockedOn(stack)) {
							world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(), player.getSoundSource(), 1F, 1F);
							setIsLockedOn(stack, true);
						}
					}
				} else {
					resetLockon(world, stack);
				}
			} else {
				if(getLockonProgress(stack) > 1) {
					lockon += (1F / 60F);
				} else {
					lockon = 0;
				}
			}
		}
	}

	public void resetLockon(Level world, ItemStack stack) {
		setLockonProgress(stack, 0);
		setIsLockedOn(stack, false);
	}

	public void progressLockon(Level world, ItemStack stack) {
		setLockonProgress(stack, getLockonProgress(stack) + 1);
	}

	public static boolean getIsLockingOn(ItemStack stack) { return getValueBool(stack, KEY_LOCKINGON); }
	public static void setIsLockingOn(ItemStack stack, boolean value) { setValueBool(stack, KEY_LOCKINGON, value); }
	public static int getLockonProgress(ItemStack stack) { return getValueInt(stack, KEY_LOCKONPROGRESS); }
	public static void setLockonProgress(ItemStack stack, int value) { setValueInt(stack, KEY_LOCKONPROGRESS, value); }
}

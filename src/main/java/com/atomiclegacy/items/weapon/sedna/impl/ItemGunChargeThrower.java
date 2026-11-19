package com.atomiclegacy.items.weapon.sedna.impl;

import java.lang.reflect.Field;
import java.util.function.BiPredicate;

import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F3] 1.7.10 com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower: the grappling hook rope.
 *
 * <p>Key states: 1.7.10 read HbmPlayerProps.getKeyPressed on both sides. The server side of the port keeps the gun
 * key states in the stack (ItemGunBaseNT.getPrimary/getSecondary, set by GunKeyPayload); the client side of the
 * local player reads {@link #CLIENT_KEY_PRESSED}, set by the client setup to the GunClientHandler key map.
 */
public class ItemGunChargeThrower extends ItemGunBaseNT {

	public static final String KEY_LASTHOOK = "lasthook";

	/** Client key state of the local player (GunClientHandler KEY_STATE), installed by the client setup. */
	public static BiPredicate<Player, GunKeybind> CLIENT_KEY_PRESSED = (player, key) -> false;

	public ItemGunChargeThrower(Properties properties, WeaponQuality quality, GunConfig... cfg) {
		super(properties, quality, cfg);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {
		super.inventoryTick(stack, world, entity, slot, isHeld);

		if(this.getState(stack, 0) == GunState.RELOADING) {
			if(getLastHook(stack) != -1) setLastHook(stack, -1);
		}

		if(isHeld && entity instanceof Player) {
			// the client only moves the local player (1.7.10 ran this for every client-side player, which changes nothing for remote players)
			if(world.isClientSide && !LOCAL_PLAYER.test(entity)) return;
			Entity e = world.getEntity(getLastHook(stack));
			if(e instanceof LegacyBulletEntity hook && !hook.isRemoved() && !hook.isRetired() && hook.ammoType() == StandardAmmoType.CT_HOOK && hook.velocity < 0.01) {
				Player player = (Player) entity;
				Vec3 vec = new Vec3(e.getX() - player.getX(), e.getY() - player.getY() - player.getEyeHeight(), e.getZ() - player.getZ());
				double line = vec.length();
				if(keyPressed(player, stack, GunKeybind.GUN_PRIMARY)) {
					vec = vec.normalize().scale(0.1);
					player.setDeltaMovement(player.getDeltaMovement().add(vec.x, vec.y + 0.04, vec.z));
					if(!world.isClientSide && line < 2) e.discard();
				} else if(!keyPressed(player, stack, GunKeybind.GUN_SECONDARY)) {
					Vec3 motion = player.getDeltaMovement();
					Vec3 nextPos = new Vec3(player.getX() + motion.x, player.getY() + player.getEyeHeight() + motion.y, player.getZ() + motion.z);
					Vec3 delta = new Vec3(e.getX() - nextPos.x, e.getY() - nextPos.y, e.getZ() - nextPos.z);
					if(delta.length() > line) {
						delta = delta.normalize().scale(line);
						Vec3 newNext = new Vec3(e.getX() - delta.x, e.getY() - delta.y, e.getZ() - delta.z);
						Vec3 vel = new Vec3(newNext.x - player.getX(), newNext.y - player.getY() - player.getEyeHeight(), newNext.z - player.getZ());
						if(vel.length() < 3) {
							player.setDeltaMovement(vel);
						}
					}
				} else {
					player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
				}

				if(player.getDeltaMovement().y > -0.1) player.fallDistance = 0;
				resetFlightTime(player);
			}
		} else {
			if(getLastHook(stack) != -1) setLastHook(stack, -1);
		}
	}

	/** HbmPlayerProps.getData(player).getKeyPressed(key) */
	private static boolean keyPressed(Player player, ItemStack stack, GunKeybind key) {
		if(player.level().isClientSide) return CLIENT_KEY_PRESSED.test(player, key);
		return key == GunKeybind.GUN_PRIMARY ? getPrimary(stack, 0) : key == GunKeybind.GUN_SECONDARY ? getSecondary(stack, 0) : false;
	}

	private static Field aboveGroundTickCount;
	private static boolean aboveGroundTickCountLookedUp;

	/** ArmorUtil.resetFlightTime: floatingTickCount = 0 (reflection like the original); aboveGroundTickCount in 1.21. */
	public static void resetFlightTime(Player player) {
		if(!(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) return;
		if(!aboveGroundTickCountLookedUp) {
			aboveGroundTickCountLookedUp = true;
			try {
				Field field = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
				field.setAccessible(true);
				aboveGroundTickCount = field;
			} catch(Exception e) {
				aboveGroundTickCount = null;
			}
		}
		if(aboveGroundTickCount == null) return;
		try {
			aboveGroundTickCount.setInt(serverPlayer.connection, 0);
		} catch(Exception ignored) { }
	}

	public static int getLastHook(ItemStack stack) { return getValueInt(stack, KEY_LASTHOOK); }
	public static void setLastHook(ItemStack stack, int value) { setValueInt(stack, KEY_LASTHOOK, value); }
}

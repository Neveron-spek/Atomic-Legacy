package com.atomiclegacy.items.weapon.sedna.impl;

import java.util.List;

import com.atomiclegacy.items.ICustomizable;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.GunEffects;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * [weapons batch F2] 1.7.10 com.hbm.items.weapon.sedna.impl.ItemGunNI4NI: regenerates one coin every 80 ticks up to
 * 4 (+2 per NICKEL / DOUBLOONS mod; the mod items are not ported, only the ids), the three hardcoded tooltip lines
 * and the dark/light/grip color customization stored in the "colors" int array.
 */
public class ItemGunNI4NI extends ItemGunBaseNT implements ICustomizable {

	public ItemGunNI4NI(Properties properties, WeaponQuality quality, GunConfig... cfg) {
		super(properties, quality, cfg);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {
		super.inventoryTick(stack, world, entity, slot, isHeld);

		if(!world.isClientSide) {

			int maxCoin = 4;
			if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_NICKEL)) maxCoin += 2;
			if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_DOUBLOONS)) maxCoin += 2;

			if(getCoinCount(stack) < maxCoin) {
				setCoinCharge(stack, getCoinCharge(stack) + 1);

				if(getCoinCharge(stack) >= 80) {
					setCoinCharge(stack, 0);
					int newCount = getCoinCount(stack) + 1;
					setCoinCount(stack, newCount);

					if(isHeld && entity instanceof LivingEntity living) {
						GunEffects.playSoundAtEntity(living, HbmSoundEvents.TECH_BOOP.get(), 1.0F, 1F + newCount / (float) maxCoin);
					}
				}
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(Component.literal("Now, don't get the wrong idea."));
		list.add(Component.literal("I ").append(Component.literal("fucking hate ").withStyle(ChatFormatting.RED)).append(Component.literal("this game.").withStyle(ChatFormatting.GRAY)));
		list.add(Component.literal("I didn't do this for you, I did it for sea."));
		super.appendHoverText(stack, context, list, flag);
	}

	@Override
	public void customize(Player player, ItemStack stack, String... args) {

		if(args.length == 0) {
			resetColors(stack);
			player.displayClientMessage(Component.literal("Colors reset!").withStyle(ChatFormatting.GREEN), false);
			return;
		}

		if(args.length != 3) {
			resetColors(stack);
			player.displayClientMessage(Component.literal("Requires three hexadecimal colors!").withStyle(ChatFormatting.RED), false);
			return;
		}

		try {
			int dark = Integer.parseInt(args[0], 16);
			int light = Integer.parseInt(args[1], 16);
			int grip = Integer.parseInt(args[2], 16);

			if(dark < 0 || dark > 0xffffff || light < 0 || light > 0xffffff || grip < 0 || grip > 0xffffff) {
				player.displayClientMessage(Component.literal("Colors must range from 0 to FFFFFF!").withStyle(ChatFormatting.RED), false);
				return;
			}

			setColors(stack, dark, light, grip);
			player.displayClientMessage(Component.literal("Colors set!").withStyle(ChatFormatting.GREEN), false);

		} catch(Throwable ex) {
			player.displayClientMessage(Component.literal(String.valueOf(ex.getLocalizedMessage())).withStyle(ChatFormatting.RED), false);
		}
	}

	public static void resetColors(ItemStack stack) {
		if(readTag(stack) == null) return;
		updateTag(stack, tag -> tag.remove("colors"));
	}

	public static void setColors(ItemStack stack, int dark, int light, int grip) {
		updateTag(stack, tag -> tag.putIntArray("colors", new int[] {dark, light, grip}));
	}

	public static int[] getColors(ItemStack stack) {
		CompoundTag tag = readTag(stack);
		if(tag == null || !tag.contains("colors")) return null;
		int[] colors = tag.getIntArray("colors");
		if(colors.length != 3) return null;
		return colors;
	}

	public static final String KEY_COIN_COUNT = "coincount";
	public static final String KEY_COIN_CHARGE = "coincharge";
	public static int getCoinCount(ItemStack stack) { return getValueInt(stack, KEY_COIN_COUNT); }
	public static void setCoinCount(ItemStack stack, int value) { setValueInt(stack, KEY_COIN_COUNT, value); }
	public static int getCoinCharge(ItemStack stack) { return getValueInt(stack, KEY_COIN_CHARGE); }
	public static void setCoinCharge(ItemStack stack, int value) { setValueInt(stack, KEY_COIN_CHARGE, value); }
}

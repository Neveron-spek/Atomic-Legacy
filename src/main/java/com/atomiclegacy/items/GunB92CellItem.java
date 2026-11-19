package com.atomiclegacy.items;

import java.util.List;

import com.atomiclegacy.registry.HbmItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * [parity c1] 1.7.10 com.hbm.items.weapon.GunB92Cell. NBT int "energy" under the same name in CUSTOM_DATA. The item
 * damage 1 that marked a full cell (the only thing the rigged-cell recipe matched on) is the state energy == 25; the
 * recipe matches the CUSTOM_DATA {energy: 25}, so the cell keeps no other tags. Drained on the server only.
 */
public class GunB92CellItem extends Item {

	public GunB92CellItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int i, boolean b) {
		if(world.isClientSide) return;
		if(entity instanceof Player player && getPower(stack) < 25) {

			for(ItemStack slot : player.getInventory().items) {
				if(!slot.isEmpty() && slot.getItem() == HbmItems.GUN_B92.get()) {
					int p = GunB92Item.getPower(slot);
					if(p > 1) {
						GunB92Item.setPower(slot, p - 1);
						setPower(stack, getPower(stack) + 1);
						return;
					}
				}
			}
		}
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || oldStack.getItem() != newStack.getItem();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(Component.literal("Draws energy from the B92, allowing you to"));
		list.add(Component.literal("reload it an additional 25 times."));
		list.add(Component.literal("The cell will permanently hold its charge,"));
		list.add(Component.literal("it is not meant to be used as a battery enhancement"));
		list.add(Component.literal("for the B92, but rather as a bomb."));
		list.add(Component.literal(""));
		list.add(Component.literal("Charges: " + getPower(stack) + " / 25"));
	}

	public static int getPower(ItemStack stack) { return GunB92Item.getInt(stack, "energy"); }
	public static void setPower(ItemStack stack, int i) { GunB92Item.setInt(stack, "energy", i); }

	public static ItemStack getFullCell() {
		ItemStack stack = new ItemStack(HbmItems.GUN_B92_AMMO.get());
		setPower(stack, 25);
		return stack;
	}
}

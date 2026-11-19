package com.atomiclegacy.items;

import com.atomiclegacy.network.ItemAnimationPayload;
import com.atomiclegacy.render.anim.BusAnimation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** 1.7.10 com.hbm.items.IAnimatedItem. getAnimation is only called on the client. */
public interface IAnimatedItem<T extends Enum<?>> {

	/** Fetch the animation for a given type */
	public BusAnimation getAnimation(T type, ItemStack stack);

	/** Should a player holding this item aim it like a gun/bow? */
	public boolean shouldPlayerModelAim(ItemStack stack);

	public Class<T> getEnum();

	/** Run a specified animation (HbmAnimationPacket(type, 0, 0)) */
	public default void playAnimation(Player player, T type) {
		if(player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new ItemAnimationPayload(type.ordinal(), 0));
		}
	}
}

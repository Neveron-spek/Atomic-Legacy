package com.atomiclegacy.items;

import java.util.List;

import com.atomiclegacy.config.HbmConfig;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** [parity c1] 1.7.10 com.hbm.items.weapon.WeaponizedCell: a dropped cell detonates after 50 seconds or when burning. */
public class WeaponizedCellItem extends Item {

	public WeaponizedCellItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity item) {
		Level world = item.level();

		if(item.tickCount > 50 * 20 || item.isOnFire()) {

			if(!world.isClientSide) {

				if(HbmConfig.DROPPED_STAR_CELL_EFFECT.get()) {
					GunB92Item.fleija((ServerLevel) world, item.getX(), item.getY(), item.getZ(), 100);
				}

				item.discard();
			}
		}

		int randy = (50 * 20) - item.tickCount;

		if(randy < 1)
			randy = 1;

		// World#spawnParticle only acts on the client
		if(world.isClientSide) {
			double w = item.getBbWidth(), h = item.getBbHeight();
			if(world.random.nextInt(50 * 20) >= randy)
				world.addParticle(DustParticleOptions.REDSTONE, item.getX() + world.random.nextGaussian() * w / 2, item.getY() + world.random.nextGaussian() * h, item.getZ() + world.random.nextGaussian() * w / 2, 0.0, 0.0, 0.0);
			else
				world.addParticle(ParticleTypes.SMOKE, item.getX() + world.random.nextGaussian() * w / 2, item.getY() + world.random.nextGaussian() * h, item.getZ() + world.random.nextGaussian() * w / 2, 0.0, 0.0, 0.0);

			if(randy < 100)
				world.addParticle(ParticleTypes.LAVA, item.getX() + world.random.nextGaussian() * w / 2, item.getY() + world.random.nextGaussian() * h, item.getZ() + world.random.nextGaussian() * w / 2, 0.0, 0.0, 0.0);
		}

		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(Component.literal("A charged energy cell, rigged to explode"));
		list.add(Component.literal("when left on the floor for too long."));
	}
}

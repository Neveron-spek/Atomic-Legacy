package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyChemicalEntity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * [weapons batch F1] EntityEffectHandler#onUpdate, HbmLivingProps.oil: an oiled (chemthrower LIQUID + FLAMMABLE spray,
 * also LegacyMistEntity) living entity that catches fire explodes (3F, no fire, block damage) and loses the oil, otherwise the
 * oil counts down one per tick and every 5 ticks one coal block dust "sweat" particle is sent to players within 25 blocks.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class GunBatchF1Events {

	private GunBatchF1Events() {
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Post event) {
		if(!(event.getEntity() instanceof LivingEntity entity)) return;
		Level level = entity.level();
		if(level.isClientSide || !(level instanceof ServerLevel server)) return;

		CompoundTag data = entity.getPersistentData();
		if(!data.contains(LegacyChemicalEntity.OIL_TICKS_TAG)) return;
		int oil = data.getInt(LegacyChemicalEntity.OIL_TICKS_TAG);

		if(oil > 0) {

			if(entity.isOnFire()) {
				data.putInt(LegacyChemicalEntity.OIL_TICKS_TAG, 0);
				level.explode(null, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 3F, false, Level.ExplosionInteraction.TNT);
			} else {
				data.putInt(LegacyChemicalEntity.OIL_TICKS_TAG, oil - 1);
			}

			if(entity.tickCount % 5 == 0) {
				AABB box = entity.getBoundingBox();
				double ix = box.minX - 0.2 + (box.maxX - box.minX + 0.4) * server.random.nextDouble();
				double iy = box.minY + (box.maxY - box.minY + 0.2) * server.random.nextDouble();
				double iz = box.minZ - 0.2 + (box.maxZ - box.minZ + 0.4) * server.random.nextDouble();
				BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COAL_BLOCK.defaultBlockState());
				for(ServerPlayer player : server.players()) {
					if(player.distanceToSqr(entity) <= 25 * 25) server.sendParticles(player, dust, false, ix, iy, iz, 1, 0, 0, 0, 0);
				}
			}
		}
	}
}

package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.network.GunPlasmaPulsePayload;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** [weapons batch F2] 1.7.10 Orchestras: ORCHESTRA_TAU, ORCHESTRA_FATMAN and ORCHESTRA_COILGUN. */
public class OrchestrasF2 {

	/**
	 * Client hook for the looped GUN_TAU_LOOP AudioWrapper of ORCHESTRA_TAU (ItemGunBaseNT.loopedSounds): called with
	 * the SPINUP animation timer while spinning up, with -1 when the sound has to stop. Set by the client setup.
	 */
	public static BiConsumer<LivingEntity, Integer> TAU_LOOP = (entity, timer) -> { };

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_TAU = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(entity.level().isClientSide) {
			// looped spinup sound (pitch 0.75 + timer * 0.01, stopped at timer 300 or on any state change)
			TAU_LOOP.accept(entity, type == GunAnimation.SPINUP ? timer : -1);
			return;
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_TAU_FIRE.get(), 0.5F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
		}

		if(type == GunAnimation.ALT_CYCLE) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_TAU_FIRE.get(), 0.5F, 0.7F + entity.getRandom().nextFloat() * 0.2F);
		}

		if(type == GunAnimation.SPINUP) {
			if(timer % 10 == 0 && timer < 130) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				if(mag.getAmount(stack, ctx.inventory) <= 0) {
					ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex);
					return;
				}
				mag.useUpAmmo(stack, ctx.inventory, 1);
			}

			if(timer > 200) {
				ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex);

				// ModDamageSource.tauBlast (bypasses armor, explosion)
				entity.hurt(entity.damageSources().source(HbmDamageTypes.TAU_BLAST), 1_000F);

				ItemGunBaseNT.setWear(stack, ctx.configIndex, Math.min(ItemGunBaseNT.getWear(stack, ctx.configIndex) + 10_000F, ctx.config.getDurability(stack)));

				double eyeY = entity.getEyeY();
				entity.level().playSound(null, entity.getX(), eyeY, entity.getZ(), HbmSoundEvents.ENTITY_UFO_BLAST.get(), SoundSource.HOSTILE, 5.0F, 0.9F);
				entity.level().playSound(null, entity.getX(), eyeY, entity.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 5.0F, 0.5F);

				if(entity.level() instanceof ServerLevel level) {
					float yaw = level.random.nextFloat() * 180F;
					for(int i = 0; i < 3; i++) {
						PacketDistributor.sendToPlayersNear(level, null, entity.getX(), eyeY, entity.getZ(), 100, new GunPlasmaPulsePayload(entity.getX(), eyeY, entity.getZ(), 1.0F, 0.8F, 0.5F, -60F + 60F * i, yaw, 2F));
					}
				}
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FATMAN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_FATMAN_RELOAD.get(), 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_COILGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE && stack.getItem() == HbmItems.GUN_N_I_4_N_I.get()) {
			if(timer == 0) GunEffects.muzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) GunEffects.playSoundAtEntity(entity, HbmSoundEvents.GUN_COIL_RELOAD.get(), 1F, 1F);
		}
	};
}

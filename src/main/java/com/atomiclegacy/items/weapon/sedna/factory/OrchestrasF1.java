package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** [weapons batch F1] 1.7.10 Orchestras: ORCHESTRA_FLAMER, ORCHESTRA_FLAMER_DAYBREAKER and ORCHESTRA_CHEMTHROWER. */
public class OrchestrasF1 {

	/**
	 * Client hook for ItemGunBaseNT.loopedSounds / MainRegistry.proxy.getLoopedSound(GUN_FLAMER_LOOP, pos, 1F, 15F, 1F, 10),
	 * installed by GunBatchF1ClientEffects: {@code true} starts the loop if it is not playing and keeps a playing loop alive
	 * (attached to the entity), {@code false} stops a playing loop. A no-op on the dedicated server.
	 */
	public static BiConsumer<LivingEntity, Boolean> LOOP_SOUND = (entity, keep) -> { };

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	/** Shared RELOAD cues of the flamethrower family (NTMSounds GUN_LATCH_OPEN, GUN_IMPACT, GUN_REVOLVER_CLOSE, GUN_CANISTER_INSERT, GUN_VALVE). */
	private static void flamerReload(LivingEntity entity, GunAnimation type, int timer) {
		if(type == GunAnimation.RELOAD) {
			if(timer == 15) sound(entity, HbmSoundEvents.GUN_LATCH_OPEN, 1F, 1F);
			if(timer == 35) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.5F, 1F);
			if(timer == 60) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 70) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 85) sound(entity, HbmSoundEvents.GUN_VALVE, 1F, 1F);
		}
	}

	/** Client side of ORCHESTRA_FLAMER / ORCHESTRA_CHEMTHROWER: the looped flame sound while CYCLE and the anim timer is below 5. */
	private static void flameLoop(LivingEntity entity, GunAnimation type, int timer) {
		if(type == GunAnimation.CYCLE) {
			if(timer < 5) {
				//start sound / keepalive
				LOOP_SOUND.accept(entity, true);
			} else {
				//stop sound due to timeout
				LOOP_SOUND.accept(entity, false);
			}
		} else {
			//stop sound due to state change
			LOOP_SOUND.accept(entity, false);
		}
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAMER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(entity.level().isClientSide) {
			flameLoop(entity, type, timer);
			return;
		}

		flamerReload(entity, type, timer);
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAMER_DAYBREAKER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		flamerReload(entity, type, timer);
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CHEMTHROWER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(entity.level().isClientSide) {
			flameLoop(entity, type, timer);
		}
	};
}

package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** [weapons batch F3] 1.7.10 Orchestras: ORCHESTRA_FIREEXT, ORCHESTRA_CHARGE_THROWER and ORCHESTRA_DRILL. */
public class OrchestrasF3 {

	private static void sound(LivingEntity entity, DeferredHolder<SoundEvent, SoundEvent> sound, float volume, float pitch) {
		GunEffects.playSoundAtEntity(entity, sound.get(), volume, pitch);
	}

	/**
	 * Client half of ORCHESTRA_DRILL (ItemGunBaseNT.loopedSounds / AudioWrapper ENGINE_LOOP keyed by the SPEED bus):
	 * installed by the client setup, no-op on the server.
	 */
	public static BiConsumer<LivingEntity, ItemStack> CLIENT_DRILL_LOOP = (entity, stack) -> { };
	/** Stop the looped engine sound due to a state change (animation other than CYCLE / CYCLE_DRY). */
	public static Consumer<LivingEntity> CLIENT_DRILL_STOP = entity -> { };

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FIREEXT = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) sound(entity, HbmSoundEvents.GUN_VALVE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CHARGE_THROWER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE_DRY) {
			Entity e = entity.level().getEntity(ItemGunChargeThrower.getLastHook(stack));
			if(timer == 0 && e == null) sound(entity, HbmSoundEvents.GUN_DRY_FIRE, 1F, 0.75F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 30) sound(entity, HbmSoundEvents.GUN_ROCKET_INSERT, 1F, 1F);
			if(timer == 40) sound(entity, HbmSoundEvents.GUN_BOLT_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DRILL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(entity.level().isClientSide) {
			CLIENT_DRILL_LOOP.accept(entity, stack);
		}
		//stop sound due to state change
		if(type != GunAnimation.CYCLE && type != GunAnimation.CYCLE_DRY && entity.level().isClientSide) {
			CLIENT_DRILL_STOP.accept(entity);
		}
		if(entity.level().isClientSide) return;

		if(type == GunAnimation.RELOAD) {
			if(timer == 15) sound(entity, HbmSoundEvents.GUN_LATCH_OPEN, 1F, 1F);
			if(timer == 35) sound(entity, HbmSoundEvents.GUN_IMPACT, 0.5F, 1F);
			if(timer == 60) sound(entity, HbmSoundEvents.GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 70) sound(entity, HbmSoundEvents.GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 85) sound(entity, HbmSoundEvents.GUN_VALVE, 1F, 1F);
		}
	};
}

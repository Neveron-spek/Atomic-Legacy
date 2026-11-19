package com.atomiclegacy.items.weapon.sedna.mods;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC2;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC3;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory44;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 WeaponModCarbineBayonet (slot BAYONET; gun_carbine): the secondary key stabs instead of inspecting. */
public class WeaponModCarbineBayonet extends WeaponModBase {

	public WeaponModCarbineBayonet(int id) {
		super(id, "BAYONET");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.FUN_ANIMNATIONS) return (T) LAMBDA_CARBINE_ANIMS;
		if(key == GunConfig.I_INSPECTDURATION) return cast(30, base);
		if(key == GunConfig.CON_ONPRESSSECONDARY) return (T) XFactory44.SMACK_A_FUCKER;
		if(key == GunConfig.CON_ORCHESTRA) return (T) ORCHESTRA_CARBINE;
		if(key == GunConfig.I_INSPECTCANCEL) return cast(false, base);
		return base;
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CARBINE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.INSPECT) {
			if(timer == 15 && ctx.getPlayer() != null) stab(ctx, HbmConfig.carbineBayonetDamage());
			return;
		}

		OrchestrasC3.ORCHESTRA_CARBINE.accept(stack, ctx);
	};

	/** The INSPECT timer == 15 body shared by ORCHESTRA_CARBINE and WeaponModMASBayonet.ORCHESTRA_MAS36 (identical apart from the damage config). */
	static void stab(LambdaContext ctx, float damage) {
		LivingEntity entity = ctx.entity;
		HitResult mop = OrchestrasC2.getMouseOver(ctx.getPlayer(), 3.0D);
		if(mop != null) {
			if(mop.getType() == HitResult.Type.ENTITY) {
				Entity hit = ((EntityHitResult) mop).getEntity();
				hit.hurt(entity.damageSources().playerAttack(ctx.getPlayer()), damage);
				Vec3 motion = hit.getDeltaMovement();
				hit.setDeltaMovement(motion.x * 2, motion.y, motion.z * 2);
				hit.hurtMarked = true;
				entity.level().playSound(null, hit.getX(), hit.getY(), hit.getZ(), HbmSoundEvents.GUN_STAB_A_FUCKER.get(), hit.getSoundSource(), 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
			}
			if(mop.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHit = (BlockHitResult) mop;
				BlockState b = entity.level().getBlockState(blockHit.getBlockPos());
				Vec3 hitVec = mop.getLocation();
				entity.level().playSound(null, hitVec.x, hitVec.y, hitVec.z, b.getSoundType(entity.level(), blockHit.getBlockPos(), entity).getStepSound(), SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
			}
		}
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CARBINE_ANIMS = (stack, type) -> {
		switch(type) {
		case INSPECT: return new BusAnimation()
				.addBus("STAB", new BusAnimationSequence().addPos(0, 1, -2, 250, IType.SIN_DOWN).hold(250).addPos(0, 1, 5, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 500, IType.SIN_FULL));
		}
		return XFactory762mm.LAMBDA_CARBINE_ANIMS.apply(stack, type);
	};
}

package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.ArmorFSBItem;
import com.atomiclegacy.items.HbmToolBehavior;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * [parity c1] 1.7.10 XFactoryPA: power-armor conditional weapons (gun_pa_melee, the melee controller, and
 * gun_pa_ranged, the ranged attack remote), together with com.hbm.items.armor.IPAMelee / IPARanged /
 * IPAWeaponsProvider and the components ArmorRPAMelee, ArmorNCRPAMelee and ArmorNCRPARanged.
 *
 * <p>The controllers do nothing by themselves: every action is forwarded to the component of the chestplate worn
 * (armorInventory[2]) when it is an IPAWeaponsProvider whose full set is worn (hasFSBArmorIgnoreCharge): rpa_plate
 * gives ArmorRPAMelee only, ncrpa_plate gives ArmorNCRPAMelee and ArmorNCRPARanged. The first person drawing of the
 * melee components (IPAMelee#setupFirstPerson / renderFirstPerson) lives in the client class ItemRenderPAMelee.
 */
public class XFactoryPA {

	/** MainRegistry.proxy.me(); set by the client setup. */
	public static Supplier<Player> LOCAL_PLAYER = () -> null;

	public interface IPAMelee {
		BusAnimation playAnim(ItemStack stack, GunAnimation type);
		void orchestra(ItemStack stack, LambdaContext ctx);

		void clickPrimary(ItemStack stack, LambdaContext ctx);
		void clickSecondary(ItemStack stack, LambdaContext ctx);
	}

	public interface IPARanged {
		void clickPrimary(ItemStack stack, LambdaContext ctx);
		void clickSecondary(ItemStack stack, LambdaContext ctx);
	}

	public static final ArmorRPAMelee RPA_MELEE = new ArmorRPAMelee();
	public static final ArmorNCRPAMelee NCRPA_MELEE = new ArmorNCRPAMelee();
	public static final ArmorNCRPARanged NCRPA_RANGED = new ArmorNCRPARanged();

	/** The FSB group of an IPAWeaponsProvider chestplate (ArmorRPA "rpa", ArmorNCRPA "ncrpa") whose set is complete, else null. */
	private static String providerGroup(Player player) {
		if(player == null) return null;
		if(!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSBItem chest)) return null;
		String group = chest.fsbGroup();
		if(!group.equals("rpa") && !group.equals("ncrpa")) return null;
		return ArmorFSBItem.hasFSBArmorIgnoringCharge(player) ? group : null;
	}

	public static IPAMelee getMeleeComponentClient() {
		return getMeleeComponentCommon(LOCAL_PLAYER.get());
	}

	public static IPAMelee getMeleeComponentCommon(Player player) {
		String group = providerGroup(player);
		if(group == null) return null;
		return group.equals("rpa") ? RPA_MELEE : NCRPA_MELEE;
	}

	public static IPARanged getRangedComponentCommon(Player player) {
		String group = providerGroup(player);
		if(group == null) return null;
		return group.equals("ncrpa") ? NCRPA_RANGED : null; // ArmorRPA#getRangedComponent returns null
	}

	public static ItemGunBaseNT createMelee(Item.Properties properties) {
		return new ItemGunPA(properties, WeaponQuality.UTILITY, new GunConfig()
				.draw(10).crosshair(Crosshair.NONE)
				.rec(new Receiver(0))
				.pp(LAMBDA_CLICK_MELEE_PRIMARY).ps(LAMBDA_CLICK_MELEE_SENONDARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_MELEE_ANIMS).orchestra(ORCHESTRA)
				);
	}

	public static ItemGunBaseNT createRanged(Item.Properties properties) {
		return new ItemGunPA(properties, WeaponQuality.UTILITY, new GunConfig()
				.draw(0).crosshair(Crosshair.CROSS)
				.rec(new Receiver(0))
				.pp(LAMBDA_CLICK_RANGED_PRIMARY).ps(LAMBDA_CLICK_RANGED_SENONDARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				);
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		IPAMelee component = getMeleeComponentCommon(ctx.getPlayer());
		if(component != null) component.orchestra(stack, ctx);
	};

	public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MELEE_ANIMS = (stack, type) -> {
		IPAMelee component = getMeleeComponentClient();
		if(component != null) return component.playAnim(stack, type);
		return null;
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		IPAMelee component = getMeleeComponentCommon(ctx.getPlayer());
		if(component != null) component.clickPrimary(stack, ctx);
	};
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_SENONDARY = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		IPAMelee component = getMeleeComponentCommon(ctx.getPlayer());
		if(component != null) component.clickSecondary(stack, ctx);
	};

	public static void doSwing(ItemStack stack, LambdaContext ctx, GunAnimation anim, int cooldown) {

		Player player = ctx.getPlayer();
		int index = ctx.configIndex;
		GunState state = ItemGunBaseNT.getState(stack, index);

		if(state == GunState.IDLE) {
			ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);
			ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
			ItemGunBaseNT.setTimer(stack, index, cooldown);
		}
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		IPARanged component = getRangedComponentCommon(ctx.getPlayer());
		if(component != null) component.clickPrimary(stack, ctx);
	};
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_SENONDARY = (stack, ctx) -> {
		if(ctx.getPlayer() == null) return;
		IPARanged component = getRangedComponentCommon(ctx.getPlayer());
		if(component != null) component.clickSecondary(stack, ctx);
	};

	public static class ItemGunPA extends ItemGunBaseNT {

		public ItemGunPA(Item.Properties properties, WeaponQuality quality, GunConfig... cfg) {
			super(properties, quality, cfg);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) { }
	}

	/**
	 * Shared hit of the melee components: EntityDamageUtil.getMouseOver(player, 3.0D, 0.5D), then
	 * attackEntityFromNT(living, causePlayerDamage(player), damage, true, false, knockback, dt, pierce) (the port's
	 * sedna physical hit; the allowSpecialCancel = false nuance of the Forge attack event has no counterpart) or a
	 * plain player attack for non-living entities, the hit sound at the entity, or the step sound of the block hit.
	 */
	private static void hit(ItemStack stack, LambdaContext ctx, boolean light, float damage, float largeMult, SoundEvent sound, GibRule gib) {
		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		HitResult mop = OrchestrasC2.getMouseOver(player, 3.0D, 0.5D);

		if(mop != null) {
			if(mop instanceof EntityHitResult ehr && ehr.getEntity().isAlive()) {
				float knockback = light ? 0F : 1.5F;
				float dt = light ? 5F : 15F;
				float pierce = light ? 0.1F : 0.25F;

				if(ehr.getEntity() instanceof LivingEntity living) {
					if(living.getMaxHealth() >= 100) damage *= largeMult;
					LegacyProjectileUtil.attackSednaPhysicalKnockback(living, player, damage, dt, pierce, knockback);
					if(gib.test(living)) HbmToolBehavior.gib(living);
				} else {
					ehr.getEntity().hurt(player.damageSources().playerAttack(player), damage);
				}

				entity.level().playSound(null, ehr.getEntity().getX(), ehr.getEntity().getY(), ehr.getEntity().getZ(), sound, SoundSource.PLAYERS, 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
			}
			if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {
				BlockState b = entity.level().getBlockState(bhr.getBlockPos());
				Vec3 hitVec = mop.getLocation();
				entity.level().playSound(null, hitVec.x, hitVec.y, hitVec.z, b.getSoundType(entity.level(), bhr.getBlockPos(), entity).getStepSound(), SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
			}
		}
	}

	private interface GibRule {
		boolean test(LivingEntity living);
	}

	/** 1.7.10 com.hbm.items.armor.ArmorRPAMelee */
	public static class ArmorRPAMelee implements IPAMelee {

		@Override public void clickPrimary(ItemStack stack, LambdaContext ctx) { doSwing(stack, ctx, GunAnimation.CYCLE, 14); }
		@Override public void clickSecondary(ItemStack stack, LambdaContext ctx) { doSwing(stack, ctx, GunAnimation.ALT_CYCLE, 20); }

		@Override
		public void orchestra(ItemStack stack, LambdaContext ctx) {
			LivingEntity entity = ctx.entity;
			if(entity.level().isClientSide) return;
			GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
			int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

			// refire check so you can just continuously beat the shit out of someone
			if(type == GunAnimation.CYCLE && timer == 14 && ItemGunBaseNT.getPrimary(stack, 0)) {
				doSwing(stack, ctx, GunAnimation.CYCLE, 14);
			}

			boolean swings = type == GunAnimation.CYCLE && (timer == 3 || timer == 9);
			boolean slap = type == GunAnimation.ALT_CYCLE && timer == 8;

			if((swings || slap) && ctx.getPlayer() != null) {
				hit(stack, ctx, swings, swings ? HbmConfig.meleeRpaSwingsDamage() : HbmConfig.meleeRpaSlapDamage(), HbmConfig.meleeRpaLargeMult(), HbmSoundEvents.GUN_SMACK.get(),
						living -> living.getRandom().nextInt(slap ? 3 : 10) == 0 && !living.isAlive());
			}
		}

		@Override
		public BusAnimation playAnim(ItemStack stack, GunAnimation type) {
			if(type == GunAnimation.EQUIP) return new BusAnimation()
					.addBus("EQUIP", new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 250, IType.SIN_DOWN));
			if(type == GunAnimation.CYCLE) return new BusAnimation()
					.addBus("SWINGRIGHT", new BusAnimationSequence().addPos(1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
					.addBus("SWINGLEFT", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
			if(type == GunAnimation.ALT_CYCLE) return new BusAnimation()
					.addBus("SLAPTURN", new BusAnimationSequence().addPos(1, 0, 0, 250, IType.LINEAR).hold(150).addPos(0, 0, 0, 350, IType.LINEAR))
					.addBus("SLAP", new BusAnimationSequence().hold(250).addPos(1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL));

			return null;
		}
	}

	/** 1.7.10 com.hbm.items.armor.ArmorNCRPAMelee */
	public static class ArmorNCRPAMelee implements IPAMelee {

		@Override public void clickPrimary(ItemStack stack, LambdaContext ctx) { doSwing(stack, ctx, GunAnimation.CYCLE, 25); }
		@Override public void clickSecondary(ItemStack stack, LambdaContext ctx) { doSwing(stack, ctx, GunAnimation.ALT_CYCLE, 30); }

		@Override
		public void orchestra(ItemStack stack, LambdaContext ctx) {
			LivingEntity entity = ctx.entity;
			if(entity.level().isClientSide) return;
			GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
			int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

			boolean swings = type == GunAnimation.CYCLE && (timer == 5 || timer == 15);
			boolean sweep = type == GunAnimation.ALT_CYCLE && timer == 5;

			if((swings || sweep) && ctx.getPlayer() != null) {
				hit(stack, ctx, swings, swings ? HbmConfig.meleeNcrpaSwingsDamage() : HbmConfig.meleeNcrpaSweepDamage(), HbmConfig.meleeNcrpaLargeMult(), HbmSoundEvents.GUN_STAB_A_FUCKER.get(),
						living -> !living.isAlive());
			}
		}

		@Override
		public BusAnimation playAnim(ItemStack stack, GunAnimation type) {
			if(type == GunAnimation.EQUIP) return new BusAnimation()
					.addBus("EQUIP", new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 750, IType.SIN_DOWN));
			if(type == GunAnimation.CYCLE) return new BusAnimation()
					.addBus("SWINGRIGHT", new BusAnimationSequence().addPos(1, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
					.addBus("SWINGLEFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL));
			if(type == GunAnimation.ALT_CYCLE) return new BusAnimation()
					.addBus("SWEEPTURN", new BusAnimationSequence().addPos(1, 0, 0, 100, IType.LINEAR).hold(350).addPos(0, 0, 0, 500, IType.LINEAR))
					.addBus("SWEEPCUT", new BusAnimationSequence().hold(100).addPos(1, 0, 0, 250, IType.SIN_DOWN).hold(100).addPos(0, 0, 0, 500, IType.SIN_FULL));

			return null;
		}
	}

	/** 1.7.10 com.hbm.items.armor.ArmorNCRPARanged */
	public static class ArmorNCRPARanged implements IPARanged {

		public static MagazineBelt rocketSteerMag = new MagazineBelt();
		public static MagazineBelt rocketMag = new MagazineBelt();

		@Override public void clickPrimary(ItemStack stack, LambdaContext ctx) { fireRocket(stack, ctx, true); }
		@Override public void clickSecondary(ItemStack stack, LambdaContext ctx) { fireRocket(stack, ctx, false); }

		public static void fireRocket(ItemStack stack, LambdaContext ctx, boolean steer) {

			Player player = ctx.getPlayer();
			GunState state = ItemGunBaseNT.getState(stack, 0);
			MagazineBelt mag = steer ? rocketSteerMag : rocketMag;

			if(state == GunState.IDLE) {
				if(mag.acceptedBullets.isEmpty()) {
					mag.addConfigs(XFactoryRocket.ncrpaConfigs(steer));
				}
				BulletConfig cfg = mag.getType(stack, player.getInventory());
				int amount = mag.getAmount(stack, player.getInventory());

				if(amount > 0) {
					mag.useUpAmmo(stack, player.getInventory(), 1);
					Lego.spawnBullet(player, cfg, HbmConfig.rangedNcrpaDamage(), 0, 0.25F * (player.getRandom().nextBoolean() ? - 1 : 1), 0, 0);
					ItemGunBaseNT.setState(stack, 0, GunState.COOLDOWN);
					ItemGunBaseNT.setTimer(stack, 0, 10);
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.GUN_ROCKET_FIRE.get(), SoundSource.PLAYERS, 0.5F, 0.9F + player.getRandom().nextFloat() * 0.2F);
					player.inventoryMenu.broadcastChanges();
				} else {
					ItemGunBaseNT.setState(stack, 0, GunState.COOLDOWN);
					ItemGunBaseNT.setTimer(stack, 0, 10);
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.GUN_DRY_FIRE.get(), SoundSource.PLAYERS, 1F, 1F);
				}
			}
		}
	}
}

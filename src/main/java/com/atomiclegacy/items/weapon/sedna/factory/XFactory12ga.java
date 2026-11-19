package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.DecoCrtBlock;
import com.atomiclegacy.blocks.LegacyBarrelBlock;
import com.atomiclegacy.entity.LegacyDuchessGambitEntity;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineBelt;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.network.GunPlasmaPulsePayload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.render.anim.AnimationLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 1.7.10 XFactory12ga: the nine 12 gauge rounds, g12_equestrian_tkr (ammo_secret G12_EQUESTRIAN),
 * gun_maresleg, gun_maresleg_akimbo, gun_maresleg_broken and gun_liberator; [weapons batch D1] gun_spas12,
 * gun_autoshotgun, gun_autoshotgun_shredder and gun_autoshotgun_sexy with their shredder/equestrian BJ rounds.
 */
public class XFactory12ga {

	/** XWeaponModManager.ID_SAWED_OFF (mod item SAW is not ported; renderers/name mutator keep the original check). */
	public static final int ID_SAWED_OFF = 203;

	/** LAMBDA_STANDARD_EXPLODE: Lego.standardExplode(bullet, mop, 2F), then the bullet dies. */
	public static final BulletImpact LAMBDA_STANDARD_EXPLODE = (bullet, hit) -> {
		standardExplode(bullet, hit.getLocation(), bullet.baseDamage(), 2F, 1F);
		return true;
	};

	/** g12_phosphorus onImpact: entity hits get HbmLivingProps.phosphorus raised to at least 300, the standard hit follows. */
	public static final BulletImpact LAMBDA_PHOSPHORUS = (bullet, hit) -> {
		if(hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
			HbmLivingHazards data = HbmLivingHazards.get(living);
			data.extendPhosphorus(300);
			HbmLivingHazards.set(living, data);
		}
		return false;
	};

	static {
		float buckshotSpread = 0.035F;
		float magnumSpread = 0.015F;
		StandardAmmoType.G12_BP.setBlackPowder(true).setProjectiles(8).setSpread(buckshotSpread).setRicochetAngle(15);
		StandardAmmoType.G12_BP_MAGNUM.setBlackPowder(true).setProjectiles(4).setSpread(buckshotSpread).setRicochetAngle(25);
		StandardAmmoType.G12_BP_SLUG.setBlackPowder(true).setSpread(0.01F).setRicochetAngle(5);
		StandardAmmoType.G12.setProjectiles(8).setSpread(buckshotSpread).setRicochetAngle(15);
		StandardAmmoType.G12_SLUG.setSpread(0.0F).setRicochetAngle(25);
		StandardAmmoType.G12_FLECHETTE.setProjectiles(8).setSpread(0.025F).setRicochetAngle(5);
		StandardAmmoType.G12_MAGNUM.setProjectiles(4).setSpread(magnumSpread).setRicochetAngle(15);
		StandardAmmoType.G12_EXPLOSIVE.setOnImpact(LAMBDA_STANDARD_EXPLODE).setSpread(0F).setRicochetAngle(15);
		StandardAmmoType.G12_PHOSPHORUS.setProjectiles(8).setSpread(magnumSpread).setRicochetAngle(15).setOnImpact(LAMBDA_PHOSPHORUS);
		// G12_EQUESTRIAN (g12_equestrian_tkr): setDamage(0F) only, every other field is the BulletConfig default.
	}

	public static final BulletConfig g12_bp = new BulletConfig(StandardAmmoType.G12_BP).setCasingEffect(OrchestrasC4.CASING_G12_BP);
	public static final BulletConfig g12_bp_magnum = new BulletConfig(StandardAmmoType.G12_BP_MAGNUM).setCasingEffect(OrchestrasC4.CASING_G12_BP);
	public static final BulletConfig g12_bp_slug = new BulletConfig(StandardAmmoType.G12_BP_SLUG).setCasingEffect(OrchestrasC4.CASING_G12_BP);
	public static final BulletConfig g12 = new BulletConfig(StandardAmmoType.G12).setCasingEffect(OrchestrasC4.CASING_G12);
	public static final BulletConfig g12_slug = new BulletConfig(StandardAmmoType.G12_SLUG).setCasingEffect(OrchestrasC4.CASING_G12_SLUG);
	public static final BulletConfig g12_flechette = new BulletConfig(StandardAmmoType.G12_FLECHETTE).setCasingEffect(OrchestrasC4.CASING_G12_FLECHETTE);
	public static final BulletConfig g12_magnum = new BulletConfig(StandardAmmoType.G12_MAGNUM).setCasingEffect(OrchestrasC4.CASING_G12_MAGNUM);
	public static final BulletConfig g12_explosive = new BulletConfig(StandardAmmoType.G12_EXPLOSIVE).setCasingEffect(OrchestrasC4.CASING_G12_EXPLOSIVE);
	public static final BulletConfig g12_phosphorus = new BulletConfig(StandardAmmoType.G12_PHOSPHORUS).setCasingEffect(OrchestrasC4.CASING_G12_PHOSPHORUS);

	/** BulletConfig.setItem(EnumAmmoSecret.G12_EQUESTRIAN): matches the flattened ammo_secret variant g12_equestrian. */
	public static final BulletConfig g12_equestrian_tkr = new BulletConfig(StandardAmmoType.G12_EQUESTRIAN) {
		@Override
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && stack.is(HbmItems.AMMO_SECRET.get())
					&& stack.getItem() instanceof LegacyVariantItem variant && "g12_equestrian".equals(variant.variant(stack).id());
		}

		@Override
		public ItemStack toStack() {
			return LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "g12_equestrian");
		}
	}.setCasingEffect(OrchestrasC4.CASING_G12_EQUESTRIAN);

	private static final BulletConfig[] all = new BulletConfig[] {g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum, g12_explosive, g12_phosphorus};

	public static ItemGunBaseNT createMaresleg(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(600).draw(10).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::maresLegDamage).delay(20).reload(22, 10, 13, 0).jam(24).sound(HbmSoundEvents.GUN_SHOTGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 6).addConfigs(all))
						.offset(0.75, -0.0625, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
				.setupStandardConfiguration()
				.anim(LAMBDA_MARESLEG_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_MARESLEG)
				).setDefaultAmmo(StandardAmmoType.G12, 12).setNameMutator(LAMBDA_NAME_MARESLEG);
	}

	public static ItemGunBaseNT createMareslegAkimbo(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE,
				new GunConfig().dura(600).draw(5).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::maresLegAkimboLeftDamage).spreadHipfire(0F).spreadAmmo(1.35F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HbmSoundEvents.GUN_SHOTGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 6).addConfigs(all))
						.offset(0.75, -0.0625, 0.1875D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_MARESLEG_SHORT_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_MARESLEG_AKIMBO),
				new GunConfig().dura(600).draw(5).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::maresLegAkimboRightDamage).spreadHipfire(0F).spreadAmmo(1.35F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HbmSoundEvents.GUN_SHOTGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(1, 6).addConfigs(all))
						.offset(0.75, -0.0625, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
				.ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
				.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_MARESLEG_SHORT_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_MARESLEG_AKIMBO)
				).setDefaultAmmo(StandardAmmoType.G12, 24);
	}

	public static ItemGunBaseNT createMareslegBroken(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(0).draw(5).inspect(39).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::brokenDamage).spreadAmmo(1.15F).delay(20).reload(22, 10, 13, 0).jam(24).sound(HbmSoundEvents.GUN_SHOTGUN_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 6).addConfigs(g12_equestrian_tkr, g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum, g12_explosive, g12_phosphorus))
						.offset(0.75, -0.0625, -0.1875)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_NOWEAR_FIRE).recoil(LAMBDA_RECOIL_MARESLEG))
				.setupStandardConfiguration()
				.anim(LAMBDA_MARESLEG_SHORT_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_MARESLEG_SHORT)
				).setDefaultAmmo(StandardAmmoType.G12_MAGNUM, 24);
	}

	public static ItemGunBaseNT createLiberator(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(200).draw(20).inspect(21).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::liberatorDamage).delay(20).rounds(4).reload(25, 15, 7, 0).jam(45).sound(HbmSoundEvents.GUN_LIBERATOR_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 4).addConfigs(all))
						.offset(0.75, -0.0625, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_LIBERATOR))
				.setupStandardConfiguration()
				.anim(LAMBDA_LIBERATOR_ANIMS).orchestra(OrchestrasC4.ORCHESTRA_LIBERATOR)
				).setDefaultAmmo(StandardAmmoType.G12, 12);
	}

	/** Lego.LAMBDA_NOWEAR_FIRE: standard fire without wear. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE = (stack, ctx) -> {
		Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
	};

	public static Function<ItemStack, String> LAMBDA_NAME_MARESLEG = (stack) -> {
		if(XWeaponModManager.hasUpgrade(stack, 0, ID_SAWED_OFF)) return stack.getItem().getDescriptionId() + "_short";
		return null;
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MARESLEG = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_LIBERATOR = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	/**
	 * Lego.standardExplode(bullet, hitVec, range, damageMod): ExplosionVNT(range) exactly at the hit
	 * vector with the shooter as exploder, no block processing, EntityProcessorCrossSmooth(1,
	 * damage * damageMod) using the round's piercing values (knockback multiplier 1),
	 * PlayerProcessorStandard and ExplosionEffectWeapon(10, 2.5F, 1F). Same entity processing as
	 * XFactory50.tinyExplode, which is the port of the same EntityProcessorCrossSmooth.
	 */
	public static void standardExplode(LegacyBulletEntity bullet, Vec3 hitVec, float damage, float range, float damageMod) {
		if(!(bullet.level() instanceof ServerLevel level)) return;
		double x = hitVec.x;
		double y = hitVec.y;
		double z = hitVec.z;
		Entity exploder = bullet.ownerEntity();
		StandardAmmoType ammo = bullet.ammoType();

		/// EntityProcessorCross(1) + EntityProcessorCrossSmooth ///
		float fixedDamage = damage * damageMod;
		double nodeDist = 1D;
		double knockbackMult = 1D;
		float size = range * 2.0F;
		AABB area = new AABB(x - size - 1.0D, y - size - 1.0D, z - size - 1.0D, x + size + 1.0D, y + size + 1.0D, z + size + 1.0D);
		// setAllowSelfDamage: the exploder is not excluded
		List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
		Explosion compat = new Explosion(level, exploder, x, y, z, range, false, Explosion.BlockInteraction.KEEP);
		EventHooks.onExplosionDetonate(level, compat, list, size);

		Vec3 center = new Vec3(x, y, z);
		Vec3[] nodes = new Vec3[7];
		net.minecraft.core.Direction[] dirs = { net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST };
		for(int i = 0; i < 6; i++) nodes[i] = new Vec3(x + dirs[i].getStepX() * nodeDist, y + dirs[i].getStepY() * nodeDist, z + dirs[i].getStepZ() * nodeDist);
		nodes[6] = center; // ForgeDirection.UNKNOWN

		Map<Entity, Float> damageMap = new LinkedHashMap<>();
		HashMap<ServerPlayer, Vec3> affectedPlayers = new HashMap<>();

		for(Entity entity : list) {
			AABB box = entity.getBoundingBox();
			double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
			double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
			double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
			double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
			double distanceScaled = dist / size;

			if(distanceScaled <= 1.0D) {
				double deltaX = entity.getX() - x;
				double deltaY = entity.getEyeY() - y;
				double deltaZ = entity.getZ() - z;
				double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

				if(distance != 0.0D) {
					deltaX /= distance;
					deltaY /= distance;
					deltaZ /= distance;

					double density = 0;
					for(Vec3 vec : nodes) {
						double d = Explosion.getSeenPercent(vec, entity);
						if(d > density) density = d;
					}

					double knockback = (1.0D - distanceScaled) * density;
					float dmg = density < 0.125 ? 0F : (float) (fixedDamage * (1 - distanceScaled));
					if(!damageMap.containsKey(entity) || damageMap.get(entity) < dmg) damageMap.put(entity, dmg);
					double enchKnockback = entity instanceof LivingEntity living ? knockback * (1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) : knockback;

					if(!(entity instanceof LegacyBulletEntity) && !(entity instanceof Player)) {
						entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * enchKnockback * knockbackMult, deltaY * enchKnockback * knockbackMult, deltaZ * enchKnockback * knockbackMult));
						entity.hurtMarked = true;
					}

					if(entity instanceof ServerPlayer player) {
						affectedPlayers.put(player, new Vec3(deltaX * knockback * knockbackMult, deltaY * knockback * knockbackMult, deltaZ * knockback * knockbackMult));
					}
				}
			}
		}

		for(Map.Entry<Entity, Float> entry : damageMap.entrySet()) {
			Entity entity = entry.getKey();
			float amount = entry.getValue();
			if(!entity.isAlive()) continue;
			if(exploder == entity) amount *= 0.5F;
			// BulletConfig.getDamage(null, exploder, EXPLOSIVE)
			DamageSource dmg = level.damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, null, exploder instanceof LivingEntity ? exploder : null);
			if(entity instanceof LivingEntity living) {
				amount = LegacyProjectileUtil.sednaExplosiveDamage(living, amount, ammo.armorThresholdNegation(), ammo.armorPiercingPercent());
				XFactory50.hurtWithoutKnockback(living, dmg, amount);
			} else {
				entity.hurt(dmg, amount);
			}
		}

		/// PlayerProcessorStandard: ExplosionKnockbackPacket adds the motion on the client ///
		for(Map.Entry<ServerPlayer, Vec3> entry : affectedPlayers.entrySet()) {
			ServerPlayer player = entry.getKey();
			player.setDeltaMovement(player.getDeltaMovement().add(entry.getValue()));
			player.hurtMarked = true;
		}

		/// ExplosionEffectWeapon(10, 2.5F, 1F): ExplosionSmallCreator.composeEffect ///
		LegacyProjectileUtil.sendSmallExplosionEffect(level, center, 10, 2.5F, 1F);
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MARESLEG_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
				.addBus("SIGHT", new BusAnimationSequence().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
		case RELOAD:
			// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
			boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) <= 0;
			return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 400, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 400).addPos(-85, 0, 0, 200))
				.addBus("SHELL", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(0, 0.25, -3, 0).addPos(0, empty ? 0.25 : 0.125, -1.5, 150, IType.SIN_UP).addPos(0, empty ? 0.25 : -0.25, 0, 150, IType.SIN_DOWN))
				.addBus("FLAG", new BusAnimationSequence().addPos(0, 0, 0, empty ? 900 : 0).addPos(1, 1, 1, 0));
		case RELOAD_CYCLE: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 0))
				.addBus("LEVER", new BusAnimationSequence().addPos(-85, 0, 0, 0))
				.addBus("SHELL", new BusAnimationSequence().addPos(0, 0.25, -3, 0).addPos(0, 0.125, -1.5, 150, IType.SIN_UP).addPos(0, -0.125, 0, 150, IType.SIN_DOWN))
				.addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
		case RELOAD_END: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(-85, 0, 0, 0).addPos(0, 0, 0, 200))
				.addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(-85, 0, 0, 0).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 650).addPos(-85, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 850).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 45, 800).addPos(0, 0, 0, 200, IType.SIN_UP))
				.addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
		case INSPECT: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-35, 0, 0, 300, IType.SIN_FULL).addPos(-35, 0, 0, 1150).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("TURN", new BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, -90, 500, IType.SIN_FULL).addPos(0, 0, -90, 500).addPos(0, 0, 0, 500, IType.SIN_FULL));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MARESLEG_SHORT_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 250, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
				.addBus("SIGHT", new BusAnimationSequence().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200))
				.addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
				.addBus("SHELL", new BusAnimationSequence().addPos(-20, 0, 0, 0)); //gets rid of the shell in the barrel during cycling
		case CYCLE_DRY: return new BusAnimation()
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("HAMMER", new BusAnimationSequence().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200))
				.addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
				.addBus("SHELL", new BusAnimationSequence().addPos(-20, 0, 0, 0));
		case JAMMED: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(-85, 0, 0, 0).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 650).addPos(-85, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
				.addBus("FLAG", new BusAnimationSequence().addPos(1, 1, 1, 0));
		}

		return LAMBDA_MARESLEG_ANIMS.apply(stack, type);
	};

	/** This fucking sucks */
	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_LIBERATOR_ANIMS = (stack, type) -> {
		// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
		int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -2.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL));
		case CYCLE_DRY: return new BusAnimation();
		case RELOAD: if(ammo == 0) return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
				.addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
				.addBus("SHELL1", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
				.addBus("SHELL2", new BusAnimationSequence().addPos(2, -4, -2, 0))
				.addBus("SHELL3", new BusAnimationSequence().addPos(2, -4, -2, 0))
				.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0));
			if(ammo == 1) return new BusAnimation()
					.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
					.addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
					.addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL2", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
					.addBus("SHELL3", new BusAnimationSequence().addPos(2, -4, -2, 0))
					.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0));
			if(ammo == 2) return new BusAnimation()
					.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
					.addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
					.addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL3", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
					.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0));
			if(ammo == 3) return new BusAnimation()
					.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100))
					.addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
					.addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL3", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
		case RELOAD_CYCLE:
			if(ammo == 0) return new BusAnimation()
					.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0))
					.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0))
					.addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL2", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
					.addBus("SHELL3", new BusAnimationSequence().addPos(2, -4, -2, 0))
					.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0));
			if(ammo == 1) return new BusAnimation()
					.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0))
					.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0))
					.addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL3", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
					.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0));
			if(ammo == 2) return new BusAnimation()
					.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0))
					.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0))
					.addBus("SHELL1", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL2", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL3", new BusAnimationSequence().addPos(0, 0, 0, 0))
					.addBus("SHELL4", new BusAnimationSequence().addPos(2, -4, -2, 0).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
			return null;
		case RELOAD_END: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50))
				.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP))
				.addBus(ammo >= 0 ? "SHELL1" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo >= 1 ? "SHELL2" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo >= 2 ? "SHELL3" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo >= 3 ? "SHELL4" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo < 0 ? "SHELL1" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 1 ? "SHELL2" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 2 ? "SHELL3" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 3 ? "SHELL4" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0));
		case JAMMED: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50).addPos(0, 0, 0, 550).addPos(15, 0, 0, 100).addPos(15, 0, 0, 600).addPos(0, 0, 0, 50))
				.addBus("BREAK", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP).addPos(0, 0, 0, 600).addPos(45, 0, 0, 250, IType.SIN_DOWN).addPos(45, 0, 0, 300).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus(ammo >= 0 ? "SHELL1" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo >= 1 ? "SHELL2" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo >= 2 ? "SHELL3" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo >= 3 ? "SHELL4" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo < 0 ? "SHELL1" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 1 ? "SHELL2" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 2 ? "SHELL3" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 3 ? "SHELL4" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0));
		case INSPECT: return new BusAnimation()
				.addBus("LATCH", new BusAnimationSequence().addPos(15, 0, 0, 100).addPos(15, 0, 0, 1100).addPos(0, 0, 0, 50))
				.addBus("BREAK", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN).addPos(60, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_UP))
				.addBus(ammo > 0 ? "SHELL1" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo > 1 ? "SHELL2" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo > 2 ? "SHELL3" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo > 3 ? "SHELL4" : "NULL", new BusAnimationSequence().addPos(0, 0, 0, 0))
				.addBus(ammo < 1 ? "SHELL1" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 2 ? "SHELL2" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 3 ? "SHELL3" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0))
				.addBus(ammo < 4 ? "SHELL4" : "NULL", new BusAnimationSequence().addPos(2, -8, -2, 0));
		}

		return null;
	};

	// =====================================================================================================
	// [weapons batch D1] gun_spas12, gun_autoshotgun, gun_autoshotgun_shredder, gun_autoshotgun_sexy with
	// g12_equestrian_bj, the shredder beams (g12_shredder*) and their submunitions (g12_sub*).
	// =====================================================================================================

	/** LAMBDA_BOAT: a Duchess Gambit 50 blocks above the hit, weapon.boat played another 50 blocks above it. */
	public static final BulletImpact LAMBDA_BOAT = (bullet, mop) -> {
		Vec3 hit = mop.getLocation();
		LegacyDuchessGambitEntity pippo = new LegacyDuchessGambitEntity(bullet.level(), new Vec3(hit.x, hit.y + 50, hit.z));
		bullet.level().addFreshEntity(pippo);
		bullet.level().playSound(null, pippo.getX(), pippo.getY() + 50, pippo.getZ(), HbmSoundEvents.WEAPON_BOAT.get(), SoundSource.PLAYERS, 100F, 1F);
		return true;
	};

	/** BulletConfig.LAMBDA_STANDARD_ENTITY_HIT with dmgClass PLASMA (getDamage + EntityDamageUtil.attackEntityFromNT). */
	public static final BulletImpact LAMBDA_PLASMA_ENTITY_HIT = (bullet, mop) -> {
		if(!(mop instanceof EntityHitResult ehr)) return false;
		Entity entity = ehr.getEntity();
		Entity owner = bullet.ownerEntity();
		StandardAmmoType ammo = bullet.ammoType();

		if(entity == owner && bullet.tickCount < ammo.selfDamageDelay()) return false;
		if(entity instanceof LivingEntity l && l.getHealth() <= 0) return false;

		DamageSource source = BeamBulletConfig.getDamage(bullet, owner instanceof LivingEntity lo ? lo : null, DamageClass.PLASMA);
		float intendedDamage = bullet.baseDamage();

		if(!(entity instanceof LivingEntity living)) {
			LegacyProjectileUtil.hurtNoIFrame(entity, source, bullet.baseDamage());
			return false;
		}
		if(ammo.headshotMultiplier() > 1F) {
			double head = living.getBbHeight() - living.getEyeHeight();
			if(living.isAlive() && ehr.getLocation().y > (living.getY() + living.getBbHeight() - head * 2)) {
				intendedDamage *= ammo.headshotMultiplier();
			}
		}

		float prevHealth = living.getHealth();
		BeamBulletConfig.attackEntityFromNT(living, source, DamageClass.PLASMA, intendedDamage, ammo.knockbackMultiplier(), ammo.armorThresholdNegation(), ammo.armorPiercingPercent());
		float newHealth = living.getHealth();

		if(ammo.damageFalloffByPenetration()) bullet.setDamage(bullet.baseDamage() - Math.max(prevHealth - newHealth, 0) * 0.5F);
		if(!ammo.penetrates() || bullet.baseDamage() < 0) {
			bullet.setPos(ehr.getLocation());
			return true;
		}
		return false;
	};

	/** LAMBDA_SHREDDER_RICOCHET ("this sucks"): ricochets up to 3 times (angle 90), with a plasma pulse and a 0.5 block blast every bounce. */
	public static final BulletImpact LAMBDA_SHREDDER_RICOCHET = (bullet, mop) -> {
		if(!(mop instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return false;
		BlockPos pos = hit.getBlockPos();
		BlockState b = level.getBlockState(pos);

		// Material.glass, same mapping as the port's standard ricochet
		if(b.getSoundType(level, pos, bullet) == SoundType.GLASS) {
			level.destroyBlock(pos, false, bullet);
			bullet.setPos(hit.getLocation());
			return false;
		}
		// BlockDetonatable#onShot: only RedBarrel (detonateOnShot) reacts
		if(b.getBlock() instanceof LegacyBarrelBlock barrel && barrel.detonatesWhenShot()) {
			barrel.detonateOnShot(level, pos, bullet);
		}
		// deco_crt: meta % 4 + 4 keeps the rotation and switches to crt_broken
		if(b.is(HbmBlocks.DECO_CRT.get())) {
			level.setBlock(pos, b.setValue(DecoCrtBlock.VARIANT, 1), 3);
		}

		Direction dir = hit.getDirection();
		Vec3 face = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		Vec3 vel = bullet.getDeltaMovement().normalize();

		double angle = Math.abs(getCrossAngle(vel, face) - 90);
		StandardAmmoType ammo = bullet.ammoType();

		if(angle <= ammo.ricochetAngle()) {

			spawnPulse(level, hit, bullet.getYRot(), bullet.getXRot());

			List<Entity> blast = level.getEntities(bullet, new AABB(bullet.getX(), bullet.getY(), bullet.getZ(), bullet.getX(), bullet.getY(), bullet.getZ()).inflate(0.5, 0.5, 0.5));
			Entity owner = bullet.ownerEntity();
			DamageSource source = BeamBulletConfig.getDamage(bullet, owner instanceof LivingEntity lo ? lo : null, DamageClass.PLASMA);

			for(Entity e : blast) {
				if(!e.isAlive()) continue;
				if(e instanceof LivingEntity living) {
					BeamBulletConfig.attackEntityFromNT(living, source, DamageClass.PLASMA, bullet.baseDamage(), 0D, 0F, 0F);
				} else {
					e.hurt(source, bullet.baseDamage());
				}
			}

			boolean dead = false;
			bullet.setRicochets(bullet.ricochets() + 1);
			if(bullet.ricochets() > ammo.maxRicochetCount()) {
				bullet.setPos(hit.getLocation());
				bullet.discard();
				dead = true;
			}

			Vec3 motion = bullet.getDeltaMovement();
			switch(dir.getAxis()) {
			case Y -> bullet.setDeltaMovement(motion.x, -motion.y, motion.z);
			case Z -> bullet.setDeltaMovement(motion.x, motion.y, -motion.z);
			case X -> bullet.setDeltaMovement(-motion.x, motion.y, motion.z);
			}
			bullet.setPos(hit.getLocation());
			// TrackerUtil.sendTeleport: so the ricochet is more accurate instead of the interp smoothing everything up
			if(!dead) level.getChunkSource().broadcast(bullet, new ClientboundTeleportEntityPacket(bullet));
			return dead;

		} else {
			bullet.setPos(hit.getLocation());
			return true;
		}
	};

	/**
	 * makeShredderSubmunition: original.clone() (every BulletConfig field, including projectiles, spread, onImpact,
	 * casing) with setRicochetAngle(90).setRicochetCount(3).setVel(0.5F).setLife(50).setupDamageClass(PLASMA)
	 * .setOnRicochet(LAMBDA_SHREDDER_RICOCHET). The PLASMA class is applied by LAMBDA_PLASMA_ENTITY_HIT.
	 */
	private static void makeShredderSubmunition(StandardAmmoType original, StandardAmmoType sub) {
		sub.setProjectiles(original.projectileCount(), original.projectileCountMax()).setSpread(original.projectileSpread())
				.setGrav(original.projectileGravity()).setKnockback(original.knockbackMultiplier()).setSelfDamageDelay(original.selfDamageDelay())
				.setBlackPowder(original.blackPowder()).setSpectral(original.spectral()).setOnImpact(original.onImpact()).setOnUpdate(original.onUpdate());
		sub.setRicochetAngle(90).setRicochetCount(3).setVel(0.5F).setLife(50).setOnRicochet(LAMBDA_SHREDDER_RICOCHET).setOnEntityHit(LAMBDA_PLASMA_ENTITY_HIT);
	}

	/** BobMathUtil.getCrossAngle */
	private static double getCrossAngle(Vec3 vel, Vec3 rel) {
		vel = vel.normalize();
		rel = rel.normalize();
		double vecProd = rel.x * vel.x + rel.y * vel.y + rel.z * vel.z;
		double bot = rel.length() * vel.length();
		double angle = Math.acos(vecProd / bot) * 180 / Math.PI;
		if(angle >= 180) angle -= 180;
		return angle;
	}

	/** spawnPulse: "plasmablast" r 0.5, g 0.5, b 1.0, scale 0.75 within 100 blocks; block hits use the face orientation. */
	public static void spawnPulse(ServerLevel world, HitResult mop, float yaw, float pitch) {
		double x = mop.getLocation().x;
		double y = mop.getLocation().y;
		double z = mop.getLocation().z;

		if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {
			Direction side = bhr.getDirection();
			if(side == Direction.UP) { yaw = 0F; pitch = 0F; }
			if(side == Direction.DOWN) { yaw = 0F; pitch = 0F; }
			if(side == Direction.NORTH) { yaw = 0F; pitch = 90F; }
			if(side == Direction.SOUTH) { yaw = 180F; pitch = 90F; }
			if(side == Direction.EAST) { yaw = 90F; pitch = 90F; }
			if(side == Direction.WEST) { yaw = 270F; pitch = 90F; }

			x += side.getStepX() * 0.05;
			y += side.getStepY() * 0.05;
			z += side.getStepZ() * 0.05;
		}

		PacketDistributor.sendToPlayersNear(world, null, x, y, z, 100, new GunPlasmaPulsePayload(x, y, z, 0.5F, 0.5F, 1.0F, pitch, yaw, 0.75F));
	}

	/**
	 * makeShredderConfig: setBeam().setRenderRotations(false).setLife(5).setDamage(original.damageMult * original.projectilesMax)
	 * .setupDamageClass(LASER), the original's ammo item and casing, rendered with RENDER_LASER_CYAN. The beam itself deals no
	 * direct hit damage: block hits blast 0.75 blocks around and scatter the submunitions off the face, entity hits scatter
	 * them in random directions from the hit point.
	 */
	private static BeamBulletConfig makeShredderConfig(BulletConfig original, StandardAmmoType beamType, BulletConfig submunition) {
		BeamBulletConfig cfg = new BeamBulletConfig(beamType) {
			@Override public boolean matches(ItemStack stack) { return original.matches(stack); }
			@Override public ItemStack toStack() { return original.toStack(); }
		};
		cfg.setRenderRotations(false).setLife(5).setupDamageClass(DamageClass.LASER);
		cfg.setLaserColor(0x15, 0x15, 0x80);
		cfg.setCasingEffect(original.casingEffect);
		cfg.setOnBeamImpact((beam, mop) -> {
			if(!(beam.level() instanceof ServerLevel level)) return;

			int projectiles = submunition.projectilesMin();
			if(submunition.projectilesMax() > submunition.projectilesMin()) projectiles += level.random.nextInt(submunition.projectilesMax() - submunition.projectilesMin() + 1);

			if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {

				Direction dir = bhr.getDirection();
				Vec3 hitVec = mop.getLocation().add(dir.getStepX() * 0.1, dir.getStepY() * 0.1, dir.getStepZ() * 0.1);
				BlockHitResult moved = new BlockHitResult(hitVec, dir, bhr.getBlockPos(), bhr.isInside());

				spawnPulse(level, moved, beam.getYRot(), beam.getXRot());

				List<Entity> blast = level.getEntities(beam, new AABB(hitVec, hitVec).inflate(0.75, 0.75, 0.75));
				DamageSource source = BeamBulletConfig.getDamage(beam, beam.getThrower(), DamageClass.LASER);

				for(Entity e : blast) {
					if(!e.isAlive()) continue;
					if(e instanceof LivingEntity living) {
						BeamBulletConfig.attackEntityFromNT(living, source, DamageClass.LASER, beam.damage, 0D, 0F, 0F);
					} else {
						e.hurt(source, beam.damage);
					}
				}

				for(int i = 0; i < projectiles; i++) {
					LegacyBulletEntity bullet = new LegacyBulletEntity(level, hitVec.x, hitVec.y, hitVec.z, new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()),
							submunition.ammo, beam.damage, beam.thrower, submunition.spread() + 0.2F);
					level.addFreshEntity(bullet);
				}
			}

			if(mop instanceof EntityHitResult ehr) {

				spawnPulse(level, mop, beam.getYRot(), beam.getXRot());
				Vec3 hitVec = ehr.getLocation();

				for(int i = 0; i < projectiles; i++) {
					Vec3 vec = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).normalize();
					LegacyBulletEntity bullet = new LegacyBulletEntity(level, hitVec.x, hitVec.y, hitVec.z, vec,
							submunition.ammo, beam.damage, beam.thrower, submunition.spread() + 0.2F);
					level.addFreshEntity(bullet);
				}
			}
		});
		return cfg;
	}

	static {
		StandardAmmoType.G12_EQUESTRIAN_BJ.setOnImpact(LAMBDA_BOAT);
		makeShredderSubmunition(StandardAmmoType.G12, StandardAmmoType.G12_SUB);
		makeShredderSubmunition(StandardAmmoType.G12_SLUG, StandardAmmoType.G12_SUB_SLUG);
		makeShredderSubmunition(StandardAmmoType.G12_FLECHETTE, StandardAmmoType.G12_SUB_FLECHETTE);
		makeShredderSubmunition(StandardAmmoType.G12_MAGNUM, StandardAmmoType.G12_SUB_MAGNUM);
		makeShredderSubmunition(StandardAmmoType.G12_EXPLOSIVE, StandardAmmoType.G12_SUB_EXPLOSIVE);
		makeShredderSubmunition(StandardAmmoType.G12_PHOSPHORUS, StandardAmmoType.G12_SUB_PHOSPHORUS);
	}

	/** g12_equestrian_bj: setItem(EnumAmmoSecret.G12_EQUESTRIAN), setDamage(0F), LAMBDA_BOAT; SpentCasing "12gaEquestrianBJ" (same colors as TKR). */
	public static final BulletConfig g12_equestrian_bj = new BulletConfig(StandardAmmoType.G12_EQUESTRIAN_BJ) {
		@Override
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && stack.is(HbmItems.AMMO_SECRET.get())
					&& stack.getItem() instanceof LegacyVariantItem variant && "g12_equestrian".equals(variant.variant(stack).id());
		}

		@Override
		public ItemStack toStack() {
			return LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "g12_equestrian");
		}
	}.setCasingEffect(OrchestrasC4.CASING_G12_EQUESTRIAN);

	public static final BulletConfig g12_sub = new BulletConfig(StandardAmmoType.G12_SUB).setCasingEffect(OrchestrasC4.CASING_G12);
	public static final BulletConfig g12_sub_slug = new BulletConfig(StandardAmmoType.G12_SUB_SLUG).setCasingEffect(OrchestrasC4.CASING_G12_SLUG);
	public static final BulletConfig g12_sub_flechette = new BulletConfig(StandardAmmoType.G12_SUB_FLECHETTE).setCasingEffect(OrchestrasC4.CASING_G12_FLECHETTE);
	public static final BulletConfig g12_sub_magnum = new BulletConfig(StandardAmmoType.G12_SUB_MAGNUM).setCasingEffect(OrchestrasC4.CASING_G12_MAGNUM);
	public static final BulletConfig g12_sub_explosive = new BulletConfig(StandardAmmoType.G12_SUB_EXPLOSIVE).setCasingEffect(OrchestrasC4.CASING_G12_EXPLOSIVE);
	public static final BulletConfig g12_sub_phosphorus = new BulletConfig(StandardAmmoType.G12_SUB_PHOSPHORUS).setCasingEffect(OrchestrasC4.CASING_G12_PHOSPHORUS);
	public static final BeamBulletConfig g12_shredder = makeShredderConfig(g12, StandardAmmoType.G12_SHREDDER, g12_sub);
	public static final BeamBulletConfig g12_shredder_slug = makeShredderConfig(g12_slug, StandardAmmoType.G12_SHREDDER_SLUG, g12_sub_slug);
	public static final BeamBulletConfig g12_shredder_flechette = makeShredderConfig(g12_flechette, StandardAmmoType.G12_SHREDDER_FLECHETTE, g12_sub_flechette);
	public static final BeamBulletConfig g12_shredder_magnum = makeShredderConfig(g12_magnum, StandardAmmoType.G12_SHREDDER_MAGNUM, g12_sub_magnum);
	public static final BeamBulletConfig g12_shredder_explosive = makeShredderConfig(g12_explosive, StandardAmmoType.G12_SHREDDER_EXPLOSIVE, g12_sub_explosive);
	public static final BeamBulletConfig g12_shredder_phosphorus = makeShredderConfig(g12_phosphorus, StandardAmmoType.G12_SHREDDER_PHOSPHORUS, g12_sub_phosphorus);

	public static ItemGunBaseNT createSpas12(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(600).draw(20).inspect(39).reloadSequential(true).reloadChangeType(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::spasDamage).spreadHipfire(0F).delay(20).reload(5, 10, 10, 10, 0).jam(36).sound(HbmSoundEvents.GUN_SPAS_FIRE, 1.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 8).addConfigs(all))
						.offset(0.75, -0.0625, -0.1875)
						.setupStandardFire().recoil(LAMBDA_RECOIL_MARESLEG))
				.setupStandardConfiguration().ps(LAMBDA_SPAS_SECONDARY).pt(null)
				.anim(LAMBDA_SPAS_ANIMS).orchestra(OrchestrasD1.ORCHESTRA_SPAS)
				).setDefaultAmmo(StandardAmmoType.G12, 16);
	}

	public static ItemGunBaseNT createAutoshotgun(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.A_SIDE, new GunConfig()
				.dura(2_000).draw(10).inspect(33).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::autoshotgunDamage).delay(10).auto(true).autoAfterDry(true).dryfireAfterAuto(true).reload(44).jam(19).sound(HbmSoundEvents.GUN_SHREDDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 20).addConfigs(all))
						.offset(0.75, -0.125, -0.25)
						.setupStandardFire().recoil(LAMBDA_RECOIL_AUTOSHOTGUN))
				.setupStandardConfiguration()
				.anim(LAMBDA_SHREDDER_ANIMS).orchestra(OrchestrasD1.ORCHESTRA_SHREDDER)
				).setDefaultAmmo(StandardAmmoType.G12, 20);
	}

	public static ItemGunBaseNT createAutoshotgunShredder(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.B_SIDE, new GunConfig()
				.dura(2_000).draw(10).inspect(33).reloadSequential(true).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::shredderDamage).delay(10).auto(true).autoAfterDry(true).dryfireAfterAuto(true).reload(44).jam(19).sound(HbmSoundEvents.GUN_SHREDDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineBelt().addConfigs(g12_shredder, g12_shredder_slug, g12_shredder_flechette, g12_shredder_magnum, g12_shredder_explosive, g12_shredder_phosphorus))
						.offset(0.75, -0.125, -0.25)
						.setupStandardFire().recoil(LAMBDA_RECOIL_AUTOSHOTGUN))
				.setupStandardConfiguration()
				.anim(LAMBDA_SHREDDER_ANIMS).orchestra(OrchestrasD1.ORCHESTRA_SHREDDER)
				).setDefaultAmmo(StandardAmmoType.G12, 20);
	}

	public static ItemGunBaseNT createAutoshotgunSexy(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.LEGENDARY, new GunConfig()
				.dura(5_000).draw(20).inspect(65).reloadSequential(true).inspectCancel(false).crosshair(Crosshair.L_CIRCLE).hideCrosshair(false).smoke(Lego.LAMBDA_STANDARD_SMOKE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::sexyDamage).delay(4).auto(true).dryfireAfterAuto(true).reload(110).jam(19).sound(HbmSoundEvents.GUN_SHREDDER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 100).addConfigs(g12_equestrian_bj, g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum, g12_explosive, g12_phosphorus))
						.offset(0.75, -0.125, -0.25)
						.setupStandardFire().recoil(LAMBDA_RECOIL_SEXY))
				.setupStandardConfiguration()
				.anim(LAMBDA_SEXY_ANIMS).orchestra(OrchestrasD1.ORCHESTRA_SHREDDER_SEXY)
				).setDefaultAmmo(StandardAmmoType.G12_MAGNUM, 50);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_AUTOSHOTGUN = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5) + 1.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_SEXY = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
	};

	/** LAMBDA_SPAS_SECONDARY: fires every remaining round of the cycle at once (pitch * 0.9 for a double shot), always a 20 tick cooldown. */
	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SPAS_SECONDARY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		Player player = ctx.getPlayer();
		Receiver rec = ctx.config.getReceivers(stack)[0];
		int index = ctx.configIndex;
		GunState state = ItemGunBaseNT.getState(stack, index);
		if(state == GunState.IDLE) {
			if(rec.getCanFire(stack).apply(stack, ctx)) {
				rec.getOnFire(stack).accept(stack, ctx);
				int remaining = rec.getRoundsPerCycle(stack);
				int timeFired = 1;
				for(int i = 0; i < remaining; i++) {
					if(rec.getCanFire(stack).apply(stack, ctx)) {
						rec.getOnFire(stack).accept(stack, ctx);
						timeFired++;
					}
				}
				if(rec.getFireSound(stack) != null) GunEffects.playSound(entity, rec.getFireSound(stack).get(), rec.getFireVolume(stack), rec.getFirePitch(stack) * (timeFired > 1 ? 0.9F : 1F));
				ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
				ItemGunBaseNT.setTimer(stack, index, 20);
			} else {
				if(rec.getDoesDryFire(stack)) {
					ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
					ItemGunBaseNT.setState(stack, index, GunState.DRAWING);
					ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
				}
			}
		}
		if(state == GunState.RELOADING) {
			ItemGunBaseNT.setReloadCancel(stack, true);
		}
	};

	/** ResourceManager.spas_12_anim (models/weapons/animations/spas12.json), loaded on first client use. */
	private static HashMap<String, BusAnimation> spas_12_anim;

	public static HashMap<String, BusAnimation> spas12Anim() {
		if(spas_12_anim == null) spas_12_anim = AnimationLoader.load(AtomicLegacy.id("models/weapons/animations/spas12.json"));
		return spas_12_anim;
	}

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_SPAS_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
		case CYCLE: return spas12Anim().get("Fire");
		case CYCLE_DRY: return spas12Anim().get("FireDry");
		case ALT_CYCLE: return spas12Anim().get("FireAlt");
		case RELOAD:
			// MagazineSingleTypeBase#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
			boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) <= 0;
			return spas12Anim().get(empty ? "ReloadEmptyStart" : "ReloadStart");
		case RELOAD_CYCLE: return spas12Anim().get("Reload");
		case RELOAD_END: return spas12Anim().get("ReloadEnd");
		case JAMMED: return spas12Anim().get("Jammed");
		case INSPECT: return spas12Anim().get("Inspect");
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_SHREDDER_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
				.addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0, 18, 100));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 150).addPos(0, 0, 18, 100));
		case RELOAD: return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence().addPos(0, -8, 0, 250, IType.SIN_UP).addPos(0, -8, 0, 1000).addPos(0, 0, 0, 300))
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(-25, 0, 0, 300, IType.SIN_FULL).addPos(-25, 0, 0, 500).addPos(-27, 0, 0, 100, IType.SIN_DOWN).addPos(-25, 0, 0, 100, IType.SIN_FULL).addPos(-25, 0, 0, 150).addPos(0, 0, 0, 300, IType.SIN_FULL));
		case JAMMED: return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, -2, 0, 150, IType.SIN_UP).addPos(0, 0, 0, 100))
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
		case INSPECT: return new BusAnimation()
				.addBus("MAG", new BusAnimationSequence()
						.addPos(0, -1, 0, 150).addPos(6, -1, 0, 150).addPos(6, 12, 0, 350, IType.SIN_DOWN).addPos(6, -2, 0, 350, IType.SIN_UP).addPos(6, -1, 0, 50)
						.addPos(6, -1, 0, 100).addPos(0, -1, 0, 150, IType.SIN_FULL).addPos(0, 0, 0, 150, IType.SIN_UP))
				.addBus("SPEEN", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(360, 0, 0, 700))
				.addBus("LIFT", new BusAnimationSequence().addPos(0, 0, 0, 1450).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
		}

		return null;
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_SEXY_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(45, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
		case CYCLE:
			int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
			return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().hold(50).addPos(0, 0, -0.25, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
				.addBus("BARREL", new BusAnimationSequence().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150))
				.addBus("CYCLE", new BusAnimationSequence().addPos(1, 0, 0, 150))
				.addBus("HOOD", new BusAnimationSequence().hold(50).addPos(3, 0, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 50, IType.SIN_UP))
				.addBus("SHELLS", new BusAnimationSequence().setPos(amount - 1, 0, 0));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 18, 50));
		case RELOAD: return new BusAnimation()
				.addBus("LOWER", new BusAnimationSequence().addPos(15, 0, 0, 500, IType.SIN_FULL).hold(2750).addPos(12, 0, 0, 100, IType.SIN_DOWN).addPos(15, 0, 0, 100, IType.SIN_FULL).hold(1050).addPos(18, 0, 0, 100, IType.SIN_DOWN).addPos(15, 0, 0, 100, IType.SIN_FULL).hold(300).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("LEVER", new BusAnimationSequence().addPos(0, 0, 1, 150).hold(4700).addPos(0, 0, 0, 150))
				.addBus("HOOD", new BusAnimationSequence().hold(250).addPos(60, 0, 0, 500, IType.SIN_FULL).hold(3250).addPos(0, 0, 0, 500, IType.SIN_UP))
				.addBus("BELT", new BusAnimationSequence().setPos(1, 0, 0).hold(750).addPos(0, 0, 0, 500, IType.SIN_UP).hold(2000).addPos(1, 0, 0, 500, IType.SIN_UP))
				.addBus("MAG", new BusAnimationSequence().hold(1500).addPos(0, -1, 0, 250, IType.SIN_UP).addPos(2, -1, 0, 500, IType.SIN_UP).addPos(7, 1, 0, 250, IType.SIN_UP).addPos(15, 2, 0, 250).setPos(0, -2, 0).addPos(0, 0, 0, 500, IType.SIN_UP))
				.addBus("MAGROT", new BusAnimationSequence().hold(2250).addPos(0, 0, -180, 500, IType.SIN_FULL).setPos(0, 0, 0));
		case INSPECT: return new BusAnimation()
				.addBus("BOTTLE", new BusAnimationSequence().setPos(8, -8, -2).addPos(6, -4, -2, 500, IType.SIN_DOWN).addPos(3, -3, -5, 500, IType.SIN_FULL).addPos(3, -2, -5, 1000).addPos(4, -6, -2, 750, IType.SIN_FULL).addPos(6, -8, -2, 500, IType.SIN_UP))
				.addBus("SIP", new BusAnimationSequence().setPos(25, 0, 0).hold(500).addPos(-90, 0, 0, 500, IType.SIN_FULL).addPos(-110, 0, 0, 1000).addPos(25, 0, 0, 750, IType.SIN_FULL));
		}

		return null;
	};
}

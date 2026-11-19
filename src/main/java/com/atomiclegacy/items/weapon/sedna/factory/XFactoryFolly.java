package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig.DamageClass;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineSingleReload;
import com.atomiclegacy.network.GunPlasmaPulsePayload;
import com.atomiclegacy.radiation.RadiationEvents;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * [weapons batch F4] 1.7.10 XFactoryFolly: the ammo_secret rounds folly_sm (SUBATOMIC beam) and folly_nuke, and gun_folly
 * (WeaponQuality.SECRET, no creative tab). GUN_FOLLY_DAMAGE (1000, "no config here lol") is exposed as HbmConfig.follyDamage.
 *
 * <p>Deviations: folly_nuke.setChunkloading() has no equivalent (LegacyBulletEntity does not chunkload); the LAMBDA_SM_UPDATE
 * block erasure is limited to the world's build height instead of the fixed 1.7.10 range (0;256); the MK5 explosion and the
 * torex come from NukeExplosionManager.scheduleMk5Nuclear (statFac + statFacStandard of the original).
 */
public class XFactoryFolly {

	/** BulletConfig.setItem(EnumAmmoSecret.X) for a beam config: matches ammo_secret:&lt;variant&gt;. */
	public static class SecretBeamBulletConfig extends BeamBulletConfig {
		public final String secretVariant;

		public SecretBeamBulletConfig(StandardAmmoType ammo, String secretVariant) {
			super(ammo);
			this.secretVariant = secretVariant;
		}

		@Override
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && stack.is(HbmItems.AMMO_SECRET.get())
					&& ((LegacyVariantItem) HbmItems.AMMO_SECRET.get()).variant(stack).id().equals(this.secretVariant);
		}

		@Override
		public ItemStack toStack() {
			return LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), this.secretVariant);
		}
	}

	/** EntityBulletBaseMK4#beam heading normalized (Vec3NT#normalizeSelf, double precision). */
	private static Vec3 heading(LegacyBulletBeamEntity beam) {
		double len = Math.sqrt(beam.headingX * beam.headingX + beam.headingY * beam.headingY + beam.headingZ * beam.headingZ);
		return len < 1.0E-4D ? Vec3.ZERO : new Vec3(beam.headingX / len, beam.headingY / len, beam.headingZ / len);
	}

	public static final Consumer<LegacyBulletBeamEntity> LAMBDA_SM_UPDATE = (beam) -> {
		if(beam.level().isClientSide) return;
		Vec3 dir = heading(beam);

		if(beam.tickCount < 50) {
			double spacing = 10;
			double dist = beam.tickCount * spacing;
			// AuxParticlePacketNT {type: plasmablast, r/g/b 0.75, pitch + 90, -yaw, scale} within 250 blocks of the beam origin
			float scale = 2F + beam.tickCount / (float) (beam.beamLength / spacing) * 3F;
			PacketDistributor.sendToPlayersNear((ServerLevel) beam.level(), null, beam.getX(), beam.getY(), beam.getZ(), 250,
					new GunPlasmaPulsePayload(beam.getX() + dir.x * dist, beam.getY() + dir.y * dist, beam.getZ() + dir.z * dist,
							0.75F, 0.75F, 0.75F, beam.getXRot() + 90, -beam.getYRot(), scale));
		}

		if(beam.tickCount != 2) return;

		if(beam.thrower != null) RadiationEvents.contaminateCreative(beam.thrower, 150F);

		Level world = beam.level();
		List<Entity> entities = world.getEntities(beam, beam.getBoundingBox().expandTowards(beam.headingX, beam.headingY, beam.headingZ).inflate(1.0D, 1.0D, 1.0D));
		DamageSource source = BeamBulletConfig.getDamage(beam, beam.thrower, beam.config.dmgClass);

		for(int i = 1; i < beam.beamLength; i += 2) {
			int x = Mth.floor(beam.getX() + dir.x * i);
			int y = Mth.floor(beam.getY() + dir.y * i);
			int z = Mth.floor(beam.getZ() + dir.z * i);

			for(int ix = x - 1; ix <= x + 1; ix++) for(int iy = y - 1; iy <= y + 1; iy++) for(int iz = z - 1; iz <= z + 1; iz++) {
				// 1.7.10: iy > 0 && iy < 256 (above the bedrock floor, inside the world)
				if(iy > world.getMinBuildHeight() && iy < world.getMaxBuildHeight()) world.setBlock(new BlockPos(ix, iy, iz), Blocks.AIR.defaultBlockState(), 3);
				AABB aabb = new AABB(ix - 1, iy - 1, iz - 1, ix + 2, iy + 2, iz + 2);
				for(Entity e : entities) if(e != beam.thrower && e.getBoundingBox().intersects(aabb)) {
					if(e instanceof LivingEntity living) BeamBulletConfig.attackEntityFromNT(living, source, beam.config.dmgClass, beam.damage, 0D, 1_000F, 1F);
					else LegacyProjectileUtil.hurtNoIFrame(e, source, beam.damage);
				}
			}
		}
	};

	public static final BulletImpact LAMBDA_NUKE_IMPACT = (bullet, mop) -> {
		if(mop instanceof EntityHitResult && bullet.tickCount < 2) return false;
		if(bullet.isRemoved()) return false;
		bullet.discard();
		Vec3 hit = mop.getLocation();
		// EntityNukeExplosionMK5.statFac(world, 100, hit) + EntityNukeTorex.statFacStandard(world, hit, 100)
		if(bullet.level() instanceof ServerLevel level) NukeExplosionManager.scheduleMk5Nuclear(level, hit.x, hit.y, hit.z, 100);
		return true;
	};

	public static final BeamBulletConfig folly_sm;
	public static final BulletConfig folly_nuke;

	static {
		StandardAmmoType.FOLLY_SM.setLife(100).setVel(2D).setGrav(0.015D).setSpectral(true);
		folly_sm = new SecretBeamBulletConfig(StandardAmmoType.FOLLY_SM, "folly_sm").setupDamageClass(DamageClass.SUBATOMIC).setLife(100).setRenderRotations(false)
				.setSpectral(true).setDoesPenetrate(true).setOnUpdateBeam(LAMBDA_SM_UPDATE).setBeamRenderer(BeamBulletConfig.RENDER_FOLLY);
		// setChunkloading(): not available in the port (see the class comment)
		StandardAmmoType.FOLLY_NUKE.setLife(600).setVel(4D).setGrav(0.015D).setOnImpact(LAMBDA_NUKE_IMPACT);
		folly_nuke = new XFactory44.SecretBulletConfig(StandardAmmoType.FOLLY_NUKE, "folly_nuke");
	}

	public static ItemGunBaseNT createFolly(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.SECRET, new GunConfig()
				.dura(0).draw(40).crosshair(Crosshair.NONE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::follyDamage).delay(26).dryfire(false).reload(160).jam(0).sound(HbmSoundEvents.GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS, 100.0F, 1.0F)
						.mag(new MagazineSingleReload(0, 1).addConfigs(folly_sm, folly_nuke))
						.offset(0.75, -0.0625, -0.1875D).offsetScoped(0.75, -0.0625, -0.125D)
						.canFire(LAMBDA_CAN_FIRE).fire(LAMBDA_FIRE).recoil(LAMBDA_RECOIL_FOLLY))
				.setupStandardConfiguration().pt(LAMBDA_TOGGLE_AIM)
				.anim(LAMBDA_FOLLY_ANIMS).orchestra(OrchestrasF4.ORCHESTRA_FOLLY)
				);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM = (stack, ctx) -> {
		if(ItemGunBaseNT.getState(stack, ctx.configIndex) == GunState.IDLE) {
			boolean wasAiming = ItemGunBaseNT.getIsAiming(stack);
			ItemGunBaseNT.setIsAiming(stack, !wasAiming);
			if(!wasAiming) ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.SPINUP, ctx.configIndex);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) -> {
		Lego.doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
	};

	public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE = (stack, ctx) -> {
		if(ctx.entity instanceof Player) {
			if(!ItemGunBaseNT.getIsAiming(stack)) return false;
			if(ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) != GunAnimation.SPINUP) return false;
			if(ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex) < 100) return false;
		}
		return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0;
	};

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_FOLLY = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(25, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FOLLY_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-60, 0, 0, 0).addPos(5, 0, 0, 1500, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -4.5, 50).addPos(0, 0, -4.5, 500).addPos(0, 0, 0, 500, IType.SIN_UP))
				.addBus("LOAD", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(-25, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 1000, IType.SIN_FULL));
		case RELOAD: return new BusAnimation()
				.addBus("LOAD", new BusAnimationSequence().addPos(60, 0, 0, 1000, IType.SIN_FULL).addPos(60, 0, 0, 6000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
				.addBus("SCREW", new BusAnimationSequence().addPos(0, 0, 0, 1000).addPos(0, 0, -135, 1000, IType.SIN_FULL).addPos(0, 0, -135, 4000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
				.addBus("BREECH", new BusAnimationSequence().addPos(0, 0, 0, 1000).addPos(0, 0, -0.5, 1000, IType.SIN_FULL).addPos(0, -4, -0.5, 1000, IType.SIN_FULL).addPos(0, -4, -0.5, 2000).addPos(0, 0, -0.5, 1000, IType.SIN_FULL).addPos(0, 0, 0, 1000, IType.SIN_FULL))
				.addBus("SHELL", new BusAnimationSequence().addPos(0, -4, -4.5, 0).addPos(0, -4, -4.5, 3000).addPos(0, 0, -4.5, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
		}

		return null;
	};
}

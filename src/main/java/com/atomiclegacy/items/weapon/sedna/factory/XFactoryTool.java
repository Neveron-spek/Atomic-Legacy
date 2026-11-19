package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LayeringBlock;
import com.atomiclegacy.blocks.SandMixBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.StandardAmmoItem.BulletImpact;
import com.atomiclegacy.items.StandardAmmoItem.BulletUpdate;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.BulletConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/**
 * [weapons batch F3] 1.7.10 XFactoryTool: gun_fireext (fext_water / fext_foam / fext_sand) and gun_charge_thrower
 * (ct_hook / ct_mortar / ct_mortar_charge).
 *
 * <p>Adaptations: the fext_* "vanillaExt blockdust" client particles run through the port's client update hook
 * (StandardAmmoType.onClientUpdate, GunBatchF3ClientEffects). IRepairable.tryExtinguish of burning machine cores
 * (CompatExternal.getCoreFromPos) is not ported, no machine of the port burns. 1.7.10 also ran the block lambdas
 * (LAMBDA_*_HIT) for entity hits with the uninitialised block coordinates (0, 0, 0) of the entity hit; the port
 * keeps only their net effect on the bullet (water dies on an entity hit, foam and sand fly on).
 */
public class XFactoryTool {

	/** WeaponModScope texture for UTILITY guns; the scope mod itself is not ported to these guns. */
	public static final ResourceLocation scope = AtomicLegacy.id("textures/misc/scope_tool.png");

	/** BulletConfig.setItem(new ItemStack(ModItems.ammo_fireext, 1, meta)): matches the ammo_fireext variant. */
	public static class FireExtBulletConfig extends BulletConfig {
		public final String variant;

		public FireExtBulletConfig(StandardAmmoType ammo, String variant) {
			super(ammo);
			this.variant = variant;
		}

		@Override
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && stack.is(HbmItems.AMMO_FIREEXT.get())
					&& ((LegacyVariantItem) HbmItems.AMMO_FIREEXT.get()).variant(stack).id().equals(this.variant);
		}

		@Override
		public ItemStack toStack() {
			return LegacyVariantItem.stackFor(HbmItems.AMMO_FIREEXT.get(), this.variant);
		}
	}

	/** LAMBDA_WATER_HIT (onRicochet): fire, foam layers and foam blocks in the 3x3x3 around the hit become air, hiss, the bullet dies. */
	public static BulletImpact LAMBDA_WATER_HIT = (bullet, mop) -> {
		if(!(bullet.level() instanceof ServerLevel level) || !(mop instanceof BlockHitResult bhr)) return false;
		BlockPos hit = bhr.getBlockPos();
		boolean fizz = false;
		for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) for(int k = -1; k <= 1; k++) {
			BlockPos pos = hit.offset(i, j, k);
			BlockState block = level.getBlockState(pos);
			if(block.is(Blocks.FIRE) || block.is(HbmBlocks.FOAM_LAYER.get()) || block.is(HbmBlocks.BLOCK_FOAM.get())) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				fizz = true;
			}
		}
		if(fizz) hiss(level, bullet);
		return true;
	};

	/** LAMBDA_WATER_UPDATE server half: a volcanic lava source block at the bullet position becomes obsidian and the bullet dies. */
	public static BulletUpdate LAMBDA_WATER_UPDATE = (bullet) -> {
		if(!(bullet.level() instanceof ServerLevel level)) return;
		BlockPos pos = new BlockPos(Mth.floor(bullet.getX()), Mth.floor(bullet.getY()), Mth.floor(bullet.getZ()));
		BlockState state = level.getBlockState(pos);
		if(state.is(HbmBlocks.VOLCANIC_LAVA_BLOCK.get()) && state.hasProperty(LiquidBlock.LEVEL) && state.getValue(LiquidBlock.LEVEL) == 0) {
			level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
			bullet.discard();
		}
	};

	/** LAMBDA_FOAM_HIT (onRicochet): fire material in the 3x3x3 becomes air; half the time offset to the hit side; lay / grow / solidify foam. */
	public static BulletImpact LAMBDA_FOAM_HIT = (bullet, mop) -> {
		if(!(bullet.level() instanceof ServerLevel level) || !(mop instanceof BlockHitResult bhr)) return false;
		BlockPos hit = bhr.getBlockPos();
		boolean fizz = false;
		for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) for(int k = -1; k <= 1; k++) {
			BlockPos pos = hit.offset(i, j, k);
			if(level.getBlockState(pos).is(BlockTags.FIRE)) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				fizz = true;
			}
		}
		BlockPos pos = hit;
		if(level.random.nextBoolean()) pos = pos.relative(bhr.getDirection());
		BlockState b = level.getBlockState(pos);
		if(b.canBeReplaced() && HbmBlocks.FOAM_LAYER.get().defaultBlockState().canSurvive(level, pos)) {
			if(!b.is(HbmBlocks.FOAM_LAYER.get())) {
				level.setBlock(pos, HbmBlocks.FOAM_LAYER.get().defaultBlockState(), 3);
			} else {
				int layers = b.getValue(LayeringBlock.LAYERS); // meta + 1
				if(layers < 7) level.setBlock(pos, b.setValue(LayeringBlock.LAYERS, layers + 1), 3);
				else level.setBlock(pos, HbmBlocks.BLOCK_FOAM.get().defaultBlockState(), 3);
			}
		}
		if(fizz) hiss(level, bullet);
		return false;
	};

	/** LAMBDA_SAND_HIT (onRicochet): half the time offset to the hit side; lay / grow boron sand layers, then a boron sand mix block. */
	public static BulletImpact LAMBDA_SAND_HIT = (bullet, mop) -> {
		if(!(bullet.level() instanceof ServerLevel level) || !(mop instanceof BlockHitResult bhr)) return false;
		BlockPos pos = bhr.getBlockPos();
		if(level.random.nextBoolean()) pos = pos.relative(bhr.getDirection());
		BlockState b = level.getBlockState(pos);
		if((b.canBeReplaced() || b.is(HbmBlocks.SAND_BORON_LAYER.get())) && HbmBlocks.SAND_BORON_LAYER.get().defaultBlockState().canSurvive(level, pos)) {
			if(!b.is(HbmBlocks.SAND_BORON_LAYER.get())) {
				level.setBlock(pos, HbmBlocks.SAND_BORON_LAYER.get().defaultBlockState(), 3);
			} else {
				int layers = b.getValue(LayeringBlock.LAYERS);
				if(layers < 7) level.setBlock(pos, b.setValue(LayeringBlock.LAYERS, layers + 1), 3);
				else level.setBlock(pos, HbmBlocks.SAND_MIX.get().defaultBlockState().setValue(SandMixBlock.VARIANT, 0), 3); // EnumSandType.BORON
			}
			if(b.is(BlockTags.FIRE)) hiss(level, bullet);
		}
		return false;
	};

	/** World#playSoundEffect(NTMSounds.VANILLA_HISS ("random.fizz"), 1.0F, 1.5F + rand * 0.5F) */
	private static void hiss(ServerLevel level, LegacyBulletEntity bullet) {
		level.playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.5F + level.random.nextFloat() * 0.5F);
	}

	/** setOnEntityHit: target.entityHit.extinguish(); water also died in LAMBDA_WATER_HIT, foam and sand flew on. */
	public static BulletImpact LAMBDA_EXTINGUISH_STOP = (bullet, mop) -> {
		if(mop instanceof EntityHitResult ehr) ehr.getEntity().clearFire();
		return true;
	};
	public static BulletImpact LAMBDA_EXTINGUISH_PASS = (bullet, mop) -> {
		if(mop instanceof EntityHitResult ehr) ehr.getEntity().clearFire();
		return false;
	};

	/** LAMBDA_SET_HOOK (onUpdate, server half): the thrower's held charge thrower remembers the hook during its first two ticks. */
	public static BulletUpdate LAMBDA_SET_HOOK = (bullet) -> {
		if(bullet.tickCount < 2 && bullet.ownerEntity() instanceof Player player) {
			ItemStack held = player.getMainHandItem();
			if(held.is(HbmItems.GUN_CHARGE_THROWER.get())) {
				ItemGunChargeThrower.setLastHook(held, bullet.getId());
			}
		}
		bullet.ignoreFrustumCheck = true; // [parity S4: culling] ignoreFrustumCheck
	};

	/** LAMBDA_SET_HOOK client half: ignoreFrustumCheck = true. */
	public static BulletUpdate LAMBDA_SET_HOOK_CLIENT = (bullet) -> { bullet.ignoreFrustumCheck = true; }; // [parity S4: culling]

	/** LAMBDA_HOOK (onImpact): block hits move the hook 0.05 back along its motion and stick it in the block. */
	public static BulletImpact LAMBDA_HOOK = (bullet, mop) -> {
		if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {
			Vec3 motion = bullet.getDeltaMovement();
			Vec3 vec = new Vec3(-motion.x, -motion.y, -motion.z).normalize().scale(0.05);
			bullet.setPos(mop.getLocation().x + vec.x, mop.getLocation().y + vec.y, mop.getLocation().z + vec.z);
			bullet.getStuck(bhr.getBlockPos(), bhr.getDirection());
		}
		return false;
	};

	/**
	 * LAMBDA_MORTAR: ExplosionVNT(5) with BlockAllocatorBulkie(60, 8), BlockProcessorStandard,
	 * EntityProcessorCrossSmooth(1, damage).setupPiercing(...), PlayerProcessorStandard, ExplosionEffectWeapon(10, 2.5F, 1F).
	 */
	public static BulletImpact LAMBDA_MORTAR = (bullet, mop) -> {
		if(mop instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.ownerEntity()) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();
		float size = 5F;
		Explosion compat = new Explosion(level, bullet.ownerEntity(), hit.x, hit.y, hit.z, size, false, Explosion.BlockInteraction.DESTROY);
		Set<BlockPos> affected = allocateBulkie(level, compat, hit.x, hit.y, hit.z, size, 60D, 8);
		processEntities(level, bullet.ownerEntity(), hit.x, hit.y, hit.z, size, bullet.baseDamage(), bullet.ammoType());
		processBlocksStandard(level, compat, affected, size, true, null);
		LegacyProjectileUtil.sendSmallExplosionEffect(level, hit, 10, 2.5F, 1F);
		return true;
	};

	/**
	 * LAMBDA_MORTAR_CHARGE: ExplosionVNT(15) with BlockAllocatorStandard (16), BlockProcessorStandard.setNoDrop()
	 * .withBlockEffect(new BlockMutatorDebris(block_slag, 1)), the piercing entity processor, PlayerProcessorStandard and
	 * ExplosionCreator.composeEffectSmall half a block above the hit (before the explosion, like the original).
	 */
	public static BulletImpact LAMBDA_MORTAR_CHARGE = (bullet, mop) -> {
		if(mop instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.ownerEntity()) return false;
		if(!(bullet.level() instanceof ServerLevel level)) return true;
		Vec3 hit = mop.getLocation();
		float size = 15F;
		Explosion compat = new Explosion(level, bullet.ownerEntity(), hit.x, hit.y, hit.z, size, false, Explosion.BlockInteraction.DESTROY);
		LegacyProjectileUtil.composeExplosionEffect(level, new Vec3(hit.x, hit.y + 0.5, hit.z), 10, 2F, 0.5F, 25F, 5, 8, 20, 0.75F, 1F, -2F, 150);
		Set<BlockPos> affected = allocateStandard(level, compat, hit.x, hit.y, hit.z, size, 16);
		processEntities(level, bullet.ownerEntity(), hit.x, hit.y, hit.z, size, bullet.baseDamage(), bullet.ammoType());
		processBlocksStandard(level, compat, affected, size, false, HbmBlocks.BLOCK_SLAG.get().defaultBlockState());
		return true;
	};

	/** BlockAllocatorStandard(resolution): the vanilla explosion rays with the block resistance. */
	public static Set<BlockPos> allocateStandard(ServerLevel level, Explosion compat, double x, double y, double z, float size, int resolution) {
		Set<BlockPos> affectedBlocks = new HashSet<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for(int i = 0; i < resolution; ++i) {
			for(int j = 0; j < resolution; ++j) {
				for(int k = 0; k < resolution; ++k) {
					if(i == 0 || i == resolution - 1 || j == 0 || j == resolution - 1 || k == 0 || k == resolution - 1) {
						double d0 = (double) ((float) i / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d1 = (double) ((float) j / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d2 = (double) ((float) k / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= d3;
						d1 /= d3;
						d2 /= d3;
						float powerRemaining = size * (0.7F + level.random.nextFloat() * 0.6F);
						double currentX = x;
						double currentY = y;
						double currentZ = z;
						for(float stepSize = 0.3F; powerRemaining > 0.0F; powerRemaining -= stepSize * 0.75F) {
							cursor.set(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
							if(!level.isInWorldBounds(cursor)) break;
							BlockState state = level.getBlockState(cursor);
							if(!state.isAir()) {
								float blockResistance = state.getExplosionResistance(level, cursor, compat);
								powerRemaining -= (blockResistance + 0.3F) * stepSize;
							}
							if(powerRemaining > 0.0F) affectedBlocks.add(cursor.immutable());
							currentX += d0 * (double) stepSize;
							currentY += d1 * (double) stepSize;
							currentZ += d2 * (double) stepSize;
						}
					}
				}
			}
		}
		return affectedBlocks;
	}

	/** BlockAllocatorBulkie(maximum, resolution): rays of the full explosion size, stopped by blocks harder than the maximum. */
	public static Set<BlockPos> allocateBulkie(ServerLevel level, Explosion compat, double x, double y, double z, float size, double maximum, int resolution) {
		Set<BlockPos> affectedBlocks = new HashSet<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for(int i = 0; i < resolution; ++i) {
			for(int j = 0; j < resolution; ++j) {
				for(int k = 0; k < resolution; ++k) {
					if(i == 0 || i == resolution - 1 || j == 0 || j == resolution - 1 || k == 0 || k == resolution - 1) {
						double d0 = (double) ((float) i / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d1 = (double) ((float) j / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d2 = (double) ((float) k / ((float) resolution - 1.0F) * 2.0F - 1.0F);
						double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= d3;
						d1 /= d3;
						d2 /= d3;
						double currentX = x;
						double currentY = y;
						double currentZ = z;
						double dist = 0;
						for(float stepSize = 0.3F; dist <= size;) {
							double deltaX = currentX - x;
							double deltaY = currentY - y;
							double deltaZ = currentZ - z;
							dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
							cursor.set(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
							if(level.isInWorldBounds(cursor)) {
								BlockState state = level.getBlockState(cursor);
								if(!state.isAir()) {
									float blockResistance = state.getExplosionResistance(level, cursor, compat);
									if(maximum < blockResistance) break;
								}
								affectedBlocks.add(cursor.immutable());
							}
							currentX += d0 * (double) stepSize;
							currentY += d1 * (double) stepSize;
							currentZ += d2 * (double) stepSize;
						}
					}
				}
			}
		}
		return affectedBlocks;
	}

	/**
	 * BlockProcessorStandard: drops with chance 1 / size (or none with setNoDrop), onBlockExploded; then the optional
	 * BlockMutatorDebris post pass: every affected position that is air now and touches a normal cube other than the
	 * debris block becomes the debris block.
	 */
	public static void processBlocksStandard(ServerLevel level, Explosion compat, Set<BlockPos> affectedBlocks, float size, boolean drops, BlockState debris) {
		float dropChance = 1.0F / size;
		List<BlockPos> remaining = new java.util.ArrayList<>();
		for(BlockPos pos : affectedBlocks) {
			BlockState state = level.getBlockState(pos);
			if(state.isAir()) continue;
			remaining.add(pos);
			boolean canDrop = drops && state.canDropFromExplosion(level, pos, compat);
			state.onExplosionHit(level, pos, compat, (stack, dropPos) -> {
				if(canDrop && level.random.nextFloat() <= dropChance) Block.popResource(level, dropPos, stack);
			});
		}
		if(debris != null) {
			for(BlockPos pos : remaining) {
				if(!level.getBlockState(pos).isAir()) continue;
				for(Direction dir : Direction.values()) {
					BlockPos n = pos.relative(dir);
					BlockState b = level.getBlockState(n);
					if(b.isSolidRender(level, n) && !b.is(debris.getBlock())) {
						level.setBlock(pos, debris, 3);
						break;
					}
				}
			}
		}
	}

	/**
	 * EntityProcessorCrossSmooth(1, damage).setupPiercing(threshold negation, piercing) with setAllowSelfDamage (the
	 * exploder takes half) and PlayerProcessorStandard, like XFactory12ga.standardExplode without its weapon SFX.
	 */
	public static void processEntities(ServerLevel level, Entity exploder, double x, double y, double z, float range, float fixedDamage, StandardAmmoType ammo) {
		double nodeDist = 1D;
		double knockbackMult = 1D;
		float size = range * 2.0F;
		AABB area = new AABB(x - size - 1.0D, y - size - 1.0D, z - size - 1.0D, x + size + 1.0D, y + size + 1.0D, z + size + 1.0D);
		List<Entity> list = level.getEntities((Entity) null, area, entity -> true);
		Explosion compat = new Explosion(level, exploder, x, y, z, range, false, Explosion.BlockInteraction.KEEP);
		EventHooks.onExplosionDetonate(level, compat, list, size);

		Vec3[] nodes = new Vec3[7];
		Direction[] dirs = { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		for(int i = 0; i < 6; i++) nodes[i] = new Vec3(x + dirs[i].getStepX() * nodeDist, y + dirs[i].getStepY() * nodeDist, z + dirs[i].getStepZ() * nodeDist);
		nodes[6] = new Vec3(x, y, z); // ForgeDirection.UNKNOWN

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
	}

	static {
		// fext_*: setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04D).setSpread(0.025F / 0.05F / 0.05F)
		StandardAmmoType.FEXT_WATER.setLife(100).setVel(0.75D).setGrav(0.04D).setSpread(0.025F).setOnUpdate(LAMBDA_WATER_UPDATE).setOnEntityHit(LAMBDA_EXTINGUISH_STOP).setOnRicochet(LAMBDA_WATER_HIT);
		StandardAmmoType.FEXT_FOAM.setLife(100).setVel(0.75D).setGrav(0.04D).setSpread(0.05F).setOnEntityHit(LAMBDA_EXTINGUISH_PASS).setOnRicochet(LAMBDA_FOAM_HIT);
		StandardAmmoType.FEXT_SAND.setLife(100).setVel(0.75D).setGrav(0.04D).setSpread(0.05F).setOnEntityHit(LAMBDA_EXTINGUISH_PASS).setOnRicochet(LAMBDA_SAND_HIT);
		// ct_hook: setRenderRotations(false).setLife(6_000).setVel(3F).setGrav(0.035D).setDoesPenetrate(true).setDamageFalloffByPen(false)
		StandardAmmoType.CT_HOOK.setLife(6_000).setVel(3D).setGrav(0.035D).setOnUpdate(LAMBDA_SET_HOOK).setOnClientUpdate(LAMBDA_SET_HOOK_CLIENT).setOnImpact(LAMBDA_HOOK);
		// ct_mortar / ct_mortar_charge: setDamage(2.5F / 5F).setLife(200).setVel(3F).setGrav(0.035D)
		StandardAmmoType.CT_MORTAR.setLife(200).setVel(3D).setGrav(0.035D).setOnImpact(LAMBDA_MORTAR);
		StandardAmmoType.CT_MORTAR_CHARGE.setLife(200).setVel(3D).setGrav(0.035D).setOnImpact(LAMBDA_MORTAR_CHARGE);
	}

	public static final BulletConfig fext_water = new FireExtBulletConfig(StandardAmmoType.FEXT_WATER, "water").setReloadCount(300);
	public static final BulletConfig fext_foam = new FireExtBulletConfig(StandardAmmoType.FEXT_FOAM, "foam").setReloadCount(300);
	public static final BulletConfig fext_sand = new FireExtBulletConfig(StandardAmmoType.FEXT_SAND, "sand").setReloadCount(300);

	public static final BulletConfig ct_hook = new BulletConfig(StandardAmmoType.CT_HOOK);
	public static final BulletConfig ct_mortar = new BulletConfig(StandardAmmoType.CT_MORTAR);
	public static final BulletConfig ct_mortar_charge = new BulletConfig(StandardAmmoType.CT_MORTAR_CHARGE);

	public static ItemGunBaseNT createFireExt(Item.Properties properties) {
		return new ItemGunBaseNT(properties, WeaponQuality.UTILITY, new GunConfig()
				.dura(5_000).draw(10).inspect(55).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCLE)
				.rec(new Receiver(0)
						.dmg(HbmConfig::extinguisherDamage).delay(1).dry(0).auto(true).spread(0F).spreadHipfire(0F).reload(20).jam(0).sound(HbmSoundEvents.GUN_EXTINGUISHER_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 300).addConfigs(fext_water, fext_foam, fext_sand))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire())
				.setupStandardConfiguration()
				.orchestra(OrchestrasF3.ORCHESTRA_FIREEXT)
				);
	}

	public static ItemGunBaseNT createChargeThrower(Item.Properties properties) {
		return new ItemGunChargeThrower(properties, WeaponQuality.UTILITY, new GunConfig()
				.dura(3_000).draw(10).inspect(55).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
				.rec(new Receiver(0)
						.dmg(HbmConfig::chargeThrowerDamage).delay(4).dry(10).auto(true).spread(0F).spreadHipfire(0F).reload(60).jam(0).sound(HbmSoundEvents.GUN_CHARGE_FIRE, 1.0F, 1.0F)
						.mag(new MagazineFullReload(0, 1).addConfigs(ct_hook, ct_mortar, ct_mortar_charge))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.setupStandardFire().recoil(LAMBDA_RECOIL_CT))
				.setupStandardConfiguration()
				.anim(LAMBDA_CT_ANIMS).orchestra(OrchestrasF3.ORCHESTRA_CHARGE_THROWER)
				).setDefaultAmmo(StandardAmmoType.CT_MORTAR, 3);
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_CT = (stack, ctx) -> {
		ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CT_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		case CYCLE: return new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
		case RELOAD: return new BusAnimation()
				.addBus("RAISE", new BusAnimationSequence().addPos(-45, 0, 0, 500, IType.SIN_FULL).hold(2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("AMMO", new BusAnimationSequence().setPos(0, -10, -5).hold(500).addPos(0, 0, 5, 750, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP).hold(4000))
				.addBus("TWIST", new BusAnimationSequence().setPos(0, 0, 25).hold(2000).addPos(0, 0, 0, 150));
		case INSPECT: return new BusAnimation()
				.addBus("TURN", new BusAnimationSequence().addPos(0, 60, 0, 500, IType.SIN_FULL).hold(1750).addPos(0, 0, 0, 500, IType.SIN_FULL))
				.addBus("ROLL", new BusAnimationSequence().hold(750).addPos(0, 0, -90, 500, IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_FULL));
		}

		return null;
	};
}

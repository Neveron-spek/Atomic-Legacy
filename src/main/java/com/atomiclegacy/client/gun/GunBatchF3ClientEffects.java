package com.atomiclegacy.client.gun;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.SandMixBlock;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasC2;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF3;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryDrill;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.atomiclegacy.network.GunDrillParticleBurstPayload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * [weapons batch F3] Client pieces of XFactoryTool / XFactoryDrill: the "vanillaExt blockdust" particles of the extinguisher
 * rounds (ClientProxy.effectNT), ItemGunBaseNT.loopedSounds engine loop of the drill (ORCHESTRA_DRILL), the drill block
 * highlight (ModEventHandlerRenderer DrawBlockHighlightEvent -> XFactoryDrill.drawBlockHighlight), the ParticleBurstPacket
 * handler and the HbmPlayerProps key states / HbmAnimations hooks of the common code.
 */
public final class GunBatchF3ClientEffects {

	private GunBatchF3ClientEffects() {
	}

	/** Called from GunClientHandler.clientSetup with the client key state map. */
	public static void clientSetup(Predicate<GunKeybind> keyPressed) {
		ItemGunChargeThrower.CLIENT_KEY_PRESSED = (player, key) -> player == Minecraft.getInstance().player && keyPressed.test(key);
		XFactoryDrill.RELEVANT_TRANSFORMATION = HbmAnimations::getRelevantTransformation;
		OrchestrasF3.CLIENT_DRILL_LOOP = GunBatchF3ClientEffects::drillLoop;
		OrchestrasF3.CLIENT_DRILL_STOP = GunBatchF3ClientEffects::drillStop;
		// LAMBDA_WATER_UPDATE / LAMBDA_FOAM_UPDATE / LAMBDA_SAND_UPDATE client halves
		StandardAmmoType.FEXT_WATER.setOnClientUpdate(bullet -> blockDust(bullet, Blocks.WATER.defaultBlockState(), 0.05));
		StandardAmmoType.FEXT_FOAM.setOnClientUpdate(bullet -> blockDust(bullet, HbmBlocks.BLOCK_FOAM.get().defaultBlockState(), 0.1));
		StandardAmmoType.FEXT_SAND.setOnClientUpdate(bullet -> blockDust(bullet, HbmBlocks.SAND_MIX.get().defaultBlockState().setValue(SandMixBlock.VARIANT, 0), 0.1));
	}

	/**
	 * effectNT "vanillaExt" / "blockdust": EntityBlockDustFX at the bullet with motion (mX, mY + 0.2, mZ) where the lambdas pass
	 * motion + gaussian * deviation and motionY - 0.2 + gaussian * deviation, lifetime 10 + rand(20), colour 0.8 grey.
	 * The 1.7.10 motion vector is unscaled (length 1 before gravity); the port's delta movement is the displacement, so it is
	 * divided by the round's velocity.
	 */
	private static void blockDust(LegacyBulletEntity bullet, BlockState state, double deviation) {
		Minecraft mc = Minecraft.getInstance();
		if(!(bullet.level() instanceof ClientLevel level)) return;
		Vec3 motion = bullet.getDeltaMovement().scale(1D / bullet.ammoType().projectileSpeed());
		double mX = motion.x + level.random.nextGaussian() * deviation;
		double mY = motion.y - 0.2 + level.random.nextGaussian() * deviation;
		double mZ = motion.z + level.random.nextGaussian() * deviation;
		TerrainParticle fx = new TerrainParticle(level, bullet.getX(), bullet.getY(), bullet.getZ(), mX, mY + 0.2, mZ, state);
		fx.setParticleSpeed(mX, mY + 0.2, mZ);
		fx.setLifetime(10 + level.random.nextInt(20));
		fx.setColor(0.8F, 0.8F, 0.8F);
		mc.particleEngine.add(fx);
	}

	/** ParticleBurstPacket: EffectRenderer#addBlockDestroyEffects */
	public static void handleParticleBurst(GunDrillParticleBurstPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null) return;
		BlockState state = Block.stateById(payload.blockState());
		if(state.isAir()) return;
		mc.particleEngine.destroy(payload.pos(), state);
	}

	/* ===================== DRILL ENGINE LOOP ===================== */

	private static final Map<LivingEntity, EngineLoopSound> LOOPED_SOUNDS = new WeakHashMap<>();

	/** MainRegistry.proxy.getLoopedSound(ENGINE_LOOP, pos, volume = speed, range 15, pitch = speed, keepAlive 25), attached to the entity. */
	private static void drillLoop(LivingEntity entity, ItemStack stack) {
		double speed = HbmAnimations.getRelevantTransformation("SPEED")[0];
		EngineLoopSound runningAudio = LOOPED_SOUNDS.get(entity);
		Minecraft mc = Minecraft.getInstance();

		if(speed > 0) {
			//start sound
			if(runningAudio == null || !runningAudio.isPlaying()) {
				// [weapon mods G3: electric engine] electric ? TURBINE_LARGE_LOOP : ENGINE_LOOP
				boolean electric = com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager.ID_ENGINE_ELECTRIC);
				EngineLoopSound audio = new EngineLoopSound(electric ? HbmSoundEvents.LARGE_TURBINE_RUNNING.get() : HbmSoundEvents.ENGINE_LOOP.get(), entity, (float) speed, 15F, (float) speed, 25);
				LOOPED_SOUNDS.put(entity, audio);
				mc.getSoundManager().play(audio);
			}
			//keepalive
			if(runningAudio != null && runningAudio.isPlaying()) {
				runningAudio.keepAlive();
				runningAudio.updateVolume((float) speed);
				runningAudio.updatePitch((float) speed);
			}
		}
	}

	private static void drillStop(LivingEntity entity) {
		EngineLoopSound runningAudio = LOOPED_SOUNDS.get(entity);
		if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
	}

	/** AudioDynamic: looped, follows the entity, expires keepAlive ticks after the last keepAlive(), volume faded by distance over the range. */
	private static final class EngineLoopSound extends AbstractTickableSoundInstance {
		private final LivingEntity parent;
		private final float range;
		private final int keepAlive;
		private int timeSinceKA;
		private float maxVolume;
		private boolean playing = true;

		private EngineLoopSound(net.minecraft.sounds.SoundEvent sound, LivingEntity parent, float volume, float range, float pitch, int keepAlive) { // [weapon mods G3: electric engine]
			super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.parent = parent;
			this.range = range;
			this.keepAlive = keepAlive;
			this.maxVolume = volume;
			this.x = parent.getX();
			this.y = parent.getY();
			this.z = parent.getZ();
			this.looping = true;
			this.delay = 0;
			this.pitch = pitch;
			this.volume = volume;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			if(mc.level == null || mc.player == null || this.parent.isRemoved()) {
				stopSound();
				return;
			}
			this.x = this.parent.getX();
			this.y = this.parent.getY();
			this.z = this.parent.getZ();
			if(this.parent == mc.player) {
				this.volume = this.maxVolume;
			} else {
				double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
				this.volume = dist >= this.range ? 0F : (float) (this.maxVolume * (1D - dist / this.range));
			}
			if(this.timeSinceKA > this.keepAlive) {
				stopSound();
			}
			this.timeSinceKA++;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		boolean isPlaying() {
			return this.playing && !isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);
		}

		void keepAlive() { this.timeSinceKA = 0; }
		void updateVolume(float volume) { this.maxVolume = volume; }
		void updatePitch(float pitch) { this.pitch = pitch; }

		void stopSound() {
			this.playing = false;
			stop();
		}
	}

	/* ===================== DRILL BLOCK HIGHLIGHT ===================== */

	@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
	public static final class GameEvents {

		private GameEvents() {
		}

		/** ModEventHandlerRenderer: the drill cancels the vanilla block highlight and draws XFactoryDrill.drawBlockHighlight. */
		@SubscribeEvent
		public static void onHighlight(RenderHighlightEvent.Block event) {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;
			if(player == null) return;
			ItemStack held = player.getMainHandItem();
			if(!held.is(HbmItems.GUN_DRILL.get())) return;
			event.setCanceled(true);
			drawBlockHighlight(player, held, event.getDeltaTracker().getGameTimeDeltaPartialTick(false), event.getPoseStack(), event.getMultiBufferSource(), event.getCamera().getPosition());
		}
	}

	/**
	 * XFactoryDrill.drawBlockHighlight: an outlined 1x1x1 box (white with an AoE, dark red 0x800000 alone) on the aimed block,
	 * plus the dark red AoE cube; boxes expanded by 0.002, drawn relative to the interpolated player position (camera here).
	 */
	public static void drawBlockHighlight(Player player, ItemStack drill, float interp, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, Vec3 camera) {
		HitResult mop = OrchestrasC2.getMouseOver(player, XFactoryDrill.getModdableReach(drill, 5.0D));

		if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = bhr.getBlockPos();
			int aoe = player.isShiftKeyDown() ? 0 : XFactoryDrill.getModdableAoE(drill, 1);

			float exp = 0.002F;
			VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
			pose.pushPose();
			pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			AABB aabb = new AABB(0, 0, 0, 1, 1, 1).inflate(exp, exp, exp);
			int color = aoe > 0 ? -1 : 0x800000;
			LevelRenderer.renderLineBox(pose, consumer, aabb, ((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);

			if(aoe > 0) {
				aabb = new AABB(-aoe, -aoe, -aoe, 1 + aoe, 1 + aoe, 1 + aoe).inflate(exp, exp, exp);
				LevelRenderer.renderLineBox(pose, consumer, aabb, 0x80 / 255F, 0F, 0F, 1F);
			}
			pose.popPose();
		}
	}
}

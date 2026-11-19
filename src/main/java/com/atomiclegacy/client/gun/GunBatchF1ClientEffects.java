package com.atomiclegacy.client.gun;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyChemicalEntity;
import com.atomiclegacy.entity.LegacyChemicalEntity.ChemicalStyle;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF1;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * [weapons batch F1] Client side of the flamethrower family: ItemGunBaseNT.loopedSounds with the AudioDynamic of
 * MainRegistry.proxy.getLoopedSound(GUN_FLAMER_LOOP, pos, volume 1, range 15, pitch 1, keepAlive 10) and the per-tick
 * particles of EntityChemical (FlameCreator META_FIRE / META_BALEFIRE within 100 blocks, "vanillaExt"/"colordust").
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunBatchF1ClientEffects {

	private GunBatchF1ClientEffects() {
	}

	/** ItemGunBaseNT.loopedSounds */
	private static final Map<LivingEntity, FlameLoopSound> LOOPED_SOUNDS = new WeakHashMap<>();

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		OrchestrasF1.LOOP_SOUND = GunBatchF1ClientEffects::loopSound;
		LegacyChemicalEntity.CLIENT_EFFECTS = GunBatchF1ClientEffects::chemicalEffects;
	}

	/** ORCHESTRA_FLAMER / ORCHESTRA_CHEMTHROWER client branch for the CYCLE animation. */
	static void loopSound(LivingEntity entity, boolean keep) {
		Minecraft mc = Minecraft.getInstance();
		FlameLoopSound runningAudio = LOOPED_SOUNDS.get(entity);
		boolean playing = runningAudio != null && runningAudio.isPlaying();

		if(keep) {
			//start sound
			if(!playing) {
				FlameLoopSound audio = new FlameLoopSound(entity);
				LOOPED_SOUNDS.put(entity, audio);
				mc.getSoundManager().play(audio);
			}
			//keepalive
			if(playing) {
				runningAudio.keepAlive();
			}
		} else {
			//stop sound due to timeout / state change
			if(playing) runningAudio.stopSound();
		}
	}

	/** AudioDynamic: repeat, AttenuationType.NONE, volume by distance over range (max volume when attached to the local player), keepAlive expiry. */
	private static final class FlameLoopSound extends AbstractTickableSoundInstance {
		private final LivingEntity parentEntity;
		private final float maxVolume = 1F;
		private final float range = 15F;
		private final int keepAlive = 10;
		private int timeSinceKA;

		private FlameLoopSound(LivingEntity entity) {
			super(HbmSoundEvents.GUN_FLAMER_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.parentEntity = entity;
			this.looping = true;
			this.delay = 0;
			this.pitch = 1F;
			this.volume = maxVolume;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();
		}

		@Override
		public void tick() {
			LocalPlayer player = Minecraft.getInstance().player;

			if(parentEntity != null && player != parentEntity) {
				this.x = parentEntity.getX();
				this.y = parentEntity.getY();
				this.z = parentEntity.getZ();
			}

			// only adjust volume over distance if the sound isn't attached to this entity
			if(player != null && player != parentEntity) {
				float f = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2) + Math.pow(z - player.getZ(), 2));
				this.volume = func(f);
			} else {
				// shitty hack that prevents stereo weirdness when using 0 0 0
				if(player == parentEntity && parentEntity != null) {
					this.x = parentEntity.getX();
					this.y = parentEntity.getY() + 10;
					this.z = parentEntity.getZ();
				}
				this.volume = maxVolume;
			}

			if(this.timeSinceKA > this.keepAlive) {
				stopSound();
			}

			this.timeSinceKA++;
		}

		private float func(float dist) {
			return (dist / range) * -maxVolume + maxVolume;
		}

		void keepAlive() {
			this.timeSinceKA = 0;
		}

		void stopSound() {
			stop();
			Minecraft.getInstance().getSoundManager().stop(this);
		}

		boolean isPlaying() {
			return !isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}

	/** EntityChemical#onUpdate (client): balefire / burning flame particles within 100 blocks, colordust for liquids. */
	static void chemicalEffects(LegacyChemicalEntity chem, ChemicalStyle style) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		LocalPlayer me = mc.player;
		if(world == null || me == null) return;
		HbmFluidDefinition type = chem.getFluid();

		if(type.name().equals("balefire")) {

			if(me.distanceTo(chem) < 100)
				world.addParticle(HbmParticleTypes.FLAMETHROWER_BALEFIRE.get(), chem.getX(), chem.getY() - 0.125, chem.getZ(), 0, 0, 0);

		} else if(style == ChemicalStyle.LIQUID) {

			Color color = new Color(type.color());
			Vec3 motion = chem.getDeltaMovement();
			// "colordust": EntityBlockDustFX of wool, colored, lifetime 10 + rand(20), mY + 0.2 applied by the effect handler
			Particle fx = mc.particleEngine.createParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()), chem.getX(), chem.getY(), chem.getZ(),
					motion.x + world.random.nextGaussian() * 0.05, motion.y - 0.2 + world.random.nextGaussian() * 0.05 + 0.2, motion.z + world.random.nextGaussian() * 0.05);
			if(fx != null) {
				fx.setColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F);
				fx.setLifetime(10 + world.random.nextInt(20));
			}

		} else if(style == ChemicalStyle.BURNING) {

			if(me.distanceTo(chem) < 100)
				world.addParticle(HbmParticleTypes.FLAMETHROWER_FIRE.get(), chem.getX(), chem.getY() - 0.125, chem.getZ(), 0, 0, 0);
		}
	}
}

package com.atomiclegacy.client.gun;

import java.util.Map;
import java.util.WeakHashMap;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF4;
import com.atomiclegacy.items.weapon.sedna.hud.HUDComponentDurabilityBar;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunStinger;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * [weapons batch F4] client side of gun_stinger: RenderScreenOverlay.renderStingerLockon (the lock-on bar under the
 * crosshair) and the GUN_LOCKON AudioWrapper loop of Orchestras.ORCHESTRA_STINGER
 * (MainRegistry.proxy.getLoopedSound(GUN_LOCKON, pos, volume 1, range 15, pitch 1, keep alive 10 ticks)).
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunBatchF4ClientEffects {

	private GunBatchF4ClientEffects() {
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		OrchestrasF4.LOCKON_LOOP = GunBatchF4ClientEffects::lockonLoop;
	}

	/** RenderScreenOverlay#renderStingerLockon: the 30x10 frame at (146, 18) and the 28 px progress bar at (147, 29). */
	public static void renderStingerLockon(GuiGraphics graphics) {
		int progress = (int) (ItemGunStinger.lockon * 28);
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		graphics.blit(HUDComponentDurabilityBar.misc, w / 2 - 15, h / 2 + 18, 146, 18, 30, 10);
		graphics.blit(HUDComponentDurabilityBar.misc, w / 2 - 14, h / 2 + 19, 147, 29, progress, 8);
	}

	/* ===== GUN_LOCKON loop (ItemGunBaseNT.loopedSounds) ===== */

	private static final int KEEP_ALIVE_TICKS = 10;
	private static final double RANGE = 15.0D;
	private static final Map<LivingEntity, LockonSound> LOOPS = new WeakHashMap<>();

	/** active: start the loop if it is not playing and keep it alive; otherwise stop a running loop (timeout). */
	public static void lockonLoop(LivingEntity entity, boolean active) {
		Minecraft mc = Minecraft.getInstance();
		LockonSound sound = LOOPS.get(entity);
		if(active) {
			if(sound == null || sound.isStopped() || !mc.getSoundManager().isActive(sound)) {
				sound = new LockonSound(entity);
				LOOPS.put(entity, sound);
				mc.getSoundManager().play(sound);
			}
			sound.keepAlive();
		} else if(sound != null) {
			sound.requestStop();
		}
	}

	private static final class LockonSound extends AbstractTickableSoundInstance {
		private final LivingEntity entity;
		private int keepAlive = KEEP_ALIVE_TICKS;

		private LockonSound(LivingEntity entity) {
			super(HbmSoundEvents.GUN_LOCKON.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.entity = entity;
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();
			this.looping = true;
			this.delay = 0;
			this.pitch = 1.0F;
			this.volume = 1.0F;
			this.attenuation = Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			if(mc.level == null || mc.player == null || !entity.isAlive() || keepAlive-- <= 0) {
				requestStop();
				return;
			}
			// AudioWrapper#updatePosition
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();
			// AudioDynamic falloff: full volume at the source, silent at the range
			double distance = Math.sqrt(mc.player.distanceToSqr(x, y, z));
			this.volume = distance >= RANGE ? 0.0F : (float) ((RANGE - distance) / RANGE);
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		private void keepAlive() {
			keepAlive = KEEP_ALIVE_TICKS;
		}

		private void requestStop() {
			stop();
			LOOPS.remove(entity, this);
		}
	}
}

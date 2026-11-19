package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyArmorParity;
import com.atomiclegacy.items.LegacyJetpackItem;
import com.atomiclegacy.items.SettingsToolItem;
import com.atomiclegacy.pollution.HbmPollutionWorlds;
import com.atomiclegacy.registry.HbmDataAttachments;
import com.atomiclegacy.worldgen.ZirnoxWasteManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** [e3] Second bug pass: player-data lifecycle of the 1.7.10 extended properties and cleanup of static per-world state. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class ParityE3Events {
    private ParityE3Events() {
    }

    /**
     * 1.7.10 created a new EntityPlayerMP both on death and on the return from the End. ModEventHandler#onPlayerClone
     * copied only HbmPlayerProps; HbmLivingProps (radiation, digamma, asbestos, black lung, contagion, oil, fire
     * timers, contamination effects) always started empty on the new entity. NeoForge copies every serializable
     * attachment on a non-death clone, so the living data is dropped here for that case (death is already covered by
     * the missing copyOnDeath).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        // [user decision 2026-11-19] the 1.7.10 loss of HbmLivingProps on the return from the End is a bug of the
        // original (new EntityPlayerMP without copying); the port keeps radiation/digamma/etc. on a non-death clone.
        // HbmPlayerProps.keysPressed is not part of serialize(): a fresh entity has no key held.
        LegacyJetpackItem.setInputActive(player, false);
    }

    /** The HbmLivingProps part of the port's attachments. */
    public static void resetLivingProps(Player player) {
        player.removeData(HbmDataAttachments.LIVING_RADIATION);
        player.removeData(HbmDataAttachments.LIVING_HAZARDS);
        player.removeData(HbmDataAttachments.MKU_CONTAGION);
    }

    /** ModEventHandler#onPlayerChangeDimension: setKeyPressed(JETPACK, false) (DASH is a one-shot packet in the port). */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LegacyJetpackItem.setInputActive(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SettingsToolItem.clearKeyState(event.getEntity());
        LegacyArmorParity.clearPlink(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            unloadLevelState(level);
        }
    }

    public static void unloadLevelState(ServerLevel level) {
        ZirnoxWasteManager.clear(level.dimension());
        HbmPollutionWorlds.unload(level.dimension());
        MeteorPedestalBroadcasts.clear(level.dimension());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ZirnoxWasteManager.clear(null);
        HbmPollutionWorlds.clear();
        MeteorPedestalBroadcasts.clear(null);
    }
}

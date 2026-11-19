package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.api.client.HbmLookOverlayApi;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayData;
import com.atomiclegacy.api.client.HbmLookOverlayApi.OverlayLine;
import com.atomiclegacy.blocks.CraneMachineBlock;
import com.atomiclegacy.tileentity.CraneMachineBlockEntity;
import com.atomiclegacy.tileentity.DroneCrateBlockEntity;
import com.atomiclegacy.tileentity.DroneWaypointBlockEntity;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity R2] ILookOverlay.printHook of the logistics blocks: RTTY torches (RadioTorchRWBase, Logic, Counter,
 * Reader, Controller, Relay), DroneCrate, DroneWaypoint and CraneSplitter. Title colour 0xFFFF00.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ParityR2LookOverlays {
    private static final String AQUA = "§b";
    private static final String RED = "§c";

    private ParityR2LookOverlays() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        HbmLookOverlayApi.registerProvider(AtomicLegacy.id("parity_r2_logistics"), 1_500, (level, hit) -> {
            BlockEntity entity = level.getBlockEntity(hit.getBlockPos());
            List<OverlayLine> text = new ArrayList<>();
            if (entity instanceof RadioTorchBlockEntity radio) {
                switch (radio.kind()) {
                    case SENDER, RECEIVER, LOGIC, RELAY -> {
                        if (!radio.channel().isEmpty()) text.add(OverlayLine.white(AQUA + "Freq: " + radio.channel()));
                        text.add(OverlayLine.white(RED + "Signal: " + radio.signal()));
                    }
                    case COUNTER -> {
                        for (int i = 0; i < 3; i++) {
                            if (!radio.counterChannel(i).isEmpty()) {
                                text.add(OverlayLine.white(AQUA + "Freq " + (i + 1) + ": " + radio.counterChannel(i)));
                                text.add(OverlayLine.white(RED + "Signal " + (i + 1) + ": " + radio.counterLastCount(i)));
                            }
                        }
                    }
                    case READER -> {
                        for (int i = 0; i < 8; i++) {
                            if (radio.readerChannel(i).isEmpty() || radio.readerName(i).isEmpty()) continue;
                            text.add(OverlayLine.white(AQUA + radio.readerChannel(i) + ": " + radio.readerName(i)));
                        }
                    }
                    case CONTROLLER -> text.add(OverlayLine.white(AQUA + "Freq: " + radio.channel()));
                }
                return new OverlayData(entity.getBlockState().getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
            }
            if (entity instanceof DroneWaypointBlockEntity waypoint) {
                text.add(OverlayLine.white("Waypoint distance: " + waypoint.height()));
                BlockPos next = waypoint.nextTarget();
                if (next != null) text.add(OverlayLine.white("Next waypoint: " + next.getX() + " / " + next.getY() + " / " + next.getZ()));
                return new OverlayData(entity.getBlockState().getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
            }
            if (entity instanceof DroneCrateBlockEntity crate) {
                BlockPos next = crate.nextTarget();
                if (next == null) return null;
                text.add(OverlayLine.white("Next waypoint: " + next.getX() + " / " + next.getY() + " / " + next.getZ()));
                return new OverlayData(entity.getBlockState().getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
            }
            if (entity instanceof CraneMachineBlockEntity crane && crane.kind() == CraneMachineBlock.Kind.SPLITTER) {
                text.add(OverlayLine.white("Splitter ratio: " + crane.leftRatio() + ":" + crane.rightRatio()));
                return new OverlayData(entity.getBlockState().getBlock().getName(), HbmLookOverlayApi.COLOR_YELLOW, text);
            }
            return null;
        });
    }
}

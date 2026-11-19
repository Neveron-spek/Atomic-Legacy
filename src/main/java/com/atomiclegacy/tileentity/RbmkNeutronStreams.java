package com.atomiclegacy.tileentity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.radiation.ChunkRadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the 1.7.10 RBMK neutron stream system (NeutronNodeWorld,
 * NeutronHandler, RBMKNeutronHandler.RBMKNeutronStream).
 *
 * Rods emit streams during their update; all streams are resolved at the
 * start of the next server tick ({@code ServerTickEvent} START in 1.7.10),
 * in emission order, delivering flux to the receiving rods/outgassers which
 * then burn it during that tick.  The node cache of the original only cached
 * the column type and lid state, both of which are read live here.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class RbmkNeutronStreams {
    private static final Map<ResourceKey<Level>, List<Stream>> STREAMS = new LinkedHashMap<>();

    private RbmkNeutronStreams() {
    }

    /** One RBMKNeutronStream: origin column, direction vector, flux quantity and fast ratio. */
    public static final class Stream {
        final BlockPos origin;
        final double vx;
        final double vz;
        double fluxQuantity;
        double fluxRatio;

        Stream(BlockPos origin, double vx, double vz, double fluxQuantity, double fluxRatio) {
            this.origin = origin.immutable();
            this.vx = vx;
            this.vz = vz;
            this.fluxQuantity = fluxQuantity;
            this.fluxRatio = fluxRatio;
        }

        public double fluxQuantity() {
            return fluxQuantity;
        }

        public double fluxRatio() {
            return fluxRatio;
        }
    }

    static void emit(ServerLevel level, BlockPos origin, double vx, double vz, double flux, double ratio) {
        STREAMS.computeIfAbsent(level.dimension(), k -> new ArrayList<>())
                .add(new Stream(origin, vx, vz, flux, ratio));
    }

    /** Number of streams waiting for the next pass (diagnostics). */
    static int pending(ServerLevel level) {
        List<Stream> list = STREAMS.get(level.dimension());
        return list == null ? 0 : list.size();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        runAll(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STREAMS.clear();
    }

    /** NeutronHandler.onServerTick: run every world's streams, then remove them. */
    static void runAll(MinecraftServer server) {
        if (STREAMS.isEmpty()) {
            return;
        }
        List<Map.Entry<ResourceKey<Level>, List<Stream>>> worlds = new ArrayList<>(STREAMS.entrySet());
        STREAMS.clear();
        for (Map.Entry<ResourceKey<Level>, List<Stream>> entry : worlds) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            run(level, entry.getValue());
        }
    }

    /** Resolve the given streams immediately (used by the diagnostics with a private list). */
    static void runLevel(ServerLevel level) {
        List<Stream> list = STREAMS.remove(level.dimension());
        if (list != null) {
            run(level, list);
        }
    }

    private static void run(ServerLevel level, List<Stream> streams) {
        int range = fluxRange();
        int columnHeight = RbmkComponentBlock.columnHeight(level);
        for (Stream stream : streams) {
            runStreamInteraction(level, stream, range, columnHeight);
        }
    }

    static int fluxRange() {
        return com.atomiclegacy.tileentity.RbmkDials.getFluxRange();
    }

    private static BlockPos step(Stream stream, int i) {
        int x = (int) Math.floor(0.5D + stream.vx * i);
        int z = (int) Math.floor(0.5D + stream.vz * i);
        return stream.origin.offset(x, 0, z);
    }

    private static void runStreamInteraction(ServerLevel level, Stream stream, int range, int columnHeight) {
        if (stream.fluxQuantity == 0.0D) {
            return;
        }
        RbmkComponentBlockEntity origin = RbmkComponentBlockEntity.rbmkColumnAt(level, stream.origin);
        if (origin == null) {
            return; // Doesn't exist anymore!
        }
        int moderatedCount = 0;
        for (int i = 1; i <= range; i++) {
            if (stream.fluxQuantity == 0.0D) {
                return;
            }
            BlockPos targetPos = step(stream, i);
            RbmkComponentBlockEntity target = RbmkComponentBlockEntity.rbmkColumnAt(level, targetPos);
            if (target == null) {
                int hits = RbmkComponentBlockEntity.opaqueColumnHits(level, targetPos);
                if (hits == columnHeight) {
                    return;
                } else if (hits > 0) {
                    // 1.7.10 irradiates the stream ORIGIN here (irradiateFromFlux(pos, hits)).
                    irradiate(level, stream.origin, stream.fluxQuantity, hits, columnHeight);
                    stream.fluxQuantity *= 1.0D - ((double) hits / columnHeight);
                    continue;
                } else {
                    irradiate(level, stream.origin, stream.fluxQuantity, 0, columnHeight);
                    continue;
                }
            }
            RbmkStreamType type = target.streamType();
            if (type == RbmkStreamType.OTHER) {
                continue;
            }
            if (!target.hasLid()) {
                ChunkRadiationData.get(level).incrementRadiation(targetPos, (float) (stream.fluxQuantity * 0.05F));
            }
            if (type == RbmkStreamType.MODERATOR || target.isModeratedColumn()) {
                moderatedCount++;
                moderate(stream);
            }
            if (type == RbmkStreamType.ROD) {
                if (target.streamHasRod()) {
                    target.receiveStream(stream);
                    return;
                }
            } else if (type == RbmkStreamType.OUTGASSER) {
                if (target.outgasserCanProcessForStream()) {
                    target.outgasserReceiveStream(stream);
                    return;
                }
            } else if (type == RbmkStreamType.CONTROL_ROD) {
                if (target.controlLevel() > 0.0D) {
                    stream.fluxQuantity *= target.controlMultiplierForStream();
                    continue;
                }
                return;
            } else if (type == RbmkStreamType.REFLECTOR) {
                if (origin.isModeratedColumn()) {
                    moderatedCount++;
                }
                if (stream.fluxRatio > 0 && moderatedCount > 0) {
                    for (int m = 0; m < moderatedCount; m++) {
                        moderate(stream);
                    }
                }
                double reflectorEfficiency = RbmkComponentBlockEntity.reflectorEfficiencyDial();
                if (reflectorEfficiency != 1.0D) {
                    stream.fluxQuantity *= reflectorEfficiency;
                    continue;
                }
                origin.receiveStream(stream);
                return;
            } else if (type == RbmkStreamType.ABSORBER) {
                target.absorbStreamHeat(RbmkComponentBlockEntity.absorberHeatConversionDial() * stream.fluxQuantity);
                double absorberEfficiency = RbmkComponentBlockEntity.absorberEfficiencyDial();
                if (absorberEfficiency == 1) {
                    return;
                }
                stream.fluxQuantity *= absorberEfficiency;
            }
        }
        // getNodes(): only the column at the full range matters for the tail logic.
        RbmkComponentBlockEntity last = RbmkComponentBlockEntity.rbmkColumnAt(level, step(stream, range));
        if (last == null) {
            // irradiateFromFlux(new BlockPos(origin + vector)) with the hits at that position.
            BlockPos after = BlockPos.containing(stream.origin.getX() + stream.vx, stream.origin.getY(),
                    stream.origin.getZ() + stream.vz);
            int hits = RbmkComponentBlockEntity.opaqueColumnHits(level, after);
            irradiate(level, after, stream.fluxQuantity, hits, columnHeight);
        }
        // A control rod as the last node only (re)created cache nodes in 1.7.10;
        // it had no effect on flux, heat or radiation, so nothing is done here.
    }

    private static void irradiate(ServerLevel level, BlockPos pos, double flux, int hits, int columnHeight) {
        ChunkRadiationData.get(level).incrementRadiation(pos,
                (float) (flux * 0.05F * (1 - (double) hits / columnHeight)));
    }

    private static void moderate(Stream stream) {
        stream.fluxRatio *= (1 - RbmkComponentBlockEntity.moderatorEfficiencyDial());
    }

    /** RBMKNeutronHandler.RBMKType. */
    public enum RbmkStreamType {
        ROD,
        MODERATOR,
        CONTROL_ROD,
        REFLECTOR,
        ABSORBER,
        OUTGASSER,
        OTHER
    }
}

package com.atomiclegacy.explosion;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.entity.ChekhovBulletEntity;
import com.atomiclegacy.entity.JeremyShellEntity;
import com.atomiclegacy.entity.LegacyArtilleryShellEntity;
import com.atomiclegacy.entity.LegacyBossProjectileEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyGrenadeEntity;
import com.atomiclegacy.entity.LegacyHimarsRocketEntity;
import com.atomiclegacy.entity.LegacyShrapnelEntity;
import com.atomiclegacy.entity.NukeTorexEntity;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.radiation.HbmRadiationWorlds;
import com.atomiclegacy.worldgen.NuclearFalloutTerrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class NukeExplosionManager {
    // EntityNukeExplosionMK5 called cacheChunksTick(BombConfig.mk5) and
    // destructionTick(BombConfig.mk5) every tick, BombConfig.mk5 = 50 ms each.
    // Earlier 4/5 ms slices plus hard step caps made craters appear ~10x
    // slower than the original (a Tsar needed most of an hour to reach its
    // centre). The work is bounded by these original time slices only; the
    // count caps are just overflow guards.
    private static final int MK5_RAY_STEPS_PER_TICK = 1 << 26;
    private static final int MK5_BLOCK_UPDATES_PER_TICK = 1 << 22;
    private static final int MK5_DESTROY_BATCH_SIZE = 512;
    private static final int MK5_RAY_DIRECTION_BATCH_SIZE = 4_096;
    private static final int MK5_RAY_DIRECTION_BATCHES_AHEAD = 8;
    // [parity V3: BombConfig.mk5] 6.02_mk5BlastTime milliseconds per tick (default 50).
    private static long mk5Nanos() {
        return com.atomiclegacy.config.HbmParityV3Config.mk5() * 1_000_000L;
    }
    /* Every 1.7.10 MK5 entity ran its own budget each tick, so concurrent blasts
     * all progressed. The per-tick budget is shared between active blasts, each
     * keeping at least a quarter of it. */
    private static double budgetShare = 1.0D;
    /* EntityNukeExplosionMK3 (waste=false) terrain instead of MK5 rays; consumed by the next NukeTask. */
    private static final int MK3_NONE = 0, MK3_FLEIJA = 1, MK3_SOLINIUM = 2;
    private static int pendingMk3Mode = MK3_NONE;
    private static final int MK5_DAMAGE_ENTITIES_PER_TICK = 256;
    private static final long MK5_DAMAGE_NANOS_PER_TICK = 500_000L;
    // High-version balance adjustment requested for heavily buffed players
    // and affix mobs. Range and the legacy linear falloff remain unchanged.
    private static final float MK5_MAX_DAMAGE = 5_000.0F;
    private static final long CACHE_MISS = Long.MIN_VALUE;
    private static final long SAMPLE_AIR = 1L << 32;
    private static final long SAMPLE_FLUID_EMPTY = 1L << 33;
    // These queues belong to one concrete loaded level, not merely to a
    // dimension id. Integrated servers reuse minecraft:overworld when the
    // player leaves one save and creates another in the same JVM; a
    // dimension-id key would let the abandoned blast continue in the new
    // save. Identity keys plus the unload hook below make that impossible.
    private static final Map<ServerLevel, Queue<NukeTask>> TASKS = new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<Long, Long>> FIELD_DISTURBERS = new IdentityHashMap<>();
    private static final ExecutorService PLANNER = Executors.newFixedThreadPool(
            Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2)),
            task -> {
                Thread thread = new Thread(task, "RHbm-NukePlanner");
                thread.setDaemon(true);
                return thread;
            }
    );
    // One producer per blast: a producer blocks while its look-ahead queue is
    // full, so a shared single thread would stall every later blast's rays
    // until the earlier (possibly Tsar-sized) blast consumed all of its own.
    private static final ExecutorService RAY_PLANNER = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "RHbm-NukeRayPlanner");
        thread.setDaemon(true);
        return thread;
    });

    private NukeExplosionManager() {
    }

    private enum EntityEffectMode {
        NUCLEAR_BLAST,
        SOLINIUM_RADIATION
    }

    public static void scheduleLittleBoy(ServerLevel level, double x, double y, double z) {
        scheduleLegacyNuke(level, x, y, z, com.atomiclegacy.config.HbmParityV3Config.boyRadius()); // [parity V3: BombConfig.boyRadius]
    }

    public static void scheduleMissileNuke(ServerLevel level, double x, double y, double z) {
        scheduleLegacyNuke(level, x, y, z, com.atomiclegacy.config.HbmParityV3Config.missileRadius()); // [parity V3: BombConfig.missileRadius]
    }

    public static void scheduleLegacyNuke(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true, 0);
    }

    public static void scheduleLegacyNukeMoreFallout(ServerLevel level, double x, double y, double z,
                                                     int radius, int falloutAdd) {
        scheduleInternal(level, x, y, z, radius, true, true, true, falloutAdd);
    }

    /** 1.7.10 EntityMissileMicro: PARAMS_HIGH delegates to MK5 directly and
     * does not create the ordinary missile Torex. MK5 still creates fallout. */
    public static void scheduleMicroNuke(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, false, true, true, 0);
    }

    // [parity M3: dfc] TileEntityCore meltdown: a bare EntityNukeExplosionMK3 (coefficient 1, waste false)
    // whose cloud is an EntityCloudFleijaRainbow spawned by the caller, so no Fleija cloud is emitted here.
    public static void scheduleFleijaNukeWithoutCloud(ServerLevel level, double x, double y, double z, int radius) {
        pendingMk3Mode = MK3_FLEIJA;
        scheduleInternal(level, x, y, z, radius, false, false, false, 0);
    }

    /** 1.7.10 EntityMissileSchrabidium: MK3 Fleija explosion.  The Fleija
     * cloud is emitted explicitly; no standard Torex is substituted. */
    public static void scheduleFleijaNuke(ServerLevel level, double x, double y, double z, int radius) {
        pendingMk3Mode = MK3_FLEIJA;
        scheduleInternal(level, x, y, z, radius, false, false, false, 0);
        sendFleijaCloud(level, x, y, z, radius);
    }

    /** 1.7.10 EntityNukeExplosionMK3.makeSol: terrain destruction plus an
     * unobstructed 15,000..250,000 RAD pulse inside the configured radius;
     * unlike FLEIJA it does not apply ordinary nuclear-blast damage. */
    public static void scheduleSoliniumNuke(ServerLevel level, double x, double y, double z, int radius) {
        pendingMk3Mode = MK3_SOLINIUM;
        scheduleInternal(level, x, y, z, radius, false, false, false, 0,
                EntityEffectMode.SOLINIUM_RADIATION);
    }

    /** EntityNukeExplosionMK5.statFac (ordinary nuclear/TX warheads). */
    public static void scheduleMk5Nuclear(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true, 0);
    }

    /** EntityNukeExplosionMK5.statFacNoRad used by custom N2. */
    public static void scheduleMk5NoRadiation(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, false, false, 0);
    }

    /** TOM starts the persistent impact climate only after its asynchronous crater is complete. */
    public static void scheduleTom(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, false, false, 0,
                EntityEffectMode.NUCLEAR_BLAST,
                finishedLevel -> com.atomiclegacy.world.TomImpactSavedData.get(finishedLevel).beginImpact());
    }

    /**
     * Exact MK5 core created by {@code EntityDeathBlast}.  The old call is
     * {@code statFacNoRad(world, strength, ...)}: its ray length is half the
     * supplied strength and the death-blast entity itself owns the muke sound
     * and particle packet.  It therefore must not inherit the common modern
     * Torex, start sounds, or generic start particles.
     */
    public static void scheduleDeathBlastMk5NoRadiation(
            ServerLevel level, double x, double y, double z, int strength) {
        HbmAdvancements.awardAll(level, "manhattan");
        com.atomiclegacy.satellite.SatelliteEventTracker.reportRadiation(
                level,
                com.atomiclegacy.satellite.SatelliteEventTracker.DETECTOR_HIGH_DURATION,
                com.atomiclegacy.satellite.SatelliteEventTracker.BurstIntensity.HIGH,
                x, z);
        NukeTask task = new NukeTask(
                new Vec3(x, y, z),
                strength,
                strength / 2,
                false,
                0,
                EntityEffectMode.NUCLEAR_BLAST, null);
        task.applyInitialDamage(level);
        TASKS.computeIfAbsent(level, unused -> new ArrayDeque<>()).add(task);
    }

    /** EntityMissileMirv doubles missileRadius and uses the normal MK5/Torex pair. */
    public static void scheduleMirv(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius * 2, true, true, true, 0);
    }

    /** EntityMissileDoomsday: MK5.moreFallout(100). */
    public static void scheduleDoomsday(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true, 100);
    }

    /** EntityMissileDoomsdayRusted uses missileRadius (not doubled) and the
     * same extra fallout modifier. */
    public static void scheduleRustedDoomsday(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true, 100);
    }

    private static void scheduleInternal(ServerLevel level, double x, double y, double z, int radius,
                                          boolean spawnTorex, boolean fallout, boolean radiation, int falloutAdd) {
        scheduleInternal(level, x, y, z, radius, spawnTorex, fallout, radiation, falloutAdd,
                EntityEffectMode.NUCLEAR_BLAST);
    }

    private static void scheduleInternal(ServerLevel level, double x, double y, double z, int radius,
                                          boolean spawnTorex, boolean fallout, boolean radiation, int falloutAdd,
                                          EntityEffectMode entityEffectMode) {
        scheduleInternal(level, x, y, z, radius, spawnTorex, fallout, radiation, falloutAdd,
                entityEffectMode, null);
    }

    private static void scheduleInternal(ServerLevel level, double x, double y, double z, int radius,
                                         boolean spawnTorex, boolean fallout, boolean radiation, int falloutAdd,
                                         EntityEffectMode entityEffectMode,
                                         Consumer<ServerLevel> completionAction) {
        HbmAdvancements.awardAll(level, "manhattan");
        com.atomiclegacy.satellite.SatelliteEventTracker.reportRadiation(
                level,
                com.atomiclegacy.satellite.SatelliteEventTracker.DETECTOR_HIGH_DURATION,
                com.atomiclegacy.satellite.SatelliteEventTracker.BurstIntensity.HIGH,
                x, z);
        // [parity V3: BombConfig.falloutRange] EntityNukeExplosionMK5: (int) (length * 2.5 + falloutAdd) * falloutRange / 100
        int falloutScale = fallout ? Math.max(1, (int) (radius * 2.5D + Math.max(0, falloutAdd))
                * com.atomiclegacy.config.HbmParityV3Config.falloutRange() / 100) : 0;
        // The old MK5 starts irradiating nearby entities as soon as it begins
        // updating. Do not leave the persistent blast-site field behind the
        // much longer asynchronous terrain queue: players could otherwise
        // stand in a still-forming crater with an almost clean chunk readout.
        // [parity K2: nuke] EntityNukeExplosionMK5 does not call incrementRad; the former chunk "fallout field"
        // (seedFalloutRadiation, up to 10 000 RAD per chunk) was invented and is no longer applied.
        NukeTask task = new NukeTask(new Vec3(x, y, z), radius * 2, radius, radiation, falloutScale,
                entityEffectMode, completionAction);
        // Terrain tasks are deliberately serialized per dimension, but blast
        // damage is not terrain work. Resolve the first legacy damage pass at
        // detonation time so a second nuke cannot sit harmlessly behind an
        // older crater task. Follow-up passes remain budgeted in tick().
        playInitialSound(level, x, y, z);
        if (spawnTorex) {
            spawnTorex(level, x, y + 0.5D, z, radius);
        }
        sendInitialParticles(level, x, y, z);
        task.applyInitialDamage(level);
        TASKS.computeIfAbsent(level, unused -> new ArrayDeque<>()).add(task);
    }

    private static void sendFleijaCloud(ServerLevel level, double x, double y, double z, int radius) {
        int count = Math.max(64, radius * 10);
        level.sendParticles(com.atomiclegacy.registry.HbmParticleTypes.LEGACY_CLOUD.get(),
                x, y, z, count, radius * 0.05D, radius * 0.05D, radius * 0.05D, 0.35D);
    }

    /**
     * Applies the old custom-nuke stage priority before entering the bounded
     * destruction queue. The world mutation remains on the server thread, so
     * this method is safe to call from a block entity update.
     */
    public static void scheduleCustomNuke(ServerLevel level, double x, double y, double z,
                                          float tnt, float nuke, float hydro, float amat,
                                          float dirty, float schrab, float euph) {
        // NukeCustom.explodeCustom; callers pass the block centre (xCoord + 0.5).
        double ox = x - 0.5D, oy = y - 0.5D, oz = z - 0.5D;
        dirty = Math.min(dirty, 100.0F);
        if (euph > 0.0F) {
            // MK3 (waste=false, extType 0) at xCoord/yCoord/zCoord, range 150
            level.playSound(null, ox, oy, oz, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS, 100000.0F, 1.0F);
            pendingMk3Mode = MK3_FLEIJA;
            scheduleInternal(level, ox, oy, oz, 150, false, false, false, 0);
            sendFleijaCloud(level, ox, oy, oz, 50);
            return;
        }
        if (schrab > 0.0F) {
            schrab += amat / 2 + hydro / 4 + nuke / 8 + tnt / 16;
            schrab = Math.min(schrab, 250.0F);
            BlockPos center = BlockPos.containing(x, y, z);
            if (!isFleijaSuppressed(level, center)) {
                scheduleFleijaNuke(level, x, y, z, (int) schrab);
            }
            return;
        }
        if (amat > 0.0F) {
            amat += hydro / 2 + nuke / 4 + tnt / 8;
            amat = Math.min(amat, 350.0F);
            BalefireExplosionManager.schedule(level, BlockPos.containing(x, y, z), (int) amat);
            return;
        }
        if (hydro > 0.0F) {
            hydro += nuke / 2 + tnt / 4;
            hydro = Math.min(hydro, 350.0F);
            dirty *= 0.25F;
            scheduleInternal(level, x, y, z, mk5Radius((int) hydro), true, true, true, (int) dirty);
            return;
        }
        if (nuke > 0.0F) {
            nuke += tnt / 2;
            nuke = Math.min(nuke, 200.0F);
            // the nuclear stage is centred at yCoord + 5
            scheduleInternal(level, x, oy + 5.0D, z, mk5Radius((int) nuke), true, true, true, (int) dirty);
            return;
        }
        if (tnt >= 75.0F) {
            tnt = Math.min(tnt, 150.0F);
            scheduleInternal(level, x, y, z, mk5Radius((int) tnt), true, false, false, 0);
        } else if (tnt > 0.0F) {
            level.explode(null, x, y, z, tnt, true, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        }
    }

    /** EntityNukeExplosionMK5.statFac: {@code if(r == 0) r = 25;} */
    private static int mk5Radius(int r) {
        return r == 0 ? 25 : r;
    }

    /** Registers the 1.7.10 MachineFieldDisturber exclusion point for FLEIJA detonations. */
    public static void registerFieldDisturber(ServerLevel level, BlockPos pos, int lifetimeTicks) {
        FIELD_DISTURBERS.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(pos.asLong(), level.getGameTime() + lifetimeTicks);
    }

    /** Matches EntityNukeExplosionMK3.statFacFleija's 300-block exclusion check. */
    public static boolean isFleijaSuppressed(ServerLevel level, BlockPos explosionCenter) {
        Map<Long, Long> fields = FIELD_DISTURBERS.get(level);
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        fields.entrySet().removeIf(entry -> entry.getValue() < now);
        if (fields.isEmpty()) {
            FIELD_DISTURBERS.remove(level);
            return false;
        }
        for (long serializedPos : fields.keySet()) {
            if (BlockPos.of(serializedPos).distSqr(explosionCenter) < 90_000.0D) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Queue<NukeTask> tasks = TASKS.get(level);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        budgetShare = Math.max(0.25D, 1.0D / tasks.size());
        // Snapshot: a blast may detonate other warheads that schedule new tasks.
        for (NukeTask task : new java.util.ArrayList<>(tasks)) {
            if (task.tick(level)) {
                task.complete(level);
                tasks.remove(task);
            }
        }
        budgetShare = 1.0D;
        if (tasks.isEmpty()) {
            TASKS.remove(level);
        }
    }

    /** Diagnostics only: whether a terrain task is still running in this level. */
    public static boolean hasPendingTasks(ServerLevel level) {
        Queue<NukeTask> tasks = TASKS.get(level);
        return tasks != null && !tasks.isEmpty();
    }

    /** Diagnostics only: abandons every running blast in this level. */
    public static void cancelAllForDiagnostics(ServerLevel level) {
        Queue<NukeTask> tasks = TASKS.remove(level);
        if (tasks != null) {
            tasks.forEach(NukeTask::cancel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Queue<NukeTask> tasks = TASKS.remove(level);
        if (tasks != null) {
            tasks.forEach(NukeTask::cancel);
            tasks.clear();
        }
        FIELD_DISTURBERS.remove(level);
    }

    private static void playInitialSound(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(), SoundSource.HOSTILE, 10000.0F, 0.5F + level.random.nextFloat() * 0.1F);
        level.playSound(null, x, y, z, HbmSoundEvents.RBMK_EXPLOSION.get(), SoundSource.HOSTILE, 10000.0F, 0.8F + level.random.nextFloat() * 0.05F);
    }

    private static void spawnTorex(ServerLevel level, double x, double y, double z, int radius) {
        NukeTorexEntity torex = new NukeTorexEntity(HbmEntityTypes.NUKE_TOREX.get(), level);
        torex.setPos(x, y, z);
        double scaledRadius = radius * 0.01D;
        double squirt = Math.sqrt(scaledRadius + 1.0D / ((scaledRadius + 2.0D) * (scaledRadius + 2.0D)))
                - 1.0D / (scaledRadius + 2.0D);
        torex.setScale(Mth.clamp((float) (squirt * 1.5D), 0.5F, 5.0F));
        level.addFreshEntity(torex);
    }

    private static void sendInitialParticles(ServerLevel level, double x, double y, double z) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > 1000.0D * 1000.0D) {
                continue;
            }
            level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, ParticleTypes.FLASH, true, x, y + 1.0D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y + 8.0D, z, 120, 8.0D, 10.0D, 8.0D, 0.04D);
            level.sendParticles(player, ParticleTypes.LARGE_SMOKE, true, x, y + 2.0D, z, 96, 20.0D, 3.0D, 20.0D, 0.18D);
            level.sendParticles(player, ParticleTypes.FLAME, true, x, y + 1.0D, z, 80, 12.0D, 2.0D, 12.0D, 0.12D);
        }
    }

    /** [parity K2: diagnostics] MK5 prompt radiation of one entity at the given damage pulse (radiation on). */
    public static void parityK2PromptRadiation(ServerLevel level, Vec3 center, int strength, LivingEntity entity, int pulseIndex) {
        NukeTask.testPromptRadiation(level, center, strength, entity, pulseIndex);
    }

    /** [parity K2: diagnostics] ExplosionHurtUtil.doRadiation(15000, 250000, radius) for one entity. */
    public static void parityK2SoliniumRadiation(Vec3 center, double radius, Entity entity) {
        NukeTask.soliniumRadiation(center, radius, entity);
    }

    /**
     * [parity U3: diagnostics] One MK5 ray (ExplosionNukeRayBatched.collectTip + processChunk) traced synchronously
     * from {@code center} along the unit vector (dx, dy, dz), with the destruction pass applied. Returns the
     * removed positions.
     */
    public static java.util.List<BlockPos> parityU3Ray(ServerLevel level, Vec3 center, double dx, double dy, double dz,
                                                       int strength, int length) {
        NukeTask task = new NukeTask(center, strength, length, false, 0, EntityEffectMode.NUCLEAR_BLAST, null);
        task.cancelled = true;
        NukeTask.RayScan ray = task.new RayScan(dx, dy, dz, strength, length);
        while (!ray.done()) {
            if (ray.step(level, center, task.blockSampleCache) == 0) {
                break;
            }
        }
        java.util.List<BlockPos> removed = new java.util.ArrayList<>();
        for (long packed : task.pendingDestroy) {
            if (task.fastDestroyCollected(level, packed)) {
                removed.add(BlockPos.of(packed));
            }
        }
        return removed;
    }

    /** [parity V4: nuke tip] EntityNukeExplosionMK5 passes (int) truncated coordinates to the ray explosion. */
    static Vec3 legacyRayCenter(Vec3 center) {
        return new Vec3((int) center.x, (int) center.y, (int) center.z);
    }

    /** [parity V4: diagnostics] parityU3Ray from the MK5 ray center ((int) truncated), as the real task does. */
    public static java.util.List<BlockPos> parityV4Ray(ServerLevel level, Vec3 center, double dx, double dy, double dz,
                                                       int strength, int length) {
        return parityU3Ray(level, legacyRayCenter(center), dx, dy, dz, strength, length);
    }

    private static final class NukeTask {
        private final Vec3 center;
        /** [parity V4: nuke tip] new ExplosionNukeRayBatched(world, (int) posX, (int) posY, (int) posZ, ...). */
        private final Vec3 rayCenter;
        private final int strength;
        private final int length;
        private final boolean radiationEnabled;
        private final int falloutScale;
        private final EntityEffectMode entityEffectMode;
        private final Consumer<ServerLevel> completionAction;
        private final Long2LongOpenHashMap blockSampleCache = new Long2LongOpenHashMap();
        private final Queue<Long> pendingDestroy = new ArrayDeque<>();
        private final LongOpenHashSet queuedDestroy = new LongOpenHashSet();
        private final Queue<Long> pendingFluidCleanup = new ArrayDeque<>();
        private final LongOpenHashSet visitedFluidCleanup = new LongOpenHashSet();
        private final Map<Long, BitSet> destroyedBlockMasks = new HashMap<>();
        private final Queue<Long> pendingFluidProbeSections = new ArrayDeque<>();
        private final Queue<Long> pendingWaterloggedProbeSections = new ArrayDeque<>();
        private final Map<Long, ShortOpenHashSet> sectionUpdates = new HashMap<>();
        private final BlockingQueue<RayBatch> directionBatches =
                new ArrayBlockingQueue<>(MK5_RAY_DIRECTION_BATCHES_AHEAD);
        private final CompletableFuture<Void> directionPlanner;
        private CompletableFuture<List<Long>> sortFuture;
        private RayBatch activeBatch;
        private int activeBatchIndex;
        private RayScan activeRay;
        private DamagePulse activeDamagePulse;
        private boolean damageSweepRequested;
        private int damagePulseIndex;
        private boolean raysDone;
        private boolean terrainFinished;
        private boolean fluidBoundaryScanInitialized;
        private boolean hasActiveFluidProbeSection;
        private long activeFluidProbeSection;
        private int activeFluidProbeIndex;
        private boolean hasActiveWaterloggedProbeSection;
        private long activeWaterloggedProbeSection;
        private int activeWaterloggedProbeIndex;
        private boolean falloutScheduled;
        private volatile boolean cancelled;
        private boolean completionDelivered;

        private NukeTask(Vec3 center, int strength, int length, boolean radiationEnabled, int falloutScale,
                         EntityEffectMode entityEffectMode, Consumer<ServerLevel> completionAction) {
            this.center = center;
            this.rayCenter = legacyRayCenter(center);
            this.strength = strength;
            this.length = length;
            this.radiationEnabled = radiationEnabled;
            this.falloutScale = falloutScale;
            this.entityEffectMode = entityEffectMode;
            this.completionAction = completionAction;
            this.blockSampleCache.defaultReturnValue(CACHE_MISS);
            int mk3Mode = pendingMk3Mode;
            pendingMk3Mode = MK3_NONE;
            if (mk3Mode != MK3_NONE) {
                this.mk3 = new Mk3ColumnExplosion(center.x, center.y, center.z, length, mk3Mode == MK3_SOLINIUM);
                this.directionPlanner = CompletableFuture.completedFuture(null);
            } else {
                this.mk3 = null;
                this.directionPlanner = CompletableFuture.runAsync(this::createDirectionBatches, RAY_PLANNER);
            }
        }

        private final Mk3ColumnExplosion mk3;

        private boolean tick(ServerLevel level) {
            if (this.cancelled) {
                return true;
            }
            // The old MK5 runs one damage pass for every tick its terrain
            // explosion remains alive. Coalesce requests while a bounded pass
            // is still running: queuing one full snapshot per tick can build a
            // minutes-long backlog and make entities take the blast long after
            // detonation. The first pass was already completed synchronously
            // by scheduleInternal(), before this terrain task entered the
            // per-dimension queue.
            if (!this.terrainFinished) {
                this.damageSweepRequested = true;
            }
            processDamage(level);

            if (!this.terrainFinished && this.mk3 != null) {
                this.terrainFinished = this.mk3.tick(level);
            } else if (!this.terrainFinished) {
                if (!this.raysDone) {
                    scanRays(level, (int) (MK5_RAY_STEPS_PER_TICK * budgetShare));
                }
                collectSortedDestroyBatch();
                applyPendingDestroy(level, (int) (MK5_BLOCK_UPDATES_PER_TICK * budgetShare));
                boolean structuralDestructionFinished = this.raysDone
                        && this.pendingDestroy.isEmpty()
                        && this.sortFuture == null;
                if (structuralDestructionFinished) {
                    applyPendingFluidCleanup(level, (int) (MK5_BLOCK_UPDATES_PER_TICK * budgetShare));
                }
                flushSectionUpdates(level);
                this.terrainFinished = structuralDestructionFinished && fluidCleanupFinished();
                if (this.terrainFinished) {
                    this.visitedFluidCleanup.clear();
                    this.destroyedBlockMasks.clear();
                }
            }
            if (this.terrainFinished) {
                scheduleFallout(level);
            }
            return this.terrainFinished && this.activeDamagePulse == null && !this.damageSweepRequested;
        }

        private void complete(ServerLevel level) {
            if (!completionDelivered && completionAction != null) {
                completionDelivered = true;
                completionAction.accept(level);
            }
        }

        private void cancel() {
            this.cancelled = true;
            this.directionPlanner.cancel(true);
            if (this.sortFuture != null) {
                this.sortFuture.cancel(true);
                this.sortFuture = null;
            }
            // clear() wakes a producer waiting for capacity. The producer
            // also polls cancelled between batches, so it cannot refill an
            // abandoned queue and monopolize the single ray-planner thread.
            this.directionBatches.clear();
            this.activeBatch = null;
            this.activeRay = null;
            this.activeDamagePulse = null;
            this.pendingDestroy.clear();
            this.queuedDestroy.clear();
            this.pendingFluidCleanup.clear();
            this.visitedFluidCleanup.clear();
            this.destroyedBlockMasks.clear();
            this.pendingFluidProbeSections.clear();
            this.hasActiveFluidProbeSection = false;
            this.pendingWaterloggedProbeSections.clear();
            this.hasActiveWaterloggedProbeSection = false;
            this.sectionUpdates.clear();
            this.blockSampleCache.clear();
        }

        private void scheduleFallout(ServerLevel level) {
            if (this.falloutScheduled || this.falloutScale <= 0) {
                return;
            }
            this.falloutScheduled = true;
            BlockPos centerPos = BlockPos.containing(this.center);
            NuclearFalloutTerrainEffects.schedule(level, centerPos, this.falloutScale);
        }

        private void processDamage(ServerLevel level) {
            long deadline = System.nanoTime() + MK5_DAMAGE_NANOS_PER_TICK;
            int processed = 0;
            while (processed < MK5_DAMAGE_ENTITIES_PER_TICK && System.nanoTime() < deadline) {
                if (this.activeDamagePulse == null) {
                    if (!this.damageSweepRequested) {
                        return;
                    }
                    this.damageSweepRequested = false;
                    this.activeDamagePulse = DamagePulse.capture(
                            level,
                            this.center,
                            entityEffectRange(),
                            this.damagePulseIndex++
                    );
                    if (System.nanoTime() >= deadline) {
                        return;
                    }
                }

                Entity entity = this.activeDamagePulse.next();
                if (entity == null) {
                    this.activeDamagePulse = null;
                    continue;
                }
                processed++;
                applyLegacyEntityEffect(level, entity, this.activeDamagePulse.index());
            }
        }

        private void applyInitialDamage(ServerLevel level) {
            DamagePulse initial = DamagePulse.capture(level, this.center, entityEffectRange(), 0);
            Entity entity;
            while ((entity = initial.next()) != null) {
                applyLegacyEntityEffect(level, entity, 0);
            }
            this.damagePulseIndex = 1;
        }

        private double entityEffectRange() {
            return this.entityEffectMode == EntityEffectMode.SOLINIUM_RADIATION
                    ? this.length
                    : this.length * 2.0D;
        }

        private void applyLegacyEntityEffect(ServerLevel level, Entity entity, int pulseIndex) {
            if (this.entityEffectMode == EntityEffectMode.SOLINIUM_RADIATION) {
                applySoliniumRadiation(entity);
            } else {
                // [parity K2: nuke] EntityNukeExplosionMK5.onUpdate radiates before dealDamage, over the whole
                // AABB of length * 2 (no sphere check, no exemptions besides ContaminationUtil).
                if (entity instanceof LivingEntity living) {
                    applyPromptRadiation(level, living, pulseIndex);
                }
                applyLegacyDamage(level, entity, pulseIndex);
            }
        }

        /**
         * [parity K2: nuke] ExplosionHurtUtil.doRadiation(world, x, y, z, 15000, 250000, destructionRange)
         * (EntityNukeExplosionMK3 solinium): contaminate(RADIATION, CREATIVE, outer + (inner - outer) * (1 - d / r)).
         */
        private void applySoliniumRadiation(Entity entity) {
            soliniumRadiation(this.center, this.length, entity);
        }

        static void soliniumRadiation(Vec3 center, double radius, Entity entity) {
            if (!(entity instanceof LivingEntity living)) {
                return;
            }
            double distance = Math.sqrt(living.distanceToSqr(center));
            if (distance > radius) {
                return;
            }
            double interpolation = 1.0D - distance / radius;
            float amount = (float) (15000F + (250000F - 15000F) * interpolation);
            com.atomiclegacy.util.ContaminationUtil.contaminate(living,
                    com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                    com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, amount);
        }

        private void applyLegacyDamage(ServerLevel level, Entity entity, int pulseIndex) {
            if (!entity.isAlive() || isLegacyExplosionExempt(entity)) {
                return;
            }
            double range = this.length * 2.0D;
            double distance = Math.sqrt(entity.distanceToSqr(this.center));
            if (distance > range) {
                return;
            }
            if (isObstructed(level, entity)) {
                return;
            }

            float damage = (float) (MK5_MAX_DAMAGE * (range - distance) / range);
            // EntityDamageUtil.attackEntityFromNT(..., ignoreIFrame=true) in
            // 1.7.10 explicitly allowed every MK5 pulse through hurt immunity.
            entity.invulnerableTime = 0;
            boolean hurt = entity.hurt(level.damageSources().source(HbmDamageTypes.NUCLEAR_BLAST), damage);
            entity.invulnerableTime = 0;
            entity.igniteForSeconds(5.0F);

            if (hurt) {
                Vec3 knockback = new Vec3(
                        entity.getX() - this.center.x,
                        entity.getEyeY() - this.center.y,
                        entity.getZ() - this.center.z
                );
                if (knockback.lengthSqr() > 0.0D) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.normalize().scale(0.2D)));
                    entity.hurtMarked = true;
                }
            }
        }

        private boolean isObstructed(ServerLevel level, Entity entity) {
            Vec3 target = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            return level.clip(new ClipContext(
                    this.center,
                    target,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
            )).getType() != HitResult.Type.MISS;
        }

        /**
         * [parity K2: nuke] EntityNukeExplosionMK5.radiate: while fallout is on, strength >= 75 and ticksExisted < 10
         * with the ray explosion already created (ticks 2..9; tick 1 is the initial pulse 0, so pulse p = tick p + 1)
         * every living entity gets contaminate(RADIATION, RAD_BYPASS, 2 500 000 / (ticks * 5 + 1) / res / len^2),
         * res = sum of the explosion resistance along the ray (at least 1), len = distance to the eyes.
         */
        private void applyPromptRadiation(ServerLevel level, LivingEntity entity, int pulseIndex) {
            promptRadiation(level, this.center, this.strength, this.radiationEnabled, entity, pulseIndex);
        }

        static void promptRadiation(ServerLevel level, Vec3 center, int strength, boolean radiationEnabled,
                                    LivingEntity entity, int pulseIndex) {
            int ticksExisted = pulseIndex + 1;
            if (!radiationEnabled || strength < 75 || ticksExisted < 2 || ticksExisted >= 10) {
                return;
            }
            float rads = 2_500_000F / (ticksExisted * 5 + 1);
            Vec3 vec = new Vec3(entity.getX() - center.x, entity.getEyeY() - center.y,
                    entity.getZ() - center.z);
            double len = vec.length();
            vec = vec.normalize();
            float res = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int i = 1; i < len; i++) {
                cursor.set((int) Math.floor(center.x + vec.x * i), (int) Math.floor(center.y + vec.y * i),
                        (int) Math.floor(center.z + vec.z * i));
                res += com.atomiclegacy.blocks.LegacyCoreProxy.blockResistance(level, cursor, level.getBlockState(cursor)); // [parity U3: dummy parts]
            }
            if (res < 1) {
                res = 1;
            }
            float eRads = rads;
            eRads /= res;
            eRads /= (float) (len * len);
            com.atomiclegacy.util.ContaminationUtil.contaminate(entity,
                    com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                    com.atomiclegacy.util.ContaminationUtil.ContaminationType.RAD_BYPASS, eRads);
        }

        /** Diagnostics hook: the MK5 prompt dose for one entity at the given pulse. */
        static void testPromptRadiation(ServerLevel level, Vec3 center, int strength, LivingEntity entity, int pulseIndex) {
            promptRadiation(level, center, strength, true, entity, pulseIndex);
        }

        private static boolean isLegacyExplosionExempt(Entity entity) {
            return entity instanceof Cat
                    || entity instanceof Ocelot
                    || entity instanceof Player player && (player.isCreative() || player.isSpectator())
                    || entity instanceof JeremyShellEntity
                    || entity instanceof LegacyBulletEntity
                    || entity instanceof com.atomiclegacy.entity.B92BeamEntity // [parity c1] ExplosionNukeGeneric: EntityB92Beam
                    || entity instanceof ChekhovBulletEntity
                    || entity instanceof LegacyBossProjectileEntity
                    || entity instanceof LegacyArtilleryShellEntity
                    || entity instanceof LegacyHimarsRocketEntity
                    || entity instanceof LegacyShrapnelEntity
                    || entity instanceof LegacyGrenadeEntity
                    || entity instanceof UniversalGrenadeEntity;
        }

        private static final class DamagePulse {
            private final List<Entity> entities;
            private final int index;
            private int cursor;

            private DamagePulse(List<Entity> entities, int index) {
                this.entities = entities;
                this.index = index;
            }

            private static DamagePulse capture(ServerLevel level, Vec3 center, double range, int index) {
                AABB area = new AABB(center, center).inflate(range);
                return new DamagePulse(level.getEntities((Entity) null, area, Entity::isAlive), index);
            }

            private Entity next() {
                return this.cursor < this.entities.size() ? this.entities.get(this.cursor++) : null;
            }

            private int index() {
                return this.index;
            }
        }

        private void scanRays(ServerLevel level, int maxSteps) {
            if (this.directionPlanner.isCompletedExceptionally() || this.directionPlanner.isCancelled()) {
                AtomicLegacy.LOGGER.error("Nuke ray planner failed; cancelling terrain destruction at {}", this.center);
                this.raysDone = true;
                this.blockSampleCache.clear();
                this.pendingDestroy.clear();
                this.queuedDestroy.clear();
                this.pendingFluidCleanup.clear();
                this.visitedFluidCleanup.clear();
                this.destroyedBlockMasks.clear();
                this.pendingFluidProbeSections.clear();
                this.hasActiveFluidProbeSection = false;
                this.pendingWaterloggedProbeSections.clear();
                this.hasActiveWaterloggedProbeSection = false;
                this.sortFuture = null;
                return;
            }
            long deadline = System.nanoTime() + (long) (mk5Nanos() * budgetShare);
            int steps = 0;
            while (steps < maxSteps) {
                if ((steps & 63) == 0 && System.nanoTime() >= deadline) {
                    return;
                }
                if (this.activeRay == null) {
                    if (!startNextRay()) {
                        if (allRayDirectionsConsumed()) {
                            this.raysDone = true;
                            this.blockSampleCache.clear();
                            flushDestroySort();
                        }
                        return;
                    }
                }
                steps += this.activeRay.step(level, this.rayCenter, this.blockSampleCache);
                if (this.activeRay.done()) {
                    this.activeRay = null;
                }
                if (this.pendingDestroy.size() >= MK5_DESTROY_BATCH_SIZE) {
                    flushDestroySort();
                }
            }
        }

        private boolean startNextRay() {
            while (true) {
                if (this.activeBatch != null && this.activeBatchIndex < this.activeBatch.count()) {
                    int index = this.activeBatchIndex++;
                    this.activeRay = new RayScan(
                            this.activeBatch.xs()[index],
                            this.activeBatch.ys()[index],
                            this.activeBatch.zs()[index],
                            this.strength,
                            this.length);
                    return true;
                }
                this.activeBatch = this.directionBatches.poll();
                this.activeBatchIndex = 0;
                if (this.activeBatch == null) {
                    return false;
                }
            }
        }

        private boolean allRayDirectionsConsumed() {
            return this.directionPlanner.isDone()
                    && this.activeBatch == null
                    && this.directionBatches.isEmpty();
        }

        private void applyPendingDestroy(ServerLevel level, int maxUpdates) {
            int checked = 0;
            long deadline = System.nanoTime() + (long) (mk5Nanos() * budgetShare);
            while (checked < maxUpdates && !this.pendingDestroy.isEmpty()) {
                if ((checked & 7) == 0 && System.nanoTime() >= deadline) {
                    return;
                }
                long pos = this.pendingDestroy.remove();
                this.queuedDestroy.remove(pos);
                fastDestroyCollected(level, pos);
                checked++;
            }
        }

        /**
         * Direct section writes deliberately avoid millions of neighbour
         * updates, but that also means a liquid source between two rays can
         * remain suspended after the surrounding terrain is gone. Keep an
         * exact per-section mask of blocks this blast really removed, scan
         * those sections and their six face neighbours, then seed the
         * connected cleanup from surviving liquids on that boundary.
         *
         * Direct liquid in an intact sealed vessel is not a boundary seed.
         * Waterlogged blocks are a separate case: the complete block must be
         * removed anywhere inside the exact blast sphere, including positions
         * missed between destruction rays. Loaded sphere sections are therefore
         * palette-filtered for waterlogged states before any block-level scan.
         */
        private void applyPendingFluidCleanup(ServerLevel level, int maxUpdates) {
            if (!this.fluidBoundaryScanInitialized) {
                initializeFluidBoundaryScan(level);
            }

            int cleaned = 0;
            int scanned = 0;
            long deadline = System.nanoTime() + (long) (mk5Nanos() * budgetShare);
            while (cleaned < maxUpdates && scanned < MK5_RAY_STEPS_PER_TICK) {
                if (((cleaned + scanned) & 7) == 0 && System.nanoTime() >= deadline) {
                    return;
                }

                if (!this.pendingFluidCleanup.isEmpty()) {
                    BlockPos origin = BlockPos.of(this.pendingFluidCleanup.remove());
                    for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                        BlockPos adjacent = origin.relative(direction);
                        if (!intersectsExplosionSphere(adjacent)) {
                            continue;
                        }
                        long packed = adjacent.asLong();
                        if (this.visitedFluidCleanup.contains(packed)) {
                            continue;
                        }
                        if (fastDestroyFluid(level, adjacent)) {
                            this.visitedFluidCleanup.add(packed);
                            this.pendingFluidCleanup.add(packed);
                        }
                    }
                    cleaned++;
                    continue;
                }

                if (!this.hasActiveFluidProbeSection && !this.pendingFluidProbeSections.isEmpty()) {
                    Long sectionKey = this.pendingFluidProbeSections.poll();
                    this.activeFluidProbeSection = sectionKey;
                    this.activeFluidProbeIndex = 0;
                    this.hasActiveFluidProbeSection = true;
                }

                if (this.hasActiveFluidProbeSection) {
                    SectionPos sectionPos = SectionPos.of(this.activeFluidProbeSection);
                    LevelChunk chunk = level.getChunkSource().getChunkNow(sectionPos.x(), sectionPos.z());
                    int sectionIndex = level.getSectionIndex(sectionPos.minBlockY());
                    if (chunk == null || sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                        finishActiveFluidProbeSection();
                        continue;
                    }
                    LevelChunkSection section = chunk.getSection(sectionIndex);
                    if (this.activeFluidProbeIndex == 0
                            && !section.maybeHas(state -> state.getBlock() instanceof LiquidBlock)) {
                        finishActiveFluidProbeSection();
                        continue;
                    }

                    int localIndex = this.activeFluidProbeIndex++;
                    scanned++;
                    int localX = localIndex & 15;
                    int localZ = (localIndex >>> 4) & 15;
                    int localY = (localIndex >>> 8) & 15;
                    BlockState state = section.getBlockState(localX, localY, localZ);
                    if (state.getBlock() instanceof LiquidBlock) {
                        int x = sectionPos.minBlockX() + localX;
                        int y = sectionPos.minBlockY() + localY;
                        int z = sectionPos.minBlockZ() + localZ;
                        BlockPos fluidPos = new BlockPos(x, y, z);
                        if (intersectsExplosionSphere(fluidPos)
                                && (wasDestroyedByBlast(x, y, z) || touchesDestroyedBlastBlock(x, y, z))) {
                            long packed = fluidPos.asLong();
                            if (this.visitedFluidCleanup.add(packed)) {
                                if (fastDestroyFluid(level, fluidPos)) {
                                    this.pendingFluidCleanup.add(packed);
                                    cleaned++;
                                } else {
                                    this.visitedFluidCleanup.remove(packed);
                                }
                            }
                        }
                    }

                    if (this.activeFluidProbeIndex >= 16 * 16 * 16) {
                        finishActiveFluidProbeSection();
                    }
                    continue;
                }

                if (!this.hasActiveWaterloggedProbeSection && !this.pendingWaterloggedProbeSections.isEmpty()) {
                    Long sectionKey = this.pendingWaterloggedProbeSections.poll();
                    this.activeWaterloggedProbeSection = sectionKey;
                    this.activeWaterloggedProbeIndex = 0;
                    this.hasActiveWaterloggedProbeSection = true;
                }
                if (!this.hasActiveWaterloggedProbeSection) {
                    return;
                }

                SectionPos sectionPos = SectionPos.of(this.activeWaterloggedProbeSection);
                LevelChunk chunk = level.getChunkSource().getChunkNow(sectionPos.x(), sectionPos.z());
                int sectionIndex = level.getSectionIndex(sectionPos.minBlockY());
                if (chunk == null || sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                    finishActiveWaterloggedProbeSection();
                    continue;
                }
                LevelChunkSection section = chunk.getSection(sectionIndex);
                if (this.activeWaterloggedProbeIndex == 0
                        && !section.maybeHas(NukeTask::isWaterlogged)) {
                    finishActiveWaterloggedProbeSection();
                    continue;
                }

                int localIndex = this.activeWaterloggedProbeIndex++;
                scanned++;
                int localX = localIndex & 15;
                int localZ = (localIndex >>> 4) & 15;
                int localY = (localIndex >>> 8) & 15;
                BlockState state = section.getBlockState(localX, localY, localZ);
                if (isWaterlogged(state)) {
                    BlockPos waterloggedPos = new BlockPos(
                            sectionPos.minBlockX() + localX,
                            sectionPos.minBlockY() + localY,
                            sectionPos.minBlockZ() + localZ);
                    if (intersectsExplosionSphere(waterloggedPos)) {
                        long packed = waterloggedPos.asLong();
                        if (this.visitedFluidCleanup.add(packed)) {
                            if (fastDestroyFluid(level, waterloggedPos)) {
                                this.pendingFluidCleanup.add(packed);
                                cleaned++;
                            } else {
                                this.visitedFluidCleanup.remove(packed);
                            }
                        }
                    }
                }

                if (this.activeWaterloggedProbeIndex >= 16 * 16 * 16) {
                    finishActiveWaterloggedProbeSection();
                }
            }
        }

        private void initializeFluidBoundaryScan(ServerLevel level) {
            this.fluidBoundaryScanInitialized = true;
            LongOpenHashSet candidateSections = new LongOpenHashSet();
            for (long sectionKey : this.destroyedBlockMasks.keySet()) {
                SectionPos sectionPos = SectionPos.of(sectionKey);
                int sectionX = sectionPos.x();
                int sectionY = sectionPos.y();
                int sectionZ = sectionPos.z();
                candidateSections.add(sectionKey);
                candidateSections.add(SectionPos.asLong(sectionX - 1, sectionY, sectionZ));
                candidateSections.add(SectionPos.asLong(sectionX + 1, sectionY, sectionZ));
                candidateSections.add(SectionPos.asLong(sectionX, sectionY - 1, sectionZ));
                candidateSections.add(SectionPos.asLong(sectionX, sectionY + 1, sectionZ));
                candidateSections.add(SectionPos.asLong(sectionX, sectionY, sectionZ - 1));
                candidateSections.add(SectionPos.asLong(sectionX, sectionY, sectionZ + 1));
            }
            for (long sectionKey : candidateSections) {
                this.pendingFluidProbeSections.add(sectionKey);
            }

            int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(this.center.x - this.length) - 1);
            int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(this.center.x + this.length));
            int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(this.center.z - this.length) - 1);
            int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(this.center.z + this.length));
            int minSectionY = SectionPos.blockToSectionCoord(Math.max(
                    level.getMinBuildHeight(), Mth.floor(this.center.y - this.length) - 1));
            int maxSectionY = SectionPos.blockToSectionCoord(Math.min(
                    level.getMaxBuildHeight() - 1, Mth.floor(this.center.y + this.length)));
            for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    if (level.getChunkSource().getChunkNow(sectionX, sectionZ) == null) {
                        continue;
                    }
                    for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                        if (sectionIntersectsExplosionSphere(sectionX, sectionY, sectionZ)) {
                            this.pendingWaterloggedProbeSections.add(SectionPos.asLong(sectionX, sectionY, sectionZ));
                        }
                    }
                }
            }
        }

        private void finishActiveFluidProbeSection() {
            this.hasActiveFluidProbeSection = false;
            this.activeFluidProbeIndex = 0;
        }

        private void finishActiveWaterloggedProbeSection() {
            this.hasActiveWaterloggedProbeSection = false;
            this.activeWaterloggedProbeIndex = 0;
        }

        private boolean fluidCleanupFinished() {
            return this.fluidBoundaryScanInitialized
                    && this.pendingFluidCleanup.isEmpty()
                    && this.pendingFluidProbeSections.isEmpty()
                    && !this.hasActiveFluidProbeSection
                    && this.pendingWaterloggedProbeSections.isEmpty()
                    && !this.hasActiveWaterloggedProbeSection;
        }

        private boolean touchesDestroyedBlastBlock(int x, int y, int z) {
            return wasDestroyedByBlast(x - 1, y, z)
                    || wasDestroyedByBlast(x + 1, y, z)
                    || wasDestroyedByBlast(x, y - 1, z)
                    || wasDestroyedByBlast(x, y + 1, z)
                    || wasDestroyedByBlast(x, y, z - 1)
                    || wasDestroyedByBlast(x, y, z + 1);
        }

        private boolean wasDestroyedByBlast(int x, int y, int z) {
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(x),
                    SectionPos.blockToSectionCoord(y),
                    SectionPos.blockToSectionCoord(z));
            BitSet destroyed = this.destroyedBlockMasks.get(sectionKey);
            return destroyed != null && destroyed.get(localBlockIndex(x, y, z));
        }

        private void recordDestroyedBlock(long sectionKey, BlockPos pos) {
            this.destroyedBlockMasks.computeIfAbsent(sectionKey, unused -> new BitSet(16 * 16 * 16))
                    .set(localBlockIndex(pos.getX(), pos.getY(), pos.getZ()));
        }

        private static int localBlockIndex(int x, int y, int z) {
            return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        }

        /** True when the unit block volume intersects the exact blast sphere. */
        private boolean intersectsExplosionSphere(BlockPos pos) {
            double dx = Math.max(0.0D, Math.abs(this.center.x - (pos.getX() + 0.5D)) - 0.5D);
            double dy = Math.max(0.0D, Math.abs(this.center.y - (pos.getY() + 0.5D)) - 0.5D);
            double dz = Math.max(0.0D, Math.abs(this.center.z - (pos.getZ() + 0.5D)) - 0.5D);
            return dx * dx + dy * dy + dz * dz <= (double) this.length * this.length;
        }

        /** True when the complete section volume intersects the exact blast sphere. */
        private boolean sectionIntersectsExplosionSphere(int sectionX, int sectionY, int sectionZ) {
            double minX = SectionPos.sectionToBlockCoord(sectionX);
            double minY = SectionPos.sectionToBlockCoord(sectionY);
            double minZ = SectionPos.sectionToBlockCoord(sectionZ);
            double dx = distanceToInterval(this.center.x, minX, minX + 16.0D);
            double dy = distanceToInterval(this.center.y, minY, minY + 16.0D);
            double dz = distanceToInterval(this.center.z, minZ, minZ + 16.0D);
            return dx * dx + dy * dy + dz * dz <= (double) this.length * this.length;
        }

        private static double distanceToInterval(double value, double min, double max) {
            return value < min ? min - value : value > max ? value - max : 0.0D;
        }

        /**
         * A non-liquid block that holds a fluid: WATERLOGGED=true states and the 1.21 plants that are always
         * water-filled without that property (seagrass, tall seagrass, kelp, bubble columns). 1.7.10 has no such
         * blocks; left behind they would stand in the crater as cubes of water.
         */
        private static boolean isWaterlogged(BlockState state) {
            return !state.getFluidState().isEmpty() && !(state.getBlock() instanceof LiquidBlock);
        }

        private void flushSectionUpdates(ServerLevel level) {
            if (this.sectionUpdates.isEmpty()) {
                return;
            }
            for (Map.Entry<Long, ShortOpenHashSet> entry : List.copyOf(this.sectionUpdates.entrySet())) {
                long sectionKey = entry.getKey();
                ShortSet positions = entry.getValue();
                if (positions.isEmpty()) {
                    this.sectionUpdates.remove(sectionKey);
                    continue;
                }
                SectionPos sectionPos = SectionPos.of(sectionKey);
                LevelChunk chunk = level.getChunkSource().getChunkNow(sectionPos.chunk().x, sectionPos.chunk().z);
                if (chunk == null) {
                    continue;
                }
                int sectionIndex = level.getSectionIndex(sectionPos.minBlockY());
                if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                    this.sectionUpdates.remove(sectionKey);
                    continue;
                }
                LevelChunkSection section = chunk.getSection(sectionIndex);
                ClientboundSectionBlocksUpdatePacket packet = new ClientboundSectionBlocksUpdatePacket(sectionPos, positions, section);
                for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(sectionPos.chunk(), false)) {
                    player.connection.send(packet);
                }
                this.sectionUpdates.remove(sectionKey);
            }
        }

        private void flushDestroySort() {
            if (this.sortFuture != null || this.pendingDestroy.isEmpty()) {
                return;
            }
            List<Long> batch = new ArrayList<>(this.pendingDestroy.size());
            while (!this.pendingDestroy.isEmpty()) {
                batch.add(this.pendingDestroy.remove());
            }
            int centerChunkX = BlockPos.containing(this.center).getX() >> 4;
            int centerChunkZ = BlockPos.containing(this.center).getZ() >> 4;
            this.sortFuture = CompletableFuture.supplyAsync(() -> sortByChunkDistance(batch, centerChunkX, centerChunkZ), PLANNER);
        }

        private void collectSortedDestroyBatch() {
            if (this.sortFuture == null || !this.sortFuture.isDone()) {
                return;
            }
            if (this.sortFuture.isCompletedExceptionally() || this.sortFuture.isCancelled()) {
                AtomicLegacy.LOGGER.error("Nuke destruction sorter failed; continuing unsorted at {}", this.center);
                this.sortFuture = null;
                return;
            }
            this.pendingDestroy.addAll(this.sortFuture.join());
            this.sortFuture = null;
        }

        private List<Long> sortByChunkDistance(List<Long> batch, int centerChunkX, int centerChunkZ) {
            batch.sort((a, b) -> {
                BlockPos posA = BlockPos.of(a);
                BlockPos posB = BlockPos.of(b);
                int distA = Math.abs((posA.getX() >> 4) - centerChunkX) + Math.abs((posA.getZ() >> 4) - centerChunkZ);
                int distB = Math.abs((posB.getX() >> 4) - centerChunkX) + Math.abs((posB.getZ() >> 4) - centerChunkZ);
                return Integer.compare(distA, distB);
            });
            return batch;
        }

        private boolean destroyCollected(ServerLevel level, long packedPos, boolean tip) {
            BlockPos pos = BlockPos.of(packedPos);
            if (!level.isLoaded(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            // [parity U3: nuke threshold] ExplosionNukeRayBatched.processChunk removes every non-air block on the
            // collected ray; bedrock and other very resistant blocks only survive because the ray stops at them.
            if (state.isAir()) {
                return false;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), tip ? 3 : 2);
            HbmRadiationWorlds.invalidateResistance(level, pos);
            return true;
        }

        private boolean fastDestroyCollected(ServerLevel level, long packedPos) {
            BlockPos pos = BlockPos.of(packedPos);
            LevelChunk chunk = level.getChunkAt(pos);
            int sectionIndex = level.getSectionIndex(pos.getY());
            if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                return false;
            }
            LevelChunkSection section = chunk.getSection(sectionIndex);
            BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            // [parity U3: nuke threshold] ExplosionNukeRayBatched.processChunk removes every non-air block on the
            // collected ray; bedrock and other very resistant blocks only survive because the ray stops at them.
            if (state.isAir()) {
                return false;
            }
            boolean liquid = state.getBlock() instanceof LiquidBlock;
            if (state.hasBlockEntity()) {
                chunk.removeBlockEntity(pos);
            }
            section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, Blocks.AIR.defaultBlockState(), false);
            chunk.setUnsaved(true);
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ())
            );
            this.sectionUpdates.computeIfAbsent(sectionKey, unused -> new ShortOpenHashSet()).add(SectionPos.sectionRelativePos(pos));
            HbmRadiationWorlds.invalidateResistanceSection(level, sectionKey);
            recordDestroyedBlock(sectionKey, pos);
            if (liquid && this.visitedFluidCleanup.add(packedPos)) {
                this.pendingFluidCleanup.add(packedPos);
            }
            return true;
        }

        private boolean fastDestroyFluid(ServerLevel level, BlockPos pos) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
            if (chunk == null) {
                return false;
            }
            int sectionIndex = level.getSectionIndex(pos.getY());
            if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                return false;
            }
            LevelChunkSection section = chunk.getSection(sectionIndex);
            BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            // A waterlogged state is removed as one complete block: the blast
            // must not leave the solid shell behind after evaporating its water.
            if (!(state.getBlock() instanceof LiquidBlock) && !isWaterlogged(state)) {
                return false;
            }
            if (state.hasBlockEntity()) {
                chunk.removeBlockEntity(pos);
            }
            section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15,
                    Blocks.AIR.defaultBlockState(), false);
            chunk.setUnsaved(true);
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ())
            );
            this.sectionUpdates.computeIfAbsent(sectionKey, unused -> new ShortOpenHashSet())
                    .add(SectionPos.sectionRelativePos(pos));
            HbmRadiationWorlds.invalidateResistanceSection(level, sectionKey);
            return true;
        }

        private float masqueradeResistance(ServerLevel level, BlockPos pos, BlockState state) {
            if (state.is(Blocks.SANDSTONE)) {
                return Blocks.STONE.getExplosionResistance();
            }
            if (state.is(Blocks.OBSIDIAN)) {
                return Blocks.STONE.getExplosionResistance() * 3.0F;
            }
            return com.atomiclegacy.blocks.LegacyCoreProxy.blockResistance(level, pos, state); // [parity U3: dummy parts]
        }

        private static long encodeSample(BlockSample sample) {
            long encoded = Float.floatToRawIntBits(sample.resistance()) & 0xffffffffL;
            if (sample.air()) {
                encoded |= SAMPLE_AIR;
            }
            if (sample.fluidEmpty()) {
                encoded |= SAMPLE_FLUID_EMPTY;
            }
            return encoded;
        }

        private static BlockSample decodeSample(long encoded) {
            float resistance = Float.intBitsToFloat((int) (encoded & 0xffffffffL));
            return new BlockSample(resistance, (encoded & SAMPLE_AIR) != 0L, (encoded & SAMPLE_FLUID_EMPTY) != 0L);
        }

        private BlockSample sampleBlock(ServerLevel level, BlockPos.MutableBlockPos cursor, Long2LongOpenHashMap cache, int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            long encoded = cache.get(key);
            if (encoded != CACHE_MISS) {
                return decodeSample(encoded);
            }
            BlockState state = Blocks.AIR.defaultBlockState();
            int sectionIndex = level.getSectionIndex(y);
            if (sectionIndex >= 0 && sectionIndex < level.getSectionsCount()) {
                // An unwatched chunk still contains terrain when a remote warhead detonates.
                LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
                state = chunk.getSection(sectionIndex).getBlockState(x & 15, y & 15, z & 15);
            }
            BlockSample sample = new BlockSample(masqueradeResistance(level, cursor, state), state.isAir(), state.getFluidState().isEmpty());
            cache.put(key, encodeSample(sample));
            return sample;
        }

        private record BlockSample(float resistance, boolean air, boolean fluidEmpty) {
        }

        private void createDirectionBatches() {
            int radius = this.length;
            int max = (int) (2.5D * Math.PI * Math.pow(radius, 2.0D));
            double[] xs = new double[Math.min(MK5_RAY_DIRECTION_BATCH_SIZE, Math.max(1, max))];
            double[] ys = new double[xs.length];
            double[] zs = new double[xs.length];
            int count = 0;
            double gspX = Math.PI;
            double gspY = 0.0D;
            for (int gspNum = 1; gspNum <= max; gspNum++) {
                if (this.cancelled) {
                    return;
                }
                xs[count] = Math.sin(gspX) * Math.cos(gspY);
                zs[count] = Math.sin(gspX) * Math.sin(gspY);
                ys[count] = Math.cos(gspX);
                count++;
                if (count >= xs.length) {
                    if (!offerRayBatch(new RayBatch(xs, ys, zs, count))) {
                        return;
                    }
                    xs = new double[Math.min(MK5_RAY_DIRECTION_BATCH_SIZE, Math.max(1, max - gspNum))];
                    ys = new double[xs.length];
                    zs = new double[xs.length];
                    count = 0;
                }
                if (gspNum < max) {
                    int k = gspNum + 1;
                    double hk = -1.0D + 2.0D * (k - 1.0D) / (max - 1.0D);
                    gspX = Math.acos(hk);
                    double longitude = gspY + 3.6D / Math.sqrt(max) / Math.sqrt(Math.max(1.0E-12D, 1.0D - hk * hk));
                    gspY = longitude % (Math.PI * 2.0D);
                }
            }
            if (count > 0 && !this.cancelled) {
                offerRayBatch(new RayBatch(xs, ys, zs, count));
            }
        }

        private boolean offerRayBatch(RayBatch batch) {
            while (!this.cancelled) {
                try {
                    if (this.directionBatches.offer(batch, 100L, TimeUnit.MILLISECONDS)) {
                        return true;
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        private record RayBatch(double[] xs, double[] ys, double[] zs, int count) {
        }

        private final class RayScan {
            private final double rayX;
            private final double rayY;
            private final double rayZ;
            private final int rayLength;
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private float resistanceBudget;
            private int step;
            private boolean done;
            // [parity V4: nuke tip] collectTip keeps only lastPos; processChunk re-traces i < |lastPos - pos| (float
            // lastPos), so the tip itself is removed only when that float length exceeds its step.
            private long tipPos;
            private int tipStep = -1;

            private RayScan(double rayX, double rayY, double rayZ, int strength, int length) {
                this.rayX = rayX;
                this.rayY = rayY;
                this.rayZ = rayZ;
                this.rayLength = (int) Math.ceil(strength);
                this.resistanceBudget = strength;
            }

            private int step(ServerLevel level, Vec3 center, Long2LongOpenHashMap cache) {
                if (this.done) {
                    return 0;
                }
                if (this.step > length || this.step >= this.rayLength) {
                    finish(center);
                    return 0;
                }
                double x = center.x + this.rayX * this.step;
                double y = center.y + this.rayY * this.step;
                double z = center.z + this.rayZ * this.step;
                int ix = (int) Math.floor(x);
                int iy = (int) Math.floor(y);
                int iz = (int) Math.floor(z);
                if (iy < level.getMinBuildHeight() || iy >= level.getMaxBuildHeight()) {
                    finish(center);
                    return 0;
                }
                this.cursor.set(ix, iy, iz);
                BlockSample sample = sampleBlock(level, this.cursor, cache, ix, iy, iz);
                double factor = 100.0D - (double) this.step / (double) this.rayLength * 100.0D;
                factor *= 0.07D;
                if (sample.fluidEmpty()) {
                    this.resistanceBudget -= Math.pow(sample.resistance(), 7.5D - factor);
                }
                if (this.resistanceBudget > 0.0F && !sample.air()) {
                    if (this.tipStep >= 0) {
                        queue(this.tipPos);
                    }
                    this.tipPos = BlockPos.asLong(ix, iy, iz);
                    this.tipStep = this.step;
                }
                if (this.resistanceBudget <= 0.0F || this.step + 1 >= length || this.step + 1 >= this.rayLength) {
                    finish(center);
                }
                this.step++;
                return 1;
            }

            private void queue(long packed) {
                if (NukeTask.this.queuedDestroy.add(packed)) {
                    NukeTask.this.pendingDestroy.add(packed);
                }
            }

            private void finish(Vec3 center) {
                this.done = true;
                if (this.tipStep < 0) {
                    return;
                }
                double dx = (float) (center.x + this.rayX * this.tipStep) - center.x;
                double dy = (float) (center.y + this.rayY * this.tipStep) - center.y;
                double dz = (float) (center.z + this.rayZ * this.tipStep) - center.z;
                if (this.tipStep < Math.sqrt(dx * dx + dy * dy + dz * dz)) {
                    queue(this.tipPos);
                }
                this.tipStep = -1;
            }

            private boolean done() {
                return this.done;
            }
        }
    }
}

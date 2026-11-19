package com.atomiclegacy.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.LegacyTurretBlockEntity;
import com.atomiclegacy.tileentity.LegacyTurretType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;

/**
 * [parity P3] Dedicated-server checks of turrets, artillery and mobs against the pinned 1.7.10 sources
 * (tileentity/turret/*, XFactoryTurret, entity/mob/*, GlyphidStats.GlyphidStatsNT, EntityMappings.addSpawn,
 * BossSpawnHandler, MobConfig, WeaponRecipes). Turrets are placed on a spawn-chunk pad (see pad()) and ticked directly.
 * Not covered: client rendering/animations/sounds, GUI drawing, projectile flight after spawning.
 */
final class ParityP3Diagnostics {

    private ParityP3Diagnostics() {
    }


    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        List<int[]> forced = new ArrayList<>();
        try {
            turretTable(problems, cases);
            turretAmmo(problems, cases);
            turretConfig(problems, cases);
            turretBehaviour(level, forced, problems, cases);
            mobAttributes(level, problems, cases);
            mobDrops(level, forced, problems, cases);
            spawnRules(server, problems, cases);
            recipes(server, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity P3 diagnostics crashed", e);
            problems.add("P3 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : forced) level.setChunkForced(chunk[0], chunk[1], false);
        }
        return cases[0];
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String what) {
        cases[0]++;
        if (!ok) problems.add("P3: " + what);
    }

    // --- turrets: TileEntityTurret* getters (maxPower, consumption, heightOffset, barrel, range, grace, yaw, pitch,
    // elevation, depression, inaccuracy, fire delay, detector interval) ---
    private static void turretTable(List<String> problems, int[] cases) {
        Object[][] expected = {
                {LegacyTurretType.FRIENDLY, 10_000L, 100L, 1.5, 3.5, 32.0, 3.0, 4.5, 3.0, 45.0, 30.0, 15.0, 5, 10},
                {LegacyTurretType.FRITZ, 10_000L, 100L, 1.5, 2.25, 48.0, 2.0, 4.5, 3.0, 45.0, 30.0, 15.0, 1, 10},
                {LegacyTurretType.HOWARD, 50_000L, 500L, 2.25, 3.25, 250.0, 3.0, 12.0, 8.0, 90.0, 50.0, 5.0, 1, 10},
                {LegacyTurretType.HOWARD_DAMAGED, 50_000L, 500L, 2.25, 3.25, 16.0, 5.0, 3.0, 2.0, 90.0, 50.0, 5.0, 4, 10},
                {LegacyTurretType.MAXWELL, 10_000_000L, 10_000L, 2.0, 2.125, 64.0, 5.0, 9.0, 6.0, 40.0, 35.0, 2.0, 1, 10},
                {LegacyTurretType.RICHARD, 10_000L, 100L, 1.5, 1.25, 64.0, 8.0, 4.5, 3.0, 25.0, 25.0, 5.0, 10, 10},
                {LegacyTurretType.TAUON, 100_000L, 1_000L, 1.5, 1.9375, 128.0, 3.0, 9.0, 6.0, 35.0, 35.0, 5.0, 5, 10},
                {LegacyTurretType.HIMARS, 1_000_000L, 100L, 5.0, 0.5, 5000.0, 250.0, 1.0, 0.5, 30.0, 30.0, 5.0, 40, 10},
                {LegacyTurretType.SENTRY, 1_000L, 5L, 1.5, 1.25, 24.0, 2.0, 4.5, 3.0, 20.0, 20.0, 15.0, 10, 10},
                {LegacyTurretType.SENTRY_DAMAGED, 1_000L, 5L, 1.5, 1.25, 24.0, 2.0, 3.0, 2.0, 20.0, 20.0, 15.0, 10, 10},
        };
        for (Object[] row : expected) {
            LegacyTurretType t = (LegacyTurretType) row[0];
            boolean ok = t.maxPower() == (long) row[1] && t.consumption() == (long) row[2]
                    && t.heightOffset() == (double) row[3] && t.barrelLength() == (double) row[4]
                    && t.detectorRange(0, 0) == (double) row[5] && t.detectorGrace(0) == (double) row[6]
                    && t.yawSpeed() == (double) row[7] && t.pitchSpeed() == (double) row[8]
                    && t.elevation() == (double) row[9] && t.depression() == (double) row[10]
                    && t.acceptableInaccuracy() == (double) row[11] && t.fireDelay(0) == (int) row[12]
                    && t.detectorInterval(0) == (int) row[13];
            check(problems, cases, ok, "turret table " + t);
        }
        LegacyTurretType arty = LegacyTurretType.ARTY;
        check(problems, cases, arty.maxPower() == 100_000L && arty.heightOffset() == 3.0 && arty.barrelLength() == 9.0
                && arty.detectorRange(0, 0) == 3000.0 && arty.detectorRange(0, 1) == 250.0
                && arty.detectorGrace(0) == 250.0 && arty.detectorGrace(1) == 32.0
                && arty.fireDelay(0) == 300 && arty.fireDelay(1) == 40
                && arty.detectorInterval(0) == 200 && arty.detectorInterval(1) == 20
                && arty.yawSpeed() == 1.0 && arty.pitchSpeed() == 0.5 && arty.elevation() == 90.0
                && arty.depression() == 30.0 && arty.acceptableInaccuracy() == 0.0, "turret table ARTY");
        check(problems, cases, LegacyTurretType.MAXWELL.detectorRange(3, 0) == 73.0, "maxwell green range");
    }

    private static void turretAmmo(List<String> problems, int[] cases) {
        ItemStack micro = StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.ROCKET_MICRO);
        check(problems, cases, !LegacyTurretType.RICHARD.acceptsAmmo(micro), "richard rejects rocket_micro");
        for (StandardAmmoType rocket : new StandardAmmoType[] {StandardAmmoType.ROCKET_HE, StandardAmmoType.ROCKET_HEAT,
                StandardAmmoType.ROCKET_DEMO, StandardAmmoType.ROCKET_INC, StandardAmmoType.ROCKET_PHOSPHORUS}) {
            check(problems, cases, LegacyTurretType.RICHARD.acceptsAmmo(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), rocket)),
                    "richard accepts " + rocket);
        }
        check(problems, cases, LegacyTurretType.FRIENDLY.acceptsAmmo(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.R556_AP))
                && !LegacyTurretType.FRIENDLY.acceptsAmmo(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.P9_SP))
                && LegacyTurretType.SENTRY.acceptsAmmo(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.P9_JHP))
                && LegacyTurretType.TAUON.acceptsAmmo(StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.TAU_URANIUM))
                && LegacyTurretType.HOWARD.acceptsAmmo(new ItemStack(HbmItems.AMMO_DGK.get())), "turret ammo lists");
    }

    private static void turretConfig(List<String> problems, int[] cases) {
        double[][] values = {
                {HbmConfig.TURRET_CHEKHOV_DAMAGE.get(), 10}, {HbmConfig.TURRET_FRIENDLY_DAMAGE.get(), 5},
                {HbmConfig.TURRET_FRITZ_DAMAGE.get(), 1}, {HbmConfig.TURRET_HOWARD_DAMAGE.get(), 2},
                {HbmConfig.TURRET_METHUSALEM_DAMAGE.get(), 2}, {HbmConfig.TURRET_JEREMY_DAMAGE.get(), 50},
                {HbmConfig.TURRET_MAXWELL_DAMAGE.get(), 0.25}, {HbmConfig.TURRET_RICHARD_DAMAGE.get(), 30},
                {HbmConfig.TURRET_BROWN_DAMAGE.get(), 5}, {HbmConfig.TURRET_EDWIN_DAMAGE.get(), 5},
                {HbmConfig.TURRET_TAUON_DAMAGE.get(), 30}};
        boolean ok = true;
        for (double[] v : values) ok &= v[0] == v[1];
        check(problems, cases, ok, "XFactoryTurret TURRET_*_DAMAGE defaults");
        check(problems, cases, HbmConfig.MASK_MAN_DELAY.get() == 1200 && HbmConfig.MASK_MAN_MIN_RAD.get() == 50
                && HbmConfig.MASK_MAN_UNDERGROUND.get(), "MobConfig mask man defaults");
    }

    /**
     * Turret and drop pads. Behavioural pads use the spawn chunks at y = 230: chunks force-loaded near x = 19000 during
     * this synchronous start-up run never get their entity sections loaded (EntityStorage deserialises through the
     * server task mailbox, which does not run here), so Level#getEntities - the turret target search - sees nothing.
     * The pad is cleared after every case; y 228..238 is not used by the other diagnostics.
     */
    private static BlockPos pad(ServerLevel level, List<int[]> forced, int index) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = new BlockPos(spawn.getX(), 230, spawn.getZ());
        long deadline = System.currentTimeMillis() + 5_000L;
        while ((!level.isPositionEntityTicking(pos) || !entitiesLoaded(level, pos)) && System.currentTimeMillis() < deadline) {
            if (!level.getChunkSource().pollTask()) Thread.onSpinWait();
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-18, -1, -18), pos.offset(18, 8, 18))) {
            level.setBlock(p, p.getY() < pos.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static void unpad(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-18, -1, -18), pos.offset(18, 8, 18))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    private static boolean entitiesLoaded(ServerLevel level, BlockPos pos) {
        for (int cx = (pos.getX() - 24) >> 4; cx <= (pos.getX() + 24) >> 4; cx++) {
            for (int cz = (pos.getZ() - 24) >> 4; cz <= (pos.getZ() + 24) >> 4; cz++) {
                if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(cx, cz))) return false;
            }
        }
        return true;
    }

    private static LegacyTurretBlockEntity turret(ServerLevel level, BlockPos pos, Block block) {
        BlockState state = block.defaultBlockState();
        level.setBlock(pos, state, 2 | 16);
        LegacyTurretBlockEntity be = (LegacyTurretBlockEntity) level.getBlockEntity(pos);
        be.applyPower(0L, be.type().maxPower());
        return be;
    }

    private static void tick(ServerLevel level, LegacyTurretBlockEntity be, int ticks) {
        for (int i = 0; i < ticks; i++) LegacyTurretBlockEntity.tick(level, be.getBlockPos(), be.getBlockState(), be);
    }

    private static Entity target(LegacyTurretBlockEntity be) {
        try {
            Field f = LegacyTurretBlockEntity.class.getDeclaredField("target");
            f.setAccessible(true);
            return (Entity) f.get(be);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String debug(LegacyTurretBlockEntity be, Entity e) {
        try {
            java.lang.reflect.Method los = LegacyTurretBlockEntity.class.getDeclaredMethod("entityInLOS", Entity.class);
            java.lang.reflect.Method ok = LegacyTurretBlockEntity.class.getDeclaredMethod("acceptableTarget", Entity.class);
            los.setAccessible(true);
            ok.setAccessible(true);
            return " [power " + be.menuData().get(0) + ", on " + be.menuData().get(2) + ", target " + target(be)
                    + ", accept " + ok.invoke(be, e) + ", los " + los.invoke(be, e) + ", turret " + be.turretPos()
                    + ", entity " + e.position() + ", alive " + e.isAlive() + ", found "
                    + be.getLevel().getEntities((Entity) null, new AABB(be.turretPos(), be.turretPos()).inflate(32)).size()
                    + ", timer " + field(be, "searchTimer") + ", seek " + seek(be) + "]";
        } catch (ReflectiveOperationException ex) {
            return " [" + ex + "]";
        }
    }

    private static Object field(Object o, String name) throws ReflectiveOperationException {
        Field f = LegacyTurretBlockEntity.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static Object seek(LegacyTurretBlockEntity be) throws ReflectiveOperationException {
        java.lang.reflect.Method m = LegacyTurretBlockEntity.class.getDeclaredMethod("seekTarget", net.minecraft.world.level.Level.class);
        m.setAccessible(true);
        m.invoke(be, be.getLevel());
        return target(be);
    }

    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, Vec3 at) {
        T mob = type.create(level);
        mob.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        for (Entity e : level.getEntities((Entity) null, new AABB(pos).inflate(40), e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            e.discard();
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        unpad(level, pos);
    }

    private static void turretBehaviour(ServerLevel level, List<int[]> forced, List<String> problems, int[] cases) {
        // Tauon: aims, fires TAU_URANIUM every 5 ticks, base damage 30 * (1 + rand / 3) of the SUBATOMIC sedna class.
        BlockPos pos = pad(level, forced, 0);
        LegacyTurretBlockEntity tauon = turret(level, pos, HbmBlocks.TURRET_TAUON.get());
        tauon.menuData().set(2, 1);
        tauon.setItem(LegacyTurretBlockEntity.AMMO_START, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.TAU_URANIUM).copyWithCount(8));
        Mob husk = place(level, EntityType.HUSK, Vec3.atBottomCenterOf(pos).add(10.0D, 0.0D, 0.0D));
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D);
        husk.setHealth(1000.0F);
        float before = husk.getHealth();
        int shotTick = -1;
        for (int t = 0; t < 200 && shotTick < 0; t++) {
            tick(level, tauon, 1);
            if (tauon.getItem(LegacyTurretBlockEntity.AMMO_START).getCount() < 8) shotTick = t;
        }
        float delta = before - husk.getHealth();
        check(problems, cases, shotTick >= 0 && target(tauon) == husk, "tauon targets and fires" + debug(tauon, husk));
        check(problems, cases, delta > 25.0F && delta <= 40.0F, "tauon damage 30..40 (got " + delta + ")");
        check(problems, cases, husk.getLastDamageSource() != null
                && husk.getLastDamageSource().is(com.atomiclegacy.registry.HbmDamageTypes.SEDNA_SUBATOMIC), "tauon subatomic damage");
        check(problems, cases, tauon.menuData().get(0) < LegacyTurretType.TAUON.maxPower(), "tauon consumes power");
        clear(level, pos);

        // Richard: rocket_micro is not part of rocket_ml; a rocket_he is fired and consumed.
        pos = pad(level, forced, 1);
        LegacyTurretBlockEntity richard = turret(level, pos, HbmBlocks.TURRET_RICHARD.get());
        richard.menuData().set(2, 1);
        richard.setItem(LegacyTurretBlockEntity.AMMO_START, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.ROCKET_MICRO).copyWithCount(4));
        husk = place(level, EntityType.HUSK, Vec3.atBottomCenterOf(pos).add(16.0D, 0.0D, 0.0D));
        tick(level, richard, 80);
        check(problems, cases, richard.getItem(LegacyTurretBlockEntity.AMMO_START).getCount() == 4, "richard does not fire rocket_micro");
        richard.applyPower(0L, LegacyTurretType.RICHARD.maxPower()); // 100 HE/tick drains the 10000 HE buffer in 100 ticks
        richard.setItem(LegacyTurretBlockEntity.AMMO_START + 1, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.ROCKET_HE).copyWithCount(4));
        // loaded == 0 starts the 100 tick reload (TileEntityTurretRichard) before the first rocket.
        tick(level, richard, 60);
        richard.applyPower(0L, LegacyTurretType.RICHARD.maxPower());
        tick(level, richard, 60);
        check(problems, cases, richard.getItem(LegacyTurretBlockEntity.AMMO_START + 1).getCount() < 4
                && richard.getItem(LegacyTurretBlockEntity.AMMO_START).getCount() == 4, "richard fires rocket_he" + debug(richard, husk));
        clear(level, pos);

        // Target filters: SentryDamaged shoots any living entity, HowardDamaged keeps the base filter (no animals).
        pos = pad(level, forced, 2);
        LegacyTurretBlockEntity edwin = turret(level, pos, HbmBlocks.TURRET_SENTRY_DAMAGED.get());
        Mob cow = place(level, EntityType.COW, Vec3.atBottomCenterOf(pos).add(8.0D, 0.0D, 0.0D));
        tick(level, edwin, 2);
        check(problems, cases, target(edwin) == cow, "sentry_damaged targets animals" + debug(edwin, cow));
        clear(level, pos);
        pos = pad(level, forced, 3);
        LegacyTurretBlockEntity methusalem = turret(level, pos, HbmBlocks.TURRET_HOWARD_DAMAGED.get());
        cow = place(level, EntityType.COW, Vec3.atBottomCenterOf(pos).add(10.0D, 0.0D, 0.0D));
        tick(level, methusalem, 2);
        check(problems, cases, target(methusalem) == null, "howard_damaged ignores animals");
        clear(level, pos);

        // targetAnimals: 1.7.10 IMob extends IAnimals, so an animals-only filter still accepts hostile mobs.
        pos = pad(level, forced, 4);
        LegacyTurretBlockEntity friendly = turret(level, pos, HbmBlocks.TURRET_FRIENDLY.get());
        friendly.menuData().set(2, 1);
        friendly.menuData().set(5, 0);
        friendly.menuData().set(4, 1);
        husk = place(level, EntityType.HUSK, Vec3.atBottomCenterOf(pos).add(10.0D, 0.0D, 0.0D));
        tick(level, friendly, 2);
        check(problems, cases, target(friendly) == husk, "targetAnimals accepts IMob" + debug(friendly, husk));
        friendly.menuData().set(4, 0);
        friendly.setItem(LegacyTurretBlockEntity.AMMO_START, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoType.R556_SP).copyWithCount(10));
        friendly.menuData().set(5, 1);
        tick(level, friendly, 60);
        check(problems, cases, friendly.getItem(LegacyTurretBlockEntity.AMMO_START).getCount() < 10, "friendly fires 5.56");
        clear(level, pos);
    }

    // --- mobs: applyEntityAttributes / GlyphidStatsNT / getTotalArmorValue ---
    // Glyphids use the 1.7.10 old-AI path (moveForward = movementSpeed, getAIMoveSpeed() = 0.1F), so the port stores
    // speed * 0.1 for the modern navigator.
    private static void mobAttributes(ServerLevel level, List<String> problems, int[] cases) {
        attr(level, problems, cases, HbmEntityTypes.NUCLEAR_CREEPER::get, 50, 0.3, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.TAINTED_CREEPER::get, 15, 0.35, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.PHOSGENE_CREEPER::get, 20, 0.25, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.VOLATILE_CREEPER::get, 20, 0.25, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.GOLD_CREEPER::get, 20, 0.25, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.CYBER_CRAB::get, 4, 0.75, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.TESLA_CRAB::get, 10, 0.5, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.TAINT_CRAB::get, 25, 0.5, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.MASK_MAN::get, 1000, 0.25, 15, -1);
        attr(level, problems, cases, HbmEntityTypes.QUACKOS::get, 4, -1, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.DUCK::get, 4, -1, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.PIGEON::get, 20, 0.699999988079071, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.FBI::get, 20, 0.3, -1, 20);
        attr(level, problems, cases, HbmEntityTypes.FBI_DRONE::get, 35, -1, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.RAD_BEAST::get, 120, -1, 16, 8);
        attr(level, problems, cases, HbmEntityTypes.GHOST::get, 8, 0.2, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.UNDEAD_SOLDIER::get, 20, 0.25, 5, -1);
        attr(level, problems, cases, HbmEntityTypes.PARASITE_MAGGOT::get, 8, 1.0, 2, -1);
        attr(level, problems, cases, HbmEntityTypes.LEGACY_UFO::get, 20000, -1, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.LEGACY_CHOPPER::get, 750, -1, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.LEGACY_WORM_HEAD::get, 15000, 0.15, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.LEGACY_WORM_BODY::get, 15000, -1, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.BLOCK_SPIDER::get, 10, 1.0, -1, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID::get, 20, 0.1, 2, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_BOMBARDIER::get, 15, 0.1, 2, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_BRAWLER::get, 35, 0.1, 10, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_DIGGER::get, 50, 0.1, 10, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_BLASTER::get, 35, 0.1, 10, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_BEHEMOTH::get, 125, 0.08, 25, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_BRENDA::get, 250, 0.12, 50, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_NUCLEAR::get, 100, 0.08, 50, -1);
        attr(level, problems, cases, HbmEntityTypes.GLYPHID_SCOUT::get, 20, 0.15, 5, -1);
    }

    private static void attr(ServerLevel level, List<String> problems, int[] cases, Supplier<? extends EntityType<?>> type,
                             double health, double speed, double damage, double armor) {
        Entity entity = type.get().create(level);
        boolean ok = entity instanceof LivingEntity;
        if (ok) {
            LivingEntity living = (LivingEntity) entity;
            ok = near(living.getMaxHealth(), health)
                    && (speed < 0 || near(base(living, Attributes.MOVEMENT_SPEED), speed))
                    && (damage < 0 || near(base(living, Attributes.ATTACK_DAMAGE), damage))
                    && (armor < 0 || near(living.getArmorValue(), armor));
        }
        String got = entity instanceof LivingEntity l ? " (health " + l.getMaxHealth() + ", speed " + base(l, Attributes.MOVEMENT_SPEED)
                + ", damage " + base(l, Attributes.ATTACK_DAMAGE) + ", armor " + l.getArmorValue() + ")" : " (not living)";
        check(problems, cases, ok, "attributes " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type.get()) + got);
        if (entity != null) entity.discard();
    }

    private static double base(LivingEntity living, Holder<Attribute> attribute) {
        return living.getAttribute(attribute) == null ? Double.NaN : living.getAttribute(attribute).getBaseValue();
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-6;
    }

    // --- drops: dropFewItems / dropRareDrop / EntityCreeper.onDeath / EntityChicken ---
    private static void mobDrops(ServerLevel level, List<int[]> forced, List<String> problems, int[] cases) {
        BlockPos pos = pad(level, forced, 5);
        Vec3 at = Vec3.atBottomCenterOf(pos);
        Mob skeleton = place(level, EntityType.SKELETON, at.add(6, 0, 0));
        Mob creeper = place(level, HbmEntityTypes.GOLD_CREEPER.get(), at);
        creeper.hurt(level.damageSources().mobAttack(skeleton), 1000.0F);
        List<ItemEntity> items = drops(level, at);
        check(problems, cases, items.stream().anyMatch(i -> i.getItem().is(net.minecraft.tags.ItemTags.CREEPER_DROP_MUSIC_DISCS)),
                "creeper killed by skeleton drops a record");
        check(problems, cases, items.stream().anyMatch(i -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i.getItem().getItem()).equals(AtomicLegacy.id("crystal_gold"))), "gold creeper drops crystal_gold");
        clearItems(items);

        Mob duck = place(level, HbmEntityTypes.DUCK.get(), at);
        duck.hurt(level.damageSources().generic(), 1000.0F);
        items = drops(level, at);
        check(problems, cases, items.stream().filter(i -> i.getItem().is(Items.CHICKEN)).mapToInt(i -> i.getItem().getCount()).sum() == 1,
                "duck drops one raw chicken (EntityChicken)");
        clearItems(items);

        int coils = 0;
        for (int i = 0; i < 12; i++) {
            Mob crab = place(level, HbmEntityTypes.TAINT_CRAB.get(), at);
            crab.hurt(level.damageSources().generic(), 1000.0F);
            items = drops(level, at);
            coils += items.stream().filter(e -> e.getItem().is(HbmItems.COIL_COPPER.get())).mapToInt(e -> e.getItem().getCount()).sum();
            clearItems(items);
        }
        check(problems, cases, coils > 0, "taint crab copper coil without player kill");
        clear(level, pos);
    }

    private static List<ItemEntity> drops(ServerLevel level, Vec3 at) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(4.0D));
    }

    private static void clearItems(List<ItemEntity> items) {
        for (ItemEntity item : items) item.discard();
    }

    // --- EntityMappings.addSpawn ---
    private static void spawnRules(MinecraftServer server, List<String> problems, int[] cases) {
        var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        Biome plains = biomes.getOrThrow(Biomes.PLAINS);
        Biome ocean = biomes.getOrThrow(Biomes.OCEAN);
        Biome desert = biomes.getOrThrow(Biomes.DESERT);
        check(problems, cases, hasSpawn(plains, MobCategory.CREATURE, HbmEntityTypes.PIGEON.get(), 1, 5, 10), "pigeon spawn 1 (5-10) plains");
        check(problems, cases, hasSpawn(ocean, MobCategory.WATER_CREATURE, HbmEntityTypes.PLASTIC_BAG.get(), 1, 1, 3), "plastic bag spawn 1 (1-3) ocean");
        check(problems, cases, hasSpawn(desert, MobCategory.MONSTER, HbmEntityTypes.PHOSGENE_CREEPER.get(), 5, 1, 1)
                && hasSpawn(desert, MobCategory.MONSTER, HbmEntityTypes.VOLATILE_CREEPER.get(), 10, 1, 1)
                && hasSpawn(desert, MobCategory.MONSTER, HbmEntityTypes.GOLD_CREEPER.get(), 1, 1, 1), "creeper spawns 5/10/1 in all biomes");
        check(problems, cases, !hasSpawn(desert, MobCategory.MONSTER, HbmEntityTypes.NUCLEAR_CREEPER.get(), -1, -1, -1)
                && !hasSpawn(plains, MobCategory.MONSTER, HbmEntityTypes.TAINTED_CREEPER.get(), -1, -1, -1), "no natural nuclear/tainted creeper spawn");
    }

    private static boolean hasSpawn(Biome biome, MobCategory category, EntityType<?> type, int weight, int min, int max) {
        for (MobSpawnSettings.SpawnerData data : biome.getMobSettings().getMobs(category).unwrap()) {
            if (data.type == type && (weight < 0 || data.getWeight().asInt() == weight && data.minCount == min && data.maxCount == max)) {
                return true;
            }
        }
        return false;
    }

    // --- WeaponRecipes: ammo_dgk has no crafting recipe; 240mm stock shells use ModBlocks.tnt (tnt_ntm) ---
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        var manager = server.getRecipeManager();
        check(problems, cases, manager.byKey(AtomicLegacy.id("ammo_dgk_cordite")).isEmpty()
                && manager.byKey(AtomicLegacy.id("ammo_dgk_ballistite")).isEmpty()
                && manager.byKey(AtomicLegacy.id("ammo_press/ammo_dgk")).isPresent(), "ammo_dgk only from the ammo press");
        net.minecraft.world.item.Item tnt = HbmBlocks.TNT_NTM.get().asItem();
        for (String id : new String[] {"ammo_shell_stock_gunpowder", "ammo_shell_stock_ballistite", "ammo_shell_stock_cordite"}) {
            ResourceLocation key = AtomicLegacy.id(id);
            boolean ok = manager.byKey(key).map(holder -> holder.value().getIngredients().stream()
                    .anyMatch(ing -> ing.test(new ItemStack(tnt))) && holder.value().getIngredients().stream()
                    .noneMatch(ing -> ing.test(new ItemStack(Items.TNT)))).orElse(false);
            check(problems, cases, ok, id + " uses tnt_ntm");
        }
    }
}

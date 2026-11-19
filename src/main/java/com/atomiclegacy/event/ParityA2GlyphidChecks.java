package com.atomiclegacy.event;

import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.GlyphidSpawnerBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.GlyphidEntity;
import com.atomiclegacy.entity.GlyphidEntity.Variant;
import com.atomiclegacy.entity.GlyphidStats;
import com.atomiclegacy.entity.LegacyMistEntity;
import com.atomiclegacy.entity.logic.EntityWaypoint;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.tileentity.GlyphidSpawnerBlockEntity;
import com.atomiclegacy.util.LegacyDamageResistance;
import com.atomiclegacy.worldgen.GlyphidHiveGenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * [parity A2: glyphids] Server-side checks of the glyphid port against HBM 1.7.10 (entity/mob/glyphid/*,
 * GlyphidStats.GlyphidStatsNT, BlockGlyphidSpawner, GlyphidHive, EntityWaypoint, MobConfig). Everything except the
 * Brenda split runs on entities that are never added to the level; the split runs at y = 270 above the world spawn,
 * only when that position is entity-ticking and empty, and removes what it spawned.
 * Not covered: AI movement and pathing, projectile flight, scout hive building, digging, hive spawning in a world.
 */
final class ParityA2GlyphidChecks {

    private ParityA2GlyphidChecks() {
    }

    static void run(MinecraftServer server, List<String> problems, int[] cases) {
        try {
            ServerLevel level = server.overworld();
            stats(level, problems, cases);
            resistances(level, problems, cases);
            damageHandling(level, problems, cases);
            melee(level, problems, cases);
            tasks(level, problems, cases);
            scoutMath(problems, cases);
            waypoint(level, problems, cases);
            spawner(problems, cases);
            hiveSchematic(problems, cases);
            brendaSplit(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity A2 glyphid checks crashed", e);
            problems.add("A2: glyphid checks crashed: " + e);
        }
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String what) {
        cases[0]++;
        if (!ok) problems.add("A2: " + what);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-5;
    }

    private static GlyphidEntity shared(ServerLevel level, Variant variant) {
        GlyphidEntity glyphid = HbmEntityTypes.GLYPHID.get().create(level);
        if (glyphid != null) {
            glyphid.setVariant(variant);
        }
        return glyphid;
    }

    // --- GlyphidStatsNT bundles, isArmorBroken multipliers, per-class constants ---
    private static void stats(ServerLevel level, List<String> problems, int[] cases) {
        // variant, health, speed, damage, thresholdMultForArmor, resistanceMult, isArmorBroken factor,
        // (int) getScale() * 2 meat, blastSize, blastResToDig, swingDuration
        Object[][] expected = {
                {Variant.NORMAL, 20.0, 1.0, 2.0, 1.0F, 0.1F, 0.6F, 2, 1, 100, 15},
                {Variant.BOMBARDIER, 15.0, 1.0, 2.0, 1.0F, 0.1F, 0.6F, 2, 1, 100, 15},
                {Variant.BRAWLER, 35.0, 1.0, 10.0, 2.0F, 0.15F, 0.25F, 2, 1, 125, 15},
                {Variant.DIGGER, 50.0, 1.0, 10.0, 3.0F, 0.20F, 0.25F, 2, 1, 130, 15},
                {Variant.BLASTER, 35.0, 1.0, 10.0, 2.0F, 0.15F, 0.25F, 2, 1, 125, 15},
                {Variant.BEHEMOTH, 125.0, 0.8, 25.0, 5.0F, 0.35F, 0.15F, 2, 2, 150, 100},
                {Variant.BRENDA, 250.0, 1.2, 50.0, 10.0F, 0.5F, 0.12F, 4, 3, 150, 15},
                {Variant.NUCLEAR, 100.0, 0.8, 50.0, 10.0F, 0.5F, 0.12F, 4, 3, 150, 15},
                {Variant.SCOUT, 20.0, 1.5, 5.0, 0.5F, 0.5F, 0.0F, 0, 1, 75, 15},
        };
        for (Object[] row : expected) {
            Variant variant = (Variant) row[0];
            GlyphidStats.StatBundle bundle = GlyphidStats.forVariant(variant);
            check(problems, cases, near(bundle.health(), (double) row[1]) && near(bundle.speed(), (double) row[2])
                            && near(bundle.damage(), (double) row[3]) && near(bundle.thresholdPerArmor(), (float) row[4])
                            && near(bundle.resistanceMultiplier(), (float) row[5]),
                    "GlyphidStatsNT bundle of " + variant);
            check(problems, cases, near(GlyphidStats.armorBreakMultiplier(variant), (float) row[6]),
                    "isArmorBroken factor of " + variant);

            GlyphidEntity glyphid = shared(level, variant);
            if (glyphid == null) {
                check(problems, cases, false, "glyphid entity " + variant);
                continue;
            }
            check(problems, cases, near(glyphid.getHealth(), (double) row[1]) && near(glyphid.getMaxHealth(), (double) row[1]),
                    variant + " created through setVariant starts at full health (got " + glyphid.getHealth() + ")");
            check(problems, cases, glyphid.legacyMeatCount(0) == (int) row[7] && glyphid.legacyMeatCount(2) == (int) row[7] + 2,
                    variant + " meat count ((int) scale * 2 + looting)");
            check(problems, cases, glyphid.digBlastSize() == (int) row[8] && glyphid.digResistance() == (int) row[9],
                    variant + " blastSize / blastResToDig (got " + glyphid.digBlastSize() + ", " + glyphid.digResistance() + ")");
            check(problems, cases, glyphid.legacySwingDuration() == (int) row[10], variant + " swingDuration");
            check(problems, cases, near(glyphid.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D),
                    variant + " has no knockback resistance");
            check(problems, cases, glyphid.fireImmune() == (variant == Variant.BRENDA || variant == Variant.NUCLEAR),
                    variant + " isImmuneToFire");
            check(problems, cases, glyphid.armorCount() == 5 && near(glyphid.armorDamageThreshold(), (float) row[4]),
                    variant + " spawns with five armor plates");

            // DW_SUBTYPE is set after applyEntityAttributes ran, so the radioactive multipliers never applied
            double speed = glyphid.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            double damage = glyphid.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            glyphid.setSubtype(GlyphidEntity.TYPE_RADIOACTIVE);
            check(problems, cases, near(glyphid.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), speed)
                            && near(glyphid.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), damage)
                            && near(speed, (double) row[2] * 0.1D) && near(damage, (double) row[3]),
                    variant + " radioactive subtype keeps the class speed and damage");
            glyphid.discard();
        }
    }

    // --- EntityGlyphid.getCurrentDTDR through the DT/DR handler ---
    private static void resistances(ServerLevel level, List<String> problems, int[] cases) {
        GlyphidEntity grunt = shared(level, Variant.NORMAL);
        GlyphidEntity brenda = shared(level, Variant.BRENDA);
        if (grunt == null || brenda == null) {
            check(problems, cases, false, "glyphid entity for DT/DR");
            return;
        }
        Object[][] expected = {
                // class, grunt DT, grunt DR, brenda DT, brenda DR
                {"PHYSICAL", 1.0F, 0.1F, 10.0F, 0.5F},
                {"FIRE", 0.0F, 0.1F * 0.2F, 0.0F, 0.5F * 0.2F},
                {"EXPLOSIVE", 0.5F, 0.1F * 0.35F, 5.0F, 0.5F * 0.35F},
                {"LASER", 0.5F, 0.1F * 0.5F, 5.0F, 0.5F * 0.5F},
                {"ELECTRIC", 0.25F, 0.1F * 0.25F, 2.5F, 0.5F * 0.25F},
                {"SUBATOMIC", 0.0F, 0.1F * 0.1F, 0.0F, 0.5F * 0.1F},
        };
        for (Object[] row : expected) {
            float[] g = LegacyDamageResistance.getDTDR(grunt, (String) row[0]);
            float[] b = LegacyDamageResistance.getDTDR(brenda, (String) row[0]);
            check(problems, cases, near(g[0], (float) row[1]) && near(g[1], (float) row[2])
                            && near(b[0], (float) row[3]) && near(b[1], (float) row[4]),
                    "getCurrentDTDR " + row[0] + " (grunt " + g[0] + "/" + g[1] + ", brenda " + b[0] + "/" + b[1] + ")");
        }
        float[] nuke = brenda.legacyCurrentDTDR("nuclearblast", LegacyDamageResistance.CATEGORY_EXPLOSION, false);
        check(problems, cases, near(nuke[0], 2.5F) && near(nuke[1], 0.0F), "getCurrentDTDR nuclearBlast: DT * 0.25, no DR");
        float[] unblockable = brenda.legacyCurrentDTDR("generic", "generic", true);
        check(problems, cases, unblockable[0] == 0.0F && unblockable[1] == 0.0F, "getCurrentDTDR unblockable: 0 / 0");
        float[] cactus = LegacyDamageResistance.getDTDR(brenda, level.damageSources().cactus());
        check(problems, cases, near(cactus[0], 10.0F) && near(cactus[1], 0.5F), "getCurrentDTDR through a DamageSource");
        grunt.discard();
        brenda.discard();
    }

    // --- attackEntityFrom / DamageResistanceHandler / onDamageDealt ---
    private static void damageHandling(ServerLevel level, List<String> problems, int[] cases) {
        GlyphidEntity brenda = shared(level, Variant.BRENDA);
        GlyphidEntity grunt = shared(level, Variant.NORMAL);
        GlyphidEntity brawler = shared(level, Variant.BRAWLER);
        if (brenda == null || grunt == null || brawler == null) {
            check(problems, cases, false, "glyphid entity for damage handling");
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        brenda.moveTo(spawn.getX() + 0.5D, 300.0D, spawn.getZ() + 0.5D, 0.0F, 0.0F);
        grunt.moveTo(spawn.getX() + 0.5D, 300.0D, spawn.getZ() + 0.5D, 0.0F, 0.0F);
        brawler.moveTo(spawn.getX() + 0.5D, 300.0D, spawn.getZ() + 0.5D, 0.0F, 0.0F);

        check(problems, cases, !brenda.hurt(level.damageSources().mobAttack(grunt), 50.0F) && near(brenda.getHealth(), 250.0D),
                "damage from another glyphid is ignored");
        check(problems, cases, !brenda.hurt(level.damageSources().cactus(), 9.0F) && near(brenda.getHealth(), 250.0D),
                "damage below the armor threshold (10) is cancelled");
        boolean hit = brenda.hurt(level.damageSources().cactus(), 210.0F);
        check(problems, cases, hit && near(brenda.getHealth(), 150.0D),
                "DT 10 / DR 0.5: 210 damage removes 100 health (health " + brenda.getHealth() + ")");
        // onDamageDealt(100): rand.nextInt(100) <= min((100 * 0.12)^2, 100) always breaks one plate
        check(problems, cases, brenda.armorCount() == 4 && near(brenda.armorDamageThreshold(), 8.0F),
                "onDamageDealt breaks off one armor plate (plates " + brenda.armorCount() + ")");

        check(problems, cases, !brawler.hurt(level.damageSources().fall(), 10.0F) && near(brawler.getHealth(), 35.0D),
                "brawler ignores fall damage up to 10");
        brenda.discard();
        grunt.discard();
        brawler.discard();
    }

    // --- EntityMob.attackEntity range, EntityGlyphid.attackEntityAsMob swing gate ---
    private static void melee(ServerLevel level, List<String> problems, int[] cases) {
        GlyphidEntity attacker = shared(level, Variant.BRENDA);
        GlyphidEntity victim = shared(level, Variant.NORMAL);
        if (attacker == null || victim == null) {
            check(problems, cases, false, "glyphid entity for melee");
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        double x = spawn.getX() + 0.5D;
        double z = spawn.getZ() + 0.5D;
        attacker.moveTo(x, 300.0D, z, 0.0F, 0.0F);
        victim.moveTo(x + 1.9D, 300.0D, z, 0.0F, 0.0F);
        check(problems, cases, attacker.isWithinMeleeAttackRange(victim), "melee reach: distance 1.9 < 2.0");
        victim.moveTo(x + 2.1D, 300.0D, z, 0.0F, 0.0F);
        check(problems, cases, !attacker.isWithinMeleeAttackRange(victim), "melee reach: distance 2.1 is out of range");
        victim.moveTo(x + 1.0D, 302.0D, z, 0.0F, 0.0F);
        check(problems, cases, !attacker.isWithinMeleeAttackRange(victim), "melee reach: bounding boxes must overlap vertically");

        attacker.swinging = true;
        attacker.swingTime = 1;
        check(problems, cases, !attacker.doHurtTarget(victim) && attacker.swingTime == 1,
                "attackEntityAsMob does nothing while the swing is in progress");
        attacker.discard();
        victim.discard();
    }

    // --- task system: isAtDestination / scout override ---
    private static void tasks(ServerLevel level, List<String> problems, int[] cases) {
        check(problems, cases, GlyphidEntity.TASK_IDLE == 0 && GlyphidEntity.TASK_RETREAT_FOR_REINFORCEMENTS == 1
                && GlyphidEntity.TASK_BUILD_HIVE == 2 && GlyphidEntity.TASK_INITIATE_RETREAT == 3
                && GlyphidEntity.TASK_FOLLOW == 4 && GlyphidEntity.TASK_TERRAFORM == 5 && GlyphidEntity.TASK_DIG == 6,
                "task ids");
        GlyphidEntity grunt = shared(level, Variant.NORMAL);
        GlyphidEntity scout = shared(level, Variant.SCOUT);
        if (grunt == null || scout == null) {
            check(problems, cases, false, "glyphid entity for tasks");
            return;
        }
        // task coordinates default to 0,0,0 with the 25 (5 block) fallback radius
        grunt.moveTo(3.0D, 0.0D, 4.0D, 0.0F, 0.0F);
        check(problems, cases, grunt.isAtDestination(), "isAtDestination: 5 blocks from the task position");
        grunt.moveTo(3.0D, 0.0D, 4.5D, 0.0F, 0.0F);
        check(problems, cases, !grunt.isAtDestination(), "isAtDestination: beyond 5 blocks");

        scout.moveTo(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        check(problems, cases, !scout.isAtDestination(), "scout isAtDestination needs TASK_BUILD_HIVE");
        scout.setCurrentTask(GlyphidEntity.TASK_BUILD_HIVE, null);
        check(problems, cases, scout.isAtDestination(), "scout isAtDestination with TASK_BUILD_HIVE");
        scout.setCurrentTask(GlyphidEntity.TASK_TERRAFORM, null);
        check(problems, cases, !scout.isAtDestination(), "scout isAtDestination is false while terraforming");
        grunt.discard();
        scout.discard();
    }

    // --- EntityGlyphidScout.playerBaseDirFinder / canBuildHiveHere rays ---
    private static void scoutMath(List<String> problems, int[] cases) {
        // 1.7.10 Vec3.subtract(v) = v - this: ten blocks from the scout toward the target
        Vec3 point = GlyphidEntity.scoutGuidancePoint(new Vec3(100.0D, 64.0D, 100.0D), new Vec3(200.0D, 64.0D, 100.0D));
        check(problems, cases, near(point.x, 110.0D) && near(point.y, 64.0D) && near(point.z, 100.0D),
                "playerBaseDirFinder moves 10 blocks toward the target (got " + point + ")");
        // Vec3(0, 0, length).rotateAroundY(angle) = (length * sin, 0, length * cos)
        Vec3 first = GlyphidEntity.scoutHiveProbeOffset(0, 8);
        Vec3 quarter = GlyphidEntity.scoutHiveProbeOffset(4, 8);
        Vec3 last = GlyphidEntity.scoutHiveProbeOffset(7, 16);
        check(problems, cases, near(first.x, 0.0D) && near(first.z, 8.0D) && near(quarter.x, 8.0D) && near(quarter.z, 0.0D)
                        && last.x > 0.0D && last.z < 0.0D && Math.abs(last.length() - 16.0D) < 1.0E-2 /* Mth sin table */,
                "canBuildHiveHere rays cover the +X half circle (got " + quarter + ", " + last + ")");
    }

    // --- EntityWaypoint defaults ---
    private static void waypoint(ServerLevel level, List<String> problems, int[] cases) {
        EntityWaypoint waypoint = HbmEntityTypes.GLYPHID_WAYPOINT.get().create(level);
        if (waypoint == null) {
            check(problems, cases, false, "waypoint entity");
            return;
        }
        check(problems, cases, waypoint.getMaxAge() == 2400 && waypoint.getRadius() == 3 && !waypoint.isHighPriority()
                && waypoint.getWaypointType() == GlyphidEntity.TASK_IDLE && waypoint.fireImmune(), "waypoint defaults");
        check(problems, cases, waypoint.getColor() == 0x566573, "waypoint color idle");
        waypoint.setWaypointType(GlyphidEntity.TASK_RETREAT_FOR_REINFORCEMENTS);
        check(problems, cases, waypoint.getColor() == 0x5FA6E8, "waypoint color retreat");
        waypoint.setWaypointType(GlyphidEntity.TASK_BUILD_HIVE);
        check(problems, cases, waypoint.getColor() == 0x127766, "waypoint color build hive");
        waypoint.setWaypointType(GlyphidEntity.TASK_INITIATE_RETREAT);
        check(problems, cases, waypoint.getColor() == 0x127766, "waypoint color initiate retreat");
        waypoint.discard();
    }

    // --- BlockGlyphidSpawner: spawnMap, createSwarm formulas, MobConfig tuples, quantityDropped ---
    private static void spawner(List<String> problems, int[] cases) {
        Variant[] order = GlyphidSpawnerBlockEntity.spawnOrder();
        Variant[] want = {Variant.NORMAL, Variant.BOMBARDIER, Variant.BRAWLER, Variant.DIGGER, Variant.BLASTER,
                Variant.BEHEMOTH, Variant.BRENDA, Variant.NUCLEAR};
        check(problems, cases, java.util.Arrays.equals(order, want), "spawnMap order " + java.util.Arrays.toString(order));

        tuple(problems, cases, "glyphidChance", HbmConfig.GLYPHID_GRUNT_CHANCE, 50, -45, 0);
        tuple(problems, cases, "brawlerChance", HbmConfig.GLYPHID_BRAWLER_CHANCE, 10, 30, 1);
        tuple(problems, cases, "bombardierChance", HbmConfig.GLYPHID_BOMBARDIER_CHANCE, 20, -15, 1);
        tuple(problems, cases, "blasterChance", HbmConfig.GLYPHID_BLASTER_CHANCE, -5, 40, 5);
        tuple(problems, cases, "diggerChance", HbmConfig.GLYPHID_DIGGER_CHANCE, -15, 25, 5);
        tuple(problems, cases, "behemothChance", HbmConfig.GLYPHID_BEHEMOTH_CHANCE, -30, 45, 10);
        tuple(problems, cases, "brendaChance", HbmConfig.GLYPHID_BRENDA_CHANCE, -50, 60, 20);
        tuple(problems, cases, "johnsonChance", HbmConfig.GLYPHID_NUCLEAR_CHANCE, -50, 60, 50);

        // (int) min(baseSwarmSize * max(swarmScalingMult * (soot / sootStep), 1), 10)
        check(problems, cases, GlyphidSpawnerBlockEntity.swarmAmount(0.0D, 5, 1.2D, 50) == 5
                && GlyphidSpawnerBlockEntity.swarmAmount(50.0D, 5, 1.2D, 50) == 6
                && GlyphidSpawnerBlockEntity.swarmAmount(75.0D, 5, 1.2D, 50) == 9
                && GlyphidSpawnerBlockEntity.swarmAmount(500.0D, 5, 1.2D, 50) == 10, "swarm size by soot");
        // (int) (base + (mod - mod / max((soot + 1) / 3, 1)))
        check(problems, cases, GlyphidSpawnerBlockEntity.adjustedChance(50, -45, 0.0D) == 50
                && GlyphidSpawnerBlockEntity.adjustedChance(50, -45, 5.0D) == 27
                && GlyphidSpawnerBlockEntity.adjustedChance(10, 30, 8.0D) == 30
                && GlyphidSpawnerBlockEntity.adjustedChance(-50, 60, 59.0D) == 7, "spawn chance by soot");

        RandomSource random = RandomSource.create(42L);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < 200; i++) {
            int count = GlyphidSpawnerBlock.eggCount(random, 0);
            min = Math.min(min, count);
            max = Math.max(max, count);
        }
        check(problems, cases, min == 1 && max == 3, "spawner drops 1-3 eggs (got " + min + "-" + max + ")");
        check(problems, cases, GlyphidSpawnerBlock.eggCount(random, 3) >= 4, "spawner egg count adds the fortune level");
    }

    private static void tuple(List<String> problems, int[] cases, String key,
                              ModConfigSpec.ConfigValue<List<? extends Integer>> value, int base, int modifier, int minimum) {
        List<? extends Integer> defaults = value.getDefault();
        check(problems, cases, defaults.size() == 3 && defaults.get(0) == base && defaults.get(1) == modifier
                && defaults.get(2) == minimum, "MobConfig " + key + " default " + defaults);
    }

    // --- GlyphidHive.schematicSmall ---
    private static void hiveSchematic(List<String> problems, int[] cases) {
        String zero = "00000000000";
        String[][] want = {
                {zero, zero, zero, zero, "00001110000", "00001110000", "00001110000", zero, zero, zero, zero},
                {zero, zero, "00001110000", "00011111000", "00111111100", "00111111100", "00111111100", "00011111000",
                        "00001110000", zero, zero},
                {zero, "00001110000", "00111111100", "00111111100", "01113331110", "01113331110", "01113331110",
                        "00111111100", "00111111100", "00001110000", zero},
                {zero, "00001110000", "00111111100", "00112221100", "01122222110", "01122222110", "01122222110",
                        "00112221100", "00111111100", "00001110000", zero},
                {zero, "00001110000", "00111111100", "00111111100", "01111111110", "01111111110", "01111111110",
                        "00111111100", "00111111100", "00001110000", zero},
        };
        for (int layer = 0; layer < 5; layer++) {
            boolean ok = true;
            for (int row = 0; row < 11 && ok; row++) {
                for (int column = 0; column < 11; column++) {
                    if (GlyphidHiveGenerator.schematicMarker(layer, row, column) != want[layer][row].charAt(column) - '0') {
                        ok = false;
                        break;
                    }
                }
            }
            check(problems, cases, ok, "GlyphidHive.schematicSmall layer " + layer);
        }
    }

    // --- EntityGlyphidBrenda.onDeath: pheromone mist and twelve grunts ---
    private static void brendaSplit(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = new BlockPos(spawn.getX(), 270, spawn.getZ());
        AABB box = new AABB(pos).inflate(16.0D);
        if (!level.isPositionEntityTicking(pos) || !level.getEntities((Entity) null, box, entity -> true).isEmpty()) {
            return; // nothing may be disturbed; the case is skipped and reported as not run by the caller's count
        }
        GlyphidEntity brenda = shared(level, Variant.BRENDA);
        if (brenda == null) {
            check(problems, cases, false, "glyphid entity for the brenda split");
            return;
        }
        try {
            brenda.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            brenda.setHealth(0.0F);
            brenda.die(level.damageSources().cactus());
            List<GlyphidEntity> children = level.getEntitiesOfClass(GlyphidEntity.class, box, entity -> entity != brenda);
            boolean grunts = children.size() == 12;
            for (GlyphidEntity child : children) {
                grunts &= child.getVariant() == Variant.NORMAL && child.getSubtype() == GlyphidEntity.TYPE_NORMAL
                        && near(child.getHealth(), 20.0D);
            }
            check(problems, cases, grunts, "brenda splits into 12 grunts (got " + children.size() + ")");
            check(problems, cases, level.getEntitiesOfClass(LegacyMistEntity.class, box).size() == 1,
                    "brenda leaves one pheromone mist");
        } finally {
            for (Entity entity : level.getEntities((Entity) null, box, entity -> !(entity instanceof Player))) {
                entity.discard();
            }
            brenda.discard();
        }
    }
}

package com.atomiclegacy.entity;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.explosion.BalefireExplosionManager;
import com.atomiclegacy.explosion.LegacyMukeExplosion;
import com.atomiclegacy.explosion.LegacyVnt;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.items.CustomMissileData;
import com.atomiclegacy.items.CustomMissileItem;
import com.atomiclegacy.items.MissilePartItem;
import com.atomiclegacy.blocks.TaintBlock;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [parity P1: missile impacts] Per-missile behaviour of the 1.7.10 EntityMissileTier0..4, EntityMissileStealth,
 * EntityMissileShuttle, EntityMissileAntiBallistic and EntityMissileCustom subclasses, folded into the single
 * launcher_missile entity. Every helper mirrors the old call it is named after (ExplosionLarge, ExplosionChaos,
 * ExplosionNT, World.createExplosion/newExplosion). Positions are the missile's own position at impact (posX/Y/Z
 * before the final move), except EntityMissileTaint, which used the hit vector and hit block.
 */
public final class LegacyMissileImpacts {
    private LegacyMissileImpacts() {}

    /** Diagnostics only: when non-null, helpers record their call instead of touching the world. */
    @Nullable static List<String> dryRun;

    public static List<String> dryRunImpact(LegacyLauncherMissileEntity missile, @Nullable BlockHitResult hit) {
        dryRun = new ArrayList<>();
        try {
            impact(missile, hit);
            return dryRun;
        } finally {
            dryRun = null;
        }
    }

    public static List<String> dryRunKill(LegacyLauncherMissileEntity missile) {
        dryRun = new ArrayList<>();
        try {
            killMissile(missile);
            return dryRun;
        } finally {
            dryRun = null;
        }
    }

    private static boolean dry(String call) {
        if (dryRun == null) return false;
        dryRun.add(call);
        return true;
    }

    private static String f(double v) {
        return Float.toString((float) v);
    }

    // ------------------------------------------------------------------ dispatch

    /** EntityMissileBaseNT.onMissileImpact / cluster(). {@code hit} is null for an airburst cluster. */
    public static void impact(LegacyLauncherMissileEntity missile, @Nullable BlockHitResult hit) {
        if (!(missile.level() instanceof ServerLevel level)) return;
        Vec3 p = missile.position();
        if (missile.missileItem().getItem() instanceof CustomMissileItem) {
            CustomMissileData data = CustomMissileData.read(missile.missileItem());
            if (data == null) throw new IllegalStateException("Invalid assembled missile at impact");
            custom(level, missile, data, p);
            return;
        }
        String id = missile.missileId();
        switch (id) {
            case "missile_test" -> missileTest(level, p);
            case "missile_micro" -> {
                // ExplosionNukeSmall.explode(world, posX, posY + 0.5, posZ, PARAMS_HIGH)
                Vec3 c = new Vec3(p.x, p.y + 0.5D, p.z);
                int radius = HbmConfig.FATMAN_RADIUS.get();
                if (!dry("mukeEffect(" + f(c.y) + ")+mk5(" + radius + ",torex=false)")) {
                    LegacyMukeExplosion.sendMukeEffect(level, c);
                    NukeExplosionManager.scheduleMicroNuke(level, c.x, c.y, c.z, radius);
                }
            }
            case "missile_schrabidium" -> {
                int radius = HbmConfig.DROPPED_ANTI_SCHRABIDIUM_RADIUS.get();
                if (!dry("fleija(" + radius + ")")
                        && !NukeExplosionManager.isFleijaSuppressed(level, BlockPos.containing(p)))
                    NukeExplosionManager.scheduleFleijaNuke(level, p.x, p.y, p.z, radius);
            }
            case "missile_bhole" -> {
                createExplosion(level, missile, p, 1.5F);
                if (!dry("blackHole(1.5)")) LegacyBlackHoleEntity.spawn(level, p, 1.5F, false);
            }
            case "missile_taint" -> taintMissile(level, missile, hit == null ? p : hit.getLocation(),
                    hit == null ? BlockPos.containing(p) : hit.getBlockPos());
            case "missile_emp" -> {
                if (!dry("empBlast(50)+empBlastEntity(maxAge=50)")) {
                    LegacyEmpUtil.empBlast(level, (int) p.x, (int) p.y, (int) p.z, 50);
                    LegacyEmpEntity.spawnBlast(level, p.x, p.y, p.z, 50);
                }
            }
            case "missile_generic" -> { explodeStandard(missile, p, 15F, 24, false); effectSmall(level, p); }
            case "missile_decoy" -> newExplosion(level, missile, p, 4F, false, false);
            case "missile_incendiary" -> { explodeStandard(missile, p, 15F, 24, true); effectSmall(level, p); }
            case "missile_cluster" -> { createExplosion(level, missile, p, 5F); cluster(missile, p, 25); }
            case "missile_buster" -> {
                for (int i = 0; i < 15; i++) createExplosion(level, missile, p.add(0, -i, 0), 5F);
                spawnParticles(level, p, 5); spawnShrapnels(level, p, 5); spawnRubble(level, p, 5);
            }
            case "missile_strong" -> { explodeStandard(missile, p, 30F, 32, false); effectStandard(level, p); }
            case "missile_incendiary_strong" -> {
                explodeStandard(missile, p, 30F, 32, true);
                effectStandard(level, p);
                igniteFlammableBlocks(level, (int) ((float) p.x + 0.5F), (int) ((float) p.y + 0.5F), (int) ((float) p.z + 0.5F), 25);
            }
            case "missile_cluster_strong" -> { createExplosion(level, missile, p, 15F); cluster(missile, p, 50); }
            case "missile_buster_strong" -> {
                for (int i = 0; i < 20; i++) createExplosion(level, missile, p.add(0, -i, 0), 7.5F);
                spawnParticles(level, p, 8); spawnShrapnels(level, p, 8); spawnRubble(level, p, 8);
            }
            case "missile_emp_strong" -> {
                if (!dry("empEntity")) LegacyEmpEntity.spawnPersistent(level, p.x, p.y, p.z);
            }
            case "missile_burst" -> { explodeStandard(missile, p, 50F, 48, false); effectLarge(level, p); }
            case "missile_inferno" -> {
                explodeStandard(missile, p, 50F, 48, true);
                effectLarge(level, p);
                igniteAllBlocks(level, (int) p.x, (int) p.y, (int) p.z, 10);
                igniteFlammableBlocks(level, (int) p.x, (int) p.y, (int) p.z, 25);
            }
            case "missile_rain" -> { createExplosion(level, missile, p, 25F); cluster(missile, p, 100); }
            case "missile_drill" -> {
                for (int i = 0; i < 30; i++) explosionNTErode(level, missile, p.add(0, -i, 0), 10F);
                spawnParticles(level, p, 25); spawnShrapnels(level, p, 12); jolt(level, p, 10, 50, 1);
            }
            case "missile_shuttle" -> shuttle(level, p);
            case "missile_stealth" -> { explodeStandard(missile, p, 20F, 24, false); effectStandard(level, p); }
            // [parity V3: BombConfig.missileRadius] EntityMissileTier4: nuclear r, cluster 2r, doomsday 2r, rusted r.
            case "missile_nuclear" -> { int r = com.atomiclegacy.config.HbmParityV3Config.missileRadius(); if (!dry("mk5(" + r + ")+torex(" + r + ")")) NukeExplosionManager.scheduleMk5Nuclear(level, p.x, p.y, p.z, r); }
            case "missile_nuclear_cluster" -> { int r = com.atomiclegacy.config.HbmParityV3Config.missileRadius(); if (!dry("mk5(" + r * 2 + ")+torex(" + r * 2 + ")")) NukeExplosionManager.scheduleMirv(level, p.x, p.y, p.z, r); }
            case "missile_doomsday" -> { int r = com.atomiclegacy.config.HbmParityV3Config.missileRadius() * 2; if (!dry("mk5(" + r + ",moreFallout=100)+torex(" + r + ")")) NukeExplosionManager.scheduleDoomsday(level, p.x, p.y, p.z, r); }
            case "missile_doomsday_rusted" -> { int r = com.atomiclegacy.config.HbmParityV3Config.missileRadius(); if (!dry("mk5(" + r + ",moreFallout=100)+torex(" + r + ")")) NukeExplosionManager.scheduleRustedDoomsday(level, p.x, p.y, p.z, r); }
            case "missile_volcano" -> volcano(level, p);
            default -> throw new IllegalStateException("Unmapped legacy missile impact: " + id);
        }
    }

    /** EntityMissileAntiBallistic: ExplosionLarge.explode(posX, posY, posZ, radius, true, false, false). */
    public static void antiBallistic(LegacyLauncherMissileEntity missile, float radius) {
        if (missile.level() instanceof ServerLevel level) largeExplode(level, missile.position(), radius, true, false, false, false);
    }

    /** EntityMissileBaseNT.killMissile / EntityMissileCustom.killMissile. */
    public static void killMissile(LegacyLauncherMissileEntity missile) {
        if (!(missile.level() instanceof ServerLevel level)) return;
        Vec3 p = missile.position();
        Vec3 m = missile.getDeltaMovement();
        largeExplode(level, p, 5F, true, false, true, false);
        if (!dry("shrapnelShower(15,0.075)")) LegacyProjectileUtil.spawnShrapnelShower(level, p, m, 15, 0.075D);
        if (missile.missileItem().getItem() instanceof CustomMissileItem) return;
        spawnMissileDebris(level, p, m, 0.25D, debris(missile.missileId()), rareDrop(missile.missileId()));
    }

    // ------------------------------------------------------------------ custom warheads

    private static void custom(ServerLevel level, LegacyLauncherMissileEntity m, CustomMissileData data, Vec3 p) {
        MissilePartItem.Definition def = MissilePartItem.definition(data.warhead());
        float s = def.primary();
        Vec3 motion = m.getDeltaMovement();
        switch (def.warhead()) {
            case HE -> { largeExplode(level, p, s, true, false, true, false); jolt(level, p, s, (int) (s * 50), 0.25D); }
            case INC -> { largeExplode(level, p, s, true, false, true, true); jolt(level, p, s * 1.5D, (int) (s * 50), 0.25D); }
            case CLUSTER -> { }
            case BUSTER -> buster(level, p, motion, s, s * 4);
            case NUCLEAR, TX -> { if (!dry("mk5(" + (int) s + ")+torex(" + f(s) + ")")) NukeExplosionManager.scheduleMk5Nuclear(level, p.x, p.y, p.z, (int) s); }
            case BALEFIRE -> {
                if (!dry("balefire(" + (int) s + ")+torexBale(" + f(s) + ")")) {
                    BalefireExplosionManager.schedule(level, BlockPos.containing(p), (int) s);
                    level.addFreshEntity(new NukeTorexEntity(level, p.x, p.y, p.z, torexScale(s), 1));
                }
            }
            case N2 -> { if (!dry("mk5NoRad(" + (int) s + ")+torex(" + f(s) + ")")) NukeExplosionManager.scheduleMk5NoRadiation(level, p.x, p.y, p.z, (int) s); }
            case TAINT -> customTaint(level, p, (int) s);
            case CLOUD -> {
                if (!dry("auxSFX2002+poisonCloud(750,2.5,2)")) {
                    level.levelEvent(2002, new BlockPos((int) Math.round(p.x), (int) Math.round(p.y), (int) Math.round(p.z)), 0);
                    // [parity R1: FX entities] ExplosionChaos.spawnPoisonCloud(750, 2.5, type 2): real EntityPinkCloudFX.
                    Vec3 c = p.subtract(motion);
                    LegacyCloudFxEntity.spawnPoisonCloud(level, c.x, c.y, c.z, 750, 2.5D, 2);
                }
            }
            case TURBINE -> turbine(level, m, p, motion, (int) s);
            case SCHRAB -> { }
        }
    }

    private static float torexScale(float scale) {
        double x = scale * 0.01D;
        double squirt = Math.sqrt(x + 1.0D / ((x + 2.0D) * (x + 2.0D))) - 1.0D / (x + 2.0D);
        return Math.max(0.5F, Math.min(5F, (float) squirt * 1.5F));
    }

    private static void customTaint(ServerLevel level, Vec3 p, int r) {
        if (dry("taint(" + r + ")")) return;
        RandomSource rand = level.random;
        for (int i = 0; i < r * 10; i++) {
            BlockPos t = new BlockPos(rand.nextInt(r) + (int) p.x - (r / 2 - 1),
                    rand.nextInt(r) + (int) p.y - (r / 2 - 1), rand.nextInt(r) + (int) p.z - (r / 2 - 1));
            BlockState state = level.getBlockState(t);
            if (isNormalCube(level, t, state) && !state.isAir())
                level.setBlock(t, HbmBlocks.TAINT.get().defaultBlockState().setValue(TaintBlock.AGE, rand.nextInt(3) + 4), 2);
        }
    }

    /** TURBINE: (int) strength blades EntityBulletBaseNT(BulletConfigSyncingUtil.TURBINE), vec (0.5, 0, 0) rotated 2pi/count. [parity A1] */
    private static void turbine(ServerLevel level, Entity source, Vec3 p, Vec3 motion, int count) {
        largeExplode(level, p, 10F, true, false, true, false);
        if (dry("turbineBlades(" + count + ")")) return;
        Vec3 vec = new Vec3(0.5D, 0.0D, 0.0D);
        for (int i = 0; i < count; i++) {
            level.addFreshEntity(new com.atomiclegacy.entity.LegacyTurbineBladeEntity(level, p.x - motion.x,
                    p.y - motion.y + level.random.nextGaussian(), p.z - motion.z, vec.x, vec.z));
            vec = vec.yRot((float) (Math.PI * 2F / (float) count));
        }
    }

    // ------------------------------------------------------------------ legacy helpers

    private static void explodeStandard(Entity source, Vec3 p, float strength, int resolution, boolean fire) {
        if (dry("explodeStandard(" + f(strength) + "," + resolution + "," + fire + ")")) return;
        LegacyProjectileUtil.standardLegacyMissileExplosion(source, p, strength, resolution, fire);
    }

    private static void effectSmall(ServerLevel level, Vec3 p) {
        if (dry("composeEffectSmall")) return;
        LegacyProjectileUtil.composeExplosionEffect(level, p, 10, 2.0F, 0.5F, 25.0F, 5, 8, 20, 0.75F, 1.0F, -2.0F, 150.0F);
    }

    private static void effectStandard(ServerLevel level, Vec3 p) {
        if (dry("composeEffectStandard")) return;
        LegacyProjectileUtil.composeExplosionEffect(level, p, 15, 5.0F, 1.0F, 45.0F, 10, 16, 50, 1.0F, 3.0F, -2.0F, 200.0F);
    }

    private static void effectLarge(ServerLevel level, Vec3 p) {
        if (dry("composeEffectLarge")) return;
        LegacyProjectileUtil.composeExplosionEffect(level, p, 30, 6.5F, 2.0F, 65.0F, 25, 16, 50, 1.25F, 3.0F, -2.0F, 350.0F);
    }

    /** World.createExplosion(entity, x, y, z, size, true): not flaming, smoking. */
    private static void createExplosion(ServerLevel level, @Nullable Entity source, Vec3 p, float size) {
        newExplosion(level, source, p, size, false, true);
    }

    private static void newExplosion(ServerLevel level, @Nullable Entity source, Vec3 p, float size, boolean flaming, boolean smoking) {
        if (dry("newExplosion(" + f(size) + "," + flaming + "," + smoking + ")")) return;
        LegacyVnt.vanilla(level, source, p.x, p.y, p.z, size, flaming, smoking);
    }

    /** ExplosionLarge.explode / explodeFire. */
    private static void largeExplode(ServerLevel level, Vec3 p, float strength, boolean cloud, boolean rubble, boolean shrapnel, boolean fire) {
        if (dry((fire ? "explodeFire(" : "explodeLarge(") + f(strength) + "," + cloud + "," + rubble + "," + shrapnel + ")")) return;
        LegacyVnt.vanilla(level, null, p.x, p.y, p.z, strength, fire, true);
        if (cloud) LegacyBuildingEntity.spawnParticles(level, p.x, p.y, p.z, cloudFunction((int) strength));
        if (rubble) spawnRubbleRaw(level, p, ((int) strength) / 10);
        if (shrapnel) LegacyProjectileUtil.spawnShrapnel(level, p, ((int) strength) / 3);
    }

    static int cloudFunction(int i) {
        return (int) (850 * (1 - Math.pow(Math.E, -i / 15)) + 15);
    }

    private static void spawnParticles(ServerLevel level, Vec3 p, int count) {
        if (dry("spawnParticles(" + count + ")")) return;
        LegacyBuildingEntity.spawnParticles(level, p.x, p.y, p.z, count);
    }

    private static void spawnShrapnels(ServerLevel level, Vec3 p, int count) {
        if (dry("spawnShrapnels(" + count + ")")) return;
        LegacyProjectileUtil.spawnShrapnel(level, p, count);
    }

    private static void spawnRubble(ServerLevel level, Vec3 p, int count) {
        if (dry("spawnRubble(" + count + ")")) return;
        spawnRubbleRaw(level, p, count);
    }

    /** ExplosionLarge.spawnRubble (integer divisions as in the original). */
    private static void spawnRubbleRaw(ServerLevel level, Vec3 p, int count) {
        RandomSource rand = level.random;
        for (int i = 0; i < count; i++) {
            double my = 0.75D * (1 + ((count + rand.nextInt(count * 5))) / 25);
            double mx = rand.nextGaussian() * 0.75D * (1 + (count / 50));
            double mz = rand.nextGaussian() * 0.75D * (1 + (count / 50));
            level.addFreshEntity(new MineRubbleEntity(level, p.x, p.y, p.z, new Vec3(mx, my, mz), Blocks.STONE.defaultBlockState()));
        }
    }

    /** ExplosionLarge.jolt. The old code discarded the result of vec4.normalize(). */
    private static void jolt(ServerLevel level, Vec3 c, double strength, int count, double vel) {
        if (dry("jolt(" + f(strength) + "," + count + "," + f(vel) + ")")) return;
        RandomSource rand = level.random;
        for (int j = 0; j < count; j++) {
            double phi = rand.nextDouble() * (Math.PI * 2);
            double costheta = rand.nextDouble() * 2 - 1;
            double theta = Math.acos(costheta);
            double x = Math.sin(theta) * Math.cos(phi), y = Math.sin(theta) * Math.sin(phi), z = Math.cos(theta);
            for (int i = 0; i < strength; i++) {
                BlockPos b = new BlockPos((int) (c.x + x * i), (int) (c.y + y * i), (int) (c.z + z * i));
                BlockState state = level.getBlockState(b);
                if (state.liquid()) {
                    level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
                    state = Blocks.AIR.defaultBlockState();
                }
                if (!state.isAir()) {
                    if (state.getExplosionResistance(level, b, null) > 70) continue;
                    double rx = b.getX() + 0.5F, ry = b.getY() + 0.5F, rz = b.getZ() + 0.5F;
                    Vec3 motion = new Vec3(c.x - rx, c.y - ry, c.z - rz).scale(vel);
                    level.addFreshEntity(new MineRubbleEntity(level, rx, ry, rz, motion, state));
                    level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }
    }

    /** ExplosionLarge.buster. */
    private static void buster(ServerLevel level, Vec3 p, Vec3 vector, float strength, float depth) {
        if (dry("buster(" + f(strength) + "," + f(depth) + ")")) return;
        Vec3 v = vector.normalize();
        for (int i = 0; i < depth; i += 2)
            LegacyVnt.vanilla(level, null, p.x + v.x * i, p.y + v.y * i, p.z + v.z * i, strength, false, true);
    }

    /** ExplosionChaos.cluster(world, x, y, z, count, rotationYaw, rotationPitch, PI/4, PI/4, 1F). */
    private static void cluster(LegacyLauncherMissileEntity missile, Vec3 p, int count) {
        if (dry("cluster(" + count + ")")) return;
        ClusterSubmunitionEntity.spawn(missile.level(), p, count, missile.getYRot(), missile.getXRot());
    }

    /** ExplosionChaos.igniteFlammableBlocks. */
    private static void igniteFlammableBlocks(ServerLevel level, int x, int y, int z, int r) {
        if (dry("igniteFlammableBlocks(" + r + ")")) return;
        int r22 = r * r / 2;
        for (int xx = -r; xx < r; xx++) for (int yy = -r; yy < r; yy++) for (int zz = -r; zz < r; zz++) {
            if (xx * xx + yy * yy + zz * zz >= r22) continue;
            BlockPos b = new BlockPos(x + xx, y + yy, z + zz);
            if (level.getBlockState(b).isFlammable(level, b, Direction.UP) && level.getBlockState(b.above()).is(Blocks.AIR))
                level.setBlock(b.above(), Blocks.FIRE.defaultBlockState(), 3);
        }
    }

    /** ExplosionChaos.igniteAllBlocks. */
    private static void igniteAllBlocks(ServerLevel level, int x, int y, int z, int r) {
        if (dry("igniteAllBlocks(" + r + ")")) return;
        LegacyProjectileUtil.igniteAllBlocksLegacy(level, new BlockPos(x, y, z), r);
    }

    private static boolean isNormalCube(ServerLevel level, BlockPos pos, BlockState state) {
        return state.isRedstoneConductor(level, pos);
    }

    /** EntityMissileTest: slaked Sellafield sphere (the port's sellafield_slaked has no charge meta). */
    private static void missileTest(ServerLevel level, Vec3 p) {
        if (dry("sellafieldSphere(50)")) return;
        int x0 = (int) Math.floor(p.x), y0 = (int) Math.floor(p.y), z0 = (int) Math.floor(p.z);
        BlockState slaked = HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
        for (int dx = -50; dx <= 50; dx++) for (int dy = -50; dy <= 50; dy++) for (int dz = -50; dz <= 50; dz++) {
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > 50) continue;
            BlockPos t = new BlockPos(x0 + dx, y0 + dy, z0 + dz);
            BlockState state = level.getBlockState(t);
            if (isNormalCube(level, t, state)) {
                if (!state.is(slaked.getBlock())) level.setBlock(t, slaked, 3);
            } else {
                level.setBlock(t, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** EntityMissileTaint: createExplosion at the hit vector, 100 tries in the 11^3 cube of the hit block, meta 0. */
    private static void taintMissile(ServerLevel level, Entity source, Vec3 hitVec, BlockPos hitBlock) {
        createExplosion(level, source, hitVec, 5F);
        if (dry("taintCube(100,age=0)")) return;
        RandomSource rand = level.random;
        for (int i = 0; i < 100; i++) {
            BlockPos t = new BlockPos(rand.nextInt(11) + hitBlock.getX() - 5, rand.nextInt(11) + hitBlock.getY() - 5,
                    rand.nextInt(11) + hitBlock.getZ() - 5);
            BlockState state = level.getBlockState(t);
            if (isNormalCube(level, t, state) && !state.isAir())
                level.setBlock(t, HbmBlocks.TAINT.get().defaultBlockState().setValue(TaintBlock.AGE, 0), 2);
        }
    }

    private static void volcano(ServerLevel level, Vec3 p) {
        largeExplode(level, p, 10.0F, true, true, true, false);
        if (dry("volcanoBlocks")) return;
        int x0 = (int) Math.floor(p.x), y0 = (int) Math.floor(p.y), z0 = (int) Math.floor(p.z);
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            level.setBlock(new BlockPos(x0 + x, y0 + y, z0 + z), HbmBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(x0, y0, z0), HbmBlocks.VOLCANO_CORE.get().defaultBlockState(), 3);
    }

    /** EntityMissileShuttle: ExplosionNT(null, pos + 0.5, 20).overrideResolution(64) NOSOUND NOPARTICLE, rbmkmush 10, robin_explosion. */
    private static void shuttle(ServerLevel level, Vec3 p) {
        if (!dry("explosionNT(20,64,nosound,noparticle)+rbmkmush(10)+robin_explosion")) {
            Vec3 c = p.add(0.5D, 0.5D, 0.5D);
            Set<BlockPos> affected = LegacyVnt.allocate(level, c, 20F, 64, false);
            LegacyVnt.standardEntities(level, null, c, 40F);
            LegacyVnt.processBlocks(level, null, c, 20F, affected, 1F / 20F, 0);
            for (var player : level.players()) {
                if (player.distanceToSqr(p.x + 0.5D, p.y + 1D, p.z + 0.5D) > 250D * 250D) continue;
                level.sendParticles(player, HbmParticleTypes.RBMK_MUSH.get(), true, p.x + 0.5D, p.y + 1D, p.z + 0.5D, 0, 10.0D, 0.0D, 0.0D, 1.0D);
            }
            level.playSound(null, p.x, p.y, p.z, HbmSoundEvents.WEAPON_ROBIN_EXPLOSION.get(), SoundSource.PLAYERS,
                    4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        }
    }

    /** ExplosionNT.errosion */
    private static Map<Block, Block> erosion() {
        return Map.of(HbmBlocks.CONCRETE.get(), Blocks.GRAVEL,
                HbmBlocks.CONCRETE_SMOOTH.get(), Blocks.GRAVEL,
                HbmBlocks.BRICK_CONCRETE.get(), HbmBlocks.BRICK_CONCRETE_BROKEN.get(),
                HbmBlocks.BRICK_CONCRETE_BROKEN.get(), Blocks.GRAVEL);
    }

    /** new ExplosionNT(world, exploder, x, y, z, size).addAllAttrib(ERRODE).explode(), default resolution 16. */
    private static void explosionNTErode(ServerLevel level, Entity exploder, Vec3 c, float size) {
        if (dry("explosionNT(" + f(size) + ",errode)")) return;
        Map<Block, Block> erosion = erosion();
        int resolution = 16;
        Set<BlockPos> affected = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < resolution; i++) for (int j = 0; j < resolution; j++) for (int k = 0; k < resolution; k++) {
            if (i != 0 && i != resolution - 1 && j != 0 && j != resolution - 1 && k != 0 && k != resolution - 1) continue;
            double dx = (float) i / (resolution - 1.0F) * 2.0F - 1.0F;
            double dy = (float) j / (resolution - 1.0F) * 2.0F - 1.0F;
            double dz = (float) k / (resolution - 1.0F) * 2.0F - 1.0F;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len; dy /= len; dz /= len;
            float power = size * (0.7F + level.random.nextFloat() * 0.6F);
            double x = c.x, y = c.y, z = c.z;
            for (float step = 0.3F; power > 0.0F; power -= step * 0.75F) {
                cursor.set(Math.floor(x), Math.floor(y), Math.floor(z));
                if (!level.isInWorldBounds(cursor)) break;
                BlockState state = level.getBlockState(cursor);
                if (!state.isAir()) power -= (state.getExplosionResistance(level, cursor, null) + 0.3F) * step;
                if (!state.isAir() && power > 0.0F) affected.add(cursor.immutable());
                else if (erosion.containsKey(state.getBlock())) affected.add(cursor.immutable());
                x += dx * step; y += dy * step; z += dz * step;
            }
        }
        LegacyVnt.standardEntities(level, exploder, c, size * 2.0F);
        LegacyVnt.vanillaEffect(level, c);
        Set<BlockPos> normal = new HashSet<>();
        List<BlockPos> eroding = new ArrayList<>();
        for (BlockPos b : affected) {
            BlockState state = level.getBlockState(b);
            if (state.isAir()) continue;
            if (level.random.nextFloat() < 0.6F && erosion.containsKey(state.getBlock())) eroding.add(b);
            else normal.add(b);
        }
        LegacyVnt.processBlocks(level, exploder, c, size, normal, 1.0F / size, 0);
        Explosion compat = new Explosion(level, exploder, c.x, c.y, c.z, size, false, Explosion.BlockInteraction.DESTROY);
        for (BlockPos b : eroding) {
            BlockState state = level.getBlockState(b);
            boolean cube = isNormalCube(level, b, state);
            state.onBlockExploded(level, b, compat);
            if (cube) level.setBlock(b, erosion.get(state.getBlock()).defaultBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------ debris

    /** ExplosionLarge.spawnMissileDebris. */
    private static void spawnMissileDebris(ServerLevel level, Vec3 p, Vec3 m, double deviation,
                                           @Nullable List<ItemStack> debris, @Nullable ItemStack rare) {
        if (dry("missileDebris(" + (debris == null ? "null" : debris.stream().map(s -> id(s) + "x" + s.getCount()).toList())
                + "," + (rare == null ? "null" : id(rare)) + ")")) return;
        RandomSource rand = level.random;
        if (debris != null) for (ItemStack stack : debris) {
            int k = rand.nextInt(stack.getCount() + 1);
            for (int j = 0; j < k; j++) {
                ItemEntity item = new ItemEntity(level, p.x, p.y, p.z, stack.copy());
                Vec3 v = new Vec3((m.x + rand.nextGaussian() * deviation) * 0.85, (m.y + rand.nextGaussian() * deviation) * 0.85,
                        (m.z + rand.nextGaussian() * deviation) * 0.85);
                item.setDeltaMovement(v);
                item.setPos(p.x + v.x * 2, p.y + v.y * 2, p.z + v.z * 2);
                level.addFreshEntity(item);
            }
        }
        if (rare != null && rand.nextInt(10) == 0) {
            ItemEntity item = new ItemEntity(level, p.x, p.y, p.z, rare.copy());
            item.setDeltaMovement(m.x + rand.nextGaussian() * deviation * 0.1, m.y + rand.nextGaussian() * deviation * 0.1,
                    m.z + rand.nextGaussian() * deviation * 0.1);
            level.addFreshEntity(item);
        }
    }

    private static String id(ItemStack stack) {
        if (stack.getItem() instanceof com.atomiclegacy.items.LegacyVariantItem v) return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "_" + v.variant(stack).id();
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** getDebris() of each missile class; null for the doomsday missiles. */
    @Nullable static List<ItemStack> debris(String id) {
        return switch (id) {
            case "missile_test", "missile_micro", "missile_schrabidium", "missile_bhole", "missile_taint", "missile_emp" ->
                    List.of(item("wire_aluminium", 4), item("plate_titanium", 4), item("shell_aluminium", 2), item("ducttape", 1));
            case "missile_generic", "missile_decoy", "missile_incendiary", "missile_cluster", "missile_buster" ->
                    List.of(item("plate_titanium", 4), item("thruster_small", 1));
            case "missile_strong", "missile_incendiary_strong", "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong" ->
                    List.of(item("plate_steel", 10), item("plate_titanium", 6), item("thruster_medium", 1));
            case "missile_burst", "missile_inferno", "missile_rain", "missile_drill" ->
                    List.of(item("plate_steel", 16), item("plate_titanium", 10), item("thruster_large", 1));
            case "missile_nuclear", "missile_nuclear_cluster", "missile_volcano" ->
                    List.of(item("plate_titanium", 16), item("plate_steel", 20), item("plate_aluminium", 12), item("thruster_large", 1));
            case "missile_doomsday", "missile_doomsday_rusted" -> null;
            case "missile_stealth" -> List.of(item("bolt_steel", 4));
            case "missile_shuttle" -> List.of(item("plate_steel", 8), item("thruster_medium", 2), item("canister_empty", 1),
                    new ItemStack(net.minecraft.world.item.Items.GLASS_PANE, 2));
            default -> throw new IllegalStateException("No debris table for " + id);
        };
    }

    /** getDebrisRareDrop() of each missile class. */
    @Nullable static ItemStack rareDrop(String id) {
        String path = switch (id) {
            case "missile_test", "missile_schrabidium", "missile_doomsday", "missile_doomsday_rusted" -> null;
            case "missile_micro" -> "ammo_standard_nuke_high";
            case "missile_bhole" -> "black_hole";
            case "missile_taint" -> "powder_spark_mix";
            case "missile_emp" -> "emp_bomb";
            case "missile_generic" -> "warhead_generic_small";
            case "missile_decoy" -> "ingot_steel";
            case "missile_incendiary" -> "warhead_incendiary_small";
            case "missile_cluster" -> "warhead_cluster_small";
            case "missile_buster" -> "warhead_buster_small";
            case "missile_strong", "missile_emp_strong" -> "warhead_generic_medium";
            case "missile_incendiary_strong" -> "warhead_incendiary_medium";
            case "missile_cluster_strong" -> "warhead_cluster_medium";
            case "missile_buster_strong" -> "warhead_buster_medium";
            case "missile_burst" -> "warhead_generic_large";
            case "missile_inferno" -> "warhead_incendiary_large";
            case "missile_rain" -> "warhead_cluster_large";
            case "missile_drill" -> "warhead_buster_large";
            case "missile_nuclear" -> "warhead_nuclear";
            case "missile_nuclear_cluster" -> "warhead_mirv";
            case "missile_volcano" -> "warhead_volcano";
            case "missile_stealth" -> "powder_ash_misc";
            case "missile_shuttle" -> "missile_generic";
            default -> throw new IllegalStateException("No rare drop for " + id);
        };
        if ("ammo_standard_nuke_high".equals(path)) // DictFrame.fromOne(ammo_standard, EnumAmmo.NUKE_HIGH)
            return com.atomiclegacy.items.LegacyVariantItem.stackFor(item("ammo_standard", 1).getItem(), "nuke_high");
        return path == null ? null : item(path, 1);
    }

    private static ItemStack item(String path, int count) {
        var key = ResourceLocation.fromNamespaceAndPath("atomiclegacy", path);
        var item = BuiltInRegistries.ITEM.get(key);
        if (item == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("Missing legacy missile debris item: " + key);
        return new ItemStack(item, count);
    }
}

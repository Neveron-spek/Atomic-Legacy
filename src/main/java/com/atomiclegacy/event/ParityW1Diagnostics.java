package com.atomiclegacy.event;

import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyC130Entity;
import com.atomiclegacy.entity.LegacyParachuteCrateEntity;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory40mm;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.tileentity.HbmStructureLoot;
import com.atomiclegacy.tileentity.SupplyCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** [parity W1: diagnostics] EntityC130 / EntityParachuteCrate airdrop from the 26mm supply/weapon flares. */
final class ParityW1Diagnostics {
    private ParityW1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            registrations(problems, cases);
            pools(problems, cases);
            flare(level, StandardAmmoType.G26_FLARE_SUPPLY, LegacyC130Entity.PayloadType.SUPPLIES, problems, cases);
            flare(level, StandardAmmoType.G26_FLARE_WEAPON, LegacyC130Entity.PayloadType.WEAPONS, problems, cases);
            flight(level, LegacyC130Entity.PayloadType.SUPPLIES, problems, cases);
            flight(level, LegacyC130Entity.PayloadType.WEAPONS, problems, cases);
            damage(level, problems, cases);
            crate(level, problems, cases);
        } catch (RuntimeException e) {
            problems.add("W1 airdrop threw " + e);
        }
        return cases[0];
    }

    private static void registrations(List<String> problems, int[] cases) {
        EntityType<?> c130 = HbmEntityTypes.LEGACY_C130.get();
        EntityType<?> crate = HbmEntityTypes.LEGACY_PARACHUTE_CRATE.get();
        check(c130.getWidth() == 8.0F && c130.getHeight() == 4.0F, problems, cases, "entity_c130 setSize(8, 4)");
        check(c130.clientTrackingRange() == 63 && crate.clientTrackingRange() == 63, problems, cases,
                "entity_c130 / entity_parachute_crate tracking range 1000 (63 chunks)");
        check(crate.fireImmune() && crate.getWidth() == 0.6F && crate.getHeight() == 1.8F, problems, cases,
                "entity_parachute_crate isImmuneToFire, default 0.6x1.8 size");
        check(StandardAmmoType.G26_FLARE_SUPPLY.onUpdate() == XFactory40mm.LAMBDA_SPAWN_C130_SUPPLIESS
                && StandardAmmoType.G26_FLARE_WEAPON.onUpdate() == XFactory40mm.LAMBDA_SPAWN_C130_WEAPONS
                && StandardAmmoType.G26_FLARE.onUpdate() == null, problems, cases,
                "g26_flare_supply/weapon setOnUpdate(LAMBDA_SPAWN_C130_*), g26_flare none");
    }

    private static void pools(List<String> problems, int[] cases) {
        check(HbmStructureLoot.displayEntries("POOL_SUPPLIES").size() == 8
                && HbmStructureLoot.displayEntries("POOL_WEAPONS").size() == 10
                && HbmStructureLoot.displayEntries("POOL_AMMO").size() == 10, problems, cases,
                "ItemPoolsC130 pools complete (8/10/10 existing entries)");
    }

    private static BlockPos base(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        level.getChunkAt(spawn);
        return new BlockPos(spawn.getX(), 280, spawn.getZ());
    }

    private static void flare(ServerLevel level, StandardAmmoType ammo, LegacyC130Entity.PayloadType payload,
                              List<String> problems, int[] cases) {
        BlockPos base = base(level);
        LegacyBulletEntity bullet = new LegacyBulletEntity(level, base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D,
                new Vec3(0.0D, 1.0D, 0.0D), ammo, 0.0F);
        int x = (int) Math.floor(bullet.getX());
        int z = (int) Math.floor(bullet.getZ());
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        AABB search = new AABB(x - 260, y + 90, z - 260, x + 260, y + 110, z + 260);
        List<LegacyC130Entity> before = level.getEntitiesOfClass(LegacyC130Entity.class, search);

        int spawnedEarly = 0;
        for (int tick : new int[] {39, 41}) {
            bullet.tickCount = tick;
            ammo.onUpdate().onUpdate(bullet);
            spawnedEarly += newPlanes(level, search, before).size();
        }
        check(spawnedEarly == 0, problems, cases, ammo.id() + " spawns no C-130 outside tick 40");

        bullet.tickCount = 40;
        ammo.onUpdate().onUpdate(bullet);
        List<LegacyC130Entity> planes = newPlanes(level, search, before);
        check(planes.size() == 1, problems, cases, ammo.id() + " spawns exactly one C-130 at bullet tick 40 (got " + planes.size() + ")");
        for (LegacyC130Entity plane : planes) {
            double dx = x - plane.getX();
            double dz = z - plane.getZ();
            Vec3 motion = plane.getDeltaMovement();
            check(Math.abs(Math.sqrt(dx * dx + dz * dz) - 200.0D) < 1.0E-6D && plane.getY() == y + 100.0D,
                    problems, cases, "C-130 starts 200 blocks before the flare column, getHeightValue + 100");
            check(Math.abs(motion.length() - 2.0D) < 1.0E-9D && motion.y == 0.0D
                            && Math.abs(dx - motion.x * 100.0D) < 1.0E-6D && Math.abs(dz - motion.z * 100.0D) < 1.0E-6D,
                    problems, cases, "C-130 heads for the column at 2 blocks/tick");
            check(plane.payload == payload, problems, cases, "C-130 payload " + payload);
            check(plane.trackingRangeOverride() == LegacyC130Entity.FLARE_TRACKING_RANGE, problems, cases,
                    "TrackerUtil.setTrackingRange(c130, 250)");
            check(Math.abs(plane.getXRot() + 90.0F) < 1.0E-4F, problems, cases, "level-flight pitch -90 (EntityPlaneBase#rotation)");
            plane.discard();
        }
        bullet.discard();
    }

    private static List<LegacyC130Entity> newPlanes(ServerLevel level, AABB search, List<LegacyC130Entity> before) {
        List<LegacyC130Entity> result = new ArrayList<>();
        for (LegacyC130Entity plane : level.getEntitiesOfClass(LegacyC130Entity.class, search)) {
            if (!before.contains(plane) && !plane.isRemoved()) result.add(plane);
        }
        return result;
    }

    private static void tick(Entity entity) {
        entity.setOldPosAndRot();
        entity.tickCount++;
        entity.tick();
    }

    private static void flight(ServerLevel level, LegacyC130Entity.PayloadType payload, List<String> problems, int[] cases) {
        BlockPos base = base(level);
        int x = base.getX();
        int z = base.getZ();
        int y = 180;
        LegacyC130Entity plane = new LegacyC130Entity(level);
        plane.fac(level, x, y, z, payload);
        LegacyC130Entity.loadAndSpawn(level, plane);
        AABB search = new AABB(x - 40, y + 80, z - 40, x + 40, y + 100, z + 40);
        List<LegacyParachuteCrateEntity> before = level.getEntitiesOfClass(LegacyParachuteCrateEntity.class, search);
        for (int i = 0; i < 99; i++) tick(plane);
        boolean earlyDrop = level.getEntitiesOfClass(LegacyParachuteCrateEntity.class, search).size() != before.size();
        tick(plane);
        Vec3 motion = plane.getDeltaMovement();
        check(Math.abs(plane.getX() - x) < 1.0E-6D && Math.abs(plane.getZ() - z) < 1.0E-6D && plane.getY() == y + 100.0D,
                problems, cases, "C-130 is over the target at tick 100");
        List<LegacyParachuteCrateEntity> crates = new ArrayList<>();
        for (LegacyParachuteCrateEntity crate : level.getEntitiesOfClass(LegacyParachuteCrateEntity.class, search)) {
            if (!before.contains(crate)) crates.add(crate);
        }
        check(!earlyDrop && crates.size() == 1, problems, cases, payload + " crate dropped once at tick lifetime/2 = 100");
        for (LegacyParachuteCrateEntity crate : crates) {
            check(Math.abs(crate.getX() - (x - motion.x * 7.0D)) < 1.0E-6D && crate.getY() == y + 90.0D
                            && Math.abs(crate.getZ() - (z - motion.z * 7.0D)) < 1.0E-6D,
                    problems, cases, "crate released at (pos - motion * 7, posY - 10)");
            if (payload == LegacyC130Entity.PayloadType.SUPPLIES) {
                check(crate.items.size() == 5 && crate.items.stream().allMatch(s -> inPool(s, "POOL_SUPPLIES")),
                        problems, cases, "supply crate: 5 rolls of POOL_SUPPLIES");
            } else {
                int n = crate.items.size();
                boolean ok = (n == 7 || n == 8);
                for (int i = 0; i < n && ok; i++) {
                    ok = inPool(crate.items.get(i), i < n - 6 ? "POOL_WEAPONS" : "POOL_AMMO");
                }
                check(ok, problems, cases, "weapon crate: 1-2 POOL_WEAPONS then 6 POOL_AMMO rolls (got " + n + ")");
            }
            crate.discard();
        }
        plane.tickCount = LegacyC130Entity.LIFETIME - 1;
        tick(plane);
        boolean aliveAt200 = !plane.isRemoved();
        tick(plane);
        check(aliveAt200 && plane.isRemoved(), problems, cases, "C-130 removed when ticksExisted > 200");
        if (!plane.isRemoved()) plane.discard();
    }

    private static boolean inPool(ItemStack stack, String pool) {
        if (stack.isEmpty()) return false;
        for (Map.Entry<ItemStack, Integer> entry : HbmStructureLoot.displayEntries(pool)) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), stack)) return true;
        }
        return false;
    }

    private static void damage(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos base = base(level);
        LegacyC130Entity plane = new LegacyC130Entity(level);
        plane.fac(level, base.getX(), 180, base.getZ(), LegacyC130Entity.PayloadType.SUPPLIES);
        LegacyC130Entity.loadAndSpawn(level, plane);
        check(!plane.hurt(level.damageSources().source(HbmDamageTypes.NUCLEAR_BLAST), 100.0F) && plane.health == 50.0F,
                problems, cases, "C-130 ignores nuclearBlast damage");
        check(plane.hurt(level.damageSources().generic(), 49.0F) && plane.health == 1.0F && plane.isPickable(),
                problems, cases, "C-130 has 50 HP");
        plane.hurt(level.damageSources().generic(), 1.0F);
        check(!plane.isPickable(), problems, cases, "shot-down C-130 is no longer collidable");
        double vy = plane.getDeltaMovement().y;
        tick(plane);
        check(Math.abs(plane.getDeltaMovement().y - (vy - 0.025D)) < 1.0E-9D && plane.syncedHealth() <= 0.0F,
                problems, cases, "shot-down C-130 falls with motionY -= 0.025");
        plane.tickCount = LegacyC130Entity.LIFETIME / 2 - 1;
        AABB search = plane.getBoundingBox().inflate(40.0D);
        int crates = level.getEntitiesOfClass(LegacyParachuteCrateEntity.class, search).size();
        tick(plane);
        check(level.getEntitiesOfClass(LegacyParachuteCrateEntity.class, search).size() == crates,
                problems, cases, "shot-down C-130 drops no crate");
        plane.discard();
    }

    private static void crate(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos base = base(level);
        BlockPos floor = base.below(5);
        for (int dy = -1; dy <= 6; dy++) level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        LegacyParachuteCrateEntity crate = new LegacyParachuteCrateEntity(level);
        crate.setPos(floor.getX() + 0.5D, floor.getY() + 5.5D, floor.getZ() + 0.5D);
        crate.items.add(new ItemStack(Items.DIAMOND, 3));
        level.addFreshEntity(crate);
        tick(crate);
        check(Math.abs(crate.getDeltaMovement().y + 0.02D) < 1.0E-12D, problems, cases, "crate gains -0.02 motionY per tick");
        int ticks = 1;
        while (!crate.isRemoved() && ticks < 200) {
            tick(crate);
            ticks++;
        }
        double terminal = 0.0D;
        for (int i = 0; i < 20; i++) if (terminal > -0.2D) terminal -= 0.02D; // -0.22 in double arithmetic
        check(crate.getDeltaMovement().y == terminal, problems, cases, "crate terminal motionY (" + terminal + ")");
        BlockPos cratePos = floor.above();
        check(crate.isRemoved() && level.getBlockState(cratePos).is(HbmBlocks.CRATE_SUPPLY.get()), problems, cases,
                "crate lands on the first non-air block and places crate_supply above it");
        if (level.getBlockEntity(cratePos) instanceof SupplyCrateBlockEntity be) {
            List<ItemStack> contents = be.takeContents();
            check(contents.size() == 1 && contents.get(0).is(Items.DIAMOND) && contents.get(0).getCount() == 3,
                    problems, cases, "crate_supply receives the crate items");
        } else {
            check(false, problems, cases, "crate_supply has its tile entity");
        }
        level.setBlock(cratePos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floor, Blocks.AIR.defaultBlockState(), 3);

        LegacyParachuteCrateEntity high = new LegacyParachuteCrateEntity(level);
        high.setPos(base.getX() + 0.5D, 700.0D, base.getZ() + 0.5D);
        tick(high);
        check(high.getY() == 600.0D && !high.isRemoved(), problems, cases, "crate posY capped at 600");
        high.discard();
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) {
            problems.add("W1 " + message);
        }
    }
}

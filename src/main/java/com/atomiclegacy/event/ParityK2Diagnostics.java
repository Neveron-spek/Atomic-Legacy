package com.atomiclegacy.event;

import com.mojang.authlib.GameProfile;
import com.atomiclegacy.blocks.LegacyBlockHazardRadiation;
import com.atomiclegacy.blocks.SellafieldBlock;
import com.atomiclegacy.explosion.LegacyMukeExplosion;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmHazardSystem;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.radiation.HbmRadiationWorlds;
import com.atomiclegacy.radiation.LegacyRadiate;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.util.ContaminationUtil;
import com.atomiclegacy.util.ContaminationUtil.ContaminationType;
import com.atomiclegacy.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;
import java.util.UUID;

/**
 * [parity K2] Direct radiation sources and the chunk radiation system (1.7.10 ChunkRadiationHandlerSimple) checked
 * against the numbers of work/original. Nukes are not detonated: their dose functions are called directly.
 */
public final class ParityK2Diagnostics {
    private ParityK2Diagnostics() {
    }

    /** Chunk-aligned site far from (0, 0) and from the other diagnostics. */
    private static final int BX = -12288, BZ = 12288, Y = 300;
    private static final int CX = BX >> 4, CZ = BZ >> 4;
    private static final float EPS = 1.0E-3F;

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        for (int cx = CX - 3; cx <= CX + 6; cx++) {
            for (int cz = CZ - 3; cz <= CZ + 3; cz++) {
                level.getChunk(cx, cz);
            }
        }
        try {
            chunk(level, problems, cases);
            destruction(level, problems, cases);
            blocks(level, problems, cases);
            contact(server, level, problems, cases);
            radiate(level, problems, cases);
            nukes(level, problems, cases);
        } catch (Throwable t) {
            StackTraceElement[] trace = t.getStackTrace();
            problems.add("parity K2: exception " + t + (trace.length > 0 ? " at " + trace[0] : "")
                    + (trace.length > 1 ? " < " + trace[1] : ""));
        }
        cleanup(level);
        return cases[0];
    }

    /// ChunkRadiationHandlerSimple: storage, clamp, spread, decay, overwrite, NBT ///
    private static void chunk(ServerLevel level, List<String> problems, int[] cases) {
        ChunkRadiationData data = ChunkRadiationData.get(level);
        forgetSite(level);
        BlockPos c = new BlockPos(BX + 8, Y, BZ + 8);
        data.incrementRadiation(c, 12.5D);
        data.incrementRadiation(c, 2.5D);
        check(problems, cases, near(data.getRadiation(c), 15F), "incrementRad adds (got " + data.getRadiation(c) + ")");
        data.setRadiation(c, 250_000D);
        check(problems, cases, near(data.getRadiation(c), 100_000F), "maxRad 100 000 clamp (got " + data.getRadiation(c) + ")");
        data.incrementRadiation(c, 1_000_000D, 10D);
        check(problems, cases, near(data.getRadiation(c), 100_000F), "no per-source cap (max argument ignored)");
        data.decrementRadiation(c, 200_000D);
        check(problems, cases, data.getRadiation(c) == 0F, "decrementRad floors at 0");
        BlockPos unloaded = new BlockPos(29_000_000 - 64, Y, 29_000_000 - 64);
        data.incrementRadiation(unloaded, 50D);
        check(problems, cases, data.getRadiation(unloaded) == 0F, "writes into unloaded chunks are dropped (blockExists)");

        // one updateSystem pass, single source of 1000 in a tracked chunk, neighbours untracked
        forgetSite(level);
        data.setRadiation(c, 1000D);
        HbmRadiationWorlds.parityK2UpdateSystem(level);
        float center = raw(level, CX, CZ), side = raw(level, CX + 1, CZ), diag = raw(level, CX + 1, CZ + 1);
        check(problems, cases, near(center, 1000F * 0.6F * 0.99F - 0.05F) && near(side, 75F) && near(diag, 25F),
                "spread 60/7.5/2.5 %, decay only for tracked chunks: " + center + " / " + side + " / " + diag);
        // second pass from 9 sources: total is conserved up to the decay of every contribution
        float before = sum(level, CX, CZ, 1);
        HbmRadiationWorlds.parityK2UpdateSystem(level);
        float after = sum(level, CX, CZ, 2);
        // decay is applied once per contribution (up to 9 times per chunk), so about 7 % is lost here
        check(problems, cases, after < before * 0.99F && after > before * 0.9F, "second pass: total " + before + " -> " + after);

        // overwrite: two sources share an untracked neighbour -> value * percent, not the sum
        forgetSite(level);
        data.setRadiation(new BlockPos(BX, Y, BZ), 1000D);
        data.setRadiation(new BlockPos(BX + 32, Y, BZ), 1000D);
        HbmRadiationWorlds.parityK2UpdateSystem(level);
        check(problems, cases, near(raw(level, CX + 1, CZ), 75F), "untracked shared neighbour is overwritten: " + raw(level, CX + 1, CZ));
        // tracked shared neighbour (0 entry): decay applied per contribution
        forgetSite(level);
        data.setRadiation(new BlockPos(BX, Y, BZ), 1000D);
        data.setRadiation(new BlockPos(BX + 32, Y, BZ), 1000D);
        data.setRadiation(new BlockPos(BX + 16, Y, BZ), 0D);
        HbmRadiationWorlds.parityK2UpdateSystem(level);
        float tracked = ((0F + 75F) * 0.99F - 0.05F + 75F) * 0.99F - 0.05F;
        check(problems, cases, near(raw(level, CX + 1, CZ), tracked), "tracked shared neighbour " + raw(level, CX + 1, CZ) + " expected " + tracked);
        // small values decay to 0 and 0 entries stop spreading
        forgetSite(level);
        data.setRadiation(c, 0.04D);
        HbmRadiationWorlds.parityK2UpdateSystem(level);
        check(problems, cases, raw(level, CX, CZ) == 0F, "0.04 RAD decays to 0 in one pass");
        HbmRadiationWorlds.parityK2UpdateSystem(level);
        check(problems, cases, !HbmRadiationWorlds.parityK2Tracked(level, CX, CZ) || raw(level, CX, CZ) == 0F, "0 entries are not sources");

        // chunk NBT (hfr_simple_radiation): save returns the map value, load puts the stored value
        forgetSite(level);
        data.setRadiation(c, 42D);
        check(problems, cases, near(HbmRadiationWorlds.chunkDataSaved(level, new ChunkPos(CX, CZ)), 42F), "chunk save writes the value");
        HbmRadiationWorlds.parityK2ChunkLoad(level, new ChunkPos(CX, CZ), 7F);
        check(problems, cases, near(data.getRadiation(c), 7F), "chunk load restores the stored value");
        check(problems, cases, "hfr_simple_radiation".equals(ChunkRadiationData.NBT_KEY), "NBT key hfr_simple_radiation");
        forgetSite(level);
    }

    /// handleWorldDestruction: threshold 10, grass -> waste_earth, tall grass -> air ///
    private static void destruction(ServerLevel level, List<String> problems, int[] cases) {
        int cx = CX + 4, cz = CZ;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx * 16, cz * 16) + 3;
        surface(level, cx, cz, top);
        HbmRadiationWorlds.parityK2WorldDestruction(level, cx, cz, 9.99F);
        check(problems, cases, countSurface(level, cx, cz, top, "waste_earth") == 0, "below 10 RAD no world destruction");
        HbmRadiationWorlds.parityK2WorldDestruction(level, cx, cz, 10F);
        int waste = countSurface(level, cx, cz, top, "waste_earth");
        int grass = 0;
        for (int a = 0; a < 16; a++) {
            for (int b = 0; b < 16; b++) {
                if (level.getBlockState(new BlockPos(cx * 16 + a, top + 1, cz * 16 + b)).is(Blocks.SHORT_GRASS)) {
                    grass++;
                }
            }
        }
        // per column 50 tries with 1/3 * 1/2 for each of the two blocks
        check(problems, cases, waste >= 240 && grass <= 16, "10 RAD: grass -> waste_earth " + waste + "/256, short grass left " + grass);
        for (int a = 0; a < 16; a++) {
            for (int b = 0; b < 16; b++) {
                level.setBlock(new BlockPos(cx * 16 + a, top, cz * 16 + b), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx * 16 + a, top + 1, cz * 16 + b), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    /// BlockHazard / BlockHazardFalling / BlockSellafield / BlockOre(ore_schrabidium) world emission ///
    private static void blocks(ServerLevel level, List<String> problems, int[] cases) {
        ChunkRadiationData data = ChunkRadiationData.get(level);
        BlockPos pos = new BlockPos(BX + 8, Y, BZ + 8);
        Block uranium = HbmBlocks.BLOCK_URANIUM.get();
        float expected = (float) HbmHazardSystem.rawRadiation(new ItemStack(uranium)) * 0.1F;
        forgetSite(level);
        level.setBlock(pos, uranium.defaultBlockState(), 3);
        boolean scheduled = level.getBlockTicks().hasScheduledTick(pos, uranium);
        level.getBlockState(pos).tick(level, pos, level.random);
        check(problems, cases, expected > 0 && scheduled && near(data.getRadiation(pos), expected)
                        && level.getBlockTicks().hasScheduledTick(pos, uranium),
                "block_uranium: onBlockAdded rad = item * 0.1 (" + expected + "), +rad per update, next update scheduled; got " + data.getRadiation(pos));
        forgetSite(level);
        level.getBlockState(pos).randomTick(level, pos, level.random);
        check(problems, cases, data.getRadiation(pos) == 0F, "BlockHazard does not tick randomly");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        Block ore = HbmBlocks.ORE_SCHRABIDIUM.get();
        level.setBlock(pos, ore.defaultBlockState(), 3);
        forgetSite(level);
        level.getBlockState(pos).randomTick(level, pos, level.random);
        check(problems, cases, near(data.getRadiation(pos), 0.1F), "ore_schrabidium: BlockOre rad 0.1 per update (got " + data.getRadiation(pos) + ")");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        Block sellafield = HbmBlocks.SELLAFIELD.get();
        LegacyBlockHazardRadiation.resetForTest(sellafield);
        BlockState s3 = sellafield.defaultBlockState().setValue(SellafieldBlock.LEVEL, 3);
        level.setBlock(pos, s3, 2 | 16);
        forgetSite(level);
        // the placement above already ran onBlockAdded; emulate "before any placement" with the reset field
        LegacyBlockHazardRadiation.resetForTest(sellafield);
        level.getBlockState(pos).randomTick(level, pos, level.random);
        float first = (float) data.getRadiation(pos);
        float afterAdd = (float) HbmHazardSystem.rawRadiation(new ItemStack(sellafield)) * 0.1F;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, s3, 3);
        forgetSite(level);
        BlockState placed = level.getBlockState(pos);
        if (placed.is(sellafield)) {
            placed.randomTick(level, pos, level.random);
        }
        float second = (float) data.getRadiation(pos);
        check(problems, cases, near(first, 0.5F * 4) && (!placed.is(sellafield) || near(second, 0.5F * 4)),
                "sellafield meta 3: 0.5 * 4 before a placement (" + first + "), 0.5 * 4 after too (onBlockAdded is empty) (" + second + ")");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        forgetSite(level);
    }

    /// contact sources: corium, fluids, radon, digamma flame, meltdown gas chunk emission ///
    private static void contact(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = new BlockPos(BX + 8, Y, BZ + 8);
        Object[][] table = {
                {"corium_block", HbmBlocks.CORIUM_BLOCK.get(), 1F, false},
                {"toxic_block", HbmBlocks.TOXIC_BLOCK.get(), 1F, false},
                {"schrabidic_block", HbmBlocks.SCHRABIDIC_BLOCK.get(), 1F, false},
                {"rad_lava_block", HbmBlocks.RAD_LAVA_BLOCK.get(), 5F, false},
                {"gas_radon", HbmBlocks.GAS_RADON.get(), 0.05F, true},
                {"gas_radon_tomb", HbmBlocks.GAS_RADON_TOMB.get(), 0.5F, true},
        };
        FakePlayer player = player(level);
        for (Object[] row : table) {
            BlockState state = ((Block) row[1]).defaultBlockState();
            float dose = (float) row[2];
            boolean bypass = (boolean) row[3];
            Pig pig = pig(level);
            state.entityInside(level, pos, pig);
            check(problems, cases, near(rad(pig), dose), row[0] + ": mob gets " + dose + " (got " + rad(pig) + ")");
            pig.discard();
            reset(player);
            state.entityInside(level, pos, player);
            check(problems, cases, near(rad(player), dose * (bypass ? 1F : ContaminationUtil.calculateRadiationMod(player))),
                    row[0] + ": survival player (got " + rad(player) + ")");
            reset(player);
            player.setGameMode(GameType.CREATIVE);
            state.entityInside(level, pos, player);
            check(problems, cases, rad(player) == 0F && near(env(player), dose), row[0] + ": creative blocked, reading " + env(player));
            reset(player);
            player.tickCount = 100;
            state.entityInside(level, pos, player);
            check(problems, cases, rad(player) == 0F, row[0] + ": players younger than 200 ticks are skipped");
        }
        // digamma flame: contaminate(DIGAMMA, DIGAMMA, 0.05F)
        Pig pig = pig(level);
        HbmBlocks.FIRE_DIGAMMA.get().defaultBlockState().entityInside(level, pos, pig);
        check(problems, cases, near(HbmLivingRadiation.get(pig).getDigamma(), 0.05F), "fire_digamma: 0.05 digamma (got " + HbmLivingRadiation.get(pig).getDigamma() + ")");
        pig.discard();
        // meltdown gas: incrementRad(5) under open sky, no cap
        forgetSite(level);
        ChunkRadiationData.get(level).setRadiation(pos, 20_000D);
        BlockPos gas = pos.above(5);
        level.setBlock(gas, HbmBlocks.GAS_MELTDOWN.get().defaultBlockState(), 2);
        level.getBlockState(gas).tick(level, gas, level.random);
        check(problems, cases, near(ChunkRadiationData.get(level).getRadiation(pos), 20_005F),
                "gas_meltdown: +5 chunk RAD with sky access, no 5000 cap (got " + ChunkRadiationData.get(level).getRadiation(pos) + ")");
        level.setBlock(gas, Blocks.AIR.defaultBlockState(), 2);
        forgetSite(level);
        reset(player);
    }

    /// shared radiate loop (drum, ZIRNOX, demon lamp, RBMK debris, logic block) ///
    private static void radiate(ServerLevel level, List<String> problems, int[] cases) {
        Vec3 source = new Vec3(BX + 8.5, Y + 0.5, BZ + 8.5);
        Pig pig = pig(level);
        pig.setPos(source.x + 10, Y, source.z);
        double len = new Vec3(pig.getX() - source.x, pig.getEyeY() - source.y, pig.getZ() - source.z).length();
        LegacyRadiate.dose(level, source, pig, 75_000F, false, ContaminationType.CREATIVE);
        float expected = (float) (75_000F / 1F / (float) (len * len));
        check(problems, cases, near(env(pig), expected), "radiate in air: rads / len^2 = " + expected + " (got " + env(pig) + ")");
        // a stone wall on the ray: res = 6 (explosion resistance)
        BlockPos wall = BlockPos.containing(source.x + 5, source.y, source.z);
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), 2);
        reset(pig);
        LegacyRadiate.dose(level, source, pig, 75_000F, false, ContaminationType.CREATIVE);
        float res = sumResistance(level, source, pig, len);
        check(problems, cases, res >= 6F && near(env(pig), (float) (75_000F / res / (float) (len * len))),
                "radiate through stone: res " + res + ", got " + env(pig));
        level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2);
        // logic block: len at least 1, HAZMAT2
        reset(pig);
        pig.setPos(source.x, source.y - pig.getEyeHeight(), source.z);
        LegacyRadiate.dose(level, source, pig, 100F, true, ContaminationType.HAZMAT2);
        check(problems, cases, near(env(pig), 100F), "logic block: len clamped to 1 -> 100 RAD (got " + env(pig) + ")");
        pig.discard();
    }

    /// MK5 prompt radiation, solinium, mini nuke chunk radiation ///
    private static void nukes(ServerLevel level, List<String> problems, int[] cases) {
        Vec3 center = new Vec3(BX + 8.5, Y + 0.5, BZ + 8.5);
        Pig pig = pig(level);
        pig.setPos(center.x + 20, Y, center.z);
        double len = new Vec3(pig.getX() - center.x, pig.getEyeY() - center.y, pig.getZ() - center.z).length();
        NukeExplosionManager.parityK2PromptRadiation(level, center, 150, pig, 0);
        check(problems, cases, env(pig) == 0F, "MK5: no radiation on tick 1 (explosion not created yet)");
        NukeExplosionManager.parityK2PromptRadiation(level, center, 150, pig, 1);
        float expected = 2_500_000F / (2 * 5 + 1) / 1F / (float) (len * len);
        check(problems, cases, near(env(pig), expected), "MK5 tick 2: 2.5M / 11 / len^2 = " + expected + " (got " + env(pig) + ")");
        reset(pig);
        NukeExplosionManager.parityK2PromptRadiation(level, center, 150, pig, 9);
        NukeExplosionManager.parityK2PromptRadiation(level, center, 74, pig, 1);
        check(problems, cases, env(pig) == 0F, "MK5: nothing from tick 10 on and below strength 75");
        reset(pig);
        NukeExplosionManager.parityK2SoliniumRadiation(center, 100D, pig);
        double d = Math.sqrt(pig.distanceToSqr(center));
        float sol = (float) (15000F + (250000F - 15000F) * (1D - d / 100D));
        check(problems, cases, near(env(pig), sol) && near(rad(pig), HbmLivingRadiation.MAX_RADIATION),
                "solinium: 15000 + 235000 * (1 - d / r) = " + sol + " (got " + env(pig) + ")");
        pig.discard();

        ChunkRadiationData data = ChunkRadiationData.get(level);
        forgetSite(level);
        LegacyMukeExplosion.detonateSafeMiniNuke(level, null, center);
        float m = 2F / 3F;
        float c0 = (float) data.getRadiation(BlockPos.containing(center));
        float c1 = (float) data.getRadiation(BlockPos.containing(center.add(16, 0, 0)));
        float c2 = (float) data.getRadiation(BlockPos.containing(center.add(32, 0, 0)));
        float c3 = (float) data.getRadiation(BlockPos.containing(center.add(32, 0, 16)));
        check(problems, cases, near(c0, 50 * m) && near(c1, 25 * m) && near(c2, 16 * m) && near(c3, 12 * m),
                "ExplosionNukeSmall PARAMS_SAFE: 50/25/16/12 * 2/3 (integer division), got " + c0 + " / " + c1 + " / " + c2 + " / " + c3);
        forgetSite(level);
    }

    // ------------------------------------------------------------------ helpers

    private static float sumResistance(ServerLevel level, Vec3 source, LivingEntity e, double len) {
        Vec3 vec = new Vec3(e.getX() - source.x, e.getEyeY() - source.y, e.getZ() - source.z).normalize();
        float res = 0;
        for (int i = 1; i < len; i++) {
            res += level.getBlockState(BlockPos.containing(Math.floor(source.x + vec.x * i), Math.floor(source.y + vec.y * i),
                    Math.floor(source.z + vec.z * i))).getBlock().getExplosionResistance();
        }
        return Math.max(res, 1);
    }

    private static void surface(ServerLevel level, int cx, int cz, int top) {
        for (int a = 0; a < 16; a++) {
            for (int b = 0; b < 16; b++) {
                level.setBlock(new BlockPos(cx * 16 + a, top, cz * 16 + b), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx * 16 + a, top + 1, cz * 16 + b), Blocks.SHORT_GRASS.defaultBlockState(), 2);
            }
        }
    }

    private static int countSurface(ServerLevel level, int cx, int cz, int top, String id) {
        Block block = BuiltInRegistries.BLOCK.get(com.atomiclegacy.AtomicLegacy.id(id));
        int n = 0;
        for (int a = 0; a < 16; a++) {
            for (int b = 0; b < 16; b++) {
                if (level.getBlockState(new BlockPos(cx * 16 + a, top, cz * 16 + b)).is(block)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static void forgetSite(ServerLevel level) {
        for (int x = CX - 3; x <= CX + 6; x++) {
            for (int z = CZ - 3; z <= CZ + 3; z++) {
                HbmRadiationWorlds.parityK2Forget(level, x, z);
            }
        }
    }

    private static float raw(ServerLevel level, int cx, int cz) {
        return HbmRadiationWorlds.parityK2Raw(level, cx, cz);
    }

    private static float sum(ServerLevel level, int cx, int cz, int r) {
        float s = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                s += Math.max(0F, raw(level, cx + x, cz + z));
            }
        }
        return s;
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) {
            problems.add("parity K2: " + message);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= Math.max(EPS, Math.abs(b) * 1.0E-5);
    }

    private static float rad(LivingEntity e) {
        return HbmLivingRadiation.get(e).getRadiation();
    }

    private static float env(LivingEntity e) {
        return HbmLivingRadiation.get(e).getEnvironmentRadiation();
    }

    private static FakePlayer player(ServerLevel level) {
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("6b2f0a1e-4c1d-4c55-9a40-000000000042"), "parity_k2"));
        p.setPos(BX + 8.5, Y, BZ + 8.5);
        return p;
    }

    private static void reset(LivingEntity e) {
        if (e instanceof net.minecraft.server.level.ServerPlayer p) {
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.tickCount = 400;
        }
        e.removeAllEffects();
        HbmLivingRadiation d = HbmLivingRadiation.get(e);
        d.setRadiation(0F);
        d.setDigamma(0F);
        d.setEnvironmentRadiation(0F);
        d.getContamination().clear();
        HbmLivingRadiation.set(e, d);
    }

    private static Pig pig(ServerLevel level) {
        Pig pig = EntityType.PIG.create(level);
        pig.setPos(BX + 8.5, Y, BZ + 8.5);
        pig.setNoGravity(true);
        pig.setNoAi(true);
        level.addFreshEntity(pig);
        reset(pig);
        return pig;
    }

    private static void cleanup(ServerLevel level) {
        AABB area = new AABB(BX - 48, Y - 48, BZ - 48, BX + 112, Y + 48, BZ + 64);
        for (Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) {
            entity.discard();
        }
        forgetSite(level);
    }
}

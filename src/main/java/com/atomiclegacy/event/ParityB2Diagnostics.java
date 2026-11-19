package com.atomiclegacy.event;

import com.atomiclegacy.blocks.IcfCoreBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.config.HbmParityV3Config;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.items.BlueprintItem;
import com.atomiclegacy.network.MarkerPayload;
import com.atomiclegacy.network.SerializableRecipePayload;
import com.atomiclegacy.radiation.HbmHazardRegistry;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.rtty.RttyProxyLayouts;
import com.atomiclegacy.tileentity.PwrControllerBlockEntity;
import com.atomiclegacy.tileentity.RbmkDials;
import com.atomiclegacy.worldgen.CraterBiomes;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** [parity b2: diagnostics] Self-check of the b2 batch (work/parity-b2.md). */
final class ParityB2Diagnostics {
    private ParityB2Diagnostics() {
    }

    private static int cases;

    private static void check(boolean ok, List<String> problems, String what) {
        cases++;
        if (!ok) problems.add("b2 " + what);
    }

    static int run(MinecraftServer server, List<String> problems) {
        cases = 0;
        try { recipeSync(server, problems); } catch (Throwable t) { problems.add("b2 recipe sync threw " + t); }
        try { craterBiomes(server, problems); } catch (Throwable t) { problems.add("b2 crater biomes threw " + t); }
        try { dials(server, problems); } catch (Throwable t) { problems.add("b2 dials threw " + t); }
        try { markers(server, problems); } catch (Throwable t) { problems.add("b2 markers threw " + t); }
        try { misc(problems); } catch (Throwable t) { problems.add("b2 misc threw " + t); }
        return cases;
    }

    // 1. SerializableRecipePacket
    private static void recipeSync(MinecraftServer server, List<String> problems) {
        check(!HbmParityV3Config.enableServerRecipeSync(), problems, "0.05_enableServerRecipeSync must default to false");
        byte[] file = "{\"recipes\":[]}".getBytes(StandardCharsets.UTF_8);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        SerializableRecipePayload.STREAM_CODEC.encode(buf, new SerializableRecipePayload(false, CustomMachineRecipes.FILE_NAME, file));
        SerializableRecipePayload decoded = SerializableRecipePayload.STREAM_CODEC.decode(buf);
        check(!decoded.reinit() && decoded.filename().equals(CustomMachineRecipes.FILE_NAME) && Arrays.equals(decoded.fileBytes(), file),
                problems, "recipe file packet round trip");
        check(buf.readableBytes() == 0, problems, "recipe file packet leaves no bytes");
        SerializableRecipePayload.STREAM_CODEC.encode(buf, SerializableRecipePayload.reinitialize());
        check(buf.readableBytes() == 1 && SerializableRecipePayload.STREAM_CODEC.decode(buf).reinit(), problems, "reinit packet is a single boolean");

        boolean hadSynced = !CustomMachineRecipes.recipeSyncHandlers.isEmpty();
        if (!hadSynced) {
            Map<String, Integer> before = new HashMap<>();
            CustomMachineRecipes.recipes.forEach((key, list) -> before.put(key, list.size()));
            CustomMachineRecipes.receiveRecipes(CustomMachineRecipes.FILE_NAME, file);
            CustomMachineRecipes.initialize();
            check(CustomMachineRecipes.recipes.isEmpty() && CustomMachineRecipes.modified, problems, "synced recipe file replaces the local recipes");
            CustomMachineRecipes.clearReceivedRecipes();
            Map<String, Integer> after = new HashMap<>();
            CustomMachineRecipes.recipes.forEach((key, list) -> after.put(key, list.size()));
            check(CustomMachineRecipes.recipeSyncHandlers.isEmpty() && before.equals(after), problems, "clearReceivedRecipes restores the local recipes");
        }
    }

    // 2. BiomeGenCraterBase / EntityFalloutRain.getBiomeChange
    private static void craterBiomes(MinecraftServer server, List<String> problems) {
        Registry<Biome> registry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Map<ResourceKey<Biome>, Integer> sky = Map.of(CraterBiomes.CRATER, 0x525A52, CraterBiomes.CRATER_INNER, 0x424A42, CraterBiomes.CRATER_OUTER, 0x6B9189);
        Map<ResourceKey<Biome>, Integer> grass = Map.of(CraterBiomes.CRATER, 0x505050, CraterBiomes.CRATER_INNER, 0x303030, CraterBiomes.CRATER_OUTER, 0x6F6752);
        for (ResourceKey<Biome> key : sky.keySet()) {
            Optional<Holder.Reference<Biome>> holder = registry.getHolder(key);
            check(holder.isPresent(), problems, "biome " + key.location() + " is registered");
            if (holder.isEmpty()) continue;
            Biome biome = holder.get().value();
            check(!biome.hasPrecipitation(), problems, key.location() + " setDisableRain");
            check(biome.getSkyColor() == sky.get(key) && biome.getFogColor() == sky.get(key), problems, key.location() + " sky/fog colour");
            check(biome.getSpecialEffects().getGrassColorOverride().orElse(-1).equals(grass.get(key))
                    && biome.getSpecialEffects().getFoliageColorOverride().orElse(-1) == 0x6A7039, problems, key.location() + " grass/foliage colour");
            // spawnableCreatureList / WaterCreatureList / CaveCreatureList are cleared; the monster list only gets the
            // all-biome HBM creepers (EntityMappings.addSpawn over BiomeGenBase.getBiomeGenArray()), as in the original
            check(biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).isEmpty()
                    && biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.WATER_CREATURE).isEmpty()
                    && biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.AMBIENT).isEmpty(), problems, key.location() + " has no creature spawns");
            check(CraterBiomes.isCrater(holder.get()), problems, key.location() + " isCrater");
        }
        Optional<Holder.Reference<Biome>> inner = registry.getHolder(CraterBiomes.CRATER_INNER);
        Optional<Holder.Reference<Biome>> crater = registry.getHolder(CraterBiomes.CRATER);
        Optional<Holder.Reference<Biome>> outer = registry.getHolder(CraterBiomes.CRATER_OUTER);
        Optional<Holder.Reference<Biome>> plains = registry.getHolder(net.minecraft.world.level.biome.Biomes.PLAINS);
        if (inner.isEmpty() || crater.isEmpty() || outer.isEmpty() || plains.isEmpty()) return;
        check(HbmParityV3Config.enableCraterBiomes(), problems, "17.B_toggle defaults to true");
        check(CraterBiomes.getBiomeChange(14.9, 150, plains.get()) == CraterBiomes.CRATER_INNER, problems, "scale 150, 14.9 % -> inner");
        check(CraterBiomes.getBiomeChange(14.9, 149, plains.get()) == CraterBiomes.CRATER, problems, "scale 149, 14.9 % -> crater");
        check(CraterBiomes.getBiomeChange(54.9, 100, plains.get()) == CraterBiomes.CRATER, problems, "scale 100, 54.9 % -> crater");
        check(CraterBiomes.getBiomeChange(54.9, 100, inner.get()) == null, problems, "inner is never downgraded to crater");
        check(CraterBiomes.getBiomeChange(55, 100, plains.get()) == CraterBiomes.CRATER_OUTER, problems, "scale 100, 55 % -> outer");
        check(CraterBiomes.getBiomeChange(99, 25, plains.get()) == CraterBiomes.CRATER_OUTER, problems, "scale 25 -> outer");
        check(CraterBiomes.getBiomeChange(99, 25, crater.get()) == null && CraterBiomes.getBiomeChange(99, 25, inner.get()) == null, problems, "outer never replaces crater/inner");
        check(CraterBiomes.getBiomeChange(1, 24, plains.get()) == null, problems, "scale 24 changes nothing");
        check(ParityB2Events.craterRadiation(crater.get()) == 5F && ParityB2Events.craterRadiation(inner.get()) == 25F
                && ParityB2Events.craterRadiation(outer.get()) == 0.5F && ParityB2Events.craterRadiation(plains.get()) == 0F
                && HbmParityV3Config.craterBiomeWaterMult() == 5F, problems, "crater RAD/s 5 / 25 / 0.5, water x5");

        // the chunk rewrite itself, undone afterwards
        ServerLevel level = server.overworld();
        LevelChunk chunk = level.getChunk(0, 0);
        int minQy = QuartPos.fromBlock(level.getMinBuildHeight());
        int maxQy = QuartPos.fromBlock(level.getMaxBuildHeight()) - 1;
        @SuppressWarnings("unchecked")
        Holder<Biome>[][][] saved = new Holder[4][maxQy - minQy + 1][4];
        for (int qx = 0; qx < 4; qx++) for (int qy = minQy; qy <= maxQy; qy++) for (int qz = 0; qz < 4; qz++) {
            saved[qx][qy - minQy][qz] = chunk.getNoiseBiome(qx, qy, qz);
        }
        boolean changed = CraterBiomes.apply(level, chunk, 8.5D, 8.5D, 150, false);
        boolean allInner = true;
        for (int qx = 0; qx < 4; qx++) for (int qy = minQy; qy <= maxQy; qy++) for (int qz = 0; qz < 4; qz++) {
            allInner &= chunk.getNoiseBiome(qx, qy, qz).is(CraterBiomes.CRATER_INNER);
        }
        check(changed && allInner, problems, "fallout of scale 150 turns the centre chunk into the inner crater at every height");
        check(!CraterBiomes.apply(level, chunk, 8.5D, 8.5D, 150, false), problems, "an unchanged chunk is not re-sent");
        check(!CraterBiomes.apply(level, chunk, 8.5D, 8.5D, 100, false), problems, "a weaker blast keeps the inner crater");
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> saved[qx & 3][Mth.clamp(qy, minQy, maxQy) - minQy][qz & 3], level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        check(chunk.getNoiseBiome(0, QuartPos.fromBlock(level.getSeaLevel()), 0) == saved[0][QuartPos.fromBlock(level.getSeaLevel()) - minQy][0], problems, "test chunk restored");
    }

    // 3. RBMKDials
    private static void dials(MinecraftServer server, List<String> problems) {
        Map<String, String> expected = new HashMap<>();
        expected.put("dialSaveDials", "true");
        expected.put("dialPassiveCooling", "2.5");
        expected.put("dialPassiveCoolingInner", "0.1");
        expected.put("dialColumnHeatFlow", "0.2");
        expected.put("dialDiffusionMod", "1.0");
        expected.put("dialHeatProvision", "0.2");
        expected.put("dialColumnHeight", "4");
        expected.put("dialEnablePermaScrap", "true");
        expected.put("dialBoilerHeatConsumption", "0.1");
        expected.put("dialControlSpeed", "1.0");
        expected.put("dialReactivityMod", "1.0");
        expected.put("dialOutgasserSpeedMod", "1.0");
        expected.put("dialControlSurgeMod", "1.0");
        expected.put("dialFluxRange", "5");
        expected.put("dialReasimRange", "10");
        expected.put("dialReasimBoilers", "false");
        expected.put("dialReasimBoilerSpeed", "0.05");
        expected.put("dialDisableMeltdowns", "false");
        expected.put("dialEnableMeltdownOverpressure", "false");
        expected.put("dialModeratorEfficiency", "1.0");
        expected.put("dialAbsorberEfficiency", "1.0");
        expected.put("dialReflectorEfficiency", "1.0");
        expected.put("dialDisableDepletion", "false");
        expected.put("dialDisableXenon", "false");
        expected.put("dialAbsorberHeatConversion", "0.05");
        check(RbmkDials.keys().size() == 25, problems, "25 RBMKKeys, found " + RbmkDials.keys().size());
        GameRules fresh = new GameRules();
        Set<String> seen = new HashSet<>();
        for (GameRules.Key<?> key : RbmkDials.keys()) {
            seen.add(key.getId());
            check(fresh.getRule(key).serialize().equals(expected.get(key.getId())), problems,
                    "gamerule " + key.getId() + " default " + fresh.getRule(key).serialize() + " != " + expected.get(key.getId()));
            check(server.getGameRules().getRule(key) != null, problems, "gamerule " + key.getId() + " exists on the server");
        }
        check(seen.equals(expected.keySet()), problems, "gamerule names " + seen);

        // clamps of RBMKDials.refresh, then back to the server's values
        fresh.getRule(RbmkDials.KEY_COLUMN_HEIGHT).set(99, null);
        fresh.getRule(RbmkDials.KEY_FLUX_RANGE).set(0, null);
        fresh.getRule(RbmkDials.KEY_PASSIVE_COOLING).set(-3D);
        fresh.getRule(RbmkDials.KEY_COLUMN_HEAT_FLOW).set(7D);
        fresh.getRule(RbmkDials.KEY_DISABLE_XENON).set(true, null);
        RbmkDials.refresh(fresh);
        check(RbmkDials.getColumnHeight() == 15 && RbmkDials.getFluxRange() == 1 && RbmkDials.getPassiveCooling() == 0D
                && RbmkDials.getColumnHeatFlow() == 1D && !RbmkDials.getXenon() && RbmkDials.getDepletion(), problems, "dial clamps");
        // createDials: without dialSaveDials everything returns to the defaults (and dialSaveDials itself to true)
        fresh.getRule(RbmkDials.KEY_SAVE_DIALS).set(false, null);
        RbmkDials.createDials(fresh);
        check(RbmkDials.getColumnHeight() == 3 && RbmkDials.getFluxRange() == 5 && RbmkDials.getPassiveCooling() == 2.5D
                && RbmkDials.getXenon() && fresh.getBoolean(RbmkDials.KEY_SAVE_DIALS), problems, "createDials resets unsaved dials");
        fresh.getRule(RbmkDials.KEY_COLUMN_HEIGHT).set(7, null);
        RbmkDials.createDials(fresh);
        check(RbmkDials.getColumnHeight() == 6, problems, "createDials keeps saved dials");
        RbmkDials.DoubleValue value = fresh.getRule(RbmkDials.KEY_REASIM_BOILER_SPEED);
        GameRules reread = new GameRules();
        reread.getRule(RbmkDials.KEY_REASIM_BOILER_SPEED).setFrom(value, null);
        check(reread.getRule(RbmkDials.KEY_REASIM_BOILER_SPEED).get() == 0.05D && value.getCommandResult() == 0, problems, "decimal game rule copy");
        RbmkDials.refresh(server.getGameRules());
        check(RbmkDials.getColumnHeight() == Mth.clamp(server.getGameRules().getInt(RbmkDials.KEY_COLUMN_HEIGHT), 2, 16) - 1, problems, "dial cache follows the server rules");
    }

    // 4. sendError markers
    private static void markers(MinecraftServer server, List<String> problems) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        MarkerPayload marker = new MarkerPayload(new BlockPos(1, -2, 3), 0xff0000, 5_000, 128D, PwrControllerBlockEntity.ERROR_NON_REACTOR);
        MarkerPayload.STREAM_CODEC.encode(buf, marker);
        check(MarkerPayload.STREAM_CODEC.decode(buf).equals(marker), problems, "marker packet round trip");
        check(PwrControllerBlockEntity.ERROR_MAX_SIZE.equals("Max size exceeded") && PwrControllerBlockEntity.ERROR_NON_REACTOR.equals("Non-reactor block")
                && PwrControllerBlockEntity.ERROR_FUEL_RODS.equals("Fuel rods required") && PwrControllerBlockEntity.ERROR_NEUTRON_SOURCES.equals("Neutron sources required"),
                problems, "PWR sendError labels");
    }

    // 5-9
    private static void misc(List<String> problems) {
        // ROR proxy of the oil burner: the extra cell sits above the core for every facing
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos core = new BlockPos(10, 64, 10);
            BlockPos[] cells = RttyProxyLayouts.proxyCells("heater_oilburner", core, dir);
            check(cells != null && cells.length == 5 && Arrays.asList(cells).contains(core.above())
                    && Arrays.asList(cells).contains(core.relative(dir)) && Arrays.asList(cells).contains(core.relative(dir.getClockWise())),
                    problems, "heater_oilburner ROR cells facing " + dir);
        }

        // setPools528 replaces the regular pools
        boolean enable528 = HbmConfig.ENABLE_528_MODE.get();
        List<String> mixed = List.of("alt.test", "528.test");
        check(BlueprintItem.activeRecipePools(mixed).equals(enable528 ? List.of("528.test") : List.of("alt.test")), problems, "528 pool replaces the regular pool: " + BlueprintItem.activeRecipePools(mixed));
        check(BlueprintItem.activeRecipePools(List.of("528.test")).equals(enable528 ? List.of("528.test") : List.of()), problems, "528-only pool");
        check(BlueprintItem.activeRecipePools(List.of("secret.psalm")).equals(List.of("secret.psalm")), problems, "secret pool always needs its blueprint");
        // HazardRegistry values that depend on enable528 follow the loaded config
        float pu239 = HbmHazardRegistry.purg * HbmHazardRegistry.billet + HbmHazardRegistry.pu239 * HbmHazardRegistry.billet
                + (enable528 ? HbmHazardRegistry.wst : HbmHazardRegistry.u) * HbmHazardRegistry.billet;
        List<HbmHazardRegistry.Entry> rod = HbmHazardRegistry.entriesForKey("pile_rod_pu239");
        check(rod.size() == 1 && rod.get(0).baseLevel() == pu239, problems, "pile_rod_pu239 radiation follows enable528");

        // machine_controller: a NaN target passes the clamp like MathHelper.clamp_double
        check(Double.isNaN(Mth.clamp(Double.NaN * 0.01D, 0.0D, 1.0D)), problems, "NaN target survives the clamp");

        // port-only fluids stay hidden
        int portOnly = 0;
        for (HbmFluidDefinition fluid : HbmFluids.definitions()) {
            if (fluid.oldId() > 156) {
                portOnly++;
                check(fluid.isPortOnly() && !fluid.allowsFluidIdentifier(), problems, "port-only fluid " + fluid.name() + " is hidden");
            }
        }
        check(portOnly > 0, problems, "port-only fluids are still registered");
        for (HbmFluidDefinition fluid : HbmFluids.niceOrder()) {
            if (fluid.isPortOnly()) check(false, problems, "port-only fluid " + fluid.name() + " listed in niceOrder");
        }

        // ICF footprint contains both wings (old cores are completed on load)
        if (HbmBlocks.ICF.get() instanceof IcfCoreBlock icf) {
            BlockPos core = new BlockPos(0, 64, 0);
            List<BlockPos> cells = icf.machineOccupiedPositions(core, Direction.SOUTH);
            check(cells.contains(core.offset(8, 3, 2)) && cells.contains(core.offset(-8, 4, -2)) && cells.contains(core.offset(0, 2, 2))
                    && !cells.contains(core.offset(0, 1, 2)), problems, "ICF footprint wings");
        } else {
            check(false, problems, "icf block is an IcfCoreBlock");
        }
    }
}

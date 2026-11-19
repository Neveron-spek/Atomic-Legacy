package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyNukeDefinition;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.config.HbmParityV3Config;
import com.atomiclegacy.entity.LegacyLauncherMissileEntity;
import com.atomiclegacy.entity.LegacyMissileImpacts;
import com.atomiclegacy.inventory.recipes.HotSmeltingRecipe;
import com.atomiclegacy.inventory.recipes.anvil.AnvilSmithingHotRecipe;
import com.atomiclegacy.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.atomiclegacy.inventory.recipes.anvil.HbmAnvilRecipes;
import com.atomiclegacy.items.HbmToolBehavior;
import com.atomiclegacy.items.HbmToolItems;
import com.atomiclegacy.items.HbmToolProfile;
import com.atomiclegacy.items.LegacyHotItem;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmHazardRegistry;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.radiation.HbmReactiveHazards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * [parity V3: diagnostics] Wired BombConfig/RadiationConfig/ToolConfig/MobConfig/ClientConfig keys (defaults and the
 * effect of a changed value) and the hot item heat mechanic. Platform at x = 37000.
 */
final class ParityV3Diagnostics {
    private static final BlockPos BASE = new BlockPos(37000, 150, 0);

    private ParityV3Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        level.getChunk(BASE);
        Runnable[] parts = {
                () -> defaults(problems, cases),
                () -> bombs(level, problems, cases),
                () -> radiation(level, problems, cases),
                () -> hazards(level, problems, cases),
                () -> tools(problems, cases),
                () -> hotItems(server, level, problems, cases),
        };
        String[] names = {"defaults", "bombs", "radiation", "hazards", "tools", "hot items"};
        for (int i = 0; i < parts.length; i++) {
            try {
                parts[i].run();
            } catch (Throwable t) {
                problems.add("parity V3 " + names[i] + " threw " + t);
            }
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("parity V3: " + what);
    }

    /** Sets a config value for the duration of the check and restores it. */
    private static <T> void with(ModConfigSpec.ConfigValue<T> value, T temp, Runnable body) {
        T old = value.get();
        value.set(temp);
        try {
            body.run();
        } finally {
            value.set(old);
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static void defaults(List<String> problems, int[] cases) {
        check(HbmParityV3Config.gadgetRadius() == 150 && HbmParityV3Config.boyRadius() == 120 && HbmParityV3Config.manRadius() == 175
                && HbmParityV3Config.mikeRadius() == 250 && HbmParityV3Config.tsarRadius() == 500 && HbmParityV3Config.prototypeRadius() == 150
                && HbmParityV3Config.fleijaRadius() == 50 && HbmParityV3Config.missileRadius() == 100 && HbmParityV3Config.soliniumRadius() == 150
                && HbmParityV3Config.n2Radius() == 200, problems, cases, "BombConfig radius defaults active");
        check(HbmParityV3Config.blastSpeed() == 1024 && HbmParityV3Config.mk5() == 50 && HbmParityV3Config.falloutRange() == 100
                && HbmParityV3Config.falloutDelay() == 4, problems, cases, "BombConfig explosion defaults active");
        check(HbmParityV3Config.recursionDepth() == 1000 && !HbmParityV3Config.recursiveStone() && !HbmParityV3Config.recursiveNetherrack()
                && HbmParityV3Config.abilityHammer() && HbmParityV3Config.abilityVein() && HbmParityV3Config.abilityLuck()
                && HbmParityV3Config.abilitySilk() && HbmParityV3Config.abilityFurnace() && HbmParityV3Config.abilityShredder()
                && HbmParityV3Config.abilityCentrifuge() && HbmParityV3Config.abilityCrystallizer() && HbmParityV3Config.abilityMercury()
                && HbmParityV3Config.abilityExplosion(), problems, cases, "ToolConfig defaults active");
        check(HbmParityV3Config.fogRad() == 100 && HbmParityV3Config.fogCh() == 20 && HbmParityV3Config.hellRad() == 0.1D
                && HbmParityV3Config.worldRadEffects() && !HbmParityV3Config.cleanupDeadDirt() && HbmParityV3Config.enableContamination()
                && HbmParityV3Config.enableChunkRads() && !HbmParityV3Config.disableHot() && !HbmParityV3Config.disableExplosive()
                && !HbmParityV3Config.disableHydro() && !HbmParityV3Config.disableBlinding(), problems, cases, "RadiationConfig defaults active");
        with(HbmParityV3Config.FOG_CH, 0, () -> check(HbmParityV3Config.fogCh() == 20, problems, cases, "FOG_01_threshold 0 -> setDef 20"));
        with(HbmParityV3Config.FOG_CH, 7, () -> check(HbmParityV3Config.fogCh() == 7, problems, cases, "FOG_01_threshold 7 read"));
        // client spec is not loaded on the dedicated server: the accessors must return the ClientConfig defaults
        check(HbmClientConfig.geigerOffsetHorizontal() == 0 && HbmClientConfig.geigerOffsetVertical() == 0
                && HbmClientConfig.infoOffsetHorizontal() == 0 && HbmClientConfig.infoOffsetVertical() == 0 && HbmClientConfig.infoPosition() == 0
                && HbmClientConfig.doddRbmkDiagnostic() && HbmClientConfig.renderCableHang() && HbmClientConfig.renderReeds()
                && HbmClientConfig.renderHeliostatBeamLimit() == 250, problems, cases, "ClientConfig accessor defaults");
        check(HbmClientConfig.RENDER_HELIOSTAT_BEAM_LIMIT.getDefault() == 250 && HbmClientConfig.DODD_RBMK_DIAGNOSTIC.getDefault(),
                problems, cases, "ClientConfig spec defaults");
    }

    private static void bombs(ServerLevel level, List<String> problems, int[] cases) {
        check(LegacyNukeDefinition.GADGET.radius() == 150 && LegacyNukeDefinition.MAN.radius() == 175 && LegacyNukeDefinition.MIKE.radius() == 250
                && LegacyNukeDefinition.TSAR.radius() == 500 && LegacyNukeDefinition.FLEIJA.radius() == 50
                && LegacyNukeDefinition.PROTOTYPE.radius() == 150 && LegacyNukeDefinition.SOLINIUM.radius() == 150
                && LegacyNukeDefinition.N2.radius() == 200, problems, cases, "nuke block radii = BombConfig defaults");
        Object[][] keys = {
                {HbmParityV3Config.GADGET_RADIUS, LegacyNukeDefinition.GADGET}, {HbmParityV3Config.MAN_RADIUS, LegacyNukeDefinition.MAN},
                {HbmParityV3Config.MIKE_RADIUS, LegacyNukeDefinition.MIKE}, {HbmParityV3Config.TSAR_RADIUS, LegacyNukeDefinition.TSAR},
                {HbmParityV3Config.FLEIJA_RADIUS, LegacyNukeDefinition.FLEIJA}, {HbmParityV3Config.PROTOTYPE_RADIUS, LegacyNukeDefinition.PROTOTYPE},
                {HbmParityV3Config.SOLINIUM_RADIUS, LegacyNukeDefinition.SOLINIUM}, {HbmParityV3Config.N2_RADIUS, LegacyNukeDefinition.N2}};
        for (Object[] k : keys) {
            @SuppressWarnings("unchecked")
            ModConfigSpec.ConfigValue<Integer> value = (ModConfigSpec.ConfigValue<Integer>) k[0];
            LegacyNukeDefinition def = (LegacyNukeDefinition) k[1];
            with(value, 37, () -> check(def.radius() == 37, problems, cases, def + " radius follows its BombConfig key"));
        }
        ItemStack nuclear = new ItemStack(item("missile_nuclear"));
        ItemStack doomsday = new ItemStack(item("missile_doomsday"));
        check(String.join(",", LegacyMissileImpacts.dryRunImpact(missile(level, nuclear), null)).equals("mk5(100)+torex(100)")
                && String.join(",", LegacyMissileImpacts.dryRunImpact(missile(level, doomsday), null)).equals("mk5(200,moreFallout=100)+torex(200)"),
                problems, cases, "missile_nuclear/doomsday use missileRadius 100 / 200");
        with(HbmParityV3Config.MISSILE_RADIUS, 60, () -> check(
                String.join(",", LegacyMissileImpacts.dryRunImpact(missile(level, nuclear), null)).equals("mk5(60)+torex(60)")
                        && String.join(",", LegacyMissileImpacts.dryRunImpact(missile(level, doomsday), null)).equals("mk5(120,moreFallout=100)+torex(120)"),
                problems, cases, "3.07_missileRadius 60 changes the missile warheads"));
        with(HbmParityV3Config.BLAST_SPEED, 7, () -> check(HbmParityV3Config.blastSpeed() == 7, problems, cases, "6.01_blastSpeed read"));
        with(HbmParityV3Config.MK5_BLAST_TIME, 9, () -> check(HbmParityV3Config.mk5() == 9, problems, cases, "6.02_mk5BlastTime read"));
    }

    private static LegacyLauncherMissileEntity missile(ServerLevel level, ItemStack stack) {
        return LegacyLauncherMissileEntity.create(level, stack, BASE.getX() + 0.5, BASE.getY(), BASE.getZ() + 0.5,
                BASE.getX() + 1000, BASE.getZ(), Direction.EAST);
    }

    private static void radiation(ServerLevel level, List<String> problems, int[] cases) {
        ChunkRadiationData data = ChunkRadiationData.get(level);
        data.clearRadiation(BASE);
        data.incrementRadiation(BASE, 50.0D);
        check(Math.abs(data.getRadiation(BASE) - 50.0D) < 1e-4, problems, cases, "chunk radiation stored with chunk rads on");
        with(HbmParityV3Config.ENABLE_CHUNK_RADS, false, () -> {
            data.incrementRadiation(BASE, 25.0D);
            check(data.getRadiation(BASE) == 0.0D, problems, cases, "RADIATION_01_enableChunkRads off: chunk radiation reads 0");
        });
        check(Math.abs(data.getRadiation(BASE) - 50.0D) < 1e-4, problems, cases, "RADIATION_01_enableChunkRads off dropped the write");
        data.clearRadiation(BASE);

        Zombie zombie = EntityType.ZOMBIE.create(level);
        HbmLivingRadiation rad = HbmLivingRadiation.get(zombie);
        rad.setRadiation(10.0F);
        check(rad.getRadiation() == 10.0F, problems, cases, "contamination on: entity radiation set");
        with(HbmParityV3Config.ENABLE_CONTAMINATION, false, () -> {
            rad.setRadiation(40.0F);
            rad.addRadiation(5.0F);
            check(rad.getRadiation() == 0.0F, problems, cases, "RADIATION_00_enableContamination off: getRadiation 0");
        });
        check(rad.getRadiation() == 10.0F, problems, cases, "RADIATION_00_enableContamination off: set/increment ignored");
        with(HbmParityV3Config.HELL_RAD, 0.5D, () -> check(HbmParityV3Config.hellRad() == 0.5D, problems, cases, "AMBIENT_00_nether read"));

        BlockPos earth = BASE.offset(4, 0, 0);
        Block waste = BuiltInRegistries.BLOCK.get(AtomicLegacy.id("waste_earth"));
        level.setBlock(earth.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(earth, waste.defaultBlockState(), 3);
        with(HbmParityV3Config.CLEANUP_DEAD_DIRT, true, () -> level.getBlockState(earth).randomTick(level, earth, level.random));
        check(level.getBlockState(earth).is(Blocks.DIRT), problems, cases, "RADWORLD_03_regrow: waste earth decays to dirt");
        level.setBlock(earth, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void hazards(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = BASE.offset(10, 0, 0);
        ItemEntity tnt = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.TNT));
        tnt.setRemainingFireTicks(100);
        with(HbmParityV3Config.DISABLE_EXPLOSIVE, true, () -> HbmReactiveHazards.updateDroppedItem(tnt));
        check(!tnt.isRemoved(), problems, cases, "HAZ_03_disableExplosive: burning dropped TNT does not explode");
        HbmReactiveHazards.updateDroppedItem(tnt);
        check(tnt.isRemoved(), problems, cases, "explosive hazard default: burning dropped TNT explodes");

        Item hydro = null;
        for (Item candidate : BuiltInRegistries.ITEM) {
            if (HbmHazardRegistry.entries(new ItemStack(candidate)).stream().anyMatch(e -> e.type() == HbmHazardRegistry.Type.HYDROACTIVE)) {
                hydro = candidate;
                break;
            }
        }
        check(hydro != null, problems, cases, "a hydroactive item exists");
        if (hydro == null) return;
        BlockPos wet = BASE.offset(20, 0, 0);
        level.setBlock(wet, Blocks.WATER.defaultBlockState(), 3);
        ItemEntity drop = new ItemEntity(level, wet.getX() + 0.5, wet.getY() + 0.2, wet.getZ() + 0.5, new ItemStack(hydro));
        with(HbmParityV3Config.DISABLE_HYDRO, true, () -> HbmReactiveHazards.updateDroppedItem(drop));
        check(!drop.isRemoved(), problems, cases, "HAZ_04_disableHydroactive: wet dropped item does not explode");
        HbmReactiveHazards.updateDroppedItem(drop);
        check(drop.isRemoved(), problems, cases, "hydroactive hazard default: wet dropped item explodes");
        level.setBlock(wet, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void tools(List<String> problems, int[] cases) {
        HbmToolProfile profile = ((HbmToolItems.AbilityItem) item("steel_pickaxe")).profile();
        check(profile.areaAbilities().stream().anyMatch(e -> e.ability() == HbmToolBehavior.AreaAbility.RECURSION)
                && profile.maxLevel(HbmToolBehavior.AreaAbility.RECURSION) == 0, problems, cases, "steel_pickaxe has veinminer by default");
        with(HbmParityV3Config.ABILITY_VEIN, false, () -> check(
                profile.areaAbilities().stream().noneMatch(e -> e.ability() == HbmToolBehavior.AreaAbility.RECURSION)
                        && profile.maxLevel(HbmToolBehavior.AreaAbility.RECURSION) == -1,
                problems, cases, "11.04_abilityVein off removes veinminer"));
        HbmToolProfile schrab = ((HbmToolItems.AbilityItem) item("schrabidium_pickaxe")).profile();
        boolean hasSilk = schrab.harvestAbilities().stream().anyMatch(e -> e.ability() == HbmToolBehavior.HarvestAbility.SILK);
        with(HbmParityV3Config.ABILITY_SILK, false, () -> check(
                schrab.harvestAbilities().stream().noneMatch(e -> e.ability() == HbmToolBehavior.HarvestAbility.SILK),
                problems, cases, "11.06_abilitySilk off removes silk touch"));
        check(hasSilk, problems, cases, "schrabidium_pickaxe has silk touch by default");
        with(HbmParityV3Config.RECURSION_DEPTH, 5, () -> check(HbmParityV3Config.recursionDepth() == 5, problems, cases, "11.00_recursionDepth read"));
    }

    private static void hotItems(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        String[] ids = {"ingot_chainsteel", "ingot_meteorite", "ingot_meteorite_forged", "blade_meteorite", "ingot_steel_dusted"};
        Zombie holder = EntityType.ZOMBIE.create(level);
        for (String id : ids) {
            ItemStack stack = LegacyHotItem.heatUp(new ItemStack(item(id)));
            LegacyHotItem hot = (LegacyHotItem) stack.getItem();
            check(hot.heat(stack) == 100 && hot.heatFraction(stack) == 1.0D && hot.maxHeat(stack) == 100,
                    problems, cases, id + " heats to the shared ItemHot.heat 100");
            for (int i = 0; i < 100; i++) hot.inventoryTick(stack, level, holder, 0, false);
            check(hot.heat(stack) == 0 && stack.has(DataComponents.CUSTOM_DATA) && !stack.has(DataComponents.CUSTOM_MODEL_DATA),
                    problems, cases, id + " cools to 0 in 100 ticks");
            hot.inventoryTick(stack, level, holder, 0, false);
            check(!stack.has(DataComponents.CUSTOM_DATA), problems, cases, id + " drops its NBT one tick after reaching 0");
            var recipe = server.getRecipeManager().byKey(AtomicLegacy.id("smelting_hot_" + id)).map(h -> h.value()).orElse(null);
            ItemStack input = LegacyHotItem.withGrade(new ItemStack(item(id)), id.equals("ingot_steel_dusted") ? 5 : 0);
            ItemStack out = recipe instanceof HotSmeltingRecipe hs ? hs.assemble(new SingleRecipeInput(input), server.registryAccess()) : ItemStack.EMPTY;
            check(recipe instanceof HotSmeltingRecipe hs2 && hs2.matches(new SingleRecipeInput(input), level) && out.is(input.getItem())
                    && hot.heat(out) == 100 && LegacyHotItem.grade(out) == LegacyHotItem.grade(input)
                    && hs2.getExperience() == (id.equals("ingot_steel_dusted") ? 1.0F : 0.0F),
                    problems, cases, id + " furnace recipe gives the hot item (heat 100, grade kept)");
        }
        ItemStack half = LegacyHotItem.heatUp(new ItemStack(item("ingot_meteorite")), 0.5D);
        check(((LegacyHotItem) half.getItem()).heat(half) == 50, problems, cases, "heatUp(0.5) = 50");

        // AnvilSmithingHotRecipe: dusted grade 2 + grade 2 -> grade 3, heat >= 0.5 required, output heat = mean
        AnvilSmithingRecipe gradeUp = null;
        for (AnvilSmithingRecipe r : HbmAnvilRecipes.smithing()) {
            ItemStack o = r.displayOutput();
            if (r instanceof AnvilSmithingHotRecipe && o.is(item("ingot_steel_dusted")) && LegacyHotItem.grade(o) == 3) {
                gradeUp = r;
                break;
            }
        }
        check(gradeUp != null, problems, cases, "dusted steel grade 2 -> 3 anvil recipe registered with its grade");
        if (gradeUp != null) {
            ItemStack a = LegacyHotItem.heatUp(LegacyHotItem.withGrade(new ItemStack(item("ingot_steel_dusted")), 2), 0.8D);
            ItemStack b = LegacyHotItem.heatUp(LegacyHotItem.withGrade(new ItemStack(item("ingot_steel_dusted")), 2), 0.6D);
            ItemStack cold = LegacyHotItem.heatUp(LegacyHotItem.withGrade(new ItemStack(item("ingot_steel_dusted")), 2), 0.4D);
            ItemStack wrong = LegacyHotItem.heatUp(LegacyHotItem.withGrade(new ItemStack(item("ingot_steel_dusted")), 1), 0.8D);
            check(gradeUp.match(a, b) != null && gradeUp.match(a, cold) == null && gradeUp.match(a, wrong) == null,
                    problems, cases, "hot anvil: grade matched ignoring heat, below 50% heat rejected");
            ItemStack out = gradeUp.getOutput(a, b);
            check(LegacyHotItem.grade(out) == 3 && ((LegacyHotItem) out.getItem()).heat(out) == 70,
                    problems, cases, "hot anvil output: grade 3, heat (80 + 60) / 2 = 70");
        }
    }
}

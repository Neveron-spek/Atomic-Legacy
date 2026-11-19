package com.atomiclegacy.event;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.ai.LegacyCreatureAi;
import com.atomiclegacy.entity.ai.LegacyMoveControl;
import com.atomiclegacy.entity.ai.LegacyPathFinder;
import com.atomiclegacy.entity.ai.LegacyPathNavigation;
import com.atomiclegacy.items.LegacyGlitchItem;
import com.atomiclegacy.items.LegacyLoreItem;
import com.atomiclegacy.items.LegacyPolaroidItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.LegacyContentVisibility;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * [parity J1 (2026-11-19, second J1 pass): diagnostics] Old-AI movement and the 1.7.10 path finder (PathFinderUtils,
 * EntityCreature path following, EntityMoveHelper), MainRegistry.polaroidID and its dependants, and the shaped
 * 1-to-9 recipes. ParityJ1Diagnostics is the earlier radiation batch with the same letter.
 */
final class ParityJ1bDiagnostics {
    private ParityJ1bDiagnostics() {
    }

    private static final BlockPos ORIGIN = new BlockPos(-96, 300, 96);

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            polaroid(problems, cases);
            recipes(server, level, problems, cases);
            mobSetup(level, problems, cases);
            buildPlatform(level, true);
            try {
                pathFinder(level, problems, cases);
                movement(level, problems, cases);
            } finally {
                buildPlatform(level, false);
            }
        } catch (Exception exception) {
            problems.add("parity j1b: diagnostics threw " + exception);
            AtomicLegacy.LOGGER.error("parity j1b diagnostics", exception);
        }
        return cases[0];
    }

    private static void polaroid(List<String> problems, int[] cases) throws Exception {
        int id = AtomicLegacy.polaroidID;
        check(problems, cases, id >= 1 && id <= 18 && id != 4 && id != 9, "polaroidID in 1..18 without 4 and 9: " + id);
        check(problems, cases, LegacyPolaroidItem.polaroidLines(1).equals(List.of("..."))
                && LegacyPolaroidItem.polaroidLines(11).equals(List.of("yss stares back.6public Party cel"))
                && LegacyPolaroidItem.polaroidLines(18).size() == 4
                && LegacyPolaroidItem.polaroidLines(18).get(3).equals("\"Huh, what does thirty-one mean?\""), "ItemPolaroid lines");
        check(problems, cases, "Click-click-click!".equals(LegacyGlitchItem.glitchLine(1)) && "PTANG!".equals(LegacyGlitchItem.glitchLine(11))
                && "BANG! Choo-chooo! B A N G ! ! !".equals(LegacyGlitchItem.glitchLine(18)), "ItemGlitch lines");
        for (int i = 1; i <= 18; i++) {
            check(problems, cases, !LegacyPolaroidItem.polaroidLines(i).isEmpty() && LegacyGlitchItem.glitchLine(i) != null, "polaroid/glitch line " + i);
            for (String name : new String[] {"polaroid", "glitch"}) {
                check(problems, cases, resource("/assets/atomiclegacy/models/item/" + name + "_" + i + ".json")
                        && resource("/assets/atomiclegacy/textures/item/" + name + "_" + i + ".png"), name + "_" + i + " model/texture");
            }
        }
        check(problems, cases, resource("/assets/atomiclegacy/textures/models/horse/dyx.png")
                && resource("/assets/atomiclegacy/models/block/trinkets/horse_dyx_wings.json"), "reformer polaroid 11 horse assets");
        check(problems, cases, !LegacyContentVisibility.isCreativeHiddenPath("book_secret", 11)
                && LegacyContentVisibility.isCreativeHiddenPath("book_secret", 1), "book_secret creative tab only with polaroid 11");
        for (String path : new String[] {"book_secret", "book_of_", "burnt_bark", "ams_core_thingy"}) {
            check(problems, cases, !LegacyContentVisibility.isJeiHiddenPath(path, 11) && LegacyContentVisibility.isJeiHiddenPath(path, 2),
                    "NEIConfig polaroid hide " + path);
        }
        JsonObject english;
        try (InputStream input = ParityJ1bDiagnostics.class.getResourceAsStream("/assets/atomiclegacy/lang/en_us.json")) {
            english = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        String[][] p11 = {{"crystal_charred", "Also a real horn. Weird, right?"}, {"crystal_horn", "An actual horn."},
                {"dust", "Another one bites the dust!"}, {"gem_tantalium", "AKA Tantalum."},
                {"ingot_lanthanium", "Actually Lanthanum, but whatever."}, {"ingot_neptunium", "Woo, scary!"},
                {"ingot_tantalium", "AKA Tantalum."}, {"key_red", "§4e§r"}, {"powder_tantalium", "AKA Tantalum."}};
        for (String[] row : p11) {
            String key = "item.atomiclegacy." + row[0] + ".desc.P11";
            check(problems, cases, english.has(key) && english.get(key).getAsString().equals(row[1])
                    && BuiltInRegistries.ITEM.get(AtomicLegacy.id(row[0])) instanceof LegacyLoreItem, "ItemCustomLore .desc.P11 " + row[0]);
        }
    }

    private static void recipes(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        int shapelessNine = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.id().getNamespace().equals(AtomicLegacy.MOD_ID) && holder.value() instanceof ShapelessRecipe shapeless
                    && shapeless.getIngredients().size() == 1 && shapeless.getResultItem(level.registryAccess()).getCount() == 9) {
                shapelessNine++;
                problems.add("parity j1b: 1-to-9 recipe still shapeless " + holder.id());
            }
        }
        check(problems, cases, shapelessNine == 0, "no shapeless 1-to-9 recipes");
        var holder = server.getRecipeManager().byKey(AtomicLegacy.id("ingot_aluminium_from_block")).orElse(null);
        check(problems, cases, holder != null && holder.value() instanceof ShapedRecipe shaped && shaped.getWidth() == 1 && shaped.getHeight() == 1,
                "ingot_aluminium_from_block shaped \"#\"");
        ItemStack block = new ItemStack(HbmBlocks.BLOCK_ALUMINIUM.get());
        CraftingInput input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, block));
        ItemStack result = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
        check(problems, cases, result.getCount() == 9 && BuiltInRegistries.ITEM.getKey(result.getItem()).equals(AtomicLegacy.id("ingot_aluminium")),
                "block_aluminium -> 9 ingot_aluminium in any cell");
    }

    private static void mobSetup(ServerLevel level, List<String> problems, int[] cases) {
        for (EntityType<? extends Mob> type : List.of(HbmEntityTypes.GLYPHID.get(), HbmEntityTypes.GLYPHID_SCOUT.get(),
                HbmEntityTypes.GLYPHID_BEHEMOTH.get(), HbmEntityTypes.GLYPHID_NUCLEAR.get(), HbmEntityTypes.RAD_BEAST.get(),
                HbmEntityTypes.PARASITE_MAGGOT.get(), HbmEntityTypes.GHOST.get())) {
            Mob mob = type.create(level);
            boolean movementGoal = false;
            for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
                movementGoal |= goal.getGoal() instanceof MeleeAttackGoal || goal.getGoal() instanceof RandomStrollGoal
                        || goal.getGoal() instanceof FloatGoal || goal.getGoal() instanceof RandomLookAroundGoal;
            }
            check(problems, cases, mob.getNavigation() instanceof LegacyPathNavigation nav && nav.isManualFollow()
                    && !(mob.getMoveControl() instanceof LegacyMoveControl) && !movementGoal, "old AI setup " + EntityType.getKey(type));
            mob.discard();
        }
        for (EntityType<? extends Mob> type : List.of(HbmEntityTypes.FBI.get(), HbmEntityTypes.MASK_MAN.get(),
                HbmEntityTypes.UNDEAD_SOLDIER.get(), HbmEntityTypes.CYBER_CRAB.get(), HbmEntityTypes.TESLA_CRAB.get(),
                HbmEntityTypes.TAINT_CRAB.get(), HbmEntityTypes.BLOCK_SPIDER.get())) {
            Mob mob = type.create(level);
            check(problems, cases, mob.getNavigation() instanceof LegacyPathNavigation nav && !nav.isManualFollow()
                    && mob.getMoveControl() instanceof LegacyMoveControl, "new AI path finder / move helper " + EntityType.getKey(type));
            mob.discard();
        }
    }

    private static void buildPlatform(ServerLevel level, boolean build) {
        for (int x = -2; x <= 44; x++) {
            for (int z = -6; z <= 6; z++) {
                level.setBlock(ORIGIN.offset(x, -1, z), build ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                for (int y = 0; y < 4; y++) {
                    level.setBlock(ORIGIN.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void pathFinder(ServerLevel level, List<String> problems, int[] cases) {
        Mob fbi = HbmEntityTypes.FBI.get().create(level);
        fbi.moveTo(ORIGIN.getX() + 0.5D, ORIGIN.getY(), ORIGIN.getZ() + 0.5D, 0.0F, 0.0F);
        fbi.setOnGround(true);
        LegacyPathNavigation nav = (LegacyPathNavigation) fbi.getNavigation();
        Path near = nav.pathToXYZ(ORIGIN.getX() + 6, ORIGIN.getY(), ORIGIN.getZ() + 3, 16.0F, false);
        check(problems, cases, near != null && near.canReach() && cardinal(near), "reachable path, only cardinal steps");
        check(problems, cases, nav.pathToXYZ(ORIGIN.getX() + 40, ORIGIN.getY(), ORIGIN.getZ(), 16.0F, false) == null,
                "World.getEntityPathToXYZ beyond maxDist of the target gives no path");
        Path partial = nav.partialPathToCoord(ORIGIN.getX() + 40, ORIGIN.getY(), ORIGIN.getZ(), 16.0F, false);
        Node end = partial == null ? null : partial.getEndNode();
        check(problems, cases, end != null && end.x - ORIGIN.getX() >= 15 && end.x - ORIGIN.getX() <= 16 && end.y == ORIGIN.getY(),
                "PathFinderUtils partial path ends 16 blocks towards the target: " + (end == null ? "null" : end.asBlockPos()));
        BlockPos target = LegacyPathFinder.partialTarget(level, fbi, ORIGIN.getX() + 40.5D, ORIGIN.getY() + 5.0D, ORIGIN.getZ() + 0.5D, 16.0F);
        check(problems, cases, target.getY() == ORIGIN.getY(), "partial target is moved down onto the ground: " + target);
        float water = fbi.getPathfindingMalus(PathType.WATER);
        nav.partialPathToCoord(ORIGIN.getX() + 10, ORIGIN.getY(), ORIGIN.getZ(), 16.0F, true);
        check(problems, cases, fbi.getPathfindingMalus(PathType.WATER) == water, "water flag restores the malus");

        // EntityMoveHelper: at most 30 degrees per tick, AI move speed = speed * movementSpeed
        fbi.setYRot(0.0F);
        fbi.getMoveControl().setWantedPosition(fbi.getX() + 10.0D, fbi.getY(), fbi.getZ(), 1.0D);
        fbi.getMoveControl().tick();
        check(problems, cases, Math.abs(fbi.getYRot() - 330.0F) < 0.01F || Math.abs(fbi.getYRot() + 30.0F) < 0.01F,
                "EntityMoveHelper turns 30 degrees: " + fbi.getYRot());
        check(problems, cases, Math.abs(fbi.getSpeed() - 0.3F) < 1.0E-6F, "EntityMoveHelper speed " + fbi.getSpeed());
        fbi.discard();
    }

    private static void movement(ServerLevel level, List<String> problems, int[] cases) {
        PathfinderMob maggot = HbmEntityTypes.PARASITE_MAGGOT.get().create(level);
        maggot.moveTo(ORIGIN.getX() + 0.5D, ORIGIN.getY(), ORIGIN.getZ() + 0.5D, 0.0F, 0.0F);
        maggot.setOnGround(true);
        LegacyPathNavigation nav = (LegacyPathNavigation) maggot.getNavigation();
        LegacyCreatureAi ai = new LegacyCreatureAi(maggot, nav);
        boolean followed = false;
        for (int attempt = 0; attempt < 5 && !followed; attempt++) { // 1/100 per tick the idle branch runs instead
            maggot.setYRot(0.0F);
            nav.setLegacyPath(nav.pathToXYZ(ORIGIN.getX() + 10, ORIGIN.getY(), ORIGIN.getZ(), 16.0F, false));
            followed = ai.followPath(null, false, 1.0D);
        }
        check(problems, cases, followed && Math.abs(maggot.getYRot() + 30.0F) < 0.01F && maggot.zza == 1.0F && maggot.xxa == 0.0F,
                "old AI path following: 30 degree turn, moveForward = attribute: yaw=" + maggot.getYRot() + " zza=" + maggot.zza);

        Mob target = HbmEntityTypes.PARASITE_MAGGOT.get().create(level);
        target.moveTo(ORIGIN.getX() + 0.5D, ORIGIN.getY(), ORIGIN.getZ() + 6.5D, 0.0F, 0.0F);
        followed = false;
        for (int attempt = 0; attempt < 5 && !followed; attempt++) {
            maggot.setYRot(0.0F);
            nav.setLegacyPath(nav.pathToXYZ(ORIGIN.getX() + 10, ORIGIN.getY(), ORIGIN.getZ(), 16.0F, false));
            followed = ai.followPath(target, true, 1.0D);
        }
        check(problems, cases, followed && Math.abs(maggot.xxa) > 0.1F && Math.abs(maggot.zza) < 1.0F,
                "old AI strafes while hasAttacked: xxa=" + maggot.xxa + " zza=" + maggot.zza);
        target.discard();
        maggot.discard();
    }

    private static boolean cardinal(Path path) {
        for (int i = 1; i < path.getNodeCount(); i++) {
            Node a = path.getNode(i - 1);
            Node b = path.getNode(i);
            if (a.x != b.x && a.z != b.z) {
                return false;
            }
        }
        return true;
    }

    private static boolean resource(String path) {
        try (InputStream input = ParityJ1bDiagnostics.class.getResourceAsStream(path)) {
            return input != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) {
            problems.add("parity j1b: " + message);
        }
    }
}

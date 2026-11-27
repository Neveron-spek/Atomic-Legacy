package com.atomiclegacy.event;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.BreederReactorBlock;
import com.atomiclegacy.entity.GlyphidEntity;
import com.atomiclegacy.entity.LegacyPigeonEntity;
import com.atomiclegacy.entity.ai.LegacyMoveControl;
import com.atomiclegacy.entity.ai.LegacyPathNavigation;
import com.atomiclegacy.entity.logic.EntityWaypoint;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
import com.atomiclegacy.inventory.recipes.FusionRecipe;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.inventory.recipes.PlasmaForgeRecipe;
import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;
import com.atomiclegacy.inventory.recipes.PurexRecipe;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;
import com.atomiclegacy.inventory.recipes.SupercomputerRecipe;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

/**
 * [parity K1 (2026-11-19, k1 remainder pass): diagnostics] GUIScreenRecipeSelector data (registration order, internal
 * names, name wrappers, setNamed keys, GenericRecipe#print keys), machine_reactor id of the breeding reactor, glyphid
 * attackEntity on a high-priority waypoint, pigeon path finder. ParityK1Diagnostics is the earlier K1 batch.
 */
final class ParityK1bDiagnostics {
    private static final BlockPos ORIGIN = new BlockPos(-112, 300, 112);

    private ParityK1bDiagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            recipeOrder(level, problems, cases);
            recipeLang(problems, cases);
            reactor(problems, cases);
            glyphidWaypoint(level, problems, cases);
            pigeon(level, problems, cases);
            legacyBook(level, problems, cases);
        } catch (RuntimeException | ReflectiveOperationException exception) {
            problems.add("K1b: diagnostics failed: " + exception);
        }
        return cases[0];
    }

    /** 1.7.10 written book (short id 386, plain-text pages) of the lighthouse chest must load as a 1.21.1 stack. */
    private static void legacyBook(ServerLevel level, List<String> problems, int[] cases) {
        net.minecraft.nbt.CompoundTag stack = new net.minecraft.nbt.CompoundTag();
        stack.putShort("id", (short) 386);
        stack.putByte("Count", (byte) 1);
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putString("title", "Log");
        data.putString("author", "Keeper");
        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        pages.add(net.minecraft.nbt.StringTag.valueOf("We keep the light turning.\n\"It's\" late."));
        data.put("pages", pages);
        stack.put("tag", data);
        boolean kept = com.atomiclegacy.worldgen.structure.HbmLegacyNbtTemplate.diagnosticMigrateItem(stack);
        net.minecraft.world.item.ItemStack parsed = net.minecraft.world.item.ItemStack.parseOptional(level.registryAccess(), stack);
        var content = parsed.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
        check(problems, cases, kept && parsed.is(net.minecraft.world.item.Items.WRITTEN_BOOK) && content != null
                && content.pages().size() == 1
                && content.pages().getFirst().raw().getString().equals("We keep the light turning.\n\"It's\" late."),
                "legacy written book loads with its page text: " + parsed + " " + content);
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) {
            problems.add("K1b: " + message);
        }
    }

    private static <I extends net.minecraft.world.item.crafting.RecipeInput, T extends Recipe<I>> void order(ServerLevel level, List<String> problems, int[] cases, String type,
            RecipeType<T> recipeType, Function<T, String> group, String expectedFirst) {
        List<RecipeHolder<T>> sorted = level.getRecipeManager().getAllRecipesFor(recipeType).stream()
                .sorted(LegacyRecipeNames.comparator(type, group)).toList();
        List<String> unknown = new ArrayList<>();
        long last = Long.MIN_VALUE;
        boolean monotonic = true;
        for (RecipeHolder<T> holder : sorted) {
            String name = LegacyRecipeNames.internalName(type, holder, group.apply(holder.value()));
            long key = LegacyRecipeNames.sortKey(type, name);
            if (key == Long.MAX_VALUE) unknown.add(holder.id().getPath());
            if (key < last) monotonic = false;
            last = key;
        }
        check(problems, cases, !sorted.isEmpty() && unknown.isEmpty(), type + " recipes without 1.7.10 name: " + unknown);
        check(problems, cases, monotonic, type + " comparator not monotonic");
        check(problems, cases, !sorted.isEmpty() && expectedFirst.equals(LegacyRecipeNames.internalName(type, sorted.getFirst(),
                group.apply(sorted.getFirst().value()))), type + " first recipe != " + expectedFirst);
    }

    private static void recipeOrder(ServerLevel level, List<String> problems, int[] cases) {
        order(level, problems, cases, LegacyRecipeNames.ASSEMBLY, HbmRecipeTypes.ASSEMBLY_MACHINE.get(), AssemblyMachineRecipe::group, "ass.plateiron");
        order(level, problems, cases, LegacyRecipeNames.PLASMA_FORGE, HbmRecipeTypes.PLASMA_FORGE.get(), PlasmaForgeRecipe::group,
                LegacyRecipeNames.order(LegacyRecipeNames.PLASMA_FORGE).getFirst());
        order(level, problems, cases, LegacyRecipeNames.FUSION, HbmRecipeTypes.FUSION.get(), FusionRecipe::group, "fus.dd");
        order(level, problems, cases, LegacyRecipeNames.PRECISION_ASSEMBLER, HbmRecipeTypes.PRECISION_ASSEMBLER.get(),
                PrecisionAssemblerRecipe::group, "precass.crystalcircuit");
        order(level, problems, cases, LegacyRecipeNames.PUREX, HbmRecipeTypes.PUREX.get(), PurexRecipe::group, "purex.uzh");
        order(level, problems, cases, LegacyRecipeNames.ROCK_MILL, HbmRecipeTypes.ROCK_MILL.get(), RockMillRecipe::group, "rock.cobble");
        order(level, problems, cases, LegacyRecipeNames.SUPERCOMPUTER, HbmRecipeTypes.SUPERCOMPUTER.get(), SupercomputerRecipe::group,
                LegacyRecipeNames.order(LegacyRecipeNames.SUPERCOMPUTER).getFirst());
        // PrecAssRecipes#registerPair: every base recipe is followed by its ".recycle" twin
        List<String> precass = LegacyRecipeNames.order(LegacyRecipeNames.PRECISION_ASSEMBLER);
        int base = precass.indexOf("precass.chip");
        check(problems, cases, base >= 0 && "precass.chip.recycle".equals(precass.get(base + 1)), "precass recycle order");
        check(problems, cases, "purex.recycle".equals(LegacyRecipeNames.wrapper("purex.zirnoxnu"))
                && "purex.schrab".equals(LegacyRecipeNames.wrapper("purex.schrabpwr"))
                && "rock.crushing".equals(LegacyRecipeNames.wrapper("rock.cobble"))
                && LegacyRecipeNames.wrapper("rock.clay") == null
                && "com.copy".equals(LegacyRecipeNames.wrapper("com.copyflightcalc"))
                && LegacyRecipeNames.wrapper("com.klaus") == null
                && "precass.recycle".equals(LegacyRecipeNames.wrapper("precass.chip.recycle")), "name wrappers");
        check(problems, cases, LegacyRecipeNames.isNamed("fus.dd") && LegacyRecipeNames.isNamed("ass.nitra")
                && LegacyRecipeNames.isNamed("chem.aggregate") && !LegacyRecipeNames.isNamed("chem.stone"), "setNamed recipes");
        Map<String, String> paths = LegacyRecipeNames.pathNames();
        check(problems, cases, "purex.schrabpwr".equals(paths.get("purex:schrab_pwr_mep"))
                && "rock.schist".equals(paths.get("rock_mill:gneiss")), "file name mapping");
    }

    private static void recipeLang(List<String> problems, int[] cases) {
        JsonObject lang;
        try (InputStream input = ParityK1bDiagnostics.class.getResourceAsStream("/assets/" + AtomicLegacy.MOD_ID + "/lang/en_us.json")) {
            if (input == null) {
                check(problems, cases, false, "en_us.json missing");
                return;
            }
            lang = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException exception) {
            check(problems, cases, false, "en_us.json unreadable: " + exception);
            return;
        }
        List<String> keys = new ArrayList<>(List.of("gui.recipe.duration", "gui.recipe.consumption", "gui.recipe.input",
                "gui.recipe.output", "gui.recipe.atPressure", "gui.recipe.fusionIn", "gui.recipe.fusionOut", "gui.recipe.fusionFlux",
                "gui.recipe.plasmaIn", "gui.recipe.setRecipe", "rock.crushing", "purex.recycle", "purex.schrab",
                "precass.recycle", "com.copy", "fus.dd", "fus.stellar", "ass.nitra", "ass.analogAlt", "chem.hydrogen", "chem.aggregate"));
        List<String> missing = keys.stream().filter(key -> !lang.has(key)).toList();
        check(problems, cases, missing.isEmpty(), "recipe tooltip keys missing: " + missing);
        check(problems, cases, "Press ENTER to toggle focus".equals(lang.get("gui.recipe.search_focus").getAsString())
                && "Consumption".equals(lang.get("gui.recipe.consumption").getAsString())
                && "Reprocessing of %s".equals(lang.get("purex.recycle").getAsString())
                && "Breeding Reactor (LEGACY)".equals(lang.get("block.atomiclegacy.machine_reactor").getAsString())
                && !lang.has("block.atomiclegacy.machine_reactor_breeding"), "recipe/reactor lang values");
    }

    private static void reactor(List<String> problems, int[] cases) {
        ResourceLocation id = AtomicLegacy.id("machine_reactor");
        ResourceLocation old = AtomicLegacy.id("machine_reactor_breeding");
        check(problems, cases, BuiltInRegistries.BLOCK.get(id) instanceof BreederReactorBlock
                && HbmBlocks.MACHINE_REACTOR.get() == BuiltInRegistries.BLOCK.get(id), "machine_reactor is the breeding reactor");
        check(problems, cases, !BuiltInRegistries.BLOCK.containsKey(old) && !BuiltInRegistries.ITEM.containsKey(old)
                && !BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(old) && !BuiltInRegistries.MENU.containsKey(old),
                "machine_reactor_breeding left over");
        check(problems, cases, BuiltInRegistries.ITEM.get(id) == HbmBlocks.MACHINE_REACTOR.get().asItem()
                && id.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(HbmBlockEntities.BREEDER_REACTOR.get()))
                && HbmBlockEntities.BREEDER_REACTOR.get().isValid(HbmBlocks.MACHINE_REACTOR.get().defaultBlockState()),
                "machine_reactor item/block entity");
        // ModBlocks: setHardness(5.0F).setResistance(10.0F)
        check(problems, cases, HbmBlocks.MACHINE_REACTOR.get().defaultBlockState().getDestroySpeed(null, BlockPos.ZERO) == 5.0F,
                "machine_reactor hardness");
    }

    private static void glyphidWaypoint(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        level.setBlock(ORIGIN.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
        GlyphidEntity glyphid = HbmEntityTypes.GLYPHID.get().create(level);
        EntityWaypoint waypoint = HbmEntityTypes.GLYPHID_WAYPOINT.get().create(level);
        if (glyphid == null || waypoint == null) {
            check(problems, cases, false, "glyphid/waypoint create");
            return;
        }
        glyphid.moveTo(ORIGIN.getX() + 0.5D, ORIGIN.getY(), ORIGIN.getZ() + 0.5D, 0.0F, 0.0F);
        waypoint.moveTo(ORIGIN.getX() + 1.5D, ORIGIN.getY(), ORIGIN.getZ() + 0.5D, 0.0F, 0.0F);
        Method attack = GlyphidEntity.class.getDeclaredMethod("legacyAttackEntity", Entity.class, float.class);
        attack.setAccessible(true);
        Field attackTime = GlyphidEntity.class.getDeclaredField("legacyAttackTime");
        attackTime.setAccessible(true);
        attack.invoke(glyphid, waypoint, glyphid.distanceTo(waypoint));
        // EntityMob.attackEntity: attackTime = 20, EntityGlyphid.attackEntityAsMob swings at the waypoint
        check(problems, cases, attackTime.getInt(glyphid) == 20 && glyphid.swinging, "glyphid attackEntity on waypoint");
        attackTime.setInt(glyphid, 0);
        glyphid.swinging = false;
        waypoint.moveTo(ORIGIN.getX() + 4.5D, ORIGIN.getY(), ORIGIN.getZ() + 0.5D, 0.0F, 0.0F);
        attack.invoke(glyphid, waypoint, glyphid.distanceTo(waypoint));
        check(problems, cases, attackTime.getInt(glyphid) == 0 && !glyphid.swinging, "glyphid attackEntity range < 2");
        glyphid.discard();
        waypoint.discard();
        level.setBlock(ORIGIN.below(), Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    private static void pigeon(ServerLevel level, List<String> problems, int[] cases) {
        LegacyPigeonEntity pigeon = HbmEntityTypes.PIGEON.get().create(level);
        if (pigeon == null) {
            check(problems, cases, false, "pigeon create");
            return;
        }
        // EntityPigeon isAIEnabled: PathNavigate (1.7.10 PathFinder) and EntityMoveHelper
        check(problems, cases, pigeon.getNavigation() instanceof LegacyPathNavigation navigation && !navigation.isManualFollow()
                && pigeon.getMoveControl() instanceof LegacyMoveControl, "pigeon navigation/move helper");
        pigeon.discard();
    }
}

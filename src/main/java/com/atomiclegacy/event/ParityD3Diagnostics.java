package com.atomiclegacy.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.ArcWelderRecipe;
import com.atomiclegacy.inventory.recipes.SolderingStationRecipe;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.ArcWelderBlockEntity;
import com.atomiclegacy.tileentity.BedrockOreBlockEntity;
import com.atomiclegacy.tileentity.SolderingStationBlockEntity;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * [parity d3] Server-side checks of the appearance batch: break-particle textures of registered blocks
 * (no legacy_placeholder where 1.7.10 has an icon), presence of the copied resources, item/handheld for
 * setFull3D items, and the "display" stack of the arc welder / soldering station reaching the update tag.
 * Not covered here (client only): the actual drawing.
 */
final class ParityD3Diagnostics {
    private ParityD3Diagnostics() {
    }

    private static final String ASSETS = "/assets/" + AtomicLegacy.MOD_ID + "/";
    private static final Pattern MODEL = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Blocks that keep the placeholder particle: no icon in 1.7.10 (charges, particle accelerator parts,
     * floodlight beam), port-only helper blocks, and unported legacy placeholders.
     */
    private static final Set<String> PLACEHOLDER_ALLOWED = Set.of(
            "bm_power_box", "charge_c4", "charge_dynamite", "charge_miner", "charge_semtex", "control_panel_custom",
            "copper", "crane_ejector", "dummy_block_puf6", "dummy_block_silo_hatch", "dummy_block_uf6", "dummy_block_vault",
            "dummy_plate_cargo", "floodlight_beam", "gold", "heavy_door_part", "keypad_test", "machine_battery",
            "machine_battery_potato", "machine_dineutronium_battery", "machine_dummy", "machine_fensu",
            "machine_lithium_battery", "machine_minirtg", "machine_schrabidium_battery", "machine_tacz_ammo_assembler",
            "ngtv", "niobium", "null", "obj_tester", "pa_beamline", "pa_detector", "pa_dipole", "pa_quadrupole", "pa_rfc",
            "pa_source", "railgun_plasma", "railing_bend", "railing_end_flipped_floor", "railing_end_flipped_self",
            "railing_end_floor", "railing_end_self", "railing_normal", "sand_gold198", "schrabidate", "silo_hatch_drillgon",
            "sliding_blast_door_2", "sliding_blast_door_keypad", "sliding_blast_door_legacy", "sliding_gate_door",
            "small_hatch", "spinny_light", "spotlight_beam", "tantalium", "undef", "void", "waste_grass_tall");

    private static final List<String> HANDHELD = List.of(
            "wrench", "designator_arty_range", "ullapool_caber", "screwdriver", "screwdriver_desh", "hand_drill",
            "hand_drill_desh", "wrench_archineer", "blowtorch", "acetylene_torch", "arc_electrode",
            "arc_electrode_lanthanium", "arc_electrode_desh", "arc_electrode_saturnite", "arc_electrode_burnt",
            "arc_electrode_burnt_lanthanium", "arc_electrode_burnt_desh", "arc_electrode_burnt_saturnite",
            "schrabidium_hoe", "titanium_pickaxe", "titanium_shovel", "titanium_hoe", "steel_hoe", "alloy_hoe", "cmb_hoe",
            "desh_hoe", "cobalt_hoe", "cobalt_decorated_hoe", "starmetal_hoe", "schrabidium_hammer", "bottle_opener",
            "wrench_flipped", "memespoon");

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            particles(problems, cases);
            resources(problems, cases);
            display(server.overworld(), problems, cases);
            bedrockOre(server.overworld(), problems, cases);
        } catch (Exception exception) {
            problems.add("D3: diagnostics threw " + exception);
        }
        return cases[0];
    }

    private static void particles(List<String> problems, int[] cases) {
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            String blockstate = text("blockstates/" + id.getPath() + ".json");
            if (blockstate == null) {
                continue;
            }
            Matcher matcher = MODEL.matcher(blockstate);
            boolean ok = true;
            while (matcher.find()) {
                String particle = particle(matcher.group(1), 0);
                if (particle == null) {
                    continue;
                }
                if (particle.contains("legacy_placeholder") && !PLACEHOLDER_ALLOWED.contains(id.getPath())) {
                    problems.add("D3: " + id.getPath() + " break particles use legacy_placeholder (" + matcher.group(1) + ")");
                    ok = false;
                } else if (particle.startsWith(AtomicLegacy.MOD_ID + ":")
                        && !exists("textures/" + particle.substring(particle.indexOf(':') + 1) + ".png")) {
                    problems.add("D3: " + id.getPath() + " particle texture missing: " + particle);
                    ok = false;
                }
                if (!ok) {
                    break;
                }
            }
            cases[0]++;
        }
    }

    /** Particle texture of a model of this mod; null when it cannot be decided from the JSON chain. */
    private static String particle(String model, int depth) {
        if (!model.startsWith(AtomicLegacy.MOD_ID + ":") || depth > 8) {
            return null;
        }
        String source = text("models/" + model.substring(model.indexOf(':') + 1) + ".json");
        if (source == null) {
            return null;
        }
        JsonObject json = JsonParser.parseString(source).getAsJsonObject();
        JsonObject textures = json.has("textures") ? json.getAsJsonObject("textures") : new JsonObject();
        String texture = string(textures.get("particle"));
        for (int i = 0; texture != null && texture.startsWith("#") && i < 5; i++) {
            texture = string(textures.get(texture.substring(1)));
        }
        if (texture != null) {
            return texture;
        }
        String parent = string(json.get("parent"));
        if (parent == null) {
            return null;
        }
        String inherited = particle(parent, depth + 1);
        if (inherited == null) {
            for (String key : new String[] {"all", "side", "texture", "cross"}) {
                if (textures.has(key)) {
                    return string(textures.get(key));
                }
            }
        }
        return inherited;
    }

    private static void resources(List<String> problems, int[] cases) {
        for (int i = 0; i < 10; i++) {
            check(exists("textures/block/ore_random_" + (i + 1) + ".png"), problems, cases, "ore_random_" + (i + 1) + ".png missing");
            String shape = text("models/block/ore_bedrock_shape_" + i + ".json");
            check(shape != null && shape.contains("block/ore_random_" + (i + 1) + "\"") && shape.contains("\"tintindex\": 1"),
                    problems, cases, "ore_bedrock_shape_" + i + " must overlay ore_random_" + (i + 1) + " with tint index 1");
        }
        String mush = text("models/block/mush.json");
        check(mush != null && mush.contains("minecraft:block/cross"), problems, cases, "mush must use the crossed-squares model (render type 1)");
        String wand = text("models/block/wand_air.json");
        check(wand != null && JsonParser.parseString(wand).getAsJsonObject().getAsJsonArray("elements").size() == 7,
                problems, cases, "wand_air must have the outer cube and six inner faces (RenderBlockWand)");
        for (String item : HANDHELD) {
            String model = text("models/item/" + item + ".json");
            check(model != null && "minecraft:item/handheld".equals(string(JsonParser.parseString(model).getAsJsonObject().get("parent"))),
                    problems, cases, item + " must be item/handheld (isFull3D in 1.7.10)");
        }
    }

    private static void display(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = new BlockPos(26_000, 100, 26_000);
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        try {
            List<RecipeHolder<ArcWelderRecipe>> arc = level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get());
            ArcWelderRecipe arcRecipe = null;
            for (RecipeHolder<ArcWelderRecipe> holder : arc) {
                if (holder.value().inputs().size() <= ArcWelderBlockEntity.INPUT_END - ArcWelderBlockEntity.INPUT_START
                        && holder.value().inputs().stream().noneMatch(input -> input.ingredient().getItems().length == 0)) {
                    arcRecipe = holder.value();
                    break;
                }
            }
            check(arcRecipe != null, problems, cases, "no arc welder recipe to test display with");
            if (arcRecipe != null) {
                level.setBlock(pos, HbmBlocks.MACHINE_ARC_WELDER.get().defaultBlockState(), 2 | 16);
                ArcWelderBlockEntity welder = (ArcWelderBlockEntity) level.getBlockEntity(pos);
                check(welder.getDisplay().isEmpty(), problems, cases, "empty arc welder has no display");
                for (int i = 0; i < arcRecipe.inputs().size(); i++) {
                    ArcWelderRecipe.CountedIngredient input = arcRecipe.inputs().get(i);
                    welder.setItem(ArcWelderBlockEntity.INPUT_START + i, input.ingredient().getItems()[0].copyWithCount(input.count()));
                }
                ArcWelderBlockEntity.tick(level, pos, level.getBlockState(pos), welder);
                check(ItemStack.isSameItemSameComponents(welder.getDisplay(), arcRecipe.result()), problems, cases,
                        "arc welder display must be the matched recipe output, got " + welder.getDisplay());
                CompoundTag tag = welder.getUpdateTag(level.registryAccess());
                check(welder.getUpdatePacket() != null, problems, cases, "arc welder has an update packet");
                ArcWelderBlockEntity client = new ArcWelderBlockEntity(pos, level.getBlockState(pos));
                client.handleUpdateTag(tag, level.registryAccess());
                check(ItemStack.matches(client.getDisplay(), welder.getDisplay()) && !client.getDisplay().isEmpty(), problems, cases,
                        "arc welder display survives the update tag");
                for (int i = ArcWelderBlockEntity.INPUT_START; i < ArcWelderBlockEntity.INPUT_END; i++) {
                    welder.setItem(i, ItemStack.EMPTY);
                }
                ArcWelderBlockEntity.tick(level, pos, level.getBlockState(pos), welder);
                check(welder.getDisplay().isEmpty(), problems, cases, "arc welder display clears without a recipe");
                welder.clearContent();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            }

            SolderingStationRecipe solderRecipe = null;
            for (RecipeHolder<SolderingStationRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLDERING_STATION.get())) {
                SolderingStationRecipe recipe = holder.value();
                List<SolderingStationRecipe.CountedIngredient> all = new ArrayList<>(recipe.toppings());
                all.addAll(recipe.pcb());
                all.addAll(recipe.solder());
                if (recipe.toppings().size() <= 3 && recipe.pcb().size() <= 2 && recipe.solder().size() <= 1
                        && all.stream().noneMatch(input -> input.ingredient().getItems().length == 0)) {
                    solderRecipe = recipe;
                    break;
                }
            }
            check(solderRecipe != null, problems, cases, "no soldering recipe to test display with");
            if (solderRecipe != null) {
                level.setBlock(pos, HbmBlocks.MACHINE_SOLDERING_STATION.get().defaultBlockState(), 2 | 16);
                SolderingStationBlockEntity station = (SolderingStationBlockEntity) level.getBlockEntity(pos);
                fill(station, SolderingStationBlockEntity.TOPPING_START, solderRecipe.toppings());
                fill(station, SolderingStationBlockEntity.PCB_START, solderRecipe.pcb());
                fill(station, SolderingStationBlockEntity.SOLDER_SLOT, solderRecipe.solder());
                SolderingStationBlockEntity.tick(level, pos, level.getBlockState(pos), station);
                // the first matching recipe wins, as in SolderingRecipes.getRecipe; it need not be the one used to fill
                check(!station.getDisplay().isEmpty(), problems, cases, "soldering station display must be set while a recipe matches");
                SolderingStationBlockEntity client = new SolderingStationBlockEntity(pos, level.getBlockState(pos));
                client.handleUpdateTag(station.getUpdateTag(level.registryAccess()), level.registryAccess());
                check(ItemStack.matches(client.getDisplay(), station.getDisplay()) && !client.getDisplay().isEmpty(), problems, cases,
                        "soldering station display survives the update tag");
                check(station.getUpdatePacket() != null, problems, cases, "soldering station has an update packet");
                for (int i = 0; i <= SolderingStationBlockEntity.SOLDER_SLOT; i++) {
                    station.setItem(i, ItemStack.EMPTY);
                }
                SolderingStationBlockEntity.tick(level, pos, level.getBlockState(pos), station);
                check(station.getDisplay().isEmpty(), problems, cases, "soldering station display clears without a recipe");
                station.clearContent();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        } finally {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
        }
    }

    private static void fill(SolderingStationBlockEntity station, int start, List<SolderingStationRecipe.CountedIngredient> inputs) {
        for (int i = 0; i < inputs.size(); i++) {
            station.setItem(start + i, inputs.get(i).ingredient().getItems()[0].copyWithCount(inputs.get(i).count()));
        }
    }

    private static void bedrockOre(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = new BlockPos(26_000, 100, 26_000);
        BedrockOreBlockEntity ore = new BedrockOreBlockEntity(pos, HbmBlocks.ORE_BEDROCK.get().defaultBlockState());
        check(ore.color() == 0, problems, cases, "bedrock ore colour defaults to 0 (derived from the resource on the client)");
        ore.configure(new ItemStack(Items.QUARTZ), ore.acidRequirement(), 0xF0EFDD, 1, 13);
        check(ore.shape() == 3 && ore.color() == 0xF0EFDD, problems, cases, "bedrock ore keeps colour and shape % 10");
        BedrockOreBlockEntity client = new BedrockOreBlockEntity(pos, HbmBlocks.ORE_BEDROCK.get().defaultBlockState());
        client.handleUpdateTag(ore.getUpdateTag(level.registryAccess()), level.registryAccess());
        check(client.shape() == 3 && client.color() == 0xF0EFDD && client.resource().is(Items.QUARTZ), problems, cases,
                "bedrock ore colour, shape and resource reach the client tag");
        check(Integer.valueOf(3).equals(client.getModelData().get(BedrockOreBlockEntity.SHAPE)), problems, cases,
                "bedrock ore exposes its shape as model data");
        ore.setLevel(level); // ClientboundBlockEntityDataPacket.create reads the registry access from the level
        check(ore.getUpdatePacket() != null, problems, cases, "bedrock ore has an update packet");
    }

    private static String string(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static boolean exists(String path) {
        try (InputStream stream = ParityD3Diagnostics.class.getResourceAsStream(ASSETS + path)) {
            return stream != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private static String text(String path) {
        try (InputStream stream = ParityD3Diagnostics.class.getResourceAsStream(ASSETS + path)) {
            if (stream == null) {
                return null;
            }
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (Exception exception) {
            return null;
        }
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) {
            problems.add("D3: " + message);
        }
    }
}

package com.atomiclegacy.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyVariantBlock;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes.AStack;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.worldgen.structure.HbmStructureIO;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * [parity X1: custom machines] CustomMachineConfigJSON (HbmMods 936c60c): config/hbmConfig/hbmCustomMachines.json with
 * the default paper press, read once after registration (MainRegistry.PostLoad; here FMLCommonSetupEvent).
 * Forced conversions: block and item ids are the port's ("atomiclegacy:cm_sheet" for "hbm:tile.cm_sheet", the old
 * "hbm:tile."/"hbm:item." prefixes are still accepted); ore dictionary names are item tags ("c:ingots/steel");
 * the 1.7.10 metadata of a component is the port's variant index (LegacyVariantBlock), pillar axis (0 / 4 / 8) or 0.
 * The controller item carries the configuration by its custom_data "machineType" instead of the damage value i + 100.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CustomMachineConfigJSON {
    public static final Gson gson = new Gson();
    public static final Map<String, MachineConfiguration> customMachines = new HashMap<>();
    public static final List<MachineConfiguration> niceList = new ArrayList<>();
    /** CraftingManager.addRecipeAuto(stack, parts) of every configuration with a valid core recipe, in config order. */
    public static final Map<MachineConfiguration, ShapedRecipe> coreRecipes = new LinkedHashMap<>();
    public static final String MACHINE_TYPE = "machineType";

    private CustomMachineConfigJSON() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            initialize();
            CustomMachineRecipes.initialize();
        });
    }

    public static File configHbmDir() {
        File dir = FMLPaths.CONFIGDIR.get().resolve("hbmConfig").toFile();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void initialize() {
        customMachines.clear();
        niceList.clear();
        coreRecipes.clear();
        File config = new File(configHbmDir().getAbsolutePath() + File.separatorChar + "hbmCustomMachines.json");

        if (!config.exists()) {
            writeDefault(config);
        }

        readConfig(config);
    }

    public static void writeDefault(File config) {
        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(Files.newOutputStream(config.toPath()), StandardCharsets.UTF_8));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("machines").beginArray();

            writer.beginObject();
            writer.name("recipeKey").value("paperPress");
            writer.name("unlocalizedName").value("paperPress");
            writer.name("localization").beginObject();
            writer.name("de_DE").value("Papierpresse");
            writer.endObject();
            writer.name("localizedName").value("Paper Press");
            writer.name("fluidInCount").value(1);
            writer.name("fluidInCap").value(1_000);
            writer.name("itemInCount").value(1);
            writer.name("fluidOutCount").value(0);
            writer.name("fluidOutCap").value(0);
            writer.name("itemOutCount").value(1);
            writer.name("generatorMode").value(false);
            writer.name("maxPollutionCap").value(100);
            writer.name("fluxMode").value(false);
            writer.name("recipeSpeedMult").value(1.0D);
            writer.name("recipeConsumptionMult").value(1.0D);
            writer.name("maxPower").value(10_000L);
            writer.name("maxHeat").value(0);

            writer.name("recipeShape").beginArray();
            writer.value("IPI").value("PCP").value("IPI");
            writer.endArray();

            writer.name("recipeParts").beginArray().setIndent("");
            writer.value("I");
            CustomMachineRecipes.writeAStack(AStack.dict("c:ingots/steel", 1), writer); // OreDictManager.STEEL.ingot()
            writer.setIndent("");
            writer.value("P");
            CustomMachineRecipes.writeAStack(AStack.dict("c:plates/steel", 1), writer); // OreDictManager.STEEL.plate()
            writer.setIndent("");
            writer.value("C");
            CustomMachineRecipes.writeAStack(AStack.item(AtomicLegacy.MOD_ID + ":circuit_basic", 1), writer); // circuit, EnumCircuitType.BASIC
            writer.endArray().setIndent("  ");

            writer.name("components").beginArray();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = 0; z <= 2; z++) {
                        if (!(x == 0 && y == 0 && z == 1) && !(x == 0 && z == 0)) {
                            writer.beginObject().setIndent("");
                            writer.name("block").value(y == 0 ? AtomicLegacy.MOD_ID + ":cm_sheet" : AtomicLegacy.MOD_ID + ":cm_block");
                            writer.name("x").value(x);
                            writer.name("y").value(y);
                            writer.name("z").value(z);
                            writer.name("metas").beginArray();
                            writer.value(0);
                            writer.endArray();
                            writer.endObject().setIndent("  ");
                        }
                    }
                }
            }

            writer.beginObject().setIndent("");
            writer.name("block").value(AtomicLegacy.MOD_ID + ":cm_port");
            writer.name("x").value(0);
            writer.name("y").value(-1);
            writer.name("z").value(0);
            writer.name("metas").beginArray();
            writer.value(0);
            writer.endArray();
            writer.endObject().setIndent("  ");

            writer.beginObject().setIndent("");
            writer.name("block").value(AtomicLegacy.MOD_ID + ":cm_port");
            writer.name("x").value(0);
            writer.name("y").value(1);
            writer.name("z").value(0);
            writer.name("metas").beginArray();
            writer.value(0);
            writer.endArray();
            writer.endObject().setIndent("  ");

            writer.endArray();
            writer.endObject();

            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (IOException e) {
            AtomicLegacy.LOGGER.error("Failed to write {}", config, e);
        }
    }

    /**
     * Opens a config file written before the mod id rename (reinhardtshbm -> atomiclegacy) and rewrites the old
     * namespace once, so existing hbmConfig/hbmRecipes files keep working.
     */
    public static java.io.Reader openMigrated(File file) throws java.io.IOException {
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (text.contains("reinhardtshbm:")) {
            text = text.replace("reinhardtshbm:", "atomiclegacy:");
            try {
                Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
                AtomicLegacy.LOGGER.info("Migrated old mod id in {}", file);
            } catch (java.io.IOException e) {
                AtomicLegacy.LOGGER.warn("Could not rewrite migrated {}", file, e);
            }
        }
        return new java.io.StringReader(text);
    }

    public static void readConfig(File config) {
        try (java.io.Reader reader = openMigrated(config)) {
            readConfig(gson.fromJson(reader, JsonObject.class));
        } catch (Exception ex) {
            AtomicLegacy.LOGGER.error("Failed to read {}", config, ex);
        }
    }

    /** CustomMachineConfigJSON#readConfig after the file has been parsed. */
    public static void readConfig(JsonObject json) {
        try {
            JsonArray machines = json.get("machines").getAsJsonArray();

            for (int i = 0; i < machines.size(); i++) {
                JsonObject machineObject = machines.get(i).getAsJsonObject();

                MachineConfiguration configuration = new MachineConfiguration();
                configuration.recipeKey = machineObject.get("recipeKey").getAsString();
                configuration.unlocalizedName = machineObject.get("unlocalizedName").getAsString();
                configuration.localizedName = machineObject.get("localizedName").getAsString();
                if (machineObject.has("localization")) {
                    JsonObject localization = machineObject.get("localization").getAsJsonObject();
                    for (Entry<String, JsonElement> entry : localization.entrySet()) {
                        configuration.localization.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                configuration.fluidInCount = machineObject.get("fluidInCount").getAsInt();
                configuration.fluidInCap = machineObject.get("fluidInCap").getAsInt();
                configuration.itemInCount = machineObject.get("itemInCount").getAsInt();
                configuration.fluidOutCount = machineObject.get("fluidOutCount").getAsInt();
                configuration.fluidOutCap = machineObject.get("fluidOutCap").getAsInt();
                configuration.itemOutCount = machineObject.get("itemOutCount").getAsInt();
                configuration.generatorMode = machineObject.get("generatorMode").getAsBoolean();
                if (machineObject.has("maxPollutionCap")) configuration.maxPollutionCap = machineObject.get("maxPollutionCap").getAsInt();
                if (machineObject.has("fluxMode")) configuration.fluxMode = machineObject.get("fluxMode").getAsBoolean();
                configuration.recipeSpeedMult = machineObject.get("recipeSpeedMult").getAsDouble();
                configuration.recipeConsumptionMult = machineObject.get("recipeConsumptionMult").getAsDouble();
                configuration.maxPower = machineObject.get("maxPower").getAsLong();
                if (machineObject.has("maxHeat")) configuration.maxHeat = machineObject.get("maxHeat").getAsInt();

                if (machineObject.has("recipeShape") && machineObject.has("recipeParts")) {
                    try {
                        JsonArray recipeShape = machineObject.get("recipeShape").getAsJsonArray();
                        JsonArray recipeParts = machineObject.get("recipeParts").getAsJsonArray();

                        List<String> shape = new ArrayList<>();
                        for (int j = 0; j < recipeShape.size(); j++) {
                            shape.add(recipeShape.get(j).getAsString());
                        }

                        Map<Character, Ingredient> key = new HashMap<>();
                        Character symbol = null;
                        for (int j = 0; j < recipeParts.size(); j++) {
                            if (j % 2 == 0) {
                                symbol = recipeParts.get(j).getAsString().charAt(0); //god is dead and we killed him
                            } else {
                                AStack a = CustomMachineRecipes.readAStack(recipeParts.get(j).getAsJsonArray());
                                key.put(symbol, a.ingredient());
                            }
                        }

                        ItemStack stack = stackFor(configuration);
                        coreRecipes.put(configuration, new ShapedRecipe("", CraftingBookCategory.MISC,
                                ShapedRecipePattern.of(key, shape), stack));
                    } catch (Exception ex) {
                        AtomicLegacy.LOGGER.error("Caught exception trying to parse core recipe for custom machine " + configuration.unlocalizedName);
                        AtomicLegacy.LOGGER.error("recipeShape was" + machineObject.get("recipeShape").toString());
                        AtomicLegacy.LOGGER.error("recipeParts was" + machineObject.get("recipeParts").toString());
                    }
                }

                JsonArray components = machineObject.get("components").getAsJsonArray();
                configuration.components = new ArrayList<>();

                for (int j = 0; j < components.size(); j++) {
                    JsonObject compObject = components.get(j).getAsJsonObject();
                    ComponentDefinition compDef = new ComponentDefinition();
                    compDef.block = blockByName(compObject.get("block").getAsString());
                    compDef.x = compObject.get("x").getAsInt();
                    compDef.y = compObject.get("y").getAsInt();
                    compDef.z = compObject.get("z").getAsInt();
                    compDef.allowedMetas = new HashSet<>();
                    compDef.metas = new ArrayList<>();
                    JsonArray metas = compObject.get("metas").getAsJsonArray();
                    for (int k = 0; k < metas.size(); k++) {
                        compDef.metas.add(metas.get(k).getAsInt());
                        compDef.allowedMetas.add(metas.get(k).getAsInt());
                    }

                    configuration.components.add(compDef);
                }

                customMachines.put(configuration.unlocalizedName, configuration);
                niceList.add(configuration);
            }

        } catch (Exception ex) {
            AtomicLegacy.LOGGER.error("Failed to parse custom machine configuration", ex);
        }
    }

    /** Block.blockRegistry.getObject(name): null for unknown names (the structure then never validates). */
    @Nullable
    public static Block blockByName(String name) {
        ResourceLocation id = ResourceLocation.tryParse(HbmStructureIO.normalizeId(name));
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        return BuiltInRegistries.BLOCK.get(id);
    }

    /** new ItemStack(ModBlocks.custom_machine, 1, i + 100) with the "machineType" tag. */
    public static ItemStack stackFor(MachineConfiguration configuration) {
        ItemStack stack = new ItemStack(HbmBlocks.CUSTOM_MACHINE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(MACHINE_TYPE, configuration.unlocalizedName);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** The configuration selected by a controller stack (the 1.7.10 damage value), or null for INVALID MACHINE CONTROLLER. */
    @Nullable
    public static MachineConfiguration configFor(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains(MACHINE_TYPE) ? customMachines.get(tag.getString(MACHINE_TYPE)) : null;
    }

    /** The 1.7.10 metadata a component block state represents. */
    public static int legacyMeta(BlockState state) {
        if (state.hasProperty(LegacyVariantBlock.VARIANT)) return state.getValue(LegacyVariantBlock.VARIANT);
        if (state.getBlock() instanceof RotatedPillarBlock && state.hasProperty(BlockStateProperties.AXIS)) {
            return switch (state.getValue(BlockStateProperties.AXIS)) {
                case X -> 4;
                case Z -> 8;
                default -> 0;
            };
        }
        Property<?> meta = state.getBlock().getStateDefinition().getProperty("meta");
        if (meta instanceof IntegerProperty integer) return state.getValue(integer);
        return 0;
    }

    /** world.setBlock(x, y, z, block, meta, 3) for a component. */
    public static BlockState stateFor(Block block, int meta) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(LegacyVariantBlock.VARIANT) && LegacyVariantBlock.VARIANT.getPossibleValues().contains(meta)) {
            return state.setValue(LegacyVariantBlock.VARIANT, meta);
        }
        if (block instanceof RotatedPillarBlock && state.hasProperty(BlockStateProperties.AXIS)) {
            return state.setValue(BlockStateProperties.AXIS, (meta & 12) == 4 ? Direction.Axis.X : (meta & 12) == 8 ? Direction.Axis.Z : Direction.Axis.Y);
        }
        Property<?> property = block.getStateDefinition().getProperty("meta");
        if (property instanceof IntegerProperty integer && integer.getPossibleValues().contains(meta)) {
            return state.setValue(integer, meta);
        }
        return state;
    }

    /** MainRegistry.proxy.getLanguageCode() compared with the 1.7.10 codes of the localization map ("de_DE"). */
    @Nullable
    public static String localized(MachineConfiguration config, String languageCode) {
        for (Entry<String, String> entry : config.localization.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(languageCode)) return entry.getValue();
        }
        return null;
    }

    public static class MachineConfiguration {

        /** The name of the recipe set that this machine can handle */
        public String recipeKey;
        /** The internal name of this machine */
        public String unlocalizedName;
        /** The display name of this machine */
        public String localizedName;
        public HashMap<String, String> localization = new HashMap<>();

        public int fluidInCount;
        public int fluidInCap;
        public int itemInCount;
        public int fluidOutCount;
        public int fluidOutCap;
        public int itemOutCount;
        /** Whether inputs should be used up when the process begins */
        public boolean generatorMode;
        public int maxPollutionCap;
        public boolean fluxMode;
        public double recipeSpeedMult = 1D;
        public double recipeConsumptionMult = 1D;
        public long maxPower;
        public int maxHeat;

        /** Definitions of blocks that this machine is composed of */
        public List<ComponentDefinition> components;

        public String id() {
            return unlocalizedName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        }
    }

    public static class ComponentDefinition {
        @Nullable
        public Block block;
        public Set<Integer> allowedMetas;
        /** the "metas" array in file order (RenderCustomMachine cycles through it once per second) */
        public List<Integer> metas;
        public int x;
        public int y;
        public int z;

        /** (int) comp.allowedMetas.toArray()[0]: a HashSet of small integers iterates in ascending order. */
        public int firstMeta() {
            int min = Integer.MAX_VALUE;
            for (int meta : allowedMetas) min = Math.min(min, meta);
            return min == Integer.MAX_VALUE ? 0 : min;
        }
    }
}

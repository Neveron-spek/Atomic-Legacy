package com.atomiclegacy.inventory.recipes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.worldgen.structure.HbmStructureIO;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * [parity X1: custom machines] CustomMachineRecipes + the SerializableRecipe file handling it relies on (HbmMods 936c60c):
 * config/hbmRecipes/hbmCustomMachines.json is read when present, otherwise the defaults are registered and the template
 * _hbmCustomMachines.json is written; /ntmreload runs this again. Stack arrays keep the SerializableRecipe formats
 * (["item", id, count, meta], ["dict", name, count], ["nbt", id, count, meta, snbt], [id, count, meta, chance],
 * [fluid, mB, pressure]); ids are the port's and dictionary names are item tags. The client and server read their own
 * files like the original; with GeneralConfig.enableServerRecipeSync (off by default) a dedicated server sends its
 * file to every player logging in (SerializableRecipePayload) and the client drops it again on disconnect.
 */
public final class CustomMachineRecipes {
    public static final Gson gson = new Gson();
    public static final HashMap<String, List<CustomMachineRecipe>> recipes = new HashMap<>();
    public static final String FILE_NAME = "hbmCustomMachines.json";
    /** SerializableRecipe.recipeSyncHandlers: recipe files received from the server, by file name. */
    public static final Map<String, byte[]> recipeSyncHandlers = new HashMap<>();
    /** SerializableRecipe.modified. */
    public static boolean modified = false;

    private CustomMachineRecipes() {
    }

    public static void registerDefaults() {
        List<CustomMachineRecipe> list = new ArrayList<>();
        CustomMachineRecipe recipe = new CustomMachineRecipe();
        recipe.inputFluids = new HbmFluidStack[] {new HbmFluidStack(fluid("WATER"), 250)};
        recipe.inputItems = new AStack[] {AStack.item(AtomicLegacy.MOD_ID + ":powder_sawdust", 1)};
        recipe.outputFluids = new HbmFluidStack[0];
        recipe.outputItems = new ChanceStack[] {new ChanceStack(new ItemStack(Items.PAPER, 3), 1F)};
        recipe.duration = 60;
        recipe.consumptionPerTick = 10;
        recipe.pollutionType = "SOOT";
        recipe.pollutionAmount = 0.03F;
        recipe.radiationAmount = 0;
        recipe.flux = 0;
        recipe.heat = 0;
        list.add(recipe);
        recipes.put("paperPress", list);
    }

    /** SerializableRecipe.initialize for this one handler. */
    public static void initialize() {
        File recDir = FMLPaths.CONFIGDIR.get().resolve("hbmRecipes").toFile();

        if (!recDir.exists()) {
            if (!recDir.mkdir()) {
                throw new IllegalStateException("Unable to make recipe directory " + recDir.getAbsolutePath());
            }
        }

        File info = new File(recDir.getAbsolutePath() + File.separatorChar + "REMOVE UNDERSCORE TO ENABLE RECIPE LOADING - RECIPES WILL RESET TO DEFAULT OTHERWISE");
        try { info.createNewFile(); } catch (IOException e) { }

        recipes.clear();

        modified = false;
        File recFile = new File(recDir.getAbsolutePath() + File.separatorChar + FILE_NAME);
        if (recipeSyncHandlers.containsKey(FILE_NAME)) {
            AtomicLegacy.LOGGER.info("Reading synced recipe file " + FILE_NAME);
            try (Reader reader = new InputStreamReader(new java.io.ByteArrayInputStream(recipeSyncHandlers.get(FILE_NAME)), StandardCharsets.UTF_8)) {
                readRecipeStream(reader);
                modified = true;
            } catch (Throwable ex) {
                AtomicLegacy.LOGGER.error("Failed to reset synced recipe stream", ex);
            }
        } else if (recFile.exists() && recFile.isFile()) {
            AtomicLegacy.LOGGER.info("Reading recipe file " + recFile.getName());
            readRecipeFile(recFile);
            modified = true;
        } else {
            AtomicLegacy.LOGGER.info("No recipe file found, registering defaults for " + FILE_NAME);
            registerDefaults();

            File recTemplate = new File(recDir.getAbsolutePath() + File.separatorChar + "_" + FILE_NAME);
            AtomicLegacy.LOGGER.info("Writing template file " + recTemplate.getName());
            writeTemplateFile(recTemplate);
        }
    }

    /** SerializableRecipe.receiveRecipes. */
    public static void receiveRecipes(String filename, byte[] data) {
        recipeSyncHandlers.put(filename, data);
    }

    /** SerializableRecipe.clearReceivedRecipes (the client left the server). */
    public static void clearReceivedRecipes() {
        boolean hasCleared = !recipeSyncHandlers.isEmpty();
        recipeSyncHandlers.clear();

        if (hasCleared) initialize();
    }

    /** The recipe file of this handler in config/hbmRecipes. */
    public static File recipeFile() {
        return FMLPaths.CONFIGDIR.get().resolve("hbmRecipes").resolve(FILE_NAME).toFile();
    }

    public static void writeTemplateFile(File template) {
        try {
            List<Entry<String, List<CustomMachineRecipe>>> recipeList = new ArrayList<>(recipes.entrySet());

            if (recipeList.isEmpty())
                throw new IllegalStateException("Error while writing recipes for CustomMachineRecipes: Recipe list is either empty or in an unsupported format!");

            JsonWriter writer = new JsonWriter(new OutputStreamWriter(Files.newOutputStream(template.toPath()), StandardCharsets.UTF_8));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("recipes").beginArray();

            for (Entry<String, List<CustomMachineRecipe>> recipe : recipeList) {
                writer.beginObject();
                writeRecipe(recipe, writer);
                writer.endObject();
            }

            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (Exception ex) {
            AtomicLegacy.LOGGER.error("Failed to write recipe template {}", template, ex);
        }
    }

    public static void readRecipeFile(File file) {
        try (Reader reader = com.atomiclegacy.config.CustomMachineConfigJSON.openMigrated(file)) {
            readRecipeStream(reader);
        } catch (IOException ex) { }
    }

    public static void readRecipeStream(Reader reader) {
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        JsonArray array = json.get("recipes").getAsJsonArray();
        for (JsonElement recipe : array) {
            if (recipe != null) readRecipe(recipe);
        }
    }

    public static void readRecipe(JsonElement recipe) {
        JsonObject obj = recipe.getAsJsonObject();

        String name = obj.get("recipeKey").getAsString();
        List<CustomMachineRecipe> list = new ArrayList<>();
        JsonArray array = obj.get("recipes").getAsJsonArray();

        for (int i = 0; i < array.size(); i++) {
            JsonObject rec = array.get(i).getAsJsonObject();
            CustomMachineRecipe recipeInstance = new CustomMachineRecipe();
            recipeInstance.inputFluids = readFluidArray(rec.get("inputFluids").getAsJsonArray());
            recipeInstance.inputItems = readAStackArray(rec.get("inputItems").getAsJsonArray());
            recipeInstance.outputFluids = readFluidArray(rec.get("outputFluids").getAsJsonArray());
            recipeInstance.outputItems = readItemStackArrayChance(rec.get("outputItems").getAsJsonArray());
            recipeInstance.duration = rec.get("duration").getAsInt();
            recipeInstance.consumptionPerTick = rec.get("consumptionPerTick").getAsInt();

            if (rec.has("pollutionType") && rec.has("pollutionAmount")) {
                recipeInstance.pollutionType = rec.get("pollutionType").getAsString();
                recipeInstance.pollutionAmount = rec.get("pollutionAmount").getAsFloat();
            } else {
                recipeInstance.pollutionType = "";
            }

            if (rec.has("radiationAmount")) recipeInstance.radiationAmount = rec.get("radiationAmount").getAsFloat();
            if (rec.has("flux")) recipeInstance.flux = rec.get("flux").getAsInt();
            if (rec.has("heat")) recipeInstance.heat = rec.get("heat").getAsInt();

            list.add(recipeInstance);
        }

        recipes.put(name, list);
    }

    public static void writeRecipe(Entry<String, List<CustomMachineRecipe>> entry, JsonWriter writer) throws IOException {
        writer.name("recipeKey").value(entry.getKey());
        writer.name("recipes").beginArray();

        for (CustomMachineRecipe recipeInstance : entry.getValue()) {
            writer.beginObject();

            writer.name("inputFluids").beginArray();
            for (HbmFluidStack stack : recipeInstance.inputFluids) writeFluidStack(stack, writer);
            writer.endArray();

            writer.name("inputItems").beginArray();
            for (AStack stack : recipeInstance.inputItems) writeAStack(stack, writer);
            writer.endArray();

            writer.name("outputFluids").beginArray();
            for (HbmFluidStack stack : recipeInstance.outputFluids) writeFluidStack(stack, writer);
            writer.endArray();

            writer.name("outputItems").beginArray();
            for (ChanceStack stack : recipeInstance.outputItems) writeItemStackChance(stack, writer);
            writer.endArray();

            // Gson 2.2.4 had no value(float): floats were widened to double
            writer.name("duration").value(recipeInstance.duration);
            writer.name("consumptionPerTick").value(recipeInstance.consumptionPerTick);
            writer.name("pollutionType").value(recipeInstance.pollutionType);
            writer.name("pollutionAmount").value((double) recipeInstance.pollutionAmount);
            writer.name("radiationAmount").value((double) recipeInstance.radiationAmount);
            writer.name("flux").value(recipeInstance.flux);
            writer.name("heat").value(recipeInstance.heat);

            writer.endObject();
        }

        writer.endArray();
    }

    /*
     * JSON IO UTIL (SerializableRecipe)
     */

    @Nullable
    private static Item itemByName(String name) {
        ResourceLocation id = ResourceLocation.tryParse(HbmStructureIO.normalizeId(name));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        return BuiltInRegistries.ITEM.get(id);
    }

    private static Item nothing() {
        ResourceLocation id = AtomicLegacy.id("nothing");
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
    }

    public static AStack readAStack(JsonArray array) {
        try {
            String type = array.get(0).getAsString();
            int stacksize = array.size() > 2 ? array.get(2).getAsInt() : 1;
            if ("nbt".equals(type)) {
                Item item = itemByName(array.get(1).getAsString());
                CompoundTag nbt = array.size() > 4 ? TagParser.parseTag(array.get(4).getAsString()) : null;
                if (item != null) return new AStack(AStack.Kind.NBT, array.get(1).getAsString(), stacksize, item, null, nbt);
            }
            if ("item".equals(type)) {
                Item item = itemByName(array.get(1).getAsString());
                if (item != null) return new AStack(AStack.Kind.ITEM, array.get(1).getAsString(), stacksize, item, null, null);
            }
            if ("dict".equals(type)) {
                String dict = array.get(1).getAsString();
                ResourceLocation tag = ResourceLocation.tryParse(dict);
                if (tag != null) return new AStack(AStack.Kind.DICT, dict, stacksize, null, TagKey.create(Registries.ITEM, tag), null);
            }
        } catch (Exception ex) { }
        AtomicLegacy.LOGGER.error("Error reading stack array " + array.toString());
        Item nothing = nothing();
        return new AStack(AStack.Kind.ITEM, BuiltInRegistries.ITEM.getKey(nothing).toString(), 1, nothing, null, null);
    }

    public static AStack[] readAStackArray(JsonArray array) {
        try {
            AStack[] items = new AStack[array.size()];
            for (int i = 0; i < items.length; i++) { items[i] = readAStack((JsonArray) array.get(i)); }
            return items;
        } catch (Exception ex) { }
        AtomicLegacy.LOGGER.error("Error reading stack array " + array.toString());
        return new AStack[0];
    }

    public static void writeAStack(AStack astack, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        if (astack.kind() == AStack.Kind.NBT) {
            writer.value(astack.nbt() != null ? "nbt" : "item");
            writer.value(astack.name());
            if (astack.stacksize() != 1 || astack.nbt() != null) writer.value(astack.stacksize());
            if (astack.nbt() != null) writer.value(0);
            if (astack.nbt() != null) writer.value(astack.nbt().toString());
        } else if (astack.kind() == AStack.Kind.ITEM) {
            writer.value("item");
            writer.value(astack.name());
            if (astack.stacksize() != 1) writer.value(astack.stacksize());
        } else {
            writer.value("dict");
            writer.value(astack.name());
            if (astack.stacksize() != 1) writer.value(astack.stacksize());
        }
        writer.endArray();
        writer.setIndent("  ");
    }

    public static ChanceStack readItemStackChance(JsonArray array) {
        try {
            Item item = itemByName(array.get(0).getAsString());
            int stacksize = array.size() > 2 ? array.get(1).getAsInt() : 1;
            if (item != null) {
                ItemStack stack = new ItemStack(item, stacksize);
                if (array.size() > 4) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(TagParser.parseTag(array.get(3).getAsString())));
                float chance = array.get(array.size() - 1).getAsFloat();
                return new ChanceStack(stack, chance);
            }
        } catch (Exception ex) { }
        AtomicLegacy.LOGGER.error("Error reading stack array " + array.toString() + " - defaulting to NOTHING item!");
        return new ChanceStack(new ItemStack(nothing()), 1F);
    }

    public static ChanceStack[] readItemStackArrayChance(JsonArray array) {
        try {
            ChanceStack[] items = new ChanceStack[array.size()];
            for (int i = 0; i < items.length; i++) { items[i] = readItemStackChance((JsonArray) array.get(i)); }
            return items;
        } catch (Exception ex) { }
        AtomicLegacy.LOGGER.error("Error reading stack array " + array.toString());
        return new ChanceStack[0];
    }

    public static void writeItemStackChance(ChanceStack stack, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        ItemStack key = stack.stack();
        CustomData data = key.get(DataComponents.CUSTOM_DATA);
        boolean hasTag = data != null && !data.isEmpty();
        writer.value(BuiltInRegistries.ITEM.getKey(key.getItem()).toString());
        if (key.getCount() != 1 || hasTag) writer.value(key.getCount());
        if (hasTag) writer.value(0);
        if (hasTag) writer.value(data.copyTag().toString());
        writer.value((double) stack.chance());
        writer.endArray();
        writer.setIndent("  ");
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseGet(() -> {
            for (HbmFluidDefinition definition : HbmFluids.definitions()) {
                if (definition.legacyFieldName().equalsIgnoreCase(name)) return definition;
            }
            throw new IllegalArgumentException("Unknown fluid " + name);
        });
    }

    public static HbmFluidStack readFluidStack(JsonArray array) {
        try {
            HbmFluidDefinition type = fluid(array.get(0).getAsString());
            int fill = array.get(1).getAsInt();
            int pressure = array.size() < 3 ? 0 : array.get(2).getAsInt();
            return new HbmFluidStack(type, fill, pressure);
        } catch (Exception ex) { }
        AtomicLegacy.LOGGER.error("Error reading fluid array " + array.toString());
        return new HbmFluidStack(HbmFluids.none(), 0);
    }

    public static HbmFluidStack[] readFluidArray(JsonArray array) {
        try {
            HbmFluidStack[] fluids = new HbmFluidStack[array.size()];
            for (int i = 0; i < fluids.length; i++) { fluids[i] = readFluidStack((JsonArray) array.get(i)); }
            return fluids;
        } catch (Exception ex) { }
        AtomicLegacy.LOGGER.error("Error reading fluid array " + array.toString());
        return new HbmFluidStack[0];
    }

    public static void writeFluidStack(HbmFluidStack stack, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        writer.value(stack.type().legacyFieldName());
        writer.value(stack.amount());
        if (stack.pressure() != 0) writer.value(stack.pressure());
        writer.endArray();
        writer.setIndent("  ");
    }

    /** RecipesCommon.AStack: ComparableStack ("item"), NBTStack ("nbt") or OreDictStack ("dict"). */
    public record AStack(Kind kind, String name, int stacksize, @Nullable Item item, @Nullable TagKey<Item> tag, @Nullable CompoundTag nbt) {
        public enum Kind { ITEM, NBT, DICT }

        public static AStack item(String id, int count) {
            Item item = itemByName(id);
            return new AStack(Kind.ITEM, id, count, item == null ? nothing() : item, null, null);
        }

        public static AStack dict(String tag, int count) {
            return new AStack(Kind.DICT, tag, count, null, TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)), null);
        }

        /** AStack#matchesRecipe(stack, ignoreSize = true) */
        public boolean matchesRecipe(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return switch (kind) {
                case ITEM -> stack.is(item);
                case NBT -> stack.is(item) && (nbt == null
                        || nbt.equals(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()));
                case DICT -> stack.is(tag);
            };
        }

        /** extractForNEI: the matching stacks with the recipe's stack size */
        public List<ItemStack> extractForNEI() {
            return Arrays.stream(ingredient().getItems()).map(stack -> {
                ItemStack copy = stack.copyWithCount(stacksize);
                if (kind == Kind.NBT && nbt != null) copy.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                return copy;
            }).toList();
        }

        public Ingredient ingredient() {
            return kind == Kind.DICT ? Ingredient.of(tag) : Ingredient.of(item);
        }
    }

    /** Tuple.Pair&lt;ItemStack, Float&gt; of an output with its chance. */
    public record ChanceStack(ItemStack stack, float chance) {
    }

    public static class CustomMachineRecipe {
        public HbmFluidStack[] inputFluids;
        public AStack[] inputItems;
        public HbmFluidStack[] outputFluids;
        public ChanceStack[] outputItems;

        public int duration;
        public int consumptionPerTick;

        public String pollutionType;
        public float pollutionAmount;
        public float radiationAmount;
        public int flux;
        public int heat;
    }

    /** Recipes of a recipe key (null when the key has none). */
    @Nullable
    public static List<CustomMachineRecipe> get(String recipeKey) {
        return recipes.get(recipeKey);
    }

    public static Map<String, List<CustomMachineRecipe>> all() {
        return recipes;
    }
}

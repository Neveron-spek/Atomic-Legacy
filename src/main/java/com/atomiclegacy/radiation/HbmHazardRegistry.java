package com.atomiclegacy.radiation;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.LegacyBedrockOreStageItem;
import com.atomiclegacy.items.LegacyHeldInventoryItem;
import com.atomiclegacy.items.LegacyHolotapeImageItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.OreBasaltBlockItem;
import com.atomiclegacy.items.PlateFuelItem;
import com.atomiclegacy.items.RbmkFuelRodItem;
import com.atomiclegacy.items.RbmkPelletItem;
import com.atomiclegacy.items.RtgDepletedPelletItem;
import com.atomiclegacy.items.RtgPelletItem;
import com.atomiclegacy.items.SellafieldBlockItem;
import com.atomiclegacy.items.StorageCrateBlockItem;
import com.atomiclegacy.items.ToolboxItem;
import com.atomiclegacy.items.WasteFuelItem;
import com.atomiclegacy.items.WatzPelletItem;
import com.atomiclegacy.items.ZirnoxRodItem;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [parity K1: HazardRegistry] Explicit item hazard table of HBM 1.7.10 (commit 936c60c): the
 * HazardRegistry.registerItems() registrations, the OreDictManager DictFrame hazards (ore dictionary keys with
 * mutex, the uranium/thorium ore blacklist), the HazardModifier classes and the HazardTransformers.
 * The registration body is generated from the original sources by work/tmp-k1/gen_hazards.py; item references
 * became port ids, metadata variants became "id#variant" keys.
 */
public final class HbmHazardRegistry {

    /** HazardTypeBase subclasses of the original. */
    public enum Type { RADIATION, DIGAMMA, HOT, BLINDING, ASBESTOS, COAL, HYDROACTIVE, EXPLOSIVE }

    public static final Type RADIATION = Type.RADIATION;
    public static final Type DIGAMMA = Type.DIGAMMA;
    public static final Type HOT = Type.HOT;
    public static final Type BLINDING = Type.BLINDING;
    public static final Type ASBESTOS = Type.ASBESTOS;
    public static final Type COAL = Type.COAL;
    public static final Type HYDROACTIVE = Type.HYDROACTIVE;
    public static final Type EXPLOSIVE = Type.EXPLOSIVE;

    /** HazardModifier. */
    @FunctionalInterface
    public interface Modifier {
        float modify(ItemStack stack, LivingEntity holder, float level);
    }

    /** HazardEntry: type, base level and the modifiers evaluated in order. */
    public record Entry(Type type, float baseLevel, List<Modifier> mods) {
        public float eval(ItemStack stack, LivingEntity holder) {
            float level = this.baseLevel;
            for (Modifier mod : this.mods) {
                level = mod.modify(stack, holder, level);
            }
            return level;
        }
    }

    /** Builder mirroring HazardData. */
    public static final class Data {
        final List<Entry> entries = new ArrayList<>();

        public Data addEntry(Type type) {
            return addEntry(type, 1F);
        }

        public Data addEntry(Type type, float level) {
            this.entries.add(new Entry(type, level, List.of()));
            return this;
        }

        Data addEntry(Entry entry) {
            this.entries.add(entry);
            return this;
        }
    }

    // ---------------------------------------------------------------- HazardRegistry constants (verbatim)
    //simplified groups for ReC compat
    public static final float gen_S = 10_000F;
    public static final float gen_H = 2_000F;
    public static final float gen_10D = 100F;
    public static final float gen_100D = 80F;
    public static final float gen_1Y = 50F;
    public static final float gen_10Y = 30F;
    public static final float gen_100Y = 10F;
    public static final float gen_1K = 7.5F;
    public static final float gen_10K = 6.25F;
    public static final float gen_100K = 5F;
    public static final float gen_1M = 2.5F;
    public static final float gen_10M = 1.5F;
    public static final float gen_100M = 1F;
    public static final float gen_1B = 0.5F;
    public static final float gen_10B = 0.1F;

    public static final float co60 = 30.0F;
    public static final float sr90 = 15.0F;
    public static final float tc99 = 2.75F;
    public static final float i131 = 150.0F;
    public static final float xe135 = 1250.0F;
    public static final float cs137 = 20.0F;
    public static final float au198 = 500.0F;
    public static final float pb209 = 10000.0F;
    public static final float at209 = 7500.0F;
    public static final float po210 = 75.0F;
    public static final float ra226 = 7.5F;
    public static final float ac227 = 30.0F;
    public static final float th232 = 0.1F;
    public static final float thf = 1.75F;
    public static final float u = 0.35F;
    public static final float u233 = 5.0F;
    public static final float u235 = 1.0F;
    public static final float u238 = 0.25F;
    public static final float uf = 0.5F;
    public static final float uzh = 0.125F;
    public static final float np237 = 2.5F;
    public static final float npf = 1.5F;
    public static final float pu = 7.5F;
    public static final float purg = 6.25F;
    public static final float pu238 = 10.0F;
    public static final float pu239 = 5.0F;
    public static final float pu240 = 7.5F;
    public static final float pu241 = 25.0F;
    public static final float puf = 4.25F;
    public static final float am241 = 8.5F;
    public static final float am242 = 9.5F;
    public static final float amrg = 9.0F;
    public static final float amf = 4.75F;
    public static final float mox = 2.5F;
    public static final float sa326 = 15.0F;
    public static final float sa327 = 17.5F;
    public static final float saf = 5.85F;
    public static final float sas3 = 5F;
    public static final float gh336 = 5.0F;
    public static final float mud = 1.0F;
    public static final float radsource_mult = 3.0F;
    public static final float pobe = po210 * radsource_mult;
    public static final float rabe = ra226 * radsource_mult;
    public static final float pube = pu238 * radsource_mult;
    public static final float zfb_bi = u235 * 0.35F;
    public static final float zfb_pu241 = pu241 * 0.5F;
    public static final float zfb_am_mix = amrg * 0.5F;
    public static final float bf = 300_000.0F;
    public static final float bfb = 500_000.0F;

    public static final float sr = sa326 * 0.1F;
    public static final float sb = sa326 * 0.1F;
    public static final float trx = 25.0F;
    public static final float trn = 0.1F;
    public static final float wst = 15.0F;
    public static final float wstv = 7.5F;
    public static final float yc = u;
    public static final float fo = 10F;

    public static final float nugget = 0.1F;
    public static final float ingot = 1.0F;
    public static final float gem = 1.0F;
    public static final float plate = ingot;
    public static final float plateCast = plate * 3;
    public static final float powder_mult = 3.0F;
    public static final float powder = ingot * powder_mult;
    public static final float powder_tiny = nugget * powder_mult;
    public static final float ore = ingot;
    public static final float block = 10.0F;
    public static final float crystal = block;
    public static final float billet = 0.5F;
    public static final float rtg = billet * 3;
    public static final float rod = 0.5F;
    public static final float rod_dual = rod * 2;
    public static final float rod_quad = rod * 4;
    public static final float rod_rbmk = rod * 8;
    // ---------------------------------------------------------------- table

    /** Port id or "id#variant" -> entries, in the original order (ore dictionary, item, stack). */
    private static final Map<String, List<Entry>> TABLE = new LinkedHashMap<>();
    /** Ore dictionary key of the entry that won the mutex, for the external-item tag fallback. */
    private static final Map<String, List<Entry>> ORE_KEYS = new HashMap<>();
    private static final Map<String, String> ITEM_ORE_KEY = new HashMap<>();
    /** Port-only objects mapped onto the original object they replace. */
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        registerOreDictionary();
        registerItems();
        registerAliases();
    }

    private HbmHazardRegistry() {
    }

    /** Unmodifiable view of the table (diagnostics). */
    public static Map<String, List<Entry>> table() {
        return Collections.unmodifiableMap(TABLE);
    }

    public static Map<String, String> aliases() {
        return Collections.unmodifiableMap(ALIASES);
    }

    /** Entries registered for a port key ("id" or "id#variant"), aliases resolved; never null. */
    public static List<Entry> entriesForKey(String key) {
        List<Entry> entries = TABLE.get(key);
        if (entries == null) {
            String alias = ALIASES.get(key);
            if (alias != null) {
                entries = TABLE.get(alias);
            }
        }
        return entries == null ? List.of() : entries;
    }

    /** HazardSystem.getHazardsFromStack: table entries plus the post transformers. */
    public static List<Entry> entries(ItemStack stack) {
        return entries(stack, null);
    }

    public static List<Entry> entries(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<Entry> entries;
        if (id.getNamespace().equals(AtomicLegacy.MOD_ID)) {
            String base = id.getPath();
            String variant = variant(stack);
            List<Entry> plain = entriesForKey(base);
            List<Entry> specific = variant == null ? List.of() : entriesForKey(base + "#" + variant);
            if (specific.isEmpty()) {
                entries = plain;
            } else if (plain.isEmpty()) {
                entries = specific;
            } else {
                entries = new ArrayList<>(plain);
                entries.addAll(specific);
            }
        } else if (id.getNamespace().equals("minecraft")) {
            entries = entriesForKey(id.toString());
            if (entries.isEmpty()) {
                entries = oreDictionaryEntries(stack);
            }
        } else {
            entries = oreDictionaryEntries(stack);
        }
        float transformed = transformedRadiation(stack, registries);
        if (transformed > 0) {
            List<Entry> withTransform = new ArrayList<>(entries);
            withTransform.add(new Entry(RADIATION, transformed, List.of()));
            return withTransform;
        }
        return entries;
    }

    /** HazardSystem.getHazardLevelFromStack: the first entry of the type, modifiers applied (holder null). */
    public static float level(ItemStack stack, Type type) {
        return level(stack, type, null);
    }

    public static float level(ItemStack stack, Type type, HolderLookup.Provider registries) {
        for (Entry entry : entries(stack, registries)) {
            if (entry.type() == type) {
                return entry.eval(stack, null);
            }
        }
        return 0F;
    }

    /** Base level of the first entry of the type registered for a port id (block radiation, BlockHazard). */
    public static float baseLevelForId(String id, Type type) {
        for (Entry entry : entriesForKey(id)) {
            if (entry.type() == type) {
                return entry.eval(ItemStack.EMPTY, null);
            }
        }
        return 0F;
    }

    /** The metadata variant of a port stack as used by the table keys, or null. */
    public static String variant(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SellafieldBlockItem) {
            CompoundTag data = customData(stack);
            if (data.contains("level")) {
                return Integer.toString(Math.max(0, Math.min(5, data.getInt("level"))));
            }
            var model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            return Integer.toString(model == null ? 0 : Math.max(0, Math.min(5, model.value())));
        }
        if (item instanceof WasteFuelItem) {
            return WasteFuelItem.isHot(stack) ? "hot" : "cold";
        }
        if (item instanceof WatzPelletItem) {
            return WatzPelletItem.isActivePellet(stack) ? WatzPelletItem.type(stack).id : null;
        }
        if (item instanceof LegacyHolotapeImageItem) {
            return LegacyHolotapeImageItem.tape(stack).id();
        }
        if (item instanceof OreBasaltBlockItem) {
            return OreBasaltBlockItem.variantName(stack);
        }
        if (item instanceof LegacyBedrockOreStageItem) {
            return LegacyBedrockOreStageItem.type(stack).name().toLowerCase(Locale.ROOT);
        }
        if (item instanceof LegacyVariantItem variantItem) {
            return variantItem.variant(stack).id();
        }
        if (item instanceof RtgDepletedPelletItem) {
            String material = customData(stack).getString("rtg_depleted_material");
            return material.isEmpty() ? null : material;
        }
        return null;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // ---------------------------------------------------------------- external items (ore dictionary -> c tags)

    private static final Map<String, String> TAG_SHAPES = Map.of(
            "nuggets", "nugget", "ingots", "ingot", "dusts", "dust", "storage_blocks", "block",
            "ores", "ore", "plates", "plate", "gems", "gem");

    /**
     * Other mods: the original applied every ore dictionary key of the stack. The 1.21 equivalent of
     * "ingotUranium" is the common tag c:ingots/uranium; the uranium/thorium ore blacklist is kept.
     */
    private static List<Entry> oreDictionaryEntries(ItemStack stack) {
        List<Entry> found = null;
        for (TagKey<Item> tag : stack.getTags().toList()) {
            ResourceLocation location = tag.location();
            if (!location.getNamespace().equals("c")) {
                continue;
            }
            String path = location.getPath();
            int slash = path.indexOf('/');
            if (slash <= 0) {
                continue;
            }
            String prefix = TAG_SHAPES.get(path.substring(0, slash));
            if (prefix == null) {
                continue;
            }
            String material = path.substring(slash + 1).replace("_", "");
            if (prefix.equals("ore") && (material.equals("uranium") || material.startsWith("thorium"))) {
                return List.of(); // HazardSystem.blacklist(U/TH232 ores)
            }
            if (found == null) {
                found = ORE_KEYS.get((prefix + material).toLowerCase(Locale.ROOT));
            }
        }
        return found == null ? List.of() : found;
    }

    // ---------------------------------------------------------------- transformers

    /** HazardTransformerRadiationNBT + HazardTransformerRadiationContainer (the ME transformer has no 1.21 target). */
    private static float transformedRadiation(ItemStack stack, HolderLookup.Provider registries) {
        float radiation = 0;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null && custom.contains(RAD_KEY)) {
            radiation += custom.copyTag().getFloat(RAD_KEY); // separate entry in the original; summed by the holder pass
        }
        Item item = stack.getItem();
        boolean isCrate = item instanceof StorageCrateBlockItem;
        boolean isBox = item instanceof LegacyHeldInventoryItem held && held.kind() == LegacyHeldInventoryItem.Kind.CONTAINMENT_BOX;
        boolean isBag = item instanceof LegacyHeldInventoryItem bag && bag.kind() == LegacyHeldInventoryItem.Kind.PLASTIC_BAG;
        boolean isContainer = item instanceof ToolboxItem;
        if (!isCrate && !isBox && !isBag && !isContainer) {
            return radiation;
        }
        if (stack.getComponentsPatch().isEmpty()) {
            return radiation;
        }
        CompoundTag data = customData(stack);
        HolderLookup.Provider provider = registries != null ? registries : serverRegistries();
        if (provider == null) {
            return radiation;
        }
        float contained = 0;
        if (isContainer) {
            for (ItemStack held : ToolboxItem.loadContents(stack, asAccess(provider))) {
                contained += containedRadiation(held, provider);
            }
        }
        if (isCrate) {
            if (data.contains(StorageCrateBlockEntity.ITEM_DATA_KEY)) {
                CompoundTag slots = data.getCompound(StorageCrateBlockEntity.ITEM_DATA_KEY);
                for (int i = 0; i < 104; i++) {
                    if (slots.contains("slot" + i)) {
                        contained += containedRadiation(ItemStack.parseOptional(provider, slots.getCompound("slot" + i)), provider);
                    }
                }
            }
        }
        if (isBox) {
            for (ItemStack held : LegacyHeldInventoryItem.loadContents(stack, asAccess(provider), LegacyHeldInventoryItem.Kind.CONTAINMENT_BOX)) {
                contained += containedRadiation(held, provider);
            }
            contained = (float) squirt(contained);
        }
        if (isBag) {
            for (ItemStack held : LegacyHeldInventoryItem.loadContents(stack, asAccess(provider), LegacyHeldInventoryItem.Kind.PLASTIC_BAG)) {
                contained += containedRadiation(held, provider);
            }
            contained *= 2F;
        }
        return contained > 0 ? radiation + contained : radiation;
    }

    public static final String RAD_KEY = "hfrHazRadiation";

    private static float containedRadiation(ItemStack held, HolderLookup.Provider provider) {
        return held.isEmpty() ? 0F : level(held, RADIATION, provider) * held.getCount();
    }

    private static RegistryAccess asAccess(HolderLookup.Provider provider) {
        if (provider instanceof RegistryAccess access) {
            return access;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? RegistryAccess.EMPTY : server.registryAccess();
    }

    private static HolderLookup.Provider serverRegistries() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    /** BobMathUtil.squirt: sqrt(x + 1/(x+2)^2) - 1/(x+2). */
    public static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    // ---------------------------------------------------------------- modifiers

    /** HazardModifierRBMKRadiation. */
    static Modifier rbmkRadiation(float target, boolean linear) {
        return (stack, holder, level) -> {
            if (stack.getItem() instanceof RbmkFuelRodItem) {
                double enrichment = 1D - RbmkFuelRodItem.depletion(stack); // ItemRBMKRod.getEnrichment
                double depletion = linear ? 1D - enrichment : 1D - Math.pow(enrichment, 2);
                double xenon = RbmkFuelRodItem.xenon(stack); // getPoisonLevel: xenon / 100
                level = (float) (level + (target - level) * depletion);
                level += xe135 * xenon;
            } else if (stack.getItem() instanceof RbmkPelletItem pellet) {
                int meta = pellet.state(stack);
                level = level + (target - level) * ((meta % 5) / 4F);
                if (meta >= 5) {
                    level += xe135 * nugget;
                }
            }
            return level;
        };
    }

    /** HazardModifierRBMKHot. */
    static Modifier rbmkHot() {
        return (stack, holder, level) -> {
            level = 0;
            if (stack.getItem() instanceof RbmkFuelRodItem) {
                double heat = RbmkFuelRodItem.hullHeat(stack);
                int fire = (int) Math.min(Math.ceil((heat - 100) / 10D), 60);
                level = fire;
            }
            return level;
        };
    }

    /** HazardModifierFuelRadiation: getDurabilityForDisplay of ItemZirnoxRod / ItemFuelRod (plate fuel). */
    static Modifier fuelRadiation(float target) {
        return (stack, holder, level) -> {
            double display;
            if (stack.getItem() instanceof ZirnoxRodItem rod) {
                display = (double) ZirnoxRodItem.life(stack) / (double) rod.fuel(stack).maxLife();
            } else if (stack.getItem() instanceof PlateFuelItem plate) {
                display = (double) PlateFuelItem.life(stack) / (double) plate.lifeTime();
            } else {
                display = 0D;
            }
            double depletion = Math.pow(display, 0.4D);
            level = (float) (level + (target - level) * depletion);
            return level;
        };
    }

    /** HazardModifierRTGRadiation. */
    static Modifier rtgRadiation(float target) {
        return (stack, holder, level) -> {
            if (stack.getItem() instanceof RtgPelletItem fuel) {
                long max = fuel.remainingLifespan(new ItemStack(fuel)); // getMaxLifespan (fresh pellet)
                double depletion = 1D - (double) fuel.remainingLifespan(stack) / (double) max;
                level = (float) (level + (target - level) * depletion);
            }
            return level;
        };
    }

    // ---------------------------------------------------------------- registration helpers (HazardRegistry)

    static boolean enable528() {
        try {
            return HbmConfig.ENABLE_528_MODE.get();
        } catch (RuntimeException notLoaded) {
            return false;
        }
    }

    private static Data makeData() { return new Data(); }
    private static Data makeData(Type hazard) { return new Data().addEntry(hazard); }
    private static Data makeData(Type hazard, float level) { return new Data().addEntry(hazard, level); }

    /**
     * [parity b2: enable528] HazardRegistry reads GeneralConfig.enable528 after the config is loaded;
     * this table is built in a static initializer that may run before the COMMON config exists, so the
     * two dependent entries are rebuilt from the config load/reload event.
     */
    public static void registerPileRods528(boolean enable528) {
        TABLE.put("pile_rod_pu239", new ArrayList<>(makeData(RADIATION, !enable528 ? purg * billet + pu239 * billet + u * billet : purg * billet + pu239 * billet + wst * billet).entries));
        TABLE.put("pile_rod_plutonium", new ArrayList<>(makeData(RADIATION, !enable528 ? purg * billet * 2 + u * billet : purg * billet * 2 + wst * billet).entries));
    }

    public static void onConfigLoaded() {
        registerPileRods528(enable528());
    }

    private static void register(String key, Data data) {
        TABLE.computeIfAbsent(key, unused -> new ArrayList<>()).addAll(data.entries);
    }

    /** DictFrame.registerHazards / registerStack for one item: hazard.clone(hazMult), mutex already resolved. */
    private static void ore(String key, String dictKey, Object... typeAndLevel) {
        register(key, entries(typeAndLevel));
        ITEM_ORE_KEY.put(key, dictKey);
    }

    /** An ore dictionary key carrying hazards (HazardSystem.oreMap), used for other mods' items via c tags. */
    private static void dict(String dictKey, Object... typeAndLevel) {
        ORE_KEYS.put(dictKey.toLowerCase(Locale.ROOT), List.copyOf(entries(typeAndLevel).entries));
    }

    private static Data entries(Object... typeAndLevel) {
        Data data = new Data();
        for (int i = 0; i < typeAndLevel.length; i += 2) {
            data.addEntry((Type) typeAndLevel[i], ((Number) typeAndLevel[i + 1]).floatValue());
        }
        return data;
    }

    /** Ore dictionary key whose hazards an item received (diagnostics). */
    public static String oreKeyOf(String key) {
        return ITEM_ORE_KEY.get(key);
    }

    private static void registerPWRFuel(String fuel, float baseRad) {
        register("pwr_fuel#" + fuel, makeData(RADIATION, baseRad));
        register("pwr_fuel_hot#" + fuel, makeData(RADIATION, baseRad * 10).addEntry(HOT, 5));
        register("pwr_fuel_depleted#" + fuel, makeData(RADIATION, baseRad * 10));
    }

    private static void registerRBMKPellet(String pellet, float base, float dep) { registerRBMKPellet(pellet, base, dep, false, 0F, 0F); }
    private static void registerRBMKPellet(String pellet, float base, float dep, boolean linear) { registerRBMKPellet(pellet, base, dep, linear, 0F, 0F); }
    private static void registerRBMKPellet(String pellet, float base, float dep, boolean linear, float blinding, float digamma) {
        Data data = new Data();
        data.addEntry(new Entry(RADIATION, base, List.of(rbmkRadiation(dep, linear))));
        if (blinding > 0) data.addEntry(new Entry(BLINDING, blinding, List.of()));
        if (digamma > 0) data.addEntry(new Entry(DIGAMMA, digamma, List.of()));
        register(pellet, data);
    }

    private static void registerRBMKRod(String rod, float base, float dep) { registerRBMK(rod, base, dep, true, false, 0F, 0F); }
    private static void registerRBMKRod(String rod, float base, float dep, float blinding) { registerRBMK(rod, base, dep, true, false, blinding, 0F); }
    private static void registerRBMKRod(String rod, float base, float dep, boolean linear) { registerRBMK(rod, base, dep, true, linear, 0F, 0F); }

    private static void registerRBMK(String rod, float base, float dep, boolean hot, boolean linear, float blinding, float digamma) {
        Data data = new Data();
        data.addEntry(new Entry(RADIATION, base, List.of(rbmkRadiation(dep, linear))));
        if (hot) data.addEntry(new Entry(HOT, 0, List.of(rbmkHot())));
        if (blinding > 0) data.addEntry(new Entry(BLINDING, blinding, List.of()));
        if (digamma > 0) data.addEntry(new Entry(DIGAMMA, digamma, List.of()));
        register(rod, data);
    }

    private static void registerBreedingRodRadiation(String type, float base) {
        register("rod#" + type, makeData(RADIATION, base));
        register("rod_dual#" + type, makeData(RADIATION, base * rod_dual));
        register("rod_quad#" + type, makeData(RADIATION, base * rod_quad));
    }

    private static void registerOtherFuel(String fuel, float base, float target, boolean blinding) {
        Data data = new Data();
        data.addEntry(new Entry(RADIATION, base, List.of(fuelRadiation(target))));
        if (blinding)
            data.addEntry(BLINDING, 20F);
        register(fuel, data);
    }

    private static void registerRTGPellet(String pellet, float base, float target) { registerRTGPellet(pellet, base, target, 0, 0); }
    private static void registerRTGPellet(String pellet, float base, float target, float hot) { registerRTGPellet(pellet, base, target, hot, 0); }

    private static void registerRTGPellet(String pellet, float base, float target, float hot, float blinding) {
        Data data = new Data();
        data.addEntry(new Entry(RADIATION, base, List.of(rtgRadiation(target))));
        if (hot > 0) data.addEntry(new Entry(HOT, hot, List.of()));
        if (blinding > 0) data.addEntry(new Entry(BLINDING, blinding, List.of()));
        register(pellet, data);
    }

    /** meta 0 -> "waste_x#cold", meta 1 -> "waste_x#hot" (both were stack registrations). */
    private static void registerOtherWaste(String waste, float base) {
        register(waste + "#cold", makeData(RADIATION, base * 0.075F));

        Data data = new Data();
        data.addEntry(new Entry(RADIATION, base, List.of()));
        data.addEntry(new Entry(HOT, 5F, List.of()));
        register(waste + "#hot", data);
    }

    private static void registerRadSourceWaste(String waste, float base) {
        register(waste + "#cold", makeData(RADIATION, base));

        Data data = new Data();
        data.addEntry(new Entry(RADIATION, base, List.of()));
        data.addEntry(new Entry(HOT, 5F, List.of()));
        register(waste + "#hot", data);
    }

    // ---------------------------------------------------------------- generated: OreDictManager DictFrame hazards

    private static void registerOreDictionary() {
        // dustCoal / dustTinyCoal / dustLignite / dustTinyLignite are HazardRegistry ore keys (makeData(COAL, powder)).
        dict("dustTinyCoal", COAL, powder_tiny);
        dict("dustCoal", COAL, powder);
        dict("nuggetUranium", RADIATION, u * nugget);
        dict("tinyUranium", RADIATION, u * nugget);
        dict("billetUranium", RADIATION, u * billet);
        dict("ingotUranium", RADIATION, u * ingot);
        dict("Uranium", RADIATION, u * ingot);
        dict("dustUranium", RADIATION, u * powder);
        dict("blockUranium", RADIATION, u * block);
        dict("nuggetUranium233", RADIATION, u233 * nugget);
        dict("nuggetU233", RADIATION, u233 * nugget);
        dict("tinyUranium233", RADIATION, u233 * nugget);
        dict("tinyU233", RADIATION, u233 * nugget);
        dict("billetUranium233", RADIATION, u233 * billet);
        dict("billetU233", RADIATION, u233 * billet);
        dict("ingotUranium233", RADIATION, u233 * ingot);
        dict("ingotU233", RADIATION, u233 * ingot);
        dict("Uranium233", RADIATION, u233 * ingot);
        dict("U233", RADIATION, u233 * ingot);
        dict("blockUranium233", RADIATION, u233 * block);
        dict("blockU233", RADIATION, u233 * block);
        dict("nuggetUranium235", RADIATION, u235 * nugget);
        dict("nuggetU235", RADIATION, u235 * nugget);
        dict("tinyUranium235", RADIATION, u235 * nugget);
        dict("tinyU235", RADIATION, u235 * nugget);
        dict("billetUranium235", RADIATION, u235 * billet);
        dict("billetU235", RADIATION, u235 * billet);
        dict("ingotUranium235", RADIATION, u235 * ingot);
        dict("ingotU235", RADIATION, u235 * ingot);
        dict("Uranium235", RADIATION, u235 * ingot);
        dict("U235", RADIATION, u235 * ingot);
        dict("blockUranium235", RADIATION, u235 * block);
        dict("blockU235", RADIATION, u235 * block);
        dict("nuggetUranium238", RADIATION, u238 * nugget);
        dict("nuggetU238", RADIATION, u238 * nugget);
        dict("tinyUranium238", RADIATION, u238 * nugget);
        dict("tinyU238", RADIATION, u238 * nugget);
        dict("billetUranium238", RADIATION, u238 * billet);
        dict("billetU238", RADIATION, u238 * billet);
        dict("ingotUranium238", RADIATION, u238 * ingot);
        dict("ingotU238", RADIATION, u238 * ingot);
        dict("Uranium238", RADIATION, u238 * ingot);
        dict("U238", RADIATION, u238 * ingot);
        dict("blockUranium238", RADIATION, u238 * block);
        dict("blockU238", RADIATION, u238 * block);
        dict("nuggetThorium232", RADIATION, th232 * nugget);
        dict("nuggetTh232", RADIATION, th232 * nugget);
        dict("nuggetThorium", RADIATION, th232 * nugget);
        dict("tinyThorium232", RADIATION, th232 * nugget);
        dict("tinyTh232", RADIATION, th232 * nugget);
        dict("tinyThorium", RADIATION, th232 * nugget);
        dict("billetThorium232", RADIATION, th232 * billet);
        dict("billetTh232", RADIATION, th232 * billet);
        dict("billetThorium", RADIATION, th232 * billet);
        dict("ingotThorium232", RADIATION, th232 * ingot);
        dict("ingotTh232", RADIATION, th232 * ingot);
        dict("ingotThorium", RADIATION, th232 * ingot);
        dict("Thorium232", RADIATION, th232 * ingot);
        dict("Th232", RADIATION, th232 * ingot);
        dict("Thorium", RADIATION, th232 * ingot);
        dict("dustThorium232", RADIATION, th232 * powder);
        dict("dustTh232", RADIATION, th232 * powder);
        dict("dustThorium", RADIATION, th232 * powder);
        dict("blockThorium232", RADIATION, th232 * block);
        dict("blockTh232", RADIATION, th232 * block);
        dict("blockThorium", RADIATION, th232 * block);
        dict("nuggetPlutonium", RADIATION, pu * nugget);
        dict("tinyPlutonium", RADIATION, pu * nugget);
        dict("billetPlutonium", RADIATION, pu * billet);
        dict("ingotPlutonium", RADIATION, pu * ingot);
        dict("Plutonium", RADIATION, pu * ingot);
        dict("dustPlutonium", RADIATION, pu * powder);
        dict("blockPlutonium", RADIATION, pu * block);
        dict("orePlutonium", RADIATION, pu * ore);
        dict("oreNetherPlutonium", RADIATION, pu * ore);
        dict("nuggetPlutoniumRG", RADIATION, purg * nugget);
        dict("tinyPlutoniumRG", RADIATION, purg * nugget);
        dict("billetPlutoniumRG", RADIATION, purg * billet);
        dict("ingotPlutoniumRG", RADIATION, purg * ingot);
        dict("PlutoniumRG", RADIATION, purg * ingot);
        dict("blockPlutoniumRG", RADIATION, purg * block);
        dict("nuggetPlutonium238", RADIATION, pu238 * nugget, HOT, 3F * nugget);
        dict("nuggetPu238", RADIATION, pu238 * nugget, HOT, 3F * nugget);
        dict("tinyPlutonium238", RADIATION, pu238 * nugget, HOT, 3F * nugget);
        dict("tinyPu238", RADIATION, pu238 * nugget, HOT, 3F * nugget);
        dict("billetPlutonium238", RADIATION, pu238 * billet, HOT, 3F * billet);
        dict("billetPu238", RADIATION, pu238 * billet, HOT, 3F * billet);
        dict("ingotPlutonium238", RADIATION, pu238 * ingot, HOT, 3F * ingot);
        dict("ingotPu238", RADIATION, pu238 * ingot, HOT, 3F * ingot);
        dict("Plutonium238", RADIATION, pu238 * ingot, HOT, 3F * ingot);
        dict("Pu238", RADIATION, pu238 * ingot, HOT, 3F * ingot);
        dict("blockPlutonium238", RADIATION, pu238 * block, HOT, 3F * block);
        dict("blockPu238", RADIATION, pu238 * block, HOT, 3F * block);
        dict("nuggetPlutonium239", RADIATION, pu239 * nugget);
        dict("nuggetPu239", RADIATION, pu239 * nugget);
        dict("tinyPlutonium239", RADIATION, pu239 * nugget);
        dict("tinyPu239", RADIATION, pu239 * nugget);
        dict("billetPlutonium239", RADIATION, pu239 * billet);
        dict("billetPu239", RADIATION, pu239 * billet);
        dict("ingotPlutonium239", RADIATION, pu239 * ingot);
        dict("ingotPu239", RADIATION, pu239 * ingot);
        dict("Plutonium239", RADIATION, pu239 * ingot);
        dict("Pu239", RADIATION, pu239 * ingot);
        dict("blockPlutonium239", RADIATION, pu239 * block);
        dict("blockPu239", RADIATION, pu239 * block);
        dict("nuggetPlutonium240", RADIATION, pu240 * nugget);
        dict("nuggetPu240", RADIATION, pu240 * nugget);
        dict("tinyPlutonium240", RADIATION, pu240 * nugget);
        dict("tinyPu240", RADIATION, pu240 * nugget);
        dict("billetPlutonium240", RADIATION, pu240 * billet);
        dict("billetPu240", RADIATION, pu240 * billet);
        dict("ingotPlutonium240", RADIATION, pu240 * ingot);
        dict("ingotPu240", RADIATION, pu240 * ingot);
        dict("Plutonium240", RADIATION, pu240 * ingot);
        dict("Pu240", RADIATION, pu240 * ingot);
        dict("blockPlutonium240", RADIATION, pu240 * block);
        dict("blockPu240", RADIATION, pu240 * block);
        dict("nuggetPlutonium241", RADIATION, pu241 * nugget);
        dict("nuggetPu241", RADIATION, pu241 * nugget);
        dict("tinyPlutonium241", RADIATION, pu241 * nugget);
        dict("tinyPu241", RADIATION, pu241 * nugget);
        dict("billetPlutonium241", RADIATION, pu241 * billet);
        dict("billetPu241", RADIATION, pu241 * billet);
        dict("ingotPlutonium241", RADIATION, pu241 * ingot);
        dict("ingotPu241", RADIATION, pu241 * ingot);
        dict("Plutonium241", RADIATION, pu241 * ingot);
        dict("Pu241", RADIATION, pu241 * ingot);
        dict("nuggetAmericium241", RADIATION, am241 * nugget);
        dict("nuggetAm241", RADIATION, am241 * nugget);
        dict("tinyAmericium241", RADIATION, am241 * nugget);
        dict("tinyAm241", RADIATION, am241 * nugget);
        dict("billetAmericium241", RADIATION, am241 * billet);
        dict("billetAm241", RADIATION, am241 * billet);
        dict("ingotAmericium241", RADIATION, am241 * ingot);
        dict("ingotAm241", RADIATION, am241 * ingot);
        dict("Americium241", RADIATION, am241 * ingot);
        dict("Am241", RADIATION, am241 * ingot);
        dict("nuggetAmericium242", RADIATION, am242 * nugget);
        dict("nuggetAm242", RADIATION, am242 * nugget);
        dict("tinyAmericium242", RADIATION, am242 * nugget);
        dict("tinyAm242", RADIATION, am242 * nugget);
        dict("billetAmericium242", RADIATION, am242 * billet);
        dict("billetAm242", RADIATION, am242 * billet);
        dict("ingotAmericium242", RADIATION, am242 * ingot);
        dict("ingotAm242", RADIATION, am242 * ingot);
        dict("Americium242", RADIATION, am242 * ingot);
        dict("Am242", RADIATION, am242 * ingot);
        dict("nuggetAmericiumRG", RADIATION, amrg * nugget);
        dict("tinyAmericiumRG", RADIATION, amrg * nugget);
        dict("billetAmericiumRG", RADIATION, amrg * billet);
        dict("ingotAmericiumRG", RADIATION, amrg * ingot);
        dict("AmericiumRG", RADIATION, amrg * ingot);
        dict("nuggetNeptunium237", RADIATION, np237 * nugget);
        dict("nuggetNp237", RADIATION, np237 * nugget);
        dict("nuggetNeptunium", RADIATION, np237 * nugget);
        dict("tinyNeptunium237", RADIATION, np237 * nugget);
        dict("tinyNp237", RADIATION, np237 * nugget);
        dict("tinyNeptunium", RADIATION, np237 * nugget);
        dict("billetNeptunium237", RADIATION, np237 * billet);
        dict("billetNp237", RADIATION, np237 * billet);
        dict("billetNeptunium", RADIATION, np237 * billet);
        dict("ingotNeptunium237", RADIATION, np237 * ingot);
        dict("ingotNp237", RADIATION, np237 * ingot);
        dict("ingotNeptunium", RADIATION, np237 * ingot);
        dict("Neptunium237", RADIATION, np237 * ingot);
        dict("Np237", RADIATION, np237 * ingot);
        dict("Neptunium", RADIATION, np237 * ingot);
        dict("dustNeptunium237", RADIATION, np237 * powder);
        dict("dustNp237", RADIATION, np237 * powder);
        dict("dustNeptunium", RADIATION, np237 * powder);
        dict("blockNeptunium237", RADIATION, np237 * block);
        dict("blockNp237", RADIATION, np237 * block);
        dict("blockNeptunium", RADIATION, np237 * block);
        dict("nuggetPolonium210", RADIATION, po210 * nugget, HOT, 3 * nugget);
        dict("nuggetPo210", RADIATION, po210 * nugget, HOT, 3 * nugget);
        dict("nuggetPolonium", RADIATION, po210 * nugget, HOT, 3 * nugget);
        dict("tinyPolonium210", RADIATION, po210 * nugget, HOT, 3 * nugget);
        dict("tinyPo210", RADIATION, po210 * nugget, HOT, 3 * nugget);
        dict("tinyPolonium", RADIATION, po210 * nugget, HOT, 3 * nugget);
        dict("billetPolonium210", RADIATION, po210 * billet, HOT, 3 * billet);
        dict("billetPo210", RADIATION, po210 * billet, HOT, 3 * billet);
        dict("billetPolonium", RADIATION, po210 * billet, HOT, 3 * billet);
        dict("ingotPolonium210", RADIATION, po210 * ingot, HOT, 3 * ingot);
        dict("ingotPo210", RADIATION, po210 * ingot, HOT, 3 * ingot);
        dict("ingotPolonium", RADIATION, po210 * ingot, HOT, 3 * ingot);
        dict("Polonium210", RADIATION, po210 * ingot, HOT, 3 * ingot);
        dict("Po210", RADIATION, po210 * ingot, HOT, 3 * ingot);
        dict("Polonium", RADIATION, po210 * ingot, HOT, 3 * ingot);
        dict("dustPolonium210", RADIATION, po210 * powder, HOT, 3 * powder);
        dict("dustPo210", RADIATION, po210 * powder, HOT, 3 * powder);
        dict("dustPolonium", RADIATION, po210 * powder, HOT, 3 * powder);
        dict("blockPolonium210", RADIATION, po210 * block, HOT, 3 * block);
        dict("blockPo210", RADIATION, po210 * block, HOT, 3 * block);
        dict("blockPolonium", RADIATION, po210 * block, HOT, 3 * block);
        dict("nuggetTechnetium99", RADIATION, tc99 * nugget);
        dict("nuggetTc99", RADIATION, tc99 * nugget);
        dict("tinyTechnetium99", RADIATION, tc99 * nugget);
        dict("tinyTc99", RADIATION, tc99 * nugget);
        dict("billetTechnetium99", RADIATION, tc99 * billet);
        dict("billetTc99", RADIATION, tc99 * billet);
        dict("ingotTechnetium99", RADIATION, tc99 * ingot);
        dict("ingotTc99", RADIATION, tc99 * ingot);
        dict("Technetium99", RADIATION, tc99 * ingot);
        dict("Tc99", RADIATION, tc99 * ingot);
        dict("nuggetRadium226", RADIATION, ra226 * nugget);
        dict("nuggetRa226", RADIATION, ra226 * nugget);
        dict("tinyRadium226", RADIATION, ra226 * nugget);
        dict("tinyRa226", RADIATION, ra226 * nugget);
        dict("billetRadium226", RADIATION, ra226 * billet);
        dict("billetRa226", RADIATION, ra226 * billet);
        dict("ingotRadium226", RADIATION, ra226 * ingot);
        dict("ingotRa226", RADIATION, ra226 * ingot);
        dict("Radium226", RADIATION, ra226 * ingot);
        dict("Ra226", RADIATION, ra226 * ingot);
        dict("dustRadium226", RADIATION, ra226 * powder);
        dict("dustRa226", RADIATION, ra226 * powder);
        dict("blockRadium226", RADIATION, ra226 * block);
        dict("blockRa226", RADIATION, ra226 * block);
        dict("nuggetActinium227", RADIATION, ac227 * nugget);
        dict("nuggetAc227", RADIATION, ac227 * nugget);
        dict("tinyActinium227", RADIATION, ac227 * nugget);
        dict("tinyAc227", RADIATION, ac227 * nugget);
        dict("billetActinium227", RADIATION, ac227 * billet);
        dict("billetAc227", RADIATION, ac227 * billet);
        dict("ingotActinium227", RADIATION, ac227 * ingot);
        dict("ingotAc227", RADIATION, ac227 * ingot);
        dict("Actinium227", RADIATION, ac227 * ingot);
        dict("Ac227", RADIATION, ac227 * ingot);
        dict("dustActinium227", RADIATION, ac227 * powder);
        dict("dustAc227", RADIATION, ac227 * powder);
        dict("blockActinium227", RADIATION, ac227 * block);
        dict("blockAc227", RADIATION, ac227 * block);
        dict("dustTinyActinium227", RADIATION, ac227 * powder_tiny);
        dict("dustTinyAc227", RADIATION, ac227 * powder_tiny);
        dict("nuggetCobalt60", RADIATION, co60 * nugget, HOT, 1 * nugget);
        dict("nuggetCo60", RADIATION, co60 * nugget, HOT, 1 * nugget);
        dict("tinyCobalt60", RADIATION, co60 * nugget, HOT, 1 * nugget);
        dict("tinyCo60", RADIATION, co60 * nugget, HOT, 1 * nugget);
        dict("billetCobalt60", RADIATION, co60 * billet, HOT, 1 * billet);
        dict("billetCo60", RADIATION, co60 * billet, HOT, 1 * billet);
        dict("ingotCobalt60", RADIATION, co60 * ingot, HOT, 1 * ingot);
        dict("ingotCo60", RADIATION, co60 * ingot, HOT, 1 * ingot);
        dict("Cobalt60", RADIATION, co60 * ingot, HOT, 1 * ingot);
        dict("Co60", RADIATION, co60 * ingot, HOT, 1 * ingot);
        dict("dustCobalt60", RADIATION, co60 * powder, HOT, 1 * powder);
        dict("dustCo60", RADIATION, co60 * powder, HOT, 1 * powder);
        dict("nuggetGold198", RADIATION, au198 * nugget, HOT, 5 * nugget);
        dict("nuggetAu198", RADIATION, au198 * nugget, HOT, 5 * nugget);
        dict("tinyGold198", RADIATION, au198 * nugget, HOT, 5 * nugget);
        dict("tinyAu198", RADIATION, au198 * nugget, HOT, 5 * nugget);
        dict("billetGold198", RADIATION, au198 * billet, HOT, 5 * billet);
        dict("billetAu198", RADIATION, au198 * billet, HOT, 5 * billet);
        dict("ingotGold198", RADIATION, au198 * ingot, HOT, 5 * ingot);
        dict("ingotAu198", RADIATION, au198 * ingot, HOT, 5 * ingot);
        dict("Gold198", RADIATION, au198 * ingot, HOT, 5 * ingot);
        dict("Au198", RADIATION, au198 * ingot, HOT, 5 * ingot);
        dict("dustGold198", RADIATION, au198 * powder, HOT, 5 * powder);
        dict("dustAu198", RADIATION, au198 * powder, HOT, 5 * powder);
        dict("nuggetLead209", RADIATION, pb209 * nugget, BLINDING, 50F * nugget, HOT, 7 * nugget);
        dict("nuggetPb209", RADIATION, pb209 * nugget, BLINDING, 50F * nugget, HOT, 7 * nugget);
        dict("tinyLead209", RADIATION, pb209 * nugget, BLINDING, 50F * nugget, HOT, 7 * nugget);
        dict("tinyPb209", RADIATION, pb209 * nugget, BLINDING, 50F * nugget, HOT, 7 * nugget);
        dict("billetLead209", RADIATION, pb209 * billet, BLINDING, 50F * billet, HOT, 7 * billet);
        dict("billetPb209", RADIATION, pb209 * billet, BLINDING, 50F * billet, HOT, 7 * billet);
        dict("ingotLead209", RADIATION, pb209 * ingot, BLINDING, 50F * ingot, HOT, 7 * ingot);
        dict("ingotPb209", RADIATION, pb209 * ingot, BLINDING, 50F * ingot, HOT, 7 * ingot);
        dict("Lead209", RADIATION, pb209 * ingot, BLINDING, 50F * ingot, HOT, 7 * ingot);
        dict("Pb209", RADIATION, pb209 * ingot, BLINDING, 50F * ingot, HOT, 7 * ingot);
        dict("nuggetSchrabidium", RADIATION, sa326 * nugget, BLINDING, 50F * nugget);
        dict("tinySchrabidium", RADIATION, sa326 * nugget, BLINDING, 50F * nugget);
        dict("billetSchrabidium", RADIATION, sa326 * billet, BLINDING, 50F * billet);
        dict("ingotSchrabidium", RADIATION, sa326 * ingot, BLINDING, 50F * ingot);
        dict("Schrabidium", RADIATION, sa326 * ingot, BLINDING, 50F * ingot);
        dict("dustSchrabidium", RADIATION, sa326 * powder, BLINDING, 50F * powder);
        dict("plateSchrabidium", RADIATION, sa326 * plate, BLINDING, 50F * plate);
        dict("plateTripleSchrabidium", RADIATION, sa326 * plateCast, BLINDING, 50F * plateCast);
        dict("blockSchrabidium", RADIATION, sa326 * block, BLINDING, 50F * block);
        dict("oreSchrabidium", RADIATION, sa326 * ore, BLINDING, 50F * ore);
        dict("oreNetherSchrabidium", RADIATION, sa326 * ore, BLINDING, 50F * ore);
        dict("nuggetSolinium", RADIATION, sa327 * nugget, BLINDING, 50F * nugget);
        dict("tinySolinium", RADIATION, sa327 * nugget, BLINDING, 50F * nugget);
        dict("billetSolinium", RADIATION, sa327 * billet, BLINDING, 50F * billet);
        dict("ingotSolinium", RADIATION, sa327 * ingot, BLINDING, 50F * ingot);
        dict("Solinium", RADIATION, sa327 * ingot, BLINDING, 50F * ingot);
        dict("blockSolinium", RADIATION, sa327 * block, BLINDING, 50F * block);
        dict("ingotSchrabidate", RADIATION, sb * ingot, BLINDING, 50F * ingot);
        dict("Schrabidate", RADIATION, sb * ingot, BLINDING, 50F * ingot);
        dict("dustSchrabidate", RADIATION, sb * powder, BLINDING, 50F * powder);
        dict("blockSchrabidate", RADIATION, sb * block, BLINDING, 50F * block);
        dict("ingotSchraranium", RADIATION, sr * ingot, BLINDING, 50F * ingot);
        dict("Schraranium", RADIATION, sr * ingot, BLINDING, 50F * ingot);
        dict("blockSchraranium", RADIATION, sr * block, BLINDING, 50F * block);
        dict("nuggetGhiorsium336", RADIATION, gh336 * nugget);
        dict("nuggetGh336", RADIATION, gh336 * nugget);
        dict("tinyGhiorsium336", RADIATION, gh336 * nugget);
        dict("tinyGh336", RADIATION, gh336 * nugget);
        dict("billetGhiorsium336", RADIATION, gh336 * billet);
        dict("billetGh336", RADIATION, gh336 * billet);
        dict("ingotGhiorsium336", RADIATION, gh336 * ingot);
        dict("ingotGh336", RADIATION, gh336 * ingot);
        dict("Ghiorsium336", RADIATION, gh336 * ingot);
        dict("Gh336", RADIATION, gh336 * ingot);
        dict("ingotWatzMud", RADIATION, mud * ingot);
        dict("WatzMud", RADIATION, mud * ingot);
        dict("ingotAsbestos", ASBESTOS, 1F * ingot);
        dict("Asbestos", ASBESTOS, 1F * ingot);
        dict("dustAsbestos", ASBESTOS, 1F * powder);
        dict("blockAsbestos", ASBESTOS, 1F * block);
        dict("oreAsbestos", ASBESTOS, 1F * ore);
        dict("oreNetherAsbestos", ASBESTOS, 1F * ore);
        dict("dustLignite", COAL, powder);
        dict("nuggetLithium", HYDROACTIVE, 1F * nugget);
        dict("ingotLithium", HYDROACTIVE, 1F * ingot);
        dict("Lithium", HYDROACTIVE, 1F * ingot);
        dict("dustTinyLithium", HYDROACTIVE, 1F * powder_tiny);
        dict("dustLithium", HYDROACTIVE, 1F * powder);
        dict("blockLithium", HYDROACTIVE, 1F * block);
        dict("oreLithium", HYDROACTIVE, 1F * ore);
        dict("oreNetherLithium", HYDROACTIVE, 1F * ore);
        dict("nuggetSodium", HYDROACTIVE, 1F * nugget);
        dict("ingotSodium", HYDROACTIVE, 1F * ingot);
        dict("dustSodium", HYDROACTIVE, 1F * powder);
        dict("ingotWhitePhosphorus", HOT, 5 * ingot);
        dict("WhitePhosphorus", HOT, 5 * ingot);
        dict("blockWhitePhosphorus", HOT, 5 * block);
        dict("dustStrontium", HOT, 1F * powder, HYDROACTIVE, 1F * powder);
        dict("ingotStrontium", HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        dict("nuggetStrontium", HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        dict("dustTinyStrontium90", RADIATION, sr90 * powder_tiny, HOT, 1F * powder_tiny, HYDROACTIVE, 1F * powder_tiny);
        dict("dustTinySr90", RADIATION, sr90 * powder_tiny, HOT, 1F * powder_tiny, HYDROACTIVE, 1F * powder_tiny);
        dict("dustStrontium90", RADIATION, sr90 * powder, HOT, 1F * powder, HYDROACTIVE, 1F * powder);
        dict("dustSr90", RADIATION, sr90 * powder, HOT, 1F * powder, HYDROACTIVE, 1F * powder);
        dict("ingotStrontium90", RADIATION, sr90 * ingot, HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        dict("ingotSr90", RADIATION, sr90 * ingot, HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        dict("Strontium90", RADIATION, sr90 * ingot, HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        dict("Sr90", RADIATION, sr90 * ingot, HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        dict("billetStrontium90", RADIATION, sr90 * billet, HOT, 1F * billet, HYDROACTIVE, 1F * billet);
        dict("billetSr90", RADIATION, sr90 * billet, HOT, 1F * billet, HYDROACTIVE, 1F * billet);
        dict("nuggetStrontium90", RADIATION, sr90 * nugget, HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        dict("nuggetSr90", RADIATION, sr90 * nugget, HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        dict("tinyStrontium90", RADIATION, sr90 * nugget, HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        dict("tinySr90", RADIATION, sr90 * nugget, HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        dict("dustTinyIodine131", RADIATION, i131 * powder_tiny, HOT, 1F * powder_tiny);
        dict("dustTinyI131", RADIATION, i131 * powder_tiny, HOT, 1F * powder_tiny);
        dict("dustIodine131", RADIATION, i131 * powder, HOT, 1F * powder);
        dict("dustI131", RADIATION, i131 * powder, HOT, 1F * powder);
        dict("dustTinyXenon135", RADIATION, xe135 * powder_tiny, HOT, 10F * powder_tiny);
        dict("dustTinyXe135", RADIATION, xe135 * powder_tiny, HOT, 10F * powder_tiny);
        dict("dustXenon135", RADIATION, xe135 * powder, HOT, 10F * powder);
        dict("dustXe135", RADIATION, xe135 * powder, HOT, 10F * powder);
        dict("dustTinyCaesium137", RADIATION, cs137 * powder_tiny, HOT, 3F * powder_tiny, HYDROACTIVE, 3F * powder_tiny);
        dict("dustTinyCs137", RADIATION, cs137 * powder_tiny, HOT, 3F * powder_tiny, HYDROACTIVE, 3F * powder_tiny);
        dict("dustCaesium137", RADIATION, cs137 * powder, HOT, 3F * powder, HYDROACTIVE, 3F * powder);
        dict("dustCs137", RADIATION, cs137 * powder, HOT, 3F * powder, HYDROACTIVE, 3F * powder);
        dict("dustAstatine209", RADIATION, at209 * powder, HOT, 20F * powder);
        dict("dustAt209", RADIATION, at209 * powder, HOT, 20F * powder);
        dict("dustTinyLignite", COAL, powder_tiny);
        ore("billet_actinium", "billetActinium227", RADIATION, ac227 * billet);
        ore("billet_am241", "billetAmericium241", RADIATION, am241 * billet);
        ore("billet_am242", "billetAmericium242", RADIATION, am242 * billet);
        ore("billet_am_mix", "billetAmericiumRG", RADIATION, amrg * billet);
        ore("billet_au198", "billetGold198", RADIATION, au198 * billet, HOT, 5 * billet);
        ore("billet_co60", "billetCobalt60", RADIATION, co60 * billet, HOT, 1 * billet);
        ore("billet_gh336", "billetGhiorsium336", RADIATION, gh336 * billet);
        ore("billet_neptunium", "billetNeptunium237", RADIATION, np237 * billet);
        ore("billet_pb209", "billetLead209", RADIATION, pb209 * billet, BLINDING, 50F * billet, HOT, 7 * billet);
        ore("billet_plutonium", "billetPlutonium", RADIATION, pu * billet);
        ore("billet_polonium", "billetPolonium210", RADIATION, po210 * billet, HOT, 3 * billet);
        ore("billet_pu238", "billetPlutonium238", RADIATION, pu238 * billet, HOT, 3F * billet);
        ore("billet_pu239", "billetPlutonium239", RADIATION, pu239 * billet);
        ore("billet_pu240", "billetPlutonium240", RADIATION, pu240 * billet);
        ore("billet_pu241", "billetPlutonium241", RADIATION, pu241 * billet);
        ore("billet_pu_mix", "billetPlutoniumRG", RADIATION, purg * billet);
        ore("billet_ra226", "billetRadium226", RADIATION, ra226 * billet);
        ore("billet_schrabidium", "billetSchrabidium", RADIATION, sa326 * billet, BLINDING, 50F * billet);
        ore("billet_solinium", "billetSolinium", RADIATION, sa327 * billet, BLINDING, 50F * billet);
        ore("billet_sr90", "billetStrontium90", RADIATION, sr90 * billet, HOT, 1F * billet, HYDROACTIVE, 1F * billet);
        ore("billet_technetium", "billetTechnetium99", RADIATION, tc99 * billet);
        ore("billet_th232", "billetThorium232", RADIATION, th232 * billet);
        ore("billet_u233", "billetUranium233", RADIATION, u233 * billet);
        ore("billet_u235", "billetUranium235", RADIATION, u235 * billet);
        ore("billet_u238", "billetUranium238", RADIATION, u238 * billet);
        ore("billet_uranium", "billetUranium", RADIATION, u * billet);
        ore("block_actinium", "blockActinium227", RADIATION, ac227 * block);
        ore("block_asbestos", "blockAsbestos", ASBESTOS, 1F * block);
        ore("block_lithium", "blockLithium", HYDROACTIVE, 1F * block);
        ore("block_neptunium", "blockNeptunium237", RADIATION, np237 * block);
        ore("block_plutonium", "blockPlutonium", RADIATION, pu * block);
        ore("block_polonium", "blockPolonium210", RADIATION, po210 * block, HOT, 3 * block);
        ore("block_pu238", "blockPlutonium238", RADIATION, pu238 * block, HOT, 3F * block);
        ore("block_pu239", "blockPlutonium239", RADIATION, pu239 * block);
        ore("block_pu240", "blockPlutonium240", RADIATION, pu240 * block);
        ore("block_pu_mix", "blockPlutoniumRG", RADIATION, purg * block);
        ore("block_ra226", "blockRadium226", RADIATION, ra226 * block);
        ore("block_schrabidate", "blockSchrabidate", RADIATION, sb * block, BLINDING, 50F * block);
        ore("block_schrabidium", "blockSchrabidium", RADIATION, sa326 * block, BLINDING, 50F * block);
        ore("block_schraranium", "blockSchraranium", RADIATION, sr * block, BLINDING, 50F * block);
        ore("block_solinium", "blockSolinium", RADIATION, sa327 * block, BLINDING, 50F * block);
        ore("block_thorium", "blockThorium232", RADIATION, th232 * block);
        ore("block_u233", "blockUranium233", RADIATION, u233 * block);
        ore("block_u235", "blockUranium235", RADIATION, u235 * block);
        ore("block_u238", "blockUranium238", RADIATION, u238 * block);
        ore("block_uranium", "blockUranium", RADIATION, u * block);
        ore("block_white_phosphorus", "blockWhitePhosphorus", HOT, 5 * block);
        ore("ingot_actinium", "ingotActinium227", RADIATION, ac227 * ingot);
        ore("ingot_am241", "ingotAmericium241", RADIATION, am241 * ingot);
        ore("ingot_am242", "ingotAmericium242", RADIATION, am242 * ingot);
        ore("ingot_am_mix", "ingotAmericiumRG", RADIATION, amrg * ingot);
        ore("ingot_asbestos", "ingotAsbestos", ASBESTOS, 1F * ingot);
        ore("ingot_au198", "ingotGold198", RADIATION, au198 * ingot, HOT, 5 * ingot);
        ore("ingot_co60", "ingotCobalt60", RADIATION, co60 * ingot, HOT, 1 * ingot);
        ore("ingot_gh336", "ingotGhiorsium336", RADIATION, gh336 * ingot);
        ore("ingot_mud", "ingotWatzMud", RADIATION, mud * ingot);
        ore("ingot_neptunium", "ingotNeptunium237", RADIATION, np237 * ingot);
        ore("ingot_pb209", "ingotLead209", RADIATION, pb209 * ingot, BLINDING, 50F * ingot, HOT, 7 * ingot);
        ore("ingot_phosphorus", "ingotWhitePhosphorus", HOT, 5 * ingot);
        ore("ingot_plutonium", "ingotPlutonium", RADIATION, pu * ingot);
        ore("ingot_polonium", "ingotPolonium210", RADIATION, po210 * ingot, HOT, 3 * ingot);
        ore("ingot_pu238", "ingotPlutonium238", RADIATION, pu238 * ingot, HOT, 3F * ingot);
        ore("ingot_pu239", "ingotPlutonium239", RADIATION, pu239 * ingot);
        ore("ingot_pu240", "ingotPlutonium240", RADIATION, pu240 * ingot);
        ore("ingot_pu241", "ingotPlutonium241", RADIATION, pu241 * ingot);
        ore("ingot_pu_mix", "ingotPlutoniumRG", RADIATION, purg * ingot);
        ore("ingot_ra226", "ingotRadium226", RADIATION, ra226 * ingot);
        ore("ingot_raw_sodium", "ingotSodium", HYDROACTIVE, 1F * ingot);
        ore("ingot_raw_strontium", "ingotStrontium", HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        // [parity d2: nugget_raw] LI/NA/SR .hazNugget() on ItemAutogen(NUGGET)
        ore("nugget_raw_lithium", "nuggetLithium", HYDROACTIVE, 1F * nugget);
        ore("nugget_raw_sodium", "nuggetSodium", HYDROACTIVE, 1F * nugget);
        ore("nugget_raw_strontium", "nuggetStrontium", HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        ore("ingot_schrabidate", "ingotSchrabidate", RADIATION, sb * ingot, BLINDING, 50F * ingot);
        ore("ingot_schrabidium", "ingotSchrabidium", RADIATION, sa326 * ingot, BLINDING, 50F * ingot);
        ore("ingot_schraranium", "ingotSchraranium", RADIATION, sr * ingot, BLINDING, 50F * ingot);
        ore("ingot_solinium", "ingotSolinium", RADIATION, sa327 * ingot, BLINDING, 50F * ingot);
        ore("ingot_sr90", "ingotStrontium90", RADIATION, sr90 * ingot, HOT, 1F * ingot, HYDROACTIVE, 1F * ingot);
        ore("ingot_technetium", "ingotTechnetium99", RADIATION, tc99 * ingot);
        ore("ingot_th232", "ingotThorium232", RADIATION, th232 * ingot);
        ore("ingot_u233", "ingotUranium233", RADIATION, u233 * ingot);
        ore("ingot_u235", "ingotUranium235", RADIATION, u235 * ingot);
        ore("ingot_u238", "ingotUranium238", RADIATION, u238 * ingot);
        ore("ingot_uranium", "ingotUranium", RADIATION, u * ingot);
        ore("lithium", "ingotLithium", HYDROACTIVE, 1F * ingot);
        ore("nugget_actinium", "nuggetActinium227", RADIATION, ac227 * nugget);
        ore("nugget_am241", "nuggetAmericium241", RADIATION, am241 * nugget);
        ore("nugget_am242", "nuggetAmericium242", RADIATION, am242 * nugget);
        ore("nugget_am_mix", "nuggetAmericiumRG", RADIATION, amrg * nugget);
        ore("nugget_au198", "nuggetGold198", RADIATION, au198 * nugget, HOT, 5 * nugget);
        ore("nugget_co60", "nuggetCobalt60", RADIATION, co60 * nugget, HOT, 1 * nugget);
        ore("nugget_gh336", "nuggetGhiorsium336", RADIATION, gh336 * nugget);
        ore("nugget_neptunium", "nuggetNeptunium237", RADIATION, np237 * nugget);
        ore("nugget_pb209", "nuggetLead209", RADIATION, pb209 * nugget, BLINDING, 50F * nugget, HOT, 7 * nugget);
        ore("nugget_plutonium", "nuggetPlutonium", RADIATION, pu * nugget);
        ore("nugget_polonium", "nuggetPolonium210", RADIATION, po210 * nugget, HOT, 3 * nugget);
        ore("nugget_pu238", "nuggetPlutonium238", RADIATION, pu238 * nugget, HOT, 3F * nugget);
        ore("nugget_pu239", "nuggetPlutonium239", RADIATION, pu239 * nugget);
        ore("nugget_pu240", "nuggetPlutonium240", RADIATION, pu240 * nugget);
        ore("nugget_pu241", "nuggetPlutonium241", RADIATION, pu241 * nugget);
        ore("nugget_pu_mix", "nuggetPlutoniumRG", RADIATION, purg * nugget);
        ore("nugget_ra226", "nuggetRadium226", RADIATION, ra226 * nugget);
        ore("nugget_schrabidium", "nuggetSchrabidium", RADIATION, sa326 * nugget, BLINDING, 50F * nugget);
        ore("nugget_solinium", "nuggetSolinium", RADIATION, sa327 * nugget, BLINDING, 50F * nugget);
        ore("nugget_sr90", "nuggetStrontium90", RADIATION, sr90 * nugget, HOT, 1F * nugget, HYDROACTIVE, 1F * nugget);
        ore("nugget_technetium", "nuggetTechnetium99", RADIATION, tc99 * nugget);
        ore("nugget_th232", "nuggetThorium232", RADIATION, th232 * nugget);
        ore("nugget_u233", "nuggetUranium233", RADIATION, u233 * nugget);
        ore("nugget_u235", "nuggetUranium235", RADIATION, u235 * nugget);
        ore("nugget_u238", "nuggetUranium238", RADIATION, u238 * nugget);
        ore("nugget_uranium", "nuggetUranium", RADIATION, u * nugget);
        ore("ore_asbestos", "oreAsbestos", ASBESTOS, 1F * ore);
        ore("ore_basalt#asbestos", "oreAsbestos", ASBESTOS, 1F * ore);
        ore("ore_enriched#asbestos", "oreAsbestos", ASBESTOS, 1F * ore);
        ore("ore_gneiss_asbestos", "oreAsbestos", ASBESTOS, 1F * ore);
        ore("ore_gneiss_lithium", "oreLithium", HYDROACTIVE, 1F * ore);
        ore("ore_gneiss_schrabidium", "oreSchrabidium", RADIATION, sa326 * ore, BLINDING, 50F * ore);
        ore("ore_nether_plutonium", "orePlutonium", RADIATION, pu * ore);
        ore("ore_nether_schrabidium", "oreSchrabidium", RADIATION, sa326 * ore, BLINDING, 50F * ore);
        ore("ore_schrabidium", "oreSchrabidium", RADIATION, sa326 * ore, BLINDING, 50F * ore);
        ore("ore_sellafield_schrabidium", "oreSchrabidium", RADIATION, sa326 * ore, BLINDING, 50F * ore);
        ore("plate_cast_schrabidium", "plateTripleSchrabidium", RADIATION, sa326 * plateCast, BLINDING, 50F * plateCast);
        ore("plate_schrabidium", "plateSchrabidium", RADIATION, sa326 * plate, BLINDING, 50F * plate);
        ore("powder_actinium", "dustActinium227", RADIATION, ac227 * powder);
        ore("powder_actinium_tiny", "dustTinyActinium227", RADIATION, ac227 * powder_tiny);
        ore("powder_asbestos", "dustAsbestos", ASBESTOS, 1F * powder);
        ore("powder_at209", "dustAstatine209", RADIATION, at209 * powder, HOT, 20F * powder);
        ore("powder_au198", "dustGold198", RADIATION, au198 * powder, HOT, 5 * powder);
        ore("powder_co60", "dustCobalt60", RADIATION, co60 * powder, HOT, 1 * powder);
        ore("powder_coal", "dustCoal", COAL, powder);
        ore("powder_coal_tiny", "dustTinyCoal", COAL, powder_tiny);
        ore("powder_cs137", "dustCaesium137", RADIATION, cs137 * powder, HOT, 3F * powder, HYDROACTIVE, 3F * powder);
        ore("powder_cs137_tiny", "dustTinyCaesium137", RADIATION, cs137 * powder_tiny, HOT, 3F * powder_tiny, HYDROACTIVE, 3F * powder_tiny);
        ore("powder_i131", "dustIodine131", RADIATION, i131 * powder, HOT, 1F * powder);
        ore("powder_i131_tiny", "dustTinyIodine131", RADIATION, i131 * powder_tiny, HOT, 1F * powder_tiny);
        ore("powder_lignite", "dustLignite", COAL, powder);
        ore("powder_lithium", "dustLithium", HYDROACTIVE, 1F * powder);
        ore("powder_lithium_tiny", "dustTinyLithium", HYDROACTIVE, 1F * powder_tiny);
        ore("powder_neptunium", "dustNeptunium237", RADIATION, np237 * powder);
        ore("powder_plutonium", "dustPlutonium", RADIATION, pu * powder);
        ore("powder_polonium", "dustPolonium210", RADIATION, po210 * powder, HOT, 3 * powder);
        ore("powder_ra226", "dustRadium226", RADIATION, ra226 * powder);
        ore("powder_schrabidate", "dustSchrabidate", RADIATION, sb * powder, BLINDING, 50F * powder);
        ore("powder_schrabidium", "dustSchrabidium", RADIATION, sa326 * powder, BLINDING, 50F * powder);
        ore("powder_sodium", "dustSodium", HYDROACTIVE, 1F * powder);
        ore("powder_sr90", "dustStrontium90", RADIATION, sr90 * powder, HOT, 1F * powder, HYDROACTIVE, 1F * powder);
        ore("powder_sr90_tiny", "dustTinyStrontium90", RADIATION, sr90 * powder_tiny, HOT, 1F * powder_tiny, HYDROACTIVE, 1F * powder_tiny);
        ore("powder_strontium", "dustStrontium", HOT, 1F * powder, HYDROACTIVE, 1F * powder);
        ore("powder_thorium", "dustThorium232", RADIATION, th232 * powder);
        ore("powder_uranium", "dustUranium", RADIATION, u * powder);
        ore("powder_xe135", "dustXenon135", RADIATION, xe135 * powder, HOT, 10F * powder);
        ore("powder_xe135_tiny", "dustTinyXenon135", RADIATION, xe135 * powder_tiny, HOT, 10F * powder_tiny);
        ore("stone_resource_asbestos", "oreAsbestos", ASBESTOS, 1F * ore);
    }

    // ---------------------------------------------------------------- generated: HazardRegistry.registerItems()

    private static void registerItems() {
        register("minecraft:gunpowder", makeData(EXPLOSIVE, 1F));
        register("minecraft:tnt", makeData(EXPLOSIVE, 4F));
        register("minecraft:pumpkin_pie", makeData(EXPLOSIVE, 1F));
        register("ball_dynamite", makeData(EXPLOSIVE, 2F));
        register("stick_dynamite", makeData(EXPLOSIVE, 1F));
        register("stick_tnt", makeData(EXPLOSIVE, 1.5F));
        register("stick_semtex", makeData(EXPLOSIVE, 2.5F));
        register("stick_c4", makeData(EXPLOSIVE, 2.5F));
        register("cordite", makeData(EXPLOSIVE, 2F));
        register("ballistite", makeData(EXPLOSIVE, 1F));
        register("insert_polonium", makeData(RADIATION, 100F));
        register("demon_core_open", makeData(RADIATION, 5F));
        register("demon_core_closed", makeData(RADIATION, 100_000F));
        register("lamp_demon", makeData(RADIATION, 100_000F));
        register("cell_tritium", makeData(RADIATION, 0.001F));
        register("cell_sas3", makeData().addEntry(RADIATION, sas3).addEntry(BLINDING, 60F));
        register("cell_balefire", makeData(RADIATION, 50F));
        register("powder_balefire", makeData(RADIATION, 500F));
        register("egg_balefire_shard", makeData(RADIATION, bf * nugget));
        register("egg_balefire", makeData(RADIATION, bf * ingot));
        register("solid_fuel_bf", makeData(RADIATION, 1000));
        register("solid_fuel_presto_bf", makeData(RADIATION, 2000));
        register("solid_fuel_presto_triplet_bf", makeData(RADIATION, 6000));
        register("nuclear_waste_long", makeData(RADIATION, 5F));
        register("nuclear_waste_long_tiny", makeData(RADIATION, 0.5F));
        register("nuclear_waste_short", makeData().addEntry(RADIATION, 30F).addEntry(HOT, 5F));
        register("nuclear_waste_short_tiny", makeData().addEntry(RADIATION, 3F).addEntry(HOT, 5F));
        register("nuclear_waste_long_depleted", makeData(RADIATION, 0.5F));
        register("nuclear_waste_long_depleted_tiny", makeData(RADIATION, 0.05F));
        register("nuclear_waste_short_depleted", makeData(RADIATION, 3F));
        register("nuclear_waste_short_depleted_tiny", makeData(RADIATION, 0.3F));
        register("scrap_nuclear", makeData(RADIATION, 1F));
        register("trinitite", makeData(RADIATION, trn * ingot));
        register("block_trinitite", makeData(RADIATION, trn * block));
        register("nuclear_waste", makeData(RADIATION, wst * ingot));
        register("yellow_barrel", makeData(RADIATION, wst * ingot * 10));
        register("billet_nuclear_waste", makeData(RADIATION, wst * billet));
        register("nuclear_waste_tiny", makeData(RADIATION, wst * nugget));
        register("nuclear_waste_vitrified", makeData(RADIATION, wstv * ingot));
        register("nuclear_waste_vitrified_tiny", makeData(RADIATION, wstv * nugget));
        register("block_waste", makeData(RADIATION, wst * block));
        register("block_waste_painted", makeData(RADIATION, wst * block));
        register("block_waste_vitrified", makeData(RADIATION, wstv * block));
        register("ancient_scrap", makeData(RADIATION, 150F));
        register("block_corium", makeData(RADIATION, 150F));
        register("block_corium_cobble", makeData(RADIATION, 150F));
        register("sellafield#0", makeData(RADIATION, 0.5F));
        register("sellafield#1", makeData(RADIATION, 1F));
        register("sellafield#2", makeData(RADIATION, 2.5F));
        register("sellafield#3", makeData(RADIATION, 4F));
        register("sellafield#4", makeData(RADIATION, 5F));
        register("sellafield#5", makeData(RADIATION, 10F));
        register("ore_sellafield_radgem", makeData(RADIATION, 25F));
        register("gem_rad", makeData(RADIATION, 25F));
        registerOtherFuel("rod_zirnox#natural_uranium_fuel", u * rod_dual, wst * rod_dual * 11.5F, false);
        registerOtherFuel("rod_zirnox#uranium_fuel", uf * rod_dual, wst * rod_dual * 10F, false);
        registerOtherFuel("rod_zirnox#th232_fuel", th232 * rod_dual, thf * rod_dual, false);
        registerOtherFuel("rod_zirnox#thorium_fuel", thf * rod_dual, wst * rod_dual * 7.5F, false);
        registerOtherFuel("rod_zirnox#mox_fuel", mox * rod_dual, wst * rod_dual * 10F, false);
        registerOtherFuel("rod_zirnox#plutonium_fuel", puf * rod_dual, wst * rod_dual * 12.5F, false);
        registerOtherFuel("rod_zirnox#u233_fuel", u233 * rod_dual, wst * rod_dual * 10F, false);
        registerOtherFuel("rod_zirnox#u235_fuel", u235 * rod_dual, wst * rod_dual * 11F, false);
        registerOtherFuel("rod_zirnox#les_fuel", saf * rod_dual, wst * rod_dual * 15F, false);
        registerOtherFuel("rod_zirnox#lithium_fuel", 0, 0.001F * rod_dual, false);
        registerOtherFuel("rod_zirnox#zfb_mox_fuel", mox * rod_dual, wst * rod_dual * 5F, false);
        register("rod_zirnox_natural_uranium_fuel_depleted", makeData(RADIATION, wst * rod_dual * 11.5F));
        register("rod_zirnox_uranium_fuel_depleted", makeData(RADIATION, wst * rod_dual * 10F));
        register("rod_zirnox_thorium_fuel_depleted", makeData(RADIATION, wst * rod_dual * 7.5F));
        register("rod_zirnox_mox_fuel_depleted", makeData(RADIATION, wst * rod_dual * 10F));
        register("rod_zirnox_plutonium_fuel_depleted", makeData(RADIATION, wst * rod_dual * 12.5F));
        register("rod_zirnox_u233_fuel_depleted", makeData(RADIATION, wst * rod_dual * 10F));
        register("rod_zirnox_u235_fuel_depleted", makeData(RADIATION, wst * rod_dual * 11F));
        register("rod_zirnox_les_fuel_depleted", makeData().addEntry(RADIATION, wst * rod_dual * 15F).addEntry(BLINDING, 20F));
        register("rod_zirnox_tritium", makeData(RADIATION, 0.001F * rod_dual));
        register("rod_zirnox_zfb_mox_depleted", makeData(RADIATION, wst * rod_dual * 5F));
        registerOtherWaste("waste_natural_uranium", wst * billet * 11.5F);
        registerOtherWaste("waste_uranium", wst * billet * 10F);
        registerOtherWaste("waste_thorium", wst * billet * 7.5F);
        registerOtherWaste("waste_mox", wst * billet * 10F);
        registerOtherWaste("waste_plutonium", wst * billet * 12.5F);
        registerOtherWaste("waste_u233", wst * billet * 10F);
        registerOtherWaste("waste_u235", wst * billet * 11F);
        registerOtherWaste("waste_schrabidium", wst * billet * 15F);
        registerOtherWaste("waste_zfb_mox", wst * billet * 5F);
        registerOtherFuel("plate_fuel_u233", u233 * ingot, wst * ingot * 13F, false);
        registerOtherFuel("plate_fuel_u235", u235 * ingot, wst * ingot * 10F, false);
        registerOtherFuel("plate_fuel_mox", mox * ingot, wst * ingot * 16F, false);
        registerOtherFuel("plate_fuel_pu239", pu239 * ingot, wst * ingot * 13.5F, false);
        registerOtherFuel("plate_fuel_sa326", sa326 * ingot, wst * ingot * 10F, true);
        registerOtherFuel("plate_fuel_ra226be", rabe * billet, pobe * nugget * 3, false);
        registerOtherFuel("plate_fuel_pu238be", pube * billet, pube * nugget * 1, false);
        registerOtherWaste("waste_plate_u233", wst * ingot * 13F);
        registerOtherWaste("waste_plate_u235", wst * ingot * 10F);
        registerOtherWaste("waste_plate_mox", wst * ingot * 16F);
        registerOtherWaste("waste_plate_pu239", wst * ingot * 13.5F);
        registerOtherWaste("waste_plate_sa326", wst * ingot * 10F);
        registerRadSourceWaste("waste_plate_ra226be", pobe * nugget * 3);
        registerRadSourceWaste("waste_plate_pu238be", pube * nugget * 1);
        register("debris_graphite", makeData().addEntry(RADIATION, 70F).addEntry(HOT, 5F));
        register("debris_metal", makeData(RADIATION, 5F));
        register("debris_fuel", makeData().addEntry(RADIATION, 500F).addEntry(HOT, 5F));
        register("debris_concrete", makeData(RADIATION, 30F));
        register("debris_exchanger", makeData(RADIATION, 25F));
        register("debris_shrapnel", makeData(RADIATION, 2.5F));
        register("debris_element", makeData(RADIATION, 100F));
        register("nugget_uranium_fuel", makeData(RADIATION, uf * nugget));
        register("billet_uranium_fuel", makeData(RADIATION, uf * billet));
        register("ingot_uranium_fuel", makeData(RADIATION, uf * ingot));
        register("block_uranium_fuel", makeData(RADIATION, uf * block));
        register("billet_uzh", makeData(RADIATION, uzh * billet));
        register("nugget_plutonium_fuel", makeData(RADIATION, puf * nugget));
        register("billet_plutonium_fuel", makeData(RADIATION, puf * billet));
        register("ingot_plutonium_fuel", makeData(RADIATION, puf * ingot));
        register("block_plutonium_fuel", makeData(RADIATION, puf * block));
        register("nugget_thorium_fuel", makeData(RADIATION, thf * nugget));
        register("billet_thorium_fuel", makeData(RADIATION, thf * billet));
        register("ingot_thorium_fuel", makeData(RADIATION, thf * ingot));
        register("block_thorium_fuel", makeData(RADIATION, thf * block));
        register("nugget_neptunium_fuel", makeData(RADIATION, npf * nugget));
        register("billet_neptunium_fuel", makeData(RADIATION, npf * billet));
        register("ingot_neptunium_fuel", makeData(RADIATION, npf * ingot));
        register("nugget_mox_fuel", makeData(RADIATION, mox * nugget));
        register("billet_mox_fuel", makeData(RADIATION, mox * billet));
        register("ingot_mox_fuel", makeData(RADIATION, mox * ingot));
        register("block_mox_fuel", makeData(RADIATION, mox * block));
        register("nugget_americium_fuel", makeData(RADIATION, amf * nugget));
        register("billet_americium_fuel", makeData(RADIATION, amf * billet));
        register("ingot_americium_fuel", makeData(RADIATION, amf * ingot));
        register("nugget_schrabidium_fuel", makeData().addEntry(RADIATION, saf * nugget).addEntry(BLINDING, 5F * nugget));
        register("billet_schrabidium_fuel", makeData().addEntry(RADIATION, saf * billet).addEntry(BLINDING, 5F * billet));
        register("ingot_schrabidium_fuel", makeData().addEntry(RADIATION, saf * ingot).addEntry(BLINDING, 5F * ingot));
        register("block_schrabidium_fuel", makeData().addEntry(RADIATION, saf * block).addEntry(BLINDING, 5F * block));
        register("nugget_hes", makeData(RADIATION, saf * nugget));
        register("billet_hes", makeData(RADIATION, saf * billet));
        register("ingot_hes", makeData(RADIATION, saf * ingot));
        register("nugget_les", makeData(RADIATION, saf * nugget));
        register("billet_les", makeData(RADIATION, saf * billet));
        register("ingot_les", makeData(RADIATION, saf * ingot));
        register("billet_balefire_gold", makeData(RADIATION, au198 * billet));
        register("billet_flashlead", makeData().addEntry(RADIATION, pb209 * 1.25F * billet).addEntry(HOT, 7F));
        register("billet_po210be", makeData(RADIATION, pobe * billet));
        register("billet_ra226be", makeData(RADIATION, rabe * billet));
        register("billet_pu238be", makeData(RADIATION, pube * billet));
        registerRTGPellet("pellet_rtg", pu238 * rtg, 0, 3F);
        registerRTGPellet("pellet_rtg_radium", ra226 * rtg, 0);
        registerRTGPellet("pellet_rtg_weak", (pu238 + (u238 * 2)) * billet, 0);
        registerRTGPellet("pellet_rtg_strontium", sr90 * rtg, 0);
        registerRTGPellet("pellet_rtg_cobalt", co60 * rtg, 0);
        registerRTGPellet("pellet_rtg_actinium", ac227 * rtg, 0);
        registerRTGPellet("pellet_rtg_polonium", po210 * rtg, 0, 3F);
        registerRTGPellet("pellet_rtg_lead", pb209 * rtg, 0, 7F, 50F);
        registerRTGPellet("pellet_rtg_gold", au198 * rtg, 0, 5F);
        registerRTGPellet("pellet_rtg_americium", am241 * rtg, 0);
        register("pellet_rtg_depleted#neptunium", makeData(RADIATION, np237 * rtg));
        register("pile_rod_ra226be", makeData(RADIATION, rabe * billet * 3));
        register("pile_rod_po210be", makeData(RADIATION, pobe * billet * 3));
        register("pile_rod_nu", makeData(RADIATION, u * billet * 3));
        register("pile_rod_pu239_graphite", makeData(RADIATION, pu239 * billet * 3));
        register("pile_rod_rgp", makeData(RADIATION, purg * billet * 3));
        register("pile_rod_waste", makeData(RADIATION, wst * billet * 3));
        register("pile_rod_thorium", makeData(RADIATION, th232 * billet * 3));
        register("pile_rod_thorium_fuel", makeData(RADIATION, thf * billet * 3));
        register("pile_rod_uranium", makeData(RADIATION, u * billet * 3));
        registerPileRods528(enable528());
        register("pile_rod_source", makeData(RADIATION, rabe * billet * 3));
        registerBreedingRodRadiation("tritium", 0.001F);
        registerBreedingRodRadiation("co60", co60);
        registerBreedingRodRadiation("ra226", ra226);
        registerBreedingRodRadiation("ac227", ac227);
        registerBreedingRodRadiation("th232", th232);
        registerBreedingRodRadiation("thf", thf);
        registerBreedingRodRadiation("u235", u235);
        registerBreedingRodRadiation("np237", np237);
        registerBreedingRodRadiation("u238", u238);
        registerBreedingRodRadiation("pu238", pu238);
        registerBreedingRodRadiation("pu239", pu239);
        registerBreedingRodRadiation("rgp", purg);
        registerBreedingRodRadiation("waste", wst);
        registerBreedingRodRadiation("uranium", u);
        registerRBMKRod("rbmk_fuel_ueu", u * rod_rbmk, wst * rod_rbmk * 20F);
        registerRBMKRod("rbmk_fuel_meu", uf * rod_rbmk, wst * rod_rbmk * 21.5F);
        registerRBMKRod("rbmk_fuel_heu233", u233 * rod_rbmk, wst * rod_rbmk * 31F);
        registerRBMKRod("rbmk_fuel_heu235", u235 * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_uzh", uzh * rod_rbmk, wst * rod_rbmk * 20F);
        registerRBMKRod("rbmk_fuel_thmeu", thf * rod_rbmk, wst * rod_rbmk * 17.5F);
        registerRBMKRod("rbmk_fuel_lep", puf * rod_rbmk, wst * rod_rbmk * 25F);
        registerRBMKRod("rbmk_fuel_mep", purg * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_hep", pu239 * rod_rbmk, wst * rod_rbmk * 32.5F);
        registerRBMKRod("rbmk_fuel_hep241", pu241 * rod_rbmk, wst * rod_rbmk * 35F);
        registerRBMKRod("rbmk_fuel_lea", amf * rod_rbmk, wst * rod_rbmk * 26F);
        registerRBMKRod("rbmk_fuel_mea", amrg * rod_rbmk, wst * rod_rbmk * 30.5F);
        registerRBMKRod("rbmk_fuel_hea241", am241 * rod_rbmk, wst * rod_rbmk * 33.5F);
        registerRBMKRod("rbmk_fuel_hea242", am242 * rod_rbmk, wst * rod_rbmk * 34F);
        registerRBMKRod("rbmk_fuel_men", npf * rod_rbmk, wst * rod_rbmk * 22.5F);
        registerRBMKRod("rbmk_fuel_hen", np237 * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_mox", mox * rod_rbmk, wst * rod_rbmk * 25.5F);
        registerRBMKRod("rbmk_fuel_les", saf * rod_rbmk, wst * rod_rbmk * 24.5F);
        registerRBMKRod("rbmk_fuel_mes", saf * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_hes", saf * rod_rbmk, wst * rod_rbmk * 50F);
        registerRBMKRod("rbmk_fuel_leaus", 0F, wst * rod_rbmk * 37.5F);
        registerRBMKRod("rbmk_fuel_heaus", 0F, wst * rod_rbmk * 32.5F);
        registerRBMKRod("rbmk_fuel_po210be", pobe * rod_rbmk, pobe * rod_rbmk * 0.1F, true);
        registerRBMKRod("rbmk_fuel_ra226be", rabe * rod_rbmk, rabe * rod_rbmk * 0.4F, true);
        registerRBMKRod("rbmk_fuel_pu238be", pube * rod_rbmk, wst * rod_rbmk * 2.5F);
        registerRBMKRod("rbmk_fuel_balefire_gold", au198 * rod_rbmk, bf * rod_rbmk * 0.5F, true);
        registerRBMKRod("rbmk_fuel_flashlead", pb209 * 1.25F * rod_rbmk, pb209 * nugget * 0.05F * rod_rbmk, true);
        registerRBMKRod("rbmk_fuel_balefire", bf * rod_rbmk, bf * rod_rbmk * 100F, true);
        registerRBMKRod("rbmk_fuel_zfb_bismuth", pu241 * rod_rbmk * 0.1F, wst * rod_rbmk * 5F);
        registerRBMKRod("rbmk_fuel_zfb_pu241", pu239 * rod_rbmk * 0.1F, wst * rod_rbmk * 7.5F);
        registerRBMKRod("rbmk_fuel_zfb_am_mix", pu241 * rod_rbmk * 0.1F, wst * rod_rbmk * 10F);
        registerRBMK("rbmk_fuel_drx", bf * rod_rbmk, bf * rod_rbmk * 100F, true, true, 0, 1F/3F);
        registerRBMKPellet("rbmk_pellet_ueu", u * billet, wst * billet * 20F);
        registerRBMKPellet("rbmk_pellet_meu", uf * billet, wst * billet * 21.5F);
        registerRBMKPellet("rbmk_pellet_heu233", u233 * billet, wst * billet * 31F);
        registerRBMKPellet("rbmk_pellet_heu235", u235 * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_uzh", uzh * billet, wst * billet * 20F);
        registerRBMKPellet("rbmk_pellet_thmeu", thf * billet, wst * billet * 17.5F);
        registerRBMKPellet("rbmk_pellet_lep", puf * billet, wst * billet * 25F);
        registerRBMKPellet("rbmk_pellet_mep", purg * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_hep239", pu239 * billet, wst * billet * 32.5F);
        registerRBMKPellet("rbmk_pellet_hep241", pu241 * billet, wst * billet * 35F);
        registerRBMKPellet("rbmk_pellet_lea", amf * billet, wst * billet * 26F);
        registerRBMKPellet("rbmk_pellet_mea", amrg * billet, wst * billet * 30.5F);
        registerRBMKPellet("rbmk_pellet_hea241", am241 * billet, wst * billet * 33.5F);
        registerRBMKPellet("rbmk_pellet_hea242", am242 * billet, wst * billet * 34F);
        registerRBMKPellet("rbmk_pellet_men", npf * billet, wst * billet * 22.5F);
        registerRBMKPellet("rbmk_pellet_hen", np237 * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_mox", mox * billet, wst * billet * 25.5F);
        registerRBMKPellet("rbmk_pellet_les", saf * billet, wst * billet * 24.5F);
        registerRBMKPellet("rbmk_pellet_mes", saf * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_hes", saf * billet, wst * billet * 50F);
        registerRBMKPellet("rbmk_pellet_leaus", 0F, wst * billet * 37.5F);
        registerRBMKPellet("rbmk_pellet_heaus", 0F, wst * billet * 32.5F);
        registerRBMKPellet("rbmk_pellet_po210be", pobe * billet, pobe * billet * 0.1F, true);
        registerRBMKPellet("rbmk_pellet_ra226be", rabe * billet, rabe * billet * 0.4F, true);
        registerRBMKPellet("rbmk_pellet_pu238be", pube * billet, wst * 1.5F);
        registerRBMKPellet("rbmk_pellet_balefire_gold", au198 * billet, bf * billet * 0.5F, true);
        registerRBMKPellet("rbmk_pellet_flashlead", pb209 * 1.25F * billet, pb209 * nugget * 0.05F, true);
        registerRBMKPellet("rbmk_pellet_balefire", bf * billet, bf * billet * 100F, true);
        registerRBMKPellet("rbmk_pellet_zfb_bismuth", pu241 * billet * 0.1F, wst * billet * 5F);
        registerRBMKPellet("rbmk_pellet_zfb_pu241", pu239 * billet * 0.1F, wst * billet * 7.5F);
        registerRBMKPellet("rbmk_pellet_zfb_am_mix", pu241 * billet * 0.1F, wst * billet * 10F);
        registerRBMKPellet("rbmk_pellet_drx", bf * billet, bf * billet * 100F, true, 0F, 1F/24F);
        register("watz_pellet#schrabidium", makeData(RADIATION, sa326 * ingot * 4));
        register("watz_pellet#hes", makeData(RADIATION, saf * ingot * 4));
        register("watz_pellet#mes", makeData(RADIATION, saf * ingot * 4));
        register("watz_pellet#les", makeData(RADIATION, saf * ingot * 4));
        register("watz_pellet#hen", makeData(RADIATION, np237 * ingot * 4));
        register("watz_pellet#meu", makeData(RADIATION, uf * ingot * 4));
        register("watz_pellet#mep", makeData(RADIATION, purg * ingot * 4));
        register("watz_pellet#du", makeData(RADIATION, u238 * ingot * 4));
        register("watz_pellet#nqd", makeData(RADIATION, u235 * ingot * 4));
        register("watz_pellet#nqr", makeData(RADIATION, pu239 * ingot * 4));
        registerPWRFuel("meu", uf * billet * 2);
        registerPWRFuel("heu233", u233 * billet * 2);
        registerPWRFuel("heu235", u235 * billet * 2);
        registerPWRFuel("men", npf * billet * 2);
        registerPWRFuel("hen237", np237 * billet * 2);
        registerPWRFuel("mox", mox * billet * 2);
        registerPWRFuel("mep", purg * billet * 2);
        registerPWRFuel("hep239", pu239 * billet * 2);
        registerPWRFuel("hep241", pu241 * billet * 2);
        registerPWRFuel("mea", amrg * billet * 2);
        registerPWRFuel("hea242", am242 * billet * 2);
        registerPWRFuel("hes326", sa326 * billet * 2);
        registerPWRFuel("hes327", sa327 * billet * 2);
        registerPWRFuel("bfb_am_mix", amrg * billet);
        registerPWRFuel("bfb_pu241", pu241 * billet);
        register("powder_yellowcake", makeData(RADIATION, yc * powder));
        register("block_yellowcake", makeData(RADIATION, yc * block * powder_mult));
        register("falloutitem", makeData(RADIATION, fo * powder));
        register("fallout", makeData(RADIATION, fo * powder * 2));
        register("block_fallout", makeData(RADIATION, yc * block * powder_mult));
        register("powder_caesium", makeData().addEntry(HYDROACTIVE, 1F).addEntry(HOT, 3F));
        register("brick_asbestos", makeData(ASBESTOS, 1F));
        register("tile_lab_broken", makeData(ASBESTOS, 1F));
        register("powder_coltan_ore", makeData(ASBESTOS, 3F));
        register("crystal_uranium", makeData(RADIATION, u * crystal));
        register("crystal_thorium", makeData(RADIATION, th232 * crystal));
        register("crystal_plutonium", makeData(RADIATION, pu * crystal));
        register("crystal_schraranium", makeData(RADIATION, sr * crystal));
        register("crystal_schrabidium", makeData(RADIATION, sa326 * crystal));
        register("crystal_phosphorus", makeData(HOT, 2F * crystal));
        register("crystal_lithium", makeData(HYDROACTIVE, 1F * crystal));
        register("crystal_trixite", makeData(RADIATION, trx * crystal));
        register("boy_propellant", makeData(EXPLOSIVE, 2F));
        register("gadget_core", makeData(RADIATION, pu239 * nugget * 10));
        register("boy_target", makeData(RADIATION, u235 * ingot * 2));
        register("boy_bullet", makeData(RADIATION, u235 * ingot));
        register("man_core", makeData(RADIATION, pu239 * nugget * 10));
        register("mike_core", makeData(RADIATION, u238 * nugget * 10));
        register("tsar_core", makeData(RADIATION, pu239 * nugget * 15));
        register("fleija_propellant", makeData().addEntry(RADIATION, 15F).addEntry(EXPLOSIVE, 8F).addEntry(BLINDING, 50F));
        register("fleija_core", makeData(RADIATION, 10F));
        register("solinium_propellant", makeData(EXPLOSIVE, 10F));
        register("solinium_core", makeData().addEntry(RADIATION, sa327 * nugget * 8).addEntry(BLINDING, 45F));
        register("nuke_fstbmb", makeData(DIGAMMA, 0.01F));
        register("holotape_image#holo_restored", makeData(DIGAMMA, 1F));
        register("holotape_damaged", makeData(DIGAMMA, 1_000F));
    }

    /**
     * Port-only objects. rod_zirnox_depleted (one item with variants) stands for the separate depleted ZIRNOX rods;
     * the 1.21 raw ore drops and deepslate ores stand for the ore block of the same material.
     */
    private static void registerAliases() {
        for (String fuel : List.of("natural_uranium_fuel", "uranium_fuel", "thorium_fuel", "mox_fuel", "plutonium_fuel",
                "u233_fuel", "u235_fuel", "les_fuel")) {
            ALIASES.put("rod_zirnox_depleted#" + fuel, "rod_zirnox_" + fuel + "_depleted");
        }
        ALIASES.put("rod_zirnox_depleted#zfb_mox_fuel", "rod_zirnox_zfb_mox_depleted");
        ALIASES.put("ore_deepslate_asbestos", "ore_asbestos");
        ALIASES.put("ore_deepslate_schrabidium", "ore_schrabidium");
        ALIASES.put("raw_schrabidium", "ore_schrabidium");
        ALIASES.put("raw_plutonium", "ore_nether_plutonium"); // drop of ore_nether_plutonium
        ALIASES.put("raw_lithium", "ore_gneiss_lithium"); // drop of ore_gneiss_lithium
    }
}

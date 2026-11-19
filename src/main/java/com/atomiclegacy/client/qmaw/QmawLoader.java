package com.atomiclegacy.client.qmaw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.FluidIconItem;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Loads the original resource-pack driven Quick Manual and Wiki format. */
public final class QmawLoader extends SimplePreparableReloadListener<QmawLoader.Prepared> {
    private static final String[] LEGACY_COKE = {
            "coke_coal", "coke_lignite", "coke_petroleum"
    };
    private static final String[] LEGACY_PA_COILS = {
            "pa_coil_gold", "pa_coil_niobium", "pa_coil_bscco", "pa_coil_chlorophyte"
    };
    private static final String[] LEGACY_SATELLITES = {
            "satellite_spy",
            "satellite_scanner",
            "satellite_radar",
            "satellite_miner_astro",
            "satellite_miner_lunar",
            "satellite_precision_laser",
            "satellite_death_ray",
            "satellite_xenium_resonator",
            "satellite_relay",
            "satellite_detector",
            "satellite_ray_scan",
            "satellite_science",
            "satellite_science_assembler",
            "satellite_science_sensor"
    };
    private static final String[] LEGACY_PILE_DEVICES = {
            "pile_loader", "pile_vent", "pile_control"
    };
    private static final String[] LEGACY_DENSE_WIRES = {
            "wire_dense_bscco",
            "wire_dense_copper",
            "wire_dense_dineutronium",
            "wire_dense_gold",
            "wire_dense_magnetized_tungsten",
            "wire_dense_neodymium",
            "wire_dense_niobium",
            "wire_dense_red_copper",
            "wire_dense_schrabidate",
            "wire_dense_schrabidium",
            "wire_dense_starmetal",
            "wire_dense_steel",
            "wire_dense_titanium",
            "wire_dense_tungsten"
    };
    /** Exact 1.7.10 GunFactory.EnumAmmo ordinals (all variants; [b3] the firearm batches restored every ammo_standard type). */
    private static final Map<Integer, String> LEGACY_STANDARD_AMMO = Map.ofEntries(
            Map.entry(0, "stone"),
            Map.entry(1, "stone_ap"),
            Map.entry(2, "stone_iron"),
            Map.entry(3, "stone_shot"),
            Map.entry(4, "m357_bp"),
            Map.entry(5, "m357_sp"),
            Map.entry(6, "m357_fmj"),
            Map.entry(7, "m357_jhp"),
            Map.entry(8, "m357_ap"),
            Map.entry(9, "m357_express"),
            Map.entry(10, "m44_bp"),
            Map.entry(11, "m44_sp"),
            Map.entry(12, "m44_fmj"),
            Map.entry(13, "m44_jhp"),
            Map.entry(14, "m44_ap"),
            Map.entry(15, "m44_express"),
            Map.entry(16, "p22_sp"),
            Map.entry(17, "p22_fmj"),
            Map.entry(18, "p22_jhp"),
            Map.entry(19, "p22_ap"),
            Map.entry(20, "p9_sp"),
            Map.entry(21, "p9_fmj"),
            Map.entry(22, "p9_jhp"),
            Map.entry(23, "p9_ap"),
            Map.entry(24, "r556_sp"),
            Map.entry(25, "r556_fmj"),
            Map.entry(26, "r556_jhp"),
            Map.entry(27, "r556_ap"),
            Map.entry(28, "r762_sp"),
            Map.entry(29, "r762_fmj"),
            Map.entry(30, "r762_jhp"),
            Map.entry(31, "r762_ap"),
            Map.entry(32, "r762_du"),
            Map.entry(33, "bmg50_sp"),
            Map.entry(34, "bmg50_fmj"),
            Map.entry(35, "bmg50_jhp"),
            Map.entry(36, "bmg50_ap"),
            Map.entry(37, "bmg50_du"),
            Map.entry(38, "b75"),
            Map.entry(39, "b75_inc"),
            Map.entry(40, "b75_exp"),
            Map.entry(41, "g12_bp"),
            Map.entry(42, "g12_bp_magnum"),
            Map.entry(43, "g12_bp_slug"),
            Map.entry(44, "g12"),
            Map.entry(45, "g12_slug"),
            Map.entry(46, "g12_flechette"),
            Map.entry(47, "g12_magnum"),
            Map.entry(48, "g12_explosive"),
            Map.entry(49, "g12_phosphorus"),
            Map.entry(50, "g26_flare"),
            Map.entry(51, "g26_flare_supply"),
            Map.entry(52, "g26_flare_weapon"),
            Map.entry(53, "g40_he"),
            Map.entry(54, "g40_heat"),
            Map.entry(55, "g40_demo"),
            Map.entry(56, "g40_inc"),
            Map.entry(57, "g40_phosphorus"),
            Map.entry(58, "rocket_he"),
            Map.entry(59, "rocket_heat"),
            Map.entry(60, "rocket_demo"),
            Map.entry(61, "rocket_inc"),
            Map.entry(62, "rocket_phosphorus"),
            Map.entry(63, "flame_diesel"),
            Map.entry(64, "flame_gas"),
            Map.entry(65, "flame_napalm"),
            Map.entry(66, "flame_balefire"),
            Map.entry(67, "capacitor"),
            Map.entry(68, "capacitor_overcharge"),
            Map.entry(69, "capacitor_ir"),
            Map.entry(70, "tau_uranium"),
            Map.entry(71, "coil_tungsten"),
            Map.entry(72, "coil_ferrouranium"),
            Map.entry(73, "nuke_standard"),
            Map.entry(74, "nuke_demo"),
            Map.entry(75, "nuke_high"),
            Map.entry(76, "nuke_tots"),
            Map.entry(77, "nuke_hive"),
            Map.entry(78, "g10"),
            Map.entry(79, "g10_shrapnel"),
            Map.entry(80, "g10_du"),
            Map.entry(81, "g10_slug"),
            Map.entry(82, "r762_he"),
            Map.entry(83, "bmg50_he"),
            Map.entry(84, "g10_explosive"),
            Map.entry(85, "p45_sp"),
            Map.entry(86, "p45_fmj"),
            Map.entry(87, "p45_jhp"),
            Map.entry(88, "p45_ap"),
            Map.entry(89, "p45_du"),
            Map.entry(90, "ct_hook"),
            Map.entry(91, "ct_mortar"),
            Map.entry(92, "ct_mortar_charge"),
            Map.entry(93, "nuke_balefire"),
            Map.entry(94, "bmg50_sm"),
            Map.entry(95, "rocket_micro")
    );

    private static volatile Map<String, QuickManualAndWiki> pages = Map.of();
    private static volatile Map<String, QuickManualAndWiki> exactTriggers = Map.of();
    private static volatile Map<Item, QuickManualAndWiki> wildcardTriggers = Map.of();

    @Override
    protected Prepared prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Map.Entry<ResourceLocation, Resource>> resources = new ArrayList<>(resourceManager
                .listResources("manual", id -> id.getPath().endsWith(".json"))
                .entrySet());
        resources.sort(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)));

        LinkedHashMap<String, ParsedPage> parsed = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : resources) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ParsedPage page = parsePage(entry.getKey(), json);
                if (page != null) {
                    ParsedPage previous = parsed.put(page.manual().name(), page);
                    if (previous != null) {
                        AtomicLegacy.LOGGER.info("[QMAW] Overriding manual page {} with {}",
                                page.manual().name(), entry.getKey());
                    }
                }
            } catch (Exception exception) {
                AtomicLegacy.LOGGER.error("[QMAW] Failed to read manual {}", entry.getKey(), exception);
            }
        }
        return new Prepared(parsed);
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        LinkedHashMap<String, QuickManualAndWiki> newPages = new LinkedHashMap<>();
        LinkedHashMap<String, QuickManualAndWiki> newExactTriggers = new LinkedHashMap<>();
        LinkedHashMap<Item, QuickManualAndWiki> newWildcardTriggers = new LinkedHashMap<>();
        int unresolved = 0;
        int intentionallyRemoved = 0;
        Set<String> unresolvedReferences = new LinkedHashSet<>();

        for (ParsedPage parsed : prepared.pages().values()) {
            QuickManualAndWiki manual = parsed.manual();
            newPages.put(manual.name(), manual);
            for (ParsedTrigger trigger : parsed.triggers()) {
                if (trigger.stack().isEmpty()) {
                    if (trigger.intentionallyRemoved()) {
                        intentionallyRemoved++;
                    } else {
                        unresolved++;
                        unresolvedReferences.add(trigger.reference());
                    }
                    continue;
                }
                if (trigger.wildcard()) {
                    newWildcardTriggers.put(trigger.stack().getItem(), manual);
                } else {
                    newExactTriggers.put(stackKey(trigger.stack()), manual);
                }
            }
        }

        pages = Map.copyOf(newPages);
        exactTriggers = Map.copyOf(newExactTriggers);
        wildcardTriggers = Map.copyOf(newWildcardTriggers);
        AtomicLegacy.LOGGER.info(
                "[QMAW] Loaded {} pages and {} triggers ({} unresolved legacy triggers, {} triggers without a ported ammo variant skipped)",
                pages.size(), exactTriggers.size() + wildcardTriggers.size(), unresolved, intentionallyRemoved);
        if (!unresolvedReferences.isEmpty()) {
            AtomicLegacy.LOGGER.warn("[QMAW] Unresolved legacy trigger references: {}", unresolvedReferences);
        }
    }

    public static Optional<QuickManualAndWiki> page(String name) {
        return Optional.ofNullable(pages.get(name));
    }

    public static Optional<QuickManualAndWiki> pageFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        QuickManualAndWiki exact = exactTriggers.get(stackKey(stack));
        return Optional.ofNullable(exact != null ? exact : wildcardTriggers.get(stack.getItem()));
    }

    public static List<QuickManualAndWiki> indexedPages() {
        return pages.values().stream()
                .filter(page -> !page.noIndex())
                .sorted(Comparator.comparing(QuickManualAndWiki::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Expands latest-upstream {{page}} composition in the current language, with en_US fallback. */
    public static String compose(String contents, String language) {
        String result = contents;
        int recursionBrake = 100;
        while (result.contains("{{") && recursionBrake-- > 0) {
            int begin = result.indexOf("{{");
            int end = result.indexOf("}}", begin + 2);
            if (end < 0) {
                break;
            }
            String tag = result.substring(begin + 2, end);
            String replacement = page(tag).map(page -> page.content(language)).orElse(tag);
            result = result.substring(0, begin) + replacement + result.substring(end + 2);
        }
        return result;
    }

    private static ParsedPage parsePage(ResourceLocation source, JsonObject json) {
        if (!json.has("name") || !json.has("title") || !json.has("content")) {
            AtomicLegacy.LOGGER.warn("[QMAW] Manual {} is missing name, title, or content", source);
            return null;
        }
        String name = json.get("name").getAsString();
        ItemStack icon = json.has("icon")
                ? firstResolvedStack(readLegacyStacks(json.getAsJsonArray("icon"), false))
                : ItemStack.EMPTY;
        Map<String, String> titles = localizedObject(json.getAsJsonObject("title"));
        Map<String, String> contents = localizedObject(json.getAsJsonObject("content"));
        boolean noIndex = json.has("noindex") && json.get("noindex").getAsBoolean();

        List<ParsedTrigger> triggers = new ArrayList<>();
        if (json.has("trigger")) {
            for (JsonElement element : json.getAsJsonArray("trigger")) {
                if (element.isJsonArray()) {
                    triggers.addAll(readLegacyStacks(element.getAsJsonArray(), true));
                }
            }
        }
        return new ParsedPage(new QuickManualAndWiki(name, icon, titles, contents, noIndex), List.copyOf(triggers));
    }

    private static Map<String, String> localizedObject(JsonObject object) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getAsString());
        }
        return values;
    }

    private static List<ParsedTrigger> readLegacyStacks(JsonArray array, boolean trigger) {
        if (array.isEmpty() || !array.get(0).isJsonPrimitive()) {
            return List.of(ParsedTrigger.unresolved("<invalid>"));
        }
        String oldId = array.get(0).getAsString();
        int count = array.size() > 1 ? Math.max(1, array.get(1).getAsInt()) : 1;
        int metadata = array.size() > 2 ? array.get(2).getAsInt() : 0;
        boolean wildcard = metadata == Short.MAX_VALUE || metadata == 32767;
        String reference = oldId + "@" + (wildcard ? "*" : metadata);
        String path = legacyPath(oldId);
        if (path == null) {
            return List.of(ParsedTrigger.unresolved(reference));
        }

        if (path.equals("fluid_icon") && !wildcard) {
            return List.of(HbmFluids.byOldId(metadata)
                    .map(fluid -> ParsedTrigger.resolved(FluidIconItem.forFluid(fluid), false, reference))
                    .orElseGet(() -> ParsedTrigger.unresolved(reference)));
        }

        List<ResourceLocation> flattened = flattenedTargets(path, metadata, wildcard);
        if (!flattened.isEmpty()) {
            List<ParsedTrigger> resolved = new ArrayList<>(flattened.size());
            for (ResourceLocation target : flattened) {
                resolved.add(resolveItem(target, count)
                        .map(stack -> ParsedTrigger.resolved(stack, wildcard, reference))
                        .orElseGet(() -> ParsedTrigger.unresolved(reference + "->" + target)));
            }
            return List.copyOf(resolved);
        }

        if (path.equals("ammo_standard") && !wildcard) {
            String variant = LEGACY_STANDARD_AMMO.get(metadata);
            if (variant == null) {
                if (!trigger) {
                    return resolveItem(AtomicLegacy.id(path), count)
                            .map(stack -> List.of(ParsedTrigger.resolved(stack, false, reference)))
                            .orElseGet(() -> List.of(ParsedTrigger.unresolved(reference)));
                }
                return List.of(ParsedTrigger.intentionallyRemoved(reference));
            }
            Optional<Item> ammo = BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(path));
            if (ammo.isEmpty() || !(ammo.get() instanceof LegacyVariantItem variantItem)) {
                return List.of(ParsedTrigger.unresolved(reference));
            }
            ItemStack stack = LegacyVariantItem.stackFor(ammo.get(), variant);
            if (!variantItem.variant(stack).id().equals(variant)) {
                return List.of(ParsedTrigger.unresolved(reference + "->variant:" + variant));
            }
            stack.setCount(Math.min(count, stack.getMaxStackSize()));
            return List.of(ParsedTrigger.resolved(stack, false, reference));
        }

        Optional<ItemStack> resolved = resolveItem(AtomicLegacy.id(path), count);
        if (resolved.isEmpty()) {
            return List.of(ParsedTrigger.unresolved(reference));
        }
        ItemStack stack = resolved.get();
        if (!wildcard && metadata > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(metadata));
        }
        return List.of(ParsedTrigger.resolved(stack, wildcard, reference));
    }

    private static ItemStack firstResolvedStack(List<ParsedTrigger> parsed) {
        for (ParsedTrigger trigger : parsed) {
            if (!trigger.stack().isEmpty()) {
                return trigger.stack();
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<ResourceLocation> flattenedTargets(String path, int metadata, boolean wildcard) {
        return switch (path) {
            case "chunk_ore" -> !wildcard && metadata == 0
                    ? List.of(AtomicLegacy.id("chunk_ore_rare"))
                    : List.of();
            case "coke" -> flattenedVariants(LEGACY_COKE, metadata, wildcard);
            case "ingot_copper" -> !wildcard && metadata == 0
                    ? List.of(ResourceLocation.withDefaultNamespace("copper_ingot"))
                    : List.of();
            case "pa_coil" -> flattenedVariants(LEGACY_PA_COILS, metadata, wildcard);
            case "satellite" -> flattenedVariants(LEGACY_SATELLITES, metadata, wildcard);
            case "pile_device" -> flattenedVariants(LEGACY_PILE_DEVICES, metadata, wildcard);
            case "wire_dense" -> {
                if (wildcard) {
                    yield localTargets(LEGACY_DENSE_WIRES);
                }
                yield metadata == 48 || metadata == 7900
                        ? List.of(AtomicLegacy.id("wire_dense_bscco"))
                        : List.of();
            }
            default -> List.of();
        };
    }

    private static List<ResourceLocation> flattenedVariants(String[] variants, int metadata, boolean wildcard) {
        if (wildcard) {
            return localTargets(variants);
        }
        return metadata >= 0 && metadata < variants.length
                ? List.of(AtomicLegacy.id(variants[metadata]))
                : List.of();
    }

    private static List<ResourceLocation> localTargets(String[] paths) {
        List<ResourceLocation> targets = new ArrayList<>(paths.length);
        for (String path : paths) {
            targets.add(AtomicLegacy.id(path));
        }
        return List.copyOf(targets);
    }

    private static Optional<ItemStack> resolveItem(ResourceLocation id, int count) {
        Optional<Item> resolved = BuiltInRegistries.ITEM.getOptional(id);
        if (resolved.isEmpty()) {
            resolved = BuiltInRegistries.BLOCK.getOptional(id)
                    .map(block -> block.asItem())
                    .filter(item -> item != Items.AIR);
        }
        return resolved.map(item -> new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize())));
    }

    private static String legacyPath(String id) {
        if (id.startsWith("hbm:item.")) {
            return id.substring("hbm:item.".length());
        }
        if (id.startsWith("hbm:tile.")) {
            return id.substring("hbm:tile.".length());
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed != null && parsed.getNamespace().equals("hbm") ? parsed.getPath() : null;
    }

    private static String stackKey(ItemStack stack) {
        StringBuilder key = new StringBuilder(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getItem() instanceof FluidIconItem) {
            FluidIconItem.fluid(stack).ifPresent(fluid -> key.append("#fluid=").append(fluid.name()));
        }
        if (stack.getItem() instanceof LegacyVariantItem variantItem) {
            return key.append("#variant=").append(variantItem.variant(stack).id())
                    .toString().toLowerCase(Locale.ROOT);
        }
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (model != null) {
            key.append("#model=").append(model.value());
        }
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!custom.isEmpty() && !(stack.getItem() instanceof FluidIconItem)) {
            key.append("#data=").append(custom.copyTag());
        }
        return key.toString().toLowerCase(Locale.ROOT);
    }

    record Prepared(Map<String, ParsedPage> pages) {
    }

    private record ParsedPage(QuickManualAndWiki manual, List<ParsedTrigger> triggers) {
    }

    private record ParsedTrigger(ItemStack stack, boolean wildcard, boolean intentionallyRemoved, String reference) {
        private static ParsedTrigger resolved(ItemStack stack, boolean wildcard, String reference) {
            return new ParsedTrigger(stack, wildcard, false, reference);
        }

        private static ParsedTrigger unresolved(String reference) {
            return new ParsedTrigger(ItemStack.EMPTY, false, false, reference);
        }

        private static ParsedTrigger intentionallyRemoved(String reference) {
            return new ParsedTrigger(ItemStack.EMPTY, false, true, reference);
        }
    }
}

package com.atomiclegacy.foundry;

import com.atomiclegacy.items.BedrockOreFragments;
import com.atomiclegacy.items.ScrapsItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

final class FoundryMaterialLookup {
    private FoundryMaterialLookup() {
    }

    static List<FoundryMaterialStack> smeltingMaterialsFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // [parity Q1: MatDistribution#registerEntry fixed entries (items without an ore dictionary match)]
        List<FoundryMaterialStack> fixedEntries = legacyMaterialEntries(id);
        if (!fixedEntries.isEmpty()) {
            return converted(fixedEntries);
        }
        List<FoundryMaterialStack> exactVanillaOreMaterials = exactVanillaOreSmeltingMaterials(id);
        if (!exactVanillaOreMaterials.isEmpty()) {
            return exactVanillaOreMaterials;
        }

        List<FoundryMaterialStack> oreMaterials = oreSmeltingMaterials(stack);
        if (!oreMaterials.isEmpty()) {
            return oreMaterials;
        }

        if (id != null
                && id.getNamespace().equals("atomiclegacy")
                && id.getPath().equals("chunk_ore_cryolite")) {
            return List.of(
                    new FoundryMaterialStack(FoundryMaterial.get("aluminium"), FoundryShape.INGOT.q(1)),
                    new FoundryMaterialStack(FoundryMaterial.get("sodium"), FoundryShape.INGOT.q(1))
            );
        }
        List<FoundryMaterialStack> rawMaterials = directRawMaterial(id);
        if (!rawMaterials.isEmpty()) {
            return rawMaterials;
        }

        Optional<FoundryMaterialStack> base = materialFromItem(stack);
        if (base.isEmpty()) {
            return List.of();
        }
        FoundryMaterialStack materialStack = base.get();
        FoundryMaterial target = materialStack.material().smeltsInto();
        if (target.behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE
                && target.behavior() != FoundryMaterial.SmeltingBehavior.ADDITIVE) {
            return List.of();
        }
        int amount = materialStack.amount() * materialStack.material().conversionOut()
                / Math.max(1, materialStack.material().conversionIn());
        return List.of(new FoundryMaterialStack(target, amount));
    }

    /** Mats#getSmeltingMaterialsFromItem: every stack becomes smeltsInto at convOut / convIn. */
    private static List<FoundryMaterialStack> converted(List<FoundryMaterialStack> stacks) {
        java.util.ArrayList<FoundryMaterialStack> result = new java.util.ArrayList<>(stacks.size());
        for (FoundryMaterialStack stack : stacks) {
            FoundryMaterial material = stack.material();
            result.add(new FoundryMaterialStack(material.smeltsInto(),
                    stack.amount() * material.conversionOut() / Math.max(1, material.conversionIn())));
        }
        return List.copyOf(result);
    }

    /** [parity Q1] MatDistribution#registerDefaults registerEntry list, keyed by the flattened item id. */
    private static List<FoundryMaterialStack> legacyMaterialEntries(ResourceLocation id) {
        if (id == null) {
            return List.of();
        }
        int ingot = FoundryShape.INGOT.q(1);
        return switch (id.toString()) {
            case "minecraft:rail" -> materials("iron", FoundryShape.INGOT.q(6, 16));
            case "minecraft:powered_rail" -> materials("gold", FoundryShape.INGOT.q(6, 6), "redstone", FoundryShape.DUST.q(1, 6));
            case "minecraft:detector_rail" -> materials("iron", FoundryShape.INGOT.q(6, 6), "redstone", FoundryShape.DUST.q(1, 6));
            case "minecraft:minecart" -> materials("iron", FoundryShape.INGOT.q(5));
            case "minecraft:charcoal" -> materials("carbon", FoundryShape.NUGGET.q(3));
            case "atomiclegacy:casing_small" -> materials("gunmetal", FoundryShape.PLATE.q(1, 4));
            case "atomiclegacy:casing_small_steel" -> materials("weaponsteel", FoundryShape.PLATE.q(1, 4));
            case "atomiclegacy:casing_large" -> materials("gunmetal", FoundryShape.PLATE.q(1, 2));
            case "atomiclegacy:casing_large_steel" -> materials("weaponsteel", FoundryShape.PLATE.q(1, 2));
            case "atomiclegacy:chunk_ore_cryolite" -> materials("aluminium", ingot, "sodium", ingot);
            case "atomiclegacy:stone_resource_limestone" -> materials("flux", FoundryShape.DUST.q(10));
            case "atomiclegacy:powder_ash_wood", "atomiclegacy:powder_ash_misc" -> materials("carbon", FoundryShape.NUGGET.q(1));
            case "atomiclegacy:powder_ash_coal" -> materials("carbon", FoundryShape.NUGGET.q(2));
            // ore dictionary equivalents of items whose flattened id does not carry the material prefix
            case "minecraft:redstone" -> materials("redstone", FoundryShape.DUST.q(1));
            case "minecraft:redstone_block" -> materials("redstone", FoundryShape.BLOCK.q(1));
            case "minecraft:diamond" -> materials("diamond", ingot);
            case "minecraft:diamond_block" -> materials("diamond", FoundryShape.BLOCK.q(1));
            case "minecraft:emerald" -> materials("emerald", ingot);
            case "minecraft:emerald_block" -> materials("emerald", FoundryShape.BLOCK.q(1));
            case "minecraft:coal_block" -> materials("coal", FoundryShape.BLOCK.q(1));
            case "atomiclegacy:lignite" -> materials("lignite", ingot);
            case "atomiclegacy:powder_coal_tiny" -> materials("coal", FoundryShape.INGOT.q(1, 4));
            case "atomiclegacy:coke_coal" -> materials("coalcoke", ingot);
            case "atomiclegacy:coke_petroleum" -> materials("petcoke", ingot);
            case "atomiclegacy:coke_lignite" -> materials("ligcoke", ingot);
            case "atomiclegacy:block_coke" -> materials("coalcoke", FoundryShape.BLOCK.q(1));
            case "atomiclegacy:block_coke_petroleum" -> materials("petcoke", FoundryShape.BLOCK.q(1));
            case "atomiclegacy:block_coke_lignite" -> materials("ligcoke", FoundryShape.BLOCK.q(1));
            default -> List.of();
        };
    }

    private static List<FoundryMaterialStack> exactVanillaOreSmeltingMaterials(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("minecraft")) {
            return List.of();
        }
        // Minecraft did not have copper ore in 1.7.10.  The exact modern
        // vanilla copper entries use HBM 1.7.10's oreCopper crucible
        // distribution instead of relying on a broad fallback.
        return switch (id.getPath()) {
            case "copper_ore", "deepslate_copper_ore", "raw_copper" -> oreSmeltingMaterials("copper");
            default -> List.of();
        };
    }

    private static List<FoundryMaterialStack> oreSmeltingMaterials(ItemStack stack) {
        return stack.getTags()
                .map(TagKey::location)
                .filter(tag -> tag.getNamespace().equals("c") || tag.getNamespace().equals("forge"))
                .map(ResourceLocation::getPath)
                .filter(path -> path.startsWith("ores/") || path.startsWith("raw_materials/"))
                .map(path -> canonicalMaterialName(path.substring(path.indexOf('/') + 1)))
                .map(FoundryMaterialLookup::oreSmeltingMaterials)
                .filter(materials -> !materials.isEmpty())
                .findFirst()
                .orElseGet(List::of);
    }

    /** Exact custom ore distributions registered by HBM 1.7.10 MatDistribution. */
    private static List<FoundryMaterialStack> oreSmeltingMaterials(String ore) {
        return switch (ore) {
            case "iron" -> materials("iron", 144, "titanium", 24, "stone", 162);
            case "titanium" -> materials("titanium", 144, "iron", 24, "stone", 162);
            case "tungsten" -> materials("tungsten", 144, "stone", 162);
            case "aluminium" -> materials("aluminium", 144, "sodium", 24, "stone", 162);
            case "coal" -> materials("carbon", 216, "stone", 162);
            case "gold" -> materials("gold", 144, "lead", 24, "stone", 162);
            case "uranium" -> materials("uranium", 144, "lead", 24, "stone", 162);
            case "thorium" -> materials("thorium", 144, "uranium", 24, "stone", 162);
            case "copper" -> materials("copper", 144, "stone", 162);
            case "lead" -> materials("lead", 144, "gold", 8, "stone", 162);
            case "beryllium" -> materials("beryllium", 144, "stone", 162);
            case "cobalt" -> materials("cobalt", 72, "stone", 162);
            case "redstone" -> materials("redstone", 288, "stone", 162);
            case "hematite" -> materials("hematite", 72);
            case "malachite" -> materials("malachite", 432);
            default -> List.of();
        };
    }

    private static List<FoundryMaterialStack> directRawMaterial(ResourceLocation id) {
        if (id == null) {
            return List.of();
        }
        String material = switch (id.toString()) {
            case "minecraft:raw_iron" -> "iron";
            case "minecraft:raw_gold" -> "gold";
            case "minecraft:raw_copper" -> "copper";
            default -> id.getNamespace().equals("atomiclegacy") && id.getPath().startsWith("raw_")
                    ? canonicalMaterialName(id.getPath().substring("raw_".length()))
                    : "";
        };
        return material.isBlank() ? List.of() : oreSmeltingMaterials(material);
    }

    private static List<FoundryMaterialStack> materials(Object... entries) {
        java.util.ArrayList<FoundryMaterialStack> materials = new java.util.ArrayList<>(entries.length / 2);
        for (int index = 0; index < entries.length; index += 2) {
            FoundryMaterial material = FoundryMaterial.get((String) entries[index]);
            if (material != null) {
                materials.add(new FoundryMaterialStack(material, (int) entries[index + 1]));
            }
        }
        return List.copyOf(materials);
    }

    static Optional<FoundryMaterialStack> materialFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof ScrapsItem) {
            return Optional.ofNullable(ScrapsItem.contents(stack));
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return Optional.empty();
        }
        String namespace = id.getNamespace();
        String path = id.getPath();

        if (namespace.equals("atomiclegacy")) {
            String bedrockOreFragmentMaterial = BedrockOreFragments.materialNameFromIndependentPath(path);
            if (!bedrockOreFragmentMaterial.isBlank()) {
                return FoundryMaterial.byName(bedrockOreFragmentMaterial)
                        .map(material -> new FoundryMaterialStack(material, FoundryShape.QUANTUM.q(8)));
            }
        }

        Optional<FoundryMaterialStack> vanilla = vanillaMaterial(namespace, path);
        if (vanilla.isPresent()) {
            return vanilla;
        }

        Optional<FoundryMaterialStack> tagged = taggedMaterial(stack);
        if (tagged.isPresent()) {
            return tagged;
        }

        return hbmMaterial(path);
    }

    /**
     * HBM 1.7.10 used the ore dictionary for foundry inputs.  In modern
     * NeoForge the equivalent contract is a common item tag, not a foreign
     * item's registry path.  This keeps the foundry interoperable with both
     * current c: tags and older Forge-tagged add-ons.
     */
    private static Optional<FoundryMaterialStack> taggedMaterial(ItemStack stack) {
        return stack.getTags()
                .map(TagKey::location)
                .filter(tag -> tag.getNamespace().equals("c") || tag.getNamespace().equals("forge"))
                .map(FoundryMaterialLookup::stackFromMaterialTag)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<FoundryMaterialStack> stackFromMaterialTag(ResourceLocation tag) {
        String[] path = tag.getPath().split("/", 2);
        if (path.length != 2 || path[1].isBlank()) {
            return Optional.empty();
        }

        String materialName = path[1];
        FoundryShape shape = switch (path[0]) {
            case "ingots" -> FoundryShape.INGOT;
            case "nuggets" -> FoundryShape.NUGGET;
            case "dusts", "powders" -> FoundryShape.DUST;
            case "plates" -> {
                if (materialName.startsWith("cast_")) {
                    materialName = materialName.substring("cast_".length());
                    yield FoundryShape.CAST_PLATE;
                }
                if (materialName.startsWith("welded_")) {
                    materialName = materialName.substring("welded_".length());
                    yield FoundryShape.WELDED_PLATE;
                }
                yield FoundryShape.PLATE;
            }
            case "wires" -> FoundryShape.WIRE;
            case "wires_dense", "dense_wires" -> FoundryShape.DENSE_WIRE;
            case "bolts" -> FoundryShape.BOLT;
            case "billets" -> FoundryShape.BILLET;
            case "pipes" -> FoundryShape.PIPE;
            case "shells" -> FoundryShape.SHELL;
            case "storage_blocks" -> FoundryShape.BLOCK;
            case "raw_materials" -> FoundryShape.INGOT;
            default -> null;
        };
        if (shape == null) {
            return Optional.empty();
        }

        return FoundryMaterial.byName(canonicalMaterialName(materialName))
                .filter(material -> FoundryMaterialShapes.supports(shape, material))
                .map(material -> new FoundryMaterialStack(material, shape.q(1)));
    }

    private static String canonicalMaterialName(String materialName) {
        return switch (materialName) {
            case "aluminum" -> "aluminium";
            case "wroughtiron" -> "wrought_iron";
            case "pigiron" -> "pig_iron";
            case "meteoriciron" -> "meteoric_iron";
            // HBM 1.7.10 keeps graphite as a carbon conversion material. Its
            // ICF pellet press accepts a graphite ingot as the carbon fuel.
            case "graphite" -> "carbon";
            default -> materialName;
        };
    }

    private static Optional<FoundryMaterialStack> vanillaMaterial(String namespace, String path) {
        if (!namespace.equals("minecraft")) {
            return Optional.empty();
        }
        return switch (path) {
            case "stone", "cobblestone" -> stack("stone", FoundryShape.BLOCK);
            case "obsidian" -> stack("obsidian", FoundryShape.BLOCK);
            case "iron_ingot" -> stack("iron", FoundryShape.INGOT);
            case "gold_ingot" -> stack("gold", FoundryShape.INGOT);
            case "copper_ingot" -> stack("copper", FoundryShape.INGOT);
            case "iron_block" -> stack("iron", FoundryShape.BLOCK);
            case "gold_block" -> stack("gold", FoundryShape.BLOCK);
            case "copper_block" -> stack("copper", FoundryShape.BLOCK);
            case "iron_nugget" -> stack("iron", FoundryShape.NUGGET);
            case "gold_nugget" -> stack("gold", FoundryShape.NUGGET);
            case "redstone" -> stack("redstone", FoundryShape.DUST); // [parity Q1: dustRedstone]
            case "coal" -> stack("coal", FoundryShape.INGOT);
            default -> Optional.empty();
        };
    }

    private static Optional<FoundryMaterialStack> hbmMaterial(String path) {
        if (path.equals("lithium")) {
            return stack("lithium", FoundryShape.INGOT);
        }
        if (path.equals("blade_titanium")) {
            return stack("titanium", FoundryShape.INGOT, 3);
        }
        if (path.equals("blade_tungsten")) {
            return stack("tungsten", FoundryShape.INGOT, 3);
        }
        if (path.equals("blades_steel")) {
            return stack("steel", FoundryShape.INGOT, 4);
        }
        if (path.equals("blades_titanium")) {
            return stack("titanium", FoundryShape.INGOT, 4);
        }
        if (path.equals("stamp_stone_flat")) {
            return stack("stone", FoundryShape.INGOT, 3);
        }
        if (path.equals("stamp_iron_flat")) {
            return stack("iron", FoundryShape.INGOT, 3);
        }
        if (path.equals("stamp_steel_flat")) {
            return stack("steel", FoundryShape.INGOT, 3);
        }
        if (path.equals("stamp_titanium_flat")) {
            return stack("titanium", FoundryShape.INGOT, 3);
        }
        if (path.equals("stamp_obsidian_flat")) {
            return stack("obsidian", FoundryShape.INGOT, 3);
        }
        if (path.equals("plate_cast_steel")) {
            return stack("steel", FoundryShape.CAST_PLATE);
        }
        if (path.equals("plate_cast_copper")) {
            return stack("copper", FoundryShape.CAST_PLATE);
        }
        if (path.equals("pipes_steel")) {
            return stack("steel", FoundryShape.BLOCK, 3);
        }

        List<Prefix> prefixes = List.of(
                new Prefix("ingot_raw_", FoundryShape.INGOT),
                new Prefix("ingot_", FoundryShape.INGOT),
                new Prefix("nugget_raw_", FoundryShape.NUGGET),
                new Prefix("nugget_", FoundryShape.NUGGET),
                new Prefix("powder_", FoundryShape.DUST),
                new Prefix("dust_", FoundryShape.DUST),
                new Prefix("plate_cast_", FoundryShape.CAST_PLATE),
                new Prefix("plate_welded_", FoundryShape.WELDED_PLATE),
                new Prefix("plate_", FoundryShape.PLATE),
                new Prefix("wire_dense_", FoundryShape.DENSE_WIRE),
                new Prefix("wire_", FoundryShape.WIRE),
                new Prefix("bolt_", FoundryShape.BOLT),
                new Prefix("billet_", FoundryShape.BILLET),
                new Prefix("pipe_", FoundryShape.PIPE),
                new Prefix("pipes_", FoundryShape.PIPE),
                new Prefix("shell_", FoundryShape.SHELL),
                new Prefix("part_barrel_light_", FoundryShape.BARREL_LIGHT),
                new Prefix("part_barrel_heavy_", FoundryShape.BARREL_HEAVY),
                new Prefix("part_receiver_light_", FoundryShape.RECEIVER_LIGHT),
                new Prefix("part_receiver_heavy_", FoundryShape.RECEIVER_HEAVY),
                new Prefix("part_stock_", FoundryShape.STOCK),
                new Prefix("part_grip_", FoundryShape.GRIP),
                new Prefix("part_mechanism_", FoundryShape.MECHANISM),
                new Prefix("block_", FoundryShape.BLOCK),
                new Prefix("raw_", FoundryShape.INGOT)
        );

        for (Prefix prefix : prefixes) {
            if (!path.startsWith(prefix.value())) {
                continue;
            }
            String suffix = path.substring(prefix.value().length());
            Optional<FoundryMaterialStack> found = stack(suffix, prefix.shape());
            if (found.isPresent() && FoundryMaterialShapes.supports(prefix.shape(), found.get().material())) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<FoundryMaterialStack> stack(String materialName, FoundryShape shape) {
        return stack(materialName, shape, 1);
    }

    private static Optional<FoundryMaterialStack> stack(String materialName, FoundryShape shape, int count) {
        return FoundryMaterial.byName(materialName)
                .map(material -> new FoundryMaterialStack(material, shape.q(count)));
    }

    private record Prefix(String value, FoundryShape shape) {
    }
}

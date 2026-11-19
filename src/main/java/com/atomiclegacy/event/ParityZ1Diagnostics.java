package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyArmorBodyModels;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [parity z1: diagnostics] Worn armor models: every row of {@link LegacyArmorBodyModels#PARTS} (the table that
 * render/LegacyFsbArmorLayer draws) has a registered item of the matching armor slot, an OBJ with that group and an
 * existing texture; the box-model / mod accessories of LegacyAccessoryArmorLayer, the T-51 layer and the Tesla back
 * mod have their items and resources; every other armor item of the mod has its vanilla layer textures.
 */
final class ParityZ1Diagnostics {
    private ParityZ1Diagnostics() {
    }

    /** item id -> resources of LegacyAccessoryArmorLayer / T51ArmorLayer / TeslaBackLayer (OBJ entries as "obj:path#group"). */
    private static final Map<String, List<String>> ACCESSORIES = Map.ofEntries(
            Map.entry("gas_mask", List.of("textures/models/gas_mask.png")),
            Map.entry("gas_mask_m65", List.of("textures/models/model_m65.png")),
            Map.entry("gas_mask_olde", List.of("textures/armor/mask_olde.png")),
            Map.entry("gas_mask_mono", List.of("textures/models/model_m65_mono.png")),
            Map.entry("hazmat_helmet_red", List.of("textures/models/model_haz_red.png")),
            Map.entry("hazmat_helmet_grey", List.of("textures/models/model_haz_grey.png")),
            Map.entry("liquidator_helmet", List.of("textures/armor/liquidator_helmet.png")),
            Map.entry("goggles", List.of("textures/models/goggles.png")),
            Map.entry("attachment_mask", List.of("textures/models/model_m65.png")),
            Map.entry("attachment_mask_mono", List.of("textures/models/model_m65_mono.png")),
            Map.entry("jetpack_fly", List.of("textures/models/jet_pack_red.png")),
            Map.entry("jetpack_break", List.of("textures/models/jet_pack_blue.png")),
            Map.entry("jetpack_vector", List.of("textures/models/jet_pack_green.png")),
            Map.entry("jetpack_boost", List.of("textures/models/jet_pack.png")),
            Map.entry("wings_murk", List.of("textures/armor/wings_murk.png", "obj:models/obj/armor/murk.obj#LeftBase",
                    "obj:models/obj/armor/murk.obj#LeftTip", "obj:models/obj/armor/murk.obj#RightBase", "obj:models/obj/armor/murk.obj#RightTip")),
            Map.entry("wings_limp", List.of("textures/armor/wings_murk.png", "obj:models/obj/armor/murk.obj#LeftBase")),
            Map.entry("back_tesla", List.of("textures/models/armor/mod_tesla.png", "obj:models/obj/armor/mod_tesla.obj#Cube",
                    "models/armor/back_tesla.json")),
            Map.entry("t51_helmet", List.of("textures/armor/t51_helmet.png", "obj:models/obj/armor/t51.obj#Helmet")),
            Map.entry("t51_plate", List.of("textures/armor/t51_chest.png", "textures/armor/t51_arm.png",
                    "obj:models/obj/armor/t51.obj#Chest", "obj:models/obj/armor/t51.obj#LeftArm", "obj:models/obj/armor/t51.obj#RightArm")),
            Map.entry("t51_legs", List.of("textures/armor/t51_leg.png", "obj:models/obj/armor/t51.obj#LeftLeg", "obj:models/obj/armor/t51.obj#RightLeg")),
            Map.entry("t51_boots", List.of("textures/armor/t51_leg.png", "obj:models/obj/armor/t51.obj#LeftBoot", "obj:models/obj/armor/t51.obj#RightBoot")));

    private static final Map<String, Set<String>> GROUP_CACHE = new HashMap<>();

    static int run(MinecraftServer server, List<String> problems) {
        int cases = 0;
        Set<Item> modelled = new HashSet<>();

        for (LegacyArmorBodyModels.Part part : LegacyArmorBodyModels.PARTS) {
            cases++;
            String where = "z1 body model " + part.mesh() + "#" + part.group();
            for (ItemLike like : part.items()) {
                Item item = like.asItem();
                if (item == Items.AIR) {
                    problems.add(where + ": item not registered");
                    continue;
                }
                modelled.add(item);
                if (!(item instanceof ArmorItem armor) || armor.getEquipmentSlot() != part.slot()) {
                    problems.add(where + ": " + key(item) + " is not armor for slot " + part.slot());
                }
            }
            if (!groups("models/obj/armor/" + part.mesh() + ".obj").contains(part.group())) {
                problems.add(where + ": OBJ group missing in models/obj/armor/" + part.mesh() + ".obj");
            }
            if (!exists(part.texture())) {
                problems.add(where + ": texture missing " + part.texture());
            }
        }

        for (Map.Entry<String, List<String>> entry : ACCESSORIES.entrySet()) {
            cases++;
            Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(entry.getKey()));
            if (item == Items.AIR) {
                problems.add("z1 accessory " + entry.getKey() + ": item not registered");
                continue;
            }
            modelled.add(item);
            for (String resource : entry.getValue()) {
                if (resource.startsWith("obj:")) {
                    String path = resource.substring(4, resource.indexOf('#'));
                    String group = resource.substring(resource.indexOf('#') + 1);
                    if (!groups(path).contains(group)) {
                        problems.add("z1 accessory " + entry.getKey() + ": OBJ group " + group + " missing in " + path);
                    }
                } else if (!exists(AtomicLegacy.id(resource))) {
                    problems.add("z1 accessory " + entry.getKey() + ": resource missing " + resource);
                }
            }
        }

        // every other armor item: the vanilla biped layer textures of its material (1.7.10 getArmorTexture)
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ArmorItem armor) || modelled.contains(item)
                    || !AtomicLegacy.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                continue;
            }
            cases++;
            boolean legs = armor.getType() == ArmorItem.Type.LEGGINGS;
            for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
                ResourceLocation texture = layer.texture(legs);
                if (!exists(texture)) {
                    problems.add("z1 armor layer " + key(item) + ": texture missing " + texture);
                }
            }
        }
        return cases;
    }

    private static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static InputStream open(ResourceLocation location) {
        return ParityZ1Diagnostics.class.getResourceAsStream("/assets/" + location.getNamespace() + "/" + location.getPath());
    }

    private static boolean exists(ResourceLocation location) {
        try (InputStream input = open(location)) {
            return input != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** OBJ "o" / "g" group names, as LegacyEntityObjMesh reads them. */
    private static Set<String> groups(String path) {
        return GROUP_CACHE.computeIfAbsent(path, p -> {
            Set<String> names = new HashSet<>();
            try (InputStream input = open(AtomicLegacy.id(p))) {
                if (input == null) {
                    return names;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("o ") || trimmed.startsWith("g ")) {
                        names.add(trimmed.substring(2).trim());
                    }
                }
            } catch (Exception e) {
                names.clear();
            }
            return names;
        });
    }
}

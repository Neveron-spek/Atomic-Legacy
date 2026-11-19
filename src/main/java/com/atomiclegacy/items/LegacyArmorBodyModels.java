package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity z1] Side-neutral table of the 1.7.10 ModelArmorBase OBJ armor models (getArmorModel of ArmorFSB suits,
 * ArmorNo9, ArmorHat, ArmorAshGlasses): which worn item draws which OBJ group with which texture and GL state, on
 * which biped part. render/LegacyFsbArmorLayer draws exactly these parts; ParityZ1Diagnostics checks items and
 * resources on the dedicated server.
 */
public final class LegacyArmorBodyModels {

    public static final List<Part> PARTS = createParts();

    private LegacyArmorBodyModels() {
    }

    public static ResourceLocation texture(String name) {
        return AtomicLegacy.id("textures/armor/" + name + ".png");
    }

    public static ResourceLocation mesh(String name) {
        return AtomicLegacy.id("models/obj/armor/" + name + ".obj");
    }

    private static List<Part> createParts() {
        List<Part> parts = new ArrayList<>();
        Mode c = Mode.CUTOUT;

        // ModelArmorBismuth: one texture for every type
        addSuit(parts, "bismuth", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "bismuth", "bismuth", "bismuth", "bismuth", c, c, c, 0F,
                HbmItems.BISMUTH_HELMET, chest(HbmItems.BISMUTH_PLATE), HbmItems.BISMUTH_LEGS, HbmItems.BISMUTH_BOOTS);
        // ModelArmorDesh (ArmorDesh = steamsuit_*): armor_steamsuit
        addSuit(parts, "steamsuit", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "steamsuit_helmet", "steamsuit_chest", "steamsuit_arm", "steamsuit_leg", c, c, c, 0F,
                HbmItems.STEAMSUIT_HELMET, chest(HbmItems.STEAMSUIT_PLATE), HbmItems.STEAMSUIT_LEGS, HbmItems.STEAMSUIT_BOOTS);
        // ModelArmorDiesel (dieselsuit_*): armor_dieselsuit = bnuuy.obj, bnuuy_* textures
        addSuit(parts, "bnuuy", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "bnuuy_helmet", "bnuuy_chest", "bnuuy_arm", "bnuuy_leg", c, c, c, 0F,
                HbmItems.DIESELSUIT_HELMET, chest(HbmItems.DIESELSUIT_PLATE), HbmItems.DIESELSUIT_LEGS, HbmItems.DIESELSUIT_BOOTS);
        addSuit(parts, "ajr", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "ajr_helmet", "ajr_chest", "ajr_arm", "ajr_leg", c, c, c, 0F,
                HbmItems.AJR_HELMET, chest(HbmItems.AJR_PLATE), HbmItems.AJR_LEGS, HbmItems.AJR_BOOTS);
        // ModelArmorAJRO: armor_ajr geometry, ajro_* textures
        addSuit(parts, "ajr", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "ajro_helmet", "ajro_chest", "ajro_arm", "ajro_leg", c, c, c, 0F,
                HbmItems.AJRO_HELMET, chest(HbmItems.AJRO_PLATE), HbmItems.AJRO_LEGS, HbmItems.AJRO_BOOTS);
        // ModelArmorRPA: armor_remnant
        addSuit(parts, "remnant", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "rpa_helmet", "rpa_chest", "rpa_arm", "rpa_leg", c, c, c, 0F,
                HbmItems.RPA_HELMET, chest(HbmItems.RPA_PLATE), HbmItems.RPA_LEGS, HbmItems.RPA_BOOTS);
        addPart(parts, "remnant", EquipmentSlot.CHEST, Anchor.BODY, "Glow", "rpa_chest", Mode.GLOW, 0F, false,
                HbmItems.RPA_PLATE);
        addPart(parts, "remnant", EquipmentSlot.CHEST, Anchor.BODY, "Fan", "rpa_chest", c, 0F, true,
                HbmItems.RPA_PLATE);
        // ModelArmorNCRPA: armor_ncr
        addSuit(parts, "ncrpa", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "ncrpa_helmet", "ncrpa_chest", "ncrpa_arm", "ncrpa_leg", c, c, c, 0F,
                HbmItems.NCRPA_HELMET, chest(HbmItems.NCRPA_PLATE), HbmItems.NCRPA_LEGS, HbmItems.NCRPA_BOOTS);
        addPart(parts, "ncrpa", EquipmentSlot.HEAD, Anchor.HEAD, "Eyes", "ncrpa_helmet", Mode.GLOW, 0F, false,
                HbmItems.NCRPA_HELMET);
        // ModelArmorBJ: type 1 (bj_plate) and type 5 (bj_plate_jetpack, + Jetpack) both draw body and arms
        addSuit(parts, "bj", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "bj_eyepatch", "bj_chest", "bj_arm", "bj_leg", c, c, c, 0F,
                HbmItems.BJ_HELMET, chest(HbmItems.BJ_PLATE, HbmItems.BJ_PLATE_JETPACK), HbmItems.BJ_LEGS, HbmItems.BJ_BOOTS);
        addPart(parts, "bj", EquipmentSlot.CHEST, Anchor.BODY, "Jetpack", "bj_jetpack", c, 0F, false,
                HbmItems.BJ_PLATE_JETPACK);
        // ModelArmorEnvsuit: helmet with GL_BLEND, lamps untextured (1, 1, 0.8) fullbright
        addSuit(parts, "envsuit", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "envsuit_helmet", "envsuit_chest", "envsuit_arm", "envsuit_leg", Mode.BLEND, c, c, 0F,
                HbmItems.ENVSUIT_HELMET, chest(HbmItems.ENVSUIT_PLATE), HbmItems.ENVSUIT_LEGS, HbmItems.ENVSUIT_BOOTS);
        addPart(parts, "envsuit", EquipmentSlot.HEAD, Anchor.HEAD, "Lamps", "envsuit_helmet", Mode.LAMP, 0F, false,
                HbmItems.ENVSUIT_HELMET);
        addSuit(parts, "hev", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "hev_helmet", "hev_chest", "hev_arm", "hev_leg", c, c, c, 0F,
                HbmItems.HEV_HELMET, chest(HbmItems.HEV_PLATE), HbmItems.HEV_LEGS, HbmItems.HEV_BOOTS);
        // ModelArmorDigamma: GL_BLEND enabled from the cassette on (cassette, then both arms)
        addSuit(parts, "fau", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "fau_helmet", "fau_chest", "fau_arm", "fau_leg", c, Mode.BLEND, c, 0F,
                HbmItems.FAU_HELMET, chest(HbmItems.FAU_PLATE), HbmItems.FAU_LEGS, HbmItems.FAU_BOOTS);
        addPart(parts, "fau", EquipmentSlot.CHEST, Anchor.BODY, "Cassette", "fau_cassette", Mode.BLEND, 0F, false,
                HbmItems.FAU_PLATE);
        // [parity I1: dnt_* is a plain ArmorFSB (biped dnt_1/dnt_2 textures); only dns_* uses ModelArmorDNT]
        addSuit(parts, "dnt", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "dnt_helmet", "dnt_chest", "dnt_arm", "dnt_leg", c, c, c, 0F,
                HbmItems.DNS_HELMET, chest(HbmItems.DNS_PLATE), HbmItems.DNS_LEGS, HbmItems.DNS_BOOTS);
        // ModelArmorTaurun: legs / feet shifted -0.01 (left) and +0.01 (right)
        addSuit(parts, "taurun", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "taurun_helmet", "taurun_chest", "taurun_arm", "taurun_leg", c, c, c, 0.01F,
                HbmItems.TAURUN_HELMET, chest(HbmItems.TAURUN_PLATE), HbmItems.TAURUN_LEGS, HbmItems.TAURUN_BOOTS);
        // ModelArmorTrenchmaster: helmet with GL_BLEND, fullbright Light, legs / feet shifted like taurun
        addSuit(parts, "trenchmaster", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "trenchmaster_helmet", "trenchmaster_chest", "trenchmaster_arm", "trenchmaster_leg", Mode.BLEND, c, c, 0.01F,
                HbmItems.TRENCHMASTER_HELMET, chest(HbmItems.TRENCHMASTER_PLATE), HbmItems.TRENCHMASTER_LEGS, HbmItems.TRENCHMASTER_BOOTS);
        addPart(parts, "trenchmaster", EquipmentSlot.HEAD, Anchor.HEAD, "Light", "trenchmaster_helmet", Mode.GLOW, 0F, false,
                HbmItems.TRENCHMASTER_HELMET);

        // ModelGlasses (ashglasses): armor_goggles renderAll with ResourceManager.goggles (textures/armor/goggles.png)
        addPart(parts, "goggles", EquipmentSlot.HEAD, Anchor.HEAD, "Cube", "goggles", c, 0F, false,
                HbmItems.ASHGLASSES);
        // ModelHat: armor_hat renderAll
        addPart(parts, "hat", EquipmentSlot.HEAD, Anchor.HEAD, "Cube_Cube.001", "hat", c, 0F, false,
                HbmItems.HAT);
        // ModelNo9: Helmet (no9), Insignia (no9_insignia), Flame untextured fullbright while "isOn"
        addPart(parts, "no9", EquipmentSlot.HEAD, Anchor.HEAD, "Helmet", "no9", c, 0F, false,
                HbmItems.NO9);
        addPart(parts, "no9", EquipmentSlot.HEAD, Anchor.HEAD, "Insignia", "no9_insignia", c, 0F, false,
                HbmItems.NO9);
        addPart(parts, "no9", EquipmentSlot.HEAD, Anchor.HEAD, "Flame", "no9", Mode.LAMP_IF_ON, 0F, false,
                HbmItems.NO9);

        return List.copyOf(parts);
    }

    private static ItemLike[] chest(ItemLike... items) {
        return items;
    }

    private static void addSuit(
            List<Part> parts,
            String mesh,
            String headGroup,
            String bodyGroup,
            String leftArmGroup,
            String rightArmGroup,
            String leftLegGroup,
            String rightLegGroup,
            String leftBootGroup,
            String rightBootGroup,
            String helmetTexture,
            String chestTexture,
            String armTexture,
            String legTexture,
            Mode helmetMode,
            Mode armMode,
            Mode bodyMode,
            float legShift,
            ItemLike helmet,
            ItemLike[] chest,
            ItemLike legs,
            ItemLike boots
    ) {
        addPart(parts, mesh, EquipmentSlot.HEAD, Anchor.HEAD, headGroup, helmetTexture, helmetMode, 0F, false, helmet);
        addPart(parts, mesh, EquipmentSlot.CHEST, Anchor.BODY, bodyGroup, chestTexture, bodyMode, 0F, false, chest);
        addPart(parts, mesh, EquipmentSlot.CHEST, Anchor.LEFT_ARM, leftArmGroup, armTexture, armMode, 0F, false, chest);
        addPart(parts, mesh, EquipmentSlot.CHEST, Anchor.RIGHT_ARM, rightArmGroup, armTexture, armMode, 0F, false, chest);
        addPart(parts, mesh, EquipmentSlot.LEGS, Anchor.LEFT_LEG, leftLegGroup, legTexture, Mode.CUTOUT, -legShift, false, legs);
        addPart(parts, mesh, EquipmentSlot.LEGS, Anchor.RIGHT_LEG, rightLegGroup, legTexture, Mode.CUTOUT, legShift, false, legs);
        addPart(parts, mesh, EquipmentSlot.FEET, Anchor.LEFT_LEG, leftBootGroup, legTexture, Mode.CUTOUT, -legShift, false, boots);
        addPart(parts, mesh, EquipmentSlot.FEET, Anchor.RIGHT_LEG, rightBootGroup, legTexture, Mode.CUTOUT, legShift, false, boots);
    }

    private static void addPart(
            List<Part> parts,
            String mesh,
            EquipmentSlot slot,
            Anchor anchor,
            String group,
            String texture,
            Mode mode,
            float shiftX,
            boolean fan,
            ItemLike... items
    ) {
        parts.add(new Part(mesh, slot, anchor, group, texture(texture), mode, shiftX, fan, items));
    }

    public enum Mode {
        /** default armor pass state (alpha test, lighting, world light). */
        CUTOUT,
        /** GL_BLEND with glBlendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA). */
        BLEND,
        /** lightmap 240 / 240 with GL_LIGHTING disabled, textured. */
        GLOW,
        /** lightmap 240 / 240, GL_LIGHTING and GL_TEXTURE_2D disabled, glColor3f(1, 1, 0.8). */
        LAMP,
        /** LAMP only while the helmet's "isOn" tag is set (ModelNo9). */
        LAMP_IF_ON
    }

    public record Part(
            String mesh,
            EquipmentSlot slot,
            Anchor anchor,
            String group,
            ResourceLocation texture,
            Mode mode,
            float shiftX,
            boolean fan,
            ItemLike[] items
    ) {
        public boolean matches(ItemStack stack) {
            for (ItemLike item : items) {
                if (stack.is(item.asItem())) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The biped part a ModelRendererObj copies its rotation from, and its fixed setRotationPoint origin. */
    public enum Anchor {
        HEAD(0.0F, 0.0F, 0.0F),
        BODY(0.0F, 0.0F, 0.0F),
        LEFT_ARM(5.0F, 2.0F, 0.0F),
        RIGHT_ARM(-5.0F, 2.0F, 0.0F),
        LEFT_LEG(1.9F, 12.0F, 0.0F),
        RIGHT_LEG(-1.9F, 12.0F, 0.0F);

        private final float originX;
        private final float originY;
        private final float originZ;

        Anchor(float originX, float originY, float originZ) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
        }

        public float originX() { return originX; }
        public float originY() { return originY; }
        public float originZ() { return originZ; }
    }
}

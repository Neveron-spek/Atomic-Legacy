package com.atomiclegacy.items;

import java.util.List;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StandardAmmoItem extends LegacyVariantItem {
    public StandardAmmoItem(Properties properties) {
        super(properties, "ammo_standard", variants(
                "bmg50_sp",
                "bmg50_fmj",
                "bmg50_jhp",
                "bmg50_ap",
                "bmg50_du",
                "r556_sp",
                "r556_fmj",
                "r556_jhp",
                "r556_ap",
                "p9_sp",
                "p9_fmj",
                "p9_jhp",
                "p9_ap",
                "tau_uranium",
                "rocket_he",
                "rocket_heat",
                "rocket_demo",
                "rocket_inc",
                "rocket_phosphorus",
                "flame_diesel",
                "nuke_demo",
                "nuke_standard",
                "nuke_high",
                "nuke_tots",
                "nuke_hive",
                "nuke_balefire",
                // Appended so existing custom_model_data values stay unchanged.
                "bmg50_sm",
                "bmg50_he",
                "p22_sp",
                "p22_fmj",
                "p22_jhp",
                "p22_ap",
                // Remaining GunFactory.EnumAmmo values in enum order (appended).
                "stone",
                "stone_ap",
                "stone_iron",
                "stone_shot",
                "m357_bp",
                "m357_sp",
                "m357_fmj",
                "m357_jhp",
                "m357_ap",
                "m357_express",
                "m44_bp",
                "m44_sp",
                "m44_fmj",
                "m44_jhp",
                "m44_ap",
                "m44_express",
                "r762_sp",
                "r762_fmj",
                "r762_jhp",
                "r762_ap",
                "r762_du",
                "b75",
                "b75_inc",
                "b75_exp",
                "g12_bp",
                "g12_bp_magnum",
                "g12_bp_slug",
                "g12",
                "g12_slug",
                "g12_flechette",
                "g12_magnum",
                "g12_explosive",
                "g12_phosphorus",
                "g26_flare",
                "g26_flare_supply",
                "g26_flare_weapon",
                "g40_he",
                "g40_heat",
                "g40_demo",
                "g40_inc",
                "g40_phosphorus",
                "flame_gas",
                "flame_napalm",
                "flame_balefire",
                "capacitor",
                "capacitor_overcharge",
                "capacitor_ir",
                "coil_tungsten",
                "coil_ferrouranium",
                "g10",
                "g10_shrapnel",
                "g10_du",
                "g10_slug",
                "r762_he",
                "g10_explosive",
                "p45_sp",
                "p45_fmj",
                "p45_jhp",
                "p45_ap",
                "p45_du",
                "ct_hook",
                "ct_mortar",
                "ct_mortar_charge",
                "rocket_micro"
        ));
    }

    /** GunFactory.EnumAmmo.order (creative tab order of ammo_standard). */
    private static final List<String> CREATIVE_ORDER = List.of(
            "stone", "stone_ap", "stone_iron", "stone_shot", "m357_bp", "m357_sp",
            "m357_fmj", "m357_jhp", "m357_ap", "m357_express", "m44_bp", "m44_sp",
            "m44_fmj", "m44_jhp", "m44_ap", "m44_express", "p22_sp", "p22_fmj",
            "p22_jhp", "p22_ap", "p9_sp", "p9_fmj", "p9_jhp", "p9_ap",
            "p45_sp", "p45_fmj", "p45_jhp", "p45_ap", "p45_du", "r556_sp",
            "r556_fmj", "r556_jhp", "r556_ap", "r762_sp", "r762_fmj", "r762_jhp",
            "r762_ap", "r762_du", "r762_he", "bmg50_sp", "bmg50_fmj", "bmg50_jhp",
            "bmg50_ap", "bmg50_du", "bmg50_sm", "bmg50_he", "b75", "b75_inc",
            "b75_exp", "g12_bp", "g12_bp_magnum", "g12_bp_slug", "g12", "g12_slug",
            "g12_flechette", "g12_magnum", "g12_explosive", "g12_phosphorus", "g10", "g10_shrapnel",
            "g10_du", "g10_slug", "g10_explosive", "g26_flare", "g26_flare_supply", "g26_flare_weapon",
            "g40_he", "g40_heat", "g40_demo", "g40_inc", "g40_phosphorus", "rocket_micro",
            "rocket_he", "rocket_heat", "rocket_demo", "rocket_inc", "rocket_phosphorus", "flame_diesel",
            "flame_gas", "flame_napalm", "flame_balefire", "capacitor", "capacitor_overcharge", "capacitor_ir",
            "tau_uranium", "coil_tungsten", "coil_ferrouranium", "nuke_standard", "nuke_demo", "nuke_high",
            "nuke_tots", "nuke_hive", "nuke_balefire", "ct_hook", "ct_mortar", "ct_mortar_charge"
    );

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        List<String> ids = variants().stream().map(Variant::id).toList();
        for (String id : CREATIVE_ORDER) {
            if (ids.contains(id)) {
                output.accept(stackFor(this, id));
            }
        }
        for (String id : ids) {
            if (!CREATIVE_ORDER.contains(id)) {
                output.accept(stackFor(this, id));
            }
        }
    }

    public static Bmg50Type bmg50Type(ItemStack stack) {
        StandardAmmoType type = standardType(stack);
        return type.family == AmmoFamily.BMG50 ? Bmg50Type.byId(type.id()) : Bmg50Type.NONE;
    }

    public static StandardAmmoType standardType(ItemStack stack) {
        if (stack.getItem() instanceof StandardAmmoItem ammoItem) {
            return StandardAmmoType.byId(ammoItem.variant(stack).id());
        }
        return StandardAmmoType.NONE;
    }

    public static ItemStack stackFor(Item item, Bmg50Type type) {
        return LegacyVariantItem.stackFor(item, type.id());
    }

    public static ItemStack stackFor(Item item, StandardAmmoType type) {
        return LegacyVariantItem.stackFor(item, type.id());
    }

    public enum AmmoFamily {
        NONE,
        BMG50,
        R556,
        P9,
        TAU,
        ROCKET_ML,
        FLAME,
        NUKE,
        LEGACY_BULLET,
        FLARE,
        P22,
        // appended for the remaining EnumAmmo rounds
        B75,
        G40,
        ENERGY,
        COIL,
        CT
    }

    public enum StandardAmmoType {
        NONE("none", AmmoFamily.NONE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        BMG50_SP("bmg50_sp", AmmoFamily.BMG50, 10.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        BMG50_FMJ("bmg50_fmj", AmmoFamily.BMG50, 8.0F, 0.8F, false, true, 7.0F, 0.10F, 1.25F),
        BMG50_JHP("bmg50_jhp", AmmoFamily.BMG50, 15.0F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        BMG50_AP("bmg50_ap", AmmoFamily.BMG50, 12.5F, 1.25F, true, false, 17.5F, 0.15F, 1.25F),
        BMG50_DU("bmg50_du", AmmoFamily.BMG50, 15.0F, 1.5F, true, false, 21.0F, 0.25F, 1.25F),
        R556_SP("r556_sp", AmmoFamily.R556, 10.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R556_FMJ("r556_fmj", AmmoFamily.R556, 8.0F, 0.8F, false, true, 4.0F, 0.10F, 1.25F),
        R556_JHP("r556_jhp", AmmoFamily.R556, 15.0F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        R556_AP("r556_ap", AmmoFamily.R556, 12.5F, 1.25F, true, false, 10.0F, 0.15F, 1.25F),
        P9_SP("p9_sp", AmmoFamily.P9, 5.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        P9_FMJ("p9_fmj", AmmoFamily.P9, 4.0F, 0.8F, false, true, 2.0F, 0.10F, 1.25F),
        P9_JHP("p9_jhp", AmmoFamily.P9, 7.5F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        P9_AP("p9_ap", AmmoFamily.P9, 6.25F, 1.25F, true, false, 5.0F, 0.15F, 1.25F),
        TAU_URANIUM("tau_uranium", AmmoFamily.TAU, 35.0F, 1.0F, true, false, 0.0F, 0.0F, 1.25F),
        ROCKET_HE("rocket_he", AmmoFamily.ROCKET_ML, 30.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        ROCKET_HEAT("rocket_heat", AmmoFamily.ROCKET_ML, 15.0F, 0.5F, false, true, 5.0F, 0.20F, 1.0F),
        ROCKET_DEMO("rocket_demo", AmmoFamily.ROCKET_ML, 22.5F, 0.75F, false, true, 0.0F, 0.0F, 1.0F),
        ROCKET_INC("rocket_inc", AmmoFamily.ROCKET_ML, 22.5F, 0.75F, false, true, 0.0F, 0.0F, 1.0F),
        ROCKET_PHOSPHORUS("rocket_phosphorus", AmmoFamily.ROCKET_ML, 22.5F, 0.75F, false, true, 0.0F, 0.0F, 1.0F),
        FLAME_DIESEL("flame_diesel", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F), // [weapons batch F1: BulletConfig headshotMult default 1.25]
        // [weapons batch F2] XFactoryCatapult nuke_*: damageMult 1 / 1 / 1 / 0.35 / 0.25 / 2.5, headshotMult default 1.25
        NUKE_DEMO("nuke_demo", AmmoFamily.NUKE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        NUKE_STANDARD("nuke_standard", AmmoFamily.NUKE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        NUKE_HIGH("nuke_high", AmmoFamily.NUKE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        NUKE_TOTS("nuke_tots", AmmoFamily.NUKE, 0.35F, 0.35F, false, true, 0.0F, 0.0F, 1.25F),
        NUKE_HIVE("nuke_hive", AmmoFamily.NUKE, 0.25F, 0.25F, false, true, 0.0F, 0.0F, 1.25F),
        NUKE_BALEFIRE("nuke_balefire", AmmoFamily.NUKE, 2.5F, 2.5F, false, true, 0.0F, 0.0F, 1.25F),
        // Appended so the ids already stored by legacy_bullet entities retain
        // their 1.21.1 ordinals.  These are precisely the first accepted
        // magazine configurations used by the 1.7.10 skeleton gun pools.
        STONE("stone", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        M357_BP("m357_bp", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        M44_BP("m44_bp", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        G12_BP("g12_bp", AmmoFamily.LEGACY_BULLET, 0.0F, 0.09375F, false, true, 0.0F, 0.0F, 1.25F),
        G26_FLARE("g26_flare", AmmoFamily.FLARE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        // XFactory22lr p22_sp (no other users; family moved from LEGACY_BULLET to P22).
        P22_SP("p22_sp", AmmoFamily.P22, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R762_SP("r762_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R762_FMJ("r762_fmj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.8F, false, true, 5.0F, 0.10F, 1.25F),
        // Appended (ordinals are synced/saved by legacy_bullet): XFactory50 bmg50_sm / bmg50_he
        // and XFactory22lr p22_fmj / p22_jhp / p22_ap with their original BulletConfig values.
        BMG50_SM("bmg50_sm", AmmoFamily.BMG50, 0.0F, 2.5F, true, false, 30.0F, 0.35F, 1.25F),
        BMG50_HE("bmg50_he", AmmoFamily.BMG50, 0.0F, 1.75F, false, true, 0.0F, 0.0F, 1.25F),
        P22_FMJ("p22_fmj", AmmoFamily.P22, 0.0F, 0.8F, false, true, 1.0F, 0.10F, 1.25F),
        P22_JHP("p22_jhp", AmmoFamily.P22, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        P22_AP("p22_ap", AmmoFamily.P22, 0.0F, 1.25F, true, false, 2.5F, 0.15F, 1.25F),
        // Remaining GunFactory.EnumAmmo rounds (appended, ordinals are synced by legacy_bullet):
        // damage multiplier, penetration, falloff, threshold negation, armor piercing and headshot
        // multiplier of the 1.7.10 BulletConfig of each round.
        STONE_AP("stone_ap", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, true, true, 0.0F, 0.0F, 1.0F),
        STONE_IRON("stone_iron", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, true, false, 0.0F, 0.0F, 1.0F),
        STONE_SHOT("stone_shot", AmmoFamily.LEGACY_BULLET, 0.0F, 1F / 6F, false, true, 0.0F, 0.0F, 1.0F),
        M357_SP("m357_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        M357_FMJ("m357_fmj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.8F, false, true, 2.0F, 0.1F, 1.25F),
        M357_JHP("m357_jhp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.5F),
        M357_AP("m357_ap", AmmoFamily.LEGACY_BULLET, 0.0F, 1.25F, true, false, 5.0F, 0.15F, 1.25F),
        M357_EXPRESS("m357_express", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, true, true, 2.0F, 0.1F, 1.25F),
        M44_SP("m44_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        M44_FMJ("m44_fmj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.8F, false, true, 3.0F, 0.1F, 1.25F),
        M44_JHP("m44_jhp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.5F),
        M44_AP("m44_ap", AmmoFamily.LEGACY_BULLET, 0.0F, 1.25F, true, false, 7.5F, 0.15F, 1.25F),
        M44_EXPRESS("m44_express", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, true, true, 3.0F, 0.1F, 1.25F),
        R762_JHP("r762_jhp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.5F),
        R762_AP("r762_ap", AmmoFamily.LEGACY_BULLET, 0.0F, 1.25F, true, false, 12.5F, 0.15F, 1.25F),
        R762_DU("r762_du", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, true, false, 15.0F, 0.25F, 1.25F),
        B75("b75", AmmoFamily.B75, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        B75_INC("b75_inc", AmmoFamily.B75, 0.0F, 0.8F, false, true, 0.0F, 0.1F, 1.25F),
        B75_EXP("b75_exp", AmmoFamily.B75, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.25F),
        G12_BP_MAGNUM("g12_bp_magnum", AmmoFamily.LEGACY_BULLET, 0.0F, 0.1875F, false, true, 0.0F, 0.0F, 1.25F),
        G12_BP_SLUG("g12_bp_slug", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        G12("g12", AmmoFamily.LEGACY_BULLET, 0.0F, 0.125F, false, true, 2.0F, 0.0F, 1.25F),
        G12_SLUG("g12_slug", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 4.0F, 0.15F, 1.5F),
        G12_FLECHETTE("g12_flechette", AmmoFamily.LEGACY_BULLET, 0.0F, 0.125F, false, true, 5.0F, 0.2F, 1.25F),
        G12_MAGNUM("g12_magnum", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 4.0F, 0.0F, 1.25F),
        G12_EXPLOSIVE("g12_explosive", AmmoFamily.LEGACY_BULLET, 0.0F, 2.5F, false, true, 0.0F, 0.0F, 1.25F),
        G12_PHOSPHORUS("g12_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.125F, false, true, 0.0F, 0.0F, 1.25F),
        G26_FLARE_SUPPLY("g26_flare_supply", AmmoFamily.FLARE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        G26_FLARE_WEAPON("g26_flare_weapon", AmmoFamily.FLARE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        G40_HE("g40_he", AmmoFamily.G40, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        G40_HEAT("g40_heat", AmmoFamily.G40, 0.0F, 0.5F, false, true, 0.0F, 0.0F, 1.25F),
        G40_DEMO("g40_demo", AmmoFamily.G40, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        G40_INC("g40_inc", AmmoFamily.G40, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        G40_PHOSPHORUS("g40_phosphorus", AmmoFamily.G40, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_GAS("flame_gas", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_NAPALM("flame_napalm", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_BALEFIRE("flame_balefire", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        CAPACITOR("capacitor", AmmoFamily.ENERGY, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        CAPACITOR_OVERCHARGE("capacitor_overcharge", AmmoFamily.ENERGY, 0.0F, 1.0F, true, true, 0.0F, 0.0F, 1.25F),
        CAPACITOR_IR("capacitor_ir", AmmoFamily.ENERGY, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        COIL_TUNGSTEN("coil_tungsten", AmmoFamily.COIL, 0.0F, 1.0F, true, false, 0.0F, 0.0F, 1.25F),
        COIL_FERROURANIUM("coil_ferrouranium", AmmoFamily.COIL, 0.0F, 1.0F, true, false, 0.0F, 0.0F, 1.25F),
        G10("g10", AmmoFamily.LEGACY_BULLET, 0.0F, 0.1F, false, true, 5.0F, 0.0F, 1.25F),
        G10_SHRAPNEL("g10_shrapnel", AmmoFamily.LEGACY_BULLET, 0.0F, 0.1F, false, true, 5.0F, 0.0F, 1.25F),
        G10_DU("g10_du", AmmoFamily.LEGACY_BULLET, 0.0F, 0.25F, true, false, 10.0F, 0.2F, 1.25F),
        G10_SLUG("g10_slug", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, true, true, 10.0F, 0.1F, 1.25F),
        R762_HE("r762_he", AmmoFamily.LEGACY_BULLET, 0.0F, 1.75F, false, true, 0.0F, 0.0F, 1.25F),
        G10_EXPLOSIVE("g10_explosive", AmmoFamily.LEGACY_BULLET, 0.0F, 0.25F, false, true, 0.0F, 0.0F, 1.25F),
        P45_SP("p45_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        P45_FMJ("p45_fmj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.8F, false, true, 2.0F, 0.1F, 1.25F),
        P45_JHP("p45_jhp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.5F),
        P45_AP("p45_ap", AmmoFamily.LEGACY_BULLET, 0.0F, 1.25F, true, false, 5.0F, 0.15F, 1.25F),
        P45_DU("p45_du", AmmoFamily.LEGACY_BULLET, 0.0F, 2.5F, true, false, 15.0F, 0.25F, 1.25F),
        CT_HOOK("ct_hook", AmmoFamily.CT, 0.0F, 1.0F, true, false, 0.0F, 0.0F, 1.25F),
        CT_MORTAR("ct_mortar", AmmoFamily.CT, 0.0F, 2.5F, false, true, 0.0F, 0.0F, 1.25F),
        CT_MORTAR_CHARGE("ct_mortar_charge", AmmoFamily.CT, 0.0F, 5.0F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_MICRO("rocket_micro", AmmoFamily.ROCKET_ML, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [C2] XFactory44 m44_equestrian_pip / m44_equestrian_mn7: projectile data of the two
        // ammo_secret M44_EQUESTRIAN BulletConfigs (setDamage(0F), onImpact set by XFactory44).
        // Not ammo_standard variants; the gun matches them against ammo_secret:m44_equestrian.
        M44_EQUESTRIAN_PIP("m44_equestrian_pip", AmmoFamily.LEGACY_BULLET, 0.0F, 0.0F, false, true, 0.0F, 0.0F, 1.25F),
        M44_EQUESTRIAN_MN7("m44_equestrian_mn7", AmmoFamily.LEGACY_BULLET, 0.0F, 0.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [weapons batch C4] ballistics of the ammo_secret round EnumAmmoSecret.G12_EQUESTRIAN
        // (XFactory12ga g12_equestrian_tkr, setDamage(0F)); not an ammo_standard variant.
        G12_EQUESTRIAN("g12_equestrian", AmmoFamily.LEGACY_BULLET, 0.0F, 0.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [weapons batch D2] XFactory556mm r556_inc_* (gun_g3_zebra): clones of the four .556 configs with the
        // INCENDIARY onImpact (set by XFactory556mm); the gun matches them against the ammo_standard r556 variants.
        R556_INC_SP("r556_inc_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R556_INC_FMJ("r556_inc_fmj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.8F, false, true, 4.0F, 0.10F, 1.25F),
        R556_INC_JHP("r556_inc_jhp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        R556_INC_AP("r556_inc_ap", AmmoFamily.LEGACY_BULLET, 0.0F, 1.25F, true, false, 10.0F, 0.15F, 1.25F),
        // [weapons batch D2] XFactory50 ammo_secret rounds: bmg50_black (spectral, set by XFactory50) and
        // bmg50_equestrian (setDamage(0F), onImpact LAMBDA_BUILDING). Not ammo_standard variants.
        BMG50_BLACK("bmg50_black", AmmoFamily.LEGACY_BULLET, 0.0F, 1.5F, true, false, 30.0F, 0.35F, 3.0F),
        BMG50_EQUESTRIAN("bmg50_equestrian", AmmoFamily.LEGACY_BULLET, 0.0F, 0.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [weapons batch D1] XFactory12ga g12_equestrian_bj (ammo_secret G12_EQUESTRIAN, setDamage(0F), LAMBDA_BOAT),
        // the shredder submunitions (makeShredderSubmunition: clones of the nine-round configs) and the
        // shredder beams (makeShredderConfig: damage = original damageMult * projectilesMax, BulletConfig defaults
        // otherwise). None of them is an ammo_standard variant.
        G12_EQUESTRIAN_BJ("g12_equestrian_bj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.0F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SUB("g12_sub", AmmoFamily.LEGACY_BULLET, 0.0F, 0.125F, false, true, 2.0F, 0.0F, 1.25F),
        G12_SUB_SLUG("g12_sub_slug", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 4.0F, 0.15F, 1.5F),
        G12_SUB_FLECHETTE("g12_sub_flechette", AmmoFamily.LEGACY_BULLET, 0.0F, 0.125F, false, true, 5.0F, 0.2F, 1.25F),
        G12_SUB_MAGNUM("g12_sub_magnum", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 4.0F, 0.0F, 1.25F),
        G12_SUB_EXPLOSIVE("g12_sub_explosive", AmmoFamily.LEGACY_BULLET, 0.0F, 2.5F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SUB_PHOSPHORUS("g12_sub_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.125F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SHREDDER("g12_shredder", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SHREDDER_SLUG("g12_shredder_slug", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SHREDDER_FLECHETTE("g12_shredder_flechette", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SHREDDER_MAGNUM("g12_shredder_magnum", AmmoFamily.LEGACY_BULLET, 0.0F, 2.0F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SHREDDER_EXPLOSIVE("g12_shredder_explosive", AmmoFamily.LEGACY_BULLET, 0.0F, 2.5F, false, true, 0.0F, 0.0F, 1.25F),
        G12_SHREDDER_PHOSPHORUS("g12_shredder_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [weapons batch E1] XFactoryRocket rocket_rpzb / rocket_qd / rocket_ml (clones of rocket_template: HE 1, HEAT 0.5,
        // DEMO/INC/PHOSPHORUS 0.75, BulletConfig defaults otherwise) and rocket_ff (ROCKET_MICRO). Ballistics and lambdas are
        // set by XFactoryRocket; the guns match them against the ammo_standard rocket variants. The existing ROCKET_* types
        // stay the turret_richard rounds. Not ammo_standard variants.
        ROCKET_RPZB_HE("rocket_rpzb_he", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_RPZB_HEAT("rocket_rpzb_heat", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_RPZB_DEMO("rocket_rpzb_demo", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_RPZB_INC("rocket_rpzb_inc", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_RPZB_PHOSPHORUS("rocket_rpzb_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_QD_HE("rocket_qd_he", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_QD_HEAT("rocket_qd_heat", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_QD_DEMO("rocket_qd_demo", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_QD_INC("rocket_qd_inc", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_QD_PHOSPHORUS("rocket_qd_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_ML_HE("rocket_ml_he", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_ML_HEAT("rocket_ml_heat", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_ML_DEMO("rocket_ml_demo", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_ML_INC("rocket_ml_inc", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_ML_PHOSPHORUS("rocket_ml_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_FF("rocket_ff", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [weapons batch F4] XFactoryFolly folly_sm (SUBATOMIC beam, spectral, penetrating; ballistics set by XFactoryFolly) and
        // folly_nuke (ammo_secret FOLLY_SM / FOLLY_NUKE); XFactory35800 p35800 / p35800_bl (ammo_secret P35_800 / P35_800_BL:
        // beams, armor piercing 0.5, threshold negation 50). BulletConfig defaults otherwise. Not ammo_standard variants.
        FOLLY_SM("folly_sm", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, true, true, 0.0F, 0.0F, 1.25F),
        FOLLY_NUKE("folly_nuke", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        P35_800("p35_800", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 50.0F, 0.5F, 1.25F),
        P35_800_BL("p35_800_bl", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 50.0F, 0.5F, 1.25F),
        // [weapons batch F1] XFactoryFlamer clones of flame_diesel / flame_gas / flame_napalm / flame_balefire (flame_nograv,
        // flame_nograv_bf, flame_topaz_*, flame_daybreaker_*; BulletConfig headshotMult 1.25). Ballistics and lambdas are set by
        // XFactoryFlamer; the guns match them against the ammo_standard flame variants (CloneBulletConfig). Not ammo_standard variants.
        FLAME_NOGRAV("flame_nograv", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_NOGRAV_BF("flame_nograv_bf", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_TOPAZ_DIESEL("flame_topaz_diesel", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_TOPAZ_GAS("flame_topaz_gas", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_TOPAZ_NAPALM("flame_topaz_napalm", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_TOPAZ_BALEFIRE("flame_topaz_balefire", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_DAYBREAKER_DIESEL("flame_daybreaker_diesel", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_DAYBREAKER_GAS("flame_daybreaker_gas", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_DAYBREAKER_NAPALM("flame_daybreaker_napalm", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FLAME_DAYBREAKER_BALEFIRE("flame_daybreaker_balefire", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [weapons batch F3] XFactoryTool fext_water / fext_foam / fext_sand (ammo_fireext water/foam/sand tanks, BulletConfig
        // defaults; ballistics and lambdas are set by XFactoryTool). Not ammo_standard variants; the ct_* entries above stay.
        FEXT_WATER("fext_water", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FEXT_FOAM("fext_foam", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        FEXT_SAND("fext_sand", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        // [parity H2: grenade_universal] ItemGrenadeFilling.fragmentation / pellets / pellets_heavy / laser (BulletConfig
        // defaults; fragmentation and laser set threshold negation 5 / 10). Ballistics and lambdas are set by
        // UniversalGrenadeFillings. Not ammo items.
        GRENADE_FRAGMENTATION("grenade_fragmentation", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 5.0F, 0.0F, 1.25F),
        GRENADE_PELLETS("grenade_pellets", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        GRENADE_PELLETS_HEAVY("grenade_pellets_heavy", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        GRENADE_LASER("grenade_laser", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 10.0F, 0.0F, 1.25F),
        // [parity c1] XFactoryRocket rocket_ncrpa (makeRPZB clones) / rocket_ncrpa_steer (makeNCR: life 400, always steered):
        // the shoulder rockets of the NCR power armor ranged controller (gun_pa_ranged). Appended last so no ordinal moves.
        ROCKET_NCRPA_HE("rocket_ncrpa_he", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_HEAT("rocket_ncrpa_heat", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_DEMO("rocket_ncrpa_demo", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_INC("rocket_ncrpa_inc", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_PHOSPHORUS("rocket_ncrpa_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_STEER_HE("rocket_ncrpa_steer_he", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_STEER_HEAT("rocket_ncrpa_steer_heat", AmmoFamily.LEGACY_BULLET, 0.0F, 0.5F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_STEER_DEMO("rocket_ncrpa_steer_demo", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_STEER_INC("rocket_ncrpa_steer_inc", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        ROCKET_NCRPA_STEER_PHOSPHORUS("rocket_ncrpa_steer_phosphorus", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        // [parity d2] GunFactory ammo_debug / ammo_debug_shot (item ammo_debug; BulletConfig defaults, spread / projectiles /
        // ricochet angle are set by the port's GunFactory). Not ammo_standard variants. Appended last so no ordinal moves.
        DEBUG("debug", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        DEBUG_SHOT("debug_shot", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F);

        private final String id;
        private final AmmoFamily family;
        private final float damage;
        private final float damageMultiplier;
        private final boolean penetrates;
        private final boolean damageFalloffByPenetration;
        private final float armorThresholdNegation;
        private final float armorPiercingPercent;
        private final float headshotMultiplier;

        StandardAmmoType(
                String id,
                AmmoFamily family,
                float damage,
                float damageMultiplier,
                boolean penetrates,
                boolean damageFalloffByPenetration,
                float armorThresholdNegation,
                float armorPiercingPercent,
                float headshotMultiplier
        ) {
            this.id = id;
            this.family = family;
            this.damage = damage;
            this.damageMultiplier = damageMultiplier;
            this.penetrates = penetrates;
            this.damageFalloffByPenetration = damageFalloffByPenetration;
            this.armorThresholdNegation = armorThresholdNegation;
            this.armorPiercingPercent = armorPiercingPercent;
            this.headshotMultiplier = headshotMultiplier;
        }

        public String id() {
            return id;
        }

        public AmmoFamily family() {
            return family;
        }

        public float damage() {
            return damage;
        }

        public float damageMultiplier() {
            return damageMultiplier;
        }

        public boolean penetrates() {
            return penetrates;
        }

        public boolean damageFalloffByPenetration() {
            return damageFalloffByPenetration;
        }

        public float armorThresholdNegation() {
            return armorThresholdNegation;
        }

        public float armorPiercingPercent() {
            return armorPiercingPercent;
        }

        public float headshotMultiplier() {
            return headshotMultiplier;
        }

        /*
         * 1.7.10 BulletConfig fields that the guns of a caliber set in their XFactory init
         * (setProjectiles, setSpread, setRicochetAngle/Count, setVel, setGrav, setLife,
         * setKnockback, setSelfDamageDelay, setBlackPowder, setSpectral, setOnImpact,
         * setOnUpdate). Null keeps the previous per-family defaults below.
         */
        private Integer projectilesMinOverride;
        private Integer projectilesMaxOverride;
        private Float spreadOverride;
        private Float ricochetAngleOverride;
        private Integer ricochetCountOverride;
        private Double velocityOverride;
        private Double gravityOverride;
        private Integer lifeOverride;
        private Float knockbackOverride;
        private Integer selfDamageDelayOverride;
        private boolean blackPowder;
        private boolean spectral;
        private BulletImpact onImpact;
        private BulletUpdate onUpdate;

        public StandardAmmoType setProjectiles(int amount) { return setProjectiles(amount, amount); }
        public StandardAmmoType setProjectiles(int min, int max) { this.projectilesMinOverride = min; this.projectilesMaxOverride = max; return this; }
        public StandardAmmoType setSpread(float spread) { this.spreadOverride = spread; return this; }
        public StandardAmmoType setRicochetAngle(float angle) { this.ricochetAngleOverride = angle; return this; }
        public StandardAmmoType setRicochetCount(int count) { this.ricochetCountOverride = count; return this; }
        public StandardAmmoType setVel(double velocity) { this.velocityOverride = velocity; return this; }
        public StandardAmmoType setGrav(double gravity) { this.gravityOverride = gravity; return this; }
        public StandardAmmoType setLife(int life) { this.lifeOverride = life; return this; }
        public StandardAmmoType setKnockback(float knockback) { this.knockbackOverride = knockback; return this; }
        public StandardAmmoType setSelfDamageDelay(int delay) { this.selfDamageDelayOverride = delay; return this; }
        public StandardAmmoType setBlackPowder(boolean blackPowder) { this.blackPowder = blackPowder; return this; }
        public StandardAmmoType setSpectral(boolean spectral) { this.spectral = spectral; return this; }
        public StandardAmmoType setOnImpact(BulletImpact onImpact) { this.onImpact = onImpact; return this; }
        public StandardAmmoType setOnUpdate(BulletUpdate onUpdate) { this.onUpdate = onUpdate; return this; }
        // [weapons batch D1] BulletConfig.onRicochet / onEntityHit replacements; null keeps the port's standard handling.
        // Return true when the bullet stops this tick (it is then discarded if still alive).
        private BulletImpact onRicochet;
        private BulletImpact onEntityHit;
        public StandardAmmoType setOnRicochet(BulletImpact onRicochet) { this.onRicochet = onRicochet; return this; }
        public StandardAmmoType setOnEntityHit(BulletImpact onEntityHit) { this.onEntityHit = onEntityHit; return this; }
        public BulletImpact onRicochet() { return onRicochet; }
        public BulletImpact onEntityHit() { return onEntityHit; }
        // [weapons batch F3] BulletConfig.onUpdate ran on both sides in 1.7.10 (EntityBulletBaseMK4#onUpdate); the port's
        // onUpdate is server-only, so the client half of a lambda (fext_* block dust, ct_hook noCulling) goes here.
        private BulletUpdate onClientUpdate;
        public StandardAmmoType setOnClientUpdate(BulletUpdate onClientUpdate) { this.onClientUpdate = onClientUpdate; return this; }
        public BulletUpdate onClientUpdate() { return onClientUpdate; }

        public boolean blackPowder() { return blackPowder; }
        /** BulletConfig.isSpectral: passes through blocks. */
        public boolean spectral() { return spectral; }
        public BulletImpact onImpact() { return onImpact; }
        public BulletUpdate onUpdate() { return onUpdate; }

        /** BulletConfig.knockbackMult: 0.1 by default, setKnockback(0F) for every .22 round. */
        public float knockbackMultiplier() {
            if (knockbackOverride != null) return knockbackOverride;
            return family == AmmoFamily.P22 ? 0.0F : 0.1F;
        }

        /** BulletConfig.onImpact = XFactory50.LAMBDA_STANDARD_EXPLODE (Lego.tinyExplode, range 2). */
        public boolean explodesOnImpact() {
            return this == BMG50_HE;
        }

        public float ricochetAngle() {
            return ricochetAngleOverride != null ? ricochetAngleOverride : 5.0F;
        }

        public int maxRicochetCount() {
            return ricochetCountOverride != null ? ricochetCountOverride : 2;
        }

        /** BulletConfig projectile count from the 1.7.10 first magazine entry. */
        public int projectileCount() {
            if (projectilesMinOverride != null) return projectilesMinOverride;
            return this == G12_BP ? 8 : 1;
        }

        /** BulletConfig.projectilesMax. */
        public int projectileCountMax() {
            return projectilesMaxOverride != null ? projectilesMaxOverride : projectileCount();
        }

        /** BulletConfig spread before the receiver's own spread terms. */
        public float projectileSpread() {
            if (spreadOverride != null) return spreadOverride;
            return switch (this) {
                case STONE -> 0.025F;
                case G12_BP -> 0.035F;
                default -> 0.0F;
            };
        }

        public double projectileSpeed() {
            if (velocityOverride != null) return velocityOverride;
            return switch (family) {
                case ROCKET_ML -> 0.0D;
                case FLAME -> 1.0D;
                case FLARE -> 2.0D;
                default -> 10.0D;
            };
        }

        public double projectileGravity() {
            if (gravityOverride != null) return gravityOverride;
            return switch (family) {
                case FLAME -> 0.02D;
                case FLARE -> 0.015D;
                default -> 0.0D;
            };
        }

        public int projectileLifetime() {
            if (lifeOverride != null) return lifeOverride;
            return switch (family) {
                case FLAME, FLARE -> 100;
                case ROCKET_ML -> 300;
                default -> 30;
            };
        }

        public int selfDamageDelay() {
            if (selfDamageDelayOverride != null) return selfDamageDelayOverride;
            return family == AmmoFamily.FLAME ? 20 : 2;
        }

        public static StandardAmmoType byId(String id) {
            for (StandardAmmoType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    /**
     * BulletConfig.onImpact. Runs on the server before the standard entity/block handling.
     * Return true when the bullet is consumed (it is then discarded).
     */
    @FunctionalInterface
    public interface BulletImpact {
        boolean onImpact(com.atomiclegacy.entity.LegacyBulletEntity bullet, net.minecraft.world.phys.HitResult hit);
    }

    /** BulletConfig.onUpdate, called every server tick after the bullet moved. */
    @FunctionalInterface
    public interface BulletUpdate {
        void onUpdate(com.atomiclegacy.entity.LegacyBulletEntity bullet);
    }

    public enum Bmg50Type {
        NONE(StandardAmmoType.NONE),
        SP(StandardAmmoType.BMG50_SP),
        FMJ(StandardAmmoType.BMG50_FMJ),
        JHP(StandardAmmoType.BMG50_JHP),
        AP(StandardAmmoType.BMG50_AP),
        DU(StandardAmmoType.BMG50_DU);

        private final StandardAmmoType standardType;

        Bmg50Type(StandardAmmoType standardType) {
            this.standardType = standardType;
        }

        public String id() {
            return this.standardType.id();
        }

        public float damage() {
            return this.standardType.damage();
        }

        public boolean penetrates() {
            return this.standardType.penetrates();
        }

        public StandardAmmoType standardType() {
            return this.standardType;
        }

        public static Bmg50Type byId(String id) {
            for (Bmg50Type type : values()) {
                if (type.id().equals(id)) {
                    return type;
                }
            }
            return NONE;
        }

        public static Bmg50Type byOrdinal(int ordinal) {
            Bmg50Type[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
        }
    }
}

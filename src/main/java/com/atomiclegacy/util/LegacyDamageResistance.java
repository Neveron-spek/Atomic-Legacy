package com.atomiclegacy.util;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyNuclearCreeperEntity;
import com.atomiclegacy.items.PoweredArmorFSBItem;
import com.atomiclegacy.registry.HbmDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [parity I1] Port of 1.7.10 com.hbm.util.DamageResistanceHandler (initDefaults table, typeToCategory, getDTDR,
 * calculateDamage, onEntityAttacked/onEntityDamaged, addInfo). It is the single source of DT/DR values; the sedna
 * bullet code (LegacyProjectileUtil, BeamBulletConfig) reads this table and applies the formula itself with its
 * piercing values, so the event handler skips the pre-applied sedna_* damage types.
 *
 * Mapping of 1.21.1 damage sources to the 1.7.10 notions:
 * <ul>
 * <li>isDamageAbsolute: #minecraft:bypasses_effects (vanilla: starve) and the port types that were absolute in
 * ModDamageSource (see {@link #ABSOLUTE}).</li>
 * <li>isUnblockable: #minecraft:bypasses_armor plus mud_poisoning and tau (setDamageBypassesArmor in 1.7.10).</li>
 * <li>damageType: DamageType.msgId lowercased (fall, drown, onFire -> onfire, laser ...).</li>
 * <li>EXPL: #is_explosion, FIRE: #is_fire, PHYS: #is_projectile, cactus and every source with an attacker or direct
 * entity (EntityDamageSource) except falling blocks, EN: msgId laser/plasma/microwave/subatomic/electric.</li>
 * </ul>
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class LegacyDamageResistance {
    public static final String CATEGORY_EXPLOSION = "EXPL";
    public static final String CATEGORY_FIRE = "FIRE";
    public static final String CATEGORY_PHYSICAL = "PHYS";
    public static final String CATEGORY_ENERGY = "EN";

    /** Currently cached DT reduction / armor piercing %, DamageResistanceHandler.currentPDT / currentPDR. */
    public static float currentPDT = 0F;
    public static float currentPDR = 0F;

    private static final Map<String, Stats> ITEM_STATS = new HashMap<>();
    private static final Map<String, Stats> SET_STATS = new HashMap<>();
    /** item path -> first registered set (itemInfoSet, "only show one set for now"). */
    private static final Map<String, String[]> ITEM_INFO_SET = new HashMap<>();
    private static final Stats CREEPER = new Stats().addCategory(CATEGORY_EXPLOSION, 2F, 0.25F);
    private static final Stats CREEPER_NUCLEAR = new Stats().addCategory(CATEGORY_EXPLOSION, 5F, 0.35F);

    /** Port damage types whose 1.7.10 ModDamageSource was setDamageIsAbsolute(). */
    private static final List<ResourceKey<DamageType>> ABSOLUTE = List.of(
            HbmDamageTypes.DIGAMMA, HbmDamageTypes.BLACK_HOLE, HbmDamageTypes.TURBOFAN, HbmDamageTypes.METEORITE,
            HbmDamageTypes.BOXCAR, HbmDamageTypes.BOAT, HbmDamageTypes.TAINT, HbmDamageTypes.BROADCAST,
            HbmDamageTypes.BANG, HbmDamageTypes.CLOUD, HbmDamageTypes.LEAD, HbmDamageTypes.ELECTRICITY,
            HbmDamageTypes.MONOXIDE, HbmDamageTypes.ASBESTOS, HbmDamageTypes.BLACK_LUNG, HbmDamageTypes.MKU,
            HbmDamageTypes.OVERDOSE, HbmDamageTypes.MICROWAVE,
            HbmDamageTypes.AMS, HbmDamageTypes.AMS_CORE); // [parity M3: dfc]
    private static final ResourceKey<DamageType> BUILDING = ResourceKey.create(
            net.minecraft.core.registries.Registries.DAMAGE_TYPE, AtomicLegacy.id("building"));
    /** Damage types whose DT/DR was already applied by the sedna bullet code with its piercing values. */
    private static final List<ResourceKey<DamageType>> PRE_APPLIED = List.of(
            HbmDamageTypes.SEDNA_PHYSICAL, HbmDamageTypes.SEDNA_EXPLOSIVE, HbmDamageTypes.SEDNA_FIRE,
            HbmDamageTypes.SEDNA_LASER, HbmDamageTypes.SEDNA_PLASMA, HbmDamageTypes.SEDNA_ELECTRIC,
            HbmDamageTypes.SEDNA_SUBATOMIC);

    static {
        initDefaults();
    }

    private LegacyDamageResistance() {
    }

    /** DamageResistanceHandler.initDefaults, values and order unchanged. */
    private static void initDefaults() {
        ITEM_STATS.put("jackt", new Stats().addCategory(CATEGORY_PHYSICAL, 1F, 0.20F));
        ITEM_STATS.put("jackt2", new Stats().addCategory(CATEGORY_PHYSICAL, 2F, 0.25F));

        registerSet("steel", new Stats().addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet("titanium", new Stats().addCategory(CATEGORY_PHYSICAL, 3F, 0.1F));
        registerSet("alloy", new Stats().addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet("cobalt", new Stats().addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet("starmetal", new Stats().addCategory(CATEGORY_PHYSICAL, 3F, 0.25F).setOther(1F, 0.1F));
        registerSet("zirconium_legs", "zirconium_legs", "zirconium_legs", "zirconium_legs", new Stats().setOther(0F, 1F));
        registerSet("dnt", new Stats());
        registerSet("cmb", new Stats().addCategory(CATEGORY_PHYSICAL, 5F, 0.5F).setOther(5F, 0.25F));
        registerSet("schrabidium", new Stats().addCategory(CATEGORY_PHYSICAL, 10F, 0.65F).setOther(5F, 0.5F));
        registerSet("robes", new Stats());

        registerSet("security", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        registerSet("steamsuit", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.25F)
                .addExact("fall", 5F, 0.25F)
                .setOther(0F, 0.1F));
        registerSet("dieselsuit", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 1F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 2F, 0.15F)
                .setOther(0F, 0.1F));
        registerSet("t51", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(0F, 0.1F));
        registerSet("ajr", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(0F, 0.15F));
        registerSet("ajro", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(0F, 0.15F));
        registerSet("rpa", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 50F, 0.75F)
                .addCategory(CATEGORY_FIRE, 25F, 0.9F)
                .addCategory(CATEGORY_EXPLOSION, 15F, 0.25F)
                .addCategory(CATEGORY_ENERGY, 30F, 0.8F)
                .addExact("fall", 0F, 1F)
                .setOther(15F, 0.45F));
        registerSet("ncrpa", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 25F, 0.65F)
                .addCategory(CATEGORY_FIRE, 10F, 0.9F)
                .addCategory(CATEGORY_EXPLOSION, 15F, 0.25F)
                .addCategory(CATEGORY_ENERGY, 10F, 0.5F)
                .addExact("fall", 0F, 1F)
                .setOther(15F, 0.25F));
        Stats bj = new Stats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 2.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 10F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(2F, 0.15F);
        registerSet("bj", bj);
        registerSet("bj_helmet", "bj_plate_jetpack", "bj_legs", "bj_boots", bj);
        registerSet("envsuit", new Stats()
                .addCategory(CATEGORY_FIRE, 2F, 0.75F)
                .addExact("drown", 0F, 1F)
                .addExact("fall", 5F, 0.75F)
                .setOther(0F, 0.1F));
        registerSet("hev", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.25F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("onFire", 0F, 1F)
                .addExact("fall", 10F, 0F)
                .setOther(2F, 0.25F));
        registerSet("bismuth", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(2F, 0.25F));
        registerSet("fau", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 100F, 0.99F)
                .addCategory(CATEGORY_EXPLOSION, 50F, 0.95F)
                .addCategory(CATEGORY_FIRE, 100F, 1F)
                .addExact("LASER", 25F, 0.95F)
                .addExact("fall", 0F, 1F)
                .setOther(100F, 0.99F));
        registerSet("dns", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 1000F, 1F)
                .addCategory(CATEGORY_EXPLOSION, 100F, 0.99F)
                .addCategory(CATEGORY_FIRE, 0F, 1F)
                .setOther(1000F, 1F));
        registerSet("taurun", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0F, 0.25F)
                .addCategory(CATEGORY_EXPLOSION, 0F, 0.25F)
                .addExact("fall", 4F, 0.5F)
                .setOther(2F, 0.1F));
        registerSet("trenchmaster", new Stats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("LASER", 15F, 0.9F)
                .addExact("fall", 10F, 0.5F)
                .setOther(5F, 0.25F));

        registerSet("euphemium", new Stats().setOther(1_000_000F, 1F));

        registerSet("hazmat", new Stats());
        registerSet("hazmat_helmet_red", "hazmat_plate_red", "hazmat_legs_red", "hazmat_boots_red", new Stats());
        registerSet("hazmat_helmet_grey", "hazmat_plate_grey", "hazmat_legs_grey", "hazmat_boots_grey", new Stats());
        registerSet("liquidator", new Stats());
        registerSet("hazmat_paa", new Stats());
        registerSet("asbestos", new Stats().addCategory(CATEGORY_FIRE, 10F, 0.9F));
    }

    private static void registerSet(String prefix, Stats stats) {
        registerSet(prefix + "_helmet", prefix + "_plate", prefix + "_legs", prefix + "_boots", stats);
    }

    private static void registerSet(String helmet, String plate, String legs, String boots, Stats stats) {
        String[] set = {helmet, plate, legs, boots};
        SET_STATS.put(setKey(helmet, plate, legs, boots), stats);
        for (String piece : set) {
            ITEM_INFO_SET.putIfAbsent(piece, set);
        }
    }

    private static String setKey(String helmet, String plate, String legs, String boots) {
        return helmet + '|' + plate + '|' + legs + '|' + boots;
    }

    private static String path(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AtomicLegacy.MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
    }

    /** Set resistances of the worn quartet, or null. Mirrors setStats.get(wornSet): item identity only. */
    public static Stats wornSetStats(LivingEntity entity) {
        String helmet = path(entity.getItemBySlot(EquipmentSlot.HEAD));
        if (helmet.isEmpty()) {
            return null;
        }
        return SET_STATS.get(setKey(helmet, path(entity.getItemBySlot(EquipmentSlot.CHEST)),
                path(entity.getItemBySlot(EquipmentSlot.LEGS)), path(entity.getItemBySlot(EquipmentSlot.FEET))));
    }

    /** Registered set stats by its four item ids (diagnostics). */
    public static Stats setStats(String helmet, String plate, String legs, String boots) {
        return SET_STATS.get(setKey(helmet, plate, legs, boots));
    }

    public static Stats itemStats(String itemPath) {
        return ITEM_STATS.get(itemPath);
    }

    // ---- damage source classification ----

    public static boolean isPreApplied(DamageSource source) {
        for (ResourceKey<DamageType> key : PRE_APPLIED) {
            if (source.is(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAbsolute(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS) && !source.is(HbmDamageTypes.NUCLEAR_BLAST)) {
            return true;
        }
        if (source.is(BUILDING)) {
            return true;
        }
        for (ResourceKey<DamageType> key : ABSOLUTE) {
            if (source.is(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnblockable(DamageSource source) {
        if (source.is(HbmDamageTypes.NUCLEAR_BLAST) || source.is(HbmDamageTypes.SHRAPNEL)) {
            return false; // ModDamageSource.nuclearBlast: setExplosion(), shrapnel: setProjectile() only
        }
        return source.is(DamageTypeTags.BYPASSES_ARMOR)
                || source.is(HbmDamageTypes.MUD_POISONING)
                || source.is(HbmDamageTypes.TAU);
    }

    public static String exactKey(DamageSource source) {
        return source.getMsgId().toLowerCase(Locale.US);
    }

    /** DamageResistanceHandler.typeToCategory. */
    public static String typeToCategory(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return CATEGORY_EXPLOSION;
        if (source.is(DamageTypeTags.IS_FIRE)) return CATEGORY_FIRE;
        if (source.is(DamageTypeTags.IS_PROJECTILE) || isLegacyProjectile(source)) return CATEGORY_PHYSICAL;
        String type = exactKey(source);
        if (isEnergyClass(type)) return CATEGORY_ENERGY;
        if (source.is(DamageTypes.CACTUS)) return CATEGORY_PHYSICAL;
        if (isEntityDamageSource(source)) return CATEGORY_PHYSICAL;
        return source.getMsgId();
    }

    /** ModDamageSource.rubble / shrapnel / suicide were setProjectile() but are not in #is_projectile of the port. */
    private static boolean isLegacyProjectile(DamageSource source) {
        return source.is(HbmDamageTypes.RUBBLE) || source.is(HbmDamageTypes.SHRAPNEL);
    }

    private static boolean isEnergyClass(String lowerType) {
        return lowerType.equals("laser") || lowerType.equals("plasma") || lowerType.equals("microwave")
                || lowerType.equals("subatomic") || lowerType.equals("electric");
    }

    /** 1.7.10 "source instanceof EntityDamageSource": anvils and falling blocks were plain DamageSources. */
    private static boolean isEntityDamageSource(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (source.getEntity() == null && direct == null) {
            return false;
        }
        return !(source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.FALLING_STALACTITE));
    }

    /** Category of a sedna DamageClass name (BulletConfig.getDamage flags + typeToCategory). */
    public static String sednaCategory(String dmgClass) {
        String type = dmgClass.toLowerCase(Locale.US);
        return switch (type) {
            case "explosive" -> CATEGORY_EXPLOSION;
            case "fire" -> CATEGORY_FIRE;
            case "physical" -> CATEGORY_PHYSICAL;
            default -> isEnergyClass(type) ? CATEGORY_ENERGY : type;
        };
    }

    // ---- DT/DR ----

    /** DamageResistanceHandler.getDTDR for a sedna DamageClass source (not unblockable). {DT, DR} */
    public static float[] getDTDR(LivingEntity entity, String dmgClass) {
        return getDTDR(entity, dmgClass.toLowerCase(Locale.US), sednaCategory(dmgClass), false);
    }

    public static float[] getDTDR(LivingEntity entity, DamageSource source) {
        return getDTDR(entity, exactKey(source), typeToCategory(source), isUnblockable(source));
    }

    /**
     * DamageResistanceHandler.getDTDR: worn set + per-item stats + innate entity class stats.
     * IResistanceProvider (glyphids) is handled by the entity classes of the port.
     */
    public static float[] getDTDR(LivingEntity entity, String exact, String category, boolean unblockable) {
        float dt = 0;
        float dr = 0;

        // [parity A2: glyphid] IResistanceProvider.getCurrentDTDR (EntityGlyphid armor threshold / resistance)
        if (entity instanceof com.atomiclegacy.entity.GlyphidEntity glyphid) {
            float[] res = glyphid.legacyCurrentDTDR(exact, category, unblockable);
            dt += res[0];
            dr += res[1];
        }

        Stats set = wornSetStats(entity);
        if (set != null) {
            Resistance res = set.getResistance(exact, category, unblockable);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            Stats stats = ITEM_STATS.get(path(entity.getItemBySlot(slot)));
            if (stats == null) continue;
            Resistance res = stats.getResistance(exact, category, unblockable);
            if (res == null) continue;
            dt += res.threshold;
            dr += res.resistance;
        }

        Stats innate = entity.getClass() == Creeper.class ? CREEPER
                : entity.getClass() == LegacyNuclearCreeperEntity.class ? CREEPER_NUCLEAR : null;
        if (innate != null) {
            Resistance res = innate.getResistance(exact, category, unblockable);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }

        return new float[] {dt, dr};
    }

    /** DamageResistanceHandler.calculateDamage on precomputed {DT, DR}. */
    public static float calculateDamage(float[] vals, float amount, float pierceDT, float pierce) {
        float dt = vals[0];
        float dr = vals[1];
        dt = Math.max(0F, dt - pierceDT);
        if (dt >= amount) return 0F;
        amount -= dt;
        dr *= Mth.clamp(1F - pierce, 0F, 2F);
        return amount * (1F - dr);
    }

    public static float calculateDamage(LivingEntity entity, DamageSource source, float amount, float pierceDT, float pierce) {
        if (isAbsolute(source)) return amount;
        return calculateDamage(getDTDR(entity, source), amount, pierceDT, pierce);
    }

    /** DamageResistanceHandler.onEntityAttacked condition on precomputed {DT, DR}. */
    public static boolean cancelsAttack(float[] vals, float amount, float pierceDT, float pierce) {
        float dt = vals[0] - pierceDT;
        float dr = vals[1] - pierce;
        return (dt > 0 && dt >= amount) || dr >= 1F;
    }

    public static void setup(float dt, float dr) {
        currentPDT = dt;
        currentPDR = dr;
    }

    public static void reset() {
        currentPDT = 0;
        currentPDR = 0;
    }

    /**
     * onEntityAttacked (LivingAttackEvent) followed by onEntityDamaged (LivingHurtEvent); NeoForge 1.21.1 exposes
     * both stages as LivingIncomingDamageEvent.
     */
    public static void onIncomingDamage(LivingEntity entity, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (isPreApplied(source)) {
            return;
        }
        float amount = event.getAmount();
        boolean absolute = isAbsolute(source);

        if (!absolute) {
            float[] vals = getDTDR(entity, source);
            if (cancelsAttack(vals, amount, currentPDT, currentPDR)) {
                event.setCanceled(true); // EntityDamageUtil.damageArmorNT is empty in 1.7.10
                return;
            }
        }

        if (exactKey(source).equals("electric")
                && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PoweredArmorFSBItem) {
            amount *= 5;
        }
        if (!absolute) {
            amount = calculateDamage(getDTDR(entity, source), amount, currentPDT, currentPDR);
        }
        event.setAmount(amount);
    }

    /** Players run the handler from RadiationEvents.applyHbmDamageProtection (shared with the nuclear blast replay). */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player) {
            return;
        }
        onIncomingDamage(entity, event);
    }

    // ---- tooltip (DamageResistanceHandler.addInfo) ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemTooltip(ItemTooltipEvent event) {
        addInfo(event.getItemStack(), event.getToolTip());
    }

    public static void addInfo(ItemStack stack, List<Component> desc) {
        String item = path(stack);
        if (item.isEmpty()) {
            return;
        }

        String[] set = ITEM_INFO_SET.get(item);
        if (set != null) {
            Stats stats = SET_STATS.get(setKey(set[0], set[1], set[2], set[3]));
            List<Component> toAdd = stats == null ? List.of() : stats.describe();
            if (!toAdd.isEmpty()) {
                desc.add(Component.translatable("damage.inset").withStyle(ChatFormatting.DARK_PURPLE));
                for (String piece : set) {
                    Item pieceItem = BuiltInRegistries.ITEM.get(AtomicLegacy.id(piece));
                    desc.add(Component.literal("  ").append(new ItemStack(pieceItem).getHoverName())
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
                desc.addAll(toAdd);
            }
        }

        Stats stats = ITEM_STATS.get(item);
        if (stats != null) {
            List<Component> toAdd = stats.describe();
            if (!toAdd.isEmpty()) {
                desc.add(Component.translatable("damage.item").withStyle(ChatFormatting.DARK_PURPLE));
                desc.addAll(toAdd);
            }
        }
    }

    public static final class Stats {
        // HashMap like the original: the tooltip order is the HashMap iteration order.
        public final HashMap<String, Resistance> exactResistances = new HashMap<>();
        public final HashMap<String, Resistance> categoryResistances = new HashMap<>();
        public Resistance otherResistance;

        public Resistance getResistance(String exactType, String category, boolean unblockable) {
            Resistance exact = exactResistances.get(exactType);
            if (exact != null) return exact;
            Resistance cat = categoryResistances.get(category);
            if (cat != null) return cat;
            return unblockable ? null : otherResistance;
        }

        Stats addExact(String type, float threshold, float resistance) {
            exactResistances.put(type.toLowerCase(Locale.US), new Resistance(threshold, resistance));
            return this;
        }

        Stats addCategory(String type, float threshold, float resistance) {
            categoryResistances.put(type, new Resistance(threshold, resistance));
            return this;
        }

        Stats setOther(float threshold, float resistance) {
            otherResistance = new Resistance(threshold, resistance);
            return this;
        }

        List<Component> describe() {
            List<Component> lines = new ArrayList<>();
            for (Map.Entry<String, Resistance> entry : categoryResistances.entrySet()) {
                lines.add(line("damage.category." + entry.getKey(), entry.getValue()));
            }
            for (Map.Entry<String, Resistance> entry : exactResistances.entrySet()) {
                lines.add(line("damage.exact." + entry.getKey(), entry.getValue()));
            }
            if (otherResistance != null) {
                lines.add(line("damage.other", otherResistance));
            }
            return lines;
        }

        private static Component line(String key, Resistance res) {
            return Component.translatable(key)
                    .append(": " + res.threshold + "/" + ((int) (res.resistance * 100)) + "%")
                    .withStyle(ChatFormatting.GRAY);
        }
    }

    public static final class Resistance {
        public final float threshold;
        public final float resistance;

        public Resistance(float threshold, float resistance) {
            this.threshold = threshold;
            this.resistance = resistance;
        }
    }
}

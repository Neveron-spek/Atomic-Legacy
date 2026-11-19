package com.atomiclegacy.util;

import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.radiation.RadiationEvents;
import com.atomiclegacy.registry.HbmMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Port of 1.7.10 com.hbm.util.ContaminationUtil (commit 936c60c).
 *
 * <p>[parity J1] contaminate(), calculateRadiationMod(), isRadImmune(), the armor checks used by the
 * ContaminationType switch and the Geiger/dosimeter chat readouts follow the original line by line. The
 * original only knows HazardType RADIATION and DIGAMMA.</p>
 */
public final class ContaminationUtil {
    /** 1.7.10 Rad-X: HazmatRegistry.getResistance(player) += 0.2F. */
    public static final float RADX_RESISTANCE = 0.2F;

    private ContaminationUtil() {
    }

    public enum HazardType {
        RADIATION,
        DIGAMMA
    }

    public enum ContaminationType {
        FARADAY,    // preventable by metal armor
        HAZMAT,     // preventable by hazmat
        HAZMAT2,    // preventable by heavy hazmat
        DIGAMMA,    // preventable by fau armor or stability
        DIGAMMA2,   // preventable by robes
        CREATIVE,   // preventable by creative mode, armor resistance still applies to radiation
        RAD_BYPASS, // same as creative but without the radiation resistance calculation
        NONE        // not preventable
    }

    /**
     * Applies asbestos exposure using the exact 1.7.10 guards: non-living
     * entities, creative players, and the first 200 player ticks are immune;
     * a protected head damages its installed filter instead of accumulating
     * asbestos.
     */
    public static void applyAsbestos(Entity entity, int amount) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player
                && (player.isCreative() || living.tickCount < 200)) {
            return;
        }

        if (HbmArmorProtection.hasHeadProtection(
                living, HbmArmorProtection.HazardClass.PARTICLE_FINE)) {
            HbmArmorProtection.damageInstalledFilter(living, amount);
            return;
        }

        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        hazards.addAsbestos(living, amount);
        HbmLivingHazards.set(living, hazards);
    }

    // ---------------------------------------------------------------- [parity J1] radiation core

    /** ContaminationUtil.contaminate: reads and stores the living radiation attachment. Server side only. */
    public static boolean contaminate(LivingEntity entity, HazardType hazard, ContaminationType cont, float amount) {
        if (entity.level().isClientSide) {
            return false;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(entity);
        boolean applied = contaminate(entity, data, hazard, cont, amount);
        HbmLivingRadiation.set(entity, data);
        return applied;
    }

    /**
     * Same as {@link #contaminate(LivingEntity, HazardType, ContaminationType, float)} on an attachment instance the
     * caller stores itself (used inside the per-tick living handler).
     */
    public static boolean contaminate(LivingEntity entity, HbmLivingRadiation data, HazardType hazard,
                                      ContaminationType cont, float amount) {
        // The environment counter rises before every protection check (Geiger "environmental" readout).
        if (hazard == HazardType.RADIATION) {
            data.addEnvironmentRadiation(amount);
        }

        if (entity instanceof Player player) {
            switch (cont) {
                case FARADAY -> { if (HbmArmorProtection.hasFaradayProtection(player)) return false; }
                case HAZMAT -> { if (checkForHazmat(player)) return false; }
                case HAZMAT2 -> { if (checkForHaz2(player)) return false; }
                case DIGAMMA -> { if (checkForDigamma(player) || checkForDigamma2(player)) return false; }
                case DIGAMMA2 -> { if (checkForDigamma2(player)) return false; }
                default -> { }
            }
            // Spectator mode did not exist in 1.7.10; it is treated like creative.
            if (isCreativeLike(player) && cont != ContaminationType.NONE && cont != ContaminationType.DIGAMMA2) {
                return false;
            }
            if (player.tickCount < 200) {
                return false;
            }
        }

        if (hazard == HazardType.RADIATION && isRadImmune(entity)) {
            return false;
        }

        switch (hazard) {
            case RADIATION -> data.addRadiation(amount
                    * (cont == ContaminationType.RAD_BYPASS ? 1.0F : calculateRadiationMod(entity)));
            case DIGAMMA -> incrementDigamma(entity, data, amount);
        }
        return true;
    }

    /** HbmLivingProps.incrementDigamma: ducks never gain digamma; the value is clamped to 0..10. */
    public static void incrementDigamma(LivingEntity entity, HbmLivingRadiation data, float digamma) {
        if (entity instanceof com.atomiclegacy.entity.LegacyDuckEntity) {
            digamma = 0.0F;
        }
        data.addDigamma(digamma);
    }

    /**
     * ContaminationUtil.applyDigammaData (HazardTypeDigamma): ducks/ocelots, creative players, players younger than
     * 200 ticks, the stability effect and full FAU/DNS armor (ArmorUtil.checkForDigamma) prevent the dose.
     */
    public static void applyDigammaData(LivingEntity entity, HbmLivingRadiation data, float amount) {
        if (entity instanceof com.atomiclegacy.entity.LegacyDuckEntity || isOcelot(entity)) {
            return;
        }
        if (entity instanceof Player player && (isCreativeLike(player) || player.tickCount < 200)) {
            return;
        }
        if (entity.hasEffect(HbmMobEffects.STABILITY)) {
            return;
        }
        if (!(entity instanceof Player player && checkForDigamma(player))) {
            incrementDigamma(entity, data, amount);
        }
    }

    /** ContaminationUtil.calculateRadiationMod: 10^-resistance for players, 1 for everything else. */
    public static float calculateRadiationMod(LivingEntity entity) {
        if (entity instanceof Player player) {
            // HbmArmorProtection caches 10^-(sum of HazmatRegistry piece values); Rad-X adds 0.2 to the exponent.
            double mod = HbmArmorProtection.radiationMultiplier(player);
            if (player.hasEffect(HbmMobEffects.RADX)) {
                mod *= Math.pow(10.0D, -RADX_RESISTANCE);
            }
            return (float) mod;
        }
        return 1.0F;
    }

    /** HazmatRegistry.getResistance(EntityPlayer): armor pieces (with cladding) + 0.2 for Rad-X. */
    public static float getResistance(Player player) {
        float res = 0.0F;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            res += (float) HbmArmorProtection.itemRadiationResistance(player.getItemBySlot(slot),
                    player.registryAccess());
        }
        if (player.hasEffect(HbmMobEffects.RADX)) {
            res += RADX_RESISTANCE;
        }
        return res;
    }

    /** ContaminationUtil.isRadImmune: the mutation effect or one of the immune entity classes. */
    public static boolean isRadImmune(Entity entity) {
        if (entity instanceof LivingEntity living && living.hasEffect(HbmMobEffects.MUTATION)) {
            return true;
        }
        return entity instanceof LivingEntity living && RadiationEvents.isLegacyRadiationImmune(living);
    }

    /** Adds HbmPotion.radiation exactly like {@code addPotionEffect(new PotionEffect(radiation.id, duration, amp))}. */
    public static void addRadiationEffect(LivingEntity entity, int duration, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.addEffect(new MobEffectInstance(HbmMobEffects.RADIATION, duration, amplifier));
        }
    }

    /** HbmLivingProps.addCont. */
    public static void addContamination(LivingEntity entity, float rad, int time, boolean ignoreArmor) {
        HbmLivingRadiation data = HbmLivingRadiation.get(entity);
        data.addContamination(new com.atomiclegacy.radiation.LegacyContaminationEffect(rad, time, ignoreArmor));
        HbmLivingRadiation.set(entity, data);
    }

    // ---------------------------------------------------------------- [parity J1] ArmorUtil checks

    private static final String[][] HAZMAT_SETS = {
            {"hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots"},
            {"hazmat_helmet_red", "hazmat_plate_red", "hazmat_legs_red", "hazmat_boots_red"},
            {"hazmat_helmet_grey", "hazmat_plate_grey", "hazmat_legs_grey", "hazmat_boots_grey"},
            {"schrabidium_helmet", "schrabidium_plate", "schrabidium_legs", "schrabidium_boots"}
    };
    private static final String[][] HAZ2_SETS = {
            {"hazmat_paa_helmet", "hazmat_paa_plate", "hazmat_paa_legs", "hazmat_paa_boots"},
            {"liquidator_helmet", "liquidator_plate", "liquidator_legs", "liquidator_boots"},
            {"euphemium_helmet", "euphemium_plate", "euphemium_legs", "euphemium_boots"},
            {"rpa_helmet", "rpa_plate", "rpa_legs", "rpa_boots"},
            {"fau_helmet", "fau_plate", "fau_legs", "fau_boots"},
            {"dns_helmet", "dns_plate", "dns_legs", "dns_boots"}
    };
    private static final String[][] DIGAMMA_SETS = {
            {"fau_helmet", "fau_plate", "fau_legs", "fau_boots"},
            {"dns_helmet", "dns_plate", "dns_legs", "dns_boots"}
    };
    private static final String[] ROBES = {"robes_helmet", "robes_plate", "robes_legs", "robes_boots"};

    /** ArmorUtil.checkForHazmat. */
    public static boolean checkForHazmat(LivingEntity entity) {
        return anySet(entity, HAZMAT_SETS) || checkForHaz2(entity) || entity.hasEffect(HbmMobEffects.MUTATION);
    }

    /** ArmorUtil.checkForHaz2. */
    public static boolean checkForHaz2(LivingEntity entity) {
        return anySet(entity, HAZ2_SETS);
    }

    /** ArmorUtil.checkForDigamma. */
    public static boolean checkForDigamma(Player player) {
        return anySet(player, DIGAMMA_SETS) || player.hasEffect(HbmMobEffects.STABILITY);
    }

    /**
     * ArmorUtil.checkForDigamma2: full robes, stability, every modded piece carries cladding_iron and max health
     * below 3. ArmorModHandler.hasMods (tag present) is approximated by "any mod slot occupied".
     */
    public static boolean checkForDigamma2(Player player) {
        if (!wearing(player, ROBES) || !player.hasEffect(HbmMobEffects.STABILITY)) {
            return false;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) {
                continue;
            }
            ItemStack[] mods = ArmorModHandler.pryMods(armor, player.registryAccess());
            boolean hasMods = false;
            for (ItemStack mod : mods) {
                if (mod != null && !mod.isEmpty()) {
                    hasMods = true;
                    break;
                }
            }
            if (hasMods) {
                ItemStack cladding = mods[ArmorModHandler.CLADDING];
                if (cladding == null || cladding.isEmpty() || !"cladding_iron".equals(path(cladding))) {
                    return false;
                }
            }
        }
        return player.getMaxHealth() < 3.0F;
    }

    private static boolean anySet(LivingEntity entity, String[][] sets) {
        for (String[] set : sets) {
            if (wearing(entity, set)) {
                return true;
            }
        }
        return false;
    }

    private static boolean wearing(LivingEntity entity, String[] set) {
        return set[0].equals(path(entity.getItemBySlot(EquipmentSlot.HEAD)))
                && set[1].equals(path(entity.getItemBySlot(EquipmentSlot.CHEST)))
                && set[2].equals(path(entity.getItemBySlot(EquipmentSlot.LEGS)))
                && set[3].equals(path(entity.getItemBySlot(EquipmentSlot.FEET)));
    }

    private static String path(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return com.atomiclegacy.AtomicLegacy.MOD_ID.equals(key.getNamespace()) ? key.getPath() : "";
    }

    private static boolean isOcelot(LivingEntity entity) {
        // 1.7.10 EntityOcelot also covered tamed cats.
        return entity instanceof net.minecraft.world.entity.animal.Ocelot
                || entity instanceof net.minecraft.world.entity.animal.Cat;
    }

    public static boolean isCreativeLike(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    // ---------------------------------------------------------------- [parity J1] readouts

    /** ContaminationUtil.printGeigerData. */
    public static void printGeigerData(Player player) {
        HbmLivingRadiation data = HbmLivingRadiation.get(player);
        double eRad = ((int) (data.getRadiation() * 10)) / 10D;
        float chunk = player.level() instanceof ServerLevel serverLevel
                ? (float) ChunkRadiationData.get(serverLevel).getRadiation(new BlockPos(
                        Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ())))
                : data.getChunkRadiation();
        double rads = ((int) (chunk * 10)) / 10D;
        double env = ((int) (data.getRadiationBuffer() * 10D)) / 10D;

        double res = ((int) (10000D - calculateRadiationMod(player) * 10000D)) / 100D;
        double resKoeff = ((int) (getResistance(player) * 100D)) / 100D;

        String chunkPrefix = getPreffixFromRad(rads);
        String envPrefix = getPreffixFromRad(env);
        String radPrefix;
        String resPrefix = "" + ChatFormatting.WHITE;

        if (eRad < 200) radPrefix = "" + ChatFormatting.GREEN;
        else if (eRad < 400) radPrefix = "" + ChatFormatting.YELLOW;
        else if (eRad < 600) radPrefix = "" + ChatFormatting.GOLD;
        else if (eRad < 800) radPrefix = "" + ChatFormatting.RED;
        else if (eRad < 1000) radPrefix = "" + ChatFormatting.DARK_RED;
        else radPrefix = "" + ChatFormatting.DARK_GRAY;

        if (resKoeff > 0) resPrefix += ChatFormatting.GREEN;

        player.sendSystemMessage(Component.literal("===== ☢ ").append(Component.translatable("geiger.title"))
                .append(Component.literal(" ☢ =====")).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.chunkRad")
                .append(Component.literal(" " + chunkPrefix + rads + " RAD/s")).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.envRad")
                .append(Component.literal(" " + envPrefix + env + " RAD/s")).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.playerRad")
                .append(Component.literal(" " + radPrefix + eRad + " RAD")).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.playerRes")
                .append(Component.literal(" " + resPrefix + res + "% (" + resKoeff + ")"))
                .withStyle(ChatFormatting.YELLOW));
    }

    /** ContaminationUtil.printDosimeterData. */
    public static void printDosimeterData(Player player) {
        double env = ((int) (HbmLivingRadiation.get(player).getRadiationBuffer() * 10D)) / 10D;
        boolean limit = false;
        if (env > 3.6D) {
            env = 3.6D;
            limit = true;
        }
        String envPrefix = getPreffixFromRad(env);
        player.sendSystemMessage(Component.literal("===== ☢ ")
                .append(Component.translatable("geiger.title.dosimeter"))
                .append(Component.literal(" ☢ =====")).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.envRad")
                .append(Component.literal(" " + envPrefix + (limit ? ">" : "") + env + " RAD/s"))
                .withStyle(ChatFormatting.YELLOW));
    }

    public static String getPreffixFromRad(double rads) {
        if (rads == 0) return "" + ChatFormatting.GREEN;
        if (rads < 1) return "" + ChatFormatting.YELLOW;
        if (rads < 10) return "" + ChatFormatting.GOLD;
        if (rads < 100) return "" + ChatFormatting.RED;
        if (rads < 1000) return "" + ChatFormatting.DARK_RED;
        return "" + ChatFormatting.DARK_GRAY;
    }
}

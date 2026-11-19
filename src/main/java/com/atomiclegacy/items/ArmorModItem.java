package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorModItem extends Item {
    private final int slotType;
    private final boolean helmet;
    private final boolean chestplate;
    private final boolean leggings;
    private final boolean boots;

    public ArmorModItem(Properties properties, int slotType, boolean helmet, boolean chestplate, boolean leggings, boolean boots) {
        super(properties.stacksTo(1));
        this.slotType = slotType;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    public static ArmorModItem helmet(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    public static ArmorModItem servos(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.SERVOS, false, true, true, false);
    }

    public static ArmorModItem cladding(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.CLADDING, true, true, true, true);
    }

    public static ArmorModItem kevlar(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.KEVLAR, false, true, false, false);
    }

    public static ArmorModItem battery(Properties properties) {
        return new ArmorModItem(properties, ArmorModHandler.BATTERY, true, true, true, true);
    }

    public int slotType() {
        return this.slotType;
    }

    public boolean appliesTo(net.minecraft.world.item.ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET -> this.helmet;
            case CHESTPLATE -> this.chestplate;
            case LEGGINGS -> this.leggings;
            case BOOTS -> this.boots;
            default -> false;
        };
    }

    /**
     * Direct equivalent of 1.7.10 ItemArmorMod#modUpdate. The armor event
     * dispatches this once for every installed modifier on the server.
     */
    public void tickArmor(Player player, ItemStack armor) {
    }

    /** Direct equivalent of 1.7.10 ItemArmorMod#modDamage. */
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return amount;
    }

    /** Attribute contribution calculated once per player tick from installed mods. */
    public double extraHealth() {
        return 0.0D;
    }

    /** Equivalent to the old operation-2 movement-speed modifier. */
    public double movementMultiplier() {
        return 1.0D;
    }

    /** Per-equipped-piece form used by armor-specific legacy modules. */
    public double movementMultiplier(ItemStack armor) {
        return movementMultiplier();
    }

    /** Equivalent to the old operation-2 attack-damage modifier. */
    public double attackDamageMultiplier(ItemStack armor) {
        return 1.0D;
    }

    /** Equivalent to the old operation-0 knockback-resistance modifier. */
    public double knockbackResistance(ItemStack armor) {
        return 0.0D;
    }

    /** Number of horizontal dash charges contributed by this module. */
    public int extraDashes() {
        return 0;
    }

    // [parity I2: tooltip] ItemArmorMod#addInformation: "Applicable To:" list and "Slot:" line (DARK_PURPLE headers).
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("armor_mod.atomiclegacy.applicable_to").withStyle(ChatFormatting.DARK_PURPLE));
        if (this.helmet && this.chestplate && this.leggings && this.boots) {
            tooltip.add(Component.literal("  ").append(Component.translatable("armor_mod.atomiclegacy.all")));
        } else {
            if (this.helmet) tooltip.add(Component.literal("  ").append(Component.translatable("armor_mod.atomiclegacy.helmets")));
            if (this.chestplate) tooltip.add(Component.literal("  ").append(Component.translatable("armor_mod.atomiclegacy.chestplates")));
            if (this.leggings) tooltip.add(Component.literal("  ").append(Component.translatable("armor_mod.atomiclegacy.leggings")));
            if (this.boots) tooltip.add(Component.literal("  ").append(Component.translatable("armor_mod.atomiclegacy.boots")));
        }
        tooltip.add(Component.literal("Slot:").withStyle(ChatFormatting.DARK_PURPLE));
        if (this.slotType >= 0 && this.slotType < ArmorModHandler.MOD_SLOTS) {
            tooltip.add(Component.literal("  ").append(Component.translatable(ArmorModHandler.slotTranslationKey(this.slotType))));
        }
    }

    // [parity I2: tooltip] ItemArmorMod#addDesc: line shown under "Mods:" on the armor piece's tooltip.
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(mod.getHoverName().copy());
    }

    // [parity I2: tooltip] helpers for the alternating colours of the original (System.currentTimeMillis() % 1000 < 500).
    protected static ChatFormatting blink(ChatFormatting first, ChatFormatting second) {
        return System.currentTimeMillis() % 1000 < 500 ? first : second;
    }

    protected static Component line(ChatFormatting color, String text) {
        return Component.literal(text).withStyle(color);
    }

    protected static Component descLine(ChatFormatting color, ItemStack mod, String suffix) {
        return Component.literal("  ").append(mod.getHoverName()).append(suffix).withStyle(color);
    }

    // [parity I2: cancel] modDamage implementations that call event.setCanceled(true) in 1.7.10.
    private static final ThreadLocal<Boolean> CANCEL_REQUESTED = ThreadLocal.withInitial(() -> false);

    protected static void requestCancel() {
        CANCEL_REQUESTED.set(true);
    }

    public static boolean consumeCancelRequest() {
        boolean requested = CANCEL_REQUESTED.get();
        CANCEL_REQUESTED.set(false);
        return requested;
    }

    public boolean appliesToHelmet() { return this.helmet; }
    public boolean appliesToChestplate() { return this.chestplate; }
    public boolean appliesToLeggings() { return this.leggings; }
    public boolean appliesToBoots() { return this.boots; }
}

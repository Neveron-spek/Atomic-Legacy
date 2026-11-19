package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** [parity I2] Direct port of the three 1.7.10 ItemModPads variants (tooltip, addDesc, walking charge). */
public final class ArmorPadsItem extends ArmorModItem {
    private final float fallDamageMultiplier;
    private final boolean staticCharging;

    public ArmorPadsItem(Properties properties, float fallDamageMultiplier, boolean staticCharging) {
        super(properties, ArmorModHandler.BOOTS_ONLY, false, false, false, true);
        this.fallDamageMultiplier = fallDamageMultiplier;
        this.staticCharging = staticCharging;
    }

    public float fallDamageMultiplier() {
        return this.fallDamageMultiplier;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return source.is(DamageTypes.FALL) ? amount * this.fallDamageMultiplier : amount;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        // distanceWalkedModified != prevDistanceWalkedModified
        if (player.level().isClientSide || !this.staticCharging || player.walkDist == player.walkDistO
                || !ArmorFSBItem.hasFSBArmorIgnoringCharge(player)) {
            return;
        }
        for (ItemStack equipped : player.getArmorSlots()) {
            if (equipped.getItem() instanceof PoweredArmorFSBItem powered) {
                long charge = powered.staticRechargeAmount();
                powered.hbmSetCharge(equipped, Math.min(powered.hbmCapacity(equipped), powered.hbmCharge(equipped) + charge));
            }
        }
    }

    private int percent() {
        return Math.round((1F - this.fallDamageMultiplier) * 100);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.fallDamageMultiplier != 1F) {
            tooltip.add(line(ChatFormatting.RED, "-" + percent() + "% fall damage"));
        }
        if (this.staticCharging) {
            tooltip.add(line(ChatFormatting.DARK_PURPLE, "Passively charges electric armor when walking"));
        }
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.DARK_PURPLE, mod, this.staticCharging
                ? " (-" + percent() + "% fall dmg / passive charge)"
                : " (-" + percent() + "% fall dmg)"));
    }
}

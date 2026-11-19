package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 servo and Desh-servo effects, scoped to their actual armor piece. */
public final class ServoArmorModItem extends ArmorModItem {
    private final boolean desh;

    public ServoArmorModItem(Item.Properties properties, boolean desh) {
        super(properties, ArmorModHandler.SERVOS, false, true, true, false);
        this.desh = desh;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (!(armor.getItem() instanceof ArmorItem armorItem)) {
            return;
        }
        if (armorItem.getType() == ArmorItem.Type.CHESTPLATE) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, this.desh ? 2 : 0));
        } else if (armorItem.getType() == ArmorItem.Type.LEGGINGS) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, this.desh ? 2 : 1));
        }
    }

    @Override
    public double movementMultiplier(ItemStack armor) {
        return armor.getItem() instanceof ArmorItem armorItem && armorItem.getType() == ArmorItem.Type.LEGGINGS
                ? (this.desh ? 1.5D : 1.25D)
                : 1.0D;
    }

    @Override
    public double attackDamageMultiplier(ItemStack armor) {
        return armor.getItem() instanceof ArmorItem armorItem && armorItem.getType() == ArmorItem.Type.CHESTPLATE
                ? (this.desh ? 2.5D : 1.5D)
                : 1.0D;
    }

    // [parity I2: tooltip] ItemModServos
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!this.desh) {
            tooltip.add(line(ChatFormatting.DARK_PURPLE, "Chestplate: Haste I / Damage +50%"));
            tooltip.add(line(ChatFormatting.DARK_PURPLE, "Leggings: Speed +25% / Jump II"));
        } else {
            tooltip.add(line(ChatFormatting.DARK_PURPLE, "Chestplate: Haste III / Damage +150%"));
            tooltip.add(line(ChatFormatting.DARK_PURPLE, "Leggings: Speed +50% / Jump III"));
        }
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        if (!(armor.getItem() instanceof ArmorItem armorItem)) return;
        if (armorItem.getType() == ArmorItem.Type.CHESTPLATE)
            tooltip.add(descLine(ChatFormatting.DARK_PURPLE, mod, this.desh ? " (Haste III / Damage +150%)" : " (Haste I / Damage +50%)"));
        if (armorItem.getType() == ArmorItem.Type.LEGGINGS)
            tooltip.add(descLine(ChatFormatting.DARK_PURPLE, mod, this.desh ? " (Speed +50% / Jump III)" : " (Speed +25% / Jump II)"));
    }
}

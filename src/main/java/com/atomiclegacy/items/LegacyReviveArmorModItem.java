package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One-use and three-use death prevention upgrades from 1.7.10. */
public final class LegacyReviveArmorModItem extends ArmorModItem {
    public LegacyReviveArmorModItem(Item.Properties properties, int uses) {
        super(properties.durability(uses), ArmorModHandler.EXTRA, false, false, true, false);
    }

    public static boolean tryRevive(Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            ItemStack revive = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.EXTRA];
            if (!(revive.getItem() instanceof LegacyReviveArmorModItem)) {
                continue;
            }

            revive.setDamageValue(revive.getDamageValue() + 1);
            if (revive.getDamageValue() >= revive.getMaxDamage()) {
                ArmorModHandler.removeMod(armor, ArmorModHandler.EXTRA);
            } else {
                ArmorModHandler.applyMod(armor, revive, player.registryAccess());
            }
            player.setHealth(player.getMaxHealth());
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 99));
            return true;
        }
        return false;
    }

    // [parity I2: tooltip] ItemModRevive (scrumpy: 1 revive, wild_p: 3)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getMaxDamage() == 1) {
            tooltip.add(line(ChatFormatting.GOLD, "But how did you survive?"));
            tooltip.add(line(ChatFormatting.RED, "I was drunk."));
        } else {
            tooltip.add(Component.literal("Explosive ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Reactive ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("Plot ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Armor").withStyle(ChatFormatting.RED)));
        }
        tooltip.add(Component.empty());
        tooltip.add(line(ChatFormatting.GOLD, (stack.getMaxDamage() - stack.getDamageValue()) + " revives left"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.GOLD, mod, " (" + (mod.getMaxDamage() - mod.getDamageValue()) + " revives left)"));
    }
}

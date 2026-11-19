package com.atomiclegacy.items;

import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MeteorCharmItem extends ArmorModItem {
    private final Kind kind;

    public MeteorCharmItem(Properties properties, Kind kind) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, true, false, false); // [parity I2] helmets and chestplates
        this.kind = kind;
    }

    // [parity I2: tooltip] ItemModCharm (no blank line before the common block)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.AQUA, "You feel blessed."));
        if (this.kind == Kind.PROTECTION) {
            tooltip.add(line(ChatFormatting.AQUA, "Diverts meteors away from the player."));
            tooltip.add(line(ChatFormatting.AQUA, "Meteors no longer destroy blocks."));
            tooltip.add(line(ChatFormatting.AQUA, "Halves broadcaster damage"));
        } else {
            tooltip.add(line(ChatFormatting.AQUA, "Disables meteorite spawning."));
            tooltip.add(line(ChatFormatting.AQUA, "Negates broadcaster damage"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.GOLD, mod, ""));
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        if (!source.is(HbmDamageTypes.BROADCAST)) {
            return amount;
        }
        return this.kind == Kind.PROTECTION ? amount * 0.5F : 0.0F;
    }

    public Kind kind() {
        return this.kind;
    }

    public enum Kind {
        METEOR,
        PROTECTION
    }
}

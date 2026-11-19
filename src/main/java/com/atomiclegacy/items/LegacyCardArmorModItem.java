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

/** The 1.7.10 Ace/Queen of Spades helmet cards. */
public final class LegacyCardArmorModItem extends ArmorModItem {
    private final boolean queen;

    public LegacyCardArmorModItem(Item.Properties properties, boolean queen) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, true, false, false);
        this.queen = queen;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        // [parity I2] HbmPlayerProps.plink("random.break", 0.5, 1 + rand*0.5) with its 10-tick cooldown; event cancelled
        if (this.queen && player.getRandom().nextInt(3) == 0) {
            LegacyArmorModParity.plink(player, net.minecraft.sounds.SoundEvents.ITEM_BREAK, 0.5F, 1.0F + player.getRandom().nextFloat() * 0.5F);
            requestCancel();
            return 0.0F;
        }
        return amount;
    }

    // [parity I2: tooltip] ItemModCard
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!this.queen) {
            tooltip.add(line(ChatFormatting.RED, "Top of the line!"));
            tooltip.add(line(ChatFormatting.RED, "Guns now have a 33% chance to not consume ammo."));
        } else {
            tooltip.add(line(ChatFormatting.RED, "Power!"));
            tooltip.add(line(ChatFormatting.RED, "Adds a 33% chance to tank damage with no cap."));
        }
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(mod.getHoverName().copy().withStyle(ChatFormatting.RED));
    }
}

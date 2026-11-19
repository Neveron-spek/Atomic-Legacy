package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * [parity I2] ItemModCladding (paint/rubber/lead/desh/ghiorsium, rad 0.025/0.005/0.1/0.2/0.5) and
 * ItemModObsidian (rad &lt; 0: no radiation value, "Makes dropped armor indestructible").
 * The resistance itself is read by HbmArmorProtection from the installed item id.
 */
public final class LegacyCladdingArmorModItem extends ArmorModItem {
    private final double rad;

    public LegacyCladdingArmorModItem(Properties properties, double rad) {
        super(properties, ArmorModHandler.CLADDING, true, true, true, true);
        this.rad = rad;
    }

    public double rad() {
        return this.rad;
    }

    private boolean obsidian() {
        return this.rad < 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (obsidian()) {
            tooltip.add(line(ChatFormatting.DARK_PURPLE, "Makes dropped armor indestructible"));
        } else {
            tooltip.add(line(ChatFormatting.YELLOW, "+" + this.rad + " rad-resistance"));
        }
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        if (obsidian()) {
            tooltip.add(descLine(ChatFormatting.DARK_PURPLE, mod, " (Item indestructible)"));
        } else {
            tooltip.add(descLine(ChatFormatting.YELLOW, mod, " (+" + this.rad + " radiation resistance)"));
        }
    }
}

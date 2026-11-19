package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The old ItemModInsert's chestplate-only armor insert values. */
public final class ArmorInsertItem extends ArmorModItem {
    private final float damageMultiplier;
    private final float projectileMultiplier;
    private final float explosionMultiplier;
    private final float speedMultiplier;
    private final boolean radioactive;
    private final boolean reactive;

    public ArmorInsertItem(
            Properties properties,
            float damageMultiplier,
            float projectileMultiplier,
            float explosionMultiplier,
            float speedMultiplier,
            boolean radioactive,
            boolean reactive
    ) {
        super(properties, ArmorModHandler.KEVLAR, false, true, false, false);
        this.damageMultiplier = damageMultiplier;
        this.projectileMultiplier = projectileMultiplier;
        this.explosionMultiplier = explosionMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.radioactive = radioactive;
        this.reactive = reactive;
    }

    public float modifyDamage(float amount, boolean projectile, boolean explosion) {
        float result = amount * this.damageMultiplier;
        if (projectile) result *= this.projectileMultiplier;
        if (explosion) result *= this.explosionMultiplier;
        return result;
    }

    public float speedMultiplier() {
        return this.speedMultiplier;
    }

    public boolean radioactive() {
        return this.radioactive;
    }

    public boolean reactive() {
        return this.reactive;
    }

    // [parity I2: tooltip] ItemModInsert#addInformation / #addDesc (hard-coded English, original colours).
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.damageMultiplier != 1F)
            tooltip.add(line(ChatFormatting.RED, (this.damageMultiplier < 1 ? "-" : "+") + Math.abs(Math.round((1F - this.damageMultiplier) * 100)) + "% damage"));
        if (this.projectileMultiplier != 1F)
            tooltip.add(line(ChatFormatting.YELLOW, "-" + Math.round((1F - this.projectileMultiplier) * 100) + "% projectile damage"));
        if (this.explosionMultiplier != 1F)
            tooltip.add(line(ChatFormatting.YELLOW, "-" + Math.round((1F - this.explosionMultiplier) * 100) + "% explosion damage"));
        if (this.speedMultiplier != 1F)
            tooltip.add(line(ChatFormatting.BLUE, "-" + Math.round((1F - this.speedMultiplier) * 100) + "% speed"));
        if (this.radioactive)
            tooltip.add(line(ChatFormatting.DARK_RED, "+100 RAD/s"));
        tooltip.add(Component.literal((stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() + "HP"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        List<String> desc = new java.util.ArrayList<>();
        if (this.damageMultiplier != 1F)
            desc.add((this.damageMultiplier < 1 ? "-" : "+") + Math.abs(Math.round((1F - this.damageMultiplier) * 100)) + "% dmg");
        if (this.projectileMultiplier != 1F)
            desc.add("-" + Math.round((1F - this.projectileMultiplier) * 100) + "% proj");
        if (this.explosionMultiplier != 1F)
            desc.add("-" + Math.round((1F - this.explosionMultiplier) * 100) + "% exp");
        if (this.explosionMultiplier != 1F) // sic: the original tests explosionMod here
            desc.add("-" + Math.round((1F - this.speedMultiplier) * 100) + "% speed");
        if (this.radioactive)
            desc.add("+100 RAD/s");
        String join = String.join(" / ", desc);
        tooltip.add(descLine(ChatFormatting.DARK_PURPLE, mod, " (" + join + " / " + (mod.getMaxDamage() - mod.getDamageValue()) + "HP)"));
    }

}

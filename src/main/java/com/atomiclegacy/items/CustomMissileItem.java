package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** 1.7.10 ItemCustomMissile NBT payload, retained as component-backed custom data. */
public final class CustomMissileItem extends Item {
    private static final String CHIP = "chip";
    private static final String WARHEAD = "warhead";
    private static final String FUSELAGE = "fuselage";
    private static final String STABILITY = "stability";
    private static final String THRUSTER = "thruster";

    public CustomMissileItem(Properties properties) {
        super(properties);
    }

    public static ItemStack build(ItemStack chip, ItemStack warhead, ItemStack fuselage, ItemStack stability, ItemStack thruster) {
        ItemStack missile = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                com.atomiclegacy.AtomicLegacy.id("missile_custom")));
        CompoundTag tag = new CompoundTag();
        tag.putString(CHIP, MissilePartItem.id(chip));
        tag.putString(WARHEAD, MissilePartItem.id(warhead));
        tag.putString(FUSELAGE, MissilePartItem.id(fuselage));
        tag.putString(THRUSTER, MissilePartItem.id(thruster));
        if (!stability.isEmpty()) {
            tag.putString(STABILITY, MissilePartItem.id(stability));
        }
        missile.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return missile;
    }

    /** [parity P1: tooltip] ItemCustomMissile.addInformation. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return;
        CustomMissileData data = CustomMissileData.read(stack);
        if (data == null) {
            tooltip.add(LegacyMissileItem.legacy("\u00a7c" + LegacyMissileItem.resolve("error.generic")));
            return;
        }
        MissilePartItem.Definition chip = MissilePartItem.definition(data.chip());
        MissilePartItem.Definition warhead = MissilePartItem.definition(data.warhead());
        MissilePartItem.Definition fuselage = MissilePartItem.definition(data.fuselage());
        MissilePartItem.Definition thruster = MissilePartItem.definition(data.thruster());
        MissilePartItem.Definition fins = data.fins().isEmpty() ? null : MissilePartItem.definition(data.fins());
        tooltip.add(line("warhead", MissilePartItem.warheadName(warhead.warhead())));
        tooltip.add(line("strength", Float.toString(warhead.primary())));
        tooltip.add(line("fuelType", MissilePartItem.fuelName(fuselage.fuel())));
        tooltip.add(line("fuelAmount", fuselage.primary() + "l"));
        tooltip.add(line("chipInaccuracy", (chip.primary() * 100) + "%"));
        tooltip.add(line("finInaccuracy", fins != null ? (fins.primary() * 100) + "%" : "100%"));
        tooltip.add(line("size", MissilePartItem.sizeName(fuselage.top()) + "/" + MissilePartItem.sizeName(fuselage.bottom())));
        float health = warhead.health() + fuselage.health() + thruster.health();
        if (fins != null) health += fins.health();
        tooltip.add(line("health", health + "HP"));
    }

    private static Component line(String key, String value) {
        return LegacyMissileItem.legacy("\u00a7l" + LegacyMissileItem.resolve("item.missile.desc." + key) + ": \u00a77" + value);
    }
}

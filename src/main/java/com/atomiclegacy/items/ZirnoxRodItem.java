package com.atomiclegacy.items;

import com.atomiclegacy.util.LegacyI18nFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class ZirnoxRodItem extends LegacyVariantItem {
    private static final String LIFE_TAG = "life";

    public static final List<Fuel> FUELS = List.of(
            new Fuel("natural_uranium_fuel", 250_000, 30, false),
            new Fuel("uranium_fuel", 200_000, 50, false),
            new Fuel("th232_fuel", 20_000, 0, true),
            new Fuel("thorium_fuel", 200_000, 40, false),
            new Fuel("mox_fuel", 165_000, 75, false),
            new Fuel("plutonium_fuel", 175_000, 65, false),
            new Fuel("u233_fuel", 150_000, 100, false),
            new Fuel("u235_fuel", 165_000, 85, false),
            new Fuel("les_fuel", 150_000, 150, false),
            new Fuel("lithium_fuel", 20_000, 0, true),
            new Fuel("zfb_mox_fuel", 50_000, 35, false)
    );

    public ZirnoxRodItem(Properties properties) {
        super(
                properties.stacksTo(1),
                "rod_zirnox",
                LegacyVariantItem.variants(FUELS.stream().map(Fuel::id).toArray(String[]::new))
        );
    }

    public Fuel fuel(ItemStack stack) {
        String id = variant(stack).id();
        for (Fuel fuel : FUELS) {
            if (fuel.id().equals(id)) {
                return fuel;
            }
        }
        return FUELS.getFirst();
    }

    // [parity M2: ZIRNOX] ItemZirnoxRod: Forge 1.7.10 durability bar from getDurabilityForDisplay = life / maxLife
    private double usedFraction(ItemStack stack) {
        return (double) life(stack) / (double) fuel(stack).maxLife();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return usedFraction(stack) > 0.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D - usedFraction(stack) * 13.0D);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int j = (int) Math.round(255.0D - usedFraction(stack) * 255.0D);
        return (255 - j) << 16 | j << 8;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity M2: ZIRNOX] ItemZirnoxRod#addInformation, including the 1.7.10 I18n "Format error" produced by the
        // %d placeholders receiving BobMathUtil.getShortNumber strings.
        Fuel fuel = fuel(stack);
        tooltip.add(Component.literal(ChatFormatting.YELLOW + LegacyI18nFormat.format("trait.rbmk.depletion",
                ((int) ((((double) life(stack)) / (double) fuel.maxLife()) * 100000)) / 1000D + "%")));
        String[] loc = fuel.breeding()
                ? LegacyI18nFormat.formatArray("desc.item.zirnoxBreedingRod", LegacyI18nFormat.shortNumber(fuel.maxLife()))
                : LegacyI18nFormat.formatArray("desc.item.zirnoxRod", fuel.heat(), LegacyI18nFormat.shortNumber(fuel.maxLife()));
        for (String line : loc) {
            tooltip.add(Component.literal(line));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static int life(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(LIFE_TAG);
    }

    public static void setLife(ItemStack stack, int life) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(LIFE_TAG, Math.max(0, life));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void incrementLife(ItemStack stack) {
        setLife(stack, life(stack) + 1);
    }

    public record Fuel(String id, int maxLife, int heat, boolean breeding) {
    }
}

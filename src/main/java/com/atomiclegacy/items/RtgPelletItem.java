package com.atomiclegacy.items;

import com.atomiclegacy.config.HbmConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 1.7.10 RTG pellet state. Depletion belongs to the pellet stack because a
 * generator may hold pellets with different remaining lifetimes.
 */
public final class RtgPelletItem extends Item {
    private static final String DEPLETION_TAG = "rtg_depletion";
    private static final String DEPLETED_MATERIAL_TAG = "rtg_depleted_material";

    private final int heat;
    private final int decayingHeat;
    private final long maxLifespan;
    private final String depletedMaterial;
    private final Supplier<? extends Item> depletedPellet;

    public RtgPelletItem(
            int heat,
            int decayingHeat,
            long maxLifespan,
            String depletedMaterial,
            Supplier<? extends Item> depletedPellet
    ) {
        super(new Properties().stacksTo(1));
        this.heat = Math.max(0, heat);
        this.decayingHeat = Math.max(0, decayingHeat);
        this.maxLifespan = Math.max(1L, maxLifespan);
        this.depletedMaterial = Objects.requireNonNull(depletedMaterial, "depletedMaterial");
        this.depletedPellet = Objects.requireNonNull(depletedPellet, "depletedPellet");
    }

    public int heat(ItemStack stack) {
        int baseHeat = HbmConfig.rtgDecay() ? this.decayingHeat : this.heat;
        if (!HbmConfig.scaleRtgPower()) {
            return baseHeat;
        }
        return (int) Math.ceil(baseHeat * (double) remainingLifespan(stack) / this.maxLifespan);
    }

    public long remainingLifespan(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(DEPLETION_TAG) ? Math.max(0L, tag.getLong(DEPLETION_TAG)) : this.maxLifespan;
    }

    /** Advances the exact old per-tick pellet depletion state. */
    public ItemStack burnTick(ItemStack stack) {
        if (!HbmConfig.rtgDecay()) {
            return stack;
        }
        long remaining = remainingLifespan(stack);
        // ItemRTGPellet.handleDecay() first permits the zero-lifetime tick,
        // then replaces the pellet on the following tick.
        if (remaining <= 0L) {
            return depletedStack();
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(DEPLETION_TAG, remaining - 1L);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** [parity S3: JEI] ItemRTGPellet#getRecipeMap value (decayItem). */
    public ItemStack decayResult() {
        return depletedStack();
    }

    private ItemStack depletedStack() {
        ItemStack depleted = new ItemStack(this.depletedPellet.get());
        CompoundTag tag = depleted.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(DEPLETED_MATERIAL_TAG, this.depletedMaterial);
        depleted.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return depleted;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return remainingLifespan(stack) < this.maxLifespan;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * remainingLifespan(stack) / this.maxLifespan);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x47C35A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        // ItemRTGPellet.addInformation (every 1.7.10 pellet has a decay item)
        long remaining = remainingLifespan(stack);
        ItemStack decay = depletedStack();
        LegacyTooltips.line(tooltip, LegacyTooltips.resolve(getDescriptionId() + ".desc"));
        LegacyTooltips.line(tooltip, LegacyTooltips.resolve("desc.item.rtgHeat", heat(stack)));
        LegacyTooltips.line(tooltip, LegacyTooltips.resolve("desc.item.rtgDecay", decay.getHoverName().getString(), decay.getCount()));
        // BobMathUtil.toPercentage
        LegacyTooltips.line(tooltip, java.text.NumberFormat.getPercentInstance().format((float) remaining / (float) this.maxLifespan));
        if (flag.isAdvanced()) {
            LegacyTooltips.line(tooltip, "EXTENDED INFO:");
            LegacyTooltips.line(tooltip, String.format(java.util.Locale.US, "%s / %s ticks", remaining, this.maxLifespan));
            LegacyTooltips.line(tooltip, String.format(java.util.Locale.US, "Time remaining: %s y, %s d, %s h", (Object[]) ticksToDate(remaining)));
            LegacyTooltips.line(tooltip, String.format(java.util.Locale.US, "Maximum life: %s y, %s d, %s h", (Object[]) ticksToDate(this.maxLifespan)));
        }
    }

    /** BobMathUtil.ticksToDate: 48000 ticks per day, 100 days per year, the rest scaled to 0-10 "hours". */
    private static String[] ticksToDate(long ticks) {
        int tickDay = 48000;
        int tickYear = tickDay * 100;
        long year = Math.floorDiv(ticks, tickYear);
        byte day = (byte) Math.floorDiv(ticks - tickYear * year, tickDay);
        float time = ticks - (tickYear * year + tickDay * day);
        time = (float) (time * 10.0D / tickDay);
        return new String[] {String.valueOf(year), String.valueOf(day), String.valueOf(time)};
    }
}

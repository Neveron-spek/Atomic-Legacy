package com.atomiclegacy.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.7.10 ItemHot / ItemHotDusted port.
 *
 * <p>[parity V3: hot items] {@code ItemHot.heat} is a {@code protected static} field assigned by every constructor, so
 * after ModItems the value of the last constructed ItemHot (ingot_chainsteel, 100) is used by every hot item:
 * heatUp writes 100, getHeat divides by 100. {@code ItemHotDusted.getMaxHeat} only hides the static method and is never
 * reached through {@code ItemHot.heatUp}. The constructor values (200/100) are kept for reference only.
 * onUpdate (inventory tick, server) decrements "heat" by one per tick; once it is 0 the whole NBT compound is dropped
 * (here: the CUSTOM_DATA component). Hot items deal no damage and set no fire themselves.</p>
 */
public class LegacyHotItem extends Item {
    public static final String HEAT_TAG = "heat";
    /** ItemHot.heat after ModItems: the ingot_chainsteel constructor argument. */
    public static final int LEGACY_STATIC_HEAT = 100;

    @SuppressWarnings("unused")
    private final int baseHeat;
    private final boolean dusted;

    public LegacyHotItem(Properties properties, int baseHeat, boolean dusted) {
        super(properties);
        this.baseHeat = baseHeat;
        this.dusted = dusted;
    }

    public boolean isDusted() {
        return this.dusted;
    }

    /** ItemHot.getMaxHeat (static): always the shared static heat. */
    public int maxHeat(ItemStack stack) {
        return LEGACY_STATIC_HEAT;
    }

    public int heat(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(HEAT_TAG);
    }

    /** ItemHot.getHeat: h / heat, unclamped. */
    public double heatFraction(ItemStack stack) {
        return (double) heat(stack) / (double) LEGACY_STATIC_HEAT;
    }

    public static ItemStack heatUp(ItemStack stack) {
        if (stack.getItem() instanceof LegacyHotItem hot) {
            hot.setHeat(stack, LEGACY_STATIC_HEAT);
        }
        return stack;
    }

    public static ItemStack heatUp(ItemStack stack, double fraction) {
        if (stack.getItem() instanceof LegacyHotItem hot) {
            hot.setHeat(stack, (int) (fraction * LEGACY_STATIC_HEAT));
        }
        return stack;
    }

    /** ItemHotDusted metadata (forging grade). */
    public static int grade(ItemStack stack) {
        Integer grade = stack.get(LegacyItemComponents.DUSTED_GRADE.get());
        return grade == null ? 0 : grade;
    }

    public static ItemStack withGrade(ItemStack stack, int grade) {
        if (grade == 0) {
            stack.remove(LegacyItemComponents.DUSTED_GRADE.get());
        } else {
            stack.set(LegacyItemComponents.DUSTED_GRADE.get(), grade);
        }
        return stack;
    }

    /** ComparableStack semantics (item + metadata, NBT ignored) used by the hot anvil recipes. */
    public static boolean sameItemAndGrade(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && grade(a) == grade(b);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        // ItemHot.onUpdate: if(!world.isRemote && stack.hasTagCompound()) { h > 0 ? heat = h - 1 : stackTagCompound = null }
        if (!level.isClientSide) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                int heat = data.copyTag().getInt(HEAT_TAG);
                if (heat > 0) {
                    setHeat(stack, heat - 1);
                } else {
                    stack.remove(DataComponents.CUSTOM_DATA);
                    stack.remove(DataComponents.CUSTOM_MODEL_DATA);
                }
            }
        }
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    /** Writes the "heat" tag (kept at 0 like setInteger("heat", 0)); custom model data 1 marks a visibly hot stack. */
    public void setHeat(ItemStack stack, int heat) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(HEAT_TAG, heat);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (heat > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (this.dusted) {
            // ItemHotDusted.addInformation: I18nUtil.resolveKey("item.hot_dusted.forged", stack.getItemDamage())
            tooltip.add(Component.translatable("item.hot_dusted.forged", grade(stack)));
        }
    }
}

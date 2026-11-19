package com.atomiclegacy.tileentity;

import com.atomiclegacy.drone.DroneItemMatcher;
import com.atomiclegacy.items.BedrockOreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * [parity X1: custom machines] ModulePatternMatcher (HbmMods 936c60c). Ore dictionary names are the stack's item tags
 * in their reported order; the standard mode is DroneItemMatcher.defaultMode (flattened items have no subtypes).
 */
public final class ModulePatternMatcher {
    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public static final String MODE_BEDROCK = "bedrock";
    public String[] modes;

    public ModulePatternMatcher(int count) {
        this.modes = new String[count];
    }

    private static List<String> names(ItemStack stack) {
        return stack.getTags().map(TagKey::location).map(ResourceLocation::toString).toList();
    }

    private static String standard(ItemStack stack) {
        return stack.getItem() instanceof BedrockOreItem ? MODE_BEDROCK : DroneItemMatcher.defaultMode(stack);
    }

    public void initPatternSmart(Level world, ItemStack stack, int i) {
        if (world.isClientSide) return;

        if (stack.isEmpty()) {
            modes[i] = null;
            return;
        }

        List<String> names = names(stack);

        if (iterateAndCheck(names, i, "c:ingots/")) return;
        if (iterateAndCheck(names, i, "c:storage_blocks/")) return;
        if (iterateAndCheck(names, i, "c:dusts/")) return;
        if (iterateAndCheck(names, i, "c:nuggets/")) return;
        if (iterateAndCheck(names, i, "c:plates/")) return;

        modes[i] = standard(stack);
    }

    private boolean iterateAndCheck(List<String> names, int i, String prefix) {
        for (String s : names) {
            if (s.startsWith(prefix)) {
                modes[i] = s;
                return true;
            }
        }
        return false;
    }

    public void nextMode(Level world, ItemStack pattern, int i) {
        if (world.isClientSide) return;

        if (pattern.isEmpty()) {
            modes[i] = null;
            return;
        }

        if (modes[i] == null) {
            modes[i] = MODE_EXACT;
        } else if (MODE_EXACT.equals(modes[i])) {
            modes[i] = pattern.getItem() instanceof BedrockOreItem ? MODE_BEDROCK : MODE_WILDCARD;
        } else if (MODE_BEDROCK.equals(modes[i])) {
            modes[i] = MODE_WILDCARD;
        } else if (MODE_WILDCARD.equals(modes[i])) {
            List<String> names = names(pattern);
            modes[i] = names.isEmpty() ? MODE_EXACT : names.get(0);
        } else {
            List<String> names = names(pattern);
            if (names.size() < 2 || modes[i].equals(names.get(names.size() - 1))) {
                modes[i] = MODE_EXACT;
            } else {
                for (int j = 0; j < names.size() - 1; j++) {
                    if (modes[i].equals(names.get(j))) {
                        modes[i] = names.get(j + 1);
                        return;
                    }
                }
            }
        }
    }

    public boolean isValidForFilter(ItemStack filter, int index, ItemStack input) {
        String mode = modes[index];

        if (mode == null) {
            modes[index] = mode = MODE_EXACT;
        }

        switch (mode) {
            case MODE_EXACT: return ItemStack.isSameItemSameComponents(input, filter);
            case MODE_WILDCARD: return input.getItem() == filter.getItem();
            case MODE_BEDROCK:
                if (input.getItem() != filter.getItem()) return false;
                if (!(input.getItem() instanceof BedrockOreItem)) return false;
                return BedrockOreItem.gradeOf(input) == BedrockOreItem.gradeOf(filter);
            default:
                return names(input).contains(mode);
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        for (int i = 0; i < modes.length; i++) {
            modes[i] = nbt.contains("mode" + i) ? nbt.getString("mode" + i) : null;
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) nbt.putString("mode" + i, modes[i]);
        }
    }

    public static String getLabel(String mode) {
        switch (mode) {
            case MODE_EXACT: return ChatFormatting.YELLOW + "Item and meta match";
            case MODE_WILDCARD: return ChatFormatting.YELLOW + "Item matches";
            case MODE_BEDROCK: return ChatFormatting.YELLOW + "Item and bedrock grade match";
            default: return ChatFormatting.YELLOW + "Ore dict key matches: " + mode;
        }
    }
}

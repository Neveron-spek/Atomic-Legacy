package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MachineUpgradeItem extends Item {
    private final UpgradeType type;
    private final int tier;

    public MachineUpgradeItem(Properties properties) {
        this(properties, UpgradeType.SPECIAL, 0);
    }

    public MachineUpgradeItem(Properties properties, UpgradeType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    /**
     * [parity e3] ItemMachineUpgrade.addInformation: outside of a machine GUI only these upgrades have text (hard-coded
     * English literals, first line red, the rest in the default tooltip colour). The IUpgradeInfoProvider lines that
     * 1.7.10 shows while a compatible machine GUI is open are not part of the item itself.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        switch (registryPath(stack)) {
            case "upgrade_radius" -> lines(tooltip, "Forcefield Range Upgrade",
                    "Radius +16 / Consumption +500", "Stacks to 16");
            case "upgrade_health" -> lines(tooltip, "Forcefield Health Upgrade",
                    "Max. Health +50 / Consumption +250", "Stacks to 16");
            case "upgrade_smelter" -> lines(tooltip, "Mining Laser Upgrade", "Smelts blocks. Easy enough.");
            case "upgrade_shredder" -> lines(tooltip, "Mining Laser Upgrade", "Crunches ores");
            case "upgrade_centrifuge" -> lines(tooltip, "Mining Laser Upgrade", "Hopefully self-explanatory");
            case "upgrade_crystallizer" -> lines(tooltip, "Mining Laser Upgrade", "Your new best friend");
            case "upgrade_screm" -> lines(tooltip, "Mining Laser Upgrade",
                    "It's like in Super Mario where all blocks are",
                    "actually Toads, but here it's Half-Life scientists",
                    "and they scream. A lot.");
            case "upgrade_nullifier" -> lines(tooltip, "Mining Laser Upgrade",
                    "50% chance to override worthless items with /dev/zero",
                    "50% chance to move worthless items to /dev/null");
            case "upgrade_gc_speed" -> {
                lines(tooltip, "Gas Centrifuge Upgrade", "Allows for total isotopic separation of HEUF6");
                tooltip.add(Component.literal("also your centrifuge goes sicko mode").withStyle(ChatFormatting.YELLOW));
            }
            default -> {
            }
        }
    }

    private static void lines(List<Component> tooltip, String title, String... rest) {
        tooltip.add(Component.literal(title).withStyle(ChatFormatting.RED));
        for (String line : rest) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

    public static boolean isMachineUpgrade(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MachineUpgradeItem)) {
            return false;
        }
        String path = registryPath(stack);
        return path.startsWith("upgrade_")
                && !path.equals("upgrade_template")
                && !path.equals("upgrade_muffler");
    }

    public static UpgradeType upgradeType(ItemStack stack) {
        if (stack.getItem() instanceof MachineUpgradeItem upgrade) {
            return upgrade.typeFor(stack);
        }
        return UpgradeType.SPECIAL;
    }

    public static int upgradeTier(ItemStack stack) {
        if (stack.getItem() instanceof MachineUpgradeItem upgrade) {
            return upgrade.tierFor(stack);
        }
        return 0;
    }

    private UpgradeType typeFor(ItemStack stack) {
        if (this.type != UpgradeType.SPECIAL || this.tier != 0) {
            return this.type;
        }

        String path = registryPath(stack);
        if (path.startsWith("upgrade_speed_") || path.startsWith("upgrade_stack_")) {
            return UpgradeType.SPEED;
        }
        if (path.startsWith("upgrade_effect_")) {
            return UpgradeType.EFFECT;
        }
        if (path.startsWith("upgrade_power_")) {
            return UpgradeType.POWER;
        }
        if (path.startsWith("upgrade_fortune_")) {
            return UpgradeType.FORTUNE;
        }
        if (path.startsWith("upgrade_afterburn_")) {
            return UpgradeType.AFTERBURN;
        }
        if (path.startsWith("upgrade_overdrive_")) {
            return UpgradeType.OVERDRIVE;
        }
        if (path.startsWith("upgrade_smelter")) {
            return UpgradeType.SMELTER;
        }
        if (path.startsWith("upgrade_shredder")) {
            return UpgradeType.SHREDDER;
        }
        if (path.startsWith("upgrade_centrifuge")) {
            return UpgradeType.CENTRIFUGE;
        }
        if (path.startsWith("upgrade_crystallizer")) {
            return UpgradeType.CRYSTALLIZER;
        }
        if (path.equals("upgrade_nullifier")) {
            return UpgradeType.NULLIFIER;
        }
        if (path.equals("upgrade_screm")) {
            return UpgradeType.SCREAM;
        }
        return UpgradeType.SPECIAL;
    }

    private int tierFor(ItemStack stack) {
        if (this.type != UpgradeType.SPECIAL || this.tier != 0) {
            return this.tier;
        }

        String path = registryPath(stack);
        if (path.startsWith("upgrade_stack_")) {
            return 1;
        }

        int lastUnderscore = path.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == path.length() - 1) {
            return 0;
        }

        try {
            return Integer.parseInt(path.substring(lastUnderscore + 1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String registryPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) {
            return "";
        }
        return id.getPath();
    }

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        SMELTER,
        SHREDDER,
        CENTRIFUGE,
        CRYSTALLIZER,
        NULLIFIER,
        SCREAM,
        SPECIAL
    }
}

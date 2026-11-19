package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Chicago Pile rod item with the original common and type-specific descriptions. */
public final class PileRodItem extends Item {
    private final Kind kind;

    public PileRodItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // ItemPileRod.addInformation: desc.item.pileRod and <name>.desc, both split at "$" (colours are in the lang text)
        LegacyTooltips.keyLines(tooltip, "desc.item.pileRod");
        LegacyTooltips.keyLines(tooltip, getDescriptionId() + ".desc");
    }

    public enum Kind {
        URANIUM(ChatFormatting.GREEN,
                "tooltip.atomiclegacy.pile_rod.uranium",
                "tooltip.atomiclegacy.pile_rod.inspect"),
        PU239(ChatFormatting.GREEN,
                "tooltip.atomiclegacy.pile_rod.pu239"),
        PLUTONIUM(ChatFormatting.LIGHT_PURPLE,
                "tooltip.atomiclegacy.pile_rod.source"),
        SOURCE(ChatFormatting.LIGHT_PURPLE,
                "tooltip.atomiclegacy.pile_rod.source"),
        BORON(ChatFormatting.BLUE,
                "tooltip.atomiclegacy.pile_rod.boron"),
        LITHIUM(ChatFormatting.GREEN,
                "tooltip.atomiclegacy.pile_rod.lithium",
                "tooltip.atomiclegacy.pile_rod.inspect"),
        DETECTOR(ChatFormatting.BLUE,
                "tooltip.atomiclegacy.pile_rod.detector");

        private final ChatFormatting tooltipColor;
        private final String[] tooltipKeys;

        Kind(ChatFormatting tooltipColor, String... tooltipKeys) {
            this.tooltipColor = tooltipColor;
            this.tooltipKeys = tooltipKeys;
        }

        public ChatFormatting tooltipColor() {
            return this.tooltipColor;
        }

        public String[] tooltipKeys() {
            return this.tooltipKeys;
        }
    }
}

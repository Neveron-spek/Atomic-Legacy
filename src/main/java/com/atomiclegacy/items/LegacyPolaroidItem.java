package com.atomiclegacy.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct 1.7.10 ItemPolaroid passive low-health protection. */
public final class LegacyPolaroidItem extends Item {
    public LegacyPolaroidItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && player.getHealth() < 10.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 2));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("tooltip.atomiclegacy.polaroid.fate").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        for (String line : polaroidLines(com.atomiclegacy.AtomicLegacy.polaroidID)) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

    /** [parity J1] ItemPolaroid.addInformation: the line(s) after "Fate chosen" and the empty line, per polaroidID. */
    public static List<String> polaroidLines(int polaroidID) {
        return switch (polaroidID) {
            case 1 -> List.of("...");
            case 2 -> List.of("Clear as glass.");
            case 3 -> List.of("'M");
            case 4 -> List.of("It's about time.");
            case 5 -> List.of("If you stare long into the abyss, the abyss stares back.");
            case 6 -> List.of("public Party celebration = new Party();");
            case 7 -> List.of("V urnerq lbh yvxr EBG13!");
            case 8 -> List.of("11011100");
            case 9 -> List.of("Vg'f nobhg gvzr.");
            case 10 -> List.of("Schrabidium dislikes the breeding reactor.");
            case 11 -> List.of("yss stares back.6public Party cel");
            case 12 -> List.of("Red streaks.");
            case 13 -> List.of("Q1");
            case 14 -> List.of("Q4");
            case 15 -> List.of("Q3");
            case 16 -> List.of("Q2");
            case 17 -> List.of("Two friends before christmas.");
            case 18 -> List.of("Duchess of the boxcars.", "", "\"P.S.: Thirty-one.\"", "\"Huh, what does thirty-one mean?\"");
            default -> List.of();
        };
    }
}

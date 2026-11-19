package com.atomiclegacy.items;

import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Exact diagnostic output and formula from ItemDigammaDiagnostic. */
public final class LegacyDigammaDiagnosticItem extends Item {
    public LegacyDigammaDiagnosticItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            HbmLivingRadiation data = HbmLivingRadiation.get(player);
            double digamma = ((int) (data.getDigamma() * 100.0D)) / 100.0D;
            double influence = ((int) ((1.0D - Math.pow(0.5D, digamma)) * 10000.0D)) / 100.0D;

            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("===== Ϝ ")
                    .append(Component.translatable("digamma.title"))
                    .append(Component.literal(" Ϝ ====="))
                    .withStyle(ChatFormatting.DARK_PURPLE));
            player.sendSystemMessage(Component.translatable("digamma.playerDigamma")
                    .append(Component.literal(" " + digamma + " DRX").withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.translatable("digamma.playerHealth")
                    .append(Component.literal(" " + influence + "%").withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.translatable("digamma.playerRes")
                    .append(Component.literal(" N/A").withStyle(ChatFormatting.BLUE))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

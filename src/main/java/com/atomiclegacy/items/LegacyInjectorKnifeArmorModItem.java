package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct port of ItemModKnife's 50-tick cumulative maximum-health loss. */
public final class LegacyInjectorKnifeArmorModItem extends ArmorModItem {
    public static final net.minecraft.resources.ResourceLocation HEALTH_MODIFIER = AtomicLegacy.id("injector_knife_health");

    public LegacyInjectorKnifeArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.tickCount % 50 != 0 || player.getMaxHealth() <= 2.0F) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double previousMaximum = player.getMaxHealth();
        maxHealth.removeModifier(HEALTH_MODIFIER);
        double unmodifiedMaximum = player.getMaxHealth();
        maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                HEALTH_MODIFIER,
                -(unmodifiedMaximum - previousMaximum + 2.0D),
                AttributeModifier.Operation.ADD_VALUE
        ));
        // [parity I2] hbm:entity.slicer; "bloodvomit" has no client handler in 1.7.10; properJolt to the wearer
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.atomiclegacy.registry.HbmSoundEvents.ENTITY_SLICER.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int time = 0, maxTime = 0;
            if (player.getMaxHealth() > 2F) {
                time = 10000 + player.getRandom().nextInt(10000);
                maxTime = 10000;
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, new com.atomiclegacy.network.ArmorParityPayloads.Effect(
                    com.atomiclegacy.network.ArmorParityPayloads.Effect.PROPER_JOLT, player.getId(), time, maxTime));
        }
        if (player.getMaxHealth() <= 2.0F) {
            HbmAdvancements.award(player, "some_wounds");
        }
    }

    // [parity I2: tooltip] ItemModKnife
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.RED, "Pain."));
        tooltip.add(Component.empty());
        tooltip.add(line(ChatFormatting.RED, "Hurts, doesn't it?"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.RED, mod, ""));
    }
}

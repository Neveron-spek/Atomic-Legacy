package com.atomiclegacy.items;

import com.atomiclegacy.registry.HbmMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** 1.7.10 ItemCanteen for canteen_vodka. */
public final class LegacyCanteenItem extends Item {
    private static final int COOLDOWN_TICKS = 3 * 60;

    public LegacyCanteenItem(Properties properties) {
        super(properties.durability(COOLDOWN_TICKS).stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide && stack.getDamageValue() > 0 && entity.tickCount % 20 == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() != 0 ||LegacyConsumableSupport.hasPotionSickness(player)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        result.setDamageValue(result.getMaxDamage());
        if (!level.isClientSide && entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 2));
            // [parity H1] VersatileConfig.applyPotionSickness; ItemCanteen plays no extra sound.
            LegacyConsumableSupport.applyPotionSickness(player, 5);
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity H1] ItemCanteen.addInformation: literal lines; [parity J1] polaroidID == 11 variant of the last line.
        LegacyConsumableSupport.literal(tooltip, "Cooldown: 3 minutes");
        LegacyConsumableSupport.literal(tooltip, "Nausea I for 10 seconds");
        LegacyConsumableSupport.literal(tooltip, "Strength III for 30 seconds");
        tooltip.add(Component.empty());
        LegacyConsumableSupport.literal(tooltip, com.atomiclegacy.AtomicLegacy.polaroidID == 11
                ? "Time to get hammered & sickled!" : "Smells like disinfectant, tastes like disinfectant.");
    }
}

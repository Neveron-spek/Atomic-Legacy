package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.render.CrucibleItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.function.Consumer;

/** The charge-gated three-hit 1.7.10 Crucible sword. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class LegacyCrucibleItem extends Item implements IAnimatedItem<com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation> {
    private static final java.util.Random itemRand = new java.util.Random();
    private static final int MAX_CHARGE = 3;
    private static final ItemAttributeModifiers ACTIVE_ATTRIBUTES = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5_000.0D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(AtomicLegacy.id("crucible_movement"), 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
            .build();

    public LegacyCrucibleItem(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_CHARGE).component(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ACTIVE_ATTRIBUTES));
    }

    public static boolean isCharged(ItemStack stack) {
        return !stack.isEmpty() && stack.getDamageValue() < stack.getMaxDamage();
    }

    private static void consumeCharge(ItemStack stack) {
        if (!isCharged(stack)) {
            return;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final CrucibleItemRenderer renderer = new CrucibleItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }

            /** ItemRenderer#renderItemInFirstPerson with the crucible's disabled swing (see CrucibleItemRenderer). */
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm, ItemStack itemInHand, float partialTick,
                                                   float equipProcess, float swingProcess) {
                CrucibleItemRenderer.applyLegacyFirstPersonTransform(poseStack);
                return true;
            }
        });
    }

    /** IEquipReceiver#onEquip, server side only. */
    public void onEquip(Player player, ItemStack stack) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer)) {
            return;
        }
        if (isCharged(player.getMainHandItem())) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.atomiclegacy.registry.HbmSoundEvents.CRUCIBLE_DEPLOY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            playAnimation(player, com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.EQUIP);
        }
    }

    /** ModEventHandler: IEquipReceiver fires when the held item changes to a different item. */
    @SubscribeEvent
    public static void onEquipmentChange(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (event.getSlot() != net.minecraft.world.entity.EquipmentSlot.MAINHAND
                || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        ItemStack to = event.getTo();
        if (to.getItem() instanceof LegacyCrucibleItem crucible && (event.getFrom().isEmpty() || event.getFrom().getItem() != to.getItem())) {
            crucible.onEquip(player, to);
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entityLiving, net.minecraft.world.InteractionHand hand) {
        if (!(entityLiving instanceof net.minecraft.server.level.ServerPlayer player)) {
            return false;
        }
        if (!isCharged(stack)) {
            return false;
        }
        playAnimation(player, com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.SWING);
        return false;
    }

    @Override
    public com.atomiclegacy.render.anim.BusAnimation getAnimation(com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation type, ItemStack stack) {
        /* crucible deploy */
        if (type == com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.EQUIP) {
            return new com.atomiclegacy.render.anim.BusAnimation()
                    .addBus("GUARD_ROT", new com.atomiclegacy.render.anim.BusAnimationSequence()
                            .addPos(90, 0, 1, 0)
                            .addPos(90, 0, 1, 800)
                            .addPos(0, 0, 1, 50));
        }

        /* crucible swing */
        if (type == com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.SWING) {
            if (com.atomiclegacy.client.gun.HbmAnimations.getRelevantTransformation("SWING_ROT")[0] == 0) {
                int offset = itemRand.nextInt(80) - 20;
                CrucibleItemRenderer.playSwing(0.8F + itemRand.nextFloat() * 0.2F);
                return new com.atomiclegacy.render.anim.BusAnimation()
                        .addBus("SWING_ROT", new com.atomiclegacy.render.anim.BusAnimationSequence()
                                .addPos(90 - offset, 90 - offset, 35, 75)
                                .addPos(90 + offset, 90 - offset, -45, 150)
                                .addPos(0, 0, 0, 500))
                        .addBus("SWING_TRANS", new com.atomiclegacy.render.anim.BusAnimationSequence()
                                .addPos(-3, 0, 0, 75)
                                .addPos(8, 0, 0, 150)
                                .addPos(0, 0, 0, 500));
            }
        }
        return null;
    }

    @Override
    public Class<com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation> getEnum() {
        return com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.class;
    }

    @Override
    public boolean shouldPlayerModelAim(ItemStack stack) {
        return false;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!isCharged(stack)) {
            return;
        }

        Level level = attacker.level();
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.75F + target.getRandom().nextFloat() * 0.2F);
        if (!level.isClientSide && !target.isAlive() && level instanceof ServerLevel serverLevel) {
            int count = Math.min((int) Math.ceil(target.getMaxHealth() / 3.0D), 250) * 4;
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), count,
                    0.0D, 0.0D, 0.0D, 0.1D);
        }
        consumeCharge(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        StringBuilder charge = new StringBuilder("Charge [");
        for (int i = MAX_CHARGE - 1; i >= 0; i--) {
            charge.append(stack.getDamageValue() <= i ? "||||||" : "   ");
        }
        tooltip.add(Component.literal(charge.append(']').toString()).withStyle(ChatFormatting.RED));
    }

    @SubscribeEvent
    public static void cancelEmptyAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof LegacyCrucibleItem) || isCharged(held)) {
            return;
        }
        event.setCanceled(true);
        if (attacker instanceof Player player && !player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.atomiclegacy.crucible.no_energy").withStyle(ChatFormatting.RED), true);
        }
    }
}

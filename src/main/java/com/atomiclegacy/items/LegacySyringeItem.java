package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.event.MkuContagionEvents;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

/**
 * Direct-use medical consumables of 1.7.10 ItemSyringe (metal syringes,
 * med_bag, syringe_taint, syringe_mkunicorn) and ItemSimpleConsumable
 * (syringe_antidote/poison/awesome, iv_empty/iv_blood, radaway*).
 * [parity H1] Every branch follows the original line by line; numbers are
 * quoted from items/special/ItemSyringe.java and ItemSimpleConsumable.java.
 */
public class LegacySyringeItem extends Item {
    private final String id;

    public LegacySyringeItem(Properties properties, String id) {
        // ItemSimpleConsumable.init: syringe_awesome .setRarity(EnumRarity.uncommon).setEffect()
        super(id.equals("syringe_awesome") ? properties.rarity(Rarity.UNCOMMON) : properties);
        this.id = id;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return id.equals("syringe_awesome") || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // ItemSyringe.onItemRightClick has no branch for the MKU syringe: it only works on hit.
        if (id.equals("syringe_mkunicorn")) {
            return InteractionResultHolder.pass(stack);
        }
        // 1.7.10 ItemInWorldManager.tryUseItem restored the stack size of creative players.
        boolean consume = !player.getAbilities().instabuild;
        switch (id) {
            case "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super" -> {
                if (LegacyConsumableSupport.hasPotionSickness(player)) {
                    return InteractionResultHolder.pass(stack);
                }
                if (!level.isClientSide) {
                    applyMetal(player);
                    if (consume) {
                        stack.shrink(1);
                    }
                    sound(level, player, HbmSoundEvents.ITEM_SYRINGE.get());
                    if (stack.isEmpty()) {
                        // "return new ItemStack(syringe_metal_empty)" came before applyPotionSickness.
                        return InteractionResultHolder.sidedSuccess(stackOf("syringe_metal_empty"), false);
                    }
                    LegacyConsumableSupport.giveOrDrop(player, stackOf("syringe_metal_empty"));
                    LegacyConsumableSupport.applyPotionSickness(player, id.equals("syringe_metal_super") ? 15 : 5);
                }
            }
            case "med_bag" -> {
                if (LegacyConsumableSupport.hasPotionSickness(player)) {
                    return InteractionResultHolder.pass(stack);
                }
                if (!level.isClientSide) {
                    player.setHealth(player.getMaxHealth());
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.CONFUSION);
                    player.removeEffect(MobEffects.DIG_SLOWDOWN);
                    player.removeEffect(MobEffects.HUNGER);
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WEAKNESS);
                    player.removeEffect(MobEffects.WITHER);
                    player.removeEffect(com.atomiclegacy.registry.HbmMobEffects.RADIATION); // [parity J1]
                    LegacyConsumableSupport.applyPotionSickness(player, 15);
                    if (consume) {
                        stack.shrink(1);
                    }
                }
            }
            case "syringe_taint" -> {
                if (!level.isClientSide) {
                    player.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 60 * 20, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
                    if (consume) {
                        stack.shrink(1);
                    }
                    sound(level, player, HbmSoundEvents.ITEM_SYRINGE.get());
                    LegacyConsumableSupport.giveOrDrop(player, stackOf("syringe_metal_empty"));
                    LegacyConsumableSupport.giveOrDrop(player, stackOf("bottle2_empty"));
                }
            }
            default -> {
                if (!level.isClientSide) {
                    int before = stack.getCount();
                    simpleConsumable(level, stack, player, player);
                    if (!consume) {
                        stack.setCount(before);
                    }
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        if (level.isClientSide) {
            return false;
        }
        switch (id) {
            case "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super" -> {
                if (!LegacyConsumableSupport.hasPotionSickness(target)) {
                    applyMetal(target);
                    LegacyConsumableSupport.applyPotionSickness(target, id.equals("syringe_metal_super") ? 15 : 5);
                    stack.shrink(1);
                    sound(level, target, HbmSoundEvents.ITEM_SYRINGE.get());
                    if (attacker instanceof Player player) {
                        LegacyConsumableSupport.giveOrDrop(player, stackOf("syringe_metal_empty"));
                    }
                }
            }
            case "syringe_taint" -> {
                target.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 60 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
                stack.shrink(1);
                sound(level, target, HbmSoundEvents.ITEM_SYRINGE.get());
                if (attacker instanceof Player player) {
                    LegacyConsumableSupport.giveOrDrop(player, stackOf("syringe_metal_empty"));
                    LegacyConsumableSupport.giveOrDrop(player, stackOf("bottle2_empty"));
                }
            }
            case "syringe_mkunicorn" -> {
                // HbmLivingProps.setContagion(entity, 3 * 60 * 60 * 20)
                MkuContagionEvents.infect(target);
                sound(level, target, HbmSoundEvents.ITEM_SYRINGE.get());
                stack.shrink(1);
            }
            case "syringe_antidote", "syringe_poison", "syringe_awesome" -> simpleConsumable(level, stack, target, attacker);
            default -> {
            }
        }
        return false;
    }

    /** ItemSyringe: the effect part shared by onItemRightClick and hitEntity. */
    private void applyMetal(LivingEntity target) {
        switch (id) {
            case "syringe_metal_stimpak" -> target.heal(5.0F);
            case "syringe_metal_medx" -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4 * 60 * 20, 2));
            case "syringe_metal_psycho" -> {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 0));
            }
            case "syringe_metal_super" -> {
                target.heal(25.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 0));
            }
            default -> {
            }
        }
    }

    /** ItemSimpleConsumable use/hit delegates (server side only). */
    private void simpleConsumable(Level level, ItemStack stack, LivingEntity affected, LivingEntity source) {
        switch (id) {
            case "syringe_antidote" -> {
                if (LegacyConsumableSupport.hasPotionSickness(affected)) {
                    return;
                }
                affected.removeAllEffects();
                soundAndDecrement(level, stack, source, HbmSoundEvents.ITEM_SYRINGE.get(), "syringe_empty");
                LegacyConsumableSupport.applyPotionSickness(affected, 5);
            }
            case "syringe_poison" -> {
                if (affected == source) {
                    affected.hurt(affected.damageSources().source(affected.getRandom().nextBoolean()
                            ? HbmDamageTypes.EUTHANIZED_SELF : HbmDamageTypes.EUTHANIZED_SELF2), 30.0F);
                } else {
                    affected.hurt(affected.damageSources().source(HbmDamageTypes.EUTHANIZED, source, source), 30.0F);
                }
                soundAndDecrement(level, stack, source, HbmSoundEvents.ITEM_SYRINGE.get(), "syringe_empty");
            }
            case "syringe_awesome" -> {
                if (LegacyConsumableSupport.hasPotionSickness(affected)) {
                    return;
                }
                soundAndDecrement(level, stack, source, HbmSoundEvents.ITEM_SYRINGE.get(), "syringe_empty");
                affected.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50 * 20, 9));
                affected.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50 * 20, 9));
                affected.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 50 * 20, 0));
                affected.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50 * 20, 24));
                affected.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50 * 20, 9));
                affected.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50 * 20, 6));
                affected.addEffect(new MobEffectInstance(MobEffects.JUMP, 50 * 20, 9));
                affected.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 50 * 20, 9));
                affected.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 50 * 20, 4));
                affected.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 4));
                affected.addEffect(new MobEffectInstance(HbmMobEffects.RADX, 50 * 20, 9));
                LegacyConsumableSupport.applyPotionSickness(affected, 5);
            }
            case "iv_empty" -> {
                // EntityDamageUtil.isRealPlayer
                if (!(source instanceof Player player) || player instanceof FakePlayer) {
                    return;
                }
                soundAndDecrement(level, stack, player, HbmSoundEvents.ITEM_SYRINGE.get(), "iv_blood");
                player.setHealth(Math.max(player.getHealth() - 5.0F, 0.0F));
                if (player.getHealth() <= 0.0F) {
                    player.die(player.damageSources().magic());
                }
            }
            case "iv_blood" -> {
                soundAndDecrement(level, stack, source, HbmSoundEvents.ITEM_RADAWAY.get(), "iv_empty");
                source.heal(5.0F);
            }
            case "radaway" -> radaway(level, stack, source, 140);
            case "radaway_strong" -> radaway(level, stack, source, 350);
            case "radaway_flush" -> radaway(level, stack, source, 500);
            default -> {
            }
        }
    }

    /** ItemSimpleConsumable.doRadaway: amplifier 0, an active RadAway is extended. */
    private static void radaway(Level level, ItemStack stack, LivingEntity user, int duration) {
        soundAndDecrement(level, stack, user, HbmSoundEvents.ITEM_RADAWAY.get(), "iv_empty");
        LegacyConsumableSupport.addStackingEffect(user, HbmMobEffects.RADAWAY, duration, 0);
    }

    /** ItemSimpleConsumable.giveSoundAndDecrement. */
    private static void soundAndDecrement(Level level, ItemStack stack, LivingEntity entity, SoundEvent sound, String container) {
        stack.shrink(1);
        sound(level, entity, sound);
        if (entity instanceof Player player) {
            LegacyConsumableSupport.giveOrDrop(player, stackOf(container));
        }
    }

    private static void sound(Level level, LivingEntity at, SoundEvent sound) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static ItemStack stackOf(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // ItemSyringe.addInformation (literal English lines); the ItemSimpleConsumable
        // items use the optional "<name>.desc" key of ItemCustomLore instead.
        switch (id) {
            case "syringe_metal_medx" -> LegacyConsumableSupport.literal(tooltip, "Resistance III for 4 minutes");
            case "syringe_metal_psycho" -> {
                LegacyConsumableSupport.literal(tooltip, "Resistance I for 2 minutes");
                LegacyConsumableSupport.literal(tooltip, "Strength I for 2 minutes");
            }
            case "syringe_metal_stimpak" -> LegacyConsumableSupport.literal(tooltip, "Heals 2.5 hearts");
            case "syringe_metal_super" -> {
                LegacyConsumableSupport.literal(tooltip, "Heals 25 hearts");
                LegacyConsumableSupport.literal(tooltip, "Slowness I for 10 seconds");
            }
            case "med_bag" -> {
                LegacyConsumableSupport.literal(tooltip, "Full heal, regardless of max health");
                LegacyConsumableSupport.literal(tooltip, "Removes negative effects");
            }
            case "syringe_taint" -> {
                LegacyConsumableSupport.literal(tooltip, "Tainted I for 60 seconds");
                LegacyConsumableSupport.literal(tooltip, "Nausea I for 5 seconds");
                LegacyConsumableSupport.literal(tooltip, "Cloud damage + taint = tainted heart effect");
            }
            case "syringe_mkunicorn" -> tooltip.add(Component.literal("?").withStyle(ChatFormatting.RED));
            default -> LegacyConsumableSupport.loreLines(tooltip, getDescriptionId() + ".desc");
        }
    }
}

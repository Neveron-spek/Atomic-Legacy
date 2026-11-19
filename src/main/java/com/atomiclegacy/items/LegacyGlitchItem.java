package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyVortexEntity;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.entity.MeteorEntity;
import com.atomiclegacy.registry.HbmEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The one-use randomized ItemGlitch effect table. */
public final class LegacyGlitchItem extends Item {
    public LegacyGlitchItem(Properties properties) {
        super(properties.stacksTo(1).durability(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int roll = level.random.nextInt(31);
            switch (roll) {
                case 2 -> player.hurt(level.damageSources().source(HbmDamageTypes.RADIATION), 1000.0F);
                case 4 -> LegacyVortexEntity.spawn(level, player.position(), 1.5F, 0.0025F, false);
                case 5, 6, 7 -> player.addItem(new ItemStack(HbmBlocks.BLOCK_METEOR_TREASURE.get(), roll == 5 ? 1 : roll == 6 ? 3 : 10));
                case 8 -> { // [parity S4: ammo_container] ItemGlitch case 8 (addChatMessage: chat, not the action bar)
                    player.addItem(new ItemStack(com.atomiclegacy.registry.HbmItems.AMMO_CONTAINER.get(), 10));
                    player.displayClientMessage(Component.literal("Oh, and by the way: The polaroid shifts reality. Things can be different if the polaroid is broken."), false);
                }
                case 9 -> player.addItem(new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("nuke_advanced_kit"))));
                case 10 -> player.addItem(new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("nuke_starter_kit"))));
                case 14 -> {
                    player.getInventory().dropAll();
                    level.explode(player, player.getX(), player.getY(), player.getZ(), 0.0F, Level.ExplosionInteraction.NONE);
                }
                case 15 -> player.addItem(new ItemStack(net.minecraft.world.item.Items.DIRT, 36 * 64));
                case 21 -> {
                    player.addItem(new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("missile_nuclear"))));
                    player.displayClientMessage(Component.translatable("message.atomiclegacy.glitch.missile"), true);
                }
                case 24 -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 9));
                case 25 -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 9));
                case 26 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 * 20, 9));
                case 27 -> LegacyVortexEntity.spawn(level, player.position().add(0, -15, 0), 2.5F, 0.0025F, false);
                case 28 -> {
                    MeteorEntity meteor = new MeteorEntity(HbmEntityTypes.METEOR.get(), level);
                    meteor.setPos(player.getX(), player.getY() + 100.0D, player.getZ());
                    level.addFreshEntity(meteor);
                }
                case 29 -> NukeExplosionManager.scheduleLegacyNuke((net.minecraft.server.level.ServerLevel) level, player.getX(), player.getY(), player.getZ(), 27);
                default -> player.displayClientMessage(Component.translatable("message.atomiclegacy.glitch.nothing"), true);
            }
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // ItemGlitch.addInformation: default colour, an empty line and the MainRegistry.polaroidID line
        LegacyTooltips.line(tooltip, "It's a gamble!");
        LegacyTooltips.line(tooltip, "");
        String line = glitchLine(com.atomiclegacy.AtomicLegacy.polaroidID);
        if (line != null) {
            LegacyTooltips.line(tooltip, line);
        }
    }

    /** [parity J1] ItemGlitch.addInformation switch(MainRegistry.polaroidID). */
    public static String glitchLine(int polaroidID) {
        return switch (polaroidID) {
            case 1 -> "Click-click-click!";
            case 2 -> "Creek!";
            case 3 -> "Bzzzt!";
            case 4 -> "TS staring off into space.";
            case 5 -> "BANG!!";
            case 6 -> "Woop!";
            case 7 -> "Poow!";
            case 8 -> "Pft!";
            case 9 -> "GF fgnevat bss vagb fcnpr.";
            case 10 -> "Backup memory #8 on 1.44 million bytes.";
            case 11 -> "PTANG!";
            case 12 -> "Bzzt-zrrt!";
            case 13 -> "Clang, click-brrthththrtrtrtrtrtr!";
            case 14 -> "KABLAM!";
            case 15 -> "PLENG!";
            case 16 -> "Wheeeeeeee-";
            case 17 -> "Thump.";
            case 18 -> "BANG! Choo-chooo! B A N G ! ! !";
            default -> null;
        };
    }
}

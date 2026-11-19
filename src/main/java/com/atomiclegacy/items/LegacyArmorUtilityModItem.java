package com.atomiclegacy.items;

import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The small, behavior-only armor modifications from 1.7.10. They share the
 * old armor-mod storage but keep their individual trigger conditions here.
 */
public final class LegacyArmorUtilityModItem extends ArmorModItem {
    public enum Kind {
        POLISH,
        BANDAID,
        SERUM,
        QUARTZ,
        MORNING_GLORY,
        SPIDER_MILK,
        INK,
        AUTO_INJECTOR
    }

    private final Kind kind;

    public LegacyArmorUtilityModItem(Item.Properties properties, Kind kind) {
        super(properties, slot(kind), helmet(kind), chest(kind), legs(kind), boots(kind));
        this.kind = kind;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return switch (this.kind) {
            case POLISH -> player.getRandom().nextInt(20) == 0 ? 0.0F : amount;
            case BANDAID -> {
                if (player.getRandom().nextInt(100) < 3) {
                    player.heal(player.getMaxHealth()); // [parity I2] heal(), as the original
                    yield 0.0F;
                }
                yield amount;
            }
            case QUARTZ -> {
                HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
                radiation.setRadiation(Math.max(0.0F, radiation.getRadiation() - 10.0F));
                yield amount;
            }
            case MORNING_GLORY -> {
                if (player.getRandom().nextInt(20) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                }
                yield amount;
            }
            case INK -> {
                if (player.getRandom().nextInt(10) == 0) {
                    // [parity I2] Blocks.yellow_flower 10 %, then Blocks.red_flower with meta rand(9), dropped at +1.0
                    if (player.getRandom().nextInt(10) == 0) {
                        player.spawnAtLocation(new ItemStack(Items.DANDELION), 1.0F);
                    }
                    player.spawnAtLocation(new ItemStack(RED_FLOWERS[player.getRandom().nextInt(9)]), 1.0F);
                    yield 0.0F;
                }
                yield amount;
            }
            default -> amount;
        };
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        switch (this.kind) {
            case SERUM -> {
                if (player.hasEffect(MobEffects.POISON)) {
                    player.removeEffect(MobEffects.POISON);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 4));
                }
            }
            case MORNING_GLORY -> {
                if (player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
            }
            case SPIDER_MILK -> player.getActiveEffectsMap().keySet().stream()
                    .filter(effect -> effect.value().getCategory() == MobEffectCategory.HARMFUL)
                    .toList()
                    .forEach(player::removeEffect);
            case AUTO_INJECTOR -> {
                HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
                if (radiation.getDigamma() >= 5.0F) {
                    ArmorModHandler.removeMod(armor, ArmorModHandler.EXTRA);
                    radiation.setDigamma(radiation.getDigamma() - 5.0F);
                    player.addEffect(new MobEffectInstance(HbmMobEffects.STABILITY, 60 * 20, 0));
                    player.heal(20.0F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), com.atomiclegacy.registry.HbmSoundEvents.ITEM_SYRINGE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F); // [parity I2] hbm:item.syringe
                }
            }
            default -> {
            }
        }
    }

    // [parity I2: tooltip] ItemModPolish/Bandaid/Serum/Quartz/MorningGlory/Milk/Ink/Auto texts and colours.
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (this.kind) {
            case POLISH -> tooltip.add(line(ChatFormatting.BLUE, "5% chance to nullify damage"));
            case BANDAID -> tooltip.add(line(ChatFormatting.RED, "3% chance for full heal when damaged"));
            case SERUM -> tooltip.add(line(ChatFormatting.GREEN, "Cures poison and gives strength"));
            case QUARTZ -> tooltip.add(line(ChatFormatting.DARK_GRAY, "Taking damage removes 10 RAD"));
            case MORNING_GLORY -> tooltip.add(line(ChatFormatting.LIGHT_PURPLE, "5% chance to apply resistance when hit, wither immunity"));
            case SPIDER_MILK -> tooltip.add(line(ChatFormatting.WHITE, "Removes bad potion effects"));
            case INK -> {
                tooltip.add(line(ChatFormatting.LIGHT_PURPLE, "10% chance to nullify damage"));
                tooltip.add(line(ChatFormatting.LIGHT_PURPLE, "Flowers!"));
            }
            case AUTO_INJECTOR -> tooltip.add(line(ChatFormatting.BLUE, "Imported from Japsterdam."));
        }
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        switch (this.kind) {
            case POLISH -> tooltip.add(descLine(ChatFormatting.BLUE, mod, " (5% chance to nullify damage)"));
            case BANDAID -> tooltip.add(descLine(ChatFormatting.RED, mod, " (3% chance for full heal)"));
            case SERUM -> tooltip.add(descLine(ChatFormatting.BLUE, mod, " (replaces poison with strength)"));
            case QUARTZ -> tooltip.add(descLine(ChatFormatting.DARK_GRAY, mod, " (-10 RAD when hit)"));
            case MORNING_GLORY -> tooltip.add(descLine(ChatFormatting.LIGHT_PURPLE, mod, " (5% for resistance, wither immunity)"));
            case SPIDER_MILK -> tooltip.add(descLine(ChatFormatting.WHITE, mod, " (Removes bad potion effects)"));
            case INK -> tooltip.add(descLine(ChatFormatting.LIGHT_PURPLE, mod, " (10% chance to nullify damage)"));
            case AUTO_INJECTOR -> tooltip.add(descLine(ChatFormatting.BLUE, mod, ""));
        }
    }

    /** 1.7.10 Blocks.red_flower metadata 0..8. */
    private static final net.minecraft.world.item.Item[] RED_FLOWERS = {
            Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET, Items.RED_TULIP,
            Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP, Items.OXEYE_DAISY
    };

    private static int slot(Kind kind) {
        return kind == Kind.AUTO_INJECTOR ? ArmorModHandler.EXTRA : ArmorModHandler.EXTRA;
    }

    private static boolean helmet(Kind kind) {
        return kind != Kind.AUTO_INJECTOR;
    }

    private static boolean chest(Kind kind) {
        return true;
    }

    private static boolean legs(Kind kind) {
        return kind != Kind.AUTO_INJECTOR;
    }

    private static boolean boots(Kind kind) {
        return kind != Kind.AUTO_INJECTOR;
    }

    private static ChatFormatting color(Kind kind) {
        return switch (kind) {
            case POLISH -> ChatFormatting.BLUE;
            case BANDAID -> ChatFormatting.RED;
            case SERUM -> ChatFormatting.GREEN;
            case QUARTZ -> ChatFormatting.DARK_GRAY;
            case MORNING_GLORY, INK -> ChatFormatting.LIGHT_PURPLE;
            case SPIDER_MILK -> ChatFormatting.WHITE;
            case AUTO_INJECTOR -> ChatFormatting.BLUE;
        };
    }
}

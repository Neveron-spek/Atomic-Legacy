package com.atomiclegacy.items;

import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.util.ContaminationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public class RadiationSurveyItem extends Item {
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> GEIGER_SOUNDS = List.of(
            HbmSoundEvents.GEIGER_1,
            HbmSoundEvents.GEIGER_2,
            HbmSoundEvents.GEIGER_3,
            HbmSoundEvents.GEIGER_4,
            HbmSoundEvents.GEIGER_5,
            HbmSoundEvents.GEIGER_6
    );

    private final int indicatorColor;
    private final String descriptionKey;

    public RadiationSurveyItem(Properties properties, int indicatorColor, String descriptionKey) {
        super(properties);
        this.indicatorColor = indicatorColor;
        this.descriptionKey = descriptionKey;
    }

    // [parity I3] 1.7.10 ItemGeigerCounter/ItemDosimeter have no tooltip.

    // [parity J1] ItemGeigerCounter/ItemDosimeter.onUpdate: clicks are chosen from HbmLivingProps.getRadBuf only,
    // every 5 world ticks; idle click chance 1/50 (Geiger) and 1/100 (dosimeter).
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // [parity I3] ItemGeigerCounter/ItemDosimeter.onUpdate: any living holder, radBuf only.
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        float x = HbmLivingRadiation.get(living).getRadiationBuffer();
        if (level.getGameTime() % 5L != 0L) {
            return;
        }
        if ("dosimeter".equals(this.descriptionKey)) {
            playDosimeter(level, living, x);
        } else {
            playGeiger(level, living, x);
        }
    }

    // [parity J1] onItemRightClick: techBoop at the player, then ContaminationUtil.printGeigerData/printDosimeterData.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    HbmSoundEvents.TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if ("dosimeter".equals(this.descriptionKey)) {
                ContaminationUtil.printDosimeterData(player);
            } else {
                ContaminationUtil.printGeigerData(player);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Sound index (0 = silent) candidates of ItemGeigerCounter.onUpdate for radBuf x > 1E-5. */
    public static List<Integer> geigerChoices(float x) {
        List<Integer> list = new ArrayList<>(8);
        if (x < 1) list.add(0);
        if (x < 5) list.add(0);
        if (x < 10) list.add(1);
        if (x > 5 && x < 15) list.add(2);
        if (x > 10 && x < 20) list.add(3);
        if (x > 15 && x < 25) list.add(4);
        if (x > 20 && x < 30) list.add(5);
        if (x > 25) list.add(6);
        return list;
    }

    /** Sound index (0 = silent) candidates of ItemDosimeter.onUpdate for radBuf x > 1E-5. */
    public static List<Integer> dosimeterChoices(float x) {
        List<Integer> list = new ArrayList<>(4);
        if (x < 0.5) list.add(0);
        if (x < 1) list.add(1);
        if (x >= 0.5 && x < 2) list.add(2);
        if (x >= 1 && x >= 2) list.add(3);
        return list;
    }

    private static void playGeiger(Level level, LivingEntity entity, float x) {
        if (x > 1.0E-5) {
            List<Integer> choices = geigerChoices(x);
            int chosen = choices.get(level.random.nextInt(choices.size()));
            if (chosen > 0) {
                playSound(level, entity, chosen);
            }
        } else if (level.random.nextInt(50) == 0) {
            playSound(level, entity, 1);
        }
    }

    private static void playDosimeter(Level level, LivingEntity entity, float x) {
        if (x > 1.0E-5) {
            List<Integer> choices = dosimeterChoices(x);
            int chosen = choices.get(level.random.nextInt(choices.size()));
            if (chosen > 0) {
                playSound(level, entity, chosen);
            }
        } else if (level.random.nextInt(100) == 0) {
            playSound(level, entity, 1);
        }
    }

    private static void playSound(Level level, LivingEntity entity, int oneBasedIndex) {
        int index = Math.max(1, Math.min(oneBasedIndex, GEIGER_SOUNDS.size())) - 1;
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                GEIGER_SOUNDS.get(index).get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    /** [parity I3] ContaminationUtil.printGeigerData; also used by the placed Geiger counter block. */
    public static void printGeigerData(Level level, Player player) {
        ContaminationUtil.printGeigerData(player); // [parity J1]
    }

    private static ChatFormatting radColor(double rads) {
        if (rads == 0.0D) return ChatFormatting.GREEN;
        if (rads < 1.0D) return ChatFormatting.YELLOW;
        if (rads < 10.0D) return ChatFormatting.GOLD;
        if (rads < 100.0D) return ChatFormatting.RED;
        if (rads < 1000.0D) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    private static ChatFormatting bodyColor(double rads) {
        if (rads < 200.0D) return ChatFormatting.GREEN;
        if (rads < 400.0D) return ChatFormatting.YELLOW;
        if (rads < 600.0D) return ChatFormatting.GOLD;
        if (rads < 800.0D) return ChatFormatting.RED;
        if (rads < 1000.0D) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }
}

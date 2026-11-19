package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 ItemModNightVision for a powered, enabled full FSB suit. */
public final class LegacyNightVisionArmorModItem extends ArmorModItem {
    public LegacyNightVisionArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    private static final String NIGHT_VISION_ACTIVE_NBT_KEY = "ITEM_MOD_NV_ACTIVE";

    // [parity I2] ItemModNightVision#modUpdate: needs the HUD toggle; NBT flag; 1/200 armor wear; removal on HUD off.
    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.level().isClientSide || !(armor.getItem() instanceof PoweredArmorFSBItem) || !ArmorFSBItem.hasFSBArmor(player)) {
            return;
        }
        net.minecraft.nbt.CompoundTag tag = armor.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (LegacyArmorModParity.enableHUD(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0));
            if (!tag.contains(NIGHT_VISION_ACTIVE_NBT_KEY)) {
                tag.putBoolean(NIGHT_VISION_ACTIVE_NBT_KEY, true);
                armor.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            }
            if (player.getRandom().nextInt(200) == 0) {
                armor.hurtAndBreak(1, player, player.getEquipmentSlotForItem(armor));
            }
        } else if (tag.contains(NIGHT_VISION_ACTIVE_NBT_KEY)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            tag.remove(NIGHT_VISION_ACTIVE_NBT_KEY);
            armor.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
    }

    // [parity I2: tooltip] ItemModNightVision (item.night_vision.description.*)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.atomiclegacy.night_vision.description.item").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(Component.translatable("item.atomiclegacy.night_vision.description.in_armor", mod.getHoverName()).withStyle(ChatFormatting.YELLOW));
    }
}

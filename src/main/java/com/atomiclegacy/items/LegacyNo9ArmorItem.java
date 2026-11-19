package com.atomiclegacy.items;

import com.atomiclegacy.radiation.HbmLivingHazards;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Exact 1.7.10 black-lung suppression from ArmorNo9. */
public final class LegacyNo9ArmorItem extends ArmorItem {
    public LegacyNo9ArmorItem(Holder<ArmorMaterial> material) {
        super(material, Type.HELMET, new Properties().stacksTo(1)); // [parity I2] setMaxDamage(0): no durability
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original
            ) {
                original.setAllVisible(false);
                return original;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }

        // [parity I2] "isOn" mirrors HbmPlayerProps.enableHUD (lamp), with fire.ignite / random.fizz on change
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        boolean turnOn = LegacyArmorModParity.enableHUD(player);
        boolean wasOn = tag.getBoolean("isOn");
        if (turnOn && !wasOn) level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.5F);
        if (!turnOn && wasOn) level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 2F);
        if (turnOn != wasOn || !tag.contains("isOn")) {
            tag.putBoolean("isOn", turnOn);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }

        // [parity I2] two independent checks of the original (cap at 0.9, then -1 from 0.25 upward)
        HbmLivingHazards hazards = HbmLivingHazards.get(player);
        if (hazards.getBlackLung() > HbmLivingHazards.MAX_BLACK_LUNG * 0.9) {
            hazards.setBlackLung(player, (int) (HbmLivingHazards.MAX_BLACK_LUNG * 0.9));
        }
        if (hazards.getBlackLung() >= HbmLivingHazards.MAX_BLACK_LUNG * 0.25) {
            hazards.setBlackLung(player, hazards.getBlackLung() - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("+0.5 DT").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Lets you breathe coal, neat!").withStyle(ChatFormatting.YELLOW));
    }
}

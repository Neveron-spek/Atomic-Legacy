package com.atomiclegacy.items;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * [parity S4: gun kits] ItemRepairKit (gun_kit_1 = 10, gun_kit_2 = 100): right click removes 25 % of the maximum
 * durability from the wear of every config of every ItemGunBaseNT in the hotbar; one use per click that did something.
 * 1.7.10 setMaxDamage(dura - 1) breaks once the damage exceeds dura - 1, i.e. after dura uses; the 1.21 durability
 * component breaks when the damage reaches its maximum, hence max_damage = dura for the same number of uses.
 */
public final class LegacyRepairKitItem extends Item {
    private final boolean oil;

    public LegacyRepairKitItem(Properties properties, int dura, boolean oil) {
        super(properties.stacksTo(1).durability(dura));
        this.oil = oil;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);

        boolean didSomething = false;
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().items.get(i);
            if (!item.isEmpty() && item.getItem() instanceof ItemGunBaseNT gun) {
                int configs = gun.getConfigCount();
                for (int j = 0; j < configs; j++) {
                    GunConfig cfg = gun.getConfig(item, j);
                    float maxDura = cfg.getDurability(item);
                    float wear = Math.min(ItemGunBaseNT.getWear(item, j), maxDura);
                    if (wear > 0) {
                        ItemGunBaseNT.setWear(item, j, Math.max(0F, ItemGunBaseNT.getWear(item, j) - maxDura * 0.25F));
                        didSomething = true;
                    }
                }
            }
        }

        if (didSomething) {
            SoundEvent sound = oil ? HbmSoundEvents.ITEM_SPRAY.get() : HbmSoundEvents.ITEM_REPAIR.get();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.consume(stack);
    }
}

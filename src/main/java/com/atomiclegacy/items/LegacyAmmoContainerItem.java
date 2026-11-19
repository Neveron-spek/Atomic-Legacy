package com.atomiclegacy.items;

import com.atomiclegacy.inventory.gui.LegacyInfoText;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [parity S4: ammo_container] ItemAmmoContainer. Damage value 1 (makeshift, icon ammo_container_alt) is carried as
 * custom_model_data 1, which also selects the alternative model.
 */
public final class LegacyAmmoContainerItem extends Item {
    public LegacyAmmoContainerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isMakeshift(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data != null && data.value() == 1;
    }

    public static ItemStack makeshift(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return stack;
    }

    /** getSubItems: damage 0 and 1. */
    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(new ItemStack(this));
        output.accept(makeshift(this));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        boolean makeshift = isMakeshift(stack);

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack inv : player.getInventory().items) {
            if (!inv.isEmpty() && inv.getItem() instanceof ItemGunBaseNT gun) {
                if (!gun.getDefaultAmmo().isEmpty() && !(makeshift && gun.isDefaultExpensive)) stacks.add(inv);
            }
        }

        if (stacks.isEmpty()) return InteractionResultHolder.consume(stack);

        Collections.shuffle(stacks);
        int maxGunCount = 3;
        for (int i = 0; i < maxGunCount && i < stacks.size(); i++) {
            ItemGunBaseNT gun = (ItemGunBaseNT) stacks.get(i).getItem();
            ItemStack ammo = gun.getDefaultAmmo().copy();
            if (makeshift) ammo.setCount((int) Math.ceil(ammo.getCount() / 2D));
            ItemStack remainder = tryAddItemToInventory(player.getInventory().items, ammo);
            if (!remainder.isEmpty()) player.drop(remainder, false);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.ITEM_UNPACK.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.inventoryMenu.broadcastChanges();
        stack.shrink(1); // 1.7.10 decrements in creative mode as well
        return InteractionResultHolder.consume(stack);
    }

    /** InventoryUtil.tryAddItemToInventory(ItemStack[], ItemStack): fill matching stacks first, then the first empty slot. */
    private static ItemStack tryAddItemToInventory(List<ItemStack> inv, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack slot : inv) {
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int transfer = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (transfer > 0) {
                    slot.grow(transfer);
                    stack.shrink(transfer);
                    if (stack.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).isEmpty()) {
                inv.set(i, stack);
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.atomiclegacy.ammo_container" + (isMakeshift(stack) ? ".1" : "") + ".desc";
        for (Component line : LegacyInfoText.lines(key)) tooltip.add(line.copy().withStyle(ChatFormatting.YELLOW));
    }
}

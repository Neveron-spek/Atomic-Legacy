package com.atomiclegacy.items;

import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Exact 1.7.10 jetpack_tank behavior: consume one tank to insert up to 1,000 mB of kerosene. */
public final class LegacyJetpackTankItem extends Item {
    private static final int FILL_AMOUNT = 1_000;

    public LegacyJetpackTankItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tank = player.getItemInHand(hand);
        // [parity I2: jetpack] ItemSyringe jetpack_tank: the worn jetpack itself or the chestplate's plate_only mod;
        // tryFill(KEROSENE, 1000) < 1000 consumes one tank (also in creative), sound hbm:item.jetpackTank.
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!level.isClientSide && !chestplate.isEmpty()) {
            ItemStack jetpack = chestplate;
            if (chestplate.getItem() instanceof net.minecraft.world.item.ArmorItem) {
                jetpack = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
            }
            if (jetpack.getItem() instanceof LegacyJetpackItem fillable && fillable.acceptsFuel("kerosene")) {
                if (fillable.fill(jetpack, "kerosene", FILL_AMOUNT) > 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), com.atomiclegacy.registry.HbmSoundEvents.ITEM_JETPACK_TANK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    tank.shrink(1);
                }
                if (jetpack.getItem() != chestplate.getItem()) {
                    ArmorModHandler.applyMod(chestplate, jetpack, player.registryAccess());
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(tank, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity H1] ItemSyringe.addInformation literal
        LegacyConsumableSupport.literal(tooltip, "Fills worn jetpack with up to 1000mB of kerosene");
    }
}

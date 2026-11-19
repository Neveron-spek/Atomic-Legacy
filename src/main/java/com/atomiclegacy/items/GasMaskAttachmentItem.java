package com.atomiclegacy.items;

import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/** Filter-bearing 1.7.10 gas-mask attachments installed in a helmet slot. */
public final class GasMaskAttachmentItem extends ArmorModItem implements FilterableGasMask {
    private final boolean mono;

    public GasMaskAttachmentItem(Item.Properties properties, boolean mono) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
        this.mono = mono;
    }

    @Override
    public Set<HbmArmorProtection.HazardClass> blacklist() {
        return this.mono
                ? Set.of(HbmArmorProtection.HazardClass.GAS_LUNG, HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.NERVE_AGENT, HbmArmorProtection.HazardClass.BACTERIA)
                : Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.NERVE_AGENT);
    }

    @Override
    public boolean isFilterApplicable(ItemStack filter) {
        return filter.getItem() instanceof GasMaskFilterItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        ItemStack filter = GasMaskItem.getInstalledFilter(stack, player.registryAccess());
        if (filter.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        GasMaskItem.removeFilter(stack);
        if (!level.isClientSide) { // [parity I2] no sound when unscrewing
            if (!player.getInventory().add(filter)) {
                player.drop(filter, true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // [parity I2: tooltip] ItemModGasmask#addInformation / #addDesc
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(ChatFormatting.GREEN, "Gas protection"));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
        GasMaskItem.addGasMaskTooltip(stack, tooltip);
        GasMaskItem.addBlacklistTooltip(this.mono, Component.literal("Will never protect against:"), tooltip);
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.GREEN, mod, " (gas protection)"));
        GasMaskItem.addGasMaskTooltip(mod, tooltip);
    }
}

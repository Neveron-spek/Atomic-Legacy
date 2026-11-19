package com.atomiclegacy.items;

import com.atomiclegacy.integration.curios.CuriosIntegration;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.util.SavedItemStackPreview;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

public class GasMaskItem extends ArmorItem implements FilterableGasMask {
    public static final String FILTER_KEY = "hfrFilter";

    private final Kind kind;

    public GasMaskItem(Holder<ArmorMaterial> material, Kind kind, Properties properties) {
        super(material, Type.HELMET, properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    public Set<HbmArmorProtection.HazardClass> blacklist() {
        return this.kind.blacklist();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack filter = getInstalledFilter(stack, player.registryAccess());
        if (filter.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        removeFilter(stack);
        if (!level.isClientSide) { // [parity I2] no sound when unscrewing (ArmorUtil.removeFilter)
            if (!player.getInventory().add(filter)) {
                player.drop(filter, true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // [parity I2: tooltip] ArmorGasMask#addInformation = ArmorUtil.addGasMaskTooltip + "hazard.neverProtects" list
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addGasMaskTooltip(stack, tooltip);
        addBlacklistTooltip(this.kind == Kind.MONO, Component.translatable("hazard.atomiclegacy.never_protects"), tooltip);
    }

    /** ArmorUtil.addGasMaskTooltip */
    public static void addGasMaskTooltip(ItemStack mask, List<Component> tooltip) {
        ItemStack filter = getInstalledFilterForTooltip(mask);
        if (filter.isEmpty()) {
            tooltip.add(Component.literal("No filter installed!").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.literal("Installed filter:").withStyle(ChatFormatting.GOLD));
        int meta = filter.getDamageValue();
        int max = filter.getMaxDamage();
        String append = max > 0 ? " (" + ((max - meta) * 100 / max) + "%)" : "";
        tooltip.add(Component.literal("  ").append(filter.getHoverName()).append(append));
    }

    /** "Will never protect against:" + " -hazard" lines; mono masks: GAS_LUNG, GAS_BLISTERING, BACTERIA, others GAS_BLISTERING. */
    public static void addBlacklistTooltip(boolean mono, Component header, List<Component> tooltip) {
        tooltip.add(header.copy().withStyle(ChatFormatting.RED));
        if (mono) {
            tooltip.add(Component.literal(" -").append(Component.translatable("hazard.atomiclegacy.gas_chlorine")).withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.literal(" -").append(Component.translatable("hazard.atomiclegacy.corrosive")).withStyle(ChatFormatting.DARK_RED));
        if (mono) {
            tooltip.add(Component.literal(" -").append(Component.translatable("hazard.atomiclegacy.bacteria")).withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static boolean installFilter(ItemStack mask, ItemStack filter, LivingEntity entity) {
        if (!(mask.getItem() instanceof FilterableGasMask gasMask) || !(filter.getItem() instanceof GasMaskFilterItem)) {
            return false;
        }
        if (!gasMask.isFilterApplicable(filter)) {
            return false;
        }

        CompoundTag root = mask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(FILTER_KEY, filter.save(entity.registryAccess()));
        mask.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }

    public static ItemStack getInstalledFilter(ItemStack mask, net.minecraft.core.HolderLookup.Provider registries) {
        if (registries == null) {
            return getInstalledFilterForTooltip(mask);
        }
        CustomData data = mask.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(FILTER_KEY)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, root.getCompound(FILTER_KEY));
    }

    /**
     * Tooltip-only path: do not parse a full saved ItemStack because tooltip
     * rendering can run in client/JEI/reload contexts where registry-backed
     * parsing is unsafe. The tooltip only needs the saved filter id and damage.
     */
    public static ItemStack getInstalledFilterForTooltip(ItemStack mask) {
        return SavedItemStackPreview.fromCustomData(mask, FILTER_KEY);
    }

    public static void removeFilter(ItemStack mask) {
        CompoundTag root = mask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(FILTER_KEY);
        if (root.isEmpty()) {
            mask.remove(DataComponents.CUSTOM_DATA);
        } else {
            mask.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    public static void damageInstalledFilter(ItemStack mask, LivingEntity entity, int damage) {
        ItemStack filter = getInstalledFilter(mask, entity.registryAccess());
        if (filter.isEmpty() || filter.getMaxDamage() <= 0) {
            return;
        }
        filter.setDamageValue(filter.getDamageValue() + damage);
        if (filter.getDamageValue() > filter.getMaxDamage()) {
            removeFilter(mask);
        } else {
            installFilter(mask, filter, entity);
        }
    }

    public boolean isFilterApplicable(ItemStack filter) {
        return filter.getItem() instanceof GasMaskFilterItem;
    }

    public static boolean isWornBy(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem
                || !CuriosIntegration.findFirstEquipped(entity, stack -> stack.getItem() instanceof GasMaskItem).isEmpty();
    }

    public enum Kind {
        STANDARD(Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.NERVE_AGENT)),
        M65(Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.NERVE_AGENT)),
        OLDE(Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.NERVE_AGENT)),
        MONO(Set.of(
                HbmArmorProtection.HazardClass.GAS_LUNG,
                HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.NERVE_AGENT,
                HbmArmorProtection.HazardClass.BACTERIA
        ));

        private final Set<HbmArmorProtection.HazardClass> blacklist;

        Kind(Set<HbmArmorProtection.HazardClass> blacklist) {
            this.blacklist = Set.copyOf(blacklist);
        }

        public Set<HbmArmorProtection.HazardClass> blacklist() {
            return this.blacklist;
        }
    }
}

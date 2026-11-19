package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Direct 1.7.10 ItemTooling / ItemToolingWeapon equivalents. */
public final class LegacyToolingItem extends Item {
    private LegacyToolingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // [parity I3: ItemTooling extends ItemCraftingDegradation - container item with +1 damage, no anvil repair]
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return LegacyCraftingDegradation.remainder(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        // [parity Q1: IToolable#onScrew of FoundryOutlet for ToolType.HAND_DRILL]
        if (ScrewdriverItem.isHandDrill(context.getItemInHand())) {
            com.atomiclegacy.blocks.FoundryOutletBlock.onScrew(context.getLevel(), context.getClickedPos(), true);
        }
        return super.useOn(context);
    }

    public static LegacyToolingItem handDrill(int durability) {
        Properties properties = new Properties();
        if (durability > 0) {
            properties.durability(durability);
        }
        return new LegacyToolingItem(properties);
    }

    public static LegacyToolingItem archineerWrench() {
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(AtomicLegacy.id("archineer_wrench_damage"), 12.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
        return new LegacyToolingItem(new Properties().durability(1_000).component(DataComponents.ATTRIBUTE_MODIFIERS, attributes));
    }
}

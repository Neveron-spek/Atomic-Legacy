package com.atomiclegacy.items;

import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.foundry.FoundryShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ScrapsItem extends Item {
    private static final String AMOUNT = "amount";
    private static final String LIQUID = "liquid";
    /**
     * The 1.7.10 autogen renderer fell back to the shared scraps sprite for
     * these materials. All other obtainable scrap materials have a baked,
     * material-specific solid sprite in the modern resource pack.
     */
    private static final Set<String> BASE_SOLID_TEXTURE_MATERIALS = Set.of(
            "actinium",
            "am_mix",
            "am241",
            "am242",
            "au198",
            "co60",
            "ghiorsium",
            "meteoric_iron",
            "neptunium",
            "obsidian",
            "pb209",
            "pig_iron",
            "pu_mix",
            "pu238",
            "pu239",
            "pu240",
            "pu241",
            "schraranium",
            "solinium",
            "u233",
            "u235",
            "wrought_iron"
    );

    private final FoundryMaterial material;

    public ScrapsItem(Properties properties, FoundryMaterial material) {
        super(properties);
        this.material = Objects.requireNonNull(material, "material");
    }

    @Override
    public Component getName(ItemStack stack) {
        FoundryMaterialStack contents = contents(stack);
        if (contents != null && isLiquid(stack)) {
            return Component.translatable(
                    "item.atomiclegacy.scraps.liquid",
                    Component.translatable(contents.material().translationKey())
            );
        }
        if (contents != null) {
            return Component.translatable(
                    "item.atomiclegacy.scraps.solid",
                    Component.translatable(contents.material().translationKey())
            );
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FoundryMaterialStack contents = contents(stack);
        if (contents == null) {
            return;
        }
        // ItemScraps.addInformation: Mats.formatAmount in the default colour; the additive warning only on molten scraps
        if (com.atomiclegacy.client.ParityQ4ClientParticles.shiftDown()) {
            // Mats.formatAmount(amount, showInMb = LSHIFT)
            LegacyTooltips.line(tooltip, (contents.amount() * 2) + "mB");
        } else {
            tooltip.add(formatAmountComponent(contents.amount()).withStyle(ChatFormatting.GRAY));
        }
        if (isLiquid(stack) && contents.material().behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE) {
            LegacyTooltips.line(tooltip, ChatFormatting.DARK_RED, "Additive, not castable!");
        }
    }

    public static ItemStack create(FoundryMaterialStack contents, boolean liquid) {
        ItemStack stack = new ItemStack(com.atomiclegacy.registry.HbmItems.scrapsItem(contents.material()).get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(AMOUNT, contents.amount());
        tag.putBoolean(LIQUID, liquid);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        int modelData = liquid
                ? (contents.material().behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE ? 2 : 1)
                : 0;
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        return stack;
    }

    public static FoundryMaterialStack contents(ItemStack stack) {
        if (!(stack.getItem() instanceof ScrapsItem scraps)) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int amount = tag.contains(AMOUNT) ? tag.getInt(AMOUNT) : FoundryShape.INGOT.q(1);
        return new FoundryMaterialStack(scraps.material, amount);
    }

    public static boolean isLiquid(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(LIQUID);
    }

    public FoundryMaterial material() {
        return this.material;
    }

    public boolean usesDedicatedSolidTexture() {
        return !BASE_SOLID_TEXTURE_MATERIALS.contains(this.material.name());
    }

    public static boolean usesBaseSolidTexture(FoundryMaterial material) {
        return BASE_SOLID_TEXTURE_MATERIALS.contains(material.name());
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (!(stack.getItem() instanceof ScrapsItem scraps)) {
            return 0xFFFFFFFF;
        }
        if (!isLiquid(stack) && scraps.usesDedicatedSolidTexture()) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | scraps.material.moltenColor();
    }

    public static String formatAmount(int amount) {
        int blocks = amount / FoundryShape.BLOCK.q(1);
        amount -= FoundryShape.BLOCK.q(blocks);
        int ingots = amount / FoundryShape.INGOT.q(1);
        amount -= FoundryShape.INGOT.q(ingots);
        int nuggets = amount / FoundryShape.NUGGET.q(1);
        amount -= FoundryShape.NUGGET.q(nuggets);

        StringBuilder builder = new StringBuilder();
        if (blocks > 0) {
            builder.append(blocks).append(blocks == 1 ? " block " : " blocks ");
        }
        if (ingots > 0) {
            builder.append(ingots).append(ingots == 1 ? " ingot " : " ingots ");
        }
        if (nuggets > 0) {
            builder.append(nuggets).append(nuggets == 1 ? " nugget " : " nuggets ");
        }
        if (amount > 0) {
            builder.append(amount).append(amount == 1 ? " quantum " : " quanta ");
        }
        return builder.isEmpty() ? "0 quanta" : builder.toString().trim();
    }

    public static MutableComponent formatAmountComponent(int amount) {
        int blocks = amount / FoundryShape.BLOCK.q(1);
        amount -= FoundryShape.BLOCK.q(blocks);
        int ingots = amount / FoundryShape.INGOT.q(1);
        amount -= FoundryShape.INGOT.q(ingots);
        int nuggets = amount / FoundryShape.NUGGET.q(1);
        amount -= FoundryShape.NUGGET.q(nuggets);

        MutableComponent result = Component.empty();
        boolean hasPart = false;
        if (blocks > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(blocks == 1 ? "matshape.atomiclegacy.block" : "matshape.atomiclegacy.blocks", blocks));
        }
        if (ingots > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(ingots == 1 ? "matshape.atomiclegacy.ingot" : "matshape.atomiclegacy.ingots", ingots));
        }
        if (nuggets > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(nuggets == 1 ? "matshape.atomiclegacy.nugget" : "matshape.atomiclegacy.nuggets", nuggets));
        }
        if (amount > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(amount == 1 ? "matshape.atomiclegacy.quantum" : "matshape.atomiclegacy.quanta", amount));
        }
        if (!hasPart) {
            result.append(Component.translatable("matshape.atomiclegacy.quanta", 0));
        }
        return result;
    }

    private static boolean appendPart(MutableComponent result, boolean hasPart, Component part) {
        if (hasPart) {
            result.append(Component.literal(" "));
        }
        result.append(part);
        return true;
    }
}

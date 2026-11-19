package com.atomiclegacy.items;

import com.atomiclegacy.blocks.FluidBarrelBlock;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import com.atomiclegacy.render.FluidBarrelItemRenderer;
import com.atomiclegacy.fluid.HbmFluidTank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class FluidBarrelBlockItem extends BlockItem {
    private final FluidBarrelBlock.Kind kind;

    public FluidBarrelBlockItem(FluidBarrelBlock block, Properties properties, FluidBarrelBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        if (this.kind == FluidBarrelBlock.Kind.PLASTIC
                || this.kind == FluidBarrelBlock.Kind.STEEL
                || this.kind == FluidBarrelBlock.Kind.TCALLOY
                || this.kind == FluidBarrelBlock.Kind.ANTIMATTER
                || this.kind == FluidBarrelBlock.Kind.CORRODED) { // [parity m1] barrel_corroded is BlockFluidBarrel / RenderBarrel too
            consumer.accept(new IClientItemExtensions() {
                private final BlockEntityWithoutLevelRenderer renderer = new FluidBarrelItemRenderer();

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return this.renderer;
                }
            });
            return;
        }

        // These two legacy variants were not part of RenderBarrel's three
        // craftable barrel item set, so retain their explicitly registered OBJ
        // item renderer.
        ObjMachineBlockItem.installRenderer(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.atomiclegacy.barrel.capacity",
                String.format(Locale.ROOT, "%,d", this.kind.capacity())
        ).withStyle(ChatFormatting.AQUA));

        switch (this.kind) {
            case PLASTIC -> {
                add(tooltip, "no_hot", ChatFormatting.YELLOW);
                add(tooltip, "no_corrosive", ChatFormatting.YELLOW);
                add(tooltip, "no_antimatter", ChatFormatting.YELLOW);
            }
            case CORRODED -> add(tooltip, "leaky", ChatFormatting.RED); // [parity L2: barrels] BlockFluidBarrel.addInformation
            case STEEL -> {
                add(tooltip, "yes_hot", ChatFormatting.GREEN);
                add(tooltip, "yes_corrosive", ChatFormatting.GREEN);
                add(tooltip, "no_highly_corrosive", ChatFormatting.YELLOW);
                add(tooltip, "no_antimatter", ChatFormatting.YELLOW);
            }
            case TCALLOY -> {
                add(tooltip, "yes_hot", ChatFormatting.GREEN);
                add(tooltip, "yes_highly_corrosive", ChatFormatting.GREEN);
                add(tooltip, "no_antimatter", ChatFormatting.YELLOW);
            }
            case ANTIMATTER -> {
                add(tooltip, "yes_hot", ChatFormatting.GREEN);
                add(tooltip, "yes_highly_corrosive", ChatFormatting.GREEN);
                add(tooltip, "yes_antimatter", ChatFormatting.GREEN);
            }
        }

        storedFluid(stack, this.kind.capacity()).ifPresent(tank -> tooltip.add(Component.translatable(
                "tooltip.atomiclegacy.barrel.contents",
                Integer.toString(tank.amount()), // [parity L2: barrels] "" + fill + "/" + max + "mB "
                Integer.toString(tank.capacity()),
                Component.translatable(tank.type().translationKey())
        ).withStyle(ChatFormatting.YELLOW)));
    }

    private static void add(List<Component> tooltip, String key, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.atomiclegacy.barrel." + key).withStyle(color));
    }

    private static java.util.Optional<HbmFluidTank> storedFluid(ItemStack stack, int capacity) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(FluidTankBlockEntity.ITEM_DATA_KEY)) {
            return java.util.Optional.empty();
        }
        CompoundTag data = root.getCompound(FluidTankBlockEntity.ITEM_DATA_KEY);
        if (!data.contains("Tank")) {
            return java.util.Optional.empty();
        }
        HbmFluidTank tank = new HbmFluidTank(capacity);
        tank.load(data.getCompound("Tank"));
        return tank.amount() > 0 && !tank.type().isNone() ? java.util.Optional.of(tank) : java.util.Optional.empty();
    }
}

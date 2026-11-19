package com.atomiclegacy.items;

import com.atomiclegacy.render.MiningLaserItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class MiningLaserBlockItem extends BlockItem {
    public MiningLaserBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** [parity Q4: MachineMiningLaser.addInformation, literal English lines in the original] */
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.literal("3x3x3 Multiblock"));
        tooltip.add(net.minecraft.network.chat.Component.literal("Only placeable on a ceiling."));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final MiningLaserItemRenderer renderer = new MiningLaserItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}

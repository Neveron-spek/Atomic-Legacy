package com.atomiclegacy.items;

import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.render.RbmkComponentItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class RbmkComponentBlockItem extends BlockItem {
    private final RbmkComponentBlock.Kind kind;

    public RbmkComponentBlockItem(Block block, Item.Properties properties, RbmkComponentBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    public RbmkComponentBlock.Kind kind() {
        return this.kind;
    }

    /** [parity T2: RBMKMiniPanelBase.addInformation -> ITooltipProvider.addStandardInfo("tile.<name>.desc")] */
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (!this.kind.isMiniPanel()) {
            return;
        }
        String key = net.minecraft.Util.makeDescriptionId("block", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this)) + ".desc";
        if (com.atomiclegacy.client.ParityQ4ClientParticles.shiftDown()) {
            for (String line : net.minecraft.network.chat.Component.translatable(key).getString().split(java.util.regex.Pattern.quote("$"))) {
                tooltip.add(net.minecraft.network.chat.Component.literal(line).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("Hold <").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC)
                    .append(net.minecraft.network.chat.Component.literal("LSHIFT").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.ITALIC))
                    .append(net.minecraft.network.chat.Component.literal("> to display more info").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC)));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final RbmkComponentItemRenderer renderer = new RbmkComponentItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}

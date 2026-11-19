package com.atomiclegacy.items;

import com.atomiclegacy.render.FunnelBlockEntityRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Uses the three-part MachineFunnel OBJ inventory renderer from 1.7.10. */
public final class FunnelBlockItem extends BlockItem {
    public FunnelBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** [parity Q4: MachineFunnel.addInformation -> ITooltipProvider.addStandardInfo("tile.machine_funnel.desc")] */
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (com.atomiclegacy.client.ParityQ4ClientParticles.shiftDown()) {
            for (String line : net.minecraft.network.chat.Component.translatable("block.atomiclegacy.machine_funnel.desc").getString().split(java.util.regex.Pattern.quote("$"))) {
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
            private final BlockEntityWithoutLevelRenderer renderer = new FunnelItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    private static final class FunnelItemRenderer extends BlockEntityWithoutLevelRenderer {
        private FunnelItemRenderer() {
            super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    net.minecraft.client.Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(net.minecraft.world.item.ItemStack stack,
                                 net.minecraft.world.item.ItemDisplayContext context,
                                 com.mojang.blaze3d.vertex.PoseStack poseStack,
                                 net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {
            FunnelBlockEntityRenderer.renderItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}

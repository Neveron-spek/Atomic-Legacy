package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * [parity d3] 1.7.10 RenderItem#doRender(EntityItem) with renderInFrame = true, hoverStart = 0 and
 * RenderDecoItem (no bobbing), expressed through the 1.21 FIXED item context.
 */
final class LegacyFramedItemRender {
    private LegacyFramedItemRender() {
    }

    static void render(ItemStack stack, Level level, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, level, null, 0);
        poseStack.pushPose();
        if (stack.getItem() instanceof BlockItem && model.isGui3d()) {
            // block branch: glScalef(1.25) glTranslatef(0, 0.05, 0) glRotatef(-90, Y), then 0.25 cube
            // (renderBlockAsItem turns the cube back by 90); FIXED block display is a 0.5 cube
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.translate(0.0F, 0.05F, 0.0F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        } else {
            // item branch: glScalef(0.5128205) glTranslatef(0, -0.05, 0) glRotatef(180, Y)
            // glTranslatef(-0.5, -0.25, -0.0421875 + 0.084375), renderItemIn2D quad 0..1, depth -0.0625..0
            poseStack.scale(0.5128205F, 0.5128205F, 0.5128205F);
            poseStack.translate(0.0F, -0.05F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, 0.25F, 0.0109375F);
        }
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, poseStack, buffers,
                packedLight, OverlayTexture.NO_OVERLAY, model);
        poseStack.popPose();
    }
}

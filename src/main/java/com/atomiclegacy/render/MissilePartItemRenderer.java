package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.items.MissilePartItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Port of 1.7.10 ItemRenderMissilePart: every custom missile part (MissilePart.registerAllParts) is drawn as its own
 * OBJ model, spinning in the inventory, instead of a flat icon.
 *
 * <p>INVENTORY works in the 16 px slot frame (x right, y down); the first two operations convert the modern item
 * frame (unit cube, y up) into it. The mirror of that conversion is cancelled by the original glScaled(-s, -s, -s),
 * so face culling stays correct.</p>
 */
public final class MissilePartItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MissilePartItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!MissileMultipartRenderer.partIds().contains(id) || !(stack.getItem() instanceof MissilePartItem part)) {
            return;
        }
        pose.pushPose();
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0.0F, 1.0F, 0.5F);
            pose.scale(1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);

            float height = MissileMultipartRenderer.guiHeight(id);
            if (height == 0.0F) {
                height = 4.0F;
            }
            float scale = 10.0F / height;
            pose.translate(height / 2.0F * scale, 0.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(135.0F));
            pose.mulPose(Axis.XP.rotationDegrees(145.0F));
            MissilePartItem.Type type = part.definition().type();
            if (type == MissilePartItem.Type.WARHEAD) {
                pose.translate(0.0F, height / 8.0F * scale, 0.0F);
            }
            if (type == MissilePartItem.Type.FUSELAGE) {
                pose.translate(0.0F, height / 4.0F * scale, 0.0F);
            }
            pose.translate(3.5F, 14.0F, 0.0F);
            pose.scale(-scale, -scale, -scale);
            pose.mulPose(Axis.YN.rotationDegrees((float) (System.currentTimeMillis() / 25L % 360L)));
        } else {
            // EQUIPPED / EQUIPPED_FIRST_PERSON: glTranslated(0.5, 0, 0), then like ENTITY: glScaled(0.4, 0.4, 0.4)
            boolean hand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            if (hand) {
                pose.translate(0.5F, 0.0F, 0.0F);
            } else {
                pose.translate(0.5F, 0.25F, 0.5F); // modern item frame: centre of the unit cube instead of its corner
            }
            pose.scale(0.4F, 0.4F, 0.4F);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MissileMultipartRenderer.model(id)), pose, buffers,
                Blocks.AIR.defaultBlockState(), packedLight, packedOverlay);
        pose.popPose();
    }
}

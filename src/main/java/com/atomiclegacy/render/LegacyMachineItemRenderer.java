package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.blocks.SawmillBlock;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the complete legacy OBJ assembly for machine items. */
public final class LegacyMachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    public LegacyMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.is(HbmBlocks.MACHINE_ANNIHILATOR.get().asItem())) {
            renderAnnihilatorItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_FORCEFIELD.get().asItem())) {
            renderForcefieldItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_MISSILE_ASSEMBLY.get().asItem())) {
            renderMissileAssemblyItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_ORBUS.get().asItem())) {
            renderOrbusItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_PRECASS.get().asItem())) {
            renderPrecisionAssemblerItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_RADIOLYSIS.get().asItem())) {
            renderRadiolysisItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_REACTOR.get().asItem())) {
            renderBreedingReactorItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_TURBOFAN.get().asItem())) {
            renderTurbofanItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_THRESHER.get().asItem())) {
            renderThresherItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_LPW2.get().asItem())) {
            renderLpw2Item(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_TURBINEGAS.get().asItem())) {
            renderGasTurbineItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_PYROOVEN.get().asItem())) {
            renderPyroOvenItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_AUTOSAW.get().asItem())) {
            renderAutosawItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_SAWMILL.get().asItem())) {
            renderSawmillItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_RTG_GREY.get().asItem())) {
            renderGreyRtgItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static BlockState legacyItemState() {
        return HbmBlocks.MACHINE_ANNIHILATOR.get().defaultBlockState();
    }

    private static void renderAnnihilatorItem(ItemDisplayContext context, PoseStack poseStack,
                                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyAnnihilatorItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemAnnihilator(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyAnnihilatorItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.75F, 2.75F, 2.75F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }

    private static void renderForcefieldItem(ItemDisplayContext context, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyForcefieldItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemForcefield(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyForcefieldItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -4.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderMissileAssemblyItem(ItemDisplayContext context, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyMissileAssemblyItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemMissileAssembly(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyMissileAssemblyItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderOrbusItem(ItemDisplayContext context, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyOrbusItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemOrbus(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyOrbusItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.0F, 2.0F, 2.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderPrecisionAssemblerItem(ItemDisplayContext context, PoseStack poseStack,
                                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyPrecisionAssemblerItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemPrecisionAssembler(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyPrecisionAssemblerItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -2.75F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
    }

    // [parity m1] machine_radar, machine_radar_large, machine_radgen: M1LegacyItemRenderers (ItemRenderBase).

    private static void renderRadiolysisItem(ItemDisplayContext context, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRadiolysisItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemRadiolysis(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRadiolysisItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(3.0F, 3.0F, 3.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderBreedingReactorItem(ItemDisplayContext context, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyBreedingReactorItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemBreedingReactor(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyBreedingReactorItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -4.5F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderTurbofanItem(ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyTurbofanItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemTurbofan(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyTurbofanItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(2.25F, 2.25F, 2.25F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderThresherItem(ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyThresherItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemThresher(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyThresherItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, 4.0F, -8.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
    }

    private static void renderLpw2Item(ItemDisplayContext context, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyLpw2ItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemLpw2(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyLpw2ItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.scale(1.15F, 1.15F, 1.15F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(0.30F, 0.30F, 0.30F);
        }
    }

    private static void renderGasTurbineItem(ItemDisplayContext context, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyGasTurbineItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemGasTurbine(legacyItemState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyGasTurbineItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -1.0F, 1.5F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
    }

    private static void renderPyroOvenItem(ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyPyroOvenItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemPyro(HbmBlocks.MACHINE_PYROOVEN.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyPyroOvenItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
    }

    private static void renderAutosawItem(ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyAutosawItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemAutosaw(HbmBlocks.MACHINE_AUTOSAW.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyAutosawItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.5F, -3.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
    }

    private static void renderSawmillItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySawmillItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemSawmill(HbmBlocks.MACHINE_SAWMILL.get().defaultBlockState(),
                SawmillBlock.hasBlade(stack), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applySawmillItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -1.5F, 0.0F);
            poseStack.scale(3.25F, 3.25F, 3.25F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
    }

    private static void renderGreyRtgItem(ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyGreyRtgItemPose(context, poseStack);
        LegacyMachineBlockEntityRenderer.renderItemRtg(HbmBlocks.MACHINE_RTG_GREY.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyGreyRtgItemPose(ItemDisplayContext context, PoseStack poseStack) {
        // machine_rtg_grey's Gen group is exactly one block wide, one block
        // high and one block deep: X/Z [-0.5, 0.5], Y [0, 1].  Unlike the
        // ItemRenderBase machines above it was an ISBRH block item in 1.7.10,
        // so it needs the normal block-item camera scale, not the old shared
        // 1/16 machine-item scale that made the icon one tenth of full size.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.scale(0.625F, -0.625F, -0.625F);
                poseStack.mulPose(Axis.XP.rotationDegrees(210.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-225.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            case HEAD, NONE -> {
                // minecraft:block/block defines no additional transform here.
            }
        }
        // Exact center of the RTG Gen geometry.
        poseStack.translate(0.0F, -0.5F, 0.0F);
    }
}

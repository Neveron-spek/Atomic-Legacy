package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * [parity U4: item renderers] 1.7.10 IItemRenderer on top of a BEWLR. Every 1.21.1 display context first undoes the
 * vanilla placement and then rebuilds the GL matrix that 1.7.10 / Forge 10.13 had set up before
 * IItemRenderer#renderItem, so the original renderItem bodies can be transcribed one to one:
 * <ul>
 * <li>INVENTORY: ForgeHooksClient#renderInventoryItem (slot pixel space, INVENTORY_BLOCK helper);</li>
 * <li>ENTITY: RenderItem#doRender bob/spin plus ForgeHooksClient#renderEntityItem (ENTITY_ROTATION /
 * ENTITY_BOBBING helpers, scale 0.5);</li>
 * <li>EQUIPPED: RenderPlayer/RenderBiped#renderEquippedItems (block, full 3D and flat branches) and
 * ForgeHooksClient#renderEquippedItem;</li>
 * <li>EQUIPPED_FIRST_PERSON: ItemRenderer#renderItemInFirstPerson ({@link #applyFirstPersonHand}) and
 * ForgeHooksClient#renderEquippedItem.</li>
 * </ul>
 * Contexts the original renderer does not handle are left to the vanilla item model by {@link LegacyItemModelWrapper}.
 */
public abstract class LegacyIItemRenderer extends BlockEntityWithoutLevelRenderer {

    public enum Type { ENTITY, EQUIPPED, EQUIPPED_FIRST_PERSON, INVENTORY }

    public enum Helper { ENTITY_ROTATION, ENTITY_BOBBING, EQUIPPED_BLOCK, BLOCK_3D, INVENTORY_BLOCK }

    /** Per-call GL state stand-ins. */
    protected PoseStack pose;
    protected MultiBufferSource buffers;
    protected int light;
    protected int overlay;
    /** data[1] of the 1.7.10 call: the holding entity for EQUIPPED / EQUIPPED_FIRST_PERSON, otherwise null. */
    protected LivingEntity entity;

    protected LegacyIItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public abstract boolean handleRenderType(ItemStack stack, Type type);

    public abstract boolean shouldUseRenderHelper(Type type, ItemStack stack, Helper helper);

    /** IItemRenderer#renderItem; {@link #pose} etc. hold the current state. */
    protected abstract void renderItem(Type type, ItemStack stack);

    /** Item#isFull3D of the original item (RenderPlayer third person branch). */
    protected boolean isFull3D(ItemStack stack) { return false; }

    /** The renderer sets player.isSwingInProgress = false in EQUIPPED_FIRST_PERSON (no swing of the hand). */
    protected boolean cancelsFirstPersonSwing(ItemStack stack) { return false; }

    /** Whether some legacy context of this stack is drawn by the renderer (ItemRenderMissile depends on the stack). */
    public boolean handlesStack(ItemStack stack) { return true; }

    /** 1.21.1 context to the 1.7.10 render type; null where the original drew nothing of its own. */
    public static Type legacyType(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> Type.INVENTORY;
            case GROUND, FIXED, NONE -> Type.ENTITY;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> Type.EQUIPPED;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> Type.EQUIPPED_FIRST_PERSON;
        };
    }

    @Override
    public final void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        this.pose = poseStack;
        this.buffers = bufferSource;
        this.light = packedLight;
        this.overlay = packedOverlay;
        this.entity = null;
        poseStack.pushPose();
        try {
            switch (context) {
                case GUI -> renderGui(stack);
                case GROUND -> renderGround(stack);
                case FIXED, NONE -> {
                    // port convention (LegacyItemContexts): item frames reuse the ENTITY transform, without bob/spin
                    poseStack.translate(0.5F, 0.5F, 0.5F);
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                    renderItem(Type.ENTITY, stack);
                }
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> renderThirdPerson(stack, context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> renderFirstPerson(stack);
                case HEAD -> renderHead(stack);
            }
        } finally {
            poseStack.popPose();
            this.entity = null;
        }
    }

    /** ForgeHooksClient#renderInventoryItem. 1.21.1 GUI item space (origin bottom-left, y up, 1 = 16 px) to slot pixels. */
    private void renderGui(ItemStack stack) {
        pose.translate(0.0F, 1.0F, 0.0F);
        pose.scale(1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);
        if (shouldUseRenderHelper(Type.INVENTORY, stack, Helper.INVENTORY_BLOCK)) {
            pose.translate(-2.0F, 3.0F, -3.0F);
            pose.scale(10.0F, 10.0F, 10.0F);
            pose.translate(1.0F, 0.5F, 1.0F);
            pose.scale(1.0F, 1.0F, -1.0F);
            pose.mulPose(Axis.XP.rotationDegrees(210.0F));
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        renderItem(Type.INVENTORY, stack);
    }

    /** RenderItem#doRender and ForgeHooksClient#renderEntityItem. */
    private void renderGround(ItemStack stack) {
        // ItemEntityRenderer: translate(0, bob + 0.25 * groundScaleY, 0) (identity transforms), spin, -0.5 item offset
        pose.translate(0.5F, 0.25F, 0.5F);
        ItemEntity item = CaptureRenderer.current;
        if (item != null && item.getItem() == stack) {
            float age = item.getAge() + CaptureRenderer.partialTick;
            float spin = item.getSpin(CaptureRenderer.partialTick);
            float bob = Mth.sin(age / 10.0F + item.bobOffs) * 0.1F + 0.1F;
            // 1.7.10 f3 = ((age + pt) / 20 + hoverStart) * 180 / PI degrees: the same angle 1.21.1 applies in radians
            if (!shouldUseRenderHelper(Type.ENTITY, stack, Helper.ENTITY_ROTATION)) pose.mulPose(Axis.YP.rotation(-spin));
            if (!shouldUseRenderHelper(Type.ENTITY, stack, Helper.ENTITY_BOBBING)) pose.translate(0.0F, -bob, 0.0F);
        }
        // 3D branch: block == null -> renderType 1 -> scale 0.5 (RenderItem.renderInFrame is false); otherwise scale 0.5
        pose.scale(0.5F, 0.5F, 0.5F);
        renderItem(Type.ENTITY, stack);
    }

    /** RenderPlayer / RenderBiped#renderEquippedItems after bipedRightArm.postRender, then ForgeHooksClient#renderEquippedItem. */
    private void renderThirdPerson(ItemStack stack, boolean left) {
        this.entity = ItemRenderWeaponBase.renderingEntity;
        undoVanillaHandPlacement(pose, left);
        pose.translate(-0.0625F, 0.4375F, 0.0625F);
        boolean is3D = shouldUseRenderHelper(Type.EQUIPPED, stack, Helper.BLOCK_3D);
        if (is3D) {
            float f2 = 0.5F;
            pose.translate(0.0F, 0.1875F, -0.3125F);
            f2 *= 0.75F;
            pose.mulPose(Axis.XP.rotationDegrees(20.0F));
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            pose.scale(-f2, -f2, f2);
        } else if (isFull3D(stack)) {
            float f2 = 0.625F;
            pose.translate(0.0F, 0.1875F, 0.0F);
            pose.scale(f2, -f2, f2);
            pose.mulPose(Axis.XP.rotationDegrees(-100.0F));
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else {
            float f2 = 0.375F;
            pose.translate(0.25F, 0.1875F, -0.1875F);
            pose.scale(f2, f2, f2);
            pose.mulPose(Axis.ZP.rotationDegrees(60.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(20.0F));
        }
        equippedChain(Type.EQUIPPED, stack);
        renderItem(Type.EQUIPPED, stack);
    }

    /** {@link #applyFirstPersonHand} already placed the hand; undo ItemRenderer's -0.5 offset. */
    private void renderFirstPerson(ItemStack stack) {
        this.entity = Minecraft.getInstance().player;
        pose.translate(0.5F, 0.5F, 0.5F);
        equippedChain(Type.EQUIPPED_FIRST_PERSON, stack);
        renderItem(Type.EQUIPPED_FIRST_PERSON, stack);
    }

    /**
     * RenderBiped / RenderPlayer helmet slot: only ItemBlocks (and skulls) were drawn on the head, with the 0.625 block
     * transform when the block renders in 3D; non-block items were not drawn at all.
     */
    private void renderHead(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) return;
        this.entity = ItemRenderWeaponBase.renderingEntity;
        // CustomHeadLayer: translate(0, -0.25, 0), rotY(180), scale(0.625, -0.625, -0.625), then the -0.5 offset
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(1.0F / 0.625F, -1.0F / 0.625F, -1.0F / 0.625F);
        pose.mulPose(Axis.YP.rotationDegrees(-180.0F));
        if (shouldUseRenderHelper(Type.EQUIPPED, stack, Helper.BLOCK_3D)) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.scale(0.625F, -0.625F, -0.625F);
        } else {
            pose.translate(0.0F, 0.25F, 0.0F);
        }
        equippedChain(Type.EQUIPPED, stack);
        renderItem(Type.EQUIPPED, stack);
    }

    /** ForgeHooksClient#renderEquippedItem. */
    private void equippedChain(Type type, ItemStack stack) {
        if (shouldUseRenderHelper(type, stack, Helper.EQUIPPED_BLOCK)) {
            pose.translate(-0.5F, -0.5F, -0.5F);
        } else {
            pose.translate(0.0F, -0.3F, 0.0F);
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.mulPose(Axis.YP.rotationDegrees(50.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(335.0F));
            pose.translate(-0.9375F, -0.0625F, 0.0F);
        }
    }

    /** Undo ItemInHandLayer#renderArmWithItem after translateToHand and ItemRenderer's -0.5 offset (as ItemRenderWeaponBase). */
    static void undoVanillaHandPlacement(PoseStack poseStack, boolean left) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.translate(-(left ? -1.0F : 1.0F) / 16.0F, -0.125F, 0.625F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    /**
     * ItemRenderer#renderItemInFirstPerson (no item in use): swing translation, equip lowering, 45 degree turn, swing
     * rotations and the 0.4 scale. 1.21.1 equipProcess = 1 - f1 of 1.7.10. The left hand mirrors the x offsets and the
     * y/z turns (1.7.10 had no off hand).
     */
    public void applyFirstPersonHand(PoseStack pose, HumanoidArm arm, ItemStack stack, float equipProcess, float swingProcess) {
        float i = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float f5 = cancelsFirstPersonSwing(stack) ? 0.0F : swingProcess;
        float f13 = 0.8F;
        float f6 = Mth.sin(f5 * (float) Math.PI);
        float f7 = Mth.sin(Mth.sqrt(f5) * (float) Math.PI);
        pose.translate(i * -f7 * 0.4F, Mth.sin(Mth.sqrt(f5) * (float) Math.PI * 2.0F) * 0.2F, -f6 * 0.2F);
        pose.translate(i * 0.7F * f13, -0.65F * f13 - equipProcess * 0.6F, -0.9F * f13);
        pose.mulPose(Axis.YP.rotationDegrees(i * 45.0F));
        f6 = Mth.sin(f5 * f5 * (float) Math.PI);
        f7 = Mth.sin(Mth.sqrt(f5) * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(i * -f6 * 20.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(i * -f7 * 20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-f7 * 80.0F));
        float f8 = 0.4F;
        pose.scale(f8, f8, f8);
    }

    protected static void glRotate(PoseStack pose, double angle, double x, double y, double z) {
        ItemRenderWeaponBase.glRotate(pose, angle, x, y, z);
    }

    protected int overlayOrNone() {
        return this.overlay == 0 ? OverlayTexture.NO_OVERLAY : this.overlay;
    }

    /** ItemEntityRenderer for minecraft:item that exposes the entity being drawn (RenderItem#doRender had it). */
    public static final class CaptureRenderer extends ItemEntityRenderer {
        static ItemEntity current;
        static float partialTick;

        public CaptureRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(ItemEntity itemEntity, float entityYaw, float partialTicks, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight) {
            ItemEntity previous = current;
            float previousTick = partialTick;
            current = itemEntity;
            partialTick = partialTicks;
            try {
                super.render(itemEntity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            } finally {
                current = previous;
                partialTick = previousTick;
            }
        }
    }
}

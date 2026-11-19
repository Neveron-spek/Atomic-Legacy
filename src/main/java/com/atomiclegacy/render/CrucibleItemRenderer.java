package com.atomiclegacy.render;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.LegacyCrucibleItem;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.7.10 ItemRenderCrucible. Each display context first undoes the 1.21.1
 * placement and then applies the 1.7.10 vanilla/Forge chain that preceded
 * IItemRenderer#renderItem (sword = isFull3D third person branch, Forge
 * renderEquippedItem chain for EQUIPPED and EQUIPPED_FIRST_PERSON, GUI slot
 * space for INVENTORY, Forge renderEntityItem scale for ENTITY).
 */
public final class CrucibleItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** ResourceManager.crucible (geometry identical to models/weapons/crucible.obj of the original). */
    private static final LegacyEntityObjMesh crucible = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/weapons/crucible.obj"));
    private static final ResourceLocation crucible_hilt = AtomicLegacy.id("textures/models/weapons/crucible_hilt.png");
    private static final ResourceLocation crucible_guard = AtomicLegacy.id("textures/models/weapons/crucible_guard.png");
    private static final ResourceLocation crucible_blade = AtomicLegacy.id("textures/models/weapons/crucible_blade.png");

    /** GL_LIGHTING disabled, lightmap 240/240, GL_CULL_FACE disabled. */
    private static final Function<ResourceLocation, RenderType> UNLIT_NO_CULL = Util.memoize(texture -> RenderType.create(
            "atomiclegacy_crucible_unlit",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)));

    public CrucibleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** The standalone baked parts are no longer used for rendering; kept registered for resource closure. */
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MachineModelRenderer.standalone("weapons/crucible_hilt"));
        event.register(MachineModelRenderer.standalone("weapons/crucible_guard_left"));
        event.register(MachineModelRenderer.standalone("weapons/crucible_guard_right"));
        event.register(MachineModelRenderer.standalone("weapons/crucible_blade"));
    }

    /** ItemCrucible#playSwing: PositionedSoundRecord.func_147674_a (volume 0.25, non-positional). */
    public static void playSwing(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(HbmSoundEvents.CRUCIBLE_SWING.get(), pitch));
    }

    /**
     * ItemRenderer#renderItemInFirstPerson for the crucible. ItemRenderCrucible sets
     * isSwingInProgress = false and snaps the equip progress, so the swing terms are 0
     * and the item is fully raised.
     */
    public static void applyLegacyFirstPersonTransform(PoseStack poseStack) {
        float f13 = 0.8F;
        poseStack.translate(0.7F * f13, -0.65F * f13, -0.9F * f13);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        float f8 = 0.4F;
        poseStack.scale(f8, f8, f8);
    }

    /** ForgeHooksClient#renderEquippedItem for renderers without the BLOCK_3D helper. */
    private static void forgeEquippedChain(PoseStack poseStack) {
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        boolean isOn = LegacyCrucibleItem.isCharged(stack);

        switch (context) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                forgeEquippedChain(poseStack);
                renderFirstPerson(isOn, poseStack, bufferSource, packedLight, packedOverlay);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                ItemRenderWeaponBase.undoVanillaHandPlacement(poseStack, context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                // RenderPlayer#renderEquippedItems, isFull3D branch
                poseStack.translate(-0.0625F, 0.4375F, 0.0625F);
                float f2 = 0.625F;
                poseStack.translate(0.0F, 0.1875F, 0.0F);
                poseStack.scale(f2, -f2, f2);
                poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                forgeEquippedChain(poseStack);
                renderEquipped(isOn, poseStack, bufferSource, packedLight, packedOverlay);
            }
            case GROUND, FIXED, HEAD, NONE -> {
                // ItemEntityRenderer extra 0.25 lift and -0.5 offset undone; ForgeHooksClient#renderEntityItem scale 0.5
                poseStack.translate(0.5F, context == ItemDisplayContext.GROUND ? 0.25F : 0.5F, 0.5F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.translate(-0.75, 0.6, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
                renderEquipped(isOn, poseStack, bufferSource, packedLight, packedOverlay);
            }
            case GUI -> {
                poseStack.translate(0.0F, 1.0F, 0.0F);
                poseStack.scale(1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);
                renderInventory(isOn, poseStack, bufferSource);
            }
        }
        poseStack.popPose();
    }

    private static void renderFirstPerson(boolean isOn, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.translate(1.5, -0.3, 0);

        pose.scale(0.3F, 0.3F, 0.3F);

        pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.mulPose(Axis.YP.rotationDegrees(90));

        boolean isSwing = false;

        double[] sRot = HbmAnimations.getRelevantTransformation("SWING_ROT");
        double[] sTrans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
        pose.translate(sTrans[0], sTrans[1], sTrans[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) sRot[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) sRot[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) sRot[1]));

        if (sRot[0] != 0)
            isSwing = true;

        double[] rot = HbmAnimations.getRelevantTransformation("GUARD_ROT");
        part("Hilt", crucible_hilt, pose, buffers, light, overlay);

        double rotGuard = rot[0];

        if (!isSwing && !isOn)
            rotGuard = 90;

        guards(rotGuard, pose, buffers, light, overlay);

        if (rot[2] == 0 && (isSwing || isOn)) {
            pose.pushPose();
            pose.translate(0.005, 0, 0);
            crucible.renderGroup("Blade", pose, buffers.getBuffer(UNLIT_NO_CULL.apply(crucible_blade)), LightTexture.FULL_BRIGHT, overlay, 0xFFFFFFFF);
            pose.popPose();
        }
    }

    private static void renderEquipped(boolean isOn, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.translate(0.75, -0.4, 0);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(0.15F, 0.15F, 0.15F);

        part("Hilt", crucible_hilt, pose, buffers, light, overlay);
        guards(isOn ? 0 : 90, pose, buffers, light, overlay);

        if (isOn) {
            pose.pushPose();
            pose.translate(0.005, 0, 0);
            crucible.renderGroup("Blade", pose, buffers.getBuffer(UNLIT_NO_CULL.apply(crucible_blade)), LightTexture.FULL_BRIGHT, overlay, 0xFFFFFFFF);
            pose.popPose();
        }
    }

    /** INVENTORY: Forge disables GL lighting before the custom renderer and ItemRenderCrucible never re-enables it. */
    private static void renderInventory(boolean isOn, PoseStack pose, MultiBufferSource buffers) {
        pose.translate(2, 14, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-135));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        double scale = 1.5D;
        pose.scale((float) scale, (float) scale, (float) scale);

        unlit("Hilt", crucible_hilt, pose, buffers);
        pose.pushPose();
        pose.translate(0, 3, 0.5);
        pose.mulPose(Axis.XN.rotationDegrees(isOn ? 0 : 90));
        pose.translate(0, -3, -0.5);
        unlit("GuardLeft", crucible_guard, pose, buffers);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 3, -0.5);
        pose.mulPose(Axis.XP.rotationDegrees(isOn ? 0 : 90));
        pose.translate(0, -3, 0.5);
        unlit("GuardRight", crucible_guard, pose, buffers);
        pose.popPose();
        pose.translate(0.005, 0, 0);
        if (isOn) {
            unlit("Blade", crucible_blade, pose, buffers);
        }
    }

    private static void guards(double rotGuard, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0, 3, 0.5);
        pose.mulPose(Axis.XN.rotationDegrees((float) rotGuard));
        pose.translate(0, -3, -0.5);
        part("GuardLeft", crucible_guard, pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 3, -0.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) rotGuard));
        pose.translate(0, -3, 0.5);
        part("GuardRight", crucible_guard, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void part(String group, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        crucible.renderGroup(group, pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);
    }

    private static void unlit(String group, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers) {
        crucible.renderGroup(group, pose, buffers.getBuffer(UNLIT_NO_CULL.apply(texture)), LightTexture.FULL_BRIGHT, 0, 0xFFFFFFFF);
    }
}

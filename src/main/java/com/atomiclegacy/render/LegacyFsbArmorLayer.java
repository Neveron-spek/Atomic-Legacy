package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyArmorBodyModels;
import com.atomiclegacy.items.LegacyArmorBodyModels.Anchor;
import com.atomiclegacy.items.LegacyArmorBodyModels.Mode;
import com.atomiclegacy.items.LegacyArmorBodyModels.Part;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Map;

/**
 * Entity renderer for the 1.7.10 ModelArmorBase OBJ models (ArmorFSB suits, ArmorNo9, ArmorHat, ArmorAshGlasses).
 *
 * <p>[parity z1] Each group is drawn like ModelRendererObj#render: translate(rotationPoint), rotate Z/Y/X,
 * translate(-originPoint), scale(1/16). The rotation point and angles are those of the animated 1.21.1 humanoid
 * part (ModelArmorBase#copyPropertiesFromBiped); the origin is the fixed setRotationPoint of the constructor
 * (head/body 0, arms (+-5, 2, 0), legs/feet (+-1.9, 12, 0)). Per-model GL state of the original render():
 * GL_BLEND (envsuit / trenchmaster helmet, fau cassette and arms), fullbright GL_LIGHTING-off glows (ncrpa Eyes,
 * rpa Glow, trenchmaster Light), untextured (1, 1, 0.8) lamps (envsuit Lamps, no9 Flame only while "isOn"),
 * the rpa fan (-currentTimeMillis / 2 % 360 degrees) and the +-0.01 leg shift of taurun / trenchmaster.</p>
 */
public final class LegacyFsbArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>>
        extends RenderLayer<T, M> {
    private static final Map<String, LegacyEntityObjMesh> MESHES = Map.ofEntries(
            Map.entry("ajr", mesh("ajr")),
            Map.entry("bismuth", mesh("bismuth")),
            Map.entry("bj", mesh("bj")),
            Map.entry("bnuuy", mesh("bnuuy")),
            Map.entry("dnt", mesh("dnt")),
            Map.entry("envsuit", mesh("envsuit")),
            Map.entry("fau", mesh("fau")),
            Map.entry("goggles", mesh("goggles")),
            Map.entry("hat", mesh("hat")),
            Map.entry("hev", mesh("hev")),
            Map.entry("ncrpa", mesh("ncrpa")),
            Map.entry("no9", mesh("no9")),
            Map.entry("remnant", mesh("remnant")),
            Map.entry("steamsuit", mesh("steamsuit")),
            Map.entry("taurun", mesh("taurun")),
            Map.entry("trenchmaster", mesh("trenchmaster"))
    );

    /** glColor3f(1F, 1F, 0.8F). */
    private static final int LAMP_COLOR = 0xFFFFFFCC;

    private static final List<Part> PARTS = LegacyArmorBodyModels.PARTS;

    public LegacyFsbArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        M parentModel = this.getParentModel();
        for (Part part : PARTS) {
            ItemStack stack = livingEntity.getItemBySlot(part.slot());
            if (!part.matches(stack)) {
                continue;
            }
            if (part.mode() == Mode.LAMP_IF_ON && !isOn(livingEntity, stack)) {
                continue;
            }

            ModelPart anchor = resolve(part.anchor(), parentModel);
            poseStack.pushPose();
            // GL11.glTranslated(-0.01 / +0.01, 0, 0) before the leg / foot render (taurun, trenchmaster)
            poseStack.translate(part.shiftX(), 0.0F, 0.0F);
            anchor.translateAndRotate(poseStack);
            if (part.fan()) {
                // ModelArmorRPA: translate(0, 4.875px, 0), rotate(-currentTimeMillis / 2 % 360, Z), translate back
                double px = 0.0625D;
                poseStack.translate(0.0D, 4.875D * px, 0.0D);
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-System.currentTimeMillis() / 2D % 360D)));
                poseStack.translate(0.0D, -4.875D * px, 0.0D);
            } else {
                poseStack.translate(-part.anchor().originX() / 16.0F, -part.anchor().originY() / 16.0F,
                        -part.anchor().originZ() / 16.0F);
            }
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

            LegacyEntityObjMesh mesh = MESHES.get(part.mesh());
            switch (part.mode()) {
                case CUTOUT -> mesh.renderGroup(part.group(), poseStack,
                        bufferSource.getBuffer(RenderType.entityCutoutNoCull(part.texture())),
                        packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                case BLEND -> mesh.renderGroup(part.group(), poseStack,
                        bufferSource.getBuffer(RenderType.entityTranslucent(part.texture())),
                        packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                case GLOW -> mesh.renderGroupTriangles(part.group(), poseStack,
                        bufferSource.getBuffer(HbmUnlitRenderTypes.opaque(part.texture())),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                case LAMP, LAMP_IF_ON -> mesh.renderGroupPositionColorTriangles(part.group(), poseStack,
                        bufferSource.getBuffer(HbmUnlitRenderTypes.OPAQUE_COLOR), LAMP_COLOR);
            }
            // [parity j2] RendererLivingEntity: an enchanted armor piece (shouldRenderPass == 15) repeats the pass with
            // the item glint over the same textured groups; the untextured lamps take no glint texture.
            if (stack.hasFoil() && part.mode() != Mode.LAMP && part.mode() != Mode.LAMP_IF_ON) {
                mesh.renderGroup(part.group(), poseStack, bufferSource.getBuffer(RenderType.armorEntityGlint()),
                        packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
            poseStack.popPose();
        }
    }

    /** ModelNo9: the lamp only for an EntityPlayer whose helmet (slot 4) has the "isOn" tag. */
    private static boolean isOn(LivingEntity entity, ItemStack helmet) {
        if (!(entity instanceof Player)) {
            return false;
        }
        CustomData data = helmet.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("isOn");
    }

    private static LegacyEntityObjMesh mesh(String name) {
        return LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/armor/" + name + ".obj"));
    }

    private static ModelPart resolve(Anchor anchor, HumanoidModel<?> model) {
        return switch (anchor) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
        };
    }
}

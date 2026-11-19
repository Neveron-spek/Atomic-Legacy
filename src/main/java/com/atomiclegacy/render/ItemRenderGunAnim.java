package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.GunB92Item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * [parity c1] 1.7.10 com.hbm.render.item.weapon.ItemRenderGunAnim with com.hbm.render.model.ModelB92 (gun_b92):
 * EQUIPPED, EQUIPPED_FIRST_PERSON and ENTITY draw the box model with textures/models/ModelB92SM.png (here
 * model_b92sm.png); the charge animation tilts the gun and slides the two pump boxes. The inventory keeps the icon.
 * [parity j2] first person hand chain re-checked against the 1.7.10 ItemRenderer (see applyFirstPersonHand).
 */
public final class ItemRenderGunAnim extends LegacyIItemRenderer {

	public static final ModelLayerLocation B92_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_b92"), "main");
	private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/models/model_b92sm.png");

	/** ModelB92#render order; Pump1 / Pump2 are the parts renderAnim offsets. */
	private static final String[] PARTS = {
			"Muzzle1", "Barrel1", "Barrel2", "Grip", "Front1", "Front2", "Body", "Top", "GripBottom", "Handle", "HandleBack",
			"Frame1", "Frame2", "Frame3", "Trigger", "BackPlate1", "Back", "BackPlate2", "Pump1", "Pump2", "BodyPlate" };

	private ModelPart b92;

	/** ModelB92 (64 x 64); mirror is set after addBox in the original and so has no effect. */
	public static LayerDefinition createB92() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		LegacyModelItemRenderers.box(root, "Muzzle1", 22, 36, 0F, 0F, 0F, 2, 3, 2, -24F, 0.5F, -1F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Barrel1", 0, 0, 0F, 0F, 0F, 24, 2, 3, -24F, 1F, -1.5F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Barrel2", 0, 5, 0F, 0F, 0F, 22, 1, 2, -22F, 0.5F, -1F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Grip", 0, 8, 0F, 0F, 0F, 20, 3, 4, -20F, 3F, -2F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Front1", 10, 36, 0F, 0F, 0F, 2, 4, 4, -22F, 0.5F, -2F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Front2", 0, 36, 0F, 0F, 0F, 2, 6, 3, -22F, 0F, -1.5F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Body", 0, 15, 0F, 0F, 0F, 15, 7, 4, 0F, 0.5F, -2F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Top", 28, 60, 0F, 0F, 0F, 15, 1, 3, 0F, 0F, -1.5F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "GripBottom", 24, 43, 0F, 0F, 0F, 18, 1, 2, -18F, 5.5F, -1F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Handle", 0, 45, 0F, 0F, 0F, 6, 15, 4, 6F, 7F, -2F, 0F, 0F, -0.2268928F);
		LegacyModelItemRenderers.box(root, "HandleBack", 20, 46, 5.5F, 0F, 0F, 1, 15, 3, 6F, 7F, -1.5F, 0F, 0F, -0.2268928F);
		LegacyModelItemRenderers.box(root, "Frame1", 28, 57, 0F, 0F, 0F, 7, 1, 2, 0.5F, 11F, -1F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Frame2", 28, 51, 0F, 0F, 0F, 2, 4, 2, -2F, 6.5F, -1F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Frame3", 46, 57, 0F, -1F, 0F, 3, 1, 2, -2F, 10.5F, -1F, 0F, 0F, 0.5235988F);
		LegacyModelItemRenderers.box(root, "Trigger", 36, 53, 0F, 0F, 0F, 2, 3, 1, 4F, 7F, -0.5F, 0F, 0F, 0.1919862F);
		LegacyModelItemRenderers.box(root, "BackPlate1", 56, 53, -1F, 0F, 0F, 1, 4, 3, 15F, 0F, -1.5F, 0F, 0F, -0.5235988F);
		LegacyModelItemRenderers.box(root, "Back", 42, 49, 0F, 0F, 0F, 2, 4, 4, 15F, 3.5F, -2F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "BackPlate2", 48, 5, -2F, 0F, 0F, 2, 4, 4, 15F, 0.5F, -2F, 0F, 0F, -0.4886922F);
		LegacyModelItemRenderers.box(root, "Pump1", 46, 29, 0F, 0F, 0F, 7, 2, 2, 10F, 1F, -1F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "Pump2", 44, 33, 0F, 0F, 0F, 3, 3, 7, 17F, 0.5F, -3.5F, 0F, 0F, 0F);
		LegacyModelItemRenderers.box(root, "BodyPlate", 0, 26, 0F, 0F, 0F, 14, 5, 5, 1.5F, 2F, -2.5F, 0F, 0F, 0F);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public boolean handleRenderType(ItemStack item, Type type) {
		return type == Type.EQUIPPED || type == Type.EQUIPPED_FIRST_PERSON || type == Type.ENTITY;
	}

	@Override
	public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
		return false;
	}

	/**
	 * [parity j2] ItemRenderer#renderItemInFirstPerson (1.7.10) line by line: the swing translation only when
	 * getItemInUseCount() == 0; the equip offset, 45 degree turn, swing rotations (getSwingProgress, also while in use)
	 * and 0.4 scale always; then, while in use with EnumAction.bow (GunB92#getItemUseAction), the bow draw chain with
	 * f10 = getMaxItemUseDuration() - (getItemInUseCount() - partialTick + 1). The left hand mirrors x offsets and
	 * the y / z turns as LegacyIItemRenderer does (1.7.10 had no off hand).
	 */
	@Override
	public void applyFirstPersonHand(PoseStack pose, HumanoidArm arm, ItemStack stack, float equipProcess, float swingProcess) {
		LocalPlayer player = Minecraft.getInstance().player;
		InteractionHand hand = player == null || arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		boolean using = player != null && player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
		float i = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		float f5 = swingProcess;
		float f13 = 0.8F;
		float f6;
		float f7;

		if(!using) {
			f6 = Mth.sin(f5 * (float) Math.PI);
			f7 = Mth.sin(Mth.sqrt(f5) * (float) Math.PI);
			pose.translate(i * -f7 * 0.4F, Mth.sin(Mth.sqrt(f5) * (float) Math.PI * 2.0F) * 0.2F, -f6 * 0.2F);
		}

		pose.translate(i * 0.7F * f13, -0.65F * f13 - equipProcess * 0.6F, -0.9F * f13);
		pose.mulPose(Axis.YP.rotationDegrees(i * 45.0F));
		f6 = Mth.sin(f5 * f5 * (float) Math.PI);
		f7 = Mth.sin(Mth.sqrt(f5) * (float) Math.PI);
		pose.mulPose(Axis.YP.rotationDegrees(i * -f6 * 20.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(i * -f7 * 20.0F));
		pose.mulPose(Axis.XP.rotationDegrees(-f7 * 80.0F));
		float f8 = 0.4F;
		pose.scale(f8, f8, f8);

		if(!using) return;
		float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		pose.mulPose(Axis.ZP.rotationDegrees(i * -18.0F));
		pose.mulPose(Axis.YP.rotationDegrees(i * -12.0F));
		pose.mulPose(Axis.XP.rotationDegrees(-8.0F));
		pose.translate(i * -0.9F, 0.2F, 0.0F);
		float f10 = (float) stack.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - partial + 1.0F);
		float f11 = f10 / 20.0F;
		f11 = (f11 * f11 + f11 * 2.0F) / 3.0F;

		if(f11 > 1.0F) {
			f11 = 1.0F;
		}

		if(f11 > 0.1F) {
			pose.translate(0.0F, Mth.sin((f10 - 0.1F) * 1.3F) * 0.01F * (f11 - 0.1F), 0.0F);
		}

		pose.translate(0.0F, 0.0F, f11 * 0.1F);
		pose.mulPose(Axis.ZP.rotationDegrees(i * -335.0F));
		pose.mulPose(Axis.YP.rotationDegrees(i * -50.0F));
		pose.translate(0.0F, 0.5F, 0.0F);
		float f12 = 1.0F + f11 * 0.2F;
		pose.scale(1.0F, 1.0F, f12);
		pose.translate(0.0F, -0.5F, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(i * 50.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(i * 335.0F));
		// Item#shouldRotateAroundWhenRendering is false for GunB92
	}

	@Override
	protected void renderItem(Type type, ItemStack item) {
		if(b92 == null) b92 = Minecraft.getInstance().getEntityModels().bakeLayer(B92_LAYER);

		switch(type) {
		case EQUIPPED_FIRST_PERSON:
			pose.pushPose();

			glRotate(pose, -135.0F, 0.0F, 0.0F, 1.0F);
			pose.translate(-0.5F, 0.0F, -0.2F);
			pose.scale(0.5F, 0.5F, 0.5F);
			pose.scale(0.5F, 0.5F, 0.5F);
			pose.translate(-0.2F, -0.1F, -0.1F);

			if(GunB92Item.getRotationFromAnim(item) > 0) {
				float off = GunB92Item.getRotationFromAnim(item) * 2;
				glRotate(pose, GunB92Item.getRotationFromAnim(item) * -90, 0.0F, 0.0F, 1.0F);
				pose.translate(off * -0.5F, off * -0.5F, 0.0F);
			}

			renderAnim(GunB92Item.getTransFromAnim(item));

			pose.popPose();
			break;
		case EQUIPPED:
		case ENTITY:
			pose.pushPose();

			glRotate(pose, -200.0F, 0.0F, 0.0F, 1.0F);
			glRotate(pose, 75.0F, 0.0F, 1.0F, 0.0F);
			glRotate(pose, -30.0F, 1.0F, 0.0F, 0.0F);
			pose.translate(0.0F, -0.2F, -0.5F);
			glRotate(pose, -5.0F, 0.0F, 0.0F, 1.0F);
			pose.scale(0.5F, 0.5F, 0.5F);
			pose.translate(-0.3F, -0.4F, 0.15F);

			renderAnim(GunB92Item.getTransFromAnim(item));
			pose.popPose();
			break;
		default: break;
		}
	}

	/** ModelB92#renderAnim: Pump1 / Pump2 get offsetX += tran (ModelRenderer offsets are in blocks). GL_CULL_FACE is enabled. */
	private void renderAnim(float tran) {
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
		for(String name : PARTS) {
			boolean pump = name.equals("Pump1") || name.equals("Pump2");
			if(pump) {
				pose.pushPose();
				pose.translate(tran, 0.0F, 0.0F);
			}
			b92.getChild(name).render(pose, consumer, light, overlayOrNone());
			if(pump) pose.popPose();
		}
	}
}

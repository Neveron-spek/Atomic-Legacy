package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryCatapult;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderFatMan. */
public class ItemRenderFatMan extends ItemRenderWeaponBase {

	/** ResourceManager.fatman (models/weapons/fatman.obj) */
	static final LegacyEntityObjMesh fatman = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/fatman.obj"));
	public static final ResourceLocation fatman_tex = AtomicLegacy.id("textures/models/weapons/fatman.png");
	public static final ResourceLocation fatman_mininuke_tex = AtomicLegacy.id("textures/models/weapons/fatman_mininuke.png");
	public static final ResourceLocation fatman_balefire_tex = AtomicLegacy.id("textures/models/weapons/fatman_balefire.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.5F * offset, -1.25F * offset, 0.5F * offset,
				-1F * offset, -1.25F * offset, 0F * offset);
	}

	protected static String label = "AUTO";

	private void part(String name) {
		fatman.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(fatman_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);

		boolean isLoaded = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0;
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lid = HbmAnimations.getRelevantTransformation("LID");
		double[] nuke = HbmAnimations.getRelevantTransformation("NUKE");
		double[] piston = HbmAnimations.getRelevantTransformation("PISTON");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] gauge = HbmAnimations.getRelevantTransformation("GAUGE");

		pose.translate(0, 1, -2);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, -1, 2);

		part("Launcher");
		pose.pushPose();
		pose.translate(0, 0, handle[2]);
		part("Handle");

		pose.translate(0.4375, -0.875, 0);
		glRotate(pose, gauge[2], 0, 0, 1);
		pose.translate(-0.4375, 0.875, 0);
		part("Gauge");
		pose.popPose();
		pose.pushPose();
		pose.translate(0.25, 0.125, 0);
		glRotate(pose, lid[2], 0, 0, 1);
		pose.translate(-0.25, -0.125, 0);
		part("Lid");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, piston[2]);
		if(!isLoaded && piston[2] == 0) pose.translate(0, 0, 3);
		part("Piston");
		pose.popPose();
		if(isLoaded || nuke[0] != 0 || nuke[1] != 0 || nuke[2] != 0) {
			pose.pushPose();
			pose.translate(nuke[0], nuke[1], nuke[2]);
			renderNuke(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getType(stack, null), pose, buffers, light, interp);
			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(-0.5, 0.5, -3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		boolean isLoaded = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0;

		part("Launcher");
		part("Handle");
		part("Gauge");
		part("Lid");
		if(!isLoaded) pose.translate(0, 0, 3);
		part("Piston");
		if(isLoaded) renderNuke(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getType(stack, null), pose, buffers, light, interp);
	}

	public static void renderNuke(Object type, PoseStack pose, MultiBufferSource buffers, int light, float interp) {
		if(type == XFactoryCatapult.nuke_balefire) {
			renderBalefire(pose, buffers, light, interp);
		} else {
			fatman.renderGroup("MiniNuke", pose, buffers.getBuffer(RenderType.entityCutout(fatman_mininuke_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		}
	}

	/** The balefire mini nuke with three animated glintBF layers (see {@link BalefireGlintRenderType}). */
	public static void renderBalefire(PoseStack pose, MultiBufferSource buffers, int light, float interp) {
		Minecraft mc = Minecraft.getInstance();
		fatman.renderGroup("MiniNuke", pose, buffers.getBuffer(RenderType.entityCutout(fatman_balefire_tex)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		BalefireGlintRenderType.offset = (mc.player != null ? mc.player.tickCount : 0) + interp;
		int layers = 3;
		for(int k = 0; k < layers; ++k) {
			fatman.renderGroup("MiniNuke", pose, buffers.getBuffer(BalefireGlintRenderType.layer(k)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		}
	}
}

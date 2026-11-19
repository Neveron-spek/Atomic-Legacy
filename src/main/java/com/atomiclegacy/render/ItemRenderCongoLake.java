package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.OrchestrasD3;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderCongoLake. */
public class ItemRenderCongoLake extends ItemRenderWeaponBase {

	/** ResourceManager.congolake (models/weapons/congolake.obj) */
	static final LegacyEntityObjMesh congolake = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/congolake.obj"));
	public static final ResourceLocation congolake_tex = AtomicLegacy.id("textures/models/weapons/congolake.png");
	/** ResourceManager.casings_tex (textures/particle/casings.png, port path textures/models/effect/casings.png) */
	public static final ResourceLocation casings_tex = AtomicLegacy.id("textures/models/effect/casings.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.5F * offset, -2F * offset, 1.25F * offset,
				0, -10 / 8D, 0.25);
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name, int argb) {
		congolake.renderGroup(name, pose, vc, light, overlay, argb);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(congolake_tex);
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);

		HbmAnimations.applyRelevantTransformation(pose, "Gun");
		part(vc, "Gun", 0xFFFFFFFF);

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Pump");
		part(vc, "Pump", 0xFFFFFFFF);
		pose.popPose();

		pose.pushPose();
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		HbmAnimations.applyRelevantTransformation(pose, "Sight");
		pose.translate(0, 2.125, 3);
		glRotate(pose, aimingProgress * -90, 1, 0, 0);
		pose.translate(0, -2.125, -3);
		part(vc, "Sight", 0xFFFFFFFF);
		pose.popPose();

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Loop");
		part(vc, "Loop", 0xFFFFFFFF);
		pose.popPose();

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "GuardOuter");
		part(vc, "GuardOuter", 0xFFFFFFFF);
		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "GuardInner");
		part(vc, "GuardInner", 0xFFFFFFFF);
		pose.popPose();
		pose.popPose();

		pose.pushPose();
		IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		// MagazineSingleTypeBase#getAmount/getCasing ignore the inventory argument (MainRegistry.proxy.me().inventory)
		if(ItemGunBaseNT.getLastAnim(stack, 0) != GunAnimation.INSPECT || mag.getAmount(stack, null) > 0) { //omit when inspecting and no shell is loaded
			HbmAnimations.applyRelevantTransformation(pose, "Shell");
			// SpentCasing colors of the loaded round (all 40mm casings have a single color), COLOR_CASE_40MM otherwise
			int color = 0xFF000000 | OrchestrasD3.casingColor(mag.getCasing(stack, null));
			VertexConsumer shell = consumer(casings_tex);
			part(shell, "Shell", color);
			part(shell, "ShellFore", color);
		}
		pose.popPose();

		double smokeScale = 0.25;

		pose.pushPose();
		pose.translate(0, 1.75, 4.25);
		double[] transform = HbmAnimations.getRelevantTransformation("Gun");
		glRotate(pose, -transform[5], 0, 0, 1);
		glRotate(pose, -transform[4], 0, 1, 0);
		glRotate(pose, -transform[3], 1, 0, 0);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.75, 4.25);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.5F, 0.5F, 0.5F);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, -2.5, 4);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, -1.25, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -15D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1.25, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		// ResourceManager.congolake.renderAll() with congolake_tex (the shell parts included, untinted)
		congolake.renderAll(pose, consumer(congolake_tex), light, overlay, 0xFFFFFFFF);

		if(type == LegacyRenderType.EQUIPPED && ent != null) {
			long shot;
			double shotRand = 0;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
				shotRand = gun.shotRand;
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			pose.pushPose();
			pose.translate(0, 1.75, 4.25);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.5F, 0.5F, 0.5F);
			this.renderMuzzleFlash(shot, 150, 7.5);
			pose.popPose();
		}
	}
}

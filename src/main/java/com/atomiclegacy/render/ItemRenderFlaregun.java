package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderFlaregun. */
public class ItemRenderFlaregun extends ItemRenderWeaponBase {

	/** ResourceManager.flaregun (models/weapons/flaregun.obj) */
	static final LegacyEntityObjMesh flaregun = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/flaregun.obj"));
	public static final ResourceLocation flaregun_tex = AtomicLegacy.id("textures/models/weapons/flaregun.png");

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
				-1.25F * offset, -1.5F * offset, 2F * offset,
				0, -5.5 / 8D, 0.5);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(flaregun_tex));
	}

	private void part(VertexConsumer vc, String name) {
		flaregun.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] open = HbmAnimations.getRelevantTransformation("OPEN");
		double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
		double[] flip = HbmAnimations.getRelevantTransformation("FLIP");

		pose.translate(recoil[0], recoil[1], recoil[2]);
		glRotate(pose, recoil[2] * 10, 1, 0, 0);
		glRotate(pose, flip[0], 1, 0, 0);

		pose.translate(0, 0, -8);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, 0, 8);

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(0, 1.8125, -4);
		glRotate(pose, hammer[0] - 15, 1, 0, 0);
		pose.translate(0, -1.8125, 4);
		part(vc, "Hammer");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 2.156, 1.78);
		glRotate(pose, open[0], 1, 0, 0);
		pose.translate(0, -2.156, -1.78);
		part(vc, "Barrel");
		pose.translate(shell[0], shell[1], shell[2]);
		part(vc, "Flare");
		pose.popPose();

		double smokeScale = 0.5;

		pose.pushPose();
		pose.translate(0, 4, 9);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 2.5D);
		pose.translate(0, 0, 0.1);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 2D);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.25, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void setupEntity(ItemStack stack) {
		super.setupEntity(stack);
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		flaregun.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);
	}
}

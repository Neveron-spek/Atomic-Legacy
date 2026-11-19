package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderBolter (no muzzle flash in the original). */
public class ItemRenderBolter extends ItemRenderWeaponBase {

	/** ResourceManager.bolter (models/weapons/bolter.obj) */
	static final LegacyEntityObjMesh bolter = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/bolter.obj"));
	public static final ResourceLocation bolter_tex = AtomicLegacy.id("textures/models/weapons/bolter.png");

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
				-1.5F * offset, -2F * offset, 2.5F * offset,
				0, -10.5 / 8D, 1.25);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(bolter_tex));
	}

	private void part(VertexConsumer vc, String name) {
		bolter.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		VertexConsumer vc = consumer();
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);

		glRotate(pose, 180, 0, 1, 0);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		glRotate(pose, recoil[0] * 5, 1, 0, 0);
		pose.translate(0, 0, recoil[0]);

		double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
		pose.translate(0, tilt[0], 3);
		glRotate(pose, tilt[0] * 35, 1, 0, 0);
		pose.translate(0, 0, -3);

		part(vc, "Body");

		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		pose.pushPose();
		pose.translate(0, 0, 5);
		glRotate(pose, mag[0] * 60 * (mag[2] == 1 ? 2.5 : 1), -1, 0, 0);
		pose.translate(0, 0, -5);
		part(vc, "Mag");
		if(mag[2] != 1) part(vc, "Bullet");
		pose.popPose();

		// unlit, full-bright, culling off, alpha blending: FontRenderer#drawString in the world
		pose.pushPose();
		Font font = Minecraft.getInstance().font;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		// MagazineFullReload#getAmount ignores the inventory argument
		String s = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) + "";
		float f3 = 0.04F;
		pose.translate(0.025F - (font.width(s) / 2) * 0.04F, 2.11F, 2.91F);
		pose.scale(f3, -f3, f3);
		glRotate(pose, 45, 1, 0, 0);
		ItemRenderQuadro.drawTwoSided(font, s, 0xff0000, pose, buffers);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -0.75, 1.25);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.25, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		glRotate(pose, 180, 0, 1, 0);
		bolter.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);
	}
}

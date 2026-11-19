package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderTeslaCannon. */
public class ItemRenderTeslaCannon extends ItemRenderWeaponBase {

	/** ResourceManager.tesla_cannon (models/weapons/tesla_cannon.obj) */
	static final LegacyEntityObjMesh tesla_cannon = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/tesla_cannon.obj"));
	public static final ResourceLocation tesla_cannon_tex = AtomicLegacy.id("textures/models/weapons/tesla_cannon.png");
	/** RenderPlushie.yomiModel (models/trinkets/yomi.obj) and yomiTex */
	static final LegacyEntityObjMesh yomiModel = LegacyEntityObjMesh.load(AtomicLegacy.id("models/trinkets/yomi.obj"));
	public static final ResourceLocation yomiTex = AtomicLegacy.id("textures/models/trinkets/yomi.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

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
				-1.75F * offset, -0.5F * offset, 1.75F * offset,
				-1.3125F * offset, 0F * offset, -0.5F * offset);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(tesla_cannon_tex));
	}

	private void part(VertexConsumer vc, String name) {
		tesla_cannon.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
		double[] count = HbmAnimations.getRelevantTransformation("COUNT");
		double[] yomi = HbmAnimations.getRelevantTransformation("YOMI");
		double[] squeeze = HbmAnimations.getRelevantTransformation("SQUEEZE");

		pose.translate(0, -2, -2);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 2);

		pose.translate(0, 0, recoil[2]);
		glRotate(pose, recoil[2] * 2, 1, 0, 0);

		var player = Minecraft.getInstance().player;
		int amount = Math.max((int) count[0], gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player != null ? player.getInventory() : null));

		part(vc, "Gun");
		part(vc, "Extension");

		double cogAngle = cycle[2];

		pose.pushPose();
		pose.translate(0, -1.625, 0);
		glRotate(pose, cogAngle, 0, 0, 1);
		pose.translate(0, 1.625, 0);
		part(vc, "Cog");
		pose.popPose();

		pose.pushPose();

		pose.translate(0, -1.625, 0);
		glRotate(pose, cogAngle, 0, 0, 1);
		pose.translate(0, 1.625, 0);

		for(int i = 0; i < Math.min(amount, 8); i++) {
			part(vc, "Capacitor");

			if(i < 4) {
				pose.translate(0, -1.625, 0);
				glRotate(pose, -22.5, 0, 0, 1);
				pose.translate(0, 1.625, 0);
			} else {
				if(i == 4) {
					pose.translate(0, -1.625, 0);
					glRotate(pose, -cogAngle, 0, 0, 1);
					pose.translate(0, 1.625, 0);
					pose.translate(-cogAngle * 0.5 / 22.5, 0, 0);
				}
				pose.translate(0.5, 0, 0);
			}
		}
		pose.popPose();

		pose.pushPose();
		pose.translate(yomi[0], yomi[1], yomi[2]);
		glRotate(pose, 135, 0, 1, 0);
		pose.scale((float) squeeze[0], (float) squeeze[1], (float) squeeze[2]);
		yomiModel.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(yomiTex)), light, overlay, 0xFFFFFFFF);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 1.5, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Extension");
		part(vc, "Cog");

		pose.pushPose();
		for(int i = 0; i < 10; i++) {
			part(vc, "Capacitor");

			if(i < 4) {
				pose.translate(0, -1.625, 0);
				glRotate(pose, -22.5, 0, 0, 1);
				pose.translate(0, 1.625, 0);
			} else {
				pose.translate(0.5, 0, 0);
			}
		}
		pose.popPose();
	}
}

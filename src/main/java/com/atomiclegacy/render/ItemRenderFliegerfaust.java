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

/** [weapons batch E1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderFliegerfaust. */
public class ItemRenderFliegerfaust extends ItemRenderWeaponBase {

	/** ResourceManager.fliegerfaust (models/weapons/fliegerfaust.obj) */
	static final LegacyEntityObjMesh fliegerfaust = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/fliegerfaust.obj"));
	public static final ResourceLocation fliegerfaust_tex = AtomicLegacy.id("textures/models/weapons/fliegerfaust.png");

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
				-2.75F * offset, -2F * offset, -1.5F * offset,
			-0.9375, -7.75 / 8D, -2.5);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(fliegerfaust_tex));
	}

	private void part(VertexConsumer vc, String name) {
		fliegerfaust.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		double[] drum = HbmAnimations.getRelevantTransformation("DRUM");
		double[] drumrot = HbmAnimations.getRelevantTransformation("DRUMROT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] unloading = HbmAnimations.getRelevantTransformation("UNLOADING");

		// MagazineFullReload#getAmount ignores the inventory argument
		int rocketNum = unloading[0] > 0 ? gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) : 9;

		glRotate(pose, aimingProgress * -45, 0, 0, 1);

		pose.translate(0, 0, 1);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 0, -1);

		pose.translate(0, -6, -1);
		glRotate(pose, reload[0], 1, 0, 0);
		pose.translate(0, 6, 1);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Launcher");

		pose.pushPose();
		pose.translate(0, 0, 2);
		pose.translate(drum[0], drum[1], drum[2]);
		pose.translate(0, 0, -8.5);
		glRotate(pose, drumrot[0], 1, 0, 0);
		pose.translate(0, 0, 8.5);
		part(vc, "Drum");
		for(int i = 1; i <= rocketNum; i++) part(vc, "Rocket" + i);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 1, -2);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Launcher");

		pose.translate(0, 0, 2);
		part(vc, "Drum");

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
			pose.translate(0, 0, 8);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 150, 7.5);
			pose.popPose();
		}
	}
}

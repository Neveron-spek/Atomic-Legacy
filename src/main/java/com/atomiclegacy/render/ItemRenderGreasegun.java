package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderGreasegun. */
public class ItemRenderGreasegun extends ItemRenderWeaponBase {

	/** ResourceManager.greasegun (models/weapons/greasegun.obj) */
	static final LegacyEntityObjMesh greasegun = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/greasegun.obj"));
	public static final ResourceLocation greasegun_tex = AtomicLegacy.id("textures/models/weapons/greasegun.png");
	public static final ResourceLocation greasegun_clean_tex = AtomicLegacy.id("textures/models/weapons/greasegun_clean.png");

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
				-1.5F * offset, -1F * offset, 1.75F * offset,
				0, -2.625 / 8D, 1.125);
	}

	private VertexConsumer consumer(ItemStack stack) {
		return buffers.getBuffer(RenderType.entityCutout(isRefurbished(stack) ? greasegun_clean_tex : greasegun_tex));
	}

	private void part(VertexConsumer vc, String name) {
		greasegun.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	private static void rotate(com.mojang.blaze3d.vertex.PoseStack pose, double angle, float x, float y, float z) {
		if(x != 0) pose.mulPose(Axis.XP.rotationDegrees((float) angle * x));
		if(y != 0) pose.mulPose(Axis.YP.rotationDegrees((float) angle * y));
		if(z != 0) pose.mulPose(Axis.ZP.rotationDegrees((float) angle * z));
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(stack);
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] stock = HbmAnimations.getRelevantTransformation("STOCK");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] flap = HbmAnimations.getRelevantTransformation("FLAP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");

		pose.translate(0, -3, -3);
		rotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 3, 3);

		pose.translate(0, -3, -3);
		rotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 3, 3);

		if(ItemGunBaseNT.aimingProgress < 1F) rotate(pose, turn[2], 0, 0, 1);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(0, 0, -4 - stock[2]);
		part(vc, "Stock");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part(vc, "Magazine");
		if(bullet[0] != 1) part(vc, "Bullet");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, -1.4375, -0.125);
		rotate(pose, handle[0], 1, 0, 0);
		pose.translate(0, 1.4375, 0.125);
		part(vc, "Handle");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.53125, 0);
		rotate(pose, flap[2], 0, 0, 1);
		pose.translate(0, -0.5125, 0);
		part(vc, "Flap");
		pose.popPose();

		double smokeScale = 0.25;

		pose.pushPose();
		pose.translate(-0.25, 0, 1.5);
		rotate(pose, turn[2], 0, 0, -1);
		rotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 8);
		rotate(pose, turn[2], 0, 0, -1);
		rotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 8);
		rotate(pose, 90, 0, 1, 0);
		rotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.5F, 0.5F, 0.5F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		rotate(pose, 25, 1, 0, 0);
		rotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 2, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		rotate(pose, 90, 0, 1, 0);
		pose.translate(0, 2, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		greasegun.renderAll(pose, consumer(stack), light, overlay, 0xFFFFFFFF);

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
			rotate(pose, 90, 0, 1, 0);
			rotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.5F, 0.5F, 0.5F);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}

	public boolean isRefurbished(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_GREASEGUN_CLEAN);
	}
}

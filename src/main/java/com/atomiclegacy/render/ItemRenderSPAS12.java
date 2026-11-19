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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderSPAS12. [weapons batch D1] */
public class ItemRenderSPAS12 extends ItemRenderWeaponBase {

	/** ResourceManager.spas_12 (models/weapons/spas-12.obj) */
	static final LegacyEntityObjMesh spas_12 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/spas-12.obj"));
	public static final ResourceLocation spas_12_tex = AtomicLegacy.id("textures/models/weapons/spas-12.png");
	/** ResourceManager.casings_tex (textures/particle/casings.png, same file as the port's casing effect texture) */
	public static final ResourceLocation casings_tex = AtomicLegacy.id("textures/models/effect/casings.png");

	/** SpentCasing.COLOR_CASE_BRASS */
	private static final int COLOR_CASE_BRASS = 0xEBC35E;

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
				-1.25F * offset, -1.75F * offset, -0.5F * offset,
				0, 0, 0);
	}

	private void part(String name) {
		spas_12.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(spas_12_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 180, 0, 1, 0);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

		glRotate(pose, equip[0], 1, 0, 0);

		HbmAnimations.applyRelevantTransformation(pose, "MainBody");
		part("MainBody");

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "PumpGrip");
		part("PumpGrip");
		pose.popPose();

		pose.pushPose();

		HbmAnimations.applyRelevantTransformation(pose, "Shell");
		int casing = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCasing(stack, Minecraft.getInstance().player.getInventory());
		int color0 = COLOR_CASE_BRASS;
		int color1 = COLOR_CASE_BRASS;

		int[] colors = TurretVisualClientEffects.casingColors(casing);
		if(colors != null) {
			color0 = colors[0];
			color1 = colors[colors.length > 1 ? 1 : 0];
		}

		VertexConsumer shells = buffers.getBuffer(RenderType.entityCutout(casings_tex));
		spas_12.renderGroup("Shell", pose, shells, light, overlay, 0xFF000000 | color1);
		spas_12.renderGroup("ShellFore", pose, shells, light, overlay, 0xFF000000 | color0);

		double smokeScale = 0.25;

		pose.pushPose();
		pose.translate(0, 1.5, -11);
		glRotate(pose, -90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.75D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.5, -11);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();

		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -0.75, 0);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(4.25, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, -4.25);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		glRotate(pose, 180, 0, 1, 0);
		part("MainBody");
		part("PumpGrip");

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
			pose.translate(0, 1.5, -11);
			glRotate(pose, -90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}

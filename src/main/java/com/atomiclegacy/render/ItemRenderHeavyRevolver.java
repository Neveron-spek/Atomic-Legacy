package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderHeavyRevolver (gun_heavy_revolver: heavy_revolver_tex,
 * gun_heavy_revolver_lilmac: lilmac_tex, gun_heavy_revolver_protege: heavy_revolver_protege_tex).
 */
public class ItemRenderHeavyRevolver extends ItemRenderWeaponBase {

	/** ResourceManager.lilmac (models/weapons/lilmac.obj) */
	public static final LegacyEntityObjMesh lilmac = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/lilmac.obj"));
	public static final ResourceLocation heavy_revolver_tex = AtomicLegacy.id("textures/models/weapons/heavy_revolver.png");
	public static final ResourceLocation heavy_revolver_protege_tex = AtomicLegacy.id("textures/models/weapons/protege.png");
	public static final ResourceLocation lilmac_tex = AtomicLegacy.id("textures/models/weapons/lilmac.png");
	public static final ResourceLocation lilmac_scope_tex = AtomicLegacy.id("textures/models/weapons/lilmac_scope.png");

	protected ResourceLocation texture;

	public ItemRenderHeavyRevolver(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1);

		boolean isScoped = this.isScoped(stack);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.0F * offset, -0.75F * offset, 1F * offset,
				0, isScoped ? (-4.75 / 8D) : (-3.875 / 8D), isScoped ? -0.25 : 0);
	}

	protected void part(ResourceLocation tex, String name) {
		lilmac.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		boolean isScoped = this.isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);

		double[] equipSpin = HbmAnimations.getRelevantTransformation("ROTATE");
		double[] spin = HbmAnimations.getRelevantTransformation("SPIN");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadLift = HbmAnimations.getRelevantTransformation("RELOAD_LIFT");
		double[] reloadJolt = HbmAnimations.getRelevantTransformation("RELOAD_JOLT");
		double[] reloadTilt = HbmAnimations.getRelevantTransformation("RELAOD_TILT");
		double[] cylinderFlip = HbmAnimations.getRelevantTransformation("RELOAD_CYLINDER");
		double[] reloadBullets = HbmAnimations.getRelevantTransformation("RELOAD_BULLETS");

		glRotate(pose, spin[0], 0, 0, 1);

		pose.translate(6, -3, 0);
		glRotate(pose, equipSpin[0], 0, 0, 1);
		pose.translate(-6, 3, 0);

		standardAimingTransform(pose, stack, 0, 0, recoil[2], -recoil[2], 0, 0);
		glRotate(pose, recoil[2] * 10, 0, 0, 1);

		pose.pushPose();
		pose.translate(-9, 2.5, 0);
		glRotate(pose, recoil[2] * -10, 0, 0, 1);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		glRotate(pose, reloadLift[0], 0, 0, 1);
		pose.translate(reloadJolt[0], 0, 0);
		glRotate(pose, reloadTilt[0], 1, 0, 0);

		part(texture, "Gun");

		pose.pushPose();
		glRotate(pose, cylinderFlip[0], 1, 0, 0);
		part(texture, "Pivot");
		pose.translate(0, 1.75, 0);
		glRotate(pose, HbmAnimations.getRelevantTransformation("DRUM")[2] * -60, 1, 0, 0);
		pose.translate(0, -1.75, 0);
		part(texture, "Cylinder");
		pose.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
		if(HbmAnimations.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1)
			part(texture, "Bullets");
		part(texture, "Casings");
		pose.popPose();

		pose.pushPose(); /// HAMMER ///
		pose.translate(4, 1.25, 0);
		glRotate(pose, -30 + 30 * HbmAnimations.getRelevantTransformation("HAMMER")[2], 0, 0, 1);
		pose.translate(-4, -1.25, 0);
		part(texture, "Hammer");
		pose.popPose();

		if(isScoped) {
			part(lilmac_scope_tex, "Scope");
		}

		pose.pushPose();
		pose.translate(0.125, 2.5, 0);
		this.renderGapFlash(gun.lastShot[0]);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.scale(0.75F, 0.75F, 0.75F);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(isScoped(stack)) {
			double scale = 1.125D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(0, -0.5, 0);
		} else {
			double scale = 1.25D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		glRotate(pose, 90, 0, 1, 0);

		part(texture, "Gun");
		part(texture, "Cylinder");
		part(texture, "Bullets");
		part(texture, "Casings");
		part(texture, "Pivot");
		part(texture, "Hammer");
		if(isScoped(stack)) {
			part(lilmac_scope_tex, "Scope");
		}

		if(type == LegacyRenderType.EQUIPPED && ent != null) {
			long shot;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			pose.pushPose();
			pose.translate(0.125, 2.5, 0);
			this.renderGapFlash(shot);
			pose.popPose();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get() || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}
}

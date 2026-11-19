package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderAmat (gun_amat, gun_amat_subtlety and
 * gun_amat_penance with their own textures; penance is always silenced).
 */
public class ItemRenderAmat extends ItemRenderWeaponBase {

	/** ResourceManager.amat (models/weapons/amat.obj) */
	static final LegacyEntityObjMesh amat = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/amat.obj"));
	public static final ResourceLocation amat_tex = AtomicLegacy.id("textures/models/weapons/amat.png");
	public static final ResourceLocation amat_subtlety_tex = AtomicLegacy.id("textures/models/weapons/amat_subtlety.png");
	public static final ResourceLocation amat_penance_tex = AtomicLegacy.id("textures/models/weapons/amat_penance.png");

	public ResourceLocation texture;

	public ItemRenderAmat(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * (isScoped(stack) ? 0.8F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;

		standardAimingTransform(pose, stack,
			-1F * offset, -1F * offset, 3.25F * offset,
			0, -4.875 / 8D, 1.875);
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		amat.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(texture);
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		boolean deployed = HbmAnimations.getRelevantAnim(0) == null || HbmAnimations.getRelevantAnim(0).animation.getBus("BIPOD") == null;
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] bipod = HbmAnimations.getRelevantTransformation("BIPOD");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] boltTurn = HbmAnimations.getRelevantTransformation("BOLT_TURN");
		double[] boltPull = HbmAnimations.getRelevantTransformation("BOLT_PULL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] scopeThrow = HbmAnimations.getRelevantTransformation("SCOPE_THROW");
		double[] scopeSpin = HbmAnimations.getRelevantTransformation("SCOPE_SPIN");

		pose.translate(0, 0, recoil[2]);

		pose.translate(0, -3, -8);
		glRotate(pose, equip[0], 1, 0, 0);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 3, 8);

		part(vc, "Gun");

		if(isScoped(stack)) {
			pose.pushPose();
			pose.translate(scopeThrow[0], scopeThrow[1], scopeThrow[2]);
			pose.translate(0, 1.5, -4.5);
			glRotate(pose, scopeSpin[0], 1, 0, 0);
			pose.translate(0, -1.5, 4.5);
			part(vc, "Scope");
			pose.popPose();
		}

		pose.pushPose();
		pose.translate(0, 0.625, 0);
		glRotate(pose, boltTurn[2], 0, 0, 1);
		pose.translate(0, -0.625, 0);
		pose.translate(0, 0, boltPull[2]);
		part(vc, "Bolt");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part(vc, "Magazine");
		pose.popPose();

		pose.pushPose();
		pose.translate(0.3125, -0.625, -1);
		glRotate(pose, deployed ? 25 : bipod[1], 0, 0, 1);
		pose.translate(-0.3125, 0.625, 1);
		part(vc, "BipodHingeLeft");
		pose.translate(0.3125, -0.625, -1);
		glRotate(pose, deployed ? 80 : bipod[0], 1, 0, 0);
		pose.translate(-0.3125, 0.625, 1);
		part(vc, "BipodLeft");
		pose.popPose();

		pose.pushPose();
		pose.translate(-0.3125, -0.625, -1);
		glRotate(pose, deployed ? -25 : -bipod[1], 0, 0, 1);
		pose.translate(0.3125, 0.625, 1);
		part(vc, "BipodHingeRight");
		pose.translate(-0.3125, -0.625, -1);
		glRotate(pose, deployed ? 80 : bipod[0], 1, 0, 0);
		pose.translate(0.3125, 0.625, 1);
		part(vc, "BipodRight");
		pose.popPose();

		if(isSilenced(stack)) {
			pose.translate(0, 0.625, -4.3125);
			pose.scale(1.25F, 1.25F, 1.25F);
			ItemRenderG3.g3.renderGroup("Silencer", pose, consumer(ItemRenderG3.g3_attachments), light, overlay, 0xFFFFFFFF);
		} else {
			part(vc, "MuzzleBrake");

			double smokeScale = 0.5;

			pose.pushPose();
			pose.translate(0, 0.625, 12);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 0.5, 11);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderGapFlash(gun.lastShot[0]);
			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.5, 6.75);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(isSilenced(stack)) {
			double scale = 0.8175D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(-0.5, 0.5, -1);
		} else {
			double scale = 0.9375D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(-0.5, 0.5, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.25, -1.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer(texture);
		part(vc, "Gun");
		part(vc, "Bolt");
		part(vc, "Magazine");
		part(vc, "BipodLeft");
		part(vc, "BipodHingeLeft");
		part(vc, "BipodRight");
		part(vc, "BipodHingeRight");
		if(isScoped(stack)) part(vc, "Scope");

		boolean silenced = isSilenced(stack);
		if(silenced) {
			pose.translate(0, 0.625, -4.3125);
			pose.scale(1.25F, 1.25F, 1.25F);
			ItemRenderG3.g3.renderGroup("Silencer", pose, consumer(ItemRenderG3.g3_attachments), light, overlay, 0xFFFFFFFF);
		} else {
			part(vc, "MuzzleBrake");
		}

		if(type == LegacyRenderType.EQUIPPED && !silenced && ent != null) {
			long shot;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			pose.pushPose();
			pose.translate(0, 0.5, 11);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderGapFlash(shot);
			pose.popPose();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return true;
	}

	public boolean isSilenced(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_AMAT_PENANCE.get() || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}
}

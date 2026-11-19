package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory762mm;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMAS36. GL_CLIP_PLANE0 (plane 0, 1, 0, -0.5 set before the
 * BULLETS translation) is reproduced by clipping the "Bullets" faces on the CPU.
 */
public class ItemRenderMAS36 extends ItemRenderWeaponBase {

	/** ResourceManager.mas36 (models/weapons/mas36.obj) */
	static final LegacyEntityObjMesh mas36 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/mas36.obj"));
	public static final ResourceLocation mas36_tex = AtomicLegacy.id("textures/models/weapons/mas36.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;

		if(isScoped(stack)) {
			standardAimingTransform(pose, stack,
					-1.5F * offset, -1.25F * offset, 1.75F * offset,
					-0.2, -5.875 / 8D, 1.125);
		} else {
			standardAimingTransform(pose, stack,
					-1.5F * offset, -1.25F * offset, 1.75F * offset,
					0, -4.6825 / 8D, 0.75);
		}
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(mas36_tex));
	}

	private void part(VertexConsumer vc, String name) {
		mas36.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] stock = HbmAnimations.getRelevantTransformation("STOCK");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] boltTurn = HbmAnimations.getRelevantTransformation("BOLT_TURN");
		double[] boltPull = HbmAnimations.getRelevantTransformation("BOLT_PULL");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] showClip = HbmAnimations.getRelevantTransformation("SHOW_CLIP");
		double[] clip = HbmAnimations.getRelevantTransformation("CLIP");
		double[] bullets = HbmAnimations.getRelevantTransformation("BULLETS");
		double[] stab = HbmAnimations.getRelevantTransformation("STAB");

		pose.translate(0, -3, -3);
		glRotate(pose, equip[0], 1, 0, 0);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 3, 3);

		pose.translate(stab[0], stab[1], stab[2]);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");
		if(hasBayonet(stack)) part(vc, "Bayonet");

		pose.pushPose();
		pose.translate(0, 0.3125, -2.125);
		glRotate(pose, stock[0], 1, 0, 0);
		pose.translate(0, -0.3125, 2.125);
		part(vc, "Stock");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.0625 * 18.5, 0);
		glRotate(pose, boltTurn[2], 0, 0, 1);
		pose.translate(0, 0.0625 * -18.5, 0);
		pose.translate(0, 0, boltPull[2]);
		part(vc, "Bolt");
		pose.popPose();

		pose.pushPose();
		pose.translate(bullet[0], bullet[1], bullet[2]);
		part(vc, "Bullet");
		pose.popPose();

		if(isScoped) part(vc, "Scope");

		if(showClip[0] != 0) {
			pose.pushPose();
			pose.translate(clip[0], clip[1], clip[2]);
			part(vc, "Clip");
			pose.popPose();
			pose.pushPose();
			pose.translate(bullets[0], bullets[1], bullets[2]);
			if(bullets[0] == 0) {
				// plane y = 0.5 in the coordinates before the BULLETS translation
				mas36.renderGroupClippedY("Bullets", pose, vc, light, overlay, 0xFFFFFFFF, 0.5D - bullets[1]);
			} else {
				part(vc, "Bullets");
			}
			pose.popPose();
		}

		double smokeScale = 0.25;

		pose.pushPose();
		pose.translate(0, 1.125, 8);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1, 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.5F, 0.5F, 0.5F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.5, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.25, -2.5);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Stock");
		part(vc, "Bolt");
		if(isScoped(stack)) part(vc, "Scope");
		if(hasBayonet(stack)) part(vc, "Bayonet");
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Stock");
		part(vc, "Bolt");
		if(isScoped(stack)) part(vc, "Scope");
		if(type != LegacyRenderType.EQUIPPED) pose.translate(0, -1, -6);
		if(hasBayonet(stack)) part(vc, "Bayonet");

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
			pose.translate(0, 1, 8);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.5F, 0.5F, 0.5F);
			this.renderMuzzleFlash(shot, 75, 10);
			pose.popPose();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}

	public boolean hasBayonet(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XFactory762mm.ID_MAS_BAYONET);
	}
}

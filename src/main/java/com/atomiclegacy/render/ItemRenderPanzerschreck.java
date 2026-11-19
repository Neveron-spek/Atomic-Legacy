package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderPanzerschreck. */
public class ItemRenderPanzerschreck extends ItemRenderWeaponBase {

	/** ResourceManager.panzerschreck (models/weapons/panzerschreck.obj) */
	static final LegacyEntityObjMesh panzerschreck = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/panzerschreck.obj"));
	public static final ResourceLocation panzerschreck_tex = AtomicLegacy.id("textures/models/weapons/panzerschreck.png");

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
				-2.75F * offset, -2F * offset, 2.5F * offset,
			-0.9375, -9.25 / 8D, 0.25);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(panzerschreck_tex));
	}

	private void part(VertexConsumer vc, String name) {
		panzerschreck.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		double[] rocket = HbmAnimations.getRelevantTransformation("ROCKET");

		pose.translate(0, -1, -1);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 1);

		pose.translate(0, -4, -3);
		glRotate(pose, reload[0], 1, 0, 0);
		pose.translate(0, 4, 3);

		part(vc, "Tube");
		if(hasShield(stack)) part(vc, "Shield");

		pose.pushPose();
		pose.translate(rocket[0], rocket[1], rocket[2]);
		part(vc, "Rocket");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 6.5);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 3D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.5, 1);
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
		double scale = -10D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Tube");
		if(hasShield(stack)) part(vc, "Shield");

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
			pose.translate(0, 0, 6.5);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 150, 7.5);
			pose.popPose();
		}
	}

	/** WeaponModPanzerschreckSawedOff (ID_NO_SHIELD, weapon_mod_special_saw) removes the shield. [weapon mods G2: comment] */
	public boolean hasShield(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_SHIELD);
	}
}

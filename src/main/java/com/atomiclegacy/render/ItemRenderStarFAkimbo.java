package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderStarFAkimbo. */
public class ItemRenderStarFAkimbo extends ItemRenderWeaponBase {

	private static final LegacyEntityObjMesh star_f = ItemRenderStarF.star_f;

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

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
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		star_f.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			int index = i == -1 ? 0 : 1;
			VertexConsumer vc = consumer(ItemRenderStarF.star_f_elite_tex);

			pose.pushPose();
			standardAimingTransform(pose, stack, -2F * offset * i, -1.75F * offset, 2.5F * offset, 0, -7.625 / 8D, 1);

			double scale = 0.25D;
			pose.scale((float) scale, (float) scale, (float) scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER", index);
			double[] tilt = HbmAnimations.getRelevantTransformation("TILT", index);
			double[] turn = HbmAnimations.getRelevantTransformation("TURN", index);
			double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
			double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
			double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);

			pose.translate(0, -2, -8);
			glRotate(pose, equip[0], 1, 0, 0);
			pose.translate(0, 2, 8);

			pose.translate(0, 1, -3);
			glRotate(pose, turn[2] * i, 0, 0, 1);
			glRotate(pose, tilt[0], 1, 0, 0);
			pose.translate(0, -1, 3);

			pose.translate(0, 0, recoil[2]);

			part(vc, "Gun");

			pose.pushPose();
			pose.translate(0, 1.75, -4.25);
			glRotate(pose, 60 * (hammer[0] - 1), 1, 0, 0);
			pose.translate(0, -1.75, 4.25);
			part(vc, "Hammer");
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 0, slide[2] * 2.3125);
			part(vc, "Slide");
			pose.popPose();

			pose.pushPose();
			pose.translate(mag[0], mag[1], mag[2]);
			part(vc, "Mag");
			pose.translate(bullet[0], bullet[1], bullet[2]);
			part(vc, "Bullet");
			pose.popPose();

			if(hasSilencer(stack, index)) {
				pose.pushPose();
				pose.translate(0, 2.375, -0.25);
				ItemRenderUzi.uzi.renderGroup("Silencer", pose, consumer(ItemRenderUzi.uzi_tex), light, overlay, 0xFFFFFFFF);
				pose.popPose();

			} else {
				double smokeScale = 0.5;

				pose.pushPose();
				pose.translate(0, 3, 6.125);
				glRotate(pose, 90, 0, 1, 0);
				pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
				this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.75D);
				pose.popPose();

				renderMuzzleFlash(gun.shotRand, gun.lastShot[index]);
			}

			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, -0.25, 1.75);
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		pose.translate(0, -0.25, 1.75);
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 8, 0);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.25, -5);
	}

	@Override
	public void renderEquipped(ItemStack stack, LivingEntity entity) {
		renderStandardGun(stack, 1);
		if(!hasSilencer(stack, 1)) renderThirdPersonFlash(entity, stack, 1);
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {
		renderStandardGun(stack, 0);
		if(!hasSilencer(stack, 0)) renderThirdPersonFlash(ent, stack, 0);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		VertexConsumer vc = consumer(ItemRenderStarF.star_f_elite_tex);
		part(vc, "Gun");
		part(vc, "Slide");
		part(vc, "Mag");
		part(vc, "Hammer");
		if(hasSilencer(stack, index)) {
			pose.translate(0, 2.375, -0.25);
			ItemRenderUzi.uzi.renderGroup("Silencer", pose, consumer(ItemRenderUzi.uzi_tex), light, overlay, 0xFFFFFFFF);
		}
	}

	@Override
	public void renderEntity(ItemStack stack) {
		boolean anySilenced = hasSilencer(stack, 0) || hasSilencer(stack, 1);

		if(anySilenced) {
			pose.scale(0.75F, 0.75F, 0.75F);
		}

		pose.pushPose();
		pose.translate(-1, 1, 0);
		renderStandardGun(stack, 1);
		pose.popPose();

		pose.pushPose();
		pose.translate(1, 1, 0);
		renderStandardGun(stack, 0);
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		boolean anySilenced = hasSilencer(stack, 0) || hasSilencer(stack, 1);

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0.5, 0, 0);
		if(anySilenced) {
			double scale = 0.625D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(0, 0, -4);
		}
		renderStandardGun(stack, 1);
		pose.popPose();

		pose.translate(0, 0, 5);

		pose.pushPose();
		glRotate(pose, 225, 0, 0, 1);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -90, 1, 0, 0);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, -45, 0, 1, 0);
		pose.translate(-0.5, 0, 0);
		if(anySilenced) {
			double scale = 0.625D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(0, 0, -4);
		}
		renderStandardGun(stack, 0);
		pose.popPose();
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public void renderStandardGun(ItemStack stack, int index) {
		VertexConsumer vc = consumer(ItemRenderStarF.star_f_elite_tex);
		part(vc, "Gun");
		part(vc, "Slide");
		part(vc, "Mag");
		part(vc, "Hammer");
		boolean silenced = hasSilencer(stack, index);
		if(silenced) {
			pose.translate(0, 2.375, -0.25);
			ItemRenderUzi.uzi.renderGroup("Silencer", pose, consumer(ItemRenderUzi.uzi_tex), light, overlay, 0xFFFFFFFF);
		}
	}

	public void renderThirdPersonFlash(Entity ent, ItemStack stack, int config) {

		long shot;
		double shotRand = 0;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[config];
			shotRand = gun.shotRand;
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		renderMuzzleFlash(shotRand, shot);
	}

	public void renderMuzzleFlash(double shotRand, long shot) {

		pose.pushPose();
		pose.translate(0, 3, 6.125);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * shotRand, 1, 0, 0);
		this.renderMuzzleFlash(shot, 75, 7.5);
		pose.popPose();
	}
}

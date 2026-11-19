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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderCarbine. */
public class ItemRenderCarbine extends ItemRenderWeaponBase {

	/** ResourceManager.carbine (models/weapons/carbine.obj) */
	static final LegacyEntityObjMesh carbine = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/carbine.obj"));
	public static final ResourceLocation carbine_tex = AtomicLegacy.id("textures/models/weapons/huntsman.png");
	public static final ResourceLocation carbine_bayonet_tex = AtomicLegacy.id("textures/models/weapons/carbine_bayonet.png");
	public static final ResourceLocation carbine_scope_tex = AtomicLegacy.id("textures/models/weapons/carbine_scope.png");

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

		if(this.isScoped(stack)) {
			standardAimingTransform(pose, stack,
					-1.5F * offset, -1.5F * offset, 0.875F * offset,
					0, -8 / 8D, 0.25);
		} else {
			standardAimingTransform(pose, stack,
					-1.5F * offset, -1.5F * offset, 0.875F * offset,
					0, -6.25 / 8D, 0.25);
		}
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		carbine.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(carbine_tex);
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] rel = HbmAnimations.getRelevantTransformation("REL");
		double[] stab = HbmAnimations.getRelevantTransformation("STAB");

		pose.translate(0, -1, -2);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 2);

		pose.translate(0, 0, -2);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 0, 2);

		pose.translate(stab[0], stab[1], stab[2]);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(0, 0, slide[2]);
		part(vc, "Slide");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		part(vc, "Magazine");
		pose.translate(rel[0], rel[1], rel[2]);
		if(bullet[0] != 1) part(vc, "Bullet");
		pose.popPose();

		if(!isScoped(stack)) {
			part(vc, "IronSight");
		} else {
			part(consumer(carbine_scope_tex), "Scope");
		}

		if(hasBayonet(stack)) {
			part(consumer(carbine_bayonet_tex), "Bayonet");
		}

		pose.pushPose();
		pose.translate(0, 1, 8);
		glRotate(pose, 90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
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
		double scale = 1.375D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0, 2);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(hasBayonet(stack)) {
			double scale = 1.1875D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(1.5, 0, 0);
		} else {
			double scale = 1.375D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(-0.5, 0, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0, -1.75);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer(carbine_tex);
		part(vc, "Gun");
		part(vc, "Slide");
		part(vc, "Magazine");
		if(!isScoped(stack)) {
			part(vc, "IronSight");
		} else {
			part(consumer(carbine_scope_tex), "Scope");
		}
		if(hasBayonet(stack)) {
			part(consumer(carbine_bayonet_tex), "Bayonet");
		}

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
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}

	public boolean hasBayonet(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XFactory762mm.ID_CARBINE_BAYONET);
	}
}

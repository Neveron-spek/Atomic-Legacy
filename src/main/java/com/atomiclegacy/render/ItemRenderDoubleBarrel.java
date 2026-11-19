package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactory12ga;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderDoubleBarrel. */
public class ItemRenderDoubleBarrel extends ItemRenderWeaponBase {

	/** ResourceManager.double_barrel (models/weapons/sacred_dragon.obj) */
	static final LegacyEntityObjMesh double_barrel = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/sacred_dragon.obj"));
	public static final ResourceLocation double_barrel_tex = AtomicLegacy.id("textures/models/weapons/double_barrel.png");
	public static final ResourceLocation double_barrel_sacred_dragon_tex = AtomicLegacy.id("textures/models/weapons/double_barrel_sacred_dragon.png");

	protected final ResourceLocation texture;

	public ItemRenderDoubleBarrel(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1.25F * offset, -1F * offset, 2F * offset,
				0, -2 / 8D, 1);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(texture));
	}

	private void part(VertexConsumer vc, String name) {
		double_barrel.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] shells = HbmAnimations.getRelevantTransformation("SHELLS");
		double[] shellFlip = HbmAnimations.getRelevantTransformation("SHELL_FLIP");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] buckle = HbmAnimations.getRelevantTransformation("BUCKLE");
		double[] no_ammo = HbmAnimations.getRelevantTransformation("NO_AMMO");

		pose.translate(recoil[0] * 3, recoil[1], recoil[2]);
		glRotate(pose, recoil[2] * 10, 1, 0, 0);

		pose.translate(0, 0, -4);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, -4);
		glRotate(pose, turn[1], 0, 1, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, -4);
		glRotate(pose, lift[0], -1, 0, 0);
		pose.translate(0, 0, 4);

		part(vc, "Stock");

		pose.pushPose();

		pose.translate(0, -0.4375, -0.875);
		glRotate(pose, barrel[0], 1, 0, 0);
		pose.translate(0, 0.4375, 0.875);

		part(vc, "BarrelShort");
		if(!isSawedOff(stack)) part(vc, "Barrel");

		pose.pushPose();
		pose.translate(0.75, 0, -0.6875);
		glRotate(pose, buckle[1], 0, 1, 0);
		pose.translate(-0.75, 0, 0.6875);
		part(vc, "Buckle");
		pose.popPose();

		pose.pushPose();
		pose.translate(-0.3125, 0.3125, 0);
		glRotate(pose, lever[2], 0, 0, 1);
		pose.translate(0.3125, -0.3125, 0);
		part(vc, "Lever");
		pose.popPose();

		if(no_ammo[0] == 0) {
			pose.pushPose();
			pose.translate(shells[0], shells[1], shells[2]);
			pose.translate(0, 0, -1);
			glRotate(pose, shellFlip[0], 1, 0, 0);
			pose.translate(0, 0, 1);
			part(vc, "Shells");
			pose.popPose();
		}

		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(2, 2, 2);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(isSawedOff(stack)) {
			double scale = 2D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(-2, 0.5, 0);
		} else {
			double scale = 1.375D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(0, 0.5, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		VertexConsumer vc = consumer();
		part(vc, "Stock");
		part(vc, "BarrelShort");
		if(!isSawedOff(stack)) part(vc, "Barrel");
		part(vc, "Buckle");
		part(vc, "Lever");
		part(vc, "Shells");

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
			pose.scale(2, 2, 2);
			this.renderMuzzleFlash(shot, 75, 5);
			pose.popPose();
		}
	}

	public boolean isSawedOff(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get() || XWeaponModManager.hasUpgrade(stack, 0, XFactory12ga.ID_SAWED_OFF);
	}
}

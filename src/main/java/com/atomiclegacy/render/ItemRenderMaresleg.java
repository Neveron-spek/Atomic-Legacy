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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMaresleg. */
public class ItemRenderMaresleg extends ItemRenderWeaponBase {

	/** ResourceManager.maresleg (models/weapons/maresleg.obj) */
	static final LegacyEntityObjMesh maresleg = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/maresleg.obj"));
	public static final ResourceLocation maresleg_tex = AtomicLegacy.id("textures/models/weapons/maresleg.png");
	public static final ResourceLocation maresleg_broken_tex = AtomicLegacy.id("textures/models/weapons/maresleg_broken.png");

	public final ResourceLocation texture;

	public ItemRenderMaresleg(ResourceLocation texture) {
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
				0, -3.875 / 8D, 1);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(texture));
	}

	// [fix: crash] smoke/flash switch the immediate buffer; fetch the cutout consumer per part
	private void part(VertexConsumer vc, String name) {
		maresleg.renderGroup(name, pose, consumer(), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		boolean shortened = getShort(stack);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] flip = HbmAnimations.getRelevantTransformation("FLIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
		double[] flag = HbmAnimations.getRelevantTransformation("FLAG");

		pose.translate(recoil[0] * 2, recoil[1], recoil[2]);
		glRotate(pose, recoil[2] * 5, 1, 0, 0);
		glRotate(pose, turn[2], 0, 0, 1);

		pose.translate(0, 0, -4);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, -4);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, -2);
		glRotate(pose, flip[0], -1, 0, 0);
		pose.translate(0, 0, 2);

		pose.pushPose();
		pose.translate(0, 1, shortened ? 3.75 : 8);
		glRotate(pose, turn[2], 0, 0, -1);
		glRotate(pose, flip[0], 1, 0, 0);
		glRotate(pose, 90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		pose.popPose();

		part(vc, "Gun");
		if(!shortened) {
			part(vc, "Stock");
			part(vc, "Barrel");
		}

		pose.pushPose();
		pose.translate(0, 0.125, -2.875);
		glRotate(pose, lever[0], 1, 0, 0);
		pose.translate(0, -0.125, 2.875);
		part(vc, "Lever");
		pose.popPose();

		pose.pushPose();
		pose.translate(shell[0], shell[1] - 0.75, shell[2]);
		part(vc, "Shell");
		pose.popPose();

		if(flag[0] != 0) {
			pose.pushPose();
			pose.translate(0, -0.5, 0);
			part(vc, "Shell");
			pose.popPose();
		}

		pose.pushPose();
		pose.translate(0, 1, shortened ? 3.75 : 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.25, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);

		if(getShort(stack)) {
			double scale = 2.5D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(-1, 0, 0);
		} else {
			double scale = 1.4375D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, 45, 0, 1, 0);
			pose.translate(-0.5, 0.5, 0);
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
		part(vc, "Gun");
		part(vc, "Lever");
		boolean shortened = getShort(stack);
		if(!shortened) {
			part(vc, "Stock");
			part(vc, "Barrel");
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
			pose.translate(0, 1, shortened ? 3.75 : 8);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 5);
			pose.popPose();
		}
	}

	public boolean getShort(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_MARESLEG_BROKEN.get() || XWeaponModManager.hasUpgrade(stack, 0, XFactory12ga.ID_SAWED_OFF);
	}
}

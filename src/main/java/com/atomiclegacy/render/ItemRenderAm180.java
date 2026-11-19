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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderAm180. */
public class ItemRenderAm180 extends ItemRenderWeaponBase {

	/** ResourceManager.am180 (models/weapons/am180.obj) */
	static final LegacyEntityObjMesh am180 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/am180.obj"));
	public static final ResourceLocation am180_tex = AtomicLegacy.id("textures/models/weapons/am180.png");

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
				-1F * offset, -1F * offset, 1F * offset,
				0, -4.1875 / 8D, 0.25);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(am180_tex));
	}

	private void part(VertexConsumer vc, String name) {
		am180.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.1875D;
		pose.scale((float) scale, (float) scale, (float) scale);

		boolean silenced = this.hasSilencer(stack);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] magazine = HbmAnimations.getRelevantTransformation("MAG");
		double[] magTurn = HbmAnimations.getRelevantTransformation("MAGTURN");
		double[] magSpin = HbmAnimations.getRelevantTransformation("MAGSPIN");
		double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");

		pose.translate(0, -2, -6);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 6);

		glRotate(pose, turn[2], 0, 0, 1);

		pose.translate(0, 0, recoil[2]);

		HbmAnimations.applyRelevantTransformation(pose, "Gun");
		part(vc, "Gun");
		if(silenced) part(vc, "Silencer");

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Trigger");
		part(vc, "Trigger");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, bolt[2]);
		HbmAnimations.applyRelevantTransformation(pose, "Bolt");
		part(vc, "Bolt");
		pose.popPose();

		pose.pushPose();
		pose.translate(magazine[0], magazine[1], magazine[2]);

		pose.translate(0, 2.0625, 3.75);
		glRotate(pose, magTurn[0], 1, 0, 0);
		glRotate(pose, magTurn[2], 0, 0, 1);
		pose.translate(0, -2.0625, -3.75);

		pose.translate(0, 2.3125, 1.5);
		glRotate(pose, magSpin[0], 1, 0, 0);
		pose.translate(0, -2.3125, -1.5);

		HbmAnimations.applyRelevantTransformation(pose, "Mag");

		pose.pushPose();
		// MagazineFullReload#getAmount ignores the inventory argument (MainRegistry.proxy.me().inventory)
		int mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
		pose.translate(0, 0, 1.5);
		glRotate(pose, mag / 59D * 360D, 0, -1, 0);
		pose.translate(0, 0, -1.5);
		part(vc, "Mag");
		pose.popPose();

		part(vc, "MagPlate");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.875, silenced ? 17 : 13);
		glRotate(pose, turn[2], 0, 0, -1);
		glRotate(pose, 90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.875, silenced ? 16.75 : 12);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		double flashScale = silenced ? 0.5 : 0.75;
		pose.scale((float) flashScale, (float) flashScale, (float) flashScale);
		this.renderMuzzleFlash(gun.lastShot[0], silenced ? 75 : 50, silenced ? 5 : 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -0.5, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(1.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0, -2);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		if(this.hasSilencer(stack)) part(vc, "Silencer");
		part(vc, "Trigger");
		part(vc, "Bolt");
		part(vc, "Mag");
		part(vc, "MagPlate");

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
			boolean silenced = this.hasSilencer(stack);

			pose.pushPose();
			pose.translate(0, 1.875, silenced ? 16.75 : 12);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			double flashScale = silenced ? 0.5 : 0.75;
			pose.scale((float) flashScale, (float) flashScale, (float) flashScale);
			this.renderMuzzleFlash(shot, silenced ? 75 : 50, silenced ? 5 : 7.5);
			pose.popPose();
		}
	}

	public boolean hasSilencer(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}
}

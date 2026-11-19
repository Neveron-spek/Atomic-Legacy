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
 * 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderG3 (gun_g3 and gun_g3_zebra, which is always
 * silenced and scoped).
 */
public class ItemRenderG3 extends ItemRenderWeaponBase {

	/** ResourceManager.g3 (models/weapons/g3.obj) */
	public static final LegacyEntityObjMesh g3 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/g3.obj"));
	public static final ResourceLocation g3_tex = AtomicLegacy.id("textures/models/weapons/g3.png");
	public static final ResourceLocation g3_green_tex = AtomicLegacy.id("textures/models/weapons/g3_polymer_green.png");
	public static final ResourceLocation g3_black_tex = AtomicLegacy.id("textures/models/weapons/g3_polymer_black.png");
	public static final ResourceLocation g3_zebra_tex = AtomicLegacy.id("textures/models/weapons/g3_zebra.png");
	public static final ResourceLocation g3_attachments = AtomicLegacy.id("textures/models/weapons/g3_attachments.png");

	public ResourceLocation texture;

	public ItemRenderG3(ResourceLocation texture) {
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
		pose.translate(0, 0, 0.875);

		boolean isScoped = this.isScoped(stack);
		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.25F * offset, -1F * offset, 2.75F * offset,
			0, isScoped ? (-5.53125 / 8D) : (-3.5625 / 8D), isScoped ? 1.46875 : 1.75);
	}

	private VertexConsumer consumer(ResourceLocation tex) {
		return buffers.getBuffer(RenderType.entityCutout(tex));
	}

	private void part(VertexConsumer vc, String name) {
		g3.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		boolean isScoped = this.isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer(getTexture(stack));
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");
		double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
		double[] plug = HbmAnimations.getRelevantTransformation("PLUG");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");

		pose.translate(0, -2, -6);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 6);

		pose.translate(0, 0, -4);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Rifle");
		if(hasStock(stack)) part(vc, "Stock");
		boolean silenced = hasSilencer(stack);
		if(!silenced) part(vc, "Flash_Hider");
		part(vc, "Trigger");

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		pose.translate(0, -1.75, -0.5);
		glRotate(pose, speen[2], 0, 0, 1);
		glRotate(pose, speen[1], 0, 1, 0);
		pose.translate(0, 1.75, 0.5);
		part(vc, "Magazine");
		if(bullet[0] == 0) part(vc, "Bullet");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, bolt[2]);
		part(vc, "Guide_And_Bolt");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.625, plug[2]);
		glRotate(pose, handle[2], 0, 0, 1);
		pose.translate(0, -0.625, 0);
		part(vc, "Plug");

		pose.translate(0, 0.625, 5.25);
		glRotate(pose, 22.5, 0, 0, 1);
		glRotate(pose, handle[1], 0, 1, 0);
		glRotate(pose, -22.5, 0, 0, 1);
		pose.translate(0, -0.625, -5.25);
		part(vc, "Handle");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, -0.875, -3.5);
		glRotate(pose, -30 * (1 - ItemGunBaseNT.getMode(stack, 0)), 1, 0, 0);
		pose.translate(0, 0.875, 3.5);
		part(vc, "Selector");
		pose.popPose();

		if(silenced || isScoped) {
			VertexConsumer att = consumer(g3_attachments);
			if(silenced) part(att, "Silencer");
			if(isScoped) part(att, "Scope");
		}

		if(!silenced) {
			double smokeScale = 0.75;

			pose.pushPose();
			pose.translate(0, 0, 13);
			glRotate(pose, 90, 0, 1, 0);
			pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 0, 12);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, -25 + gun.shotRand * 10, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(gun.lastShot[0], 75, 10);
			pose.popPose();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 2, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(hasStock(stack)) {
			double scale = 0.875D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, hasSilencer(stack) ? 50 : 45, 0, 1, 0);
			pose.translate(hasSilencer(stack) ? 0.75 : -0.5, 0.5, 0);
		} else {
			double scale = 1.125D;
			pose.scale((float) scale, (float) scale, (float) scale);
			glRotate(pose, 25, 1, 0, 0);
			glRotate(pose, hasSilencer(stack) ? 55 : 45, 0, 1, 0); //preserves proportions whilst limiting size
			pose.translate(2.5, 0.5, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0.5, -0.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		boolean silenced = hasSilencer(stack);
		boolean isScoped = this.isScoped(stack);

		VertexConsumer vc = consumer(getTexture(stack));
		part(vc, "Rifle");
		if(hasStock(stack)) part(vc, "Stock");
		part(vc, "Magazine");
		if(!silenced) part(vc, "Flash_Hider");
		part(vc, "Guide_And_Bolt");
		part(vc, "Handle");
		part(vc, "Trigger");

		pose.pushPose();
		pose.translate(0, -0.875, -3.5);
		glRotate(pose, -30, 1, 0, 0);
		pose.translate(0, 0.875, 3.5);
		part(vc, "Selector");
		pose.popPose();

		if(silenced || isScoped) {
			VertexConsumer att = consumer(g3_attachments);
			if(silenced) part(att, "Silencer");
			if(isScoped) part(att, "Scope");
		}
		//third-person muzzle flashes
		if(type == LegacyRenderType.EQUIPPED && !silenced && ent != null) {
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
			pose.translate(0, 0, 12);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, -25 + shotRand * 10, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 75, 10);
			pose.popPose();
		}
	}

	public boolean hasStock(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK);
	}

	public boolean hasSilencer(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_G3_ZEBRA.get() || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}

	public boolean isScoped(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_G3_ZEBRA.get() || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}

	public ResourceLocation getTexture(ItemStack stack) {
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_FURNITURE_GREEN)) return g3_green_tex;
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_FURNITURE_BLACK)) return g3_black_tex;
		return texture;
	}
}

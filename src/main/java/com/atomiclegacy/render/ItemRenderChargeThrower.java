package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryTool;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineFullReload;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F3] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderChargeThrower. */
public class ItemRenderChargeThrower extends ItemRenderWeaponBase {

	/** ResourceManager.charge_thrower (models/weapons/charge_thrower.obj) */
	static final LegacyEntityObjMesh charge_thrower = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/charge_thrower.obj"));
	public static final ResourceLocation charge_thrower_tex = AtomicLegacy.id("textures/models/weapons/charge_thrower.png");
	public static final ResourceLocation charge_thrower_hook_tex = AtomicLegacy.id("textures/models/weapons/charge_thrower_hook.png");
	public static final ResourceLocation charge_thrower_mortar_tex = AtomicLegacy.id("textures/models/weapons/charge_thrower_mortar.png");
	public static final ResourceLocation charge_thrower_rocket_tex = AtomicLegacy.id("textures/models/weapons/charge_thrower_rocket.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		float zoom = 0.5F;

		if(isScoped(stack)) standardAimingTransform(pose, stack,
				-1.5F * offset, -1.25F * offset, 3.5F * offset,
				-0.15625, -6.5 / 8D, 1.6875);
		else standardAimingTransform(pose, stack,
				-1.5F * offset, -1.25F * offset, 3.5F * offset,
				-1.5F * zoom, -1.25F * zoom, 3.5F * zoom);
	}

	private void part(ResourceLocation tex, String name) {
		charge_thrower.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		boolean usingScope = this.isScoped(stack) && ItemGunBaseNT.aimingProgress == 1 && ItemGunBaseNT.prevAimingProgress == 1;
		MagazineFullReload mag = (MagazineFullReload) gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

		if(usingScope) {
			double scale = 3.5D;
			pose.scale((float) scale, (float) scale, (float) scale);
			pose.translate(-0.5, -1.5, -4);
		} else {
			double scale = 0.5D;
			pose.scale((float) scale, (float) scale, (float) scale);
		}

		boolean reloading = HbmAnimations.getRelevantAnim(0) != null && HbmAnimations.getRelevantAnim(0).animation.getBus("AMMO") != null;
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] raise = HbmAnimations.getRelevantTransformation("RAISE");
		double[] ammo = HbmAnimations.getRelevantTransformation("AMMO");
		double[] twist = HbmAnimations.getRelevantTransformation("TWIST");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");

		pose.translate(0, 0, -7);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, 0, 7);

		pose.translate(0, -7, 4);
		glRotate(pose, raise[0], 1, 0, 0);
		pose.translate(0, 7, -4);

		pose.translate(recoil[0], recoil[1], recoil[2]);

		pose.translate(0, 0, -2);
		glRotate(pose, turn[1], 0, 1, 0);
		pose.translate(0, 0, 2);
		pose.translate(0, -1, 0);
		glRotate(pose, roll[2], 0, 0, 1);
		pose.translate(0, 1, 0);

		part(charge_thrower_tex, "Gun");
		if(isScoped(stack) && !usingScope) part(charge_thrower_tex, "Scope");

		if(mag.getAmount(stack, null) > 0 || reloading) {

			pose.translate(ammo[0], ammo[1], ammo[2]);
			glRotate(pose, twist[2], 0, 0, 1);

			renderAmmo(stack, mag);
			//Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.charge_thrower_rocket_tex);
			//ResourceManager.charge_thrower.renderPart("Rocket");
		}
	}

	private void renderAmmo(ItemStack stack, MagazineFullReload mag) {
		if(mag.getType(stack, null) == XFactoryTool.ct_hook) {
			part(charge_thrower_hook_tex, "Hook");
		}
		if(mag.getType(stack, null) == XFactoryTool.ct_mortar) {
			part(charge_thrower_mortar_tex, "Mortar");
		}
		if(mag.getType(stack, null) == XFactoryTool.ct_mortar_charge) {
			part(charge_thrower_mortar_tex, "Mortar");
			part(charge_thrower_mortar_tex, "Oomph");
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0.75, 1, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 0, -0.625);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0, -1);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity entity) {
		part(charge_thrower_tex, "Gun");
		if(isScoped(stack)) part(charge_thrower_tex, "Scope");

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		MagazineFullReload mag = (MagazineFullReload) gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

		if(mag.getAmount(stack, null) > 0) {
			renderAmmo(stack, mag);
		}
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}
}

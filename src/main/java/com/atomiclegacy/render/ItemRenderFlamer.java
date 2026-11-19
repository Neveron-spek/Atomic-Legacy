package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderFlamer (gun_flamer, gun_flamer_topaz, gun_flamer_daybreaker). */
public class ItemRenderFlamer extends ItemRenderWeaponBase {

	/** ResourceManager.flamethrower (models/weapons/flamethrower.obj: Gun, Tank, Gauge, HeatShield) */
	static final LegacyEntityObjMesh flamethrower = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/flamethrower.obj"));
	public static final ResourceLocation flamethrower_tex = AtomicLegacy.id("textures/models/weapons/flamethrower.png");
	public static final ResourceLocation flamethrower_topaz_tex = AtomicLegacy.id("textures/models/weapons/flamethrower_topaz.png");
	public static final ResourceLocation flamethrower_daybreaker_tex = AtomicLegacy.id("textures/models/weapons/flamethrower_daybreaker.png");

	public ResourceLocation texture;

	public ItemRenderFlamer(ResourceLocation texture) {
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
				-1.5F * offset, -1.5F * offset, 2.75F * offset,
				0, -4.625 / 8D, 0.25);
	}

	private void part(String name) {
		flamethrower.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

		pose.translate(0, 2, -6);
		glRotate(pose, -equip[0], 1, 0, 0);
		pose.translate(0, -2, 6);

		pose.translate(0, 1, 0);
		glRotate(pose, rotate[2], 0, 0, 1);
		pose.translate(0, -1, 0);

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Gun");
		part("Gun");
		if(hasShield(stack)) part("HeatShield");
		pose.popPose();

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Tank");
		part("Tank");
		pose.popPose();

		pose.pushPose();
		HbmAnimations.applyRelevantTransformation(pose, "Gauge");
		pose.translate(1.25, 1.25, 0);
		IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		var player = Minecraft.getInstance().player;
		glRotate(pose, -135 + (mag.getAmount(stack, player != null ? player.getInventory() : null) * 270D / mag.getCapacity(stack)), 0, 0, 1);
		pose.translate(-1.25, -1.25, 0);
		part("Gauge");
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -3, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-1, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0, 1);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		part("Gun");
		part("Tank");
		part("Gauge");
		if(hasShield(stack)) part("HeatShield");
	}

	public boolean hasShield(ItemStack stack) {
		return stack.getItem() == HbmItems.GUN_FLAMER_DAYBREAKER.get();
	}
}

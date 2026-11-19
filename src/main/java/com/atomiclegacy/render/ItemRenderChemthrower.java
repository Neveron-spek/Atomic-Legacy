package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderChemthrower. */
public class ItemRenderChemthrower extends ItemRenderWeaponBase {

	/** ResourceManager.chemthrower (models/weapons/chemthrower.obj: Gun, Hose, Nozzle, Gauge) */
	static final LegacyEntityObjMesh chemthrower = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/chemthrower.obj"));
	public static final ResourceLocation chemthrower_tex = AtomicLegacy.id("textures/models/weapons/chemthrower.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-2.5F * offset, -2.5F * offset, 2.5F * offset,
				0, -4.375 / 8D, 1);
	}

	private void part(String name) {
		chemthrower.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(chemthrower_tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

		pose.translate(0, -2, -4);
		glRotate(pose, equip[0], -1, 0, 0);
		pose.translate(0, 2, 4);

		glRotate(pose, 90, 0, 1, 0);
		part("Gun");
		part("Hose");
		part("Nozzle");

		pose.translate(0, 0.875, 1.75);
		IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		var player = Minecraft.getInstance().player;
		double d = (double) mag.getAmount(stack, player != null ? player.getInventory() : null) / (double) mag.getCapacity(stack);
		glRotate(pose, 135 - d * 270, 1, 0, 0);
		pose.translate(0, -0.875, -1.75);

		part("Gauge");
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -2.5, 0.5);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0.875, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, -0.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		glRotate(pose, 90, 0, 1, 0);
		part("Gun");
		part("Hose");
		part("Nozzle");
		part("Gauge");
	}
}

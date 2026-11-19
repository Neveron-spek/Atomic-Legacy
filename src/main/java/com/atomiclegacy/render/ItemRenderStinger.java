package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch F4] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderStinger. */
public class ItemRenderStinger extends ItemRenderWeaponBase {

	/** ResourceManager.stinger (models/weapons/stinger.obj) */
	static final LegacyEntityObjMesh stinger = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/stinger.obj"));
	public static final ResourceLocation stinger_tex = AtomicLegacy.id("textures/models/weapons/stinger.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.5F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-3.75F * offset, -9F * offset, -3.5F * offset,
				-2.625F * offset, -6.5, -8.5F);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(stinger_tex));
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		if(ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 1.5D;
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

		pose.pushPose();
		glRotate(pose, 180, 0, 1, 0);
		stinger.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);
		pose.popPose();

		pose.pushPose();
		pose.translate(rocket[0], rocket[1] + 3.5, rocket[2] - 3);
		ItemRenderPanzerschreck.panzerschreck.renderGroup("Rocket", pose, buffers.getBuffer(RenderType.entityCutout(ItemRenderPanzerschreck.panzerschreck_tex)), light, overlay, 0xFFFFFFFF);

		// "Not accurate" label on the rocket, unlit (lightmap 240/240), no culling
		pose.pushPose();
		String label = "Not accurate";
		Font font = Minecraft.getInstance().font;
		float f3 = 0.04F;
		pose.translate(0.025F, -0.5F, (font.width(label) / 2) * f3 - 3);
		pose.scale(f3, -f3, f3);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 45, -1, 0, 0);
		font.drawInBatch(label, 0, 0, 0xFFFF0000, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		pose.popPose();

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
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -2.5, -3.5);
		glRotate(pose, 180, 0, 1, 0);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.0625D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 225, 0, 1, 0);
		pose.translate(0.25, -2.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, -90, 0, 1, 0);
		pose.translate(0, -4, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		stinger.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);

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
			pose.translate(0, 3.5, -10.3795);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 150, 10);
			pose.popPose();
		}
	}
}

package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunNI4NI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * [weapons batch F2] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderNI4NI. The untextured coin indicators
 * (GL_TEXTURE_2D disabled, fullbright, GL lighting) use the vanilla white texture with the entity solid shader.
 */
public class ItemRenderNI4NI extends ItemRenderWeaponBase {

	/** ResourceManager.n_i_4_n_i (models/weapons/n_i_4_n_i.obj) */
	static final LegacyEntityObjMesh n_i_4_n_i = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/n_i_4_n_i.obj"));
	public static final ResourceLocation n_i_4_n_i_tex = AtomicLegacy.id("textures/models/weapons/n_i_4_n_i.png");
	public static final ResourceLocation n_i_4_n_i_greyscale_tex = AtomicLegacy.id("textures/models/weapons/n_i_4_n_i_greyscale.png");
	private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.0F * offset, -1F * offset, 1F * offset,
				0, -5 / 8D, 0.125);
	}

	private ResourceLocation tex;

	private void part(String name, int color, boolean fullbright) {
		n_i_4_n_i.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(tex)), fullbright ? LightTexture.FULL_BRIGHT : light, overlay, 0xFF000000 | (color & 0xFFFFFF));
	}

	/** glDisable(GL_TEXTURE_2D) + glColor3f with RenderArcFurnace.fullbright(true). */
	private void coin(String name, int color) {
		n_i_4_n_i.renderGroup(name, pose, buffers.getBuffer(RenderType.entitySolid(WHITE)), LightTexture.FULL_BRIGHT, overlay, 0xFF000000 | (color & 0xFFFFFF));
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		int[] color = ItemGunNI4NI.getColors(stack);
		int dark = 0xffffff;
		int light = 0xffffff;
		int grip = 0xffffff;
		if(color != null) {
			tex = n_i_4_n_i_greyscale_tex;
			dark = color[0];
			light = color[1];
			grip = color[2];
		} else {
			tex = n_i_4_n_i_tex;
		}

		double scale = 0.3125D;
		pose.scale((float) scale, (float) scale, (float) scale);
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] drum = HbmAnimations.getRelevantTransformation("DRUM");

		pose.translate(0, 0, -2.25);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 0, 2.25);

		pose.translate(0, -1, -6);
		glRotate(pose, recoil[0], 1, 0, 0);
		pose.translate(0, 1, 6);

		pose.pushPose();
		part("FrameDark", dark, false);
		part("Grip", grip, false);
		part("FrameLight", light, false);

		pose.pushPose();
		pose.translate(0, 1.1875D, 0);
		glRotate(pose, drum[2], 0, 0, 1);
		pose.translate(0, -1.1875D, 0);
		part("Cylinder", light, false);
		part("CylinderHighlights", 0xffffff, true);
		pose.popPose();

		part("Barrel", 0xffffff, true);

		int coinCount = ItemGunNI4NI.getCoinCount(stack);
		if(coinCount > 3) coin("Coin1", coinCount > 7 ? 0xFFFF00 : 0x00FF00);
		if(coinCount > 2) coin("Coin2", coinCount > 6 ? 0xFFFF00 : 0x00FF00);
		if(coinCount > 1) coin("Coin3", coinCount > 5 ? 0xFFFF00 : 0x00FF00);
		if(coinCount > 0) coin("Coin4", coinCount > 4 ? 0xFFFF00 : 0x00FF00);

		pose.pushPose();
		pose.translate(0, 0.75, 4);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.125F, 0.125F, 0.125F);
		ItemRenderLaserPistol.renderLaserFlash(pose, buffers, gun.lastShot[0], 75, 7.5, 0xFFFFFF);
		pose.popPose();

		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, 0.25, 3);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -15D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		int[] color = ItemGunNI4NI.getColors(stack);
		int dark = 0xffffff;
		int light = 0xffffff;
		int grip = 0xffffff;
		if(color != null) {
			tex = n_i_4_n_i_greyscale_tex;
			dark = color[0];
			light = color[1];
			grip = color[2];
		} else {
			tex = n_i_4_n_i_tex;
		}

		part("FrameLight", light, false);
		part("Cylinder", light, false);
		part("Grip", grip, false);
		part("FrameDark", dark, false);
		part("CylinderHighlights", 0xffffff, true);
		part("Barrel", 0xffffff, true);
		coin("Coin1", 0x00FF00);
		coin("Coin2", 0x00FF00);
		coin("Coin3", 0x00FF00);
		coin("Coin4", 0x00FF00);

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
			pose.translate(0, 0.75, 4);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.125F, 0.125F, 0.125F);
			ItemRenderLaserPistol.renderLaserFlash(pose, buffers, shot, 75, 7.5, 0xFFFFFF);
			pose.popPose();
		}
	}
}

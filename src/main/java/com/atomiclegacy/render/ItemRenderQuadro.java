package com.atomiclegacy.render;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderQuadro. */
public class ItemRenderQuadro extends ItemRenderWeaponBase {

	/** ResourceManager.quadro (models/weapons/quadro.obj) */
	static final LegacyEntityObjMesh quadro = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/quadro.obj"));
	public static final ResourceLocation quadro_tex = AtomicLegacy.id("textures/models/weapons/quadro.png");
	public static final ResourceLocation quadro_rocket_tex = AtomicLegacy.id("textures/models/weapons/quadro_rocket.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-2.5F * offset, -3.5F * offset, 2.5F * offset,
				-1.5F * offset, -3F * offset, 2.5F * offset);
	}

	protected static String label = ">> <<";

	private void part(ResourceLocation tex, String name) {
		quadro.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(tex)), light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadPush = HbmAnimations.getRelevantTransformation("RELOAD_PUSH");
		double[] reloadRotate = HbmAnimations.getRelevantTransformation("RELOAD_ROTATE");

		pose.translate(0, -1, -1);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 1);

		pose.translate(0, 0, recoil[2]);

		pose.translate(0, -1, -1);
		glRotate(pose, reloadRotate[2], 1, 0, 0);
		pose.translate(0, 1, 1);

		part(quadro_tex, "Launcher");

		pose.pushPose();

		pose.translate(0, -1, 0);
		pose.translate(0, 3, 0);
		glRotate(pose, reloadPush[1] * 30, 1, 0, 0);
		pose.translate(0, -3, 0);
		pose.translate(0, 0, reloadPush[0] * 3);
		part(quadro_rocket_tex, "Rockets");
		pose.popPose();

		if(ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F) {

			// unlit, full-bright, culling off, alpha blending: FontRenderer#drawString in the world
			pose.pushPose();
			Font font = Minecraft.getInstance().font;
			float f3 = 0.04F;
			pose.translate(-0.375F, 2.25F, 0.875F);
			glRotate(pose, 180D + (System.currentTimeMillis() / 2) % 360D, 0, -1, 0);
			pose.translate(-(font.width(label) / 2) * f3, 0, 0);
			pose.scale(f3, -f3, f3);
			drawTwoSided(font, label, new Color(0F, 1F, 1F).getRGB(), pose, buffers);
			pose.popPose();
		}

		pose.pushPose();
		pose.translate(-1, 0.75, 6.5);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		pose.popPose();
	}

	/** FontRenderer#drawString with GL_CULL_FACE disabled: the planar glyphs again with reversed winding (z mirrored). */
	static void drawTwoSided(Font font, String text, int color, PoseStack pose, MultiBufferSource buffers) {
		font.drawInBatch(text, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		pose.pushPose();
		pose.scale(1F, 1F, -1F);
		font.drawInBatch(text, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -0.5, -0.25);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 4.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, -1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -30D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1.125, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		part(quadro_tex, "Launcher");

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

			pose.pushPose(); // 1.7.10 TODO: adjust in third person, flash is too far forward/ large
			pose.translate(0, 0.75, 2);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 150, 7.5);
			pose.popPose();
		}
	}
}

package com.atomiclegacy.render;

import java.awt.Color;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** [weapons batch E1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMissileLauncher. */
public class ItemRenderMissileLauncher extends ItemRenderWeaponBase {

	/** ResourceManager.missile_launcher (models/weapons/missile_launcher.obj) */
	static final LegacyEntityObjMesh missile_launcher = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/missile_launcher.obj"));
	public static final ResourceLocation missile_launcher_tex = AtomicLegacy.id("textures/models/weapons/missile_launcher.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.5F * offset, -1.25F * offset, 0.5F * offset,
				-1F * offset, -1.25F * offset, 0F * offset);
	}

	protected static String label = "AUTO";

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(missile_launcher_tex));
	}

	private void part(VertexConsumer vc, String name) {
		missile_launcher.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		Player player = Minecraft.getInstance().player;
		VertexConsumer vc = consumer();
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
		double[] open = HbmAnimations.getRelevantTransformation("OPEN");
		double[] missile = HbmAnimations.getRelevantTransformation("MISSILE");

		pose.translate(0, -2, -2);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 2);

		part(vc, "Launcher");

		pose.pushPose();

		pose.translate(0, 0.25, 1.6875);
		glRotate(pose, open[0], 1, 0, 0);
		pose.translate(0, -0.25, -1.6875);

		part(vc, "Front");

		pose.pushPose();
		pose.translate(0, 0, barrel[2]);
		part(vc, "Barrel");
		pose.popPose();

		pose.pushPose();
		pose.translate(missile[0], missile[1], missile[2]);
		part(vc, "Missile");
		pose.popPose();

		pose.popPose();

		if(ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F) {

			// unlit, full-bright, culling off, alpha blending: FontRenderer#drawString in the world
			pose.pushPose();
			Font font = Minecraft.getInstance().font;
			float f3 = 0.04F;
			pose.translate(0.9375F, 2.25F, -0.5625F + (font.width(label) / 2) * f3);
			pose.scale(f3, -f3, f3);
			glRotate(pose, 90D, 0, 1, 0);
			float variance = 0.7F + player.getRandom().nextFloat() * 0.3F;
			ItemRenderQuadro.drawTwoSided(font, label, new Color(variance, 0F, 0F).getRGB(), pose, buffers);
			pose.popPose();
		}

		pose.pushPose();
		pose.translate(0, 1, 6.75);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, gun.shotRand * 90, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, -0.5, -2);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();

		part(vc, "Launcher");
		part(vc, "Barrel");
		part(vc, "Front");
		// MagazineSingleTypeBase#getAmount ignores the inventory argument
		if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) part(vc, "Missile");

		if(type == LegacyRenderType.EQUIPPED && ent != null) {
			long shot;
			double shotRand = 0;
			if(ent == Minecraft.getInstance().player) {
				shot = gun.lastShot[0];
				shotRand = gun.shotRand;
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			pose.pushPose();
			pose.translate(0, 1, 6.75);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, shotRand * 90, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}

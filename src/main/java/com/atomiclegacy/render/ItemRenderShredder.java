package com.atomiclegacy.render;

import java.awt.Color;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderShredder (gun_autoshotgun with shredder.png,
 * gun_autoshotgun_shredder with shredder_orig.png). [weapons batch D1]
 */
public class ItemRenderShredder extends ItemRenderWeaponBase {

	/** ResourceManager.shredder (models/weapons/shredder.obj) */
	static final LegacyEntityObjMesh shredder = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/shredder.obj"));
	public static final ResourceLocation shredder_tex = AtomicLegacy.id("textures/models/weapons/shredder.png");
	public static final ResourceLocation shredder_orig_tex = AtomicLegacy.id("textures/models/weapons/shredder_orig.png");

	protected ResourceLocation texture;

	public ItemRenderShredder(ResourceLocation texture) {
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
				-1.5F * offset, -1.25F * offset, 1.5F * offset,
			0, -6.25 / 8D, 0.5);
	}

	protected static String label = "[> <]";

	private void part(VertexConsumer vc, String name) {
		shredder.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		Player player = Minecraft.getInstance().player;
		double scale = 0.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");
		double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");

		pose.translate(0, -2, -6);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 2, 6);

		pose.translate(0, 0, -4);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, 0, 4);

		pose.translate(0, 0, recoil[2]);

		boolean sexy = stack.getItem() == HbmItems.GUN_AUTOSHOTGUN_SEXY.get();

		if(sexy || (ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F)) {

			// unlit, full-bright, culling off, alpha blending: FontRenderer#drawString in the world
			pose.pushPose();
			Font font = Minecraft.getInstance().font;
			float f3 = 0.04F;
			pose.translate((font.width(label) / 2) * f3, 3.25F, -1.75F);
			pose.scale(f3, -f3, f3);
			glRotate(pose, 180D, 0, 1, 0);
			float variance = 0.9F + player.getRandom().nextFloat() * 0.1F;
			font.drawInBatch(label, 0, 0, new Color(sexy ? variance : 0F, sexy ? 0F : variance, 0F).getRGB(), false,
					pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			pose.popPose();
		}

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(texture));

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		pose.translate(0, -1, -0.5);
		glRotate(pose, speen[0], 1, 0, 0);
		pose.translate(0, 1, 0.5);
		part(vc, "Magazine");
		pose.translate(0, -1, -0.5);
		glRotate(pose, cycle[2], 0, 0, 1);
		pose.translate(0, 1, 0.5);
		part(vc, "Shells");
		pose.popPose();

		double smokeScale = 0.75;

		pose.pushPose();
		pose.translate(0, 1, 7.5);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1, 7.5);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, gun.shotRand * 90, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(0, 0.5, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(-1.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0, 1.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		shredder.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);

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
			pose.translate(0, 1, 7.5);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, shotRand * 90, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();
		}
	}
}

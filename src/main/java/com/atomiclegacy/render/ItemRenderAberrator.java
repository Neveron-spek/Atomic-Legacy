package com.atomiclegacy.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * [weapons batch F4] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderAberrator: the gun parts (Gun, Sight, Magazine,
 * Bullet, Slide, Hammer), smoke, muzzle flash, the fireball plume and the spinning ring of sixteen golden sword sprites
 * that folds up with the aiming progress.
 */
public class ItemRenderAberrator extends ItemRenderWeaponBase {

	/** ResourceManager.aberrator (models/weapons/aberrator.obj), aberrator_tex */
	static final LegacyEntityObjMesh aberrator = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/aberrator.obj"));
	public static final ResourceLocation aberrator_tex = AtomicLegacy.id("textures/models/weapons/aberrator.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1);

		float offset = 0.8F;
		standardAimingTransform(pose, stack,
				-1.0F * offset, -1.25F * offset, 1.25F * offset,
				0, -5.25 / 8D, 0.125);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(aberrator_tex));
	}

	private void part(VertexConsumer vc, String name) {
		aberrator.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.25D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] rise = HbmAnimations.getRelevantTransformation("RISE");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] magroll = HbmAnimations.getRelevantTransformation("MAGROLL");
		double[] sight = HbmAnimations.getRelevantTransformation("SIGHT");

		pose.translate(0, rise[1], 0);

		pose.translate(0, 1, -2.25);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, -1, 2.25);

		pose.translate(0, -1, -4);
		glRotate(pose, recoil[0], 1, 0, 0);
		pose.translate(0, 1, 4);

		pose.translate(0, 1, 0);
		glRotate(pose, roll[2], 0, 0, 1);
		pose.translate(0, -1, 0);

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(0, 2.4375, -1.9375);
		glRotate(pose, sight[0], 1, 0, 0);
		pose.translate(0, -2.4375, 1.9375);
		part(vc, "Sight");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);

		pose.translate(0, 1, 0);
		glRotate(pose, magroll[2], 0, 0, 1);
		pose.translate(0, -1, 0);

		part(vc, "Magazine");
		pose.translate(bullet[0], bullet[1], bullet[2]);
		part(vc, "Bullet");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, slide[2]);
		part(vc, "Slide");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 1.25, -3.625);
		glRotate(pose, -45 + hammer[0], 1, 0, 0);
		pose.translate(0, -1.25, 3.625);
		part(vc, "Hammer");
		pose.popPose();

		double smokeScale = 0.5;

		pose.pushPose();
		pose.translate(0, 2, 4);
		glRotate(pose, recoil[0], -1, 0, 0);
		glRotate(pose, roll[2], 0, 0, -1);
		glRotate(pose, 90, 0, 1, 0);
		pose.scale((float) smokeScale, (float) smokeScale, (float) smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 2, 4);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		pose.scale(0.75F, 0.75F, 0.75F);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 2, -1.5);
		pose.scale(0.5F, 0.5F, 0.5F);
		renderFireball(pose, buffers, gun.lastShot[0]);
		pose.popPose();

		// the ring of golden swords (Items.golden_sword icon), unlit, no culling
		pose.translate(0, 2, 4.5);
		glRotate(pose, roll[2], 0, 0, -1);
		glRotate(pose, recoil[0], -1, 0, 0);
		glRotate(pose, equip[0], -1, 0, 0);
		glRotate(pose, System.currentTimeMillis() / 50D % 360D, 0, 0, 1);

		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		aimingProgress = Math.min(1F, aimingProgress * 2);

		renderSwordRing(pose, buffers, aimingProgress);
	}

	/** Sixteen golden sword sprites, each 0.5 wide, rotated 360/16 around Z and folded by the aiming progress. */
	static void renderSwordRing(PoseStack pose, MultiBufferSource buffers, float aimingProgress) {
		TextureAtlasSprite icon = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(Items.GOLDEN_SWORD).getParticleIcon();
		float minU = icon.getU0();
		float maxU = icon.getU1();
		float minV = icon.getV0();
		float maxV = icon.getV1();
		VertexConsumer tess = buffers.getBuffer(HbmUnlitRenderTypes.blend(TextureAtlas.LOCATION_BLOCKS));

		pose.pushPose();
		int amount = 16;
		for(int i = 0; i < amount; i++) {
			pose.pushPose();
			pose.translate(0, -1.5 - aimingProgress, 0);
			glRotate(pose, 90 * aimingProgress, 1, 0, 0);
			glRotate(pose, -45, 0, 0, 1);
			Matrix4f m = pose.last().pose();
			tess.addVertex(m, -0.5F, -0.5F, -0.5F).setUv(maxU, maxV).setColor(255, 255, 255, 255);
			tess.addVertex(m, 0.5F, -0.5F, -0.5F).setUv(minU, maxV).setColor(255, 255, 255, 255);
			tess.addVertex(m, 0.5F, 0.5F, -0.5F).setUv(minU, minV).setColor(255, 255, 255, 255);
			tess.addVertex(m, -0.5F, 0.5F, -0.5F).setUv(maxU, minV).setColor(255, 255, 255, 255);
			pose.popPose();
			glRotate(pose, 360D / amount, 0, 0, 1);
		}
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		pose.translate(0, -1, 4);
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
		pose.translate(-0.5, -1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1, 0.5);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Hammer");
		part(vc, "Magazine");
		part(vc, "Slide");
		part(vc, "Sight");

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
			pose.translate(0, 2, 4);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			pose.scale(0.75F, 0.75F, 0.75F);
			this.renderMuzzleFlash(shot, 75, 7.5);
			pose.popPose();

			pose.pushPose();
			pose.translate(0, 2, -1.5);
			pose.scale(0.5F, 0.5F, 0.5F);
			renderFireball(pose, buffers, shot);
			pose.popPose();
		}
	}

	/** ItemRenderAberrator#renderFireball: two additive flash_plume quads growing over 150 ms. */
	static void renderFireball(PoseStack pose, MultiBufferSource buffers, long lastShot) {

		int flash = 150;

		if(System.currentTimeMillis() - lastShot < flash) {
			double fire = (System.currentTimeMillis() - lastShot) / (double) flash;
			double height = 5 * fire;
			double length = 10 * fire;
			double offset = 1 * fire;
			double lengthOffset = -1.125;
			VertexConsumer tess = buffers.getBuffer(FLASH.apply(flash_plume));
			Matrix4f m = pose.last().pose();

			vertex(tess, m, height, -offset, 0, 0, 1);
			vertex(tess, m, -height, -offset, 0, 1, 1);
			vertex(tess, m, -height, -offset + length, -lengthOffset, 1, 0);
			vertex(tess, m, height, -offset + length, -lengthOffset, 0, 0);

			vertex(tess, m, height, -offset, 0, 0, 1);
			vertex(tess, m, -height, -offset, 0, 1, 1);
			vertex(tess, m, -height, -offset + length, lengthOffset, 1, 0);
			vertex(tess, m, height, -offset + length, lengthOffset, 0, 0);
		}
	}

	private static void vertex(VertexConsumer tess, Matrix4f m, double x, double y, double z, float u, float v) {
		tess.addVertex(m, (float) x, (float) y, (float) z).setUv(u, v).setColor(1F, 1F, 1F, 1F);
	}
}

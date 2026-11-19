package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderMK108. */
public class ItemRenderMK108 extends ItemRenderWeaponBase {

	/** ResourceManager.mk108 (models/weapons/mk108.obj) */
	static final LegacyEntityObjMesh mk108 = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/mk108.obj"));
	public static final ResourceLocation mk108_tex = AtomicLegacy.id("textures/models/weapons/mk108.png");

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

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
				-1F * offset, -1.5F * offset, 2.5F * offset,
				-0.75F, -0.75F, 1.5F);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(mk108_tex));
	}

	private void part(VertexConsumer vc, String name) {
		mk108.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	private static boolean hasBus(String bus) {
		HbmAnimations.Animation anim = HbmAnimations.getRelevantAnim(0);
		return anim != null && anim.animation.getBus(bus) != null;
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		VertexConsumer vc = consumer();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		boolean doesYeet = hasBus("GRENH1");
		boolean doesCycle = hasBus("CYCLE");
		boolean reloading = hasBus("BELT");
		boolean useShellCount = hasBus("SHELLS");
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
		double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lid = HbmAnimations.getRelevantTransformation("LID");
		double[] belt = HbmAnimations.getRelevantTransformation("BELT");
		double[] drum = HbmAnimations.getRelevantTransformation("DRUM");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] shellCount = HbmAnimations.getRelevantTransformation("SHELLS");

		if(doesYeet) {
			double[][] horizontal = new double[][] {
				HbmAnimations.getRelevantTransformation("GRENH1"),
				HbmAnimations.getRelevantTransformation("GRENH2"),
				HbmAnimations.getRelevantTransformation("GRENH3"),
			};
			double[][] vertical = new double[][] {
				HbmAnimations.getRelevantTransformation("GRENV1"),
				HbmAnimations.getRelevantTransformation("GRENV2"),
				HbmAnimations.getRelevantTransformation("GRENV3"),
			};
			double[][] spin = new double[][] {
				HbmAnimations.getRelevantTransformation("GRENS1"),
				HbmAnimations.getRelevantTransformation("GRENS2"),
				HbmAnimations.getRelevantTransformation("GRENS3"),
			};

			for(int i = 0; i < 3; i++) {
				if(horizontal[i][0] <= -4) continue;
				pose.pushPose();
				pose.translate(horizontal[i][0], vertical[i][1], 0);
				pose.translate(0, 0, -2.3125);
				glRotate(pose, -90, 1, 0, 0);
				glRotate(pose, -spin[i][0], 0, 1, 0);
				pose.translate(0, 0, 2.3125);
				part(vc, "Grenade");
				pose.popPose();
			}
		}

		pose.translate(0, -1, -8);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 8);

		pose.translate(0, 1, -4);
		glRotate(pose, lift[0], 1, 0, 0);
		pose.translate(0, -1, 4);

		pose.translate(0, 0, recoil[2]);

		part(vc, "Gun");

		pose.pushPose();
		pose.translate(0, 0, barrel[2] * 2);
		part(vc, "Barrel");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.6875, -1);
		glRotate(pose, lid[0], 1, 0, 0);
		pose.translate(0, -0.6875, 1);
		part(vc, "Lid");
		pose.popPose();

		pose.pushPose();

		pose.translate(drum[0], drum[1], drum[2]);
		part(vc, "Drum");

		double[] anglesLoaded = new double[]   {0,   0,  -5,   0,   -5,  60,  45,  -10,   0};
		double[] anglesUnloaded = new double[] {0, -30, -60, -45, -45,   0,   0,   0,   0};
		double reloadProgress = !reloading ? 1D : belt[0];
		double cycleProgress = !doesCycle ? 1 : cycle[0];
		double[][] shells = beltShells(anglesLoaded, anglesUnloaded, reloadProgress);

		// MagazineFullReload#getAmount ignores the inventory argument
		int shellAmount = useShellCount ? (int) shellCount[0] : gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);

		// draw belt, interp used for cycling (shells will transform towards the position/rotation of the next shell)
		for(int i = 0; i < shells.length - 1; i++) {
			double[] prevShell = shells[i];
			double[] nextShell = shells[i + 1];
			renderShell(vc, prevShell[0], nextShell[0], prevShell[1], nextShell[1], prevShell[2], nextShell[2], shells.length - i < shellAmount + 2, cycleProgress);
		}
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 8.125);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 50, 5);
		pose.popPose();
	}

	/** Belt link positions: Vec3NT(0, 0.53125, 0) rotated with rotateAroundZDeg(-delta) per link. */
	private static double[][] beltShells(double[] anglesLoaded, double[] anglesUnloaded, double reloadProgress) {
		double p = 0.0625D;
		double x = p * 22;
		double y = p * -46;
		double angle = 0;
		double vx = 0, vy = 0.53125;
		double[][] shells = new double[anglesLoaded.length][3];
		for(int i = 0; i < anglesLoaded.length; i++) {
			shells[i][0] = x;
			shells[i][1] = y;
			shells[i][2] = angle - 90;
			double delta = anglesUnloaded == null ? anglesLoaded[i] : anglesUnloaded[i] + (anglesLoaded[i] - anglesUnloaded[i]) * reloadProgress;
			angle += delta;
			double rad = -delta / 180D * Math.PI;
			double cos = Math.cos(rad);
			double sin = Math.sin(rad);
			double nx = vx * cos + vy * sin;
			double ny = vy * cos - vx * sin;
			vx = nx;
			vy = ny;
			x += vx;
			y += vy;
		}
		return shells;
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.0D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(1, -2.5, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, 0.5, 0.25);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -9.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, 0.5, -0.25);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		VertexConsumer vc = consumer();
		part(vc, "Gun");
		part(vc, "Barrel");
		part(vc, "Lid");
		part(vc, "Drum");

		pose.pushPose();
		double[] anglesLoaded = new double[] { 0, 0, -5, 0, -5, 60, 45, -10, 0 };
		double[][] shells = beltShells(anglesLoaded, null, 1D);

		// draw belt, interp used for cycling (shells will transform towards the position/rotation of the next shell)
		for(int i = 0; i < shells.length - 1; i++) {
			double[] prevShell = shells[i];
			double[] nextShell = shells[i + 1];
			renderShell(vc, prevShell[0], nextShell[0], prevShell[1], nextShell[1], prevShell[2], nextShell[2], true, 0F);
		}
		pose.popPose();

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
			pose.translate(0, 0, 8.125);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 50, 5);
			pose.popPose();
		}
	}

	private void renderShell(VertexConsumer vc, double x0, double x1, double y0, double y1, double rot0, double rot1, boolean shell, double interp) {
		renderShell(vc, x0 + (x1 - x0) * interp, y0 + (y1 - y0) * interp, rot0 + (rot1 - rot0) * interp, shell);
	}

	private void renderShell(VertexConsumer vc, double x, double y, double rot, boolean shell) {
		PoseStack pose = this.pose;
		pose.pushPose();
		pose.translate(x, y, 0);
		glRotate(pose, rot, 0, 0, 1);
		part(vc, "Belt");
		if(shell) part(vc, "Grenade");
		pose.popPose();
	}
}

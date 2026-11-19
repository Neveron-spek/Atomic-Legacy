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

/** 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderSexy (gun_autoshotgun_sexy, sexy_real_no_fake.png). [weapons batch D1] */
public class ItemRenderSexy extends ItemRenderWeaponBase {

	/** ResourceManager.sexy (models/weapons/sexy.obj) */
	static final LegacyEntityObjMesh sexy = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/sexy.obj"));
	public static final ResourceLocation sexy_tex = AtomicLegacy.id("textures/models/weapons/sexy_real_no_fake.png");
	/** [parity c1] ResourceManager.heretic_tex (gun_autoshotgun_heretic) */
	public static final ResourceLocation heretic_tex = AtomicLegacy.id("textures/models/weapons/sexy_heretic.png");
	/** ResourceManager.whiskey (models/weapons/whiskey.obj) */
	static final LegacyEntityObjMesh whiskey = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/whiskey.obj"));
	public static final ResourceLocation whiskey_tex = AtomicLegacy.id("textures/models/weapons/whiskey.png");

	protected ResourceLocation texture;

	public ItemRenderSexy(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1F * offset, -0.75F * offset, 3F * offset,
				-0.5F, -0.5F, 2F);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(texture));
	}

	private void part(String name) {
		sexy.renderGroup(name, pose, consumer(), light, overlay, 0xFFFFFFFF);
	}

	private static boolean hasBus(String bus) {
		HbmAnimations.Animation anim = HbmAnimations.getRelevantAnim(0);
		return anim != null && anim.animation.getBus(bus) != null;
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		double scale = 0.375D;
		pose.scale((float) scale, (float) scale, (float) scale);

		// i'm not going overboard with the animation
		boolean doesCycle = hasBus("CYCLE");
		boolean reloading = hasBus("BELT");
		boolean useShellCount = hasBus("SHELLS");
		boolean girldinner = hasBus("BOTTLE");
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lower = HbmAnimations.getRelevantTransformation("LOWER");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
		double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
		double[] hood = HbmAnimations.getRelevantTransformation("HOOD");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] belt = HbmAnimations.getRelevantTransformation("BELT");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] magRot = HbmAnimations.getRelevantTransformation("MAGROT");
		double[] shellCount = HbmAnimations.getRelevantTransformation("SHELLS");
		double[] bottle = HbmAnimations.getRelevantTransformation("BOTTLE");
		double[] sippy = HbmAnimations.getRelevantTransformation("SIP");

		if(girldinner) {
			pose.pushPose();
			pose.translate(bottle[0], bottle[1], bottle[2]);
			pose.translate(0, 2, 0);
			glRotate(pose, sippy[0], 1, 0, 0);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, -15, 1, 0, 0);
			pose.translate(0, -2, 0);
			pose.scale(1.5F, 1.5F, 1.5F);
			whiskey.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(whiskey_tex)), light, overlay, 0xFFFFFFFF);
			pose.popPose();
		}

		pose.translate(0, -1, -8);
		glRotate(pose, equip[0], 1, 0, 0);
		pose.translate(0, 1, 8);

		pose.translate(0, 0, -6);
		glRotate(pose, lower[0], 1, 0, 0);
		pose.translate(0, 0, 6);

		pose.translate(0, 0, recoil[2]);

		part("Gun");

		pose.pushPose();
		pose.translate(0, 0, barrel[2]);
		part("Barrel");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, -0.375);
		pose.scale(1F, 1F, (float) (1 + 0.457247371D * barrel[2]));
		pose.translate(0, 0, 0.375);
		part("RecoilSpring");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.4375, -2.875);
		glRotate(pose, hood[0], 1, 0, 0);
		pose.translate(0, -0.4375, 2.875);
		part("Hood");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0.46875, -6.875);
		glRotate(pose, lever[2] * 60, 1, 0, 0);
		pose.translate(0, -0.46875, 6.875);
		part("Lever");
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, -6.75);
		pose.scale(1F, 1F, (float) (1 - lever[2] * 0.25));
		pose.translate(0, 0, 6.75);
		part("LockSpring");
		pose.popPose();

		pose.pushPose();
		pose.translate(mag[0], mag[1], mag[2]);
		pose.translate(0, -1, 0);
		glRotate(pose, magRot[2], 0, 0, 1);
		pose.translate(0, 1, 0);
		part("Magazine");

		double p = 0.0625D;
		double x = p * 17;
		double y = p * -26;
		double angle = 0;
		// Vec3NT(0, 0.4375, 0) reused for every link
		double vecX = 0;
		double vecY = 0.4375;

		// basically what all this does is take an array of angles and just strings together shells with the appropriate
		// position and angle calculated out of the next angle, taking all previous transformations into account.
		// has a second array which is the "open" position that the animation can smoothly interpolate through
		double[] anglesLoaded = new double[]   {0,   0,  20,  20,  50, 60, 70};
		double[] anglesUnloaded = new double[] {0, -10, -50, -60, -60,  0,  0};
		double reloadProgress = !reloading ? 1D : belt[0];
		double cycleProgress = !doesCycle ? 1 : cycle[0];

		double[][] shells = new double[anglesLoaded.length][3];

		// generate belt, interp used for the reload animation
		for(int i = 0; i < anglesLoaded.length; i++) {
			shells[i][0] = x;
			shells[i][1] = y;
			shells[i][2] = angle - 90;
			double delta = interp(anglesUnloaded[i], anglesLoaded[i], reloadProgress);
			angle += delta;
			// Vec3NT#rotateAroundZDeg(-delta)
			double rad = -delta / 180D * Math.PI;
			double cos = Math.cos(rad);
			double sin = Math.sin(rad);
			double nx = vecX * cos + vecY * sin;
			double ny = vecY * cos - vecX * sin;
			vecX = nx;
			vecY = ny;
			x += vecX;
			y += vecY;
		}

		int shellAmount = useShellCount ? (int) shellCount[0] : gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);

		// draw belt, interp used for cycling (shells will transform towards the position/rotation of the next shell)
		for(int i = 0; i < shells.length - 1; i++) {
			double[] prevShell = shells[i];
			double[] nextShell = shells[i + 1];
			renderShell(prevShell[0], nextShell[0], prevShell[1], nextShell[1], prevShell[2], nextShell[2], shells.length - i < shellAmount + 2, cycleProgress);
		}
		pose.popPose();

		pose.pushPose();
		pose.translate(0, 0, 8);
		glRotate(pose, 90, 0, 1, 0);
		glRotate(pose, 90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		pose.popPose();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(1, 1, 6);
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
		pose.translate(0, 0, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {

		part("Gun");
		part("Barrel");
		part("RecoilSpring");
		part("Hood");
		part("Lever");
		part("LockSpring");
		part("Magazine");

		double p = 0.0625D;
		renderShell(p *  0, p *  -6,  90, true);
		renderShell(p *  5, p *   1,  30, true);
		renderShell(p * 12, p *  -1, -30, true);
		renderShell(p * 17, p *  -6, -60, true);
		renderShell(p * 17, p * -13, -90, true);
		renderShell(p * 17, p * -20, -90, true);

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
			pose.translate(0, 0, 8);
			glRotate(pose, 90, 0, 1, 0);
			glRotate(pose, 90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 150, 7.5);
			pose.popPose();
		}
	}

	/** BobMathUtil.interp */
	private static double interp(double x, double y, double interp) { return x + (y - x) * interp; }

	public void renderShell(double x0, double x1, double y0, double y1, double rot0, double rot1, boolean shell, double interp) {
		renderShell(interp(x0, x1, interp), interp(y0, y1, interp), interp(rot0, rot1, interp), shell);
	}

	public void renderShell(double x, double y, double rot, boolean shell) {
		PoseStack pose = this.pose;
		pose.pushPose();
		pose.translate(x, 0.375 + y, 0);
		glRotate(pose, rot, 0, 0, 1);
		pose.translate(0, -0.375, 0);
		part("Belt");
		if(shell) part("Shell");
		pose.popPose();
	}
}

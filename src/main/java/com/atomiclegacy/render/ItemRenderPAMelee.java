package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryPA;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryPA.IPAMelee;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * [parity c1] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderPAMelee (gun_pa_melee). The first person view is
 * drawn by the melee component of the worn power armor: ArmorRPAMelee#renderFirstPerson (armor_remnant arms,
 * rpa_arm.png) or ArmorNCRPAMelee#renderFirstPerson (armor_ncr arms, ncrpa_arm.png); nothing without a component.
 * Inventory, dropped item and weapon table show the two NCR arms; nothing is drawn in the third person hand.
 */
public class ItemRenderPAMelee extends ItemRenderWeaponBase {

	/** ResourceManager.armor_remnant / armor_ncr (models/armor/remnant.obj, ncrpa.obj; port path models/obj/armor) */
	static final LegacyEntityObjMesh armor_remnant = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/armor/remnant.obj"));
	static final LegacyEntityObjMesh armor_ncr = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/armor/ncrpa.obj"));
	public static final ResourceLocation rpa_arm = AtomicLegacy.id("textures/armor/rpa_arm.png");
	public static final ResourceLocation ncrpa_arm = AtomicLegacy.id("textures/armor/ncrpa_arm.png");

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override protected float getSwayMagnitude(ItemStack stack) { return 2F; }
	@Override protected float getSwayPeriod(ItemStack stack) { return 0.5F; }

	private void part(LegacyEntityObjMesh mesh, ResourceLocation texture, String name) {
		mesh.renderGroup(name, pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay, 0xFFFFFFFF);
	}

	/** IPAMelee#setupFirstPerson: empty for both components */
	@Override
	public void setupFirstPerson(ItemStack stack) {
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		IPAMelee component = XFactoryPA.getMeleeComponentClient();
		if(component == XFactoryPA.RPA_MELEE) renderRPA();
		if(component == XFactoryPA.NCRPA_MELEE) renderNCRPA();
	}

	/** ArmorRPAMelee#renderFirstPerson */
	private void renderRPA() {
		pose.translate(0, -1.5, 0.5);
		float scale = 0.125F;
		pose.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double swingRight = HbmAnimations.getRelevantTransformation("SWINGRIGHT")[0];
		double swingLeft = HbmAnimations.getRelevantTransformation("SWINGLEFT")[0];
		double slapTurn = HbmAnimations.getRelevantTransformation("SLAPTURN")[0];
		double slap = HbmAnimations.getRelevantTransformation("SLAP")[0];

		double forwardTilt = 60 - 60 * equip[0];
		double offsetOutward = 3;
		double roll = 60;

		pose.pushPose();

		pose.translate(-12 * swingLeft + 2 * slapTurn - 5 * slap, 6 * slap, 5 * swingLeft + 8 * slap);
		glRotate(pose, forwardTilt - swingRight * 20, 1, 0, 0);

		pose.translate(offsetOutward, 0, 0);
		pose.translate(6, 8, 0);
		glRotate(pose, 60 * swingLeft + 45 * slap, 0, 0, 1);
		glRotate(pose, roll + 15 * swingLeft + 45 * slapTurn, 0, 1, 0);
		pose.translate(-6, -8, 0);
		part(armor_remnant, rpa_arm, "LeftArm");
		pose.popPose();

		pose.pushPose();

		pose.translate(12 * swingRight - 2 * slapTurn + 5 * slap, 6 * slap, 5 * swingRight + 8 * slap);
		glRotate(pose, forwardTilt - swingLeft * 20, 1, 0, 0);

		pose.translate(-offsetOutward, 0, 0);
		pose.translate(-6, 8, 0);
		glRotate(pose, -60 * swingRight - 45 * slap, 0, 0, 1);
		glRotate(pose, -roll - 15 * swingRight - 45 * slapTurn, 0, 1, 0);
		pose.translate(6, -8, 0);
		part(armor_remnant, rpa_arm, "RightArm");
		pose.popPose();
	}

	/** ArmorNCRPAMelee#renderFirstPerson */
	private void renderNCRPA() {
		pose.translate(0, -1.5, 0.5);
		float scale = 0.125F;
		pose.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double swingRight = HbmAnimations.getRelevantTransformation("SWINGRIGHT")[0];
		double swingLeft = HbmAnimations.getRelevantTransformation("SWINGLEFT")[0];
		double sweepTurn = HbmAnimations.getRelevantTransformation("SWEEPTURN")[0];
		double sweepCut = HbmAnimations.getRelevantTransformation("SWEEPCUT")[0];

		double forwardTilt = 60 - 60 * equip[0];
		double offsetOutward = 3;
		double roll = 60;

		pose.pushPose();

		pose.translate(-14 * swingLeft - 4 * sweepTurn, 6 * sweepCut, 2 * swingLeft + 8 * sweepCut);
		glRotate(pose, forwardTilt + swingRight * 40 - 60 * sweepCut, 1, 0, 0);

		pose.translate(offsetOutward, 0, 0);
		pose.translate(6, 8, 0);
		glRotate(pose, 90 * swingLeft, 0, 0, 1);
		glRotate(pose, roll + 30 * swingLeft - 90 * sweepTurn, 0, 1, 0);
		pose.translate(-6, -8, 0);
		part(armor_ncr, ncrpa_arm, "LeftArm");
		pose.popPose();

		pose.pushPose();

		pose.translate(14 * swingRight + 4 * sweepTurn, 6 * sweepCut, 2 * swingRight + 8 * sweepCut);
		glRotate(pose, forwardTilt + swingLeft * 40 - 60 * sweepCut, 1, 0, 0);

		pose.translate(-offsetOutward, 0, 0);
		pose.translate(-6, 8, 0);
		glRotate(pose, -90 * swingRight, 0, 0, 1);
		glRotate(pose, -roll - 30 * swingRight + 90 * sweepTurn, 0, 1, 0);
		pose.translate(6, -8, 0);
		part(armor_ncr, ncrpa_arm, "RightArm");
		pose.popPose();
	}

	@Override public void setupThirdPerson(ItemStack stack) { }
	@Override public void setupThirdPersonAkimbo(ItemStack stack) { }

	@Override
	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 8, 0);
		float scale = 2.5F;
		pose.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		float scale = -12.5F;
		pose.scale(scale, scale, scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -0.5, 1);
	}

	@Override
	public void renderInv(ItemStack stack) {
		pose.pushPose();
		float scale = 0.3125F;
		pose.scale(scale, scale, scale);

		glRotate(pose, 135, 0, 0, 1);
		glRotate(pose, 135, 0, 1, 0);
		pose.translate(0, -5.5, 0);
		pose.translate(-3.5, 0, 0);
		part(armor_ncr, ncrpa_arm, "LeftArm"); // "Leftarm" in the original; part names are matched ignoring case there
		pose.translate(7, 1, -1);
		part(armor_ncr, ncrpa_arm, "RightArm");
		pose.popPose();
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity entity) {
		if(type == LegacyRenderType.EQUIPPED) return;

		pose.pushPose();
		float scale = 0.3125F;
		pose.scale(scale, scale, scale);

		glRotate(pose, 90, 1, 0, 0);
		pose.translate(0, -5.5, 0);
		pose.translate(-2, 0, 0);
		part(armor_ncr, ncrpa_arm, "LeftArm");
		pose.translate(4, 0, 0);
		part(armor_ncr, ncrpa_arm, "RightArm");
		pose.popPose();
	}
}

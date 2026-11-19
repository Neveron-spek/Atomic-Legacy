package com.atomiclegacy.render;

import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.factory.XFactoryTool;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * [weapons batch F3] 1.7.10 com.hbm.render.item.weapon.ItemRenderFireExt: a plain IItemRenderer (no ItemRenderWeaponBase
 * animation buses). The first person pass goes through the port's weapon hand hook; setupFirstPerson applies the vanilla
 * 1.7.10 ItemRenderer first person chain (no swing, equipped) in place of the sedna offset before the renderer's own
 * EQUIPPED_FIRST_PERSON transform. The texture follows the loaded tank type (normal / foam / sand).
 */
public class ItemRenderFireExt extends ItemRenderWeaponBase {

	/** ResourceManager.fireext (models/weapons/fireext.obj) */
	static final LegacyEntityObjMesh fireext = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/fireext.obj"));
	public static final ResourceLocation fireext_tex = AtomicLegacy.id("textures/models/weapons/fireext_normal.png");
	public static final ResourceLocation fireext_foam_tex = AtomicLegacy.id("textures/models/weapons/fireext_foam.png");
	public static final ResourceLocation fireext_sand_tex = AtomicLegacy.id("textures/models/weapons/fireext_sand.png");

	private static ResourceLocation texture(ItemStack item) {
		ItemGunBaseNT gun = (ItemGunBaseNT) item.getItem();
		IMagazine<?> mag = gun.getConfig(item, 0).getReceivers(item)[0].getMagazine(item);
		ResourceLocation tex = fireext_tex;
		if(mag.getType(item, null) == XFactoryTool.fext_foam) tex = fireext_foam_tex;
		if(mag.getType(item, null) == XFactoryTool.fext_sand) tex = fireext_sand_tex;
		return tex;
	}

	private void renderAll(ItemStack stack) {
		fireext.renderAll(pose, buffers.getBuffer(RenderType.entityCutout(texture(stack))), light, overlay, 0xFFFFFFFF);
	}

	/** ItemRenderer#renderItemInFirstPerson for a custom renderer (translate, rotate 45 Y, scale 0.4), then the Forge equipped chain */
	@Override
	public void setupFirstPerson(ItemStack stack) {
		// setPerspectiveAndRender turns the sedna frame by 180 Y; the plain 1.7.10 IItemRenderer is drawn by
		// ItemRenderer#renderItemInFirstPerson in camera space, so undo it (otherwise the model sits behind the camera)
		pose.mulPose(Axis.YP.rotationDegrees(180));
		float f6 = 0.8F;
		pose.translate(0.7F * f6, -0.65F * f6, -0.9F * f6);
		pose.mulPose(Axis.YP.rotationDegrees(45.0F));
		pose.scale(0.4F, 0.4F, 0.4F);
		// ForgeHooksClient#renderEquippedItem for renderers without the BLOCK_3D helper (as CrucibleItemRenderer)
		pose.translate(0.0F, -0.3F, 0.0F);
		pose.scale(1.5F, 1.5F, 1.5F);
		pose.mulPose(Axis.YP.rotationDegrees(50.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(335.0F));
		pose.translate(-0.9375F, -0.0625F, 0.0F);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		double s0 = 0.35D;
		glRotate(pose, 25, 0, 0, 1);
		pose.translate(0.5, -0.5, -0.5F);
		glRotate(pose, 80, 0, 1, 0);
		pose.scale((float) s0, (float) s0, (float) s0);
		renderAll(stack);
	}

	/** EQUIPPED: the vanilla/Forge equipped chain of the base, then the renderer's own transform. */
	@Override
	public void setupThirdPerson(ItemStack stack) {
		double scale = 0.5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.mulPose(Axis.ZP.rotationDegrees(20F));
		glRotate(pose, -5, 0, 1, 1);
		pose.mulPose(Axis.YP.rotationDegrees(10F));
		pose.mulPose(Axis.XP.rotationDegrees(15F));
		pose.translate(0.75F, -2.75F, 0.5F);
	}

	/** ENTITY: scale 0.3 */
	@Override
	public void setupEntity(ItemStack stack) {
		double s1 = 0.3D;
		pose.scale((float) s1, (float) s1, (float) s1);
	}

	/** INVENTORY: translate(2, 14, 0), rotate -90 Y, -135 X, time Y, scale(4.5, 4.5, -4.5) in the 1.7.10 slot space. */
	@Override
	public void setupInv(ItemStack stack) {
		double s = 4.5D;
		pose.translate(2, 14, 0);
		glRotate(pose, -90, 0, 1, 0);
		glRotate(pose, -135, 1, 0, 0);
		glRotate(pose, System.currentTimeMillis() / 10 % 360, 0, 1, 0);
		pose.scale((float) s, (float) s, (float) -s);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		super.setupModTable(stack);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity entity) {
		renderAll(stack);
	}
}

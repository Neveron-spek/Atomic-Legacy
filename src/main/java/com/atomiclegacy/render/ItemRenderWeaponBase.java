package com.atomiclegacy.render;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.SmokeNode;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;

/**
 * 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase.
 *
 * <p>First person: like the original RenderHandEvent hook, the hand pass is
 * replaced by its own projection (base FOV 70, or the FOV setting with
 * GUN_MODEL_FOV) and the original sway/viewbob transforms.
 *
 * <p>Other render types go through the item's BEWLR. Each context first undoes
 * the 1.21.1 vanilla item placement and then applies the exact 1.7.10
 * vanilla/Forge transform chain that preceded IItemRenderer#renderItem.
 */
public abstract class ItemRenderWeaponBase extends BlockEntityWithoutLevelRenderer {

	public static final ResourceLocation flash_plume = AtomicLegacy.id("textures/models/weapons/lilmac_plume.png");

	public static float interp;
	public static final Map<LivingEntity, Long> flashMap = new WeakHashMap<>();
	/** Entity whose layers are being rendered (data[1] of ItemRenderType.EQUIPPED). */
	public static LivingEntity renderingEntity;

	/** 1.7.10 IItemRenderer.ItemRenderType. */
	public enum LegacyRenderType {
		ENTITY,
		EQUIPPED,
		EQUIPPED_FIRST_PERSON,
		INVENTORY
	}

	/** Smoke trail quads: untextured, alpha blended, no depth write, no culling. */
	protected static final RenderType SMOKE = RenderType.create(
			"atomiclegacy_gun_smoke",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			1536,
			false,
			true,
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setTextureState(RenderStateShard.NO_TEXTURE)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.createCompositeState(false));

	/** Muzzle flash: GL_SRC_ALPHA/GL_ONE, full bright, no depth write, no culling. */
	protected static final Function<ResourceLocation, RenderType> FLASH = Util.memoize(texture -> RenderType.create(
			"atomiclegacy_gun_flash",
			DefaultVertexFormat.POSITION_TEX_COLOR,
			VertexFormat.Mode.QUADS,
			1536,
			false,
			true,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
					.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
					.setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.createCompositeState(false)));

	protected ItemRenderWeaponBase() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	public boolean isAkimbo(LivingEntity entity) { return false; }
	public boolean isLeftHanded() { return false; }

	/* ===== render context passed to the 1.7.10-style methods ===== */
	protected PoseStack pose;
	protected MultiBufferSource buffers;
	protected int light;
	protected int overlay;

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		this.pose = poseStack;
		this.buffers = bufferSource;
		this.light = packedLight;
		this.overlay = packedOverlay;
		poseStack.pushPose();
		switch(context) {
		case GUI -> {
			// 1.21.1 GUI item space (origin bottom-left, y up, 1 = 16 px) -> 1.7.10 slot space (origin top-left, y down, px)
			poseStack.translate(0.0F, 1.0F, 0.0F);
			poseStack.scale(1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);
			setupInv(stack);
			renderInv(stack);
		}
		case GROUND -> {
			// ItemEntityRenderer adds 0.25 * ground scale on top of the 1.7.10 bob, then the -0.5 item offset.
			poseStack.translate(0.5F, 0.25F, 0.5F);
			// ForgeHooksClient#renderEntityItem: glScalef(0.5)
			poseStack.scale(0.5F, 0.5F, 0.5F);
			setupEntity(stack);
			renderEntity(stack);
		}
		case FIXED, HEAD, NONE -> {
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			setupEntity(stack);
			renderEntity(stack);
		}
		case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
			boolean left = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			if(!isLeftHanded()) {
				undoVanillaHandPlacement(poseStack, left);
				legacyEquippedChain(poseStack);
				setupThirdPerson(stack);
				renderEquipped(stack, renderingEntity);
			}
		}
		}
		poseStack.popPose();
	}

	/** Undo ItemInHandLayer#renderArmWithItem after translateToHand and ItemRenderer's -0.5 offset. */
	protected static void undoVanillaHandPlacement(PoseStack poseStack, boolean left) {
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.translate(-(left ? -1.0F : 1.0F) / 16.0F, -0.125F, 0.625F);
		poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
	}

	/** RenderPlayer#renderEquippedItems (non-block, non-full3D branch) and ForgeHooksClient#renderEquippedItem. */
	protected static void legacyEquippedChain(PoseStack poseStack) {
		poseStack.translate(-0.0625F, 0.4375F, 0.0625F);
		float f2 = 0.375F;
		poseStack.translate(0.25F, 0.1875F, -0.1875F);
		poseStack.scale(f2, f2, f2);
		poseStack.mulPose(Axis.ZP.rotationDegrees(60.0F));
		poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F));
		poseStack.translate(0.0F, -0.3F, 0.0F);
		poseStack.scale(1.5F, 1.5F, 1.5F);
		poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
		poseStack.translate(-0.9375F, -0.0625F, 0.0F);
	}

	public void renderEquipped(ItemStack stack, LivingEntity entity) { renderOther(stack, LegacyRenderType.EQUIPPED, entity); }
	/** Left hand gun of akimbo/left handed weapons (the original passes no data to renderOther here). */
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity entity) { renderOther(stack, LegacyRenderType.EQUIPPED, null); }
	public void renderInv(ItemStack stack) { renderOther(stack, LegacyRenderType.INVENTORY, null); }
	public void renderEntity(ItemStack stack) { renderOther(stack, LegacyRenderType.ENTITY, null); }

	/** RenderHandEvent replacement: renders the first person gun with its own projection. */
	public void setPerspectiveAndRender(ItemStack stack, float partialTicks, MultiBufferSource.BufferSource bufferSource, int packedLight) {

		interp = partialTicks;
		Minecraft mc = Minecraft.getInstance();
		float farPlaneDistance = mc.options.getEffectiveRenderDistance() * 16;

		Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting previousSorting = RenderSystem.getVertexSorting();
		Matrix4f projection = new Matrix4f().setPerspective(
				(float) Math.toRadians(this.getFOVModifier(partialTicks, HbmClientConfig.gunModelFov())),
				(float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight(),
				0.05F, farPlaneDistance * 2.0F);
		RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.identity();
		RenderSystem.applyModelViewMatrix();

		this.pose = new PoseStack();
		this.buffers = bufferSource;
		this.light = packedLight;
		this.overlay = OverlayTexture.NO_OVERLAY;

		this.setupTransformsAndRender(stack);
		bufferSource.endBatch();

		modelView.popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
		Lighting.setupLevel();
	}

	private float getFOVModifier(float interp, boolean useFOVSetting) {

		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		LivingEntity entityplayer = camera.getEntity() instanceof LivingEntity living ? living : mc.player;
		float fov = getBaseFOV(entityplayer.getMainHandItem());

		if(useFOVSetting) fov = mc.options.fov().get();

		if(entityplayer.getHealth() <= 0.0F) {
			float f2 = (float) entityplayer.deathTime + interp;
			fov /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
		}

		if(camera.getFluidInCamera() == FogType.WATER) fov = fov * 60.0F / 70.0F;

		return fov;
	}

	protected float getBaseFOV(ItemStack stack) { return 70F; }
	public float getViewFOV(ItemStack stack, float fov) { return fov; }
	protected float getSwayMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0.1F : 0.5F; }
	protected float getSwayPeriod(ItemStack stack) { return 0.75F; }
	protected float getTurnMagnitude(ItemStack stack) { return 2.75F; }

	protected void setupTransformsAndRender(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		PoseStack poseStack = this.pose;

		float swayMagnitude = getSwayMagnitude(stack);
		float swayPeriod = getSwayPeriod(stack);
		float turnMagnitude = getTurnMagnitude(stack);

		//lighting setup (item lighting changes based on player rotation)
		float pitch = player.xRotO + (player.getXRot() - player.xRotO) * interp;
		float yaw = player.yRotO + (player.getYRot() - player.yRotO) * interp;
		Matrix4f lightRotation = new Matrix4f().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
		Vector3f light0 = lightRotation.transformDirection(new Vector3f(0.2F, 1.0F, -0.7F).normalize());
		Vector3f light1 = lightRotation.transformDirection(new Vector3f(-0.2F, 1.0F, 0.7F).normalize());
		RenderSystem.setShaderLights(light0, light1);

		//floppyness
		float armPitch = player.xBobO + (player.xBob - player.xBobO) * interp;
		float armYaw = player.yBobO + (player.yBob - player.yBobO) * interp;
		poseStack.mulPose(Axis.XP.rotationDegrees((player.getXRot() - armPitch) * 0.1F * turnMagnitude));
		poseStack.mulPose(Axis.YP.rotationDegrees((player.getYRot() - armYaw) * 0.1F * turnMagnitude));

		poseStack.pushPose();

		poseStack.mulPose(Axis.YP.rotationDegrees(180));

		//viewbob (1.21.1 has no cameraPitch, the falling tilt of 1.7.10, so that term is 0)
		if(mc.getCameraEntity() instanceof net.minecraft.world.entity.player.Player entityplayer) {
			float distanceDelta = entityplayer.walkDist - entityplayer.walkDistO;
			float distanceInterp = -(entityplayer.walkDist + distanceDelta * interp);
			float camYaw = entityplayer.oBob + (entityplayer.bob - entityplayer.oBob) * interp;
			poseStack.translate((float) Math.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 0.5F * swayMagnitude, -Math.abs((float) Math.cos(distanceInterp * (float) Math.PI * swayPeriod) * camYaw) * swayMagnitude, 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 3.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs((float) Math.cos(distanceInterp * (float) Math.PI * swayPeriod - 0.2F) * camYaw) * 5.0F));
		}

		poseStack.pushPose();
		setupFirstPerson(stack);
		renderFirstPerson(stack);
		poseStack.popPose();

		poseStack.popPose();
	}

	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 1);

		if(Minecraft.getInstance().player.isShiftKeyDown()) {
			pose.translate(0, -3.875 / 8D, 0);
		} else {
			float offset = 0.8F;
			pose.mulPose(Axis.YP.rotationDegrees(180));
			pose.translate(1.0F * offset, -0.75F * offset, -0.5F * offset);
			pose.mulPose(Axis.YP.rotationDegrees(180));
		}
	}

	public void setupThirdPerson(ItemStack stack) {
		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);

		pose.mulPose(Axis.ZP.rotationDegrees(15.0F));
		pose.mulPose(Axis.YP.rotationDegrees(12.5F));
		pose.mulPose(Axis.XP.rotationDegrees(15.0F));

		pose.translate(3.5, 0, 0);
	}

	public void setupThirdPersonAkimbo(ItemStack stack) {
		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);

		pose.mulPose(Axis.ZP.rotationDegrees(15.0F));
		pose.mulPose(Axis.YP.rotationDegrees(12.5F));
		pose.mulPose(Axis.XP.rotationDegrees(10.0F));

		pose.translate(5, 0, 0);
	}

	/**
	 * ModEventHandlerRenderer#onRenderAkimbo (RenderPlayerEvent.Specials.Pre): the pose must already be
	 * at the player's left arm (bipedLeftArm.postRender); applies the vanilla/Forge equipped chain and
	 * renders the left hand gun.
	 */
	public void renderAkimboOnLeftArm(ItemStack held, LivingEntity player, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.pose = poseStack;
		this.buffers = bufferSource;
		this.light = packedLight;
		this.overlay = OverlayTexture.NO_OVERLAY;
		poseStack.pushPose();
		legacyEquippedChain(poseStack);
		if(this.isLeftHanded()) {
			poseStack.translate(0.1875F, 0F, 0.0F);
			this.setupThirdPerson(held);
			this.renderEquippedAkimbo(held, player);
		} else {
			this.setupThirdPersonAkimbo(held);
			this.renderEquippedAkimbo(held, player);
		}
		poseStack.popPose();
	}

	public void setupInv(ItemStack stack) {
		pose.scale(1, 1, -1);
		pose.translate(8, 8, 0);
		pose.mulPose(Axis.ZP.rotationDegrees(225));
		pose.mulPose(Axis.YP.rotationDegrees(90));
	}

	public void setupEntity(ItemStack stack) {
		double scale = 0.125D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.mulPose(Axis.YP.rotationDegrees(-90));
	}

	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.mulPose(Axis.YP.rotationDegrees(90));
	}

	public void renderModTable(ItemStack stack, int index) {
		renderOther(stack, LegacyRenderType.INVENTORY, null);
	}

	/** Renders the weapon inside the weapon table preview with the given pose. */
	public void renderInModTable(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.pose = poseStack;
		this.buffers = bufferSource;
		this.light = packedLight;
		this.overlay = OverlayTexture.NO_OVERLAY;
		poseStack.pushPose();
		setupModTable(stack);
		renderModTable(stack, 0);
		poseStack.popPose();
	}

	public abstract void renderFirstPerson(ItemStack stack);
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity entity) { }

	/** GL11.glRotated(angle, x, y, z): rotation in degrees around the normalized axis; a zero axis does nothing. */
	public static void glRotate(PoseStack pose, double angle, double x, double y, double z) {
		double len = Math.sqrt(x * x + y * y + z * z);
		if(len == 0D || angle == 0D) return;
		pose.mulPose(new org.joml.Quaternionf().rotateAxis((float) Math.toRadians(angle), (float) (x / len), (float) (y / len), (float) (z / len)));
	}

	public static void standardAimingTransform(PoseStack pose, ItemStack stack, double sX, double sY, double sZ, double aX, double aY, double aZ) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		double x = sX + (aX - sX) * aimingProgress;
		double y = sY + (aY - sY) * aimingProgress;
		double z = sZ + (aZ - sZ) * aimingProgress;
		pose.translate(x, y, z);
	}

	public void renderSmokeNodes(List<SmokeNode> nodes, double scale) {

		if(nodes.size() > 1) {
			VertexConsumer tess = buffers.getBuffer(SMOKE);
			Matrix4f m = pose.last().pose();

			for(int i = 0; i < nodes.size() - 1; i++) {
				SmokeNode node = nodes.get(i);
				SmokeNode past = nodes.get(i + 1);
				// Tessellator#setColorRGBA_F clamped each channel to [0;255]; node alpha can drop below 0.
				float nodeAlpha = (float) Math.max(0D, Math.min(1D, node.alpha));
				float pastAlpha = (float) Math.max(0D, Math.min(1D, past.alpha));

				tess.addVertex(m, (float) node.forward, (float) node.lift, (float) node.side).setColor(1F, 1F, 1F, nodeAlpha);
				tess.addVertex(m, (float) node.forward, (float) node.lift, (float) (node.side + node.width * scale)).setColor(1F, 1F, 1F, 0F);
				tess.addVertex(m, (float) past.forward, (float) past.lift, (float) (past.side + past.width * scale)).setColor(1F, 1F, 1F, 0F);
				tess.addVertex(m, (float) past.forward, (float) past.lift, (float) past.side).setColor(1F, 1F, 1F, pastAlpha);

				tess.addVertex(m, (float) node.forward, (float) node.lift, (float) node.side).setColor(1F, 1F, 1F, nodeAlpha);
				tess.addVertex(m, (float) node.forward, (float) node.lift, (float) (node.side - node.width * scale)).setColor(1F, 1F, 1F, 0F);
				tess.addVertex(m, (float) past.forward, (float) past.lift, (float) (past.side - past.width * scale)).setColor(1F, 1F, 1F, 0F);
				tess.addVertex(m, (float) past.forward, (float) past.lift, (float) past.side).setColor(1F, 1F, 1F, pastAlpha);
			}
		}
	}

	public void renderMuzzleFlash(long lastShot) {
		renderMuzzleFlash(lastShot, 75, 15);
	}

	public void renderMuzzleFlash(long lastShot, int duration, double l) {

		int flash = duration;

		if(System.currentTimeMillis() - lastShot < flash) {

			double fire = (System.currentTimeMillis() - lastShot) / (double) flash;

			double width = 6 * fire;
			double length = l * fire;
			double inset = 2;
			VertexConsumer tess = buffers.getBuffer(FLASH.apply(flash_plume));
			Matrix4f m = pose.last().pose();

			flashVertex(tess, m, 0, -width, - inset, 1, 1);
			flashVertex(tess, m, 0, width, - inset, 0, 1);
			flashVertex(tess, m, 0.1, width, length - inset, 0 ,0);
			flashVertex(tess, m, 0.1, -width, length - inset, 1, 0);

			flashVertex(tess, m, 0, width, inset, 0, 1);
			flashVertex(tess, m, 0, -width, inset, 1, 1);
			flashVertex(tess, m, 0.1, -width, -length + inset, 1, 0);
			flashVertex(tess, m, 0.1, width, -length + inset, 0 ,0);

			flashVertex(tess, m, 0, - inset, width, 0, 1);
			flashVertex(tess, m, 0, - inset, -width, 1, 1);
			flashVertex(tess, m, 0.1, length - inset, -width, 1, 0);
			flashVertex(tess, m, 0.1, length - inset, width, 0 ,0);

			flashVertex(tess, m, 0, inset, -width, 1, 1);
			flashVertex(tess, m, 0, inset, width, 0, 1);
			flashVertex(tess, m, 0.1, -length + inset, width, 0 ,0);
			flashVertex(tess, m, 0.1, -length + inset, -width, 1, 0);
		}
	}

	public void renderGapFlash(long lastShot) {

		int flash = 75;

		if(System.currentTimeMillis() - lastShot < flash) {

			double fire = (System.currentTimeMillis() - lastShot) / (double) flash;

			double height = 4 * fire;
			double length = 15 * fire;
			double lift = 3 * fire;
			double offset = 1 * fire;
			double lengthOffset = 0.125;
			VertexConsumer tess = buffers.getBuffer(FLASH.apply(flash_plume));
			Matrix4f m = pose.last().pose();

			flashVertex(tess, m, 0, -height, -offset, 1, 1);
			flashVertex(tess, m, 0, height, -offset, 0, 1);
			flashVertex(tess, m, 0, height + lift, length - offset, 0 ,0);
			flashVertex(tess, m, 0, -height + lift, length - offset, 1, 0);

			flashVertex(tess, m, 0, height, offset, 0, 1);
			flashVertex(tess, m, 0, -height, offset, 1, 1);
			flashVertex(tess, m, 0, -height + lift, -length + offset, 1, 0);
			flashVertex(tess, m, 0, height + lift, -length + offset, 0 ,0);

			flashVertex(tess, m, 0, -height, -offset, 1, 1);
			flashVertex(tess, m, 0, height, -offset, 0, 1);
			flashVertex(tess, m, lengthOffset, height, length - offset, 0 ,0);
			flashVertex(tess, m, lengthOffset, -height, length - offset, 1, 0);

			flashVertex(tess, m, 0, height, offset, 0, 1);
			flashVertex(tess, m, 0, -height, offset, 1, 1);
			flashVertex(tess, m, lengthOffset, -height, -length + offset, 1, 0);
			flashVertex(tess, m, lengthOffset, height, -length + offset, 0 ,0);
		}
	}

	private static void flashVertex(VertexConsumer tess, Matrix4f m, double x, double y, double z, float u, float v) {
		tess.addVertex(m, (float) x, (float) y, (float) z).setUv(u, v).setColor(1F, 1F, 1F, 1F);
	}
}

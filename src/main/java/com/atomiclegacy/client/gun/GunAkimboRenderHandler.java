package com.atomiclegacy.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.render.ItemRenderWeaponBase;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 1.7.10 ModEventHandlerRenderer akimbo/left handed support:
 * <ul>
 * <li>{@link #applyArmPose}: onRenderHeldGun/onRenderArmorEvent arm yaw overrides
 * (left arm 0.1 + head yaw, left handed: right arm -0.5 + head yaw), called at the end of
 * HumanoidModel#setupAnim so the body, sleeves and armor all use it. The original hid and re-rendered
 * the arm in SetArmorModel only to apply this rotation after setRotationAngles.</li>
 * <li>{@link AkimboLayer}: onRenderAkimbo (RenderPlayerEvent.Specials.Pre), the left hand gun on the
 * left arm.</li>
 * </ul>
 */
public final class GunAkimboRenderHandler {

	private GunAkimboRenderHandler() {
	}

	/** MinecraftForgeClient.getItemRenderer(held, EQUIPPED) instanceof ItemRenderWeaponBase */
	public static ItemRenderWeaponBase weaponRenderer(ItemStack held) {
		if(held.isEmpty() || !(held.getItem() instanceof ItemGunBaseNT)) return null;
		BlockEntityWithoutLevelRenderer renderer = IClientItemExtensions.of(held).getCustomRenderer();
		return renderer instanceof ItemRenderWeaponBase weapon ? weapon : null;
	}

	/** Called by HumanoidModelGunPoseMixin at the tail of HumanoidModel#setupAnim. */
	public static void applyArmPose(HumanoidModel<?> model, LivingEntity entity) {
		if(!(entity instanceof Player player)) return; // RenderPlayer only
		ItemRenderWeaponBase renderGun = weaponRenderer(player.getMainHandItem());
		if(renderGun == null) return;
		ModelPart head = model.head;
		if(renderGun.isAkimbo(player)) {
			model.leftArm.yRot = 0.1F + head.yRot;
		}
		if(renderGun.isLeftHanded()) {
			model.leftArm.yRot = 0.1F + head.yRot;
			model.rightArm.yRot = -0.5F + head.yRot;
		}
	}

	@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModEvents {

		private ModEvents() {
		}

		@SubscribeEvent
		public static void addLayers(EntityRenderersEvent.AddLayers event) {
			for(PlayerSkin.Model skin : event.getSkins()) {
				EntityRenderer<? extends Player> renderer = event.getSkin(skin);
				if(renderer instanceof PlayerRenderer playerRenderer) {
					playerRenderer.addLayer(new AkimboLayer(playerRenderer));
				}
			}
		}
	}

	/** onRenderAkimbo */
	public static final class AkimboLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

		public AkimboLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
				float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
			ItemStack held = player.getMainHandItem();
			ItemRenderWeaponBase renderWeapon = weaponRenderer(held);
			if(renderWeapon == null) return;
			if(renderWeapon.isAkimbo(player) || renderWeapon.isLeftHanded()) {
				poseStack.pushPose();
				// bipedLeftArm.postRender(0.0625F)
				this.getParentModel().translateToHand(HumanoidArm.LEFT, poseStack);
				renderWeapon.renderAkimboOnLeftArm(held, player, poseStack, buffer, packedLight);
				poseStack.popPose();
			}
		}
	}
}

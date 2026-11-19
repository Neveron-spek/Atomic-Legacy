package com.atomiclegacy.client.gun;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 1.7.10 ModEventHandlerClient#onOverlayRender "HANDLE SCOPE OVERLAY" and RenderScreenOverlay#renderScope. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class GunScopeOverlay {

	private GunScopeOverlay() {
	}

	@SubscribeEvent
	public static void onOverlay(RenderGuiLayerEvent.Pre event) {
		if(!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
		Player player = Minecraft.getInstance().player;
		if(player == null) return;
		ItemStack held = player.getMainHandItem();

		if(held.getItem() instanceof ItemGunBaseNT gun && ItemGunBaseNT.aimingProgress == ItemGunBaseNT.prevAimingProgress && ItemGunBaseNT.aimingProgress == 1F) {
			GunConfig cfg = gun.getConfig(held, 0);
			if(cfg.getScopeTexture(held) != null) {
				renderScope(event.getGuiGraphics(), cfg.getScopeTexture(held));
			}
		}
	}

	public static void renderScope(GuiGraphics graphics, ResourceLocation tex) {

		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, tex);

		double w = graphics.guiWidth();
		double h = graphics.guiHeight();

		double smallest = Math.min(w, h);
		double divisor = smallest / (9D / 16D);
		smallest = 9D / 16D;
		double largest = Math.max(w, h) / divisor;

		double hMin = h < w ? 0.5 - smallest / 2D : 0.5 - largest / 2D;
		double hMax = h < w ? 0.5 + smallest / 2D : 0.5 + largest / 2D;
		double wMin = w < h ? 0.5 - smallest / 2D : 0.5 - largest / 2D;
		double wMax = w < h ? 0.5 + smallest / 2D : 0.5 + largest / 2D;

		// 1.7.10 depth -300 in the overlay projection (z far behind the GUI); kept as the bottom-most GUI layer.
		float depth = -300F;
		Matrix4f m = graphics.pose().last().pose();
		BufferBuilder tess = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		tess.addVertex(m, 0, (float) h, depth).setUv((float) wMin, (float) hMax);
		tess.addVertex(m, (float) w, (float) h, depth).setUv((float) wMax, (float) hMax);
		tess.addVertex(m, (float) w, 0, depth).setUv((float) wMax, (float) hMin);
		tess.addVertex(m, 0, 0, depth).setUv((float) wMin, (float) hMin);
		BufferUploader.drawWithShader(tess.buildOrThrow());

		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}

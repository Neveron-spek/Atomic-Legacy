package com.atomiclegacy.render;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.util.ShadyUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * [parity j2] com.hbm.render.util.RenderAccessoryUtility#getCloakFromPlayer and ModEventHandlerClient#preRenderEvent
 * (RenderPlayerEvent.Pre). The wings / axe pack / tail of onRenderArmorEvent are drawn by LegacyAccessoryArmorLayer;
 * the cape location is swapped by mixin.client.AbstractClientPlayerCapeMixin.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class RenderAccessoryUtility {

	private RenderAccessoryUtility() {
	}

	private static final Map<String, ResourceLocation> CAPES = new HashMap<>();
	static {
		for(String cape : ShadyUtil.CAPES) CAPES.put(cape, AtomicLegacy.id("textures/models/capes/" + cape + ".png"));
	}
	/** ModEventHandlerClient: textures/particle/fart.png. */
	private static final ResourceLocation FART = AtomicLegacy.id("textures/particle/fart.png");

	/** MainRegistry.polaroidID == 11. */
	private static boolean polaroid11() {
		return com.atomiclegacy.AtomicLegacy.polaroidID == 11;
	}

	public static ResourceLocation getCloakFromPlayer(Player player) {
		String cape = ShadyUtil.capeFor(player.getUUID().toString(), player.getGameProfile().getName(), polaroid11());
		return cape == null ? null : CAPES.get(cape);
	}

	@SubscribeEvent
	public static void preRenderEvent(RenderPlayerEvent.Pre event) {
		Player player = event.getEntity();

		MobEffectInstance invis = player.getEffect(MobEffects.INVISIBILITY);
		if(invis != null && invis.getAmplifier() > 0) event.setCanceled(true);

		if(player.getGameProfile().getName().toLowerCase(Locale.US).equals("martmn")) {
			event.setCanceled(true);
			Player me = Minecraft.getInstance().player;
			if(me == null) return;

			// glTranslatef(pX - mX, pY - mY + 0.75 - getYOffset(), pZ - mZ): 1.25 above the feet of the rendered player
			// (EntityOtherPlayerMP getYOffset -0.5; the client player's posY is at eye height with getYOffset 1.12)
			PoseStack pose = event.getPoseStack();
			pose.pushPose();
			pose.translate(0.0F, 1.25F, 0.0F);
			// glRotatef(-me.rotationYaw, Y), glRotatef(me.rotationPitch, X) on top of the camera rotation
			pose.mulPose(Axis.YP.rotationDegrees(-me.getYRot()));
			pose.mulPose(Axis.XP.rotationDegrees(me.getXRot()));
			// GL_LIGHTING and GL_CULL_FACE off, brightness 240 (unlit texture draw), one quad as two triangles
			VertexConsumer t = event.getMultiBufferSource().getBuffer(HbmUnlitRenderTypes.opaque(FART));
			var m = pose.last().pose();
			float[][] quad = {{-1, 1, 0, 0}, {1, 1, 1, 0}, {1, -1, 1, 1}, {-1, -1, 0, 1}};
			for(int i : new int[] {0, 1, 2, 0, 2, 3}) {
				t.addVertex(m, quad[i][0], quad[i][1], 0.0F).setUv(quad[i][2], quad[i][3]).setColor(0xFFFFFFFF);
			}
			pose.popPose();
		}
	}
}

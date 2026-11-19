package com.atomiclegacy.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix4f;

/**
 * [weapons batch F2] ItemRenderFatMan.renderBalefire glint layers: textures/misc/glintBF.png drawn over the MiniNuke with
 * glDepthFunc(GL_EQUAL), glBlendFunc(GL_SRC_COLOR, GL_ONE), no depth write, no lighting / lightmap and a texture matrix
 * of scale 2, rotation 30 - k * 60 and translation (0, offset * (0.001 + k * 0.003) * -6) per layer k, tinted
 * (0, 0.8, 0.15) * 0.76. The vanilla entity glint shader is the same GL setup (EQUAL depth test, GLINT_TRANSPARENCY).
 */
public final class BalefireGlintRenderType extends RenderType {

	static final ResourceLocation GLINT_BF = AtomicLegacy.id("textures/misc/glintbf.png");

	/** mc.thePlayer.ticksExisted + interp, set right before the layers are drawn. */
	public static float offset;

	private static final RenderType[] LAYERS = new RenderType[3];

	static {
		for(int k = 0; k < 3; k++) {
			final int layer = k;
			LAYERS[k] = create("hbm_balefire_glint_" + k, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
					CompositeState.builder()
							.setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
							.setTextureState(new RenderStateShard.TextureStateShard(GLINT_BF, false, false))
							.setWriteMaskState(COLOR_WRITE)
							.setCullState(NO_CULL)
							.setDepthTestState(EQUAL_DEPTH_TEST)
							.setTransparencyState(GLINT_TRANSPARENCY)
							.setTexturingState(new RenderStateShard.TexturingStateShard("hbm_balefire_glint_" + k, () -> setup(layer), () -> {
								RenderSystem.resetTextureMatrix();
								RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
							}))
							.createCompositeState(false));
		}
	}

	private BalefireGlintRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
		super(name, format, mode, size, crumbling, sort, setup, clear);
	}

	private static void setup(int k) {
		float scale = 2F;
		float r = 0F;
		float g = 0.8F;
		float b = 0.15F;
		float speed = -6;
		float glintColor = 0.76F;
		float movement = offset * (0.001F + (float) k * 0.003F) * speed;
		// glScalef(scale); glRotatef(30 - k * 60, 0, 0, 1); glTranslatef(0, movement, 0)
		Matrix4f matrix = new Matrix4f().scale(scale, scale, scale).rotateZ((float) Math.toRadians(30.0F - k * 60.0F)).translate(0F, movement, 0F);
		RenderSystem.setTextureMatrix(matrix);
		RenderSystem.setShaderColor(r * glintColor, g * glintColor, b * glintColor, 1.0F);
	}

	public static RenderType layer(int k) {
		return LAYERS[k];
	}
}

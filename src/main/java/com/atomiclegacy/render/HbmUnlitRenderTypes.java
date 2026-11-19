package com.atomiclegacy.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render types matching 1.7.10 Tessellator draws done with GL_LIGHTING disabled:
 * texture * vertex colour, no diffuse shading, no lightmap.
 * <ul>
 * <li>{@link #opaque} — no blending, depth write (e.g. the black hole core sphere)</li>
 * <li>{@link #blend} — glBlendFunc(770, 771) with glDepthMask(false)</li>
 * <li>{@link #additive} — glBlendFunc(GL_SRC_ALPHA, GL_ONE) with glDepthMask(false)</li>
 * <li>{@link #ADDITIVE_COLOR} — same, GL_TEXTURE_2D disabled</li>
 * </ul>
 */
public final class HbmUnlitRenderTypes extends RenderType {

	private static final ShaderStateShard TEX_COLOR_SHADER = new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
	private static final Map<ResourceLocation, RenderType> OPAQUE = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, RenderType> BLEND = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, RenderType> ADDITIVE = new ConcurrentHashMap<>();

	public static final RenderType ADDITIVE_COLOR = create("hbm_unlit_additive_color",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
			CompositeState.builder()
					.setShaderState(POSITION_COLOR_SHADER)
					.setTransparencyState(LIGHTNING_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false));

	/** [parity j2] ModelArmorWingsPheo#renderFlame: GL_TRIANGLES, GL_TEXTURE_2D / GL_LIGHTING off, blend SRC_ALPHA ONE, depth write left on. */
	public static final RenderType ADDITIVE_COLOR_TRIANGLES = create("hbm_unlit_additive_color_triangles",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 1536, false, true,
			CompositeState.builder()
					.setShaderState(POSITION_COLOR_SHADER)
					.setTransparencyState(LIGHTNING_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.createCompositeState(false));

	/** [parity z1] GL_TEXTURE_2D and GL_LIGHTING disabled, no blending, depth write (armor lamps: envsuit Lamps, no9 Flame). */
	public static final RenderType OPAQUE_COLOR = create("hbm_unlit_opaque_color",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 1536, false, false,
			CompositeState.builder()
					.setShaderState(POSITION_COLOR_SHADER)
					.setTransparencyState(NO_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.createCompositeState(false));

	/** LegoClient.renderBulletStandard: opaque untextured quads, no culling, world lightmap (or 240 when fullbright). */
	public static final RenderType TRACER = create("hbm_tracer",
			DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, false,
			CompositeState.builder()
					.setShaderState(POSITION_COLOR_LIGHTMAP_SHADER)
					.setLightmapState(LIGHTMAP)
					.setTransparencyState(NO_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.createCompositeState(false));

	private HbmUnlitRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
		super(name, format, mode, size, crumbling, sort, setup, clear);
	}

	public static RenderType opaque(ResourceLocation texture) {
		return OPAQUE.computeIfAbsent(texture, tex -> create("hbm_unlit_opaque",
				DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES, 1536, false, false,
				CompositeState.builder()
						.setShaderState(TEX_COLOR_SHADER)
						.setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
						.setTransparencyState(NO_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setWriteMaskState(COLOR_DEPTH_WRITE)
						.createCompositeState(false)));
	}

	public static RenderType blend(ResourceLocation texture) {
		return BLEND.computeIfAbsent(texture, tex -> create("hbm_unlit_blend",
				DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
				CompositeState.builder()
						.setShaderState(TEX_COLOR_SHADER)
						.setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setWriteMaskState(COLOR_WRITE)
						.createCompositeState(false)));
	}

	public static RenderType additive(ResourceLocation texture) {
		return ADDITIVE.computeIfAbsent(texture, tex -> create("hbm_unlit_additive",
				DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
				CompositeState.builder()
						.setShaderState(TEX_COLOR_SHADER)
						.setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
						.setTransparencyState(LIGHTNING_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setWriteMaskState(COLOR_WRITE)
						.createCompositeState(false)));
	}
}

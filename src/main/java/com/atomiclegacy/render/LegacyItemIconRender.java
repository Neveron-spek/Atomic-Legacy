package com.atomiclegacy.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;

import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * [parity U4: item renderers] 1.7.10 drawing helpers for item icons: ItemRenderer#renderItemIn2D (the extruded held
 * icon), RenderItem#renderIcon (the flat slot icon) and the two glint variants of ItemRenderTransformer /
 * ItemRendererMeteorSword (renderGlintFlat: DST_ALPHA/ONE over the slot quad; renderGlint3D: SRC_COLOR/ONE with a
 * texture matrix over the extruded icon), both GL_EQUAL depth tests over the icon just drawn.
 */
public final class LegacyItemIconRender extends RenderType {

    /** RenderMiscEffects.glint (hbm:textures/misc/glint.png). */
    static final ResourceLocation GLINT = AtomicLegacy.id("textures/misc/glint.png");

    /** RenderItem#renderIcon with GL_LIGHTING disabled: texture * colour, blended, depth written. */
    static final RenderType FLAT_ICON = create("hbm_legacy_flat_icon", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    /** OpenGlHelper.glBlendFunc(772, 1, 0, 0): GL_DST_ALPHA, GL_ONE. */
    private static final TransparencyStateShard DST_ALPHA_ONE = new TransparencyStateShard("hbm_dst_alpha_one", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ZERO);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    private static final RenderType FLAT_GLINT = create("hbm_legacy_flat_glint", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(GLINT, false, false))
                    .setTransparencyState(DST_ALPHA_ONE)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final Map<String, RenderType> GLINT_3D = new ConcurrentHashMap<>();

    private LegacyItemIconRender(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
                                 boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    /** renderGlint3D pass {@code pass} tinted (r, g, b) (already multiplied by the renderer's intensity). */
    private static RenderType glint3D(int pass, float r, float g, float b) {
        String key = pass + "_" + r + "_" + g + "_" + b;
        return GLINT_3D.computeIfAbsent(key, k -> create("hbm_legacy_glint3d_" + pass + "_" + r + "_" + g + "_" + b,
                DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
                        .setTextureState(new TextureStateShard(GLINT, false, false))
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(EQUAL_DEPTH_TEST)
                        .setTransparencyState(GLINT_TRANSPARENCY)
                        .setTexturingState(new TexturingStateShard("hbm_legacy_glint3d_" + pass, () -> {
                            float scale = 0.125F;
                            Matrix4f matrix = new Matrix4f().scale(scale, scale, scale);
                            if (pass == 0) {
                                float offset = (float) (Util.getMillis() % 3000L) / 3000.0F * 8.0F;
                                matrix.translate(offset, 0.0F, 0.0F).rotateZ((float) Math.toRadians(-50.0F));
                            } else {
                                float offset = (float) (Util.getMillis() % 4873L) / 4873.0F * 8.0F;
                                matrix.translate(-offset, 0.0F, 0.0F).rotateZ((float) Math.toRadians(10.0F));
                            }
                            RenderSystem.setTextureMatrix(matrix);
                            RenderSystem.setShaderColor(r, g, b, 1.0F);
                        }, () -> {
                            RenderSystem.resetTextureMatrix();
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        }))
                        .createCompositeState(false)));
    }

    /** Receives one vertex of renderItemIn2D. */
    @FunctionalInterface
    interface Sink {
        void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz);
    }

    /** ItemRenderer#renderItemIn2D(tess, f1, f2, f3, f4, width, height, thickness). */
    static void renderItemIn2D(Sink t, float f1, float f2, float f3, float f4, int width, int height, float th) {
        t.vertex(0, 0, 0, f1, f4, 0, 0, 1);
        t.vertex(1, 0, 0, f3, f4, 0, 0, 1);
        t.vertex(1, 1, 0, f3, f2, 0, 0, 1);
        t.vertex(0, 1, 0, f1, f2, 0, 0, 1);

        t.vertex(0, 1, -th, f1, f2, 0, 0, -1);
        t.vertex(1, 1, -th, f3, f2, 0, 0, -1);
        t.vertex(1, 0, -th, f3, f4, 0, 0, -1);
        t.vertex(0, 0, -th, f1, f4, 0, 0, -1);

        float f5 = 0.5F * (f1 - f3) / (float) width;
        float f6 = 0.5F * (f4 - f2) / (float) height;
        int k;
        float f7;
        float f8;
        float f9;

        for (k = 0; k < width; ++k) {
            f7 = (float) k / (float) width;
            f8 = f1 + (f3 - f1) * f7 - f5;
            t.vertex(f7, 0, -th, f8, f4, -1, 0, 0);
            t.vertex(f7, 0, 0, f8, f4, -1, 0, 0);
            t.vertex(f7, 1, 0, f8, f2, -1, 0, 0);
            t.vertex(f7, 1, -th, f8, f2, -1, 0, 0);
        }

        for (k = 0; k < width; ++k) {
            f7 = (float) k / (float) width;
            f8 = f1 + (f3 - f1) * f7 - f5;
            f9 = f7 + 1.0F / (float) width;
            t.vertex(f9, 1, -th, f8, f2, 1, 0, 0);
            t.vertex(f9, 1, 0, f8, f2, 1, 0, 0);
            t.vertex(f9, 0, 0, f8, f4, 1, 0, 0);
            t.vertex(f9, 0, -th, f8, f4, 1, 0, 0);
        }

        for (k = 0; k < height; ++k) {
            f7 = (float) k / (float) height;
            f8 = f4 + (f2 - f4) * f7 - f6;
            f9 = f7 + 1.0F / (float) height;
            t.vertex(0, f9, 0, f1, f8, 0, 1, 0);
            t.vertex(1, f9, 0, f3, f8, 0, 1, 0);
            t.vertex(1, f9, -th, f3, f8, 0, 1, 0);
            t.vertex(0, f9, -th, f1, f8, 0, 1, 0);
        }

        for (k = 0; k < height; ++k) {
            f7 = (float) k / (float) height;
            f8 = f4 + (f2 - f4) * f7 - f6;
            t.vertex(1, f7, 0, f3, f8, 0, -1, 0);
            t.vertex(0, f7, 0, f1, f8, 0, -1, 0);
            t.vertex(0, f7, -th, f1, f8, 0, -1, 0);
            t.vertex(1, f7, -th, f3, f8, 0, -1, 0);
        }
    }

    /**
     * renderItemIn2D(tess, icon.getMaxU(), icon.getMinV(), icon.getMinU(), icon.getMaxV(), w, h, 0.0625) with world
     * lighting. entityCutoutNoCull: not one of the fixed (deferred) sheet buffers, so the icon is drawn before a glint
     * pass that depth-tests GL_EQUAL against it.
     */
    static void renderHeldIcon(TextureAtlasSprite icon, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose last = pose.last();
        renderItemIn2D((x, y, z, u, v, nx, ny, nz) -> consumer.addVertex(last, x, y, z).setColor(255, 255, 255, 255)
                        .setUv(u, v).setOverlay(overlay == 0 ? OverlayTexture.NO_OVERLAY : overlay).setLight(light).setNormal(last, nx, ny, nz),
                icon.getU1(), icon.getV0(), icon.getU0(), icon.getV1(), icon.contents().width(), icon.contents().height(), 0.0625F);
    }

    /** renderGlint3D(tess, 0.0625): two passes of renderItemIn2D(0, 0, 1, 1, 255, 255) through the texture matrix. */
    static void renderGlint3D(PoseStack pose, MultiBufferSource buffers, float r, float g, float b) {
        PoseStack.Pose last = pose.last();
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer consumer = buffers.getBuffer(glint3D(pass, r, g, b));
            renderItemIn2D((x, y, z, u, v, nx, ny, nz) -> consumer.addVertex(last, x, y, z).setUv(u, v),
                    0.0F, 0.0F, 1.0F, 1.0F, 255, 255, 0.0625F);
        }
    }

    /** RenderItem#renderIcon(x, y, icon, w, h) with the given colour (alpha for ItemRendererHot). */
    static void renderFlatIcon(TextureAtlasSprite icon, PoseStack pose, MultiBufferSource buffers, int x, int y, int w, int h, float alpha) {
        VertexConsumer t = buffers.getBuffer(FLAT_ICON);
        Matrix4f m = pose.last().pose();
        int a = Math.round(alpha * 255.0F);
        t.addVertex(m, x, y + h, 0).setUv(icon.getU0(), icon.getV1()).setColor(255, 255, 255, a);
        t.addVertex(m, x + w, y + h, 0).setUv(icon.getU1(), icon.getV1()).setColor(255, 255, 255, a);
        t.addVertex(m, x + w, y, 0).setUv(icon.getU1(), icon.getV0()).setColor(255, 255, 255, a);
        t.addVertex(m, x, y, 0).setUv(icon.getU0(), icon.getV0()).setColor(255, 255, 255, a);
    }

    /** renderGlintFlat of ItemRenderTransformer / ItemRendererMeteorSword (colour already multiplied by the intensity). */
    static void renderGlintFlat(PoseStack pose, MultiBufferSource buffers, float r, float g, float b) {
        VertexConsumer tessellator = buffers.getBuffer(FLAT_GLINT);
        Matrix4f m = pose.last().pose();
        int cr = Math.round(r * 255.0F);
        int cg = Math.round(g * 255.0F);
        int cb = Math.round(b * 255.0F);
        for (int j1 = 0; j1 < 2; ++j1) {
            float scaleU = 0.00390625F;
            float scaleV = 0.00390625F;
            float anim = (float) (Util.getMillis() % (long) (3000 + j1 * 1873)) / (3000.0F + (float) (j1 * 1873)) * 256.0F;
            float offsetV = 0.0F;
            float sizeMultU = 4.0F;
            if (j1 == 1) {
                sizeMultU = -1.0F;
            }
            int x = 0;
            int sizeX = 16;
            int y = 0;
            int sizeY = 16;
            int zLevel = 0;
            tessellator.addVertex(m, x + 0, y + sizeY, zLevel).setUv((anim + sizeY * sizeMultU) * scaleU, (offsetV + sizeY) * scaleV).setColor(cr, cg, cb, 255);
            tessellator.addVertex(m, x + sizeX, y + sizeY, zLevel).setUv((anim + sizeX + sizeY * sizeMultU) * scaleU, (offsetV + sizeY) * scaleV).setColor(cr, cg, cb, 255);
            tessellator.addVertex(m, x + sizeX, y + 0, zLevel).setUv((anim + sizeX) * scaleU, (offsetV + 0.0F) * scaleV).setColor(cr, cg, cb, 255);
            tessellator.addVertex(m, x + 0, y + 0, zLevel).setUv((anim + 0.0F) * scaleU, (offsetV + 0.0F) * scaleV).setColor(cr, cg, cb, 255);
        }
    }
}

package com.atomiclegacy.render;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyHotItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * [parity U4: item renderers] The 1.7.10 renderers that draw the item's own icon: ItemRenderTransformer (the "universal
 * JSON translated" swords and tools), ItemRendererMeteorSword (tinted glint) and ItemRendererHot (glowing overlay).
 */
public final class LegacyIconItemRenderers {

    private LegacyIconItemRenderers() {}

    /** entity.getItemIcon(item, 0) / item.getIconIndex: the item model's layer0 sprite. */
    static TextureAtlasSprite icon(ItemStack stack) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(stack);
        if (model instanceof LegacyItemModelWrapper wrapper) model = wrapper.original();
        return model.getParticleIcon();
    }

    /** ItemRenderTransformer(rtp, ttp, stp, rfp, tfp, sfp, rir, tir, sir). */
    public static final class Transformer extends LegacyIItemRenderer {
        private final double[] rtp, ttp, stp, rfp, tfp, sfp, rir, tir, sir;

        public Transformer(double[] rtp, double[] ttp, double[] stp, double[] rfp, double[] tfp, double[] sfp, double[] rir, double[] tir, double[] sir) {
            this.rtp = rtp;
            this.ttp = ttp;
            this.stp = stp;
            this.rfp = rfp;
            this.tfp = tfp;
            this.sfp = sfp;
            this.rir = rir;
            this.tir = tir;
            this.sir = sir;
        }

        @Override public boolean handleRenderType(ItemStack item, Type type) { return type != Type.ENTITY; }
        @Override public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }
        /** ItemSword / ItemTool: bFull3D. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            switch (type) {
                case EQUIPPED_FIRST_PERSON -> {
                    glRotate(pose, rfp[1], 0, 1, 0);
                    glRotate(pose, rfp[2], 0, 0, 1);
                    glRotate(pose, rfp[0], 1, 0, 0);
                    pose.translate(0.5, 0.5, 0);
                    pose.translate(-0.5, -0.5, 0);
                    glRotate(pose, 180, 0, 1, 0);
                    glRotate(pose, -90, 0, 0, 1);
                    pose.translate(0.5, 0.5, 0);
                    pose.scale((float) (sfp[0] * 2), (float) (sfp[1] * 2), (float) (sfp[2] * 2));
                    pose.translate(-0.5, -0.5, 0.25);
                }
                case EQUIPPED -> {
                    glRotate(pose, rtp[1], 0, 1, 0);
                    glRotate(pose, rtp[2], 0, 0, 1);
                    glRotate(pose, rtp[0], 1, 0, 0);
                    pose.translate(ttp[0], ttp[1], ttp[2]);
                    glRotate(pose, 45, 0, 0, 1);
                    glRotate(pose, 180, 0, 1, 0);
                    glRotate(pose, -45, 0, 0, 1);
                    pose.scale((float) stp[0], (float) stp[1], (float) stp[2]);
                }
                case INVENTORY -> {
                    glRotate(pose, rir[0], 1, 0, 0);
                    glRotate(pose, rir[1], 0, 1, 0);
                    glRotate(pose, rir[2], 0, 0, 1);
                    pose.translate(tir[0] * 0.0625, tir[1] * 0.0625, tir[2] * 0.0625);
                    pose.translate(8, 8, 0);
                    pose.scale((float) sir[0], (float) sir[1], (float) sir[2]);
                    pose.translate(-8, -8, 0);
                }
                default -> { }
            }
            float in = 0.76F;
            if (type == Type.EQUIPPED || type == Type.EQUIPPED_FIRST_PERSON) { // data[1] instanceof EntityLivingBase
                LegacyItemIconRender.renderHeldIcon(icon(item), pose, buffers, light, overlay);
                if (item.hasFoil()) LegacyItemIconRender.renderGlint3D(pose, buffers, 0.5F * in, 0.25F * in, 0.8F * in);
            } else {
                // RenderItemStack.renderItemStackNoEffect(0, 0, 0, item); the pick-up pop is applied by GuiGraphics
                LegacyItemIconRender.renderFlatIcon(icon(item), pose, buffers, 0, 0, 16, 16, 1.0F);
                if (item.hasFoil()) LegacyItemIconRender.renderGlintFlat(pose, buffers, 0.5F * in, 0.25F * in, 0.8F * in);
            }
        }
    }

    /** ItemRendererMeteorSword(r, g, b): always-on glint tinted (r, g, b) * 0.36. */
    public static final class MeteorSword extends LegacyIItemRenderer {
        private final float r, g, b;

        public MeteorSword(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override public boolean handleRenderType(ItemStack stack, Type type) { return type != Type.ENTITY; }
        @Override public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }
        /** ItemSwordMeteorite extends ItemSwordAbility extends ItemSword. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            switch (type) {
                case EQUIPPED_FIRST_PERSON -> {
                    glRotate(pose, 180, 0, 1, 0);
                    glRotate(pose, -90, 0, 0, 1);
                    pose.translate(0.5, 0.5, 0);
                    pose.translate(-0.5, -0.5, 0);
                    glRotate(pose, 180, 0, 1, 0);
                    glRotate(pose, -90, 0, 0, 1);
                    pose.translate(0.5, 0.5, 0);
                    pose.scale((float) (1.36 * 2), (float) (1.36 * 2), (float) (0.68 * 2));
                    pose.translate(-0.5, -0.5, 0.25);
                }
                case EQUIPPED -> {
                    glRotate(pose, 180, 0, 1, 0);
                    glRotate(pose, -90, 0, 0, 1);
                    pose.translate(0.2, 0.55, 0);
                    glRotate(pose, 45, 0, 0, 1);
                    glRotate(pose, 180, 0, 1, 0);
                    glRotate(pose, -45, 0, 0, 1);
                    pose.scale(1.7F, 1.7F, 0.85F);
                }
                default -> { }
            }
            float in = 0.36F;
            if (type == Type.EQUIPPED || type == Type.EQUIPPED_FIRST_PERSON) { // data[1] instanceof EntityLivingBase
                LegacyItemIconRender.renderHeldIcon(icon(item), pose, buffers, light, overlay);
                LegacyItemIconRender.renderGlint3D(pose, buffers, r * in, g * in, b * in);
            } else {
                LegacyItemIconRender.renderFlatIcon(icon(item), pose, buffers, 0, 0, 16, 16, 1.0F);
                LegacyItemIconRender.renderGlintFlat(pose, buffers, r * in, g * in, b * in);
            }
            pose.popPose();
        }
    }

    /** ItemRendererHot: the slot icon plus ItemHot#hotIcon ("<texture>_hot") with alpha ItemHot.getHeat. */
    public static final class Hot extends LegacyIItemRenderer {
        /** ItemHot.heat is a static field: the value of the last constructed ItemHot, ingot_chainsteel's 100. */
        private static final double LEGACY_STATIC_HEAT = 100.0D;

        @Override public boolean handleRenderType(ItemStack stack, Type type) { return type == Type.INVENTORY; }
        @Override public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            LegacyItemIconRender.renderFlatIcon(icon(item), pose, buffers, 0, 0, 16, 16, 1.0F);
            double h = item.getItem() instanceof LegacyHotItem hot ? hot.heat(item) / LEGACY_STATIC_HEAT : 0.0D;
            if (h > 0) {
                TextureAtlasSprite hotIcon = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(AtomicLegacy.id("item/" + BuiltInRegistries.ITEM.getKey(item.getItem()).getPath() + "_hot"));
                // GL11.glColor4f(1, 1, 1, h): GL clamps the colour to [0, 1]
                LegacyItemIconRender.renderFlatIcon(hotIcon, pose, buffers, 0, 0, 16, 16, (float) Math.min(1.0D, h));
            }
        }
    }
}

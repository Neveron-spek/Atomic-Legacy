package com.atomiclegacy.render;

import java.util.Random;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.AmmoHimarsItem;
import com.atomiclegacy.items.CustomMissileData;
import com.atomiclegacy.items.HbmToolItems;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * [parity U4: item renderers] 1.7.10 IItemRenderers that draw a model: OBJ (ItemRenderShim, ItemRenderGavel,
 * ItemRenderChainsaw, ItemRenderDetonatorLaser, the ItemRenderLibrary entries of boxcar, the industrial generator,
 * the sawblade and the HIMARS rockets, ItemRenderMissileGeneric, ItemRenderMissile) and the ModelBase box models
 * (ItemRenderBigSword, ItemRenderRedstoneSword, ItemRenderDecoBlock).
 */
public final class LegacyModelItemRenderers extends RenderType {

    private LegacyModelItemRenderers(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
                                     boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    /** GL_LIGHTING and GL_TEXTURE_2D disabled, glColor only (ItemRenderDetonatorLaser lights and wave), no culling. */
    static final RenderType UNLIT_COLOR = create("hbm_legacy_unlit_color", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    private static LegacyEntityObjMesh mesh(String path) {
        return LegacyEntityObjMesh.load(AtomicLegacy.id(path));
    }

    private static ResourceLocation tex(String path) {
        return AtomicLegacy.id("textures/" + path);
    }

    /** GL_CULL_FACE as the original left it: enabled -> entityCutout, disabled / RendererLivingEntity default -> no cull. */
    private static RenderType type(ResourceLocation texture, boolean cull) {
        return cull ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
    }

    // ------------------------------------------------------------------ OBJ weapons

    /** ItemRenderShim: shimmer_sledge, shimmer_axe, stopsign, sopsign, chernobylsign. */
    public static final class Shim extends LegacyIItemRenderer {
        /** ResourceManager.shimmer_sledge / shimmer_axe (models/shimmer_*.obj), stopsign (models/weapons/stopsign.obj). */
        private static final LegacyEntityObjMesh shimmer_sledge = mesh("models/weapons/shimmer_sledge.obj");
        private static final LegacyEntityObjMesh shimmer_axe = mesh("models/weapons/shimmer_axe.obj");
        private static final LegacyEntityObjMesh stopsign = mesh("models/weapons/stopsign.obj");
        private static final ResourceLocation shimmer_sledge_tex = tex("models/shimmer_sledge.png");
        private static final ResourceLocation shimmer_axe_tex = tex("models/shimmer_axe.png");
        private static final ResourceLocation stopsign_tex = tex("models/weapons/stopsign.png");
        private static final ResourceLocation sopsign_tex = tex("models/weapons/sopsign.png");
        private static final ResourceLocation chernobylsign_tex = tex("models/weapons/chernobylsign.png");

        @Override
        public boolean handleRenderType(ItemStack item, Type type) {
            return type == Type.EQUIPPED || type == Type.EQUIPPED_FIRST_PERSON || type == Type.ENTITY;
        }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return type == Type.ENTITY && (helper == Helper.ENTITY_BOBBING || helper == Helper.ENTITY_ROTATION);
        }

        /** WeaponSpecial extends ItemSword. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            Item it = item.getItem();
            boolean sign = it == HbmItems.STOPSIGN.get() || it == HbmItems.SOPSIGN.get() || it == HbmItems.CHERNOBYLSIGN.get();
            boolean shimmer = it == HbmItems.SHIMMER_SLEDGE.get() || it == HbmItems.SHIMMER_AXE.get();
            pose.pushPose();
            if (type == Type.EQUIPPED_FIRST_PERSON && sign) {
                glRotate(pose, 180, 0.0F, 1.0F, 0.0F);
                glRotate(pose, -90.0F, 0.0F, 0.0F, 1.0F);
                pose.translate(-1.0F, -1.5F, 0.0F);
            }
            ResourceLocation texture = null;
            if (it == HbmItems.SHIMMER_SLEDGE.get()) texture = shimmer_sledge_tex;
            if (it == HbmItems.SHIMMER_AXE.get()) texture = shimmer_axe_tex;
            if (it == HbmItems.STOPSIGN.get()) texture = stopsign_tex;
            if (it == HbmItems.SOPSIGN.get()) texture = sopsign_tex;
            if (it == HbmItems.CHERNOBYLSIGN.get()) texture = chernobylsign_tex;
            if (shimmer) {
                glRotate(pose, -135.0F, 0.0F, 0.0F, 1.0F);
                glRotate(pose, 180F, 0.0F, 0.0F, 1.0F);
                pose.scale(1.5F, 1.5F, 1.5F);
                pose.translate(0.45F, -0.3F, 0.0F);
            }
            if (sign) {
                glRotate(pose, 45.0F, 0.0F, 0.0F, 1.0F);
                pose.scale(0.35F, 0.35F, 0.35F);
                pose.translate(2.0F, -2.0F, 0.0F);
                glRotate(pose, 90F, 0.0F, 1.0F, 0.0F);
            }
            if (texture != null) {
                VertexConsumer consumer = buffers.getBuffer(type(texture, false));
                if (it == HbmItems.SHIMMER_SLEDGE.get()) shimmer_sledge.renderAll(pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
                if (it == HbmItems.SHIMMER_AXE.get()) shimmer_axe.renderAll(pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
                if (sign) stopsign.renderAll(pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            }
            pose.popPose();
        }
    }

    /** ItemRenderGavel: wood_gavel, lead_gavel, diamond_gavel, mese_gavel. */
    public static final class Gavel extends LegacyIItemRenderer {
        private static final LegacyEntityObjMesh gavel = mesh("models/weapons/gavel.obj");
        private static final ResourceLocation gavel_wood = tex("models/weapons/gavel_wood.png");
        private static final ResourceLocation gavel_lead = tex("models/weapons/gavel_lead.png");
        private static final ResourceLocation gavel_diamond = tex("models/weapons/gavel_diamond.png");
        private static final ResourceLocation gavel_mese = tex("models/weapons/gavel_mese.png");

        @Override
        public boolean handleRenderType(ItemStack item, Type type) {
            return type == Type.EQUIPPED || type == Type.EQUIPPED_FIRST_PERSON || type == Type.ENTITY;
        }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return type == Type.ENTITY && (helper == Helper.ENTITY_BOBBING || helper == Helper.ENTITY_ROTATION);
        }

        /** WeaponSpecial / ItemSwordAbility extend ItemSword. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            var player = Minecraft.getInstance().player;
            Item it = item.getItem();
            ResourceLocation texture = gavel_wood;
            if (it == HbmItems.LEAD_GAVEL.get()) texture = gavel_lead;
            if (it == HbmItems.DIAMOND_GAVEL.get()) texture = gavel_diamond;
            if (it == HbmItems.MESE_GAVEL.get()) texture = gavel_mese;
            boolean mese = it == HbmItems.MESE_GAVEL.get();
            switch (type) {
                case EQUIPPED_FIRST_PERSON -> {
                    pose.translate(1, 0.5, 0);
                    if (player != null && player.isBlocking()) {
                        pose.translate(-0.5, 0, 0);
                    }
                    glRotate(pose, 45, 0, 0, 1);
                    glRotate(pose, 90, 0, 1, 0);
                    if (mese) pose.scale(2, 2, 2);
                }
                case ENTITY, EQUIPPED -> {
                    if (type == Type.ENTITY) pose.translate(-0.5, 0, 0);
                    pose.scale(0.5F, 0.5F, 0.5F);
                    glRotate(pose, 45, 0, 0, 1);
                    pose.translate(1.375, 0, 0);
                    glRotate(pose, 90, 0, 1, 0);
                    if (mese) {
                        pose.scale(2, 2, 2);
                        pose.translate(0, 0.25, 0);
                    }
                }
                default -> { }
            }
            gavel.renderAll(pose, buffers.getBuffer(type(texture, false)), light, overlayOrNone(), 0xFFFFFFFF);
            pose.popPose();
        }
    }

    /** ItemRenderChainsaw. */
    public static final class Chainsaw extends LegacyIItemRenderer {
        private static final LegacyEntityObjMesh chainsaw = mesh("models/weapons/chainsaw.obj");
        private static final ResourceLocation chainsaw_tex = tex("models/weapons/chainsaw.png");

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return true; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return type == Type.ENTITY && (helper == Helper.ENTITY_ROTATION || helper == Helper.ENTITY_BOBBING);
        }

        /** ItemToolAbilityFueled extends ItemToolAbility extends ItemTool. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        /** player.isSwingInProgress = false in EQUIPPED_FIRST_PERSON. */
        @Override protected boolean cancelsFirstPersonSwing(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            var player = Minecraft.getInstance().player;
            switch (type) {
                case EQUIPPED_FIRST_PERSON -> {
                    double s0 = 0.35D;
                    pose.translate(0.5, 0.25, -0.25F);
                    glRotate(pose, 45, 0, 0, 1);
                    glRotate(pose, 80, 0, 1, 0);
                    pose.scale((float) s0, (float) s0, (float) s0);
                    if (player == null || !player.isBlocking()) {
                        double[] sRot = HbmAnimations.getRelevantTransformation("SWING_ROT");
                        double[] sTrans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
                        pose.translate(sTrans[0], sTrans[1], sTrans[2]);
                        glRotate(pose, sRot[2], 0, 0, 1);
                        glRotate(pose, sRot[1], 0, 1, 0);
                        glRotate(pose, sRot[0], 1, 0, 0);
                    }
                }
                case EQUIPPED -> {
                    double scale = -0.375D;
                    pose.scale((float) scale, (float) scale, (float) scale);
                    glRotate(pose, 85, 0, 1, 0);
                    glRotate(pose, 135D, 1.0D, 0.0D, 0.0D);
                    pose.translate(-0.125, -2.0, 1.75);
                }
                case ENTITY -> {
                    double s1 = 0.5D;
                    pose.scale((float) s1, (float) s1, (float) s1);
                }
                case INVENTORY -> {
                    double s = 4D;
                    pose.translate(8, 10, 0);
                    glRotate(pose, -90, 0, 1, 0);
                    glRotate(pose, -135, 1, 0, 0);
                    pose.scale((float) s, (float) s, (float) -s);
                }
            }
            // GL11.glEnable(GL11.GL_CULL_FACE)
            VertexConsumer consumer = buffers.getBuffer(type(chainsaw_tex, true));
            chainsaw.renderGroup("Saw", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            boolean operate = item.getItem() instanceof HbmToolItems.AbilityItem tool && tool.canOperate(item);
            for (int i = 0; i < 20; i++) {
                double run = operate ? System.currentTimeMillis() % 100D * 0.25D / 100D : 0.0625D;
                double forward = i * 0.25 + (run) - 2.0625;
                pose.pushPose();
                pose.translate(0, 0, 1.9375);
                pose.translate(0, 0.375, 0.5625);
                double angle = Mth.clamp(forward, 0, 0.25 * Math.PI);
                glRotate(pose, angle * 180D / (Math.PI * 0.25), 1, 0, 0);
                pose.translate(0, -0.375, -0.5625);
                if (forward < 0) pose.translate(0, 0, forward);
                if (forward > Math.PI * 0.25) pose.translate(0, 0, forward - Math.PI * 0.25);
                chainsaw.renderGroup("Tooth", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** ItemRenderDetonatorLaser. */
    public static final class DetonatorLaser extends LegacyIItemRenderer {
        private static final LegacyEntityObjMesh detonator_laser = mesh("models/weapons/detonator_laser.obj");
        private static final ResourceLocation detonator_laser_tex = tex("models/weapons/detonator_laser.png");

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return true; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return type == Type.ENTITY && (helper == Helper.ENTITY_ROTATION || helper == Helper.ENTITY_BOBBING);
        }

        /** ItemLaserDetonator: setFull3D(). */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            switch (type) {
                case EQUIPPED_FIRST_PERSON -> {
                    double s0 = 0.25D;
                    pose.scale((float) s0, (float) s0, (float) s0);
                    glRotate(pose, 80F, 0.0F, 1.0F, 0.0F);
                    glRotate(pose, -20F, 1.0F, 0.0F, 0.0F);
                    pose.translate(1.0F, 0.5F, 3.0F);
                }
                case EQUIPPED -> {
                    double scale = 0.125D;
                    pose.scale((float) -scale, (float) -scale, (float) -scale);
                    glRotate(pose, 85F, 0.0F, 1.0F, 0.0F);
                    glRotate(pose, 145F, 1.0F, 0.0F, 0.0F);
                    pose.translate(-0.5F, -1.0F, 6.5F);
                }
                case ENTITY -> {
                    double s1 = 0.25D;
                    glRotate(pose, -90F, 0.0F, 1.0F, 0.0F);
                    pose.scale((float) s1, (float) s1, (float) s1);
                }
                case INVENTORY -> {
                    double s = 3.5D;
                    pose.scale((float) s, (float) s, (float) -s);
                    pose.translate(1.5F, 2.75F, 0.0F);
                    glRotate(pose, 180F, 1.0F, 0.0F, 0.0F);
                    glRotate(pose, -90F, 0.0F, 1.0F, 0.0F);
                    glRotate(pose, -45F, 1.0F, 0.0F, 0.0F);
                }
            }
            // GL_CULL_FACE enabled for the main body
            detonator_laser.renderGroup("Main", pose, buffers.getBuffer(type(detonator_laser_tex, true)), light, overlayOrNone(), 0xFFFFFFFF);

            pose.pushPose();
            // lighting, culling and texturing disabled, lightmap 240/240, glColor3f(1, 0, 0)
            detonator_laser.renderGroup("Lights", pose, buffers.getBuffer(UNLIT_COLOR), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFF0000);

            pose.pushPose();
            float px = 0.0625F;
            pose.translate(0.5626F, px * 18, -px * 14);
            VertexConsumer tess = buffers.getBuffer(UNLIT_COLOR);
            Matrix4f m = pose.last().pose();
            int sub = 32;
            double width = px * 8;
            double len = width / sub;
            double time = System.currentTimeMillis() / -100D;
            double amplitude = 0.075;
            for (int i = 0; i < sub; i++) {
                double h0 = Math.sin(i * 0.5 + time) * amplitude;
                double h1 = Math.sin((i + 1) * 0.5 + time) * amplitude;
                tess.addVertex(m, 0, (float) (-px * 0.25 + h1), (float) (len * (i + 1))).setColor(0xFFFFFF00);
                tess.addVertex(m, 0, (float) (px * 0.25 + h1), (float) (len * (i + 1))).setColor(0xFFFFFF00);
                tess.addVertex(m, 0, (float) (px * 0.25 + h0), (float) (len * i)).setColor(0xFFFFFF00);
                tess.addVertex(m, 0, (float) (-px * 0.25 + h0), (float) (len * i)).setColor(0xFFFFFF00);
            }
            pose.popPose();

            pose.pushPose();
            String s;
            Random rand = new Random(System.currentTimeMillis() / 500);
            Font font = Minecraft.getInstance().font;
            float f3 = 0.01F;
            pose.translate(0.5625F, 1.3125F, 0.875F);
            pose.scale(f3, -f3, f3);
            glRotate(pose, 90, 0, 1, 0);
            pose.translate(3F, -2F, 0.2F);
            for (int i = 0; i < 3; i++) {
                s = (rand.nextInt(900000) + 100000) + "";
                font.drawInBatch(s, 0, 0, 0xFFFF0000, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                pose.translate(0F, 12.5F, 0F);
            }
            pose.popPose();
            pose.popPose();
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ ItemRenderLibrary (ItemRenderBase)

    /** ItemRenderBase#renderItem with the per-item renderInventory / renderNonInv / renderCommon hooks. */
    public abstract static class Base extends LegacyIItemRenderer {
        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return true; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return type == Type.ENTITY && (helper == Helper.ENTITY_BOBBING || helper == Helper.ENTITY_ROTATION);
        }

        @Override
        protected final void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            // GL_CULL_FACE and GL_ALPHA_TEST enabled
            if (type == Type.INVENTORY) {
                pose.translate(8, 10, 0);
                glRotate(pose, -30, 1, 0, 0);
                glRotate(pose, 45, 0, 1, 0);
                pose.scale(-1, -1, -1);
                renderInventory();
            } else {
                if (type != Type.ENTITY)
                    pose.translate(0.5, 0.25, 0);
                else
                    pose.scale(1.5F, 1.5F, 1.5F);
                pose.scale(0.25F, 0.25F, 0.25F);
                if (type != Type.EQUIPPED)
                    glRotate(pose, 90, 0, 1, 0);
            }
            renderCommon();
            renderCommonWithStack(item);
            pose.popPose();
        }

        void renderInventory() { }
        void renderCommon() { }
        void renderCommonWithStack(ItemStack item) { }
    }

    /** ItemRenderLibrary: Item.getItemFromBlock(ModBlocks.boxcar). */
    public static final class Boxcar extends Base {
        private static final LegacyEntityObjMesh boxcar = mesh("models/entity/legacy_boxcar.obj");
        private static final ResourceLocation boxcar_tex = tex("models/boxcar.png");

        @Override void renderInventory() {
            glRotate(pose, 90, 0, 1, 0);
            pose.translate(0, -1, 0);
            pose.scale(4, 4, 4);
        }

        @Override void renderCommon() {
            pose.scale(0.5F, 0.5F, 0.5F);
            boxcar.renderAll(pose, buffers.getBuffer(type(boxcar_tex, true)), light, overlayOrNone(), 0xFFFFFFFF);
        }
    }

    /** ItemRenderLibrary: Item.getItemFromBlock(ModBlocks.machine_industrial_generator). */
    public static final class IndustrialGenerator extends Base {
        private static final LegacyEntityObjMesh igen = mesh("models/entity/legacy_igen.obj");
        private static final ResourceLocation igen_tex = tex("models/machines/igen.png");

        @Override void renderInventory() {
            pose.translate(0, -1, 0);
            pose.scale(4, 4, 4);
            glRotate(pose, 90, 0, 1, 0);
        }

        @Override void renderCommon() {
            pose.translate(0, 0, -0.5);
            pose.scale(0.75F, 0.75F, 0.75F);
            // GL_CULL_FACE disabled for the parts
            VertexConsumer consumer = buffers.getBuffer(type(igen_tex, false));
            igen.renderGroup("Body", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            igen.renderGroup("Rotor", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
        }
    }

    /** ItemRenderLibrary: ModItems.sawblade. */
    public static final class Sawblade extends Base {
        private static final LegacyEntityObjMesh sawmill = mesh("models/entity/legacy_sawmill.obj");
        private static final ResourceLocation sawmill_tex = tex("models/machines/sawmill.png");

        @Override void renderInventory() {
            pose.translate(0, -7, 0);
            pose.scale(6, 6, 6);
            glRotate(pose, -45, 0, 1, 0);
            glRotate(pose, 30, 1, 0, 0);
            pose.translate(0, 1.375, 0);
            glRotate(pose, System.currentTimeMillis() % 3600 * 0.2F, 0, 0, 1);
            pose.translate(0, -1.375, 0);
        }

        @Override void renderCommonWithStack(ItemStack item) {
            pose.translate(0, 0, -0.875);
            sawmill.renderGroup("Blade", pose, buffers.getBuffer(type(sawmill_tex, true)), light, overlayOrNone(), 0xFFFFFFFF);
        }
    }

    /** ItemRenderLibrary: ModItems.ammo_himars (HIMARSRocket texture and modelType per variant). */
    public static final class Himars extends Base {
        private static final LegacyEntityObjMesh turret_himars = mesh("models/entity/legacy_turret_himars.obj");

        @Override void renderInventory() {
            pose.translate(0, -2, 0);
            double scale = 2.75D;
            pose.scale((float) scale, (float) scale, (float) scale);
            glRotate(pose, System.currentTimeMillis() % 3600 / 10D, 0, 1, 0);
        }

        @Override void renderCommonWithStack(ItemStack item) {
            pose.translate(0, 1.5, 0);
            glRotate(pose, -45, 0, 1, 0);
            glRotate(pose, 90, 1, 0, 0);
            String id = item.getItem() instanceof AmmoHimarsItem himars ? himars.variant(item).id() : "standard";
            int modelType = id.equals("single") || id.equals("single_tb") ? 1 : 0;
            VertexConsumer consumer = buffers.getBuffer(type(tex("models/projectiles/himars_" + id + ".png"), true));
            if (modelType == 0) {
                pose.translate(0.75, 0, 0);
                turret_himars.renderGroup("RocketStandard", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
                pose.translate(-1.5, 0, 0);
                pose.translate(0, -3.375D, 0);
                turret_himars.renderGroup("TubeStandard", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            } else {
                pose.translate(0.75, 0, 0);
                turret_himars.renderGroup("RocketSingle", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
                pose.translate(-1.5, 0, 0);
                pose.translate(0, -3.375D, 0);
                turret_himars.renderGroup("TubeSingle", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            }
        }
    }

    /** RenderPylon#getRenderer (IItemRendererProvider) for red_pylon: ResourceManager.pylon "Pylon" with pylon_tex. */
    public static final class RedPylon extends Base {
        private static final LegacyEntityObjMesh pylon = mesh("models/obj/network/pylon.obj");
        private static final ResourceLocation pylon_tex = tex("models/network/pylon.png");

        @Override void renderInventory() {
            pose.translate(0, -5, 0);
            pose.scale(2.9F, 2.9F, 2.9F);
        }

        @Override void renderCommonWithStack(ItemStack stack) {
            pose.scale(1, 1, 1);
            pylon.renderGroup("Pylon", pose, buffers.getBuffer(type(pylon_tex, true)), light, overlayOrNone(), 0xFFFFFFFF);
        }
    }

    // ------------------------------------------------------------------ missiles

    /** ItemRenderMissileGeneric.RenderMissileType. */
    public enum MissileType { TYPE_TIER0, TYPE_TIER1, TYPE_TIER2, TYPE_TIER3, TYPE_STEALTH, TYPE_ABM, TYPE_NUCLEAR, TYPE_ROBIN }

    /** ItemRenderMissileGeneric; the model and texture pass (generateStandard / generateLarge) is LauncherMissileRenderer#renderAssembly. */
    public static final class MissileGeneric extends LegacyIItemRenderer {
        private final MissileType missileType;

        public MissileGeneric(MissileType missileType) {
            this.missileType = missileType;
        }

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return true; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return helper == Helper.ENTITY_BOBBING || helper == Helper.ENTITY_ROTATION;
        }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            double guiScale = 1;
            double guiOffset = 0;
            switch (this.missileType) {
                case TYPE_TIER0 -> { guiScale = 3.75D; guiOffset = 10.75D; }
                case TYPE_TIER1 -> { guiScale = 2.5D; guiOffset = 8.5D; }
                case TYPE_TIER2 -> { guiScale = 2D; guiOffset = 6.5D; }
                case TYPE_TIER3 -> { guiScale = 1.25D; guiOffset = 1D; }
                case TYPE_STEALTH -> { guiScale = 1.75D; guiOffset = 4.75D; }
                case TYPE_ABM -> { guiScale = 2.25D; guiOffset = 7D; }
                case TYPE_NUCLEAR -> { guiScale = 1.375D; guiOffset = 1.5D; }
                case TYPE_ROBIN -> { guiScale = 1.25D; guiOffset = 2D; }
            }
            switch (type) {
                case EQUIPPED -> {
                    double s = 0.15;
                    pose.translate(0.5, -0.25, 0);
                    pose.scale((float) s, (float) s, (float) s);
                }
                case EQUIPPED_FIRST_PERSON -> {
                    double heldScale = 0.1;
                    pose.translate(0.5, 0.25, 0);
                    pose.scale((float) heldScale, (float) heldScale, (float) heldScale);
                }
                case ENTITY -> {
                    double s2 = 0.15;
                    pose.scale((float) s2, (float) s2, (float) s2);
                }
                case INVENTORY -> {
                    pose.scale((float) guiScale, (float) guiScale, (float) guiScale);
                    glRotate(pose, 135, 0, 0, 1);
                    glRotate(pose, System.currentTimeMillis() / 15 % 360, 0, 1, 0);
                    pose.translate(0, -16 + guiOffset, 0);
                }
            }
            // GL_CULL_FACE disabled around renderer.accept
            LauncherMissileRenderer.renderAssembly(item, Blocks.AIR.defaultBlockState(), pose, buffers, light, overlayOrNone());
            pose.popPose();
        }
    }

    /** ItemRenderMissile (missile_custom): only when MissileMultipart.loadFromStruct(getStruct(item)) is not null. */
    public static final class Missile extends LegacyIItemRenderer {
        @Override
        public boolean handlesStack(ItemStack stack) {
            return CustomMissileData.read(stack) != null;
        }

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return true; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            CustomMissileData missile = CustomMissileData.read(item);
            if (missile == null) return;
            pose.pushPose();
            switch (type) {
                case EQUIPPED, EQUIPPED_FIRST_PERSON, ENTITY -> {
                    double s = 0.2;
                    pose.scale((float) s, (float) s, (float) s);
                    pose.translate(2, 0, 0);
                    LauncherMissileRenderer.renderAssembly(item, Blocks.AIR.defaultBlockState(), pose, buffers, light, overlayOrNone());
                }
                case INVENTORY -> {
                    double height = MissileMultipartRenderer.height(missile.warhead(), missile.fuselage(), missile.thruster());
                    if (height == 0D)
                        height = 4D;
                    double size = 20;
                    double scale = size / height;
                    pose.translate(height / 2 * scale, 0, 0);
                    glRotate(pose, 135, 0, 0, 1);
                    glRotate(pose, 215, 1, 0, 0);
                    pose.translate(7, 14, 0);
                    pose.scale((float) -scale, (float) -scale, (float) -scale);
                    glRotate(pose, System.currentTimeMillis() / 25 % 360, 0, -1, 0);
                    LauncherMissileRenderer.renderAssembly(item, Blocks.AIR.defaultBlockState(), pose, buffers, light, overlayOrNone());
                }
            }
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ ModelBase box models

    public static final ModelLayerLocation BIG_SWORD_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_big_sword"), "main");
    public static final ModelLayerLocation SWORD_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_sword"), "main");
    public static final ModelLayerLocation STEEL_ROOF_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_steel_roof"), "main");

    /** new ModelRenderer(model, u, v).addBox(bx, by, bz, w, h, d); setRotationPoint(px, py, pz); setRotation(rx, ry, rz). */
    static void box(PartDefinition parent, String name, int u, int v, float bx, float by, float bz, float w, float h, float d,
                    float px, float py, float pz, float rx, float ry, float rz) {
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(bx, by, bz, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    /** ModelBigSword (128 x 32). */
    public static LayerDefinition createBigSword() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "HandleBottom", 1, 1, 0F, 0F, 0F, 1, 3, 3, 0F, 0F, 0F, -0.7853982F, 0F, 0F);
        box(root, "HandleGrip", 17, 1, 0F, 0F, 0F, 1, 5, 2, 0F, -4F, -1F, 0F, 0F, 0F);
        box(root, "Handle1", 25, 1, 0F, -1F, 0F, 2, 1, 4, -0.5F, -3F, 0F, 0.2617994F, 0F, 0F);
        box(root, "Handle2", 41, 1, 0F, -1F, -4F, 2, 1, 4, -0.5F, -3F, 0F, -0.2617994F, 0F, 0F);
        box(root, "Blade", 57, 1, 0F, 0F, 0F, 3, 18, 1, 0F, -22F, 1.5F, 0F, 1.570796F, 0F);
        box(root, "SBladeTip", 2, 10, 0F, 0F, 0F, 1, 2, 2, 0F, -23.5F, 0F, -0.7853982F, 0F, 0F);
        return LayerDefinition.create(mesh, 128, 32);
    }

    /** ModelSword (64 x 32). */
    public static LayerDefinition createSword() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "GripBottom", 0, 17, 0F, 0F, 0F, 3, 3, 1, 0F, 0F, 0F, 0F, 0F, 0F);
        box(root, "GripHandle", 8, 2, 0F, 0F, 0F, 2, 5, 1, 0.5F, -5F, 0F, 0F, 0F, 0F);
        box(root, "Shield", 14, 5, 0F, 0F, 0F, 6, 1, 3, -1.5F, -6F, -1F, 0F, 0F, 0F);
        box(root, "Blade", 0, 0, 0F, 0F, 0F, 3, 16, 1, 0F, -22F, 0F, 0F, 0F, 0F);
        box(root, "BladeTip", 8, 0, 0F, 0F, 0F, 2, 1, 1, 0.5F, -23F, 0F, 0F, 0F, 0F);
        box(root, "Shield1", 14, 0, 0F, 0F, 0F, 1, 1, 4, -2F, -6.5F, -1.5F, 0F, 0F, 0F);
        box(root, "Shield2", 24, 0, 0F, 0F, 0F, 1, 1, 4, 4F, -6.5F, -1.5F, 0F, 0F, 0F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelSteelRoof (64 x 32). */
    public static LayerDefinition createSteelRoof() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        box(root, "Shape1", 0, 0, 0F, 0F, 0F, 16, 1, 16, -8F, 23F, -8F, 0F, 0F, 0F);
        box(root, "Shape2", 30, 15, 0F, 0F, 0F, 1, 1, 16, -3F, 22F, -8F, 0F, 0F, 0F);
        box(root, "Shape3", 0, 17, 0F, 0F, 0F, 16, 2, 2, -8F, 21F, 2F, 0F, 0F, 0F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static ModelPart bake(ModelLayerLocation layer) {
        return Minecraft.getInstance().getEntityModels().bakeLayer(layer);
    }

    /** ItemRenderBigSword: ModelBigSword with ModelBigSwordTexture.png. */
    public static final class BigSword extends LegacyIItemRenderer {
        private static final ResourceLocation TEXTURE = tex("models/model_big_sword_texture.png");
        private ModelPart bigSwordModel;

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return type != Type.INVENTORY; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }

        /** BigSword#isFull3D. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            if (bigSwordModel == null) bigSwordModel = bake(BIG_SWORD_LAYER);
            pose.pushPose();
            glRotate(pose, -135.0F, 0.0F, 0.0F, 1.0F);
            glRotate(pose, 90.0F, 0.0F, 1.0F, 0.0F);
            pose.translate(0.0F, 0.4F, -0.7F);
            bigSwordModel.render(pose, buffers.getBuffer(type(TEXTURE, false)), light, overlayOrNone());
            pose.popPose();
        }
    }

    /** ItemRenderRedstoneSword: ModelSword with ModelSwordRedstone.png. */
    public static final class RedstoneSword extends LegacyIItemRenderer {
        private static final ResourceLocation TEXTURE = tex("models/model_sword_redstone.png");
        private ModelPart swordModel;

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return type != Type.INVENTORY; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }

        /** RedstoneSword#isFull3D. */
        @Override protected boolean isFull3D(ItemStack stack) { return true; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            if (swordModel == null) swordModel = bake(SWORD_LAYER);
            pose.pushPose();
            glRotate(pose, -135.0F, 0.0F, 0.0F, 1.0F);
            pose.translate(-0.8F, 0.4F, -0.1F);
            swordModel.render(pose, buffers.getBuffer(type(TEXTURE, false)), light, overlayOrNone());
            pose.popPose();
        }
    }

    /** ItemRenderDecoBlock (steel_roof): ModelSteelRoof with SteelRoof.png. */
    public static final class DecoBlock extends LegacyIItemRenderer {
        private static final ResourceLocation TEXTURE = tex("models/steel_roof.png");
        private ModelPart roof;

        @Override
        public boolean handleRenderType(ItemStack item, Type type) { return type != Type.INVENTORY; }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) { return false; }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            if (roof == null) roof = bake(STEEL_ROOF_LAYER);
            pose.pushPose();
            switch (type) {
                case ENTITY -> {
                    pose.scale(0.5F, 0.5F, 0.5F);
                    glRotate(pose, 180.0F, 1.0F, 0.0F, 0.0F);
                    pose.translate(0.0F, -1.0F, 0.0F);
                }
                case EQUIPPED -> {
                    pose.scale(0.5F, 0.5F, 0.5F);
                    pose.translate(0.8F, -0.3F, 0.2F);
                }
                case EQUIPPED_FIRST_PERSON -> {
                    glRotate(pose, -135.0F, 0.0F, 0.0F, 1.0F);
                    pose.translate(-0.6F, -0.6F, -0.1F);
                    pose.scale(0.5F, 0.5F, 0.5F);
                }
                default -> { }
            }
            roof.render(pose, buffers.getBuffer(type(TEXTURE, false)), light, overlayOrNone());
            pose.popPose();
        }
    }
}

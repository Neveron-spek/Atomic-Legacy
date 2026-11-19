package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * [parity m1] 1.7.10 item renderers (ItemRenderLibrary / IItemRendererProvider#getRenderer / ItemRenderBoltgun) for
 * items that ObjMachineItemRenderer and LegacyMachineItemRenderer drew with invented, context-independent scales.
 * OBJ files are byte copies of the original (models/entity/legacy_*.obj, duchessgambit.obj); textures are the
 * byte-identical port copies of the ResourceManager textures.
 */
final class M1LegacyItemRenderers {

    private M1LegacyItemRenderers() {
    }

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static LegacyEntityObjMesh mesh(String name) {
        return LegacyEntityObjMesh.load(AtomicLegacy.id("models/entity/" + name + ".obj"));
    }

    private static ResourceLocation tex(String path) {
        return AtomicLegacy.id("textures/" + path);
    }

    /** GL_CULL_FACE enabled (ItemRenderBase#renderItem) -> entityCutout; disabled -> no cull. */
    private static RenderType type(ResourceLocation texture, boolean cull) {
        return cull ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
    }

    /** System.currentTimeMillis() % 3600 * 0.1D around (0, -1, 0). */
    private static float dishDegrees() {
        return (System.currentTimeMillis() % 3600L) * 0.1F;
    }

    private abstract static class Base extends LegacyModelItemRenderers.Base {
        final void draw(LegacyEntityObjMesh mesh, String group, ResourceLocation texture, boolean cull) {
            draw(mesh, group, texture, cull, 0xFFFFFFFF);
        }

        final void draw(LegacyEntityObjMesh mesh, String group, ResourceLocation texture, boolean cull, int argb) {
            VertexConsumer consumer = buffers.getBuffer(type(texture, cull));
            if (group == null) {
                mesh.renderAll(pose, consumer, light, overlayOrNone(), argb);
            } else {
                mesh.renderGroup(group, pose, consumer, light, overlayOrNone(), argb);
            }
        }
    }

    /** RenderAmmoPress#getRenderer. */
    static final class AmmoPress extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_ammo_press");
        private static final ResourceLocation TEX = tex("block/ammo_press.png");

        @Override void renderInventory() {
            pose.translate(0, -2.5, 0);
            pose.scale(5, 5, 5);
        }

        @Override void renderCommon() {
            glRotate(pose, 90, 0, 1, 0);
            draw(MESH, null, TEX, true);
        }
    }

    /** RenderBlastFurnace#getRenderer. */
    static final class BlastFurnace extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_blast_furnace");
        private static final ResourceLocation TEX = tex("models/machines/blast_furnace_1710.png");

        @Override void renderInventory() {
            pose.translate(0, -4.5, 0);
            pose.scale(4, 4, 4);
        }

        @Override void renderCommon() {
            pose.scale(0.5F, 0.5F, 0.5F);
            draw(MESH, null, TEX, true);
        }
    }

    /** RenderBoiler#getRenderer (damage 1 = boiler_burst does not exist as a port item stack). */
    static final class Boiler extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_boiler");
        private static final ResourceLocation TEX = tex("models/machines/boiler.png");

        @Override void renderInventory() {
            pose.translate(0, -3, 0);
            pose.scale(3, 3, 3);
        }

        @Override void renderCommon() {
            glRotate(pose, 90, 0, 1, 0);
            draw(MESH, null, TEX, true);
        }
    }

    /** RenderIndustrialBoiler#getRenderer. */
    static final class IndustrialBoiler extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_industrial_boiler");
        private static final ResourceLocation TEX = tex("models/machines/industrial_boiler.png");

        @Override void renderInventory() {
            pose.translate(0, -3, 0);
            pose.scale(2.5F, 2.5F, 2.5F);
        }

        @Override void renderCommon() {
            glRotate(pose, 90, 0, 1, 0);
            draw(MESH, null, TEX, true);
        }
    }

    /** RenderChimneyBrick#getRenderer (GL_CULL_FACE disabled). */
    static final class ChimneyBrick extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_chimney_brick");
        private static final ResourceLocation TEX = tex("models/machines/chimney_brick.png");

        @Override void renderInventory() {
            pose.translate(0, -5, 0);
            pose.scale(2.25F, 2.25F, 2.25F);
        }

        @Override void renderCommon() {
            pose.scale(0.5F, 0.5F, 0.5F);
            draw(MESH, null, TEX, false);
        }
    }

    /** RenderChimneyIndustrial#getRenderer (GL_CULL_FACE disabled). */
    static final class ChimneyIndustrial extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_chimney_industrial");
        private static final ResourceLocation TEX = tex("models/machines/chimney_industrial.png");

        @Override void renderInventory() {
            pose.translate(0, -5, 0);
            pose.scale(2.75F, 2.75F, 2.75F);
        }

        @Override void renderCommon() {
            pose.scale(0.25F, 0.25F, 0.25F);
            draw(MESH, null, TEX, false);
        }
    }

    /** RenderCondenser#getRenderer (machine_condenser_powered). */
    static final class PoweredCondenser extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_condenser");
        private static final ResourceLocation TEX = tex("models/machines/condenser.png");

        @Override void renderInventory() {
            pose.translate(-1, -1, 0);
            pose.scale(2.75F, 2.75F, 2.75F);
        }

        @Override void renderCommon() {
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0.5, 0, 0);
            draw(MESH, null, TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_tower_small. */
    static final class TowerSmall extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_tower_small");
        private static final ResourceLocation TEX = tex("models/machines/tower_small.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(3, 3, 3);
        }

        @Override void renderCommon() {
            pose.scale(0.25F, 0.25F, 0.25F);
            draw(MESH, null, TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_tower_large. */
    static final class TowerLarge extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_tower_large");
        private static final ResourceLocation TEX = tex("block/tower_large.png");

        @Override void renderInventory() {
            pose.translate(0, -3, 0);
            float s = 4 * 0.95F;
            pose.scale(s, s, s);
        }

        @Override void renderCommon() {
            pose.scale(0.25F, 0.25F, 0.25F);
            draw(MESH, null, TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_radar (GL_CULL_FACE disabled, rotating dish). */
    static final class Radar extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_radar");
        private static final ResourceLocation BASE_TEX = tex("models/machines/radar_base.png");
        private static final ResourceLocation DISH_TEX = tex("models/machines/radar_dish.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(5, 5, 5);
        }

        @Override void renderCommon() {
            draw(MESH, "Base", BASE_TEX, false);
            glRotate(pose, dishDegrees(), 0, -1, 0);
            pose.translate(-0.125, 0, 0);
            draw(MESH, "Dish", DISH_TEX, false);
        }
    }

    /** RenderRadarLarge#getRenderer. */
    static final class RadarLarge extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_radar_large");
        private static final ResourceLocation TEX = tex("models/machines/radar_large.png");

        @Override void renderInventory() {
            pose.translate(0, -5, 0);
            pose.scale(3, 3, 3);
        }

        @Override void renderCommon() {
            glRotate(pose, 180, 0, 1, 0);
            pose.scale(0.5F, 0.5F, 0.5F);
            draw(MESH, "Radar", TEX, true);
            glRotate(pose, dishDegrees(), 0, -1, 0);
            draw(MESH, "Dish", TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_radgen (Light: GL_TEXTURE_2D disabled, glColor3f(0, 1, 0), lit). */
    static final class Radgen extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_radgen");
        private static final ResourceLocation TEX = tex("models/machines/radgen.png");

        @Override void renderInventory() {
            pose.translate(0, -1, 0);
            pose.scale(4.5F, 4.5F, 4.5F);
        }

        @Override void renderCommon() {
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0.5, 0, 0);
            draw(MESH, "Base", TEX, true);
            draw(MESH, "Rotor", TEX, true);
            draw(MESH, "Light", WHITE, true, 0xFF00FF00);
        }
    }

    /** ItemRenderLibrary: ModBlocks.sat_dock (satDock = cargo_pad.obj). */
    static final class SatDock extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_cargo_pad");
        private static final ResourceLocation TEX = tex("models/machines/cargo_pad.png");

        @Override void renderInventory() {
            pose.scale(3, 3, 3);
        }

        @Override void renderCommon() {
            glRotate(pose, 90, 0, -1, 0);
            draw(MESH, null, TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.boat (duchessgambit). */
    static final class Boat extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("duchessgambit");
        private static final ResourceLocation TEX = tex("block/duchessgambit.png");

        @Override void renderInventory() {
            glRotate(pose, -90, 0, 1, 0);
            pose.translate(0, 1, 0);
            pose.scale(1.75F, 1.75F, 1.75F);
        }

        @Override void renderCommon() {
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0, 0, -3);
            draw(MESH, null, TEX, true);
        }
    }

    /** RenderRBMKAutoloader#getRenderer (GL_CULL_FACE disabled). */
    static final class RbmkAutoloader extends Base {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_rbmk_autoloader");
        private static final ResourceLocation TEX = tex("models/machines/rbmk_autoloader.png");

        @Override void renderInventory() {
            pose.translate(0, -6, 0);
            pose.scale(1.75F, 1.75F, 1.75F);
        }

        @Override void renderCommon() {
            glRotate(pose, 180, 0, 1, 0);
            draw(MESH, "Base", TEX, false);
            draw(MESH, "Piston", TEX, false);
        }
    }

    /** ItemRenderBoltgun (ClientProxy#registerItemRenderer(ModItems.boltgun)). */
    static final class Boltgun extends LegacyIItemRenderer {
        private static final LegacyEntityObjMesh MESH = mesh("legacy_boltgun");
        private static final ResourceLocation TEX = tex("models/weapons/boltgun.png");

        @Override
        public boolean handleRenderType(ItemStack item, Type type) {
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(Type type, ItemStack item, Helper helper) {
            return type == Type.ENTITY && (helper == Helper.ENTITY_ROTATION || helper == Helper.ENTITY_BOBBING);
        }

        /** if(anim[0] != 0) player.isSwingInProgress = false. */
        @Override
        protected boolean cancelsFirstPersonSwing(ItemStack stack) {
            return HbmAnimations.getRelevantTransformation("RECOIL")[0] != 0;
        }

        @Override
        protected void renderItem(Type type, ItemStack item) {
            pose.pushPose();
            // GL_CULL_FACE enabled
            VertexConsumer consumer = buffers.getBuffer(type(TEX, true));
            switch (type) {
                case EQUIPPED_FIRST_PERSON -> {
                    double s0 = 0.15D;
                    pose.translate(0.5, 0.35, -0.25F);
                    glRotate(pose, 15, 0, 0, 1);
                    glRotate(pose, 80, 0, 1, 0);
                    pose.scale((float) s0, (float) s0, (float) s0);
                    pose.pushPose();
                    double[] anim = HbmAnimations.getRelevantTransformation("RECOIL");
                    pose.translate(0, 0, -anim[0]);
                    MESH.renderGroup("Barrel", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
                    pose.popPose();
                }
                case EQUIPPED -> {
                    double scale = 0.25D;
                    pose.scale((float) scale, (float) scale, (float) scale);
                    glRotate(pose, 10, 0, 1, 0);
                    glRotate(pose, 10, 0, 0, 1);
                    glRotate(pose, 10, 1, 0, 0);
                    pose.translate(1.5, -0.25, 1);
                }
                case ENTITY -> {
                    double s1 = 0.1D;
                    pose.scale((float) s1, (float) s1, (float) s1);
                }
                case INVENTORY -> {
                    double s = 1.75D;
                    pose.translate(7, 10, 0);
                    glRotate(pose, -90, 0, 1, 0);
                    glRotate(pose, -135, 1, 0, 0);
                    pose.scale((float) s, (float) s, (float) -s);
                }
            }
            MESH.renderGroup("Gun", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            if (type != Type.EQUIPPED_FIRST_PERSON) {
                MESH.renderGroup("Barrel", pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            }
            pose.popPose();
        }
    }
}

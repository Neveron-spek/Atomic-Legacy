package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * [parity Y1] ItemRenderBase renderers of 1.7.10 (ItemRenderLibrary / IItemRendererProvider) for items the port drew
 * with a baked OBJ model and invented display transforms. OBJ files are byte copies of the original
 * (models/entity/legacy_*.obj); textures are the byte-identical port copies of the ResourceManager textures.
 */
final class Y1LegacyItemRenderers {

    private Y1LegacyItemRenderers() {
    }

    private static LegacyEntityObjMesh mesh(String name) {
        return LegacyEntityObjMesh.load(AtomicLegacy.id("models/entity/legacy_" + name + ".obj"));
    }

    private static ResourceLocation tex(String path) {
        return AtomicLegacy.id("textures/" + path);
    }

    /** GL_CULL_FACE enabled (ItemRenderBase#renderItem) -> entityCutout; disabled -> no cull. */
    private static RenderType type(ResourceLocation texture, boolean cull) {
        return cull ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
    }

    private abstract static class Base extends LegacyModelItemRenderers.Base {
        final void draw(LegacyEntityObjMesh mesh, String group, ResourceLocation texture, boolean cull) {
            VertexConsumer consumer = buffers.getBuffer(type(texture, cull));
            if (group == null) {
                mesh.renderAll(pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            } else {
                mesh.renderGroup(group, pose, consumer, light, overlayOrNone(), 0xFFFFFFFF);
            }
        }
    }

    /** ItemRenderLibrary: ModBlocks.soyuz_capsule (soyuz_lander "Capsule", soyuz_lander_tex). */
    static final class SoyuzCapsule extends Base {
        private static final LegacyEntityObjMesh SOYUZ_LANDER = mesh("soyuz_lander");
        private static final ResourceLocation SOYUZ_LANDER_TEX = tex("models/soyuz_capsule/soyuz_lander.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(5, 5, 5);
        }

        @Override void renderCommon() {
            draw(SOYUZ_LANDER, "Capsule", SOYUZ_LANDER_TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_press. */
    static final class Press extends Base {
        private static final LegacyEntityObjMesh BODY = mesh("press_body");
        private static final LegacyEntityObjMesh HEAD = mesh("press_head");
        private static final ResourceLocation BODY_TEX = tex("block/press_body.png");
        private static final ResourceLocation HEAD_TEX = tex("block/press_head.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(4.5F, 4.5F, 4.5F);
        }

        @Override void renderCommon() {
            draw(BODY, null, BODY_TEX, true);
            pose.translate(0, 0.5, 0);
            draw(HEAD, null, HEAD_TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_epress. */
    static final class EPress extends Base {
        private static final LegacyEntityObjMesh BODY = mesh("epress_body");
        private static final LegacyEntityObjMesh HEAD = mesh("epress_head");
        private static final ResourceLocation BODY_TEX = tex("block/epress_body.png");
        private static final ResourceLocation HEAD_TEX = tex("block/epress_head.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(4.5F, 4.5F, 4.5F);
        }

        @Override void renderCommon() {
            draw(BODY, null, BODY_TEX, true);
            pose.translate(0, 1.5, 0);
            draw(HEAD, null, HEAD_TEX, true);
        }
    }

    /** ItemRenderLibrary: ModBlocks.machine_large_turbine (Body + Blades, GL_CULL_FACE disabled). */
    static final class LargeTurbine extends Base {
        private static final LegacyEntityObjMesh TURBINE = mesh("turbine");
        private static final ResourceLocation TURBINE_TEX = tex("models/machines/turbine.png");
        private static final ResourceLocation UNIVERSAL_BRIGHT = tex("models/machines/turbofan_blades.png");

        @Override void renderInventory() {
            pose.translate(0, -1, 0);
            pose.scale(2.5F, 2.5F, 2.5F);
        }

        @Override void renderCommon() {
            glRotate(pose, 90, 0, 1, 0);
            draw(TURBINE, "Body", TURBINE_TEX, false);
            draw(TURBINE, "Blades", UNIVERSAL_BRIGHT, false);
        }
    }

    /** RenderCentrifuge#getRenderer: machine_centrifuge (renderAll) and machine_gascent ("Centrifuge"). */
    static final class Centrifuge extends Base {
        private static final LegacyEntityObjMesh CENTRIFUGE = mesh("centrifuge");
        private static final LegacyEntityObjMesh GASCENT = mesh("gascent");
        private static final ResourceLocation CENTRIFUGE_TEX = tex("models/machines/centrifuge_new.png");
        private static final ResourceLocation GASCENT_TEX = tex("models/machines/centrifuge_gas.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(3.5F, 3.5F, 3.5F);
        }

        @Override void renderCommonWithStack(ItemStack item) {
            if (BuiltInRegistries.ITEM.getKey(item.getItem()).getPath().equals("machine_gascent")) {
                draw(GASCENT, "Centrifuge", GASCENT_TEX, true);
            } else {
                draw(CENTRIFUGE, null, CENTRIFUGE_TEX, true);
            }
        }
    }

    /** RenderWoodBurner#getRenderer. */
    static final class WoodBurner extends Base {
        private static final LegacyEntityObjMesh WOOD_BURNER = mesh("wood_burner");
        private static final ResourceLocation WOOD_BURNER_TEX = tex("block/machine_wood_burner.png");

        @Override void renderInventory() {
            pose.translate(0, -4, 0);
            pose.scale(3.5F, 3.5F, 3.5F);
        }

        @Override void renderCommon() {
            glRotate(pose, 90, 0, 1, 0);
            draw(WOOD_BURNER, null, WOOD_BURNER_TEX, true);
        }
    }

    /** RenderChemicalPlant#getRenderer (Base, Slider, Spinner, Frame). */
    static final class ChemicalPlant extends Base {
        private static final LegacyEntityObjMesh CHEMICAL_PLANT = mesh("chemical_plant");
        private static final ResourceLocation CHEMICAL_PLANT_TEX = tex("block/machine_chemical_plant.png");

        @Override void renderInventory() {
            pose.translate(0, -2.75, 0);
            pose.scale(4.5F, 4.5F, 4.5F);
        }

        @Override void renderCommonWithStack(ItemStack item) {
            glRotate(pose, 90, 0, 1, 0);
            pose.scale(0.75F, 0.75F, 0.75F);
            draw(CHEMICAL_PLANT, "Base", CHEMICAL_PLANT_TEX, true);
            draw(CHEMICAL_PLANT, "Slider", CHEMICAL_PLANT_TEX, true);
            draw(CHEMICAL_PLANT, "Spinner", CHEMICAL_PLANT_TEX, true);
            draw(CHEMICAL_PLANT, "Frame", CHEMICAL_PLANT_TEX, true);
        }
    }

    /** RenderDieselGen#getRenderer. */
    static final class Diesel extends Base {
        private static final LegacyEntityObjMesh DIESELGEN = mesh("dieselgen");
        private static final ResourceLocation DIESELGEN_TEX = tex("models/machines/dieselgen.png");

        @Override void renderInventory() {
            pose.translate(0, -2.5, 0);
            pose.scale(5, 5, 5);
        }

        @Override void renderCommon() {
            pose.scale(2, 2, 2);
            draw(DIESELGEN, null, DIESELGEN_TEX, true);
        }
    }

    /** RenderFluidPump#getRenderer. */
    static final class FluidPump extends Base {
        @Override void renderInventory() {
            pose.translate(0, -2, 0);
            pose.scale(5, 5, 5);
        }

        @Override void renderCommon() {
            pose.scale(2, 2, 2);
            glRotate(pose, 90, 0, 1, 0);
            draw(FluidPumpBlockEntityRenderer.FLUID_PUMP, null, FluidPumpBlockEntityRenderer.FLUID_PUMP_TEX, true);
        }
    }

    /** RenderIntake#getRenderer. */
    static final class Intake extends Base {
        private static final LegacyEntityObjMesh INTAKE = mesh("intake");
        private static final ResourceLocation INTAKE_TEX = tex("block/machine_intake.png");

        @Override void renderInventory() {
            pose.scale(5, 5, 5);
        }

        @Override void renderCommon() {
            draw(INTAKE, null, INTAKE_TEX, true);
        }
    }
}

package com.atomiclegacy.render;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.GasMaskItem;
import com.atomiclegacy.util.ArmorModHandler;
import com.atomiclegacy.util.ShadyUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * [parity U4: armor models] The 1.7.10 armor models that the port drew as plain vanilla armor or not at all:
 * <ul>
 * <li>getArmorModel of worn helmets: ArmorGasMask (ModelGasMask for gas_mask, ModelM65 for gas_mask_m65 / _olde /
 * _mono), ArmorHazmatMask (ModelM65 for hazmat_helmet_red / _grey) and ArmorLiquidatorMask (ModelM65); the vanilla
 * armor pass of these helmets is hidden (see U4LegacyItemRenderSetup) as getArmorModel replaced it;</li>
 * <li>ModEventHandlerClient#onRenderArmorEvent (RenderPlayerEvent.SetArmorModel, players only): ItemArmorMod#modRender
 * of the installed mods (ItemModGasmask: ModelM65; JetpackBase / WingsMurk) and of a JetpackBase worn on its own.</li>
 * </ul>
 * Head / body poses come from the animated 1.21.1 parent model, as setRotationAngles copied them from the biped.
 */
public final class LegacyAccessoryArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public static final ModelLayerLocation GAS_MASK_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_gas_mask"), "main");
    public static final ModelLayerLocation M65_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_m65"), "main");
    public static final ModelLayerLocation JETPACK_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_jetpack"), "main");
    public static final ModelLayerLocation GOGGLES_LAYER = new ModelLayerLocation(AtomicLegacy.id("legacy_goggles"), "main");

    private static ResourceLocation tex(String path) {
        return AtomicLegacy.id("textures/" + path);
    }

    /** getArmorTexture of the helmets with a getArmorModel (ArmorGasMask, ArmorHazmatMask / ArmorFSB texture, ArmorLiquidatorMask). */
    private static final Map<String, ResourceLocation> M65_HELMETS = Map.of(
            "gas_mask_m65", tex("models/model_m65.png"),
            "gas_mask_olde", tex("armor/mask_olde.png"),
            "gas_mask_mono", tex("models/model_m65_mono.png"),
            "hazmat_helmet_red", tex("models/model_haz_red.png"),
            "hazmat_helmet_grey", tex("models/model_haz_grey.png"),
            "liquidator_helmet", tex("armor/liquidator_helmet.png"));
    private static final ResourceLocation GAS_MASK_TEX = tex("models/gas_mask.png");
    /** [parity z1] ArmorModel#getArmorTexture for goggles: textures/models/Goggles.png. */
    public static final ResourceLocation GOGGLES_TEX = tex("models/goggles.png");
    /** ItemModGasmask tex / tex_mono. */
    private static final Map<String, ResourceLocation> MASK_ATTACHMENTS = Map.of(
            "attachment_mask", tex("models/model_m65.png"),
            "attachment_mask_mono", tex("models/model_m65_mono.png"));
    /** JetpackRegular / JetpackBreak / JetpackVectorized / JetpackBooster getArmorTexture. */
    private static final Map<String, ResourceLocation> JETPACKS = Map.of(
            "jetpack_fly", tex("models/jet_pack_red.png"),
            "jetpack_break", tex("models/jet_pack_blue.png"),
            "jetpack_vector", tex("models/jet_pack_green.png"),
            "jetpack_boost", tex("models/jet_pack.png"));
    /** ModelArmorWings(this == wings_murk ? 0 : 1); getTexture() is wings_murk for both types. */
    private static final Map<String, Integer> WINGS = Map.of("wings_murk", 0, "wings_limp", 1);
    private static final ResourceLocation WINGS_MURK_TEX = tex("armor/wings_murk.png");
    /** ResourceManager.armor_wings (models/armor/murk.obj). */
    private static final LegacyEntityObjMesh armor_wings = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/armor/murk.obj"));
    /** [parity j2] ResourceManager.wings_bob / wings_black (ModelArmorWings#getTexture for types 2 / 3). */
    private static final ResourceLocation WINGS_BOB_TEX = tex("armor/wings_bob.png");
    private static final ResourceLocation WINGS_BLACK_TEX = tex("armor/wings_black.png");
    /** [parity j2] ResourceManager.armor_axepack (models/armor/wings_pheo.obj) / wings_pheo (textures/armor/axepack.png). */
    private static final LegacyEntityObjMesh armor_axepack = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/armor/wings_pheo.obj"));
    private static final ResourceLocation WINGS_PHEO_TEX = tex("armor/axepack.png");
    /** [parity j2] ResourceManager.armor_tail (models/armor/tail_peep.obj) / tail_peep. */
    private static final LegacyEntityObjMesh armor_tail = LegacyEntityObjMesh.load(AtomicLegacy.id("models/obj/armor/tail_peep.obj"));
    private static final ResourceLocation TAIL_PEEP_TEX = tex("armor/tail_peep.png");

    /** Helmet ids whose vanilla armor pass is replaced by this layer. */
    public static final java.util.Set<String> HELMET_IDS = java.util.Set.of("gas_mask", "gas_mask_m65", "gas_mask_olde",
            "gas_mask_mono", "hazmat_helmet_red", "hazmat_helmet_grey", "liquidator_helmet");

    private ModelPart gasMask;
    private ModelPart m65;
    private ModelPart jetpack;
    private ModelPart goggles;

    public LegacyAccessoryArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private void bake() {
        if (gasMask == null) {
            var models = Minecraft.getInstance().getEntityModels();
            gasMask = models.bakeLayer(GAS_MASK_LAYER);
            m65 = models.bakeLayer(M65_LAYER);
            jetpack = models.bakeLayer(JETPACK_LAYER);
            goggles = models.bakeLayer(GOGGLES_LAYER);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        bake();
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            String helmetId = id(helmet);
            if (helmetId.equals("gas_mask")) {
                renderGasMask(poseStack, bufferSource, packedLight, entity, GAS_MASK_TEX, helmet.hasFoil());
            } else if (helmetId.equals("goggles")) {
                renderGoggles(poseStack, bufferSource, packedLight, helmet.hasFoil());
            } else if (M65_HELMETS.containsKey(helmetId)) {
                renderM65(poseStack, bufferSource, packedLight, entity, M65_HELMETS.get(helmetId), helmet.hasFoil());
            }
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        // for(i < 4) armor = getCurrentArmor(i): installed mods, then a JetpackBase worn on its own
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor, player.registryAccess())) {
                if (mod == null || mod.isEmpty()) continue;
                modRender(poseStack, bufferSource, packedLight, player, id(mod));
            }
            if (slot == EquipmentSlot.CHEST) {
                String worn = id(armor);
                if (JETPACKS.containsKey(worn) || WINGS.containsKey(worn)) {
                    modRender(poseStack, bufferSource, packedLight, player, worn);
                }
            }
        }
        // [parity j2] onRenderArmorEvent: getCurrentArmor(2) == null && !isPotionActive(invisibility) -> named accessories
        if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && !player.hasEffect(MobEffects.INVISIBILITY)) {
            switch (ShadyUtil.accessoryFor(player.getUUID().toString(), player.getGameProfile().getName())) {
                case 2 -> renderWings(poseStack, bufferSource, packedLight, player, 2, WINGS_BOB_TEX);
                case 3 -> renderWings(poseStack, bufferSource, packedLight, player, 3, WINGS_BLACK_TEX);
                case 10 -> renderAxePack(poseStack, bufferSource, packedLight, player);
                case 11 -> renderTail(poseStack, bufferSource, packedLight);
                default -> { }
            }
        }
    }

    /** [parity j2] ModelArmorWingsPheo#render: the Wings group on bipedBody (no culling), then eight additive flames. */
    private void renderAxePack(PoseStack pose, MultiBufferSource buffers, int light, Player entity) {
        pose.pushPose();
        this.getParentModel().body.translateAndRotate(pose);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        armor_axepack.renderGroup("Wings", pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WINGS_PHEO_TEX)), light,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();

        // the flames are drawn in the model root (not on the body part), lightmap 240 / 240, lighting and texture off
        pose.pushPose();
        VertexConsumer flame = buffers.getBuffer(HbmUnlitRenderTypes.ADDITIVE_COLOR_TRIANGLES);
        double pixel = 0.0625D;
        if (entity.isCrouching()) {
            glRotate(pose, 28.6479D, 1, 0, 0);
        }
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(0, pixel * 15, pixel * 5.5);
            renderFlame(pose, flame);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, pixel * 3, pixel * 5.5);
            glRotate(pose, 25 * side, 0, 1, 0);
            glRotate(pose, 90 * side, 0, 0, 1);
            pose.translate(0, pixel * 5, 0);
            renderFlame(pose, flame);

            pose.pushPose();
            pose.translate(0, -pixel * 5, 0);
            glRotate(pose, 45, 0, 0, 1);
            pose.translate(-pixel, pixel * 5.5, 0);
            renderFlame(pose, flame);
            pose.popPose();

            pose.pushPose();
            pose.translate(0, -pixel * 5, 0);
            glRotate(pose, -45, 0, 0, 1);
            pose.translate(pixel, pixel * 5.5, 0);
            renderFlame(pose, flame);
            pose.popPose();
            pose.popPose();
        }
        pose.popPose();
    }

    /** ModelArmorWingsPheo#renderFlame: 16 GL_TRIANGLES, base 0x808080, ring 0x004040, tip 0x000000. */
    private static void renderFlame(PoseStack pose, VertexConsumer consumer) {
        double b = 0.125D;
        double t = 0.375;
        double w = 0.0625D;
        double s2 = Math.sqrt(2D) / 2D * w;
        double[][] ring = {{w, 0}, {s2, s2}, {0, w}, {-s2, s2}, {-w, 0}, {-s2, -s2}, {0, -w}, {s2, -s2}, {w, 0}};
        var matrix = pose.last().pose();
        for (double apex : new double[] {0, t}) {
            int apexColor = apex == 0 ? 0xFF808080 : 0xFF000000;
            for (int i = 0; i < 8; i++) {
                consumer.addVertex(matrix, 0F, (float) apex, 0F).setColor(apexColor);
                consumer.addVertex(matrix, (float) ring[i][0], (float) b, (float) ring[i][1]).setColor(0xFF004040);
                consumer.addVertex(matrix, (float) ring[i + 1][0], (float) b, (float) ring[i + 1][1]).setColor(0xFF004040);
            }
        }
    }

    /** [parity j2] ModelArmorTailPeep#render: bipedBody.copyTo(tail), group FaggyAssFuckingTailThing (no culling). */
    private void renderTail(PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        this.getParentModel().body.translateAndRotate(pose);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        armor_tail.renderGroup("FaggyAssFuckingTailThing", pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TAIL_PEEP_TEX)),
                light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }

    private void modRender(PoseStack pose, MultiBufferSource buffers, int light, Player player, String modId) {
        if (MASK_ATTACHMENTS.containsKey(modId)) {
            renderM65(pose, buffers, light, player, MASK_ATTACHMENTS.get(modId), false);
        } else if (JETPACKS.containsKey(modId)) {
            renderJetpack(pose, buffers, light, JETPACKS.get(modId));
        } else if (WINGS.containsKey(modId)) {
            renderWings(pose, buffers, light, player, WINGS.get(modId), WINGS_MURK_TEX);
        }
    }

    /** this.mask.rotationPoint(X, Y) / rotateAngle(Y, X) = bipedHead's; rotationPointZ and rotateAngleZ stay 0. */
    private void poseFromHead(ModelPart part) {
        ModelPart head = this.getParentModel().head;
        part.x = head.x;
        part.y = head.y;
        part.z = 0.0F;
        part.xRot = head.xRot;
        part.yRot = head.yRot;
        part.zRot = 0.0F;
    }

    /** ModelGasMask#render. */
    private void renderGasMask(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, ResourceLocation texture, boolean foil) {
        pose.pushPose();
        if (entity.isBaby()) {
            float f6 = 2.0F;
            pose.scale(1.5F / f6, 1.5F / f6, 1.5F / f6);
            pose.translate(0.0F, 16.0F * 0.0625F, 0.0F);
        } else {
            pose.scale(1.15F, 1.15F, 1.15F);
        }
        ModelPart mask = gasMask.getChild("mask");
        poseFromHead(mask);
        mask.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
        if (foil) mask.render(pose, buffers.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** [parity z1] ModelGoggles#render: google takes bipedHead's rotation point X / Y and angles Y / X, no child scaling. */
    private void renderGoggles(PoseStack pose, MultiBufferSource buffers, int light, boolean foil) {
        ModelPart google = goggles.getChild("google");
        poseFromHead(google);
        google.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(GOGGLES_TEX)), light, OverlayTexture.NO_OVERLAY);
        if (foil) google.render(pose, buffers.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
    }

    /** ModelM65#render: the filter only while ArmorUtil.getGasMaskFilterRecursively(helmet) is not null. */
    private void renderM65(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, ResourceLocation texture, boolean foil) {
        pose.pushPose();
        double d = 1D / 16D * 18D;
        if (entity.isBaby()) {
            float f6 = 2.0F;
            pose.scale(1.5F / f6, 1.5F / f6, 1.5F / f6);
            pose.translate(0.0F, 16.0F * 0.0625F, 0.0F);
        }
        pose.scale((float) d, (float) d, (float) d);
        pose.scale(1.01F, 1.01F, 1.01F);
        ModelPart mask = m65.getChild("mask");
        ModelPart filter = m65.getChild("filter");
        poseFromHead(mask);
        poseFromHead(filter);
        boolean showFilter = hasFilterRecursively(entity.getItemBySlot(EquipmentSlot.HEAD), entity);
        VertexConsumer consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(texture));
        mask.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        if (showFilter) filter.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        if (foil) {
            VertexConsumer glint = buffers.getBuffer(RenderType.armorEntityGlint());
            mask.render(pose, glint, light, OverlayTexture.NO_OVERLAY);
            if (showFilter) filter.render(pose, glint, light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }

    /** ArmorUtil#getGasMaskFilterRecursively: the helmet's own filter, else that of an IGasMask in its helmet_only mod slot. */
    private static boolean hasFilterRecursively(ItemStack mask, LivingEntity entity) {
        if (mask.isEmpty()) return false;
        var registries = entity.registryAccess();
        if (!GasMaskItem.getInstalledFilter(mask, registries).isEmpty()) return true;
        ItemStack[] mods = ArmorModHandler.pryMods(mask, registries);
        if (mods.length > ArmorModHandler.HELMET_ONLY) {
            ItemStack mod = mods[ArmorModHandler.HELMET_ONLY];
            if (mod != null && mod.getItem() instanceof com.atomiclegacy.items.FilterableGasMask) {
                return !GasMaskItem.getInstalledFilter(mod, registries).isEmpty();
            }
        }
        return false;
    }

    /** JetpackBase#modRender with ModelJetPack: JetPack copies bipedBody's rotation point and angles. */
    private void renderJetpack(PoseStack pose, MultiBufferSource buffers, int light, ResourceLocation texture) {
        ModelPart root = jetpack.getChild("JetPack");
        root.copyFrom(this.getParentModel().body);
        root.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
    }

    /** ModelArmorWings#render (doesRotateZ true), texture wings_murk for types 0 and 1, wings_bob / wings_black for 2 / 3. */
    private void renderWings(PoseStack pose, MultiBufferSource buffers, int light, Player entity, int type, ResourceLocation texture) {
        pose.pushPose();
        double px = 0.0625D;
        double rot = Math.sin((entity.tickCount) * 0.2D) * 20;
        double rot2 = Math.sin((entity.tickCount) * 0.2D - Math.PI * 0.5) * 50 + 30;
        int pivotSideOffset = 1;
        int pivotFrontOffset = 5;
        int pivotZOffset = 3;
        int tipSideOffset = 16;
        int tipZOffset = 2;
        double inwardAngle = 10D;

        pose.pushPose();
        // body offset, rotation point and Z / Y / X angles
        this.getParentModel().body.translateAndRotate(pose);

        if (type != 1 && entity.onGround()) {
            rot = 20;
            rot2 = 160;
        }
        if (type == 1) {
            if (entity.onGround()) {
                rot = 30;
                rot2 = -30;
            } else if (entity.getDeltaMovement().y < -0.1) {
                rot = 0;
                rot2 = 10;
            } else {
                rot = 30;
                rot2 = 20;
            }
        }

        pose.translate(0, -2 * px, 0);
        // GL_CULL_FACE enabled
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(texture));

        pose.pushPose();
        glRotate(pose, -inwardAngle, 0, 1, 0);
        pose.translate(pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        glRotate(pose, rot * 0.5, 0, 1, 0);
        glRotate(pose, rot + 5, 0, 0, 1);
        glRotate(pose, 45, 1, 0, 0);
        pose.translate(-pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        pose.translate(pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        glRotate(pose, rot, 0, 0, 1);
        pose.translate(-pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        wingPart("LeftBase", pose, consumer, light);
        pose.translate(tipSideOffset * px, pivotFrontOffset * px, tipZOffset * px);
        glRotate(pose, rot2, 0, 1, 0);
        glRotate(pose, rot2 * 0.25 + 5, 0, 0, 1);
        pose.translate(-tipSideOffset * px, -pivotFrontOffset * px, -tipZOffset * px);
        wingPart("LeftTip", pose, consumer, light);
        pose.popPose();

        pose.pushPose();
        glRotate(pose, inwardAngle, 0, 1, 0);
        pose.translate(-pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        glRotate(pose, -rot * 0.5, 0, 1, 0);
        glRotate(pose, -rot - 5, 0, 0, 1);
        glRotate(pose, 45, 1, 0, 0);
        pose.translate(pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        pose.translate(-pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        glRotate(pose, -rot, 0, 0, 1);
        pose.translate(pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        wingPart("RightBase", pose, consumer, light);
        pose.translate(-tipSideOffset * px, pivotFrontOffset * px, tipZOffset * px);
        glRotate(pose, -rot2, 0, 1, 0);
        glRotate(pose, -rot2 * 0.25 - 5, 0, 0, 1);
        pose.translate(tipSideOffset * px, -pivotFrontOffset * px, -tipZOffset * px);
        wingPart("RightTip", pose, consumer, light);
        pose.popPose();

        pose.popPose();
        pose.popPose();
    }

    /** ModelRendererObj#render(0.0625) with no rotation point or angles. */
    private static void wingPart(String part, PoseStack pose, VertexConsumer consumer, int light) {
        pose.pushPose();
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        armor_wings.renderGroup(part, pose, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }

    private static void glRotate(PoseStack pose, double angle, double x, double y, double z) {
        ItemRenderWeaponBase.glRotate(pose, angle, x, y, z);
    }

    // ------------------------------------------------------------------ layer definitions

    private static void child(PartDefinition parent, String name, int u, int v, float bx, float by, float bz, float w, float h, float d,
                              float px, float py, float pz, float rx) {
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(bx, by, bz, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, rx, 0.0F, 0.0F));
    }

    /** ModelGasMask (64 x 32); convertToChild against a mask root at the origin leaves the children unchanged. */
    public static LayerDefinition createGasMask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition mask = mesh.getRoot().addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);
        float o = 0.075F / 2;
        child(mask, "Shape1", 0, 0, 0F, 0F, 0F, 8, 8, 3, 0F - 4, 0F - 8 + o, 0F - 4, 0F);
        child(mask, "Shape2", 22, 0, 0F, 0F, 0F, 2, 2, 1, 1F - 4, 3F - 8 + o, -0.5333334F - 4, 0F);
        child(mask, "Shape3", 22, 0, 0F, 0F, 0F, 2, 2, 1, 5F - 4, 3F - 8 + o, -0.5F - 4, 0F);
        child(mask, "Shape4", 0, 11, 0F, 0F, 0F, 2, 2, 2, 3F - 4, 5F - 8 + o, 0F - 4, -0.7853982F);
        child(mask, "Shape5", 0, 15, 0F, 2F, -0.5F, 3, 4, 3, 2.5F - 4, 5F - 8 + o, 0F - 4, -0.7853982F);
        child(mask, "Shape6", 0, 22, 0F, 0F, 0F, 8, 1, 5, 0F - 4, 3F - 8 + o, 3F - 4, 0F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelM65 (32 x 32): mask and filter roots at the origin. */
    public static LayerDefinition createM65() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition mask = mesh.getRoot().addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition filter = mesh.getRoot().addOrReplaceChild("filter", CubeListBuilder.create(), PartPose.ZERO);
        float yOffset = 0.5F;
        child(mask, "maskHead", 0, 0, 0F, 0F, 0F, 8, 8, 8, -4F, -8F + yOffset, -4F, 0F);
        child(mask, "nose", 0, 16, 0F, 0F, 0F, 3, 3, 1, -1.5F, -3.5F + yOffset, -5F, 0F);
        child(mask, "outlet", 0, 20, 0F, -2F, 0F, 2, 2, 1, -1F, -3.5F + yOffset, -5F, -0.4799655F);
        child(mask, "noseSlope", 8, 16, 0F, 0F, -2F, 3, 2, 2, -1.5F, -2F + yOffset, -4F, 0.6108652F);
        child(mask, "eye1", 0, 23, 0F, 0F, 0F, 3, 3, 0, -3.5F, -6F + yOffset, -4.2F, 0F);
        child(mask, "eye2", 0, 26, 0F, 0F, 0F, 3, 3, 0, 0.5F, -6F + yOffset, -4.2F, 0F);
        child(mask, "iForgot", 6, 20, 0F, 0F, 0F, 2, 2, 1, -1F, -3.2F + yOffset, -6F, 0F);
        child(filter, "filterConnector", 6, 23, 0F, 0F, -3F, 2, 2, 1, -1F, -2F + yOffset, -4F, 0.6108652F);
        child(filter, "filter1", 18, 21, 0F, -1F, -5F, 3, 4, 2, -1.5F, -2F + yOffset, -4F, 0.6108652F);
        child(filter, "filter2", 18, 16, 0F, -0.5F, -5F, 4, 3, 2, -2F, -2F + yOffset, -4F, 0.6108652F);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** ModelJetPack (32 x 32): JetPack built at (0, 0, -2), so convertToChild moved every child +2 on z. */
    public static LayerDefinition createJetpack() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("JetPack", CubeListBuilder.create(), PartPose.ZERO);
        float dz = 2F;
        child(root, "Pack", 12, 10, 0F, 0F, 0F, 4, 6, 1, -2F, 3F, 0F + dz, 0F);
        child(root, "Tank1", 0, 0, 0F, 0F, 0F, 3, 8, 3, 0.5F, 2F, 0.5F + dz, 0F);
        child(root, "Tank2", 0, 11, 0F, 0F, 0F, 3, 8, 3, -3.5F, 2F, 0.5F + dz, 0F);
        child(root, "Tip1", 0, 22, 0F, 0F, 0F, 2, 1, 2, 1F, 1F, 1F + dz, 0F);
        child(root, "Tip2", 0, 25, 0F, 0F, 0F, 2, 1, 2, -3F, 1F, 1F + dz, 0F);
        child(root, "Duct1", 8, 22, 0F, 0F, 0F, 2, 1, 2, 1F, 9.5F, 1F + dz, 0F);
        child(root, "Duct2", 8, 25, 0F, 0F, 0F, 2, 1, 2, -3F, 9.5F, 1F + dz, 0F);
        child(root, "Thruster1", 12, 0, 0F, 0F, 0F, 3, 2, 3, 0.5F, 10.5F, 0.5F + dz, 0F);
        child(root, "Thruster2", 12, 5, 0F, 0F, 0F, 3, 2, 3, -3.5F, 10.5F, 0.5F + dz, 0F);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** [parity z1] ModelGoggles (64 x 32): google root at the origin, so convertToChild leaves the children unchanged. */
    public static LayerDefinition createGoggles() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition google = mesh.getRoot().addOrReplaceChild("google", CubeListBuilder.create(), PartPose.ZERO);
        child(google, "Shape1", 0, 0, 0F, 0F, 0F, 9, 3, 1, -4.5F, -3F - 2, -4.5F, 0F);
        child(google, "Shape2", 0, 4, 0F, 0F, 0F, 9, 2, 5, -4.5F, -3F - 2, -3.5F, 0F);
        child(google, "Shape5", 26, 0, 0F, 0F, 0F, 2, 2, 1, 1F, -2.5F - 2, -5F, 0F);
        child(google, "Shape6", 20, 0, 0F, 0F, 0F, 2, 2, 1, -3F, -2.5F - 2, -5F, 0F);
        child(google, "Shape7", 0, 11, 0F, 0F, 0F, 9, 1, 4, -4.5F, -3F - 2, 0.5F, 0F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Adds the layer to a humanoid renderer (players and the humanoid mobs the T-51 layer also covers). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void attach(LivingEntityRenderer renderer) {
        if (renderer.getModel() instanceof HumanoidModel) {
            renderer.addLayer(new LegacyAccessoryArmorLayer(renderer));
        }
    }
}

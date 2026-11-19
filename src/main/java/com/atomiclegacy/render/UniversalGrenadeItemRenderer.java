package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.UniversalGrenadeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Port of 1.7.10's ItemRenderGrenade.
 *
 * <p>The legacy renderer used one OBJ with ten named groups and rendered the
 * shell, filling, label and fuze as successive texture/tint passes.  Keeping
 * those passes separate is important: all 475 completed grenade combinations
 * share the same geometry, but the filling and fuze colours are part of the
 * old visual identity.</p>
 */
public final class UniversalGrenadeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<UniversalGrenadeItem.Shell, AABB> GROUND_BOUNDS =
            new EnumMap<>(UniversalGrenadeItem.Shell.class);
    private static final ModelResourceLocation FRAG = model("grenade_frag");
    private static final ModelResourceLocation FRAG_BODY = model("grenade_frag_body");
    private static final ModelResourceLocation FRAG_LABEL = model("grenade_frag_label");
    private static final ModelResourceLocation FRAG_FUZE = model("grenade_frag_fuze");
    private static final ModelResourceLocation FRAG_SPOON = model("grenade_frag_spoon");
    private static final ModelResourceLocation FRAG_RING = model("grenade_frag_ring");

    private static final ModelResourceLocation STICK = model("grenade_stick");
    private static final ModelResourceLocation STICK_BODY = model("grenade_stick_body");
    private static final ModelResourceLocation STICK_CAP_BODY = model("grenade_stick_cap_body");
    // [parity H2: grenade_universal] StickCap with grenade_stick_tex, drawn by renderFirstPerson before the tinted overlay
    private static final ModelResourceLocation STICK_CAP = model("grenade_stick_cap");
    private static final ModelResourceLocation STICK_LABEL = model("grenade_stick_label");
    private static final ModelResourceLocation STICK_FUZE = model("grenade_stick_fuze");

    private static final ModelResourceLocation TECH = model("grenade_tech");
    private static final ModelResourceLocation TECH_BODY = model("grenade_tech_body");
    private static final ModelResourceLocation TECH_LIGHTS = model("grenade_tech_lights");
    private static final ModelResourceLocation TECH_FUZE = model("grenade_tech_fuze");
    private static final ModelResourceLocation TECH_RING = model("grenade_tech_ring");

    private static final ModelResourceLocation NUKA = model("grenade_nuka");
    private static final ModelResourceLocation NUKA_BODY = model("grenade_nuka_body");
    private static final ModelResourceLocation NUKA_LABEL = model("grenade_nuka_label");
    private static final ModelResourceLocation NUKA_FUZE = model("grenade_nuka_fuze");
    private static final ModelResourceLocation NUKA_SPOON = model("grenade_nuka_spoon");
    private static final ModelResourceLocation NUKA_RING = model("grenade_nuka_ring");

    public UniversalGrenadeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("weapons/" + name);
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FRAG);
        event.register(FRAG_BODY);
        event.register(FRAG_LABEL);
        event.register(FRAG_FUZE);
        event.register(FRAG_SPOON);
        event.register(FRAG_RING);
        event.register(STICK);
        event.register(STICK_BODY);
        event.register(STICK_CAP_BODY);
        event.register(STICK_CAP);
        event.register(STICK_LABEL);
        event.register(STICK_FUZE);
        event.register(TECH);
        event.register(TECH_BODY);
        event.register(TECH_LIGHTS);
        event.register(TECH_FUZE);
        event.register(TECH_RING);
        event.register(NUKA);
        event.register(NUKA_BODY);
        event.register(NUKA_LABEL);
        event.register(NUKA_FUZE);
        event.register(NUKA_SPOON);
        event.register(NUKA_RING);
    }

    static synchronized void invalidateGroundBounds() {
        GROUND_BOUNDS.clear();
    }

    /**
     * [parity S4: culling] Rotation-invariant radius of the thrown grenade (RenderGrenadeUniversal): the same
     * standalone baked models under scale 0.0625 and applyShellTransform(shell, LegacyView.THROWN). The entity's
     * yaw / spin / -80 degree rotations act around the origin, so the farthest corner distance bounds them all.
     * A normal item-model snapshot cannot see BEWLR vertices, so the entity renderer obtains its envelope here.
     */
    static synchronized double thrownRadius(ItemStack stack) {
        UniversalGrenadeItem.Shell shell = UniversalGrenadeItem.shell(stack);
        AABB cached = GROUND_BOUNDS.get(shell);
        if (cached == null) {
            PoseStack poseStack = new PoseStack();
            poseStack.scale(0.0625F, 0.0625F, 0.0625F);
            applyShellTransform(shell, LegacyView.THROWN, poseStack);
            List<ModelResourceLocation> models = switch (shell) {
                case FRAG -> List.of(FRAG, FRAG_BODY, FRAG_LABEL, FRAG_FUZE, FRAG_SPOON, FRAG_RING);
                case STICK -> List.of(STICK, STICK_BODY, STICK_CAP_BODY, STICK_LABEL, STICK_FUZE);
                case TECH -> List.of(TECH, TECH_BODY, TECH_LIGHTS, TECH_FUZE, TECH_RING);
                case NUKE -> List.of(NUKA, NUKA_BODY, NUKA_LABEL, NUKA_FUZE, NUKA_SPOON, NUKA_RING);
            };
            var state = Blocks.IRON_BLOCK.defaultBlockState();
            for (ModelResourceLocation model : models) {
                AABB part = BakedBlockGeometrySnapshot.build(
                        MachineModelRenderer.model(model), state, List.of(poseStack),
                        net.minecraft.core.BlockPos.ZERO).bounds();
                cached = cached == null ? part : cached.minmax(part);
            }
            if (cached == null) {
                cached = new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            GROUND_BOUNDS.put(shell, cached);
        }
        double x = Math.max(Math.abs(cached.minX), Math.abs(cached.maxX));
        double y = Math.max(Math.abs(cached.minY), Math.abs(cached.maxY));
        double z = Math.max(Math.abs(cached.minZ), Math.abs(cached.maxZ));
        return Math.nextUp(Math.sqrt(x * x + y * y + z * z));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            renderFirstPerson(stack, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            applyLegacyTransform(UniversalGrenadeItem.shell(stack), context, poseStack);
            renderGrenade(stack, true, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * [parity H2: grenade_universal] RenderGrenadeUniversal: ItemRenderGrenade.renderGrenade(stack, null), i.e. the
     * shell transform of the thrown grenade and the body without spoon, ring or cap.
     */
    public static void renderThrown(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        applyShellTransform(UniversalGrenadeItem.shell(stack), LegacyView.THROWN, poseStack);
        renderGrenade(stack, false, poseStack, bufferSource, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    /** [parity H2: grenade_universal] ItemRenderGrenade.renderFirstPerson with the BODY/RING buses of the EQUIP animation. */
    private static void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {
        UniversalGrenadeItem.Shell shell = UniversalGrenadeItem.shell(stack);
        UniversalGrenadeItem.Filling filling = UniversalGrenadeItem.filling(stack);
        var state = Blocks.IRON_BLOCK.defaultBlockState();

        poseStack.scale(0.125F, 0.125F, 0.125F);
        poseStack.translate(3.0F, 1.0F, -3.0F);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));

        double[] bodyMove = HbmAnimations.getRelevantTransformation("BODYMOVE");
        double[] bodyTurn = HbmAnimations.getRelevantTransformation("BODYTURN");
        double[] ringMove = HbmAnimations.getRelevantTransformation("RINGMOVE");
        double[] ringTurn = HbmAnimations.getRelevantTransformation("RINGTURN");
        double[] renderRing = HbmAnimations.getRelevantTransformation("RENDERRING");

        poseStack.translate(bodyMove[0], bodyMove[1], bodyMove[2]);

        switch (shell) {
            case FRAG -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) bodyTurn[2]));
                renderGrenade(stack, false, poseStack, bufferSource, packedLight, packedOverlay);
                render(FRAG_SPOON, poseStack, bufferSource, state, packedLight, packedOverlay);
                if (renderRing[0] != 0) {
                    poseStack.translate(ringMove[0], ringMove[1], ringMove[2]);
                    poseStack.mulPose(Axis.XP.rotationDegrees((float) ringTurn[2]));
                    render(FRAG_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case STICK -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) bodyTurn[2]));
                renderGrenade(stack, false, poseStack, bufferSource, packedLight, packedOverlay);
                if (renderRing[0] != 0) {
                    poseStack.translate(ringMove[0], ringMove[1], ringMove[2]);
                    poseStack.mulPose(Axis.YP.rotationDegrees((float) ringTurn[1]));
                    render(STICK_CAP, poseStack, bufferSource, state, packedLight, packedOverlay);
                    renderTinted(STICK_CAP_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case TECH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) bodyTurn[2]));
                renderGrenade(stack, false, poseStack, bufferSource, packedLight, packedOverlay);
                if (renderRing[0] != 0) {
                    poseStack.translate(ringMove[0], ringMove[1], ringMove[2]);
                    poseStack.mulPose(Axis.XP.rotationDegrees((float) ringTurn[2]));
                    render(TECH_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case NUKE -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) bodyTurn[2]));
                renderGrenade(stack, false, poseStack, bufferSource, packedLight, packedOverlay);
                render(NUKA_SPOON, poseStack, bufferSource, state, packedLight, packedOverlay);
                if (renderRing[0] != 0) {
                    poseStack.translate(ringMove[0], ringMove[1], ringMove[2]);
                    poseStack.translate(-1.0F, 5.0F, 0.0F);
                    poseStack.mulPose(Axis.ZN.rotationDegrees((float) ringTurn[2]));
                    poseStack.translate(1.0F, -5.0F, 0.0F);
                    render(NUKA_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
        }
    }

    private static void applyLegacyTransform(UniversalGrenadeItem.Shell shell,
                                             ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                // Exact ItemRenderGrenade#INVENTORY matrix. The old GUI used
                // a 16-pixel coordinate space, hence the 1/16 conversion.
                poseStack.translate(0.5F, 0.5F, 0.0F);
                poseStack.scale(-1.0F / 16.0F, -1.0F / 16.0F, -1.0F / 16.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(150.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
                applyShellTransform(shell, LegacyView.INVENTORY, poseStack);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderGrenade#EQUIPPED: scale first, then translate in
                // authored OBJ units. PoseStack follows the same matrix order.
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.translate(3.0F, 1.0F, -0.5F);
                applyShellTransform(shell, LegacyView.EQUIPPED, poseStack);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // renderFirstPerson, without the animation buses (only used for the culling bounds)
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.translate(3.0F, 1.0F, -3.0F);
                poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
            }
            default -> {
                // [parity H2: grenade_universal] ItemRenderType.ENTITY (dropped item, item frame): only the 0.125
                // scale, renderGrenade has no shell transform for it. FIXED / HEAD / NONE have no 1.7.10 type.
                // The 1.7.10 origin is the item entity itself; the modern item frame starts at the corner of the
                // unit cube, so without this the dropped grenade sits half a block beside its position.
                poseStack.translate(0.5F, 0.25F, 0.5F);
                poseStack.scale(0.125F, 0.125F, 0.125F);
            }
        }
    }

    /** Shell-specific transforms from ItemRenderGrenade#renderGrenade. */
    private static void applyShellTransform(UniversalGrenadeItem.Shell shell,
                                            LegacyView view, PoseStack poseStack) {
        switch (view) {
            case INVENTORY -> {
                switch (shell) {
                    case FRAG -> {
                        poseStack.scale(3.0F, 3.0F, 3.0F);
                        poseStack.translate(0.0F, -2.0F, 0.0F);
                    }
                    case STICK -> {
                        poseStack.scale(2.0F, 2.0F, 2.0F);
                        poseStack.translate(0.0F, -4.5F, 0.0F);
                    }
                    case TECH -> {
                        poseStack.scale(3.5F, 3.5F, 3.5F);
                        poseStack.translate(0.0F, -1.75F, 0.0F);
                    }
                    case NUKE -> {
                        poseStack.scale(2.5F, 2.5F, 2.5F);
                        poseStack.translate(0.0F, -2.75F, 0.0F);
                    }
                }
            }
            case EQUIPPED -> {
                switch (shell) {
                    case FRAG -> {
                    }
                    case STICK -> poseStack.translate(0.0F, -2.0F, 0.0F);
                    case TECH -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.5F, -1.0F, 0.5F);
                    }
                    case NUKE -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.5F, -3.0F, 0.5F);
                    }
                }
            }
            case THROWN -> {
                // renderType == null (RenderGrenadeUniversal)
                switch (shell) {
                    case FRAG, STICK -> poseStack.translate(0.0F, -2.0F, 0.0F);
                    case TECH -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.0F, -1.0F, 0.0F);
                    }
                    case NUKE -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.0F, -3.0F, 0.0F);
                    }
                }
            }
        }
    }

    private enum LegacyView {
        INVENTORY,
        EQUIPPED,
        THROWN
    }

    /**
     * ItemRenderGrenade#renderGrenade without its transforms: the body passes of the shell and, for every render
     * type except the thrown entity, the static spoon / ring / cap.
     */
    private static void renderGrenade(ItemStack stack, boolean attachments, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        UniversalGrenadeItem.Shell shell = UniversalGrenadeItem.shell(stack);
        UniversalGrenadeItem.Filling filling = UniversalGrenadeItem.filling(stack);
        UniversalGrenadeItem.Fuze fuze = UniversalGrenadeItem.fuze(stack);
        var state = Blocks.IRON_BLOCK.defaultBlockState();
        switch (shell) {
            case FRAG -> {
                render(FRAG, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(FRAG_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(FRAG_LABEL, filling.labelColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(FRAG_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                if (attachments) {
                    render(FRAG_SPOON, poseStack, bufferSource, state, packedLight, packedOverlay);
                    render(FRAG_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case STICK -> {
                render(STICK, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(STICK_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(STICK_LABEL, filling.labelColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(STICK_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                if (attachments) {
                    // The old renderer used only the tinted body overlay for the
                    // static cap, so this is deliberately not a separate base texture.
                    renderTinted(STICK_CAP_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case TECH -> {
                render(TECH, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(TECH_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(TECH_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(TECH_LIGHTS, filling.labelColor(), poseStack, bufferSource, state,
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay);
                if (attachments) {
                    render(TECH_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case NUKE -> {
                render(NUKA, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(NUKA_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(NUKA_LABEL, filling.labelColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(NUKA_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                if (attachments) {
                    render(NUKA_SPOON, poseStack, bufferSource, state, packedLight, packedOverlay);
                    render(NUKA_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
        }
    }

    private static void render(ModelResourceLocation location, PoseStack poseStack, MultiBufferSource bufferSource,
                                net.minecraft.world.level.block.state.BlockState state, int packedLight, int packedOverlay) {
        BakedModel model = MachineModelRenderer.model(location);
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderTinted(ModelResourceLocation location, int color, PoseStack poseStack,
                                     MultiBufferSource bufferSource,
                                     net.minecraft.world.level.block.state.BlockState state,
                                     int packedLight, int packedOverlay) {
        BakedModel model = MachineModelRenderer.model(location);
        MachineModelRenderer.renderUnculledTinted(model, poseStack, bufferSource, state, packedLight, packedOverlay,
                0xFF000000 | color);
    }
}

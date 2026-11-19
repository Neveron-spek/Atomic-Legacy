package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyArtilleryShellEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyHimarsRocketEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.StandardAmmoItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class LegacyProjectileEntityRenderer {
    private static final ModelResourceLocation RICHARD_MISSILE = MachineModelRenderer.standalone("entity/legacy_richard_missile");
    private static final ModelResourceLocation ARTILLERY = MachineModelRenderer.standalone("entity/legacy_artillery_shell");
    private static final ModelResourceLocation[] HIMARS_MODELS = new ModelResourceLocation[LegacyProjectileUtil.HimarsType.values().length];
    private static final BlockState RENDER_STATE = Blocks.STONE.defaultBlockState();
    private static final LegacyEntityObjMesh PROJECTILE_BOUNDS_MODEL = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/obj/projectiles/projectiles.obj"));
    private static final LegacyEntityObjMesh RICHARD_BOUNDS_MODEL = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/obj/weapons/missile_launcher.obj"));
    private static final LegacyEntityObjMesh HIMARS_BOUNDS_MODEL = LegacyEntityObjMesh.load(
            AtomicLegacy.id("models/obj/turrets/turret_himars.obj"));
    private static final double RICHARD_RADIUS = buildRichardRadius();
    private static final double ARTILLERY_RADIUS = buildArtilleryRadius();
    private static final double HIMARS_STANDARD_RADIUS = rotationSafeRadius(
            HIMARS_BOUNDS_MODEL.maximumGroupFaceRadius("RocketStandard"));
    private static final double HIMARS_SINGLE_RADIUS = rotationSafeRadius(
            HIMARS_BOUNDS_MODEL.maximumGroupFaceRadius("RocketSingle"));

    static {
        for (LegacyProjectileUtil.HimarsType type : LegacyProjectileUtil.HimarsType.values()) {
            HIMARS_MODELS[type.modelData()] = MachineModelRenderer.standalone("entity/legacy_himars_" + type.id());
        }
    }

    private LegacyProjectileEntityRenderer() {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RICHARD_MISSILE);
        event.register(ARTILLERY);
        for (ModelResourceLocation model : HIMARS_MODELS) {
            event.register(model);
        }
    }

    static void renderRifleBullet(Entity entity, StandardAmmoItem.StandardAmmoType ammo, float partialTick,
                                  PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderTracer(entity, ammo, partialTick, poseStack, bufferSource, packedLight, 1.0D, false);
    }

    static double rifleBulletVisualRadius(Entity entity) {
        double displacement = frameDisplacement(entity);
        return Math.nextUp(displacement
                + Math.sqrt(displacement * displacement + 2.0D * 0.03125D * 0.03125D));
    }

    private static void renderTracer(Entity entity, StandardAmmoItem.StandardAmmoType ammo, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                     double lengthMultiplier, boolean fullBright) {
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz) * lengthMultiplier;
        if (length <= 0.0D) {
            return;
        }
        int dark;
        int light;
        switch (ammo) {
            case BMG50_AP, R556_AP, P9_AP, P22_AP, M357_AP, R762_AP -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
            }
            // LegoClient.RENDER_EXPRESS_BULLET
            case M357_EXPRESS -> {
                dark = 0x9E082E;
                light = 0xFF8A79;
            }
            case BMG50_DU, R762_DU -> {
                dark = 0x5CCD41;
                light = 0xE9FF8D;
            }
            // LegoClient.RENDER_HE_BULLET / RENDER_SM_BULLET (full bright)
            case BMG50_HE, R762_HE -> {
                dark = 0xD8CA00;
                light = 0xFFF19D;
                fullBright = true;
            }
            case BMG50_SM -> {
                dark = 0x42A8DD;
                light = 0xFFFFFF;
                fullBright = true;
            }
            // [parity H2: grenade_universal] GunFactoryClient: ItemGrenadeFilling.fragmentation / pellets / pellets_heavy
            // use LegoClient.RENDER_FRAGMENTATION (renderBulletStandard 0xFF6A00 / 0xFFE28D, full bright)
            case GRENADE_FRAGMENTATION, GRENADE_PELLETS, GRENADE_PELLETS_HEAVY -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
                fullBright = true;
            }
            // [C2] XFactory44 / GunFactoryClient: m44_ap RENDER_AP_BULLET, m44_express RENDER_EXPRESS_BULLET,
            // m44_equestrian_pip / _mn7 RENDER_LEGENDARY_BULLET (full bright)
            case M44_AP -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
            }
            case M44_EXPRESS -> {
                dark = 0x9E082E;
                light = 0xFF8A79;
            }
            case M44_EQUESTRIAN_PIP, M44_EQUESTRIAN_MN7 -> {
                dark = 0x7F006E;
                light = 0xFF7FED;
                fullBright = true;
            }
            // [D2] GunFactoryClient: r556_inc_* RENDER_AP_BULLET, bmg50_black RENDER_BLACK_BULLET (full bright)
            case R556_INC_SP, R556_INC_FMJ, R556_INC_JHP, R556_INC_AP -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
            }
            case BMG50_BLACK -> {
                dark = 0x000000;
                light = 0x7F006E;
                fullBright = true;
            }
            // [weapons batch E1] GunFactoryClient: b75 / b75_inc RENDER_AP_BULLET, b75_exp RENDER_EXPRESS_BULLET
            case B75, B75_INC -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
            }
            case B75_EXP -> {
                dark = 0x9E082E;
                light = 0xFF8A79;
            }
            // [weapons batch F2] GunFactoryClient: coil_tungsten / coil_ferrouranium RENDER_AP_BULLET
            case COIL_TUNGSTEN, COIL_FERROURANIUM -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
            }
            default -> {
                dark = 0xFFBF00;
                light = 0xFFFFFF;
            }
        }
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        // 1.7.10 rotationPitch is positive upwards; LegacyBulletEntity stores the vanilla (negated) xRot.
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        renderTracerGeometry(poseStack.last(), bufferSource.getBuffer(HbmUnlitRenderTypes.TRACER), length,
                0.03125D, 0.03125D * 0.25D, dark, light,
                fullBright ? LightTexture.FULL_BRIGHT : packedLight);
        poseStack.popPose();
    }

    private static void renderRichardMissile(LegacyBulletEntity entity, float partialTick, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        // 1.7.10 rotationPitch is positive upwards; LegacyBulletEntity stores the vanilla (negated) xRot.
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, -4.5D);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(RICHARD_MISSILE), poseStack, bufferSource, RENDER_STATE, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); // [fix: overlay 0 = red hurt tint]
        poseStack.popPose();
        poseStack.translate(0.375D, 0.0D, 0.0D);
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0D;
        if (length > 0.0D) {
            renderTracerGeometry(poseStack.last(), bufferSource.getBuffer(HbmUnlitRenderTypes.TRACER), length,
                    0.03125D, 0.03125D * 0.25D, 0x808080, 0xFFF2A7, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    static void renderTracerGeometry(PoseStack.Pose pose, VertexConsumer consumer, double length,
                                             double frontWidth, double backWidth,
                                             int dark, int light, int packedLight) {
        tracerQuad(consumer, pose,
                length, backWidth, -backWidth, dark,
                length, backWidth, backWidth, dark,
                0.0D, frontWidth, frontWidth, light,
                0.0D, frontWidth, -frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, -backWidth, -backWidth, dark,
                length, -backWidth, backWidth, dark,
                0.0D, -frontWidth, frontWidth, light,
                0.0D, -frontWidth, -frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, -backWidth, backWidth, dark,
                length, backWidth, backWidth, dark,
                0.0D, frontWidth, frontWidth, light,
                0.0D, -frontWidth, frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, -backWidth, -backWidth, dark,
                length, backWidth, -backWidth, dark,
                0.0D, frontWidth, -frontWidth, light,
                0.0D, -frontWidth, -frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, backWidth, backWidth, dark,
                length, backWidth, -backWidth, dark,
                length, -backWidth, -backWidth, dark,
                length, -backWidth, backWidth, dark,
                packedLight);
        tracerQuad(consumer, pose,
                0.0D, frontWidth, frontWidth, light,
                0.0D, frontWidth, -frontWidth, light,
                0.0D, -frontWidth, -frontWidth, light,
                0.0D, -frontWidth, frontWidth, light,
                packedLight);
    }

    private static void tracerQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                   double ax, double ay, double az, int ac,
                                   double bx, double by, double bz, int bc,
                                   double cx, double cy, double cz, int cc,
                                   double dx, double dy, double dz, int dc,
                                   int packedLight) {
        tracerVertex(consumer, pose, ax, ay, az, ac, packedLight);
        tracerVertex(consumer, pose, bx, by, bz, bc, packedLight);
        tracerVertex(consumer, pose, cx, cy, cz, cc, packedLight);
        tracerVertex(consumer, pose, dx, dy, dz, dc, packedLight);
    }

    private static void tracerVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                     double x, double y, double z, int color, int packedLight) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void renderArtilleryShell(LegacyArtilleryShellEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch - 90.0F));
        poseStack.scale(2.5F, 5.0F, 2.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ARTILLERY), poseStack, bufferSource, RENDER_STATE, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); // [fix: overlay 0 = red hurt tint]
        poseStack.popPose();
    }

    private static void renderHimarsRocket(LegacyHimarsRocketEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch - 90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(HIMARS_MODELS[entity.rocketType().modelData()]),
                poseStack,
                bufferSource,
                RENDER_STATE,
                packedLight,
                0
        );
        poseStack.popPose();
    }

    public static class Bullet extends EntityRenderer<LegacyBulletEntity> {
        public Bullet(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(LegacyBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            StandardAmmoItem.StandardAmmoType ammo = entity.ammoType();
            if (GunBatchF3BulletRenderer.handles(ammo)) { // [weapons batch F3] charge thrower rounds, extinguisher (no renderer)
                GunBatchF3BulletRenderer.render(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (GunBatchD3BulletRenderer.handles(ammo)) { // [weapons batch D3] flares / 40mm grenades
                GunBatchD3BulletRenderer.render(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (GunBatchE1BulletRenderer.handles(ammo)) { // [weapons batch E1] gun rockets
                GunBatchE1BulletRenderer.render(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (GunBatchF4BulletRenderer.handles(ammo)) { // [weapons batch F4] folly_nuke
                GunBatchF4BulletRenderer.render(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (GunBatchF2BulletRenderer.handles(ammo)) { // [weapons batch F2] Fat Man mini nukes
                GunBatchF2BulletRenderer.render(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
                renderRichardMissile(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (ammo.family() != StandardAmmoItem.AmmoFamily.FLAME) {
                renderRifleBullet(entity, ammo, partialTick, poseStack, bufferSource, packedLight);
            }
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        @Override
        public boolean shouldRender(LegacyBulletEntity entity, Frustum frustum,
                                    double cameraX, double cameraY, double cameraZ) {
            StandardAmmoItem.StandardAmmoType ammo = entity.ammoType();
            // [D2] bmg50_equestrian has no renderer in GunFactoryClient (RenderBulletMK4 draws nothing)
            if (ammo.family() == StandardAmmoItem.AmmoFamily.FLAME || ammo == StandardAmmoItem.StandardAmmoType.BMG50_EQUESTRIAN) {
                return false;
            }
            double radius;
            if (GunBatchF3BulletRenderer.handles(ammo)) { // [weapons batch F3]
                if (!GunBatchF3BulletRenderer.visible(ammo)) return false;
                radius = GunBatchF3BulletRenderer.radius(entity);
                // [parity S4: culling] ignoreFrustumCheck of the hook: the wire reaches the thrower, so the finite
                // envelope also spans the owner (eyes + 0.8 hand offset, 0.5 sag, 0.03125 girth < 1.5 inflation).
                if (entity.ignoreFrustumCheck && entity.ownerEntity() != null) {
                    AABB hook = EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius);
                    Entity owner = entity.ownerEntity();
                    AABB ownerBox = owner.getBoundingBox();
                    ownerBox = ownerBox.minmax(ownerBox.move(owner.xo - owner.getX(), owner.yo - owner.getY(), owner.zo - owner.getZ()));
                    return EntityRenderBounds.shouldRender(entity, hook.minmax(ownerBox.inflate(1.5D)), hook, frustum,
                            cameraX, cameraY, cameraZ);
                }
            } else if (GunBatchD3BulletRenderer.handles(ammo)) { // [weapons batch D3]
                radius = GunBatchD3BulletRenderer.radius(ammo);
            } else if (GunBatchE1BulletRenderer.handles(ammo)) { // [weapons batch E1]
                radius = GunBatchE1BulletRenderer.radius(entity);
            } else if (GunBatchF4BulletRenderer.handles(ammo)) { // [weapons batch F4]
                radius = GunBatchF4BulletRenderer.radius(entity);
            } else if (GunBatchF2BulletRenderer.handles(ammo)) { // [weapons batch F2]
                radius = GunBatchF2BulletRenderer.radius(ammo);
            } else if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
                double displacement = frameDisplacement(entity);
                double tracer = displacement
                        + Math.sqrt(4.0D * displacement * displacement
                        + 2.0D * 0.03125D * 0.03125D)
                        + 0.375D;
                radius = Math.max(RICHARD_RADIUS, Math.nextUp(tracer));
            } else {
                radius = rifleBulletVisualRadius(entity);
            }
            AABB bounds = EntityRenderBounds.radius(
                    entity.getX(), entity.getY(), entity.getZ(), radius);
            return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                    cameraX, cameraY, cameraZ);
        }

        @Override
        public ResourceLocation getTextureLocation(LegacyBulletEntity entity) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }

    public static class ArtilleryShell extends EntityRenderer<LegacyArtilleryShellEntity> {
        public ArtilleryShell(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(LegacyArtilleryShellEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            renderArtilleryShell(entity, partialTick, poseStack, bufferSource, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        @Override
        public boolean shouldRender(LegacyArtilleryShellEntity entity, Frustum frustum,
                                    double cameraX, double cameraY, double cameraZ) {
            AABB bounds = EntityRenderBounds.radius(
                    entity.getX(), entity.getY(), entity.getZ(), ARTILLERY_RADIUS);
            return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                    cameraX, cameraY, cameraZ);
        }

        @Override
        public ResourceLocation getTextureLocation(LegacyArtilleryShellEntity entity) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }

    public static class HimarsRocket extends EntityRenderer<LegacyHimarsRocketEntity> {
        public HimarsRocket(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(LegacyHimarsRocketEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            renderHimarsRocket(entity, partialTick, poseStack, bufferSource, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        @Override
        public boolean shouldRender(LegacyHimarsRocketEntity entity, Frustum frustum,
                                    double cameraX, double cameraY, double cameraZ) {
            double radius = entity.rocketType().modelType() == 1
                    ? HIMARS_SINGLE_RADIUS : HIMARS_STANDARD_RADIUS;
            AABB bounds = EntityRenderBounds.radius(
                    entity.getX(), entity.getY(), entity.getZ(), radius);
            return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                    cameraX, cameraY, cameraZ);
        }

        @Override
        public ResourceLocation getTextureLocation(LegacyHimarsRocketEntity entity) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }

    private static double frameDisplacement(Entity entity) {
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double buildRichardRadius() {
        PoseStack poseStack = new PoseStack();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, -4.5D);
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
        RICHARD_BOUNDS_MODEL.includeGroupFacePositions("Missile", poseStack, bounds);
        return rotationSafeRadius(bounds.maximumRadius());
    }

    private static double buildArtilleryRadius() {
        PoseStack poseStack = new PoseStack();
        poseStack.scale(2.5F, 5.0F, 2.5F);
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
        PROJECTILE_BOUNDS_MODEL.includeGroupFacePositions("Grenade", poseStack, bounds);
        return rotationSafeRadius(bounds.maximumRadius());
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}

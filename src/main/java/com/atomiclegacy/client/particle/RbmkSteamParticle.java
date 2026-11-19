package com.atomiclegacy.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * ParticleRBMKSteam: the boiler vent jet (textures/particle/rbmk_jet_steam.png, 20 frames in one row).
 * Age 10, alpha 0.25, scale 4, full brightness, SRC_ALPHA/ONE blending without depth writes,
 * a quad rotated only around Y by -playerViewY and offset by -0.9375 along its width. The legacy
 * renderer also added the EntityFX rotation arguments (rotX, rotXZ, rotZ) to its translation; that
 * offset is kept.
 */
public class RbmkSteamParticle extends Particle {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/particle/rbmk_jet_steam.png");
    private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "HBM_LEGACY_RBMK_STEAM";
        }
    };

    private final float particleScale = 4.0F;

    protected RbmkSteamParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 10;
        this.alpha = 0.25F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.hasPhysics = false;
        this.gravity = 0.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        int age = Math.min(this.age, this.lifetime);
        int texIndex = (int) (((double) age / (double) this.lifetime) * 20) % 20 - 1;
        // GL_REPEAT made index -1 sample the last frame.
        if (texIndex < 0) {
            texIndex += 20;
        }
        float f0 = 1.0F / 20.0F;
        float uMin = texIndex * f0;
        float uMax = uMin + f0;
        float vMin = 0.0F;
        float vMax = 1.0F;

        float yaw = camera.getYRot();
        float pitch = camera.getXRot();
        int flip = Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT ? 1 : 0;
        float rotX = Mth.cos(yaw * (float) Math.PI / 180.0F) * (1 - flip * 2);
        float rotZ = Mth.sin(yaw * (float) Math.PI / 180.0F) * (1 - flip * 2);
        float rotXZ = Mth.cos(pitch * (float) Math.PI / 180.0F);

        Vec3 cam = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x) + rotX;
        float pY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y) + rotXZ;
        float pZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z) + rotZ;
        // glRotatef(-playerViewY, 0, 1, 0) applied to a quad in the local XY plane.
        double theta = Math.toRadians(-yaw);
        float cos = (float) Math.cos(theta);
        float sin = (float) Math.sin(theta);

        float left = this.particleScale * -0.25F - 0.9375F;
        float right = this.particleScale * 0.25F - 0.9375F;
        float bottom = -0.25F;
        float top = this.particleScale - 0.25F;
        int light = 0xF000F0;
        vertex(buffer, pX, pY, pZ, cos, sin, left, bottom, uMax, vMax, light);
        vertex(buffer, pX, pY, pZ, cos, sin, left, top, uMax, vMin, light);
        vertex(buffer, pX, pY, pZ, cos, sin, right, top, uMin, vMin, light);
        vertex(buffer, pX, pY, pZ, cos, sin, right, bottom, uMin, vMax, light);
    }

    private void vertex(VertexConsumer buffer, float pX, float pY, float pZ, float cos, float sin,
                        float lx, float ly, float u, float v, int light) {
        // OpenGL Y rotation: x' = x cos + z sin, z' = -x sin + z cos (local z = 0).
        buffer.addVertex(pX + lx * cos, pY + ly, pZ - lx * sin)
                .setUv(u, v)
                .setColor(1.0F, 1.0F, 1.0F, this.alpha)
                .setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new RbmkSteamParticle(level, x, y, z);
        }
    }
}

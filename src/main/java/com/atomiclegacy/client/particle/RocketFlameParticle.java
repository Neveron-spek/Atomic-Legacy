package com.atomiclegacy.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * [parity P1: missile contrail] 1.7.10 ParticleRocketFlame as spawned by the "missileContrail" effect of
 * EntityMissileBaseNT.spawnContrailWithOffset: particle_base sprite, maxAge 60 + rand(20), motion damped by 0.91,
 * ten jittered quads per particle seeded by the particle id, colour fading from orange to the random add.
 */
public final class RocketFlameParticle extends TextureSheetParticle {
    private static final ParticleRenderType NO_DEPTH = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "HBM_LEGACY_ROCKET_FLAME";
        }
    };

    private final int seed;
    private final float scale;

    private RocketFlameParticle(ClientLevel level, double x, double y, double z, double mx, double my, double mz,
                                SpriteSet sprites, float scale) {
        super(level, x, y, z, 0, 0, 0);
        setSprite(sprites.get(this.random));
        this.xd = mx;
        this.yd = my;
        this.zd = mz;
        this.lifetime = 60 + this.random.nextInt(20);
        this.scale = scale;
        this.seed = level.random.nextInt();
        this.hasPhysics = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age == this.lifetime) remove();
        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Random urandom = new Random(this.seed);
        double bx = this.x, by = this.y, bz = this.z, ox = this.xo, oy = this.yo, oz = this.zo;
        for (int i = 0; i < 10; i++) {
            float add = urandom.nextFloat() * 0.3F;
            float dark = 1 - Math.min(((float) age / (float) (lifetime * 0.25F)), 1);
            this.rCol = Math.min(1F, 1 * dark + add);
            this.gCol = Math.min(1F, 0.6F * dark + add);
            this.bCol = add;
            this.alpha = (float) Math.pow(1 - Math.min(((float) age / (float) lifetime), 1), 0.5) * 0.75F;
            float spread = (float) Math.pow(((float) age / (float) lifetime) * 4F, 1.5) + 1F;
            spread *= this.scale;
            float sideOffset = (float) (0.25 * this.scale);
            this.quadSize = (urandom.nextFloat() * 0.5F + 0.1F + ((float) age / (float) lifetime) * 2F) * this.scale;
            double dx = sideOffset + (urandom.nextGaussian() - 1D) * 0.2F * spread;
            double dy = (urandom.nextGaussian() - 1D) * 0.5F * spread;
            double dz = sideOffset + (urandom.nextGaussian() - 1D) * 0.2F * spread;
            this.x = bx + dx; this.y = by + dy; this.z = bz + dz;
            this.xo = ox + dx; this.yo = oy + dy; this.zo = oz + dz;
            super.render(buffer, camera, partialTick);
        }
        this.x = bx; this.y = by; this.z = bz; this.xo = ox; this.yo = oy; this.zo = oz;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return NO_DEPTH;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float scale;

        public Provider(SpriteSet sprites, float scale) {
            this.sprites = sprites;
            this.scale = scale;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new RocketFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.scale);
        }
    }
}

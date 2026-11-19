package com.atomiclegacy.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * [parity R1: ashglasses gas cloud] 1.7.10 ClientProxy "vanillaExt" mode "cloud" with r/g/b:
 * a vanilla EntityCloudFX tinted (r, g, b) + one random 0..0.1, scale field 7.5 and no motion.
 * The colour is passed in the three speed arguments.
 */
public final class GasCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    private GasCloudParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        float rng = this.random.nextFloat() * 0.1F;
        setColor(r + rng, g + rng, b + rng);
        this.baseSize = 0.1F * 7.5F;
        this.quadSize = this.baseSize;
        this.lifetime = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        setSpriteFromAge(sprites);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.baseSize * Mth.clamp((this.age + partialTicks) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        setSpriteFromAge(this.sprites);
        move(this.xd, this.yd, this.zd);
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        Player player = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0D, false);
        if (player != null && this.y > player.getBoundingBox().minY) {
            this.y += (player.getBoundingBox().minY - this.y) * 0.2D;
            this.yd += (player.getDeltaMovement().y - this.yd) * 0.2D;
            setPos(this.x, this.y, this.z);
        }
        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double r, double g, double b) {
            return new GasCloudParticle(level, x, y, z, (float) r, (float) g, (float) b, sprites);
        }
    }
}

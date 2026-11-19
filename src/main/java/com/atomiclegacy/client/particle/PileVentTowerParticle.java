package com.atomiclegacy.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * [parity M2: pile] ParticleCoolingTower with the "tower" settings of TileEntityPileCore#handleVentilation: lift 1,
 * strafe 0.0025, noWind, colour 0xa0a0a0, alpha 0.25. The server passes base scale, max scale and life through the
 * particle velocity (count 0); life is divided by particleSetting + 1 as in ClientProxy.
 */
public final class PileVentTowerParticle extends TextureSheetParticle {
    private static final float LIFT = 1.0F;
    private static final float STRAFE = 0.0025F;
    private static final float ALPHA = 0.25F;
    private final float baseScale;
    private final float maxScale;

    private PileVentTowerParticle(ClientLevel level, double x, double y, double z, double base, double max, double life,
                                  SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(this.random));
        setColor(0xa0 / 255F, 0xa0 / 255F, 0xa0 / 255F);
        this.baseScale = (float) base;
        this.maxScale = (float) max;
        int setting = Minecraft.getInstance().options.particles().get().getId();
        this.lifetime = Math.max(1, (int) life / (setting + 1));
        this.alpha = ALPHA;
        this.quadSize = this.baseScale;
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA - ageScale * ALPHA;
        this.quadSize = this.baseScale + (float) Math.pow(this.maxScale * ageScale - this.baseScale, 2);
        this.age++;
        if (this.yd < LIFT) {
            this.yd += 0.01F;
        }
        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;
        if (this.age == this.lifetime) {
            this.remove();
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double base, double max, double life) {
            int setting = Minecraft.getInstance().options.particles().get().getId();
            if (setting != 0 && !(setting == 1 && level.random.nextBoolean())) return null;
            return new PileVentTowerParticle(level, x, y, z, base, max, life, this.sprites);
        }
    }
}

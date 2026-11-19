package com.atomiclegacy.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** [parity Q1] TileEntityCrucible's ParticleCoolingTower: lift 10, base 0.75, max 3.5, life 100-119, colour 0x202020. */
public final class CrucibleTowerParticle extends TextureSheetParticle {
    private static final float BASE_SCALE = 0.75F;
    private static final float MAX_SCALE = 3.5F;
    private static final float LIFT = 10.0F;
    private static final float STRAFE = 0.075F;
    private static final float ALPHA = 0.25F;

    private CrucibleTowerParticle(ClientLevel level, double x, double y, double z,
                                       double red, double green, double blue, SpriteSet sprites) {
        super(level, x, y, z);
        setSprite(sprites.get(this.random));
        setColor((float) red, (float) green, (float) blue);
        this.alpha = ALPHA;
        this.quadSize = BASE_SCALE;
        this.lifetime = 100 + this.random.nextInt(20);
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA - ageScale * ALPHA;
        this.quadSize = BASE_SCALE + (float) Math.pow(MAX_SCALE * ageScale - BASE_SCALE, 2.0D);
        this.age++;
        if (this.yd < LIFT) {
            this.yd += 0.01D;
        }
        this.xd += this.random.nextGaussian() * STRAFE * ageScale + 0.02D * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale - 0.01D * ageScale;
        if (this.age >= this.lifetime) {
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
                                       double red, double green, double blue) {
            return new CrucibleTowerParticle(level, x, y, z, red, green, blue, this.sprites);
        }
    }
}

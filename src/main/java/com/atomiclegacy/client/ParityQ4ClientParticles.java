package com.atomiclegacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/**
 * [parity Q4] Client-only helpers for particles spawned by utility machines. Minecraft 1.21 keeps the 1.7.10
 * EntityAuraFX as SuspendedTownParticle (the "mycelium" type, same lifetime, size and drag), so the
 * {@code vanillaExt townaura} effect is that particle recoloured and given the explicit velocity, as
 * ClientProxy.effectNT does with setRBGColorF/setVelocity.
 */
public final class ParityQ4ClientParticles {

    private ParityQ4ClientParticles() {
    }

    /** ClientProxy.effectNT "vanillaExt"/"townaura". */
    public static void townaura(double x, double y, double z, double mX, double mY, double mZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Particle particle = minecraft.particleEngine.createParticle(ParticleTypes.MYCELIUM, x, y, z, 0.0D, 0.0D, 0.0D);
        if (particle == null) return;
        RandomSource rand = minecraft.level.random;
        float color = 0.5F + rand.nextFloat() * 0.5F;
        particle.setColor(0.8F * color, 0.9F * color, 1.0F * color);
        particle.setParticleSpeed(mX, mY, mZ);
    }

    /** ClientProxy.effectNT "fluidfill": EntityCritFX with the fluid colour (nextTextureIndexX picks the second crit frame). */
    public static void fluidfill(double x, double y, double z, double mX, double mY, double mZ, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Particle particle = minecraft.particleEngine.createParticle(ParticleTypes.CRIT, x, y, z, mX, mY, mZ);
        if (particle == null) return;
        particle.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }

    /** MainRegistry.proxy.me().getDistance(x, y, z) < distance (block corner, as in the original). */
    public static boolean playerWithin(double x, double y, double z, double distance) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && Math.sqrt(minecraft.player.distanceToSqr(x, y, z)) < distance;
    }

    /** Keyboard.isKeyDown(KEY_LSHIFT) for item tooltips; false off the client. */
    public static boolean shiftDown() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }
}

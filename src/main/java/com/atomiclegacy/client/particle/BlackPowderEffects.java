package com.atomiclegacy.client.particle;

import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.network.BlackPowderEffectPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/**
 * Client half of 1.7.10 BlackPowderCreator (makeParticle) and the EffectRenderer state the two
 * black powder particles were drawn with: blending SRC_ALPHA/ONE_MINUS_SRC_ALPHA, no depth writes and
 * glAlphaFunc(GL_GREATER, 1/255). The vanilla 1.21 particle shader discards alpha below 0.1, which
 * would cut away the 0.25-alpha smoke, so the legacy_particle shader restores the 1/255 threshold.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlackPowderEffects {

	/** ModEventHandlerClient.particleBase (hbm:particle/particle_base). */
	static final ResourceLocation PARTICLE_BASE = AtomicLegacy.id("particle_base");
	/** EntityFX#setParticleTextureIndex(0) on particles.png. */
	static final ResourceLocation GENERIC_0 = ResourceLocation.withDefaultNamespace("generic_0");

	private static ShaderInstance legacyParticleShader;

	static final ParticleRenderType LEGACY_TRANSLUCENT = new ParticleRenderType() {
		@Override
		public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
			RenderSystem.setShader(() -> legacyParticleShader != null ? legacyParticleShader : GameRenderer.getParticleShader());
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(false);
			return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		@Override
		public String toString() {
			return "HBM_LEGACY_PARTICLE_TRANSLUCENT";
		}
	};

	private BlackPowderEffects() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), AtomicLegacy.id("legacy_particle"), DefaultVertexFormat.PARTICLE),
				shader -> legacyParticleShader = shader);
	}

	static TextureAtlasSprite sprite(ResourceLocation id) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
		return ((TextureAtlas) texture).getSprite(id);
	}

	public static void makeParticle(BlackPowderEffectPayload data) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		if(world == null) return;
		RandomSource rand = world.random;
		double x = data.x();
		double y = data.y();
		double z = data.z();

		// Vec3NT#normalizeSelf
		double hX = data.headingX();
		double hY = data.headingY();
		double hZ = data.headingZ();
		double len = (float) Math.sqrt(hX * hX + hY * hY + hZ * hZ);
		double mult = len < 1.0E-4D ? 0D : 1D / len;
		hX *= mult;
		hY *= mult;
		hZ *= mult;

		int cloudCount = data.cloudCount();
		float cloudScale = data.cloudScale();
		float cloudSpeedMult = data.cloudSpeedMult();
		int sparkCount = data.sparkCount();
		float sparkSpeedMult = data.sparkSpeedMult();

		for(int i = 0; i < cloudCount; i++) {
			BlackPowderSmokeParticle particle = new BlackPowderSmokeParticle(world, x, y, z, cloudScale);
			double speedMult = 0.85 + rand.nextDouble() * 0.3;
			particle.setParticleSpeed(
					hX * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05,
					hY * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05,
					hZ * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05);
			mc.particleEngine.add(particle);
		}

		for(int i = 0; i < sparkCount; i++) {
			double speedMult = 0.85 + rand.nextDouble() * 0.3;
			BlackPowderSparkParticle particle = new BlackPowderSparkParticle(world, x, y, z,
					hX * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
					hY * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
					hZ * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02);
			mc.particleEngine.add(particle);
		}
	}
}

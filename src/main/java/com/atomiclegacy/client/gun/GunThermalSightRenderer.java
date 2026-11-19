package com.atomiclegacy.client.gun;

import java.util.OptionalDouble;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyChopperEntity;
import com.atomiclegacy.entity.LegacyMaskManEntity;
import com.atomiclegacy.entity.LegacyQuackosEntity;
import com.atomiclegacy.entity.LegacyUfoEntity;
import com.atomiclegacy.entity.LegacyWormHeadEntity;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 1.7.10 ModEventHandlerClient#onRenderWorldLastEvent thermal branch + RenderOverhead.renderThermalSight for
 * guns whose GunConfig has thermal sights (gun_amat_penance): while the held gun is fully aimed
 * (ItemGunBaseNT.aimingProgress == 1), the bounding box of every loaded entity within 64 blocks except the
 * player is drawn as GL_LINES without texture, lighting or depth test, coloured by type: boss orange, IMob red,
 * player magenta, other EntityLiving green, item pale yellow, XP orb blinking yellow/green every 5 ticks,
 * dead living entities black; all other entities are skipped.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class GunThermalSightRenderer {

	private GunThermalSightRenderer() {
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event) {
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if(player == null || level == null || !isActive(player)) return;

		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer buffer = buffers.getBuffer(Types.THERMAL_LINES);
		Matrix4f pose = poseStack.last().pose();

		for(Entity ent : level.entitiesForRendering()) {
			if(ent == player) continue;
			if(ent.distanceToSqr(player) > 4096) continue;

			int color = color(ent, player);
			if(color < 0) continue;
			float r = ((color >> 16) & 0xFF) / 255F;
			float g = ((color >> 8) & 0xFF) / 255F;
			float b = (color & 0xFF) / 255F;

			AABB bb = ent.getBoundingBox();
			float minX = (float) (bb.minX - camera.x), minY = (float) (bb.minY - camera.y), minZ = (float) (bb.minZ - camera.z);
			float maxX = (float) (bb.maxX - camera.x), maxY = (float) (bb.maxY - camera.y), maxZ = (float) (bb.maxZ - camera.z);
			float[][] v = {
					{minX, maxY, minZ}, {minX, minY, minZ},
					{minX, maxY, minZ}, {maxX, maxY, minZ},
					{maxX, maxY, minZ}, {maxX, minY, minZ},
					{minX, minY, minZ}, {maxX, minY, minZ},
					{maxX, minY, minZ}, {maxX, minY, maxZ},
					{maxX, maxY, maxZ}, {maxX, maxY, minZ},
					{maxX, maxY, maxZ}, {maxX, minY, maxZ},
					{minX, maxY, minZ}, {minX, maxY, maxZ},
					{minX, maxY, maxZ}, {minX, minY, maxZ},
					{minX, maxY, maxZ}, {maxX, maxY, maxZ},
					{minX, minY, maxZ}, {maxX, minY, maxZ},
					{minX, minY, minZ}, {minX, minY, maxZ}
			};
			for(float[] p : v) buffer.addVertex(pose, p[0], p[1], p[2]).setColor(r, g, b, 1F);
		}

		buffers.endBatch(Types.THERMAL_LINES);
	}

	/** The gun branch of the original check; the FSB chestplate branch is handled by the port's armor code. */
	public static boolean isActive(Player player) {
		ItemStack held = player.getMainHandItem();
		if(held.getItem() instanceof ItemGunBaseNT gun && ItemGunBaseNT.aimingProgress == 1) {
			for(int i = 0; i < gun.getConfigCount(); i++) if(gun.getConfig(held, i).hasThermalSights(held)) return true;
		}
		return false;
	}

	/** RGB of the original setColorOpaque_F calls, -1 for skipped entities. */
	public static int color(Entity ent, Player player) {
		int color;
		if(isBoss(ent)) color = 0xFF7F00;
		else if(ent instanceof Enemy) color = 0xFF0000;
		else if(ent instanceof Player) color = 0xFF00FF;
		else if(ent instanceof Mob) color = 0x00FF00;
		else if(ent instanceof ItemEntity) color = 0xFFFF7F;
		else if(ent instanceof ExperienceOrb) color = player.tickCount % 10 < 5 ? 0xFFFF7F : 0x7FFF7F;
		else return -1;
		if(ent instanceof LivingEntity living && living.getHealth() <= 0) color = 0x000000;
		return color;
	}

	/** IBossDisplayData implementors: EntityDragon, EntityWither and HBM's boss mobs present in the port. */
	public static boolean isBoss(Entity ent) {
		return ent instanceof EnderDragon || ent instanceof WitherBoss || ent instanceof LegacyMaskManEntity || ent instanceof LegacyUfoEntity
				|| ent instanceof LegacyChopperEntity || ent instanceof LegacyWormHeadEntity || ent instanceof LegacyQuackosEntity;
	}

	private static final class Types extends RenderType {
		/** GL_LINES, GL_TEXTURE_2D/GL_LIGHTING/GL_DEPTH_TEST off, blend 770/771, default line width 1. */
		static final RenderType THERMAL_LINES = create("hbm_thermal_sight_lines",
				DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES, 1536, false, false,
				CompositeState.builder()
						.setShaderState(POSITION_COLOR_SHADER)
						.setLineState(new LineStateShard(OptionalDouble.of(1.0D)))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setDepthTestState(NO_DEPTH_TEST)
						.setCullState(NO_CULL)
						.setWriteMaskState(COLOR_WRITE)
						.createCompositeState(false));

		private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
			super(name, format, mode, size, crumbling, sort, setup, clear);
		}
	}
}

package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/**
 * [parity K3] GUIElements#drawSmoothGauge of the 1.7.10 reference: a needle rotated by -progress * 270 - 45 degrees
 * using Vec3#rotateAroundZ (x' = x cos + y sin, y' = y cos - x sin), outline scaled by 1.5 in colorOuter.
 */
public final class K3LegacyGauge {
	private K3LegacyGauge() {
	}

	public static void draw(GuiGraphics graphics, int x, int y, double progress, double tipLength, double backLength,
			double backSide, int color) {
		draw(graphics, x, y, progress, tipLength, backLength, backSide, color, 0x000000);
	}

	public static void draw(GuiGraphics graphics, int x, int y, double progress, double tipLength, double backLength,
			double backSide, int color, int colorOuter) {
		progress = Math.max(0.0D, Math.min(1.0D, progress));
		float angle = (float) Math.toRadians(-progress * 270 - 45);
		double[] tip = rotate(0, tipLength, angle);
		double[] left = rotate(backSide, -backLength, angle);
		double[] right = rotate(-backSide, -backLength, angle);

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		triangle(graphics, x, y, tip, left, right, 1.5D, colorOuter);
		triangle(graphics, x, y, tip, left, right, 1.0D, color);
		RenderSystem.enableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static double[] rotate(double x, double y, float angle) {
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		return new double[] {x * cos + y * sin, y * cos - x * sin};
	}

	private static void triangle(GuiGraphics graphics, int x, int y, double[] tip, double[] left, double[] right,
			double mult, int color) {
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		PoseStack.Pose pose = graphics.pose().last();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		buffer.addVertex(pose, (float) (x + tip[0] * mult), (float) (y + tip[1] * mult), 0.0F).setColor(r, g, b, 255);
		buffer.addVertex(pose, (float) (x + left[0] * mult), (float) (y + left[1] * mult), 0.0F).setColor(r, g, b, 255);
		buffer.addVertex(pose, (float) (x + right[0] * mult), (float) (y + right[1] * mult), 0.0F).setColor(r, g, b, 255);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}
}

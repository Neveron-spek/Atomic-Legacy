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
import net.minecraft.util.Mth;

/**
 * [parity M2] GUIElements#drawSmoothLinearGauge of the 1.7.10 reference: a five-point needle shifted by
 * progress * scale along the rotated axis; the outline scales x by 1.5 for every point and y only for tip, right and
 * left (the original back points keep their unscaled y). GL_POLYGON is drawn as a triangle fan.
 */
public final class M2LegacyLinearGauge {

    private M2LegacyLinearGauge() {
    }

    public static void draw(GuiGraphics graphics, int x, int y, double progress, double tipLength, double backLength,
            double backSide, double scale, float rotation, int color) {
        scale = Math.max(scale, 1);
        progress = Mth.clamp(progress, 0, 1) * scale;
        float angle = (float) Math.toRadians(-rotation);
        double[] tip = rotate(0, -tipLength, angle);
        double[] right = rotate(-backSide, 0, angle);
        double[] bRight = rotate(-backSide, backLength, angle);
        double[] bLeft = rotate(backSide, backLength, angle);
        double[] left = rotate(backSide, 0, angle);
        double dx = progress * Mth.cos(angle);
        double dy = progress * Mth.sin(angle);
        double m = 1.5D;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        fan(graphics, 0x000000,
                x + dx + tip[0] * m, y + dy + tip[1] * m,
                x + dx + right[0] * m, y + dy + right[1] * m,
                x + dx + bRight[0] * m, y + dy + bRight[1],
                x + dx + bLeft[0] * m, y + dy + bLeft[1],
                x + dx + left[0] * m, y + dy + left[1] * m);
        fan(graphics, color,
                x + dx + tip[0], y + dy + tip[1],
                x + dx + right[0], y + dy + right[1],
                x + dx + bRight[0], y + dy + bRight[1],
                x + dx + bLeft[0], y + dy + bLeft[1],
                x + dx + left[0], y + dy + left[1]);
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Vec3#rotateAroundZ of 1.7.10 */
    private static double[] rotate(double x, double y, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        return new double[] {x * cos + y * sin, y * cos - x * sin};
    }

    private static void fan(GuiGraphics graphics, int color, double... p) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        PoseStack.Pose pose = graphics.pose().last();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < p.length; i += 2) {
            buffer.addVertex(pose, (float) p[i], (float) p[i + 1], 0.0F).setColor(r, g, b, 255);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}

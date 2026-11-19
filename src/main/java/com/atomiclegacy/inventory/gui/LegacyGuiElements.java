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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** [parity L1: gas turbine] Port of 1.7.10 GUIElements.drawSmoothTextureModalCircle. */
public final class LegacyGuiElements {
    private LegacyGuiElements() {
    }

    public static void drawSmoothTextureModalCircle(GuiGraphics graphics, ResourceLocation texture, int xDraw, int yDraw,
                                                    int xStart, int yStart, int xDelta, int yDelta, double progress) {
        float var7 = 0.00390625F;
        float var8 = 0.00390625F;
        progress = Mth.clamp(progress, 0, 1);
        float angle = (float) (-progress * 270.0);
        double theta = Math.toRadians(angle - 135);
        int addons = 0;
        double xTarget = 0;
        double yTarget = 0;
        if (angle >= -180 && angle < -90) {
            addons = 1;
        } else if (angle >= -270 && angle < -180) {
            addons = 2;
        }
        if (angle >= -90) {
            xTarget = -1;
            yTarget = -Math.tan(theta);
        } else if (angle > -135 && angle < -90) {
            xTarget = Math.tan(Math.PI / 2 - theta);
            yTarget = 1;
        } else if (angle > -180 && angle < -135) {
            xTarget = Math.tan(Math.PI / 2 - theta);
            yTarget = 1;
        } else if (angle <= -180) {
            xTarget = 1;
            yTarget = Math.tan(theta);
        } else if (angle == -135) {
            xTarget = 0;
            yTarget = 1;
        }
        double xMid = (double) xDelta / 2;
        double yMid = (double) yDelta / 2;
        xTarget *= xMid;
        yTarget *= yMid;

        List<double[]> v = new ArrayList<>();
        v.add(new double[] {xDraw, yDraw + yDelta, xStart * var7, (yStart + yDelta) * var8});
        v.add(new double[] {xDraw + xMid, yDraw + yMid, (float) (xStart + xMid) * var7, (float) (yStart + yMid) * var8});
        if (addons == 2 || addons == 1) {
            v.add(new double[] {xDraw, yDraw, xStart * var7, yStart * var8});
            v.add(new double[] {xDraw, yDraw, xStart * var7, yStart * var8});
            v.add(new double[] {xDraw + xMid, yDraw + yMid, (float) (xStart + xMid) * var7, (float) (yStart + yMid) * var8});
        }
        if (addons == 2) {
            v.add(new double[] {xDraw + xDelta, yDraw, (xStart + xDelta) * var7, yStart * var8});
            v.add(new double[] {xDraw + xDelta, yDraw, (xStart + xDelta) * var7, yStart * var8});
            v.add(new double[] {xDraw + xMid, yDraw + yMid, (float) (xStart + xMid) * var7, (float) (yStart + yMid) * var8});
        }
        v.add(new double[] {xDraw + xTarget + xMid, yDraw - yTarget + yMid,
                (float) (xStart + xTarget + xMid) * var7, (float) (yStart - yTarget + yMid) * var8});

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.disableCull();
        PoseStack.Pose pose = graphics.pose().last();
        Matrix4f matrix = pose.pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        for (double[] vertex : v) {
            buffer.addVertex(matrix, (float) vertex[0], (float) vertex[1], 0.0F).setUv((float) vertex[2], (float) vertex[3]);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }
}

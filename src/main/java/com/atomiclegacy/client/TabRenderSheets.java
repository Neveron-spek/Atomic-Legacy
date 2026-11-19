package com.atomiclegacy.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in (-Datomiclegacy.itemRenderSheets=tabPath,tabPath): contact sheets of every stack shown in the given
 * creative tabs of this mod, drawn through the real item renderer in the GUI, first person, third person, ground
 * and item frame display contexts. Each row: ordinal label + 5 tiles; the index item-render-sheets.tsv maps the
 * ordinals to stacks. Output: screenshots/sheet_[tab]_[n].png in the game directory.
 */
public final class TabRenderSheets {
    private static final int TILE = 64;
    private static final int LABEL = 22;
    private static final ItemDisplayContext[] CONTEXTS = {
            ItemDisplayContext.GUI, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.GROUND, ItemDisplayContext.FIXED};
    private static final int GROUPS = 3;
    private static final int ROWS = 12;
    private static final int GROUP_W = LABEL + TILE * CONTEXTS.length + 6;
    private static final int W = GROUP_W * GROUPS;
    private static final int H = TILE * ROWS;

    private TabRenderSheets() {}

    public static void render(Minecraft mc, String tabs) {
        List<String> index = new ArrayList<>();
        Path dir = mc.gameDirectory.toPath().resolve("screenshots");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            AtomicLegacy.LOGGER.error("item render sheets: cannot create {}", dir, e);
            return;
        }
        for (String tabPath : tabs.split(",")) {
            tabPath = tabPath.trim();
            if (tabPath.isEmpty()) continue;
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(AtomicLegacy.id(tabPath));
            if (tab == null) {
                index.add("# no tab " + tabPath);
                continue;
            }
            List<ItemStack> stacks = new ArrayList<>(tab.getDisplayItems());
            int perSheet = GROUPS * ROWS;
            int sheets = (stacks.size() + perSheet - 1) / perSheet;
            index.add("# tab " + tabPath + " stacks=" + stacks.size() + " sheets=" + sheets);
            for (int sheet = 0; sheet < sheets; sheet++) {
                List<ItemStack> page = stacks.subList(sheet * perSheet, Math.min(stacks.size(), (sheet + 1) * perSheet));
                String file = "sheet_" + tabPath + "_" + sheet + ".png";
                try {
                    renderSheet(mc, page, sheet * perSheet, dir.resolve(file));
                } catch (Throwable t) {
                    AtomicLegacy.LOGGER.error("item render sheet {} failed", file, t);
                    index.add("# sheet " + file + " failed: " + t);
                }
                for (int i = 0; i < page.size(); i++) {
                    ItemStack stack = page.get(i);
                    index.add(file + "\t" + (sheet * perSheet + i) + "\t" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                            + (stack.getComponentsPatch().isEmpty() ? "" : "\t" + stack.getComponentsPatch()));
                }
            }
        }
        try {
            Files.write(mc.gameDirectory.toPath().resolve("item-render-sheets.tsv"), index, StandardCharsets.UTF_8);
        } catch (Exception e) {
            AtomicLegacy.LOGGER.error("item render sheets: cannot write index", e);
        }
        AtomicLegacy.LOGGER.info("HBM_ITEM_RENDER_SHEETS_COMPLETE entries={}", index.size());
    }

    private static void renderSheet(Minecraft mc, List<ItemStack> page, int firstOrdinal, Path out) throws Exception {
        RenderTarget target = new TextureTarget(W, H, true, Minecraft.ON_OSX);
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        try {
            target.setClearColor(0.16F, 0.16F, 0.18F, 1.0F);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, W, H, 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
            modelView.pushMatrix();
            modelView.identity();
            modelView.translation(0.0F, 0.0F, 10000.0F - 21000.0F);
            RenderSystem.applyModelViewMatrix();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            GuiGraphics graphics = new GuiGraphics(mc, buffers);
            Lighting.setupFor3DItems();

            for (int i = 0; i < page.size(); i++) {
                ItemStack stack = page.get(i);
                int group = i % GROUPS;
                int row = i / GROUPS;
                int gx = group * GROUP_W;
                int gy = row * TILE;
                graphics.fill(gx, gy, gx + GROUP_W, gy + TILE, (row & 1) == 0 ? 0xFF2A2A30 : 0xFF36363C);
                graphics.drawString(mc.font, String.valueOf(firstOrdinal + i), gx + 2, gy + 2, 0xFFFFFF, false);
                graphics.flush();
                for (int c = 0; c < CONTEXTS.length; c++) {
                    int x0 = gx + LABEL + c * TILE;
                    int y0 = gy;
                    RenderSystem.enableScissor(x0, H - (y0 + TILE), TILE, TILE);
                    try {
                        graphics.fill(x0 + 1, y0 + 1, x0 + TILE - 1, y0 + TILE - 1, (c & 1) == 0 ? 0xFF46464E : 0xFF505058);
                        graphics.flush();
                        if (CONTEXTS[c] == ItemDisplayContext.GUI) {
                            graphics.pose().pushPose();
                            graphics.pose().translate(x0, y0, 0.0F);
                            graphics.pose().scale(TILE / 16.0F, TILE / 16.0F, 1.0F);
                            graphics.renderItem(stack, 0, 0);
                            graphics.pose().popPose();
                            graphics.flush();
                        } else {
                            BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
                            boolean flat = !model.usesBlockLight();
                            if (flat) Lighting.setupForFlatItems();
                            PoseStack pose = new PoseStack();
                            pose.translate(x0 + TILE / 2.0F, y0 + TILE / 2.0F, 150.0F);
                            pose.scale(TILE, -TILE, TILE);
                            mc.getItemRenderer().renderStatic(mc.player, stack, CONTEXTS[c], false, pose, buffers, mc.level,
                                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                            buffers.endBatch();
                            if (flat) Lighting.setupFor3DItems();
                        }
                    } catch (Throwable t) {
                        try { buffers.endBatch(); } catch (Throwable ignored) { }
                        AtomicLegacy.LOGGER.warn("item render sheet: {} {} failed: {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), CONTEXTS[c], t.toString());
                    } finally {
                        RenderSystem.disableScissor();
                    }
                }
            }
            graphics.flush();
            try (NativeImage image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(out);
            }
        } finally {
            target.unbindWrite();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            Lighting.setupFor3DItems();
            mc.getMainRenderTarget().bindWrite(true);
            target.destroyBuffers();
        }
    }
}

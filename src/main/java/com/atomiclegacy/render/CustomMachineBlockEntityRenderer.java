package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.config.CustomMachineConfigJSON.ComponentDefinition;
import com.atomiclegacy.tileentity.CustomMachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.List;

/**
 * [parity X1: custom machines] RenderCustomMachine + SmallBlockPronter: while the structure is not valid every
 * component is drawn as a 5/16 block (11 px inset per side / 2) at 75 % alpha with the block's side icons, cycling
 * through its allowed metas once per second.
 */
public final class CustomMachineBlockEntityRenderer implements BlockEntityRenderer<CustomMachineBlockEntity> {
    private static final float pixel = 1F / 16F;
    private static final float LO = 11 * pixel / 2;
    private static final float HI = 1 - 11 * pixel / 2;

    public CustomMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CustomMachineBlockEntity custom, float interp, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        CustomMachineConfigJSON.MachineConfiguration config = custom.config;
        if (config == null || custom.structureOK) return;

        Direction dir = custom.facing();
        VertexConsumer tess = buffers.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        for (ComponentDefinition comp : config.components) {
            if (comp.block == null || comp.metas.isEmpty()) continue;
            BlockPos rel = CustomMachineBlockEntity.componentPos(BlockPos.ZERO, dir, comp.x, comp.y, comp.z);
            int index = (int) ((System.currentTimeMillis() / 1000) % comp.metas.size());
            drawSmolBlockAt(tess, matrix, CustomMachineConfigJSON.stateFor(comp.block, comp.metas.get(index)), rel.getX(), rel.getY(), rel.getZ(), light);
        }
    }

    private static TextureAtlasSprite icon(BlockState state, Direction side) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        List<net.minecraft.client.renderer.block.model.BakedQuad> quads = model.getQuads(state, side, RandomSource.create(42L), ModelData.EMPTY, null);
        return quads.isEmpty() ? model.getParticleIcon(ModelData.EMPTY) : quads.get(0).getSprite();
    }

    private static void vertex(VertexConsumer tess, Matrix4f matrix, float x, float y, float z, float u, float v, int light) {
        tess.addVertex(matrix, x, y, z).setColor(1.0F, 1.0F, 1.0F, 0.75F).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0F, 1F, 0F);
    }

    /** SmallBlockPronter#drawSmolBlockAt: south, east, north, west, top, bottom with the original vertex and UV order. */
    private static void drawSmolBlockAt(VertexConsumer t, Matrix4f m, BlockState b, float x, float y, float z, int light) {
        TextureAtlasSprite iconTop = icon(b, Direction.UP);
        TextureAtlasSprite iconBottom = icon(b, Direction.DOWN);
        TextureAtlasSprite iconNorth = icon(b, Direction.NORTH);
        TextureAtlasSprite iconSouth = icon(b, Direction.SOUTH);
        TextureAtlasSprite iconEast = icon(b, Direction.EAST);
        TextureAtlasSprite iconWest = icon(b, Direction.WEST);

        vertex(t, m, x + HI, y + HI, z + HI, iconSouth.getU1(), iconSouth.getV0(), light);
        vertex(t, m, x + LO, y + HI, z + HI, iconSouth.getU0(), iconSouth.getV0(), light);
        vertex(t, m, x + LO, y + LO, z + HI, iconSouth.getU0(), iconSouth.getV1(), light);
        vertex(t, m, x + HI, y + LO, z + HI, iconSouth.getU1(), iconSouth.getV1(), light);

        vertex(t, m, x + HI, y + HI, z + LO, iconEast.getU1(), iconEast.getV0(), light);
        vertex(t, m, x + HI, y + HI, z + HI, iconEast.getU0(), iconEast.getV0(), light);
        vertex(t, m, x + HI, y + LO, z + HI, iconEast.getU0(), iconEast.getV1(), light);
        vertex(t, m, x + HI, y + LO, z + LO, iconEast.getU1(), iconEast.getV1(), light);

        vertex(t, m, x + LO, y + HI, z + LO, iconNorth.getU1(), iconNorth.getV0(), light);
        vertex(t, m, x + HI, y + HI, z + LO, iconNorth.getU0(), iconNorth.getV0(), light);
        vertex(t, m, x + HI, y + LO, z + LO, iconNorth.getU0(), iconNorth.getV1(), light);
        vertex(t, m, x + LO, y + LO, z + LO, iconNorth.getU1(), iconNorth.getV1(), light);

        vertex(t, m, x + LO, y + HI, z + HI, iconWest.getU1(), iconWest.getV0(), light);
        vertex(t, m, x + LO, y + HI, z + LO, iconWest.getU0(), iconWest.getV0(), light);
        vertex(t, m, x + LO, y + LO, z + LO, iconWest.getU0(), iconWest.getV1(), light);
        vertex(t, m, x + LO, y + LO, z + HI, iconWest.getU1(), iconWest.getV1(), light);

        vertex(t, m, x + HI, y + HI, z + LO, iconTop.getU1(), iconTop.getV0(), light);
        vertex(t, m, x + LO, y + HI, z + LO, iconTop.getU0(), iconTop.getV0(), light);
        vertex(t, m, x + LO, y + HI, z + HI, iconTop.getU0(), iconTop.getV1(), light);
        vertex(t, m, x + HI, y + HI, z + HI, iconTop.getU1(), iconTop.getV1(), light);

        vertex(t, m, x + LO, y + LO, z + LO, iconBottom.getU1(), iconBottom.getV0(), light);
        vertex(t, m, x + HI, y + LO, z + LO, iconBottom.getU0(), iconBottom.getV0(), light);
        vertex(t, m, x + HI, y + LO, z + HI, iconBottom.getU0(), iconBottom.getV1(), light);
        vertex(t, m, x + LO, y + LO, z + HI, iconBottom.getU1(), iconBottom.getV1(), light);
    }
}

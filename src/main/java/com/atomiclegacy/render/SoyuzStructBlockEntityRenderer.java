package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.tileentity.SoyuzStructBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * [parity P2: soyuz_launcher] RenderSoyuzMultiblock: 75 % alpha 5/16 "smol" blocks for every
 * struct_launcher, concrete_smooth and struct_scaffold position checked by TileEntitySoyuzStruct.
 */
public final class SoyuzStructBlockEntityRenderer implements LongRangeBlockEntityRenderer<SoyuzStructBlockEntity> {
    private static final Set<SoyuzStructBlockEntityRenderer> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile ConstructionPreviewMesh.Geometry geometry;
    private static int generation;
    private final ConstructionPreviewMesh mesh = new ConstructionPreviewMesh();

    public SoyuzStructBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    @Override
    public void render(SoyuzStructBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        ConstructionPreviewMesh.Geometry current = geometry;
        if (current == null) {
            current = capture(++generation);
            geometry = current;
        }
        this.mesh.render(current, pose, overlay);
    }

    private static ConstructionPreviewMesh.Geometry capture(int generation) {
        BlockState launcher = HbmBlocks.STRUCT_LAUNCHER.get().defaultBlockState();
        BlockState concrete = HbmBlocks.CONCRETE_SMOOTH.get().defaultBlockState();
        BlockState scaffold = HbmBlocks.STRUCT_SCAFFOLD.get().defaultBlockState();
        List<ConstructionPreviewMesh.PlacementSpec> p = new ArrayList<>();
        for (int i = -6; i <= 6; i++) for (int j = 3; j <= 4; j++) for (int k = -6; k <= 6; k++) add(p, launcher, i, j, k);
        for (int i = -1; i <= 1; i++) for (int j = 3; j <= 4; j++) for (int k = -8; k <= -7; k++) add(p, launcher, i, j, k);
        for (int i = -2; i <= 2; i++) for (int j = 3; j <= 4; j++) for (int k = 7; k <= 9; k++) add(p, launcher, i, j, k);
        for (int i = -2; i <= 2; i++) for (int k = 5; k <= 9; k++) add(p, launcher, i, 51, k);
        for (int i = -1; i <= 1; i++) for (int k = -8; k <= -6; k++) add(p, launcher, i, 38, k);
        for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++) add(p, concrete, i, j, k);
        for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++) add(p, concrete, i, j, k);
        for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++) add(p, concrete, i, j, k);
        for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++) add(p, concrete, i, j, k);
        for (int i = -1; i <= 1; i++) for (int j = 0; j <= 2; j++) for (int k = -8; k <= -6; k++) add(p, concrete, i, j, k);
        for (int i = -2; i <= 2; i++) for (int j = 0; j <= 2; j++) for (int k = 5; k <= 9; k++) add(p, concrete, i, j, k);
        for (int i = -1; i <= 1; i++) for (int j = 5; j <= 50; j++) for (int k = 6; k <= 8; k++) add(p, scaffold, i, j, k);
        for (int j = 5; j <= 37; j++) add(p, scaffold, 0, j, -7);
        return ConstructionPreviewMesh.capture(generation, p);
    }

    private static void add(List<ConstructionPreviewMesh.PlacementSpec> placements, BlockState state, int x, int y, int z) {
        placements.add(ConstructionPreviewMesh.placement(state, x, y, z));
    }

    /** Called after a model bake: drop captured quads and every uploaded buffer. */
    static void invalidateAfterBake() {
        geometry = null;
        List<SoyuzStructBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (SoyuzStructBlockEntityRenderer renderer : renderers) renderer.mesh.close();
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzStructBlockEntity core) {
        // TileEntity.INFINITE_EXTENT_AABB in the reference
        return new AABB(core.getBlockPos()).inflate(7.0D, 0.0D, 10.0D).expandTowards(0.0D, 52.0D, 0.0D);
    }
}

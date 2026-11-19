package com.atomiclegacy.client.model;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyBlockRenderTables;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * [parity e1] 1.7.10 CraneRouter through RenderBlockMultipass: pass 0 is a crane_in cube, passes 1..6 draw
 * crane_router_overlay on side (pass - 1) only, multiplied by CraneRouter#colorMultiplier. The item
 * (shouldRenderItemMulti = false) keeps pass 0 only.
 */
public final class CraneRouterBlockModel implements IDynamicBakedModel {
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel delegate;
    private final TextureAtlasSprite base;
    private final List<BakedQuad>[] worldQuads;
    private final List<BakedQuad>[] itemQuads;

    private CraneRouterBlockModel(BakedModel delegate, Function<Material, TextureAtlasSprite> textureGetter) {
        this.delegate = delegate;
        this.base = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, AtomicLegacy.id("block/crane_in")));
        TextureAtlasSprite overlay = textureGetter.apply(
                new Material(TextureAtlas.LOCATION_BLOCKS, AtomicLegacy.id("block/crane_router_overlay")));
        EnumMap<Direction, Integer> noRotation = new EnumMap<>(Direction.class);
        this.itemQuads = WandBlockModel.legacyCube(CableModelGeometry.all(this.base), noRotation, null);
        // Tint index = RenderBlockMultipass pass of the side's overlay (side + 1).
        int[] passes = new int[6];
        for (int side = 0; side < 6; side++) {
            passes[side] = side + 1;
        }
        List<BakedQuad>[] overlays = WandBlockModel.legacyCube(CableModelGeometry.all(overlay), noRotation, passes);
        @SuppressWarnings("unchecked")
        List<BakedQuad>[] world = new List[6];
        for (int side = 0; side < 6; side++) {
            List<BakedQuad> quads = new ArrayList<>(this.itemQuads[side]);
            quads.addAll(overlays[side]);
            world[side] = quads;
        }
        this.worldQuads = world;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!AtomicLegacy.MOD_ID.equals(location.id().getNamespace())
                    || !"crane_router".equals(location.id().getPath())) {
                continue;
            }
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof CraneRouterBlockModel)) {
                models.put(location, new CraneRouterBlockModel(model, textureGetter));
            }
        }
    }

    /** CraneRouter#colorMultiplier; the tint index is the render pass. */
    public static int color(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        return 0xFF000000 | LegacyBlockRenderTables.routerPassColor(tintIndex);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        return (state == null ? this.itemQuads : this.worldQuads)[side.get3DDataValue()];
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return CUTOUT;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.base;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}

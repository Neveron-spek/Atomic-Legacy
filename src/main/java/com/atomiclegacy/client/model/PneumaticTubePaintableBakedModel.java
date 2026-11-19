package com.atomiclegacy.client.model;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * [parity V2: pneumatic_tube_paintable] RenderBlockMultipass with PneumoTubePaintableBlock.getIcon:
 * pass 0 is the paint block's face (or pneumatic_tube_paintable); pass 1, only at metadata 0 (not defused), is
 * pneumatic_tube_paintable_overlay_in on the ejection side, _overlay_out on the insertion side and _overlay elsewhere.
 * The item keeps pass 0 only (shouldRenderItemMulti is false) and uses the plain cube model.
 */
public final class PneumaticTubePaintableBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = AtomicLegacy.id("pneumatic_tube_paintable");
    private static final ModelProperty<PaintData> PAINT_DATA = new ModelProperty<>(Objects::nonNull);
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final SimpleModelState IDENTITY = new SimpleModelState(Transformation.identity());
    private final BakedModel fallback;
    private final TextureAtlasSprite base;
    private final Map<Direction, List<BakedQuad>> baseFaces;
    private final Map<Direction, List<BakedQuad>> overlayFaces;
    private final Map<Direction, List<BakedQuad>> overlayInFaces;
    private final Map<Direction, List<BakedQuad>> overlayOutFaces;

    private PneumaticTubePaintableBakedModel(BakedModel fallback, TextureAtlasSprite base, TextureAtlasSprite overlay,
                                             TextureAtlasSprite overlayIn, TextureAtlasSprite overlayOut) {
        this.fallback = fallback;
        this.base = base;
        this.baseFaces = bakeFaces(base, false);
        this.overlayFaces = bakeFaces(overlay, true);
        this.overlayInFaces = bakeFaces(overlayIn, true);
        this.overlayOutFaces = bakeFaces(overlayOut, true);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textures) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        BakedModel fallback = locations.stream().map(models::get).filter(Objects::nonNull).findFirst().orElse(null);
        if (fallback == null) return;
        PneumaticTubePaintableBakedModel model = new PneumaticTubePaintableBakedModel(fallback,
                sprite(textures, "pneumatic_tube_paintable"), sprite(textures, "pneumatic_tube_paintable_overlay"),
                sprite(textures, "pneumatic_tube_paintable_overlay_in"), sprite(textures, "pneumatic_tube_paintable_overlay_out"));
        for (ModelResourceLocation location : locations) models.put(location, model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube) {
            Block paint = tube.paintBlock();
            return modelData.derive().with(PAINT_DATA, new PaintData(paint == null ? null : paint.defaultBlockState(),
                    tube.overlayHidden(), tube.insertionDir(), tube.ejectionDir())).build();
        }
        return modelData;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        boolean own = renderType == null || renderType == RenderType.cutout();
        if (side == null) return List.of();
        PaintData paint = state == null ? null : data.get(PAINT_DATA);
        List<BakedQuad> quads = new ArrayList<>();
        if (paint != null && paint.paint() != null) {
            BakedModel surface = Minecraft.getInstance().getBlockRenderer().getBlockModel(paint.paint());
            quads.addAll(surface.getQuads(paint.paint(), side, random, ModelData.EMPTY, renderType));
        } else if (own) {
            quads.addAll(baseFaces.get(side));
        }
        if (own && state != null && (paint == null || !paint.overlayHidden())) {
            Direction ejection = paint == null ? null : paint.ejection();
            Direction insertion = paint == null ? null : paint.insertion();
            if (side == ejection) quads.addAll(overlayInFaces.get(side));
            else if (side == insertion) quads.addAll(overlayOutFaces.get(side));
            else quads.addAll(overlayFaces.get(side));
        }
        return List.copyOf(quads);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        PaintData paint = data.get(PAINT_DATA);
        if (paint == null || paint.paint() == null) return RENDER_TYPES;
        ChunkRenderTypeSet paintTypes = Minecraft.getInstance().getBlockRenderer().getBlockModel(paint.paint())
                .getRenderTypes(paint.paint(), random, ModelData.EMPTY);
        return ChunkRenderTypeSet.union(paintTypes, RENDER_TYPES);
    }

    private static Map<Direction, List<BakedQuad>> bakeFaces(TextureAtlasSprite sprite, boolean offset) {
        FaceBakery bakery = new FaceBakery();
        Map<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            Vector3f from = new Vector3f(0.0F, 0.0F, 0.0F);
            Vector3f to = new Vector3f(16.0F, 16.0F, 16.0F);
            if (offset) {
                float epsilon = 0.001F;
                switch (face) {
                    case DOWN -> from.set(from.x(), -epsilon, from.z());
                    case UP -> to.set(to.x(), 16.0F + epsilon, to.z());
                    case NORTH -> from.set(from.x(), from.y(), -epsilon);
                    case SOUTH -> to.set(to.x(), to.y(), 16.0F + epsilon);
                    case WEST -> from.set(-epsilon, from.y(), from.z());
                    case EAST -> to.set(16.0F + epsilon, to.y(), to.z());
                }
            }
            faces.put(face, List.of(bakery.bakeQuad(from, to,
                    new BlockElementFace(face, BlockElementFace.NO_TINT, "#surface",
                            new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)),
                    sprite, face, IDENTITY, null, true)));
        }
        return Map.copyOf(faces);
    }

    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return base; }
    @Override public TextureAtlasSprite getParticleIcon() { return base; }
    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }

    private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> textures, String path) {
        return textures.apply(new Material(TextureAtlas.LOCATION_BLOCKS, AtomicLegacy.id("block/" + path)));
    }

    private record PaintData(@Nullable BlockState paint, boolean overlayHidden,
                             @Nullable Direction insertion, @Nullable Direction ejection) {
    }
}

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
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * [parity e1] 1.7.10 RenderBlockSideRotation for BlockWandJigsaw, BlockWandTandem, BlockWandLoot and BlockWandLogic:
 * a full cube whose faces take getIcon(side, meta) and are turned by getRotationFromSide (uvRotate*). The item
 * (renderInventoryBlock) draws getIcon(side, 0) without rotation.
 */
public final class WandBlockModel implements IDynamicBakedModel {
    private final BakedModel delegate;
    private final LegacyBlockRenderTables.Wand wand;
    private final List<BakedQuad>[][] worldQuads;
    private final List<BakedQuad>[] itemQuads;

    @SuppressWarnings("unchecked")
    private WandBlockModel(BakedModel delegate, LegacyBlockRenderTables.Wand wand,
                           Function<Material, TextureAtlasSprite> textureGetter) {
        this.delegate = delegate;
        this.wand = wand;
        TextureAtlasSprite[] icons = new TextureAtlasSprite[4];
        for (int icon = 0; icon < icons.length; icon++) {
            icons[icon] = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                    AtomicLegacy.id("block/" + wand.texture(icon))));
        }
        this.worldQuads = new List[wand.metadataCount()][];
        for (int meta = 0; meta < this.worldQuads.length; meta++) {
            this.worldQuads[meta] = bake(icons, meta, true);
        }
        this.itemQuads = bake(icons, 0, false);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!AtomicLegacy.MOD_ID.equals(location.id().getNamespace())) {
                continue;
            }
            LegacyBlockRenderTables.Wand wand = LegacyBlockRenderTables.Wand.byId(location.id().getPath());
            BakedModel model = models.get(location);
            if (wand != null && model != null && !(model instanceof WandBlockModel)) {
                models.put(location, new WandBlockModel(model, wand, textureGetter));
            }
        }
    }

    private List<BakedQuad>[] bake(TextureAtlasSprite[] icons, int meta, boolean rotated) {
        EnumMap<Direction, TextureAtlasSprite> faces = new EnumMap<>(Direction.class);
        EnumMap<Direction, Integer> rotations = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            int side = direction.get3DDataValue();
            faces.put(direction, icons[LegacyBlockRenderTables.icon(this.wand, side, meta)]);
            if (rotated) {
                rotations.put(direction, LegacyBlockRenderTables.faceRotation(this.wand, side, meta));
            }
        }
        return legacyCube(faces, rotations, null);
    }

    /**
     * A full cube with the 1.7.10 RenderBlocks face UVs, split by cull face. renderStandardBlock shades these blocks
     * with ambient occlusion, so the quads keep it enabled. tintIndexes may be null (no tint).
     */
    @SuppressWarnings("unchecked")
    static List<BakedQuad>[] legacyCube(EnumMap<Direction, TextureAtlasSprite> faces,
                                        EnumMap<Direction, Integer> rotations, @Nullable int[] tintIndexes) {
        List<BakedQuad> all = new ArrayList<>();
        CableModelGeometry.addLegacyBox(all, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, faces, rotations);
        List<BakedQuad>[] bySide = new List[6];
        for (int i = 0; i < 6; i++) {
            bySide[i] = new ArrayList<>(2);
        }
        for (BakedQuad quad : all) {
            int side = quad.getDirection().get3DDataValue();
            bySide[side].add(new BakedQuad(quad.getVertices(), tintIndexes == null ? -1 : tintIndexes[side],
                    quad.getDirection(), quad.getSprite(), quad.isShade(), true));
        }
        return bySide;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        if (state == null) {
            return this.itemQuads[side.get3DDataValue()];
        }
        Direction facing = Direction.SOUTH;
        if (this.wand.sixWay() && state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (!this.wand.sixWay() && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return this.worldQuads[LegacyBlockRenderTables.metadata(this.wand, facing)][side.get3DDataValue()];
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
        return this.delegate.getParticleIcon();
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

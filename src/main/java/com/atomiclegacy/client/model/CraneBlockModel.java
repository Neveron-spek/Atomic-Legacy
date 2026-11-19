package com.atomiclegacy.client.model;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.CraneMachineBlockEntity;
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
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Port of the 1.7.10 crane block rendering (BlockCraneBase#getIcon + RenderBlockSideRotation): every face picks its
 * icon from the input side (block metadata) and the output side (screwdriver override), and the top face is turned
 * by the block's getRotationFromSide. Covers CraneInserter, CraneExtractor, CraneGrabber, CraneBoxer, CraneUnboxer.
 */
public final class CraneBlockModel implements IDynamicBakedModel {
    /** ForgeDirection.ROTATION_MATRIX: [axis][direction] -> direction turned around that axis. */
    private static final int[][] ROTATION_MATRIX = {
            {0, 1, 4, 5, 3, 2},
            {0, 1, 5, 4, 2, 3},
            {5, 4, 2, 3, 0, 1},
            {4, 5, 2, 3, 1, 0},
            {2, 3, 1, 0, 4, 5},
            {3, 2, 0, 1, 4, 5}
    };

    private static final Map<String, Icons> KINDS = Map.of(
            "crane_inserter", Icons.normal("crane_in", "crane_in", "crane_side_in", "crane_out", "crane_side_out", false),
            "crane_grabber", Icons.normal("crane_grabber", "crane_pull", "crane_side_pull", "crane_out", "crane_side_out", false),
            "crane_boxer", Icons.normal("crane_boxer", "crane_in", "crane_side_in", "crane_box", "crane_side_box", false),
            "crane_extractor", Icons.swapped("crane_out", "crane_in", "crane_side_in", "crane_out", "crane_side_out"),
            "crane_unboxer", Icons.swapped("crane_unboxer", "crane_in", "crane_side_in", "crane_box", "crane_side_box"));

    /** Icon names of BlockCraneBase and its subclasses (registerBlockIcons). */
    private record Icons(String in, String sideIn, String out, String sideOut, String directional, String directionalUp,
                         String directionalDown, String turnLeft, String turnRight, String sideLeftTurnUp,
                         String sideRightTurnUp, String sideLeftTurnDown, String sideRightTurnDown, String sideUpTurnLeft,
                         String sideUpTurnRight, String sideDownTurnLeft, String sideDownTurnRight, boolean turnedRotation) {
        /** CraneInserter, CraneGrabber, CraneBoxer. */
        static Icons normal(String p, String in, String sideIn, String out, String sideOut, boolean turned) {
            return new Icons(in, sideIn, out, sideOut, p + "_top", p + "_side_up", p + "_side_down", p + "_top_left",
                    p + "_top_right", p + "_side_left_turn_up", p + "_side_right_turn_up", p + "_side_left_turn_down",
                    p + "_side_right_turn_down", p + "_side_up_turn_left", p + "_side_up_turn_right",
                    p + "_side_down_turn_left", p + "_side_down_turn_right", turned);
        }

        /** CraneExtractor, CraneUnboxer: the same textures registered into the mirrored fields. */
        static Icons swapped(String p, String in, String sideIn, String out, String sideOut) {
            return new Icons(in, sideIn, out, sideOut, p + "_top", p + "_side_down", p + "_side_up", p + "_top_right",
                    p + "_top_left", p + "_side_up_turn_left", p + "_side_up_turn_right", p + "_side_down_turn_left",
                    p + "_side_down_turn_right", p + "_side_left_turn_up", p + "_side_right_turn_up",
                    p + "_side_left_turn_down", p + "_side_right_turn_down", true);
        }
    }

    private final BakedModel delegate;
    private final Icons icons;
    private final Function<String, TextureAtlasSprite> sprites;
    private final Map<Integer, List<BakedQuad>[]> cache = new HashMap<>();
    private final List<BakedQuad>[] itemQuads;

    private CraneBlockModel(BakedModel delegate, Icons icons, Function<Material, TextureAtlasSprite> textureGetter) {
        this.delegate = delegate;
        this.icons = icons;
        Map<String, TextureAtlasSprite> resolved = new HashMap<>();
        this.sprites = name -> resolved.computeIfAbsent(name,
                key -> textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, AtomicLegacy.id("block/" + key))));
        this.itemQuads = bakeItem();
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!AtomicLegacy.MOD_ID.equals(location.id().getNamespace())) {
                continue;
            }
            Icons icons = KINDS.get(location.id().getPath());
            BakedModel model = models.get(location);
            if (icons != null && model != null && !(model instanceof CraneBlockModel)) {
                models.put(location, new CraneBlockModel(model, icons, textureGetter));
            }
        }
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
        Direction input = data.get(CraneMachineBlockEntity.INPUT_SIDE);
        Direction output = data.get(CraneMachineBlockEntity.OUTPUT_SIDE);
        if (input == null) {
            input = state.hasProperty(com.atomiclegacy.blocks.CraneMachineBlock.FACING)
                    ? state.getValue(com.atomiclegacy.blocks.CraneMachineBlock.FACING) : Direction.NORTH;
        }
        if (output == null || output == input) {
            output = input.getOpposite();
        }
        int key = input.get3DDataValue() * 6 + output.get3DDataValue();
        List<BakedQuad>[] quads;
        synchronized (this.cache) {
            quads = this.cache.get(key);
            if (quads == null) {
                quads = bakeWorld(input, output);
                this.cache.put(key, quads);
            }
        }
        return quads[side.get3DDataValue()];
    }

    /** BlockCraneBase#getIcon(IBlockAccess, x, y, z, side). */
    private String worldIcon(Direction inputSide, Direction outputSide, int side) {
        boolean overridden = outputSide.getOpposite() != inputSide;
        int in = inputSide.get3DDataValue();
        int out = outputSide.get3DDataValue();
        int leftHand = ROTATION_MATRIX[in][out]; // outputSide.getRotation(inputSide)

        if (side == 0 || side == 1) {
            if (side == out) return this.icons.out;
            if (side == in) return this.icons.in;
            if (side == 1) {
                if (overridden) {
                    if (leftHand == 1) return this.icons.turnLeft;
                    if (leftHand == 0) return this.icons.turnRight;
                } else {
                    return this.icons.directional;
                }
            }
            return "crane_top";
        }
        if (side == out) return this.icons.sideOut;
        if (side == in) return this.icons.sideIn;
        if (overridden) {
            if (leftHand == side) {
                if (out == 1) return this.icons.sideLeftTurnUp;
                if (out == 0) return this.icons.sideRightTurnDown;
                if (in == 1) return this.icons.sideUpTurnRight;
                if (in == 0) return this.icons.sideDownTurnLeft;
            }
            if ((leftHand ^ 1) == side) {
                if (out == 1) return this.icons.sideRightTurnUp;
                if (out == 0) return this.icons.sideLeftTurnDown;
                if (in == 1) return this.icons.sideUpTurnLeft;
                if (in == 0) return this.icons.sideDownTurnRight;
            }
        } else {
            if (out == 1) return this.icons.directionalUp;
            if (out == 0) return this.icons.directionalDown;
        }
        return "crane_side";
    }

    /** getRotationFromSide(world, x, y, z, 1): BlockCraneBase, overridden by CraneExtractor and CraneUnboxer. */
    private int topRotation(Direction inputSide, Direction outputSide) {
        int meta = inputSide.get3DDataValue();
        if (meta <= 1) {
            return 0;
        }
        int index = meta - 2;
        if (!this.icons.turnedRotation) {
            return new int[] {3, 0, 1, 2}[index];
        }
        int leftHand = ROTATION_MATRIX[meta][outputSide.get3DDataValue()];
        if (leftHand == 1) {
            return new int[] {2, 1, 3, 0}[index];
        }
        if (leftHand == 0) {
            return new int[] {1, 2, 0, 3}[index];
        }
        return new int[] {0, 3, 2, 1}[index];
    }

    private List<BakedQuad>[] bakeWorld(Direction input, Direction output) {
        EnumMap<Direction, TextureAtlasSprite> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, this.sprites.apply(worldIcon(input, output, direction.get3DDataValue())));
        }
        EnumMap<Direction, Integer> rotations = new EnumMap<>(Direction.class);
        rotations.put(Direction.UP, topRotation(input, output));
        return bake(faces, rotations);
    }

    /** BlockCraneBase#getIcon(side, 0) through RenderBlockSideRotation#renderInventoryBlock. */
    private List<BakedQuad>[] bakeItem() {
        EnumMap<Direction, TextureAtlasSprite> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.DOWN, this.sprites.apply(this.icons.out));
        faces.put(Direction.UP, this.sprites.apply(this.icons.in));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            faces.put(direction, this.sprites.apply(this.icons.directionalUp));
        }
        return bake(faces, new EnumMap<>(Direction.class));
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[] bake(EnumMap<Direction, TextureAtlasSprite> faces, EnumMap<Direction, Integer> rotations) {
        List<BakedQuad> all = new ArrayList<>();
        CableModelGeometry.addLegacyBox(all, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, faces, rotations);
        List<BakedQuad>[] bySide = new List[6];
        for (int i = 0; i < 6; i++) {
            bySide[i] = new ArrayList<>(1);
        }
        for (BakedQuad quad : all) {
            bySide[quad.getDirection().get3DDataValue()].add(quad);
        }
        return bySide;
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
        return this.sprites.apply("crane_side");
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

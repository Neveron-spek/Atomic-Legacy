package com.atomiclegacy.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyBlockRenderTables;
import com.atomiclegacy.blocks.LegacyBlockRenderTables.Wand;
import com.atomiclegacy.registry.HbmBlocks;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * [parity e1] Server-safe checks of the block appearance batch: the face icon / uvRotate tables of the
 * IBlockSideRotation wand blocks against tables transcribed from 1.7.10 (BlockWandJigsaw, BlockWandTandem,
 * BlockWandLoot, BlockWandLogic + RenderBlockSideRotation), the CraneRouter multipass colours, the CranePartitioner
 * rotation and the resources the client models read. Not covered here (client only): the actual drawing.
 */
public final class ParityE1RenderChecks {
    private static final String ASSETS = "/assets/" + AtomicLegacy.MOD_ID + "/";

    // 1.7.10 BlockWandJigsaw / BlockWandTandem, [meta][side]. Icons: 0 blockIcon, 1 iconTop, 2 iconSide, 3 iconBack.
    private static final int[][] SIX_WAY_ICON = {
            {0, 3, 2, 2, 1, 1}, {3, 0, 2, 2, 1, 1}, {1, 1, 0, 3, 2, 2},
            {1, 1, 3, 0, 2, 2}, {1, 1, 2, 2, 0, 3}, {1, 1, 2, 2, 3, 0}};
    // getRotationFromSide(side), [meta][side].
    private static final int[][] SIX_WAY_ROTATION = {
            {0, 0, 0, 0, 0, 0}, {0, 0, 3, 3, 3, 3}, {3, 3, 0, 0, 2, 1},
            {0, 0, 0, 0, 1, 2}, {2, 1, 1, 2, 0, 0}, {1, 2, 2, 1, 0, 0}};
    // uvRotate applied to the face on each side (RenderBlockSideRotation swaps 2<->3 and 4<->5), [meta][face].
    private static final int[][] SIX_WAY_FACE_ROTATION = {
            {0, 0, 0, 0, 0, 0}, {0, 0, 3, 3, 3, 3}, {3, 3, 0, 0, 1, 2},
            {0, 0, 0, 0, 2, 1}, {2, 1, 2, 1, 0, 0}, {1, 2, 1, 2, 0, 0}};
    // 1.7.10 BlockWandLoot / BlockWandLogic, [meta 0..3][side]; the side faces are never turned.
    private static final int[][] YAW_ROTATION = {
            {0, 0, 0, 0, 0, 0}, {2, 1, 0, 0, 0, 0}, {1, 2, 0, 0, 0, 0}, {3, 3, 0, 0, 0, 0}};
    // onBlockPlacedBy of loot/logic: yaw index 0 (south), 1 (west), 2 (north), 3 (east) -> metadata.
    private static final int[] YAW_INDEX_META = {3, 2, 0, 1};
    private static final Direction[] YAW_INDEX_LOOK = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    // CraneRouter#colorMultiplier by pass.
    private static final int[] ROUTER_COLORS = {0xffffff, 0xff0000, 0xff8000, 0xffff00, 0x00ff00, 0x0080ff, 0x8000ff};
    // RenderPartitioner#renderWorldBlock: rotation in degrees by metadata 2..5.
    private static final int[] PARTITIONER_DEGREES = {90, 270, 180, 0};

    private ParityE1RenderChecks() {
    }

    public static int run(ServerLevel level, List<String> errors) {
        int[] cases = {0};
        checkWandTables(errors, cases);
        checkWandStates(errors, cases);
        checkRouter(errors, cases);
        checkPartitioner(errors, cases);
        checkResources(errors, cases);
        return cases[0];
    }

    private static void check(boolean ok, String message, List<String> errors, int[] cases) {
        cases[0]++;
        if (!ok) {
            errors.add("[e1 render] " + message);
        }
    }

    private static void checkWandTables(List<String> errors, int[] cases) {
        int[] source = {0, 1, 3, 2, 5, 4};
        for (int face = 0; face < 6; face++) {
            check(LegacyBlockRenderTables.faceRotationSource(face) == source[face],
                    "uvRotate source of face " + face, errors, cases);
            check(LegacyBlockRenderTables.isOpposite(face, face ^ 1) && !LegacyBlockRenderTables.isOpposite(face, face),
                    "isOpposite " + face, errors, cases);
        }
        int[] topToBottom = {0, 2, 1, 3};
        for (int rotation = 0; rotation < 4; rotation++) {
            check(LegacyBlockRenderTables.topToBottom(rotation) == topToBottom[rotation],
                    "topToBottom " + rotation, errors, cases);
        }
        for (Wand wand : Wand.values()) {
            check(wand.metadataCount() == (wand.sixWay() ? 6 : 4), wand.id() + " metadata count", errors, cases);
            for (int meta = 0; meta < wand.metadataCount(); meta++) {
                for (int side = 0; side < 6; side++) {
                    String where = wand.id() + " meta " + meta + " side " + side;
                    int icon = wand.sixWay() ? SIX_WAY_ICON[meta][side] : (side <= 1 ? 1 : 0);
                    int rotation = wand.sixWay() ? SIX_WAY_ROTATION[meta][side] : YAW_ROTATION[meta][side];
                    int faceRotation = wand.sixWay() ? SIX_WAY_FACE_ROTATION[meta][side] : YAW_ROTATION[meta][side];
                    int actualIcon = LegacyBlockRenderTables.icon(wand, side, meta);
                    check(actualIcon == icon, where + ": icon " + actualIcon + " != " + icon, errors, cases);
                    int actualRotation = LegacyBlockRenderTables.rotationFromSide(wand, side, meta);
                    check(actualRotation == rotation,
                            where + ": getRotationFromSide " + actualRotation + " != " + rotation, errors, cases);
                    int actualFace = LegacyBlockRenderTables.faceRotation(wand, side, meta);
                    check(actualFace == faceRotation,
                            where + ": face uvRotate " + actualFace + " != " + faceRotation, errors, cases);
                }
            }
            String id = wand.id();
            check(wand.texture(LegacyBlockRenderTables.ICON_FRONT).equals(id)
                            && wand.texture(LegacyBlockRenderTables.ICON_TOP).equals(id + "_top")
                            && wand.texture(LegacyBlockRenderTables.ICON_SIDE).equals(wand.sixWay() ? id + "_side" : id)
                            && wand.texture(LegacyBlockRenderTables.ICON_BACK).equals(wand.sixWay() ? id + "_back" : id),
                    id + " icon texture names", errors, cases);
            for (int icon = 0; icon < 4; icon++) {
                checkResource("textures/block/" + wand.texture(icon) + ".png", errors, cases);
            }
        }
    }

    private static void checkWandStates(List<String> errors, int[] cases) {
        Map<Wand, Block> blocks = Map.of(
                Wand.JIGSAW, HbmBlocks.WAND_JIGSAW.get(), Wand.TANDEM, HbmBlocks.WAND_TANDEM.get(),
                Wand.LOOT, HbmBlocks.WAND_LOOT.get(), Wand.LOGIC, HbmBlocks.WAND_LOGIC.get());
        for (Wand wand : Wand.values()) {
            Block block = blocks.get(wand);
            DirectionProperty property = wand.sixWay() ? BlockStateProperties.FACING : BlockStateProperties.HORIZONTAL_FACING;
            List<BlockState> states = block.getStateDefinition().getPossibleStates();
            check(states.size() == wand.metadataCount(),
                    wand.id() + ": " + states.size() + " states, expected " + wand.metadataCount(), errors, cases);
            boolean[] seen = new boolean[wand.metadataCount()];
            for (BlockState state : states) {
                if (!state.hasProperty(property)) {
                    check(false, wand.id() + ": state without facing " + state, errors, cases);
                    continue;
                }
                Direction facing = state.getValue(property);
                int meta = LegacyBlockRenderTables.metadata(wand, facing);
                boolean valid = meta >= 0 && meta < seen.length && !seen[meta];
                check(valid, wand.id() + ": metadata " + meta + " of " + facing, errors, cases);
                if (valid) {
                    seen[meta] = true;
                }
                if (wand.sixWay()) {
                    // The front (blockIcon) is on the side the block faces, the back on the opposite one.
                    check(meta == facing.get3DDataValue()
                                    && LegacyBlockRenderTables.icon(wand, facing.get3DDataValue(), meta)
                                    == LegacyBlockRenderTables.ICON_FRONT
                                    && LegacyBlockRenderTables.icon(wand, facing.getOpposite().get3DDataValue(), meta)
                                    == LegacyBlockRenderTables.ICON_BACK,
                            wand.id() + ": front/back of " + facing, errors, cases);
                }
            }
            if (!wand.sixWay()) {
                // Placement: the port faces the block against the player's look, 1.7.10 stores YAW_INDEX_META.
                for (int index = 0; index < 4; index++) {
                    int meta = LegacyBlockRenderTables.metadata(wand, YAW_INDEX_LOOK[index].getOpposite());
                    check(meta == YAW_INDEX_META[index],
                            wand.id() + ": yaw index " + index + " gives metadata " + meta, errors, cases);
                }
            }
            JsonObject variants = variants(wand.id(), errors, cases);
            if (variants != null) {
                check(variants.size() == wand.metadataCount(), wand.id() + ": blockstate variant count", errors, cases);
                for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                    JsonObject variant = entry.getValue().getAsJsonObject();
                    // The baked model turns the faces itself; a blockstate rotation would be a second turn.
                    check(!variant.has("x") && !variant.has("y")
                                    && (AtomicLegacy.MOD_ID + ":block/" + wand.id()).equals(string(variant, "model")),
                            wand.id() + ": blockstate variant " + entry.getKey(), errors, cases);
                }
            }
        }
    }

    private static void checkRouter(List<String> errors, int[] cases) {
        check(LegacyBlockRenderTables.ROUTER_PASSES == ROUTER_COLORS.length, "router pass count", errors, cases);
        for (int pass = 0; pass < ROUTER_COLORS.length; pass++) {
            int color = LegacyBlockRenderTables.routerPassColor(pass);
            check(color == ROUTER_COLORS[pass],
                    "router pass " + pass + " colour " + Integer.toHexString(color), errors, cases);
            for (int side = 0; side < 6; side++) {
                boolean expected = pass == 0 || side == pass - 1;
                check(LegacyBlockRenderTables.routerPassDrawsSide(pass, side) == expected,
                        "router pass " + pass + " side " + side, errors, cases);
            }
        }
        check(HbmBlocks.CRANE_ROUTER.get().defaultBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING),
                "crane_router facing property", errors, cases);
        checkResource("textures/block/crane_in.png", errors, cases);
        checkResource("textures/block/crane_router_overlay.png", errors, cases);
        String model = text("models/block/crane_router.json");
        check(model != null && model.contains("\"" + AtomicLegacy.MOD_ID + ":block/crane_in\""),
                "models/block/crane_router.json: pass 0 texture crane_in", errors, cases);
    }

    private static void checkPartitioner(List<String> errors, int[] cases) {
        Block block = HbmBlocks.CRANE_PARTITIONER.get();
        check(block.defaultBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING),
                "crane_partitioner facing property", errors, cases);
        JsonObject variants = variants("crane_partitioner", errors, cases);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int meta = facing.get3DDataValue();
            int degrees = LegacyBlockRenderTables.partitionerRotationDegrees(meta);
            check(degrees == PARTITIONER_DEGREES[meta - 2], "partitioner rotation of meta " + meta, errors, cases);
            // crane_buffer.obj has its "Back" wall on +X. Vec3#rotateAroundY: x' = x cos + z sin, z' = z cos - x sin.
            double radians = Math.toRadians(degrees);
            long x = Math.round(Math.cos(radians));
            long z = Math.round(-Math.sin(radians));
            check(x == facing.getStepX() && z == facing.getStepZ(),
                    "partitioner back wall of meta " + meta + " at " + x + "," + z, errors, cases);
            int y = LegacyBlockRenderTables.partitionerBlockstateY(facing);
            Direction turned = Direction.EAST;
            for (int quarter = 0; quarter < y / 90; quarter++) {
                turned = turned.getClockWise();
            }
            check(y % 90 == 0 && turned == facing, "partitioner blockstate y " + y + " for " + facing, errors, cases);
            if (variants == null) {
                continue;
            }
            int matched = 0;
            for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                if (!("," + entry.getKey() + ",").contains(",facing=" + facing.getSerializedName() + ",")) {
                    continue;
                }
                matched++;
                JsonObject variant = entry.getValue().getAsJsonObject();
                int actual = variant.has("y") ? variant.get("y").getAsInt() : 0;
                check(actual == y && !variant.has("x")
                                && (AtomicLegacy.MOD_ID + ":block/crane_partitioner").equals(string(variant, "model")),
                        "crane_partitioner blockstate " + entry.getKey() + ": y " + actual + " != " + y, errors, cases);
            }
            check(matched > 0, "crane_partitioner blockstate has no variant for " + facing, errors, cases);
        }
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            check(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis().isHorizontal(),
                    "crane_partitioner state " + state, errors, cases);
        }
        // RenderPartitioner#renderInventoryBlock turns the model by 90 degrees like renderBlockAsItem does for a
        // standard cube, so the 1.7.10 icon shows the -X and +Z sides of crane_buffer.obj. minecraft:block/block
        // shows +X and -Z of a model, hence every display rotation is block/block's with y + 180.
        String[] contexts = {"gui", "ground", "fixed", "thirdperson_righthand", "firstperson_righthand",
                "firstperson_lefthand"};
        int[][] rotations = {{30, 45, 0}, {0, 180, 0}, {0, 180, 0}, {75, 225, 0}, {0, 225, 0}, {0, 45, 0}};
        JsonObject display = null;
        String item = text("models/item/crane_partitioner.json");
        if (item != null) {
            try {
                JsonObject root = JsonParser.parseString(item).getAsJsonObject();
                if ((AtomicLegacy.MOD_ID + ":block/crane_partitioner").equals(string(root, "parent"))
                        && root.has("display") && root.get("display").isJsonObject()) {
                    display = root.getAsJsonObject("display");
                }
            } catch (RuntimeException exception) {
                display = null;
            }
        }
        check(display != null, "models/item/crane_partitioner.json: parent or display", errors, cases);
        for (int index = 0; display != null && index < contexts.length; index++) {
            boolean ok = false;
            try {
                com.google.gson.JsonArray rotation = display.getAsJsonObject(contexts[index]).getAsJsonArray("rotation");
                ok = rotation.size() == 3 && rotation.get(0).getAsInt() == rotations[index][0]
                        && rotation.get(1).getAsInt() == rotations[index][1]
                        && rotation.get(2).getAsInt() == rotations[index][2];
            } catch (RuntimeException exception) {
                ok = false;
            }
            check(ok, "crane_partitioner item display " + contexts[index], errors, cases);
        }
    }

    private static void checkResources(List<String> errors, int[] cases) {
        // RenderPartitioner#drawPartitioner: group -> icon.
        String[][] parts = {
                {"Side", "Side", "crane_partitioner_side"}, {"Back", "Back", "crane_partitioner_back"},
                {"Top_Top.001", "Top", "crane_top"}, {"Inner", "Inner", "crane_splitter_inner"},
                {"InnerSide", "InnerSide", "crane_splitter_inner_side"}, {"Belt", "Belt", "crane_splitter_belt"}};
        String obj = text("models/obj/blocks/crane_partitioner.obj");
        String mtl = text("models/obj/blocks/crane_partitioner.mtl");
        String model = text("models/block/crane_partitioner.json");
        check(obj != null, "missing models/obj/blocks/crane_partitioner.obj", errors, cases);
        check(mtl != null, "missing models/obj/blocks/crane_partitioner.mtl", errors, cases);
        check(model != null && model.contains("models/obj/blocks/crane_partitioner.obj")
                        && model.contains("models/obj/blocks/crane_partitioner.mtl") && model.contains("\"neoforge:obj\""),
                "models/block/crane_partitioner.json does not load crane_partitioner.obj", errors, cases);
        String objLines = obj == null ? "" : "\n" + obj.replace("\r", "") + "\n";
        String mtlLines = mtl == null ? "" : "\n" + mtl.replace("\r", "") + "\n";
        for (String[] part : parts) {
            check(objLines.contains("\no " + part[0] + "\nusemtl " + part[1] + "\n"),
                    "crane_partitioner.obj group " + part[0] + " -> material " + part[1], errors, cases);
            int start = mtlLines.indexOf("\nnewmtl " + part[1] + "\n");
            int end = start < 0 ? -1 : mtlLines.indexOf("\nnewmtl ", start + 1);
            String body = start < 0 ? "" : mtlLines.substring(start, end < 0 ? mtlLines.length() : end);
            check(body.contains("\nmap_Kd " + AtomicLegacy.MOD_ID + ":block/" + part[2] + "\n"),
                    "crane_partitioner.mtl material " + part[1] + " -> " + part[2], errors, cases);
            checkResource("textures/block/" + part[2] + ".png", errors, cases);
        }
        checkResource("textures/block/crane_splitter_belt.png.mcmeta", errors, cases);
    }

    private static JsonObject variants(String block, List<String> errors, int[] cases) {
        String json = text("blockstates/" + block + ".json");
        JsonObject variants = null;
        if (json != null) {
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("variants") && root.get("variants").isJsonObject()) {
                    variants = root.getAsJsonObject("variants");
                }
            } catch (RuntimeException exception) {
                variants = null;
            }
        }
        check(variants != null, "blockstates/" + block + ".json has no variants", errors, cases);
        return variants;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static void checkResource(String path, List<String> errors, int[] cases) {
        boolean present;
        try (InputStream stream = ParityE1RenderChecks.class.getResourceAsStream(ASSETS + path)) {
            present = stream != null;
        } catch (java.io.IOException exception) {
            present = false;
        }
        check(present, "missing resource " + path, errors, cases);
    }

    private static String text(String path) {
        try (InputStream stream = ParityE1RenderChecks.class.getResourceAsStream(ASSETS + path)) {
            return stream == null ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            return null;
        }
    }
}

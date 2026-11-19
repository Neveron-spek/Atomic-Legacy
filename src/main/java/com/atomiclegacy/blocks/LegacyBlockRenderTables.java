package com.atomiclegacy.blocks;

import net.minecraft.core.Direction;

/**
 * [parity e1] Side-independent (no client classes) tables of the 1.7.10 block renderers that the client models read:
 * IBlockSideRotation blocks (BlockWandJigsaw, BlockWandTandem, BlockWandLoot, BlockWandLogic through
 * RenderBlockSideRotation), CraneRouter (RenderBlockMultipass) and CranePartitioner (RenderPartitioner).
 * Sides and metadata use the 1.7.10 numbering: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
 */
public final class LegacyBlockRenderTables {
    /** blockIcon. */
    public static final int ICON_FRONT = 0;
    /** iconTop. */
    public static final int ICON_TOP = 1;
    /** iconSide. */
    public static final int ICON_SIDE = 2;
    /** iconBack. */
    public static final int ICON_BACK = 3;

    /**
     * RenderBlockSideRotation#renderWorldBlock assigns uvRotateBottom/Top/North/South/West/East from sides
     * 0/1/5/4/2/3, and 1.7.10 RenderBlocks reads uvRotateEast in renderFaceZNeg, uvRotateWest in renderFaceZPos,
     * uvRotateNorth in renderFaceXNeg and uvRotateSouth in renderFaceXPos. So the face drawn on side i is turned by
     * getRotationFromSide(FACE_ROTATION_SOURCE[i]).
     */
    private static final int[] FACE_ROTATION_SOURCE = {0, 1, 3, 2, 5, 4};

    /** CraneRouter#getPasses. */
    public static final int ROUTER_PASSES = 7;

    public enum Wand {
        JIGSAW("wand_jigsaw", true),
        TANDEM("wand_tandem", true),
        LOOT("wand_loot", false),
        LOGIC("wand_logic", false);

        private final String id;
        private final boolean sixWay;

        Wand(String id, boolean sixWay) {
            this.id = id;
            this.sixWay = sixWay;
        }

        /** Block id and blockIcon texture name. */
        public String id() {
            return this.id;
        }

        /** true: metadata is a ForgeDirection (jigsaw, tandem); false: metadata 0..3 from the placement yaw. */
        public boolean sixWay() {
            return this.sixWay;
        }

        public int metadataCount() {
            return this.sixWay ? 6 : 4;
        }

        /** registerBlockIcons texture name (assets/hbm/textures/blocks) of an ICON_* value. */
        public String texture(int icon) {
            return switch (icon) {
                case ICON_TOP -> this.id + "_top";
                case ICON_SIDE -> this.sixWay ? this.id + "_side" : this.id;
                case ICON_BACK -> this.sixWay ? this.id + "_back" : this.id;
                default -> this.id;
            };
        }

        public static Wand byId(String id) {
            for (Wand wand : values()) {
                if (wand.id.equals(id)) {
                    return wand;
                }
            }
            return null;
        }
    }

    private LegacyBlockRenderTables() {
    }

    /** IBlockSideRotation#topToBottom. */
    public static int topToBottom(int topRotation) {
        return switch (topRotation) {
            case 1 -> 2;
            case 2 -> 1;
            default -> topRotation;
        };
    }

    /** IBlockSideRotation#isOpposite. */
    public static boolean isOpposite(int from, int to) {
        return switch (from) {
            case 0 -> to == 1;
            case 1 -> to == 0;
            case 2 -> to == 3;
            case 3 -> to == 2;
            case 4 -> to == 5;
            case 5 -> to == 4;
            default -> false;
        };
    }

    /**
     * Block metadata of the port's FACING value. Jigsaw and tandem store the ForgeDirection. Loot and logic store
     * onBlockPlacedBy's yaw index mapping (player looking south 3, west 2, north 0, east 1); the port places them
     * with FACING opposite to the player's look, hence south 0, west 1, east 2, north 3.
     */
    public static int metadata(Wand wand, Direction facing) {
        if (wand.sixWay) {
            return facing.get3DDataValue();
        }
        return switch (facing) {
            case WEST -> 1;
            case EAST -> 2;
            case NORTH -> 3;
            default -> 0;
        };
    }

    /** getIcon(side, meta) as an ICON_* value. */
    public static int icon(Wand wand, int side, int meta) {
        if (!wand.sixWay) {
            return side <= 1 ? ICON_TOP : ICON_FRONT;
        }
        if (side == meta) return ICON_FRONT;
        if (isOpposite(side, meta)) return ICON_BACK;
        if (side <= 1) return ICON_TOP;
        if (side > 3 && meta <= 1) return ICON_TOP;
        return ICON_SIDE;
    }

    /** getRotationFromSide(world, x, y, z, side) for a block with the given metadata. */
    public static int rotationFromSide(Wand wand, int side, int meta) {
        if (!wand.sixWay) {
            if (side == 0) return topToBottom(meta);
            if (side == 1) return meta;
            return 0;
        }
        if (side == 0) return topToBottom(rotationFromSide(wand, 1, meta));
        if (side == meta || isOpposite(side, meta)) return 0;
        if (meta == 0) return 0;
        if (meta == 1) return 3;
        if (side == 1) {
            switch (meta) {
                case 2: return 3;
                case 3: return 0;
                case 4: return 1;
                case 5: return 2;
                default: break;
            }
        }
        if (meta == 2) return side == 4 ? 2 : 1;
        if (meta == 3) return side == 4 ? 1 : 2;
        if (meta == 4) return side == 2 ? 1 : 2;
        if (meta == 5) return side == 2 ? 2 : 1;
        return 0;
    }

    /** The getRotationFromSide argument whose value RenderBlocks applies to the given face. */
    public static int faceRotationSource(int face) {
        return FACE_ROTATION_SOURCE[face];
    }

    /** uvRotate value (0..3) RenderBlocks uses while drawing the given face in the world. */
    public static int faceRotation(Wand wand, int face, int meta) {
        return rotationFromSide(wand, FACE_ROTATION_SOURCE[face], meta);
    }

    /** CraneRouter#colorMultiplier for RenderBlockMultipass.currentPass. */
    public static int routerPassColor(int pass) {
        if (pass == 0) {
            return 0xffffff;
        }
        return switch (pass - 1) {
            case 0 -> 0xff0000;
            case 1 -> 0xff8000;
            case 2 -> 0xffff00;
            case 3 -> 0x00ff00;
            case 4 -> 0x0080ff;
            case 5 -> 0x8000ff;
            default -> 0xffffff;
        };
    }

    /** CraneRouter#shouldSideBeRendered: pass 0 draws every side, pass n only side n - 1. */
    public static boolean routerPassDrawsSide(int pass, int side) {
        return pass == 0 || side == pass - 1;
    }

    /** RenderPartitioner#renderWorldBlock: rotation (degrees, Vec3#rotateAroundY) of crane_buffer.obj by metadata. */
    public static int partitionerRotationDegrees(int meta) {
        return switch (meta) {
            case 2 -> 90;
            case 4 -> 180;
            case 3 -> 270;
            default -> 0;
        };
    }

    /**
     * Blockstate "y" equal to {@link #partitionerRotationDegrees}: Vec3#rotateAroundY turns east towards north,
     * a blockstate rotation turns east towards south.
     */
    public static int partitionerBlockstateY(Direction facing) {
        return (360 - partitionerRotationDegrees(facing.get3DDataValue())) % 360;
    }
}

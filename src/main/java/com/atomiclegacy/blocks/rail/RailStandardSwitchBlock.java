package com.atomiclegacy.blocks.rail;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.train.LegacyVec3;
import com.atomiclegacy.train.RailCarEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * [parity S1: trains] BlockRailWaypointSystem + RailStandardSwitch / RailStandardSwitchFlipped.
 * TileEntityRailSwitch.isSwitched is stored as the {@link #SWITCHED} property of the core block so the world model can
 * show the SignTurn / SignStraight part; it is only ever toggled on the core, like the tile entity.
 * Not ported: the server-side ParticleUtil.spawnDroneLine debug lines of every rail link (drone debug particles are
 * not part of the port; the client only drew them while holding drone items).
 */
public class RailStandardSwitchBlock extends RailDummyableBlock {
    public static final BooleanProperty SWITCHED = BooleanProperty.create("switched");

    private final boolean flipped;
    public final List<RailDef> railDefs = new ArrayList<>();

    public RailStandardSwitchBlock(Properties properties, boolean flipped) {
        super(properties);
        this.flipped = flipped;
        registerDefaultState(defaultBlockState().setValue(SWITCHED, false));
        RailDef main = new RailDef("main");
        RailDef side = new RailDef("side");
        railDefs.add(main);
        railDefs.add(side);
        main.nodes.add(LegacyVec3.of(-8.5, 0.1875, 0.5));
        main.nodes.add(LegacyVec3.of(-7.5, 0.1875, 0.5));
        main.nodes.add(LegacyVec3.of(6.5, 0.1875, 0.5));
        main.nodes.add(LegacyVec3.of(7.5, 0.1875, 0.5));
        main.nodes.add(LegacyVec3.of(8.5, 0.1875, 0.5));
        if (!flipped) {
            side.nodes.add(LegacyVec3.of(-8.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-7.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-6.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-5.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-4.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-3.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-2.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-1.5, 0.1875, 4.5));
            side.nodes.add(LegacyVec3.of(-0.5, 0.1875, 4.25));
            side.nodes.add(LegacyVec3.of(0.5, 0.1875, 3.9375));
            side.nodes.add(LegacyVec3.of(1.5, 0.1875, 3.375));
            side.nodes.add(LegacyVec3.of(2.5, 0.1875, 2.4625));
            side.nodes.add(LegacyVec3.of(3.5, 0.1875, 1.75));
            side.nodes.add(LegacyVec3.of(4.5, 0.1875, 1.1875));
            side.nodes.add(LegacyVec3.of(5.5, 0.1875, 0.875));
            side.nodes.add(LegacyVec3.of(6.5, 0.1875, 0.625));
            side.nodes.add(LegacyVec3.of(7.5, 0.1875, 0.5));
            side.nodes.add(LegacyVec3.of(8.5, 0.1875, 0.5));
        } else {
            side.nodes.add(LegacyVec3.of(-8.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-7.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-6.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-5.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-4.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-3.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-2.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-1.5, 0.1875, -3.5));
            side.nodes.add(LegacyVec3.of(-0.5, 0.1875, -3.25));
            side.nodes.add(LegacyVec3.of(0.5, 0.1875, -2.9375));
            side.nodes.add(LegacyVec3.of(1.5, 0.1875, -2.375));
            side.nodes.add(LegacyVec3.of(2.5, 0.1875, -1.4625));
            side.nodes.add(LegacyVec3.of(3.5, 0.1875, -0.75));
            side.nodes.add(LegacyVec3.of(4.5, 0.1875, -0.1875));
            side.nodes.add(LegacyVec3.of(5.5, 0.1875, 0.175));
            side.nodes.add(LegacyVec3.of(6.5, 0.1875, 0.375));
            side.nodes.add(LegacyVec3.of(7.5, 0.1875, 0.5));
            side.nodes.add(LegacyVec3.of(8.5, 0.1875, 0.5));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SWITCHED);
    }

    public boolean flipped() {
        return flipped;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, 7, 7, 1, 0};
    }

    @Override
    public int getOffset() {
        return 7;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    public static boolean isSwitched(Level world, BlockPos core) {
        BlockState state = world.getBlockState(core);
        return state.hasProperty(SWITCHED) && state.getValue(SWITCHED);
    }

    /** Whether the train at position FROM can move towards the waypoint TO along the supplied railDef. */
    public boolean canCross(Level world, int x, int y, int z, LegacyVec3 from, LegacyVec3 to, RailDef def) {
        BlockState core = world.getBlockState(new BlockPos(x, y, z));
        if (core.getBlock() != this) return true;
        Direction dir = orientation(core.getValue(META) - 10);
        if (dir == Direction.EAST) if (from.xCoord < to.xCoord) return true;
        if (dir == Direction.WEST) if (from.xCoord > to.xCoord) return true;
        if (dir == Direction.SOUTH) if (from.zCoord < to.zCoord) return true;
        if (dir == Direction.NORTH) if (from.zCoord > to.zCoord) return true;
        if (dir == Direction.EAST) if (to.xCoord < x + 0.5 + 7) return true;
        if (dir == Direction.WEST) if (to.xCoord > x + 0.5 - 7) return true;
        if (dir == Direction.SOUTH) if (to.zCoord < z + 0.5 + 7) return true;
        if (dir == Direction.NORTH) if (to.zCoord > z + 0.5 - 7) return true;
        if (core.getValue(SWITCHED)) {
            if ("side".equals(def.name)) return true;
        } else {
            if ("main".equals(def.name)) return true;
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // onBlockActivated returns false while holding a train, letting ItemTrain.onItemUse run
        if (!world.isClientSide && !player.isShiftKeyDown() && stack.is(HbmItems.TRAIN.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        int[] core = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());
        if (core != null) {
            BlockPos corePos = new BlockPos(core[0], core[1], core[2]);
            BlockState coreState = world.getBlockState(corePos);
            if (coreState.getBlock() == this) {
                world.setBlock(corePos, coreState.setValue(SWITCHED, !coreState.getValue(SWITCHED)), 3);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public LegacyVec3 getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext());
    }

    @Override
    public LegacyVec3 getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info);
    }

    /** BlockRailWaypointSystem.snapAndMove. */
    public LegacyVec3 snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info) {
        int[] pos = this.findCore(world, x, y, z);
        if (pos == null) return LegacyVec3.of(trainX, trainY, trainZ);
        int cX = pos[0];
        int cY = pos[1];
        int cZ = pos[2];
        int meta = meta(world, cX, cY, cZ);

        double moveAngle = Math.atan2(motionX, motionZ) * 180D / Math.PI + 90;
        LegacyVec3 trainPos = LegacyVec3.of(trainX, trainY, trainZ);

        //convert nodes to a list of links with in-world positions
        LegacyVec3 train = LegacyVec3.of(trainX, trainY, trainZ);
        LegacyVec3 core = LegacyVec3.of(cX + 0.5, cY, cZ + 0.5);
        List<List<Link>> links = new ArrayList<>();

        for (RailDef def : railDefs) {
            List<Link> linkList = new ArrayList<>();
            links.add(linkList);
            for (int i = 0; i < def.nodes.size() - 1; i++) {
                LegacyVec3 vec1 = getPositionFromNode(core, def.nodes.get(i), meta);
                LegacyVec3 vec2 = getPositionFromNode(core, def.nodes.get(i + 1), meta);
                linkList.add(new Link(new LegacyVec3[] {vec1, vec2}, def));
            }
        }

        //find closest node
        Link closest = null;
        LegacyVec3 startingPos = null;
        /** closest chain of link definitions */
        List<Link> cDef = null;
        double angularDiff;
        double linkAngle;
        double dist = Double.MAX_VALUE;
        /** direction */
        boolean d = true;

        for (List<Link> chain : links) {
            for (Link link : chain) {
                LegacyVec3[] array = link.nodes;
                LegacyVec3 point = getClosestPointOnLink(array[0], array[1], train);
                if (point != null) {
                    LegacyVec3 delta = point.subtract(train);
                    double length = delta.lengthVector();
                    if (!canCross(world, cX, cY, cZ, trainPos, point, link.def)) continue;
                    linkAngle = RailCarEntity.generateYaw(array[1], array[0]);
                    angularDiff = LegacyVec3.angularDifference(linkAngle, -moveAngle);
                    if (angularDiff < -180) { angularDiff += 180; linkAngle += 180; d = false; }
                    if (angularDiff > 0) { angularDiff -= 180; linkAngle -= 180; d = false; }
                    if (length < dist) {
                        closest = link;
                        startingPos = point;
                        cDef = chain;
                        dist = length;
                    }
                }
            }
        }

        if (closest == null) {
            return LegacyVec3.of(trainX, trainY, trainZ);
        }

        double distRemaining = speed;
        boolean engaged = false;
        LegacyVec3 currentPos = startingPos;

        for (int i = d ? 0 : cDef.size() - 1; d ? (i < cDef.size()) : (i >= 0); i += d ? 1 : -1) {
            Link link = cDef.get(i);
            LegacyVec3[] array = link.nodes;
            if (!engaged) {
                if (link == closest) {
                    engaged = true;
                } else {
                    continue;
                }
            }
            LegacyVec3 nextNode = array[d ? 1 : 0];
            LegacyVec3 delta = nextNode.subtract(currentPos);
            if (!canCross(world, cX, cY, cZ, currentPos, nextNode, link.def)) break;
            double len = delta.lengthVector();
            if (len >= distRemaining) {
                info.overshoot = 0;
                double newYaw = RailCarEntity.generateYaw(nextNode, currentPos);
                if (Math.abs(LegacyVec3.angularDifference(newYaw, moveAngle)) < 45) info.yaw = (float) newYaw;
                else info.yaw = (float) moveAngle;
                delta.normalize(); // result discarded in the original
                return LegacyVec3.of(currentPos.xCoord - delta.xCoord * distRemaining / len, currentPos.yCoord - delta.yCoord * distRemaining / len, currentPos.zCoord - delta.zCoord * distRemaining / len);
            }
            distRemaining -= len;
            currentPos = nextNode;
        }

        info.overshoot = distRemaining;
        info.pos = IRailNTM.floorPos(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord);
        return currentPos;
    }

    public LegacyVec3 getClosestPointOnLink(LegacyVec3 pointA, LegacyVec3 pointB, LegacyVec3 pointP) {
        LegacyVec3 ap = LegacyVec3.of(pointP.xCoord - pointA.xCoord, 0, pointP.zCoord - pointA.zCoord);
        LegacyVec3 ab = LegacyVec3.of(pointB.xCoord - pointA.xCoord, 0, pointB.zCoord - pointA.zCoord);
        double magAB = ab.xCoord * ab.xCoord + ab.zCoord * ab.zCoord;
        double dotProd = ap.xCoord * ab.xCoord + ap.zCoord * ab.zCoord;
        double dist = dotProd / magAB;
        if (dist < 0) return pointA;
        if (dist > 1) return pointB;
        return LegacyVec3.of(pointA.xCoord + ab.xCoord * dist, pointA.yCoord + (pointB.yCoord - pointA.yCoord) * dist, pointA.zCoord + ab.zCoord * dist);
    }

    /** Creates the in-world position for a node based on the node itself and the core position */
    public LegacyVec3 getPositionFromNode(LegacyVec3 core, LegacyVec3 node, int meta) {
        float rotation = 0;
        if (meta == 12) rotation = 90F / 180F * (float) Math.PI;
        if (meta == 14) rotation = 180F / 180F * (float) Math.PI;
        if (meta == 13) rotation = 270F / 180F * (float) Math.PI;
        LegacyVec3 copy = node.copy();
        copy.rotateAroundY(rotation);
        return core.addVector(copy.xCoord, copy.yCoord, copy.zCoord);
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, Direction dir, int o) {
        if (!super.checkRequirement(world, x, y, z, dir, o)) return false;
        Direction rot = rotationUp(dir);
        dir = dir.getOpposite();
        int dX = dir.getStepX();
        int dZ = dir.getStepZ();
        int rX = rot.getStepX();
        int rZ = rot.getStepZ();
        // RailStandardSwitchFlipped keeps the unflipped checkRequirement (UP rotation, same cells)
        int s = 0;
        for (int i = 0; i < 4; i++) if (!replaceable(world, x + dX * (2 + i) + rX * (2 + s), y, z + dZ * (2 + i) + rZ * (2 + s))) return false;
        for (int i = 0; i < 2; i++) if (!replaceable(world, x + dX * (4 + i) + rX * (3 + s), y, z + dZ * (4 + i) + rZ * (3 + s))) return false;
        if (!replaceable(world, x + dX * 5 + rX * (4 + s), y, z + dZ * 5 + rZ * (4 + s))) return false;
        for (int j = 0; j < 2; j++) for (int i = 0; i < 2; i++) if (!replaceable(world, x + dX * (6 + j) + rX * (3 + i + s), y, z + dZ * (6 + j) + rZ * (3 + i + s))) return false;
        if (!replaceable(world, x + dX * 7 + rX * (5 + s), y, z + dZ * 7 + rZ * (5 + s))) return false;
        for (int j = 0; j < 7; j++) for (int i = 0; i < 2; i++) if (!replaceable(world, x + dX * (8 + j) + rX * (4 + i + s), y, z + dZ * (8 + j) + rZ * (4 + i + s))) return false;
        return true;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, Direction dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        safeRem = true;
        try {
            // RailStandardSwitchFlipped rotates DOWN and shifts the side track one block closer
            Direction rot = flipped ? rotationDown(dir) : rotationUp(dir);
            int s = flipped ? -1 : 0;
            dir = dir.getOpposite();
            int dX = dir.getStepX();
            int dZ = dir.getStepZ();
            int rX = rot.getStepX();
            int rZ = rot.getStepZ();
            int r = rot.get3DDataValue();
            int d = dir.get3DDataValue();
            for (int i = 0; i < 4; i++) setDummy(world, x + dX * (2 + i) + rX * (2 + s), y, z + dZ * (2 + i) + rZ * (2 + s), r);
            for (int i = 0; i < 2; i++) setDummy(world, x + dX * (4 + i) + rX * (3 + s), y, z + dZ * (4 + i) + rZ * (3 + s), r);
            setDummy(world, x + dX * 5 + rX * (4 + s), y, z + dZ * 5 + rZ * (4 + s), r);
            for (int j = 0; j < 2; j++) for (int i = 0; i < 2; i++) setDummy(world, x + dX * (6 + j) + rX * (3 + i + s), y, z + dZ * (6 + j) + rZ * (3 + i + s), d);
            setDummy(world, x + dX * 7 + rX * (5 + s), y, z + dZ * 7 + rZ * (5 + s), r);
            for (int j = 0; j < 7; j++) for (int i = 0; i < 2; i++) setDummy(world, x + dX * (8 + j) + rX * (4 + i + s), y, z + dZ * (8 + j) + rZ * (4 + i + s), d);
        } finally {
            safeRem = false;
        }
    }

    @Override
    protected BlockState coreState(int meta) {
        return super.coreState(meta).setValue(SWITCHED, false);
    }

    public static final class RailDef {
        final String name;
        public final List<LegacyVec3> nodes = new ArrayList<>();

        public RailDef(String name) {
            this.name = name;
        }
    }

    private record Link(LegacyVec3[] nodes, RailDef def) {
    }
}

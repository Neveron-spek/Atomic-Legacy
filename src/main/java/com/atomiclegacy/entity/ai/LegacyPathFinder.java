package com.atomiclegacy.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * [parity J1] 1.7.10 net.minecraft.pathfinding.PathFinder (createEntityPathTo / addToPath / findPathOptions) on
 * top of the 1.21.1 node evaluator, which only supplies passable nodes:
 * <ul>
 * <li>one target node at floor(x - width / 2), floor(y), floor(z - width / 2);</li>
 * <li>only the four horizontal neighbours (no diagonal moves);</li>
 * <li>a neighbour is accepted only while its distance to the target is below maxDist;</li>
 * <li>cost and heuristic are squared distances, no path-type penalties;</li>
 * <li>the result reaches the target or ends at the node closest to it; null if that is the start.</li>
 * </ul>
 * World.getPathEntityToEntity / getEntityPathToXYZ searched a ChunkCache of maxDist + 8 around the entity,
 * PathFinderUtils one of maxDist + 16 (regionPadding).
 */
public class LegacyPathFinder extends PathFinder {
    /** Safety bound only; the 1.7.10 search had none but stayed inside the maxDist sphere around the target. */
    private static final int MAX_NODES = 8192;

    private final NodeEvaluator evaluator;
    private final Node[] neighbors = new Node[32];
    private final BinaryHeap openSet = new BinaryHeap();

    public LegacyPathFinder(NodeEvaluator evaluator, int maxVisitedNodes) {
        super(evaluator, maxVisitedNodes);
        this.evaluator = evaluator;
    }

    /** Vanilla entry point (used by goals of new-AI mobs): the block centre of the single target. */
    @Override
    @Nullable
    public Path findPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targetPositions, float maxRange,
                         int accuracy, float searchDepthMultiplier) {
        if (targetPositions.isEmpty()) {
            return null;
        }
        BlockPos target = targetPositions.iterator().next();
        return find(region, mob, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, maxRange);
    }

    /** 1.7.10 PathFinder.createEntityPathTo(entity, x, y, z, maxDist). */
    @Nullable
    public Path find(PathNavigationRegion region, Mob mob, double x, double y, double z, float maxDist) {
        this.openSet.clear();
        this.evaluator.prepare(region, mob);
        try {
            Node start = this.evaluator.getStart();
            if (start == null) {
                return null;
            }
            float halfWidth = mob.getBbWidth() / 2.0F;
            Target end = this.evaluator.getTarget(Mth.floor(x - halfWidth), Mth.floor(y), Mth.floor(z - halfWidth));
            return addToPath(start, end, maxDist);
        } finally {
            this.evaluator.done();
        }
    }

    private Path addToPath(Node start, Node end, float maxDist) {
        start.g = 0.0F;
        start.h = start.distanceToSqr(end);
        start.f = start.h;
        this.openSet.insert(start);
        Node closest = start;
        int visited = 0;
        while (!this.openSet.isEmpty() && visited++ < MAX_NODES) {
            Node current = this.openSet.pop();
            if (sameNode(current, end)) {
                return createPath(current, end, true);
            }
            if (current.distanceToSqr(end) < closest.distanceToSqr(end)) {
                closest = current;
            }
            current.closed = true;
            int count = this.evaluator.getNeighbors(this.neighbors, current);
            for (int i = 0; i < count; i++) {
                Node option = this.neighbors[i];
                if (option.closed || (option.x != current.x && option.z != current.z)
                        || !(option.distanceTo(end) < maxDist)) {
                    continue;
                }
                float cost = current.g + current.distanceToSqr(option);
                if (!option.inOpenSet() || cost < option.g) {
                    option.cameFrom = current;
                    option.g = cost;
                    option.h = option.distanceToSqr(end);
                    if (option.inOpenSet()) {
                        this.openSet.changeCost(option, option.g + option.h);
                    } else {
                        option.f = option.g + option.h;
                        this.openSet.insert(option);
                    }
                }
            }
        }
        return closest == start ? null : createPath(closest, end, false);
    }

    private static boolean sameNode(Node a, Node b) {
        return a.x == b.x && a.y == b.y && a.z == b.z;
    }

    private static Path createPath(Node last, Node end, boolean reached) {
        List<Node> nodes = new ArrayList<>();
        for (Node node = last; node != null; node = node.cameFrom) {
            nodes.add(node);
        }
        Collections.reverse(nodes);
        return new Path(nodes, new BlockPos(end.x, end.y, end.z), reached);
    }

    /**
     * PathFinderUtils.getPathEntityToEntityPartial / getPathEntityToCoordPartial: the point maxDist blocks along the
     * direction to the destination, moved onto the first standable block within 10 blocks below (else above).
     */
    public static BlockPos partialTarget(Level level, Entity from, double toX, double toY, double toZ, float maxDist) {
        Vec3 vec = new Vec3(toX - from.getX(), toY - from.getY(), toZ - from.getZ()).normalize();
        int x = (int) Math.floor(from.getX() + vec.x * maxDist);
        int y = (int) Math.floor(from.getY() + vec.y * maxDist);
        int z = (int) Math.floor(from.getZ() + vec.z * maxDist);
        boolean solid = false;
        for (int i = y; i > y - 10; i--) {
            if (standable(level, x, i, z)) {
                solid = true;
                y = i;
                break;
            }
        }
        if (!solid) {
            for (int i = y + 10; i > y; i--) {
                if (standable(level, x, i, z)) {
                    y = i;
                    break;
                }
            }
        }
        return new BlockPos(x, y, z);
    }

    /** !getBlock(x, i, z).getMaterial().blocksMovement() && getBlock(x, i - 1, z).isNormalCube() */
    private static boolean standable(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        return !state.blocksMotion() && below.isRedstoneConductor(level, pos.below());
    }

    /** The water flag of the 1.7.10 PathFinder ("isPathingInWater", which in fact blocked water nodes). */
    static void withWaterBlocked(Mob mob, boolean blocked, Runnable search) {
        if (!blocked) {
            search.run();
            return;
        }
        float water = mob.getPathfindingMalus(PathType.WATER);
        float border = mob.getPathfindingMalus(PathType.WATER_BORDER);
        mob.setPathfindingMalus(PathType.WATER, -1.0F);
        mob.setPathfindingMalus(PathType.WATER_BORDER, -1.0F);
        try {
            search.run();
        } finally {
            mob.setPathfindingMalus(PathType.WATER, water);
            mob.setPathfindingMalus(PathType.WATER_BORDER, border);
        }
    }
}

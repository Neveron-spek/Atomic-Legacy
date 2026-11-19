package com.atomiclegacy.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * [parity J1] Ground navigation whose searches use the 1.7.10 path finder ({@link LegacyPathFinder}).
 * <p>New-AI mobs ({@code manualFollow == false}) keep the vanilla path following, driven into
 * {@link LegacyMoveControl} (EntityMoveHelper). Old-AI mobs ({@code manualFollow == true}, EntityCreature without
 * isAIEnabled) only store the path here; {@link LegacyCreatureAi} follows it the 1.7.10 way.</p>
 */
public class LegacyPathNavigation extends GroundPathNavigation {
    private final boolean manualFollow;
    private LegacyPathFinder legacyFinder;

    public LegacyPathNavigation(Mob mob, Level level, boolean manualFollow) {
        super(mob, level);
        this.manualFollow = manualFollow;
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.legacyFinder = new LegacyPathFinder(this.nodeEvaluator, maxVisitedNodes);
        return this.legacyFinder;
    }

    /** 1.7.10 PathNavigate.getPathToEntityLiving: target.posX, boundingBox.minY, posZ with the follow range. */
    @Override
    @Nullable
    public Path createPath(Entity entity, int accuracy) {
        return search(entity.getX(), entity.getBoundingBox().minY, entity.getZ(), followRange(), 8, false);
    }

    @Override
    @Nullable
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
        if (targets.isEmpty()) {
            return null;
        }
        BlockPos target = targets.iterator().next();
        // World.getEntityPathToXYZ(int x, int y, int z): createEntityPathTo(x + 0.5, y + 0.5, z + 0.5)
        return search(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, followRange, 8, false);
    }

    /** World.getPathEntityToEntity(entity, target, maxDist, ...): ChunkCache maxDist + 8. */
    @Nullable
    public Path pathToEntity(Entity target, float maxDist, boolean avoidWater) {
        return search(target.getX(), target.getBoundingBox().minY, target.getZ(), maxDist, 8, avoidWater);
    }

    /** World.getEntityPathToXYZ(entity, x, y, z, maxDist, ...). */
    @Nullable
    public Path pathToXYZ(int x, int y, int z, float maxDist, boolean avoidWater) {
        return search(x + 0.5D, y + 0.5D, z + 0.5D, maxDist, 8, avoidWater);
    }

    /** PathFinderUtils.getPathEntityToEntityPartial (ChunkCache maxDist + 16). */
    @Nullable
    public Path partialPathToEntity(Entity target, float maxDist, boolean avoidWater) {
        BlockPos end = LegacyPathFinder.partialTarget(this.level, this.mob, target.getX(), target.getY(), target.getZ(), maxDist);
        return search(end.getX() + 0.5D, end.getY() + 0.5D, end.getZ() + 0.5D, maxDist, 16, avoidWater);
    }

    /** PathFinderUtils.getPathEntityToCoordPartial (ChunkCache maxDist + 16). */
    @Nullable
    public Path partialPathToCoord(int x, int y, int z, float maxDist, boolean avoidWater) {
        BlockPos end = LegacyPathFinder.partialTarget(this.level, this.mob, x, y, z, maxDist);
        return search(end.getX() + 0.5D, end.getY() + 0.5D, end.getZ() + 0.5D, maxDist, 16, avoidWater);
    }

    private float followRange() {
        return (float) this.mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
    }

    @Nullable
    private Path search(double x, double y, double z, float maxDist, int padding, boolean avoidWater) {
        if (this.mob.getY() < this.level.getMinBuildHeight() || maxDist <= 0.0F) {
            return null;
        }
        Path[] result = new Path[1];
        this.level.getProfiler().push("pathfind");
        BlockPos center = this.mob.blockPosition();
        int radius = (int) (maxDist + padding);
        PathNavigationRegion region = new PathNavigationRegion(this.level, center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius));
        LegacyPathFinder.withWaterBlocked(this.mob, avoidWater,
                () -> result[0] = this.legacyFinder.find(region, this.mob, x, y, z, maxDist));
        this.level.getProfiler().pop();
        return result[0];
    }

    /** EntityCreature.setPathToEntity: replace (or clear) the current path. */
    public void setLegacyPath(@Nullable Path path) {
        this.path = path;
    }

    public boolean isManualFollow() {
        return this.manualFollow;
    }

    @Override
    public void tick() {
        if (this.manualFollow) {
            this.tick++;
            return;
        }
        super.tick();
    }
}

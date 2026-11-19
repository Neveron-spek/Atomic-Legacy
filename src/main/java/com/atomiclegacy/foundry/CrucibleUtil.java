package com.atomiclegacy.foundry;

import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Port of com.hbm.util.CrucibleUtil (1.7.10). The 1.7.10 pour is a straight-down
 * hitscan (World#func_147447_a, stopOnLiquid): the first block whose collision box
 * or liquid the stream meets is the target, and only an ICrucibleAcceptor there
 * can take the material. [parity Q1]
 */
public final class CrucibleUtil {
    private CrucibleUtil() {
    }

    /**
     * CrucibleUtil#pourFullStack. Returns the stack that was poured (or spilled), or null.
     * Like the original, the first stack the acceptor agrees to take ends the attempt.
     */
    @Nullable
    public static FoundryMaterialStack pourFullStack(
            Level level,
            double x,
            double y,
            double z,
            double range,
            boolean safe,
            List<FoundryMaterialStack> stacks,
            int quanta,
            Predicate<FoundryMaterial> materialFilter
    ) {
        if (stacks.isEmpty() || quanta <= 0) {
            return null;
        }
        PourTarget target = findPouringTarget(level, x, y, z, range);
        if (target == null || target.acceptor() == null) {
            return spill(safe, stacks, quanta, materialFilter);
        }
        for (int index = 0; index < stacks.size(); index++) {
            FoundryMaterialStack stack = stacks.get(index);
            if (!materialFilter.test(stack.material())) {
                continue;
            }
            // tryPourStack: anything that is not SMELTABLE never pours
            if (stack.material().behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE) {
                continue;
            }
            int amountToPour = Math.min(stack.amount(), quanta);
            FoundryMaterialStack toPour = new FoundryMaterialStack(stack.material(), amountToPour);
            Vec3 hit = target.hit();
            if (!target.acceptor().canAcceptPartialPour(level, target.pos(), hit.x, hit.y, hit.z, target.side(), toPour)) {
                continue;
            }
            FoundryMaterialStack left = target.acceptor().pour(level, target.pos(), hit.x, hit.y, hit.z, target.side(), toPour);
            int leftover = left == null ? 0 : left.amount();
            int transferred = Math.max(0, amountToPour - leftover);
            int remaining = stack.amount() - transferred;
            if (remaining <= 0) {
                stacks.remove(index);
            } else {
                stacks.set(index, new FoundryMaterialStack(stack.material(), remaining));
            }
            return new FoundryMaterialStack(stack.material(), transferred);
        }
        return spill(safe, stacks, quanta, materialFilter);
    }

    /**
     * Vertical hitscan from (x, y, z) down to y - range. Returns the first block the
     * stream hits (its acceptor may be null), or null when nothing is hit.
     */
    @Nullable
    public static PourTarget findPouringTarget(Level level, double x, double y, double z, double range) {
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = new Vec3(x, y - range, z);
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int top = (int) Math.floor(y);
        int bottom = (int) Math.floor(y - range);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int blockY = top; blockY >= bottom; blockY--) {
            cursor.set(blockX, blockY, blockZ);
            BlockState state = level.getBlockState(cursor);
            BlockPos pos = cursor.immutable();
            if (!state.getFluidState().isEmpty()) {
                return new PourTarget(pos, Direction.UP, new Vec3(x, Math.min(y, blockY + 1.0D), z), null);
            }
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            BlockHitResult hit = shape.clip(start, end, pos);
            if (hit == null) {
                continue;
            }
            // A 1.7.10 ray that starts inside a bounding box leaves through its bottom face.
            Direction side = hit.isInside() ? Direction.DOWN : hit.getDirection();
            return new PourTarget(pos, side, hit.getLocation(), acceptorAt(level, pos));
        }
        return null;
    }

    @Nullable
    public static CrucibleAcceptor acceptorAt(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrucibleAcceptor acceptor) {
            return acceptor;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof CrucibleAcceptor acceptor) {
            return acceptor;
        }
        return null;
    }

    @Nullable
    private static FoundryMaterialStack spill(boolean safe, List<FoundryMaterialStack> stacks, int quanta, Predicate<FoundryMaterial> materialFilter) {
        if (safe) {
            return null;
        }
        for (int index = 0; index < stacks.size(); index++) {
            FoundryMaterialStack stack = stacks.get(index);
            if (!materialFilter.test(stack.material())) {
                continue;
            }
            int amount = Math.min(stack.amount(), quanta);
            int remaining = stack.amount() - amount;
            if (remaining <= 0) {
                stacks.remove(index);
            } else {
                stacks.set(index, new FoundryMaterialStack(stack.material(), remaining));
            }
            return new FoundryMaterialStack(stack.material(), amount);
        }
        return null;
    }

    public record PourTarget(BlockPos pos, Direction side, Vec3 hit, @Nullable CrucibleAcceptor acceptor) {
    }
}

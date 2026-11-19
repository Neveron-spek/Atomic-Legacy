package com.atomiclegacy.blocks.rail;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * [parity S1: trains] The parts of com.hbm.blocks.BlockDummyable (and MultiblockHandlerXR) used by the rail blocks.
 * The 1.7.10 metadata is kept verbatim in {@link #META}: 0-5 dummy rotation pointing away from the core, 6-11 extra,
 * 12-15 core rotation (ForgeDirection ordinal + 10). Direction 3D data values equal the ForgeDirection ordinals.
 */
public abstract class RailDummyableBlock extends Block implements IRailNTM {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);
    /** meta offset from dummy to TE rotation */
    public static final int offset = 10;
    /** meta offset from dummy to extra rotation */
    public static final int extra = 6;
    public static boolean safeRem = false;

    protected static final VoxelShape RAIL_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);

    /** BlockDummyable.bounding (detailed hitbox), in core-relative coordinates for a NORTH core. */
    public final List<AABB> bounding = new ArrayList<>();

    protected RailDummyableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    /** UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT */
    public abstract int[] getDimensions();

    public abstract int getOffset();

    public int getHeightOffset() {
        return 0;
    }

    public static int meta(BlockGetter world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        return state.hasProperty(META) ? state.getValue(META) : 0;
    }

    /** ForgeDirection.getOrientation: UNKNOWN (null here) outside 0..5. */
    @Nullable
    public static Direction orientation(int id) {
        return id >= 0 && id < 6 ? Direction.from3DDataValue(id) : null;
    }

    protected static int dx(@Nullable Direction d) { return d == null ? 0 : d.getStepX(); }
    protected static int dy(@Nullable Direction d) { return d == null ? 0 : d.getStepY(); }
    protected static int dz(@Nullable Direction d) { return d == null ? 0 : d.getStepZ(); }

    /** ForgeDirection.getRotation(UP) for horizontal directions. */
    protected static Direction rotationUp(Direction dir) {
        return dir.getClockWise();
    }

    /** ForgeDirection.getRotation(DOWN) for horizontal directions. */
    protected static Direction rotationDown(Direction dir) {
        return dir.getCounterClockWise();
    }

    @Nullable
    public int[] findCore(BlockGetter world, int x, int y, int z) {
        List<BlockPos> positions = new ArrayList<>();
        return findCoreRec(world, x, y, z, positions);
    }

    @Nullable
    private int[] findCoreRec(BlockGetter world, int x, int y, int z, List<BlockPos> positions) {
        BlockPos pos = new BlockPos(x, y, z);
        int metadata = meta(world, x, y, z);
        // if it's an extra, remove the extra-ness
        if (metadata >= extra) metadata -= extra;
        // if the block matches and the orientation is "UNKNOWN", it's the core
        if (world.getBlockState(pos).getBlock() == this && orientation(metadata) == null) return new int[] {x, y, z};
        if (positions.contains(pos)) return null;
        Direction dir = orientation(metadata);
        int ox = -dx(dir), oy = -dy(dir), oz = -dz(dir);
        if (world.getBlockState(new BlockPos(x + ox, y + oy, z + oz)).getBlock() != this) return null;
        positions.add(pos);
        return findCoreRec(world, x + ox, y + oy, z + oz, positions);
    }

    /// PLACEMENT (BlockDummyable.onBlockPlacedBy) ///

    @Override
    public void setPlacedBy(Level world, BlockPos placed, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!(placer instanceof Player pl)) return;
        int x = placed.getX();
        int y = placed.getY();
        int z = placed.getZ();

        safeRem = true;
        world.setBlock(placed, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        safeRem = false;

        int i = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        int o = -getOffset();
        y += getHeightOffset();

        Direction dir = Direction.NORTH;
        if (i == 0) dir = Direction.from3DDataValue(2);
        if (i == 1) dir = Direction.from3DDataValue(5);
        if (i == 2) dir = Direction.from3DDataValue(3);
        if (i == 3) dir = Direction.from3DDataValue(4);

        if (!checkRequirement(world, x, y, z, dir, o)) {
            if (!pl.getAbilities().instabuild) {
                ItemStack stack = pl.getInventory().getSelected();
                if (stack.isEmpty()) {
                    pl.getInventory().setItem(pl.getInventory().selected, new ItemStack(this));
                } else if (stack.getItem() != this.asItem() || stack.getCount() == stack.getMaxStackSize()) {
                    pl.getInventory().add(new ItemStack(this));
                } else {
                    stack.grow(1);
                }
            }
            return;
        }

        if (!world.isClientSide) {
            int cx = x + dir.getStepX() * o;
            int cy = y + dir.getStepY() * o;
            int cz = z + dir.getStepZ() * o;
            int meta = getMetaForCore(world, cx, cy, cz, pl, dir.get3DDataValue() + offset);
            world.setBlock(new BlockPos(cx, cy, cz), coreState(meta), 3);
            fillSpace(world, x, y, z, dir, o);
        }
    }

    protected BlockState coreState(int meta) {
        return defaultBlockState().setValue(META, meta);
    }

    protected int getMetaForCore(Level world, int x, int y, int z, Player player, int original) {
        return original;
    }

    protected boolean checkRequirement(Level world, int x, int y, int z, Direction dir, int o) {
        return checkSpace(world, x + dir.getStepX() * o, y + dir.getStepY() * o, z + dir.getStepZ() * o, getDimensions(), x, y, z, dir);
    }

    protected void fillSpace(Level world, int x, int y, int z, Direction dir, int o) {
        fillSpace(world, x + dir.getStepX() * o, y + dir.getStepY() * o, z + dir.getStepZ() * o, getDimensions(), this, dir);
    }

    protected static boolean replaceable(Level world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).canBeReplaced();
    }

    protected void setDummy(Level world, int x, int y, int z, int meta) {
        world.setBlock(new BlockPos(x, y, z), defaultBlockState().setValue(META, meta), 3);
    }

    /// MultiblockHandlerXR ///

    public static boolean checkSpace(Level world, int x, int y, int z, int[] dim, int ox, int oy, int oz, Direction dir) {
        if (dim == null || dim.length != 6) return false;
        int count = 0;
        int[] rot = rotate(dim, dir);
        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {
                    //if the position matches the just placed block, the space counts as unoccupied
                    if (a == ox && b == oy && c == oz) continue;
                    if (!replaceable(world, a, b, c)) return false;
                    count++;
                    if (count > 2000) return false;
                }
            }
        }
        return true;
    }

    public static void fillSpace(Level world, int x, int y, int z, int[] dim, RailDummyableBlock block, Direction dir) {
        if (dim == null || dim.length != 6) return;
        int count = 0;
        int[] rot = rotate(dim, dir);
        safeRem = true;
        try {
            for (int a = x - rot[4]; a <= x + rot[5]; a++) {
                for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                    for (int c = z - rot[2]; c <= z + rot[3]; c++) {
                        int meta;
                        if (b < y) meta = Direction.DOWN.get3DDataValue();
                        else if (b > y) meta = Direction.UP.get3DDataValue();
                        else if (a < x) meta = Direction.WEST.get3DDataValue();
                        else if (a > x) meta = Direction.EAST.get3DDataValue();
                        else if (c < z) meta = Direction.NORTH.get3DDataValue();
                        else if (c > z) meta = Direction.SOUTH.get3DDataValue();
                        else continue;
                        block.setDummy(world, a, b, c, meta);
                        count++;
                        if (count > 2000) return;
                    }
                }
            }
        } finally {
            safeRem = false;
        }
    }

    public static int[] rotate(int[] dim, Direction dir) {
        if (dim == null) return null;
        if (dir == Direction.SOUTH) return dim;
        //                                         U       D       N       S       W       E
        if (dir == Direction.NORTH) return new int[] {dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
        if (dir == Direction.EAST) return new int[] {dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
        if (dir == Direction.WEST) return new int[] {dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
        return dim;
    }

    /// REMOVAL (breakBlock / onNeighborBlockChange / onBlockHarvested) ///

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!world.isClientSide && !state.is(newState.getBlock())) {
            int i = state.getValue(META);
            if (i < 12 && !safeRem) {
                if (i >= extra) i -= extra;
                Direction d = orientation(i);
                BlockPos toward = pos.offset(-dx(d), -dy(d), -dz(d));
                if (world.getBlockState(toward).getBlock() == this) world.removeBlock(toward, false);
            }
        }
        super.onRemove(state, world, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, world, pos, block, fromPos, movedByPiston);
        if (safeRem) return;
        destroyIfOrphan(world, pos);
    }

    private void destroyIfOrphan(Level world, BlockPos pos) {
        if (world.isClientSide) return;
        if (world.getBlockState(pos).getBlock() != this) return;
        int metadata = meta(world, pos.getX(), pos.getY(), pos.getZ());
        // if it's an extra, remove the extra-ness
        if (metadata >= extra) metadata -= extra;
        Direction d = orientation(metadata);
        BlockPos toward = pos.offset(-dx(d), -dy(d), -dz(d));
        Block b = world.getBlockState(toward).getBlock();
        if (b != this && world.hasChunksAt(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            world.removeBlock(pos, false);
        }
    }

    /** BlockDummyable.onBlockHarvested: one item for the broken part, outside creative mode. */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && !player.getAbilities().instabuild) {
            popResource(world, pos, new ItemStack(this));
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    /** BlockDummyable.harvestBlock: only the statistic and the exhaustion, the drop already happened. */
    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    /// SHAPES ///

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (bounding.isEmpty()) return RAIL_SHAPE;
        VoxelShape detailed = detailedShape(world, pos);
        return detailed == null ? RAIL_SHAPE : detailed;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (bounding.isEmpty()) return RAIL_SHAPE;
        VoxelShape detailed = detailedShape(world, pos);
        return detailed == null ? Shapes.empty() : detailed;
    }

    /** BlockDummyable.addCollisionBoxesToList / collisionRayTrace with getAABBRotationOffset. */
    @Nullable
    private VoxelShape detailedShape(BlockGetter world, BlockPos pos) {
        int[] core = findCore(world, pos.getX(), pos.getY(), pos.getZ());
        if (core == null) return null;
        Direction rot = orientation(meta(world, core[0], core[1], core[2]) - offset);
        rot = rot == null || rot.getAxis() == Direction.Axis.Y ? null : rotationUp(rot);
        VoxelShape shape = Shapes.empty();
        for (AABB aabb : bounding) {
            AABB boxlet = rotationOffset(aabb, core[0] + 0.5D - pos.getX(), core[1] - pos.getY(), core[2] + 0.5D - pos.getZ(), rot);
            shape = Shapes.or(shape, Shapes.create(boxlet));
        }
        return shape;
    }

    public static AABB rotationOffset(AABB aabb, double x, double y, double z, @Nullable Direction dir) {
        AABB newBox = null;
        if (dir == Direction.NORTH) newBox = new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        if (dir == Direction.EAST) newBox = new AABB(-aabb.maxZ, aabb.minY, aabb.minX, -aabb.minZ, aabb.maxY, aabb.maxX);
        if (dir == Direction.SOUTH) newBox = new AABB(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
        if (dir == Direction.WEST) newBox = new AABB(aabb.minZ, aabb.minY, -aabb.maxX, aabb.maxZ, aabb.maxY, -aabb.minX);
        if (newBox != null) return newBox.move(x, y, z);
        return aabb.move(x + 0.5D, y + 0.5D, z + 0.5D);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

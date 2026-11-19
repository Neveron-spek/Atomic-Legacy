package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidRelease;
import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.fluid.HbmFluidTrait;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class DrainBlockEntity extends BlockEntity implements FluidCopiable, com.atomiclegacy.fluid.HbmFluidNetUser { // [parity V1: fluid net]
    private static final int CAPACITY = 2_000;
    private static final double LEGACY_DRAIN_PARTICLE_OFFSET = 2.5D;
    private final HbmFluidTank tank = new HbmFluidTank(CAPACITY);

    public DrainBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DRAIN.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DrainBlockEntity drain) {
        if (level.isClientSide) {
            drain.emitDischargeParticles(level, pos, state);
            return;
        }
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, drain); // [parity V1: fluid net] setupFluidPorts + updatePortFIFO
        if (drain.tank.amount() <= 0) {
            return;
        }
        HbmFluidDefinition fluid = drain.tank.type();
        if (fluid.hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 10.0F, true, Level.ExplosionInteraction.BLOCK);
            return;
        }
        int toSpill = Math.max(drain.tank.amount() / 2, 1);
        HbmFluidStack spilled = drain.tank.drain(fluid, toSpill, false);
        if (spilled.isEmpty()) {
            return;
        }
        // [parity Q4: TileEntityMachineDrain] FluidTrait.onRelease for every fluid; the original has no mustard gas blocks.
        HbmFluidRelease.onRelease(level, pos, fluid, HbmPollution.ReleaseType.SPILL, spilled.amount());
        drain.tryPlaceOilSpill(level, state, spilled.amount(), fluid);
        drain.sync();
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public void setType(HbmFluidDefinition type) {
        this.tank.setType(type);
        sync();
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new DrainFluidHandler(side);
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.tank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        setType(fluid);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("t", this.tank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.load(tag.getCompound("t"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void emitDischargeParticles(Level level, BlockPos pos, BlockState state) {
        if (this.tank.amount() <= 0) {
            return;
        }
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
        HbmFluidDefinition fluid = this.tank.type();
        int color = fluid.color();
        double red = ((color >> 16) & 0xFF) / 255.0D;
        double green = ((color >> 8) & 0xFF) / 255.0D;
        double blue = (color & 0xFF) / 255.0D;
        double x = pos.getX() + 0.5D - facing.getStepX() * LEGACY_DRAIN_PARTICLE_OFFSET;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D - facing.getStepZ() * LEGACY_DRAIN_PARTICLE_OFFSET;
        // [parity Q4] gaseous -> "tower", otherwise "splash"; only within 100 blocks of the client player.
        if (!com.atomiclegacy.client.ParityQ4ClientParticles.playerWithin(pos.getX(), pos.getY(), pos.getZ(), 100.0D)) {
            return;
        }
        SimpleParticleType particle = !fluid.hasTrait(HbmFluidTrait.GASEOUS)
                ? HbmParticleTypes.DRAIN_SPLASH.get()
                : HbmParticleTypes.DRAIN_TOWER.get();
        level.addParticle(particle, x, y, z, red, green, blue);
    }

    private void tryPlaceOilSpill(Level level, BlockState state, int amount, HbmFluidDefinition fluid) {
        if (amount < 100
                || level.random.nextInt(20) != 0
                || !fluid.hasTrait(HbmFluidTrait.LIQUID)
                || !fluid.hasTrait(HbmFluidTrait.VISCOUS)
                || !fluid.hasTrait(HbmFluidTrait.FLAMMABLE)) {
            return;
        }
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Vec3 start = Vec3.atCenterOf(this.worldPosition).subtract(facing.getStepX() * 3.0D, 0.0D, facing.getStepZ() * 3.0D); // [parity Q4: - dir * 3]
        Vec3 end = start.add(level.random.nextGaussian() * 5.0D, -25.0D, level.random.nextGaussian() * 5.0D);
        BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(
                start,
                end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                (Entity) null
        ));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            return;
        }
        BlockPos target = hit.getBlockPos().above();
        BlockState targetState = level.getBlockState(target);
        if (targetState.getFluidState().isEmpty()
                && targetState.canBeReplaced()
                && HbmBlocks.OIL_SPILL.get().defaultBlockState().canSurvive(level, target)) {
            level.setBlock(target, HbmBlocks.OIL_SPILL.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private final class DrainFluidHandler implements IFluidHandler {
        @Nullable
        private final Direction side;

        private DrainFluidHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return DrainBlockEntity.this.tank.getFluidInTank(tankIndex);
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 ? CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tankIndex != 0 || !canReceiveFromSide() || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && DrainBlockEntity.this.tank.type() == fluid; // [parity Q4: NONE tank receives nothing]
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !canReceiveFromSide()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != DrainBlockEntity.this.tank.type()) return 0; // [parity Q4: only the configured type]
            int accepted = DrainBlockEntity.this.tank.fill(fluid, resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                DrainBlockEntity.this.sync();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }

        private boolean canReceiveFromSide() {
            if (side == null || level == null) {
                return true;
            }
            Direction facing = getBlockState().hasProperty(LargeMachineBlock.FACING)
                    ? getBlockState().getValue(LargeMachineBlock.FACING)
                    : Direction.SOUTH;
            Direction right = facing.getClockWise();
            Direction left = right.getOpposite();
            return side == facing || side == right || side == left;
        }
    }

    // [parity V1: fluid net] TileEntityMachineDrain as IFluidStandardReceiverMK2: one port at the core with the
    // connections dir0, dir0.getRotation(UP), dir0.getRotation(DOWN); getFluidPriority LOW
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{this.tank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        Direction dir0 = com.atomiclegacy.fluid.HbmPortDefs.dir(this);
        return com.atomiclegacy.fluid.HbmPortDefs.create().make(this.worldPosition, dir0, dir0.getClockWise(), dir0.getCounterClockWise()).build();
    }
    @Override public com.atomiclegacy.power.PowerEndpoint.ConnectionPriority fluidPriority() { return com.atomiclegacy.power.PowerEndpoint.ConnectionPriority.LOW; }
}

package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.EnergyConverterBlock;
import com.atomiclegacy.capability.NativeEnergyStorage;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * [parity L1: converters] 1.7.10 TileEntityConverterHeRf / TileEntityConverterRfHe.
 * HE buffer 5,000,000; RF (Forge Energy on 1.21) buffer EnergyStorage(1M, 1M, 1M).
 * Rates come from the energyConverters config (HEToRFConverter / RFToHEConverter keys).
 */
public final class EnergyConverterBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final long MAX_POWER = 5_000_000L;
    public static final int RF_CAPACITY = 1_000_000;
    public static final int RF_MAX_TRANSFER = 1_000_000;

    private long power;
    private int rf;
    private final IEnergyStorage storage = new RfView();

    public EnergyConverterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ENERGY_CONVERTER.get(), pos, state);
    }

    public boolean heToRf() {
        return getBlockState().getBlock() instanceof EnergyConverterBlock block && block.heToRf();
    }

    public long power() {
        return power;
    }

    public int rf() {
        return rf;
    }

    public IEnergyStorage energyStorage() {
        return storage;
    }

    public void serverTick(Level level) {
        PowerNetworkManager.tickFromEndpoint(level, this);
        long oldPower = this.power;
        int oldRf = this.rf;
        if (heToRf()) {
            long heInput = HbmConfig.HE_TO_FE_HE_USED.get();
            long rfOutput = HbmConfig.HE_TO_FE_FE_CREATED.get();
            double inputDecay = HbmConfig.HE_TO_FE_INPUT_DECAY.get();
            long rfCreated = Math.min(RF_CAPACITY - this.rf, this.power / heInput * rfOutput);
            this.power -= rfCreated * heInput / rfOutput;
            this.rf = (int) (this.rf + rfCreated);
            if (this.power > 0) this.power *= (1D - inputDecay);
            for (Direction dir : Direction.values()) {
                BlockPos loc = this.worldPosition.relative(dir);
                IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, loc, dir.getOpposite());
                // 1.7.10 HBM machines are not RF receivers: skip the HE<->FE bridge exposed by HBM blocks.
                if (receiver == null || receiver instanceof NativeEnergyStorage || !receiver.canReceive()) continue;
                int maxAvailable = Math.min(RF_MAX_TRANSFER, this.rf);
                int transferred = receiver.receiveEnergy(maxAvailable, false);
                this.rf -= Math.max(0, Math.min(transferred, maxAvailable));
            }
        } else {
            long rfInput = HbmConfig.FE_TO_HE_FE_USED.get();
            long heOutput = HbmConfig.FE_TO_HE_HE_CREATED.get();
            double inputDecay = HbmConfig.FE_TO_HE_INPUT_DECAY.get();
            long rfCreated = Math.min(this.rf, (MAX_POWER - this.power) * rfInput / heOutput);
            this.rf = (int) (this.rf - rfCreated);
            this.power += rfCreated * heOutput / rfInput;
            if (this.rf > 0) this.rf -= Math.min(this.rf, (int) Math.ceil(this.rf * inputDecay));
        }
        if (oldPower != this.power || oldRf != this.rf) {
            setChanged();
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return heToRf() ? 0L : this.power; // providerSpeed = maxPower
    }

    @Override
    public long getRequestedInput() {
        return heToRf() ? MAX_POWER - this.power : 0L; // receiverSpeed = maxPower
    }

    @Override
    public ConnectionPriority getPowerPriority() {
        return ConnectionPriority.LOW; // TileEntityConverterHeRf.getPriority(); RfHe is provider only
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.max(0L, Math.min(MAX_POWER, this.power - usedOutput + receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.power + " / " + MAX_POWER + " HE");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", this.power);
        tag.putInt("Energy", this.rf); // CoFH EnergyStorage.writeToNBT key
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = tag.getLong("power");
        this.rf = Math.max(0, Math.min(RF_CAPACITY, tag.getInt("Energy")));
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

    /** FE side: HE->RF only extracts, RF->HE only receives (EnergyStorage 1M/1M/1M). */
    private final class RfView implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (heToRf()) return 0;
            int received = Math.max(0, Math.min(RF_CAPACITY - rf, Math.min(RF_MAX_TRANSFER, maxReceive)));
            if (!simulate && received > 0) {
                rf += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!heToRf()) return 0;
            int extracted = Math.max(0, Math.min(rf, Math.min(RF_MAX_TRANSFER, maxExtract)));
            if (!simulate && extracted > 0) {
                rf -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override public int getEnergyStored() { return rf; }
        @Override public int getMaxEnergyStored() { return RF_CAPACITY; }
        @Override public boolean canExtract() { return heToRf(); }
        @Override public boolean canReceive() { return !heToRf(); }
    }
}

package com.atomiclegacy.tileentity;

import com.atomiclegacy.blocks.StirlingGeneratorBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.CogEntity;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class StirlingGeneratorBlockEntity extends BlockEntity implements PowerEndpoint {
    private long powerBuffer;
    private long lastOutput;
    private long visualOutput;
    private int heat;
    private int warnCooldown;
    private int overspeed;
    private boolean hasCog = true;
    private float spin;
    private float lastSpin;

    public StirlingGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.STIRLING_GENERATOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StirlingGeneratorBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.clientSpin(state);
            return;
        }
        blockEntity.generateFromNearbyHeat(level, state);
        PowerNetworkManager.tickFromEndpoint(level, blockEntity);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return switch (stirlingKind()) {
            case NORMAL -> normalPowerConnectorPositions();
            case STEEL -> steelPowerConnectorPositions();
            case CREATIVE -> creativePowerConnectorPositions();
        };
    }

    private List<BlockPos> normalPowerConnectorPositions() {
        return List.of(
                this.worldPosition.east(2).immutable(),
                this.worldPosition.west(2).immutable(),
                this.worldPosition.south(2).immutable(),
                this.worldPosition.north(2).immutable()
        );
    }

    private List<BlockPos> steelPowerConnectorPositions() {
        return List.of(
                this.worldPosition.east(2).immutable(),
                this.worldPosition.west(2).immutable(),
                this.worldPosition.south(2).immutable(),
                this.worldPosition.north(2).immutable()
        );
    }

    private List<BlockPos> creativePowerConnectorPositions() {
        return List.of(
                this.worldPosition.east(2).immutable(),
                this.worldPosition.west(2).immutable(),
                this.worldPosition.south(2).immutable(),
                this.worldPosition.north(2).immutable()
        );
    }

    private StirlingGeneratorBlock.Kind stirlingKind() {
        return this.getBlockState().getBlock() instanceof StirlingGeneratorBlock block
                ? block.kind()
                : StirlingGeneratorBlock.Kind.NORMAL;
    }

    @Override
    public long getAvailableOutput() {
        return this.hasCog ? this.powerBuffer : 0L;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastOutput = usedOutput;
        setChanged();
        if (this.level != null && !this.level.isClientSide && this.level.getGameTime() % 10L == 0L) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.atomiclegacy.power.stirling",
                this.lastOutput,
                this.heat,
                Component.translatable(this.hasCog
                        ? "message.atomiclegacy.stirling.gear_ok"
                        : "message.atomiclegacy.stirling.gear_missing")
        );
    }

    public float getSpin(float partialTick) {
        return this.lastSpin + (this.spin - this.lastSpin) * partialTick;
    }

    public boolean hasCog() {
        return this.hasCog;
    }

    public void setHasCog(boolean hasCog) {
        this.hasCog = hasCog;
        if (!hasCog) {
            this.overspeed = 0;
            this.warnCooldown = 0;
        }
        setChanged();
    }

    public int heat() {
        return this.heat;
    }

    public long visualOutput() {
        return Math.max(this.lastOutput, this.visualOutput);
    }

    /**
     * Output generated from the heat pulled during the current server tick.
     * The renderer uses visualOutput() to hide asynchronous network jitter;
     * the look overlay must report the actual generated HE/t value instead.
     */
    public long currentOutput() {
        return this.hasCog ? this.visualOutput : 0L;
    }

    public int maxHeat() {
        return maxHeat(this.getBlockState());
    }

    public boolean isCreative() {
        return this.getBlockState().is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get());
    }

    public int getGearMeta() {
        if (this.level == null) {
            return 0;
        }
        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get())) {
            return 2;
        }
        if (state.is(HbmBlocks.MACHINE_STIRLING_STEEL.get())) {
            return 1;
        }
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("PowerBuffer", this.powerBuffer);
        tag.putLong("LastOutput", this.lastOutput);
        tag.putLong("VisualOutput", this.visualOutput);
        tag.putInt("Heat", this.heat);
        tag.putInt("WarnCooldown", this.warnCooldown);
        tag.putInt("Overspeed", this.overspeed);
        tag.putBoolean("HasCog", this.hasCog);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.powerBuffer = tag.contains("PowerBuffer") ? tag.getLong("PowerBuffer") : tag.getLong("EnergyStored");
        this.lastOutput = tag.getLong("LastOutput");
        this.visualOutput = tag.getLong("VisualOutput");
        this.heat = tag.getInt("Heat");
        this.warnCooldown = tag.getInt("WarnCooldown");
        this.overspeed = tag.getInt("Overspeed");
        this.hasCog = !tag.contains("HasCog") || tag.getBoolean("HasCog");
        if (tag.contains("Spin")) {
            this.spin = tag.getFloat("Spin");
        }
        if (tag.contains("LastSpin")) {
            this.lastSpin = tag.getFloat("LastSpin");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean tryInsertCog(Player player, InteractionHand hand, ItemStack stack, BlockPos soundPos) {
        if (this.hasCog || stack.isEmpty()) {
            return false;
        }
        int requiredMeta = getGearMeta();
        if (requiredMeta >= 2) {
            return false;
        }
        if (stack.getItem() != (requiredMeta == 1 ? HbmItems.GEAR_LARGE_STEEL.get() : HbmItems.GEAR_LARGE.get())) {
            return false;
        }

        if (this.level != null && !this.level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.setItemInHand(hand, stack);
            }
            this.hasCog = true;
            this.overspeed = 0;
            this.warnCooldown = 0;
            this.level.playSound(null, soundPos, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.5F, 0.75F);
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private void generateFromNearbyHeat(Level level, BlockState state) {
        long oldVisualOutput = this.visualOutput;
        if (!this.hasCog) {
            this.overspeed = 0;
            this.warnCooldown = 0;
            if (this.powerBuffer > 0L) {
                this.powerBuffer--;
            }
            this.visualOutput = this.powerBuffer;
            this.heat = 0;
            setChanged();
            syncVisuals(level, oldVisualOutput);
            return;
        }

        this.powerBuffer = 0L;
        this.visualOutput = 0L;
        this.heat = 0;
        pullHeatFromBelow(level);
        this.powerBuffer = (long) (this.heat * (state.is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get()) ? 1.0D : HbmConfig.STIRLING_EFFICIENCY.get()));
        this.visualOutput = this.powerBuffer;

        if (this.warnCooldown > 0) {
            this.warnCooldown--;
        }
        if (this.heat > maxHeat(state) && !isCreative()) {
            this.overspeed++;
            if (this.overspeed > 60 && this.warnCooldown == 0) {
                this.warnCooldown = 100;
                // [parity Q3: playSoundEffect at x+0.5, y+1, z+0.5]
                level.playSound(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D, HbmSoundEvents.WARN_OVERSPEED.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
            if (this.overspeed > HbmConfig.STIRLING_OVERSPEED_LIMIT.get()) {
                ejectCog(level, state);
                // [parity Q3: the !hasCog decay runs in the same tick the cog is ejected]
                if (this.powerBuffer > 0L) {
                    this.powerBuffer--;
                }
            }
        } else {
            this.overspeed = 0;
        }
        setChanged();
        syncVisuals(level, oldVisualOutput);
    }

    private void ejectCog(Level level, BlockState state) {
        this.hasCog = false;
        level.explode(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D, 5.0F, Level.ExplosionInteraction.NONE);

        Direction facing = state.hasProperty(StirlingGeneratorBlock.FACING) ? state.getValue(StirlingGeneratorBlock.FACING) : Direction.NORTH;
        Direction flight = facing.getCounterClockWise();
        CogEntity cog = new CogEntity(
                level,
                this.worldPosition.getX() + 0.5D + facing.getStepX(),
                this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() + 0.5D + facing.getStepZ()
        ).setOrientation(facing.ordinal()).setMeta(getGearMeta());
        cog.setDeltaMovement(new Vec3(
                flight.getStepX(),
                1.0D + (this.heat - maxHeat(state)) * 0.0001D,
                flight.getStepZ()
        ));
        level.addFreshEntity(cog);
        this.overspeed = 0;
        this.warnCooldown = 0;
    }

    private void pullHeatFromBelow(Level level) {
        BlockPos pos = this.worldPosition.below();
        HeatSourceBlockEntity source = null;
        if (level.getBlockEntity(pos) instanceof HeatSourceBlockEntity directSource) {
            source = directSource;
        } else if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity dummySource) {
            source = dummySource;
        }

        if (source != null) {
            int pulled = (int) (source.getHeatStored() * HbmConfig.STIRLING_DIFFUSION.get());
            if (pulled > 0) {
                source.useHeat(pulled);
                this.heat += pulled;
                return;
            }
        }

        if (this.heat > 0) {
            this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
        }
    }

    private void clientSpin(BlockState state) {
        long animationPower = Math.max(this.lastOutput, this.visualOutput);
        float momentum = animationPower * 50F / Math.max(1F, maxHeat(state));
        if (state.is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get())) {
            momentum = Math.min(momentum, 45F);
        }

        this.lastSpin = this.spin;
        this.spin += momentum;
        if (this.spin >= 360F) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }
    }

    private void syncVisuals(Level level, long oldVisualOutput) {
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    private int maxHeat(BlockState state) {
        // [parity Q3: TileEntityStirling.maxHeat() is hardcoded 300/1500; maxHeatNormal/maxHeatSteel are unused in the original]
        return state.is(HbmBlocks.MACHINE_STIRLING.get()) ? 300 : 1500;
    }
}

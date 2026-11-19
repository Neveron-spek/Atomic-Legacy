package com.atomiclegacy.tileentity;

import com.atomiclegacy.items.LegacyJetpackItem;
import com.atomiclegacy.blocks.RefuelerBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Legacy fuel dispenser: exactly one 100 mB configured tank and player-worn container filling. */
public final class RefuelerBlockEntity extends BlockEntity implements com.atomiclegacy.fluid.HbmFluidNetUser { // [parity U1: fluid net]
    public static final int CAPACITY = 100;
    private final HbmFluidTank tank = new HbmFluidTank(HbmFluids.byName("kerosene").orElse(HbmFluids.none()), CAPACITY);
    private boolean operating;
    private int operatingTime;
    private float fillLevel;
    private float previousFillLevel;

    public RefuelerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.REFUELER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RefuelerBlockEntity refueler) {
        if (level.isClientSide) {
            refueler.tickClient(level, state);
        } else {
            refueler.tickServer(level, state);
        }
    }

    public HbmFluidTank tank() {
        return tank;
    }

    public void setConfiguredFluid(HbmFluidDefinition fluid) {
        if (fluid == null || tank.type() == fluid) { // [parity Q4: setTankType also accepts NONE]
            return;
        }
        tank.setType(fluid);
        sync();
    }

    public float fillLevel(float partialTick) {
        return previousFillLevel + (fillLevel - previousFillLevel) * partialTick;
    }

    private void tickServer(Level level, BlockState state) {
        // [parity Q4: TileEntityRefueler.getPorts] port at dir = opposite(meta), which is FACING here (FACING = opposite of meta).
        com.atomiclegacy.fluid.HbmFluidNetworks.subscribe(level, this); // [parity U1: fluid net]

        boolean wasOperating = operating;
        operating = false;
        // [parity Q4: TileEntityRefueler] box (x+0.5, y, z+0.5)-(x+0.5, y+0.5, z+0.5) expanded by 0.5/0/0.5;
        // only IFillableItem equivalents are filled (no plain containers), armor mods are pried, filled and re-applied.
        AABB area = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1.0D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 1.0D);
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            for (ItemStack stack : fillTargets(player)) {
                if (stack.isEmpty()) continue;
                if (fillFillable(stack)) operating = true;
                if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && hasMods(stack)) {
                    for (ItemStack mod : com.atomiclegacy.util.ArmorModHandler.pryMods(stack, level.registryAccess())) {
                        if (mod.isEmpty()) continue;
                        if (fillFillable(mod)) {
                            com.atomiclegacy.util.ArmorModHandler.applyMod(stack, mod, level.registryAccess());
                            operating = true;
                        }
                    }
                }
            }
        }
        if (operating) {
            if (operatingTime % 20 == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
            operatingTime++;
        } else {
            operatingTime = 0;
        }
        if (wasOperating != operating || operating || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level, BlockState state) {
        previousFillLevel = fillLevel;
        float target = CAPACITY == 0 ? 0.0F : tank.amount() / (float) CAPACITY;
        float rate = target > fillLevel || !operating ? 0.1F : 0.01F;
        fillLevel += (target - fillLevel) * rate;
        if (operating) {
            // [parity Q4: TileEntityRefueler client branch] "fluidfill" crit particle tinted with the tank colour.
            Direction dir = state.getValue(RefuelerBlock.FACING); // = opposite(meta), as dir in the original
            Direction rot = dir.getCounterClockWise();
            com.atomiclegacy.client.ParityQ4ClientParticles.fluidfill(
                    worldPosition.getX() + 0.5D + level.random.nextDouble() * 0.0625D + dir.getStepX() * 0.5D + rot.getStepX() * 0.25D,
                    worldPosition.getY() + 0.375D,
                    worldPosition.getZ() + 0.5D + level.random.nextDouble() * 0.0625D + dir.getStepZ() * 0.5D + rot.getStepZ() * 0.25D,
                    -dir.getStepX() + level.random.nextGaussian() * 0.1D, 0.0D,
                    -dir.getStepZ() + level.random.nextGaussian() * 0.1D, tank.type().color());
        }
    }

    private static boolean hasMods(ItemStack armor) {
        return armor.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag()
                .contains(com.atomiclegacy.util.ArmorModHandler.MOD_COMPOUND_KEY);
    }

    /** [parity Q4: TileEntityRefueler.fillFillable] tank.setFill(tryFill(...)); true when the tank lost fluid. */
    private boolean fillFillable(ItemStack stack) {
        if (tank.type().isNone() || tank.amount() <= 0 && !(stack.getItem() instanceof LegacyJetpackItem)) {
            return false;
        }
        int prevFill = tank.amount();
        if (stack.getItem() instanceof LegacyJetpackItem jetpack) {
            if (!jetpack.acceptsFuel(tank.type().name().toLowerCase(java.util.Locale.ROOT))) return false;
            int accepted = jetpack.fill(stack, tank.type().name().toLowerCase(java.util.Locale.ROOT), tank.amount());
            if (accepted > 0) tank.drain(tank.type(), accepted, false);
            return tank.amount() < prevFill;
        }
        if (!isFillableItem(stack)) return false;
        IFluidHandler handler = FluidUtil.getFluidHandler(stack).orElse(null);
        if (handler == null || tank.amount() <= 0) return false;
        int accepted = handler.fill(HbmFluids.toNeoStack(tank.type(), tank.amount()), IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) tank.drain(tank.type(), accepted, false);
        return tank.amount() < prevFill;
    }

    /** Port classes whose 1.7.10 originals implement IFillableItem (JetpackFueledBase is handled above). */
    private static boolean isFillableItem(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof com.atomiclegacy.items.FueledArmorFSBItem
                || item instanceof com.atomiclegacy.items.LegacyChainsawItem
                || item instanceof com.atomiclegacy.items.BlowtorchItem
                || item instanceof com.atomiclegacy.items.LegacyPipetteItem
                || item instanceof com.atomiclegacy.items.weapon.sedna.impl.ItemGunDrill
                || item instanceof com.atomiclegacy.items.weapon.sedna.impl.ItemGunChemthrower;
    }

    private static List<ItemStack> fillTargets(Player player) {
        return List.of(player.getMainHandItem(), player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        Direction back = getBlockState().getValue(RefuelerBlock.FACING); // [parity Q4: port side]
        return side == null || side == back ? tank : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.save());
        tag.putBoolean("operating", operating);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("tank")) {
            tank.load(tag.getCompound("tank"));
        }
        operating = tag.getBoolean("operating");
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
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // [parity U1: fluid net] TileEntityRefueler as IFluidStandard*MK2 with TilePort nodes
    @Override public com.atomiclegacy.fluid.HbmFluidTank[] fluidReceivingTanks() { return new com.atomiclegacy.fluid.HbmFluidTank[]{tank}; }
    @Override public java.util.List<FluidPort> fluidPorts(com.atomiclegacy.fluid.HbmFluidDefinition type) {
        if (this.level == null) return java.util.List.of();
        return java.util.List.of(new FluidPort(this.worldPosition.relative(getBlockState().getValue(RefuelerBlock.FACING)), getBlockState().getValue(RefuelerBlock.FACING)));
    }
}
